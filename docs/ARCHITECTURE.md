# Lumina Studio — Architecture

This document describes how Lumina Studio is organized after the full refactor and
reorganization: the layering, the package conventions, every real engine's contract,
the data flow from import to export, the threading model, and the testing strategy.

---

## 1. Layered overview

```
┌────────────────────────────────────────────────────────────────┐
│  UI (Jetpack Compose, Material 3)                              │
│  MainAppScreen · screens/ · components/ · editor/ · theme/     │
│            ▲                              ▲                    │
│            │ StateFlow / events           │ calls              │
│  ViewModels (ui/viewmodels)               │                    │
│  StudioViewModel · GalleryViewModel       │                    │
│            ▲                              ▲                    │
├────────────┼──────────────────────────────┼────────────────────┤
│  DATA      │ Room repositories (data/)    │  PREFERENCES       │
│            │ ProjectRepository            │  StudioPreferences │
│            │ SessionRepository (autosave) │  (SharedPreferences)│
├────────────┴──────────────────────────────┴────────────────────┤
│  ENGINE (pure Android media layer — zero UI dependencies)      │
│  engine.analysis · engine.audio · engine.color                 │
│  engine.export · engine.media                                  │
│  MediaCodec · MediaExtractor · MediaMuxer · OpenGL ES ·        │
│  MediaPlayer · MediaStore · Bitmap                             │
└────────────────────────────────────────────────────────────────┘
```

**Rule of direction:** UI → ViewModels → (engines, repositories, preferences).
Engines never import from `ui.*` or `data.db.*`; they depend only on Android platform
APIs and `data.model` value types.

---

## 2. Package conventions

- `com.lumina.studio.engine.<domain>` — one package per media domain:
  - `analysis` — measuring and deciding (pixels, timecode, smart tools)
  - `audio` — decode, detection, synthesis, playback
  - `color` — the shared color pipeline and bitmap/frame operations
  - `export` — producing output files (MP4, share sheets)
  - `media` — MediaStore I/O and metadata
- Unit tests mirror the engine packages under `app/src/test/java/com/lumina/studio/engine/…`
  (`analysis/EngineAnalysisTest.kt`, `color/EngineRobolectricTest.kt`).
- Compose screens live in `ui/screens`, reusable widgets in `ui/components`, tool panels
  in `ui/components/tools`, view models in `ui/viewmodels`, persisted editor preferences
  in `ui/editor/StudioPreferences`.

---

## 3. Engine catalog

### engine.analysis

| Engine | Inputs | Outputs | Notes |
| --- | --- | --- | --- |
| `MediaAnalysisEngine` | `Bitmap` frames | luma histogram (Rec.601), percentiles, channel means, Gray-World AWB gains, Laplacian-variance sharpness score, Sobel edge mask, ≥95-IRE zebra mask | All values computed from real pixels; thresholds are constants documented in-file |
| `SmartComputationalEngine` | `MediaAnalysisEngine.*Stats`, `AudioGraphEngine` beat markers | AWB correction matrix, smart auto-tone parameters, beat-aligned clip splits | Pure decision layer over measured data — no heuristics on random values |
| `FrameAccurateTimecodeEngine` | fps, timecode strings, ranges | `SmpteTimecode`, quantized frame boundaries, ripple-delete split plans | Half-frame tolerance quantization; ripple-delete returns head + shifted `"_post"` tail clips |

### engine.audio

| Engine | Inputs | Outputs | Notes |
| --- | --- | --- | --- |
| `AudioGraphEngine` | video `Uri` | decoded PCM, RMS envelope array, onset (beat) marker list, `.wav` files | MediaExtractor + MediaCodec synchronous decode; adaptive-threshold onset detection; real WAV writer (RIFF header) |
| `SfxSynthesizer` | none (pure synthesis) | 7 WAV files, `MediaPlayer` previews | Algorithmically synthesized: whoosh, riser, impact, sparkle, gliss, boom, tick |
| `PreviewAudioController` | video `Uri`, mixer state | real playback bound to a dummy surface | seek / speed / volume / master mute; SFX one-shots layered over video audio |

### engine.color

| Engine | Inputs | Outputs | Notes |
| --- | --- | --- | --- |
| `ColorMatrixEngine` | `AdjustmentsState` | 4×5 `ColorMatrix` | The single source of truth for grading; used by preview, effects and export so what you see is what you get |
| `ImageProcessingEngine` | `Bitmap` + operations | processed `Bitmap` | rotate / crop / enhance used by camera capture flow |
| `VisualEffectsEngine` | per-frame masks from `MediaAnalysisEngine` | Compose `DrawScope` renderings | effects are driven by measured pixels (e.g. zebra overlay), never random dots |

### engine.export

| Engine | Inputs | Outputs | Notes |
| --- | --- | --- | --- |
| `CinematicVideoExporter` | project timeline, `ExportCodec`, `VideoResolution` | MP4 in MediaStore, progress, errors | MediaCodec decoder → GL shader with the preview's 4×5 matrix → MediaCodec encoder → `MediaMuxer`; original audio passed through sample-by-sample; track index resolved lazily because the muxer cannot start before encoder output format arrives |
| `ShareExporter` | file `Uri`, mime | system share sheet | real `FileProvider`-backed sharing |

### engine.media

| Engine | Inputs | Outputs | Notes |
| --- | --- | --- | --- |
| `RealMediaManager` | `Uri` / `Bitmap` | `VideoMetadata` / `PhotoMetadata`, `SaveResult`, saved gallery entries, registered export rows | metadata via `MediaMetadataRetriever` / `BitmapFactory` bounds; save + share via MediaStore |

---

## 4. Data flow

```
import / camera capture
        │
        ▼
RealMediaManager (metadata, save)          GalleryViewModel (real file scan)
        │
        ▼
ProjectRepository ──► Room (projects)      EditingSession autosave
        │
        ▼
StudioViewModel ──┬─► MediaAnalysisEngine ──► histograms / masks / sharpness
                  ├─► AudioGraphEngine ────► envelope / beats
                  ├─► FrameAccurateTimecodeEngine ► trims / ripple deletes
                  └─► ColorMatrixEngine ───► preview matrix
        │
        ▼
CinematicVideoExporter ──► MP4 (MediaStore) ──► ShareExporter / RealMediaManager
```

- The **preview** renders the selected frame through the same `ColorMatrixEngine` matrix
  that the **exporter** bakes into the output file.
- Export progress is computed from actually encoded frames, not a timer.

## 5. State management

- `StudioViewModel` (editor): exposes `StateFlow`s for timeline, playhead, mixer, analysis
  results and export state; persists editor mode and export defaults through
  `StudioPreferences` (SharedPreferences) and the working session through Room
  (`SessionRepository`, autosave).
- `GalleryViewModel` (gallery): scans real media on the device (no seeding), extracts
  real dimensions/durations, exposes filter/sort state (`GalleryFilter`,
  `GallerySortOrder`) and an honest empty state.
- Room is at schema v4 (cloud fields removed; real `width/height/fps/fileSizeBytes`
  added; `TimelineClip.sourceId` links clips to media).

## 6. Threading model

- ViewModels use `viewModelScope` + `Dispatchers.Default` for analysis and `Dispatchers.IO`
  for Room and file work; UI state is published via `StateFlow`.
- `CinematicVideoExporter` runs its MediaCodec loop on a dedicated worker with synchronous
  buffer-by-buffer drain; the GL renderer shares the preview's matrix values.
- `PreviewAudioController` owns its `MediaPlayer` instances on the main thread
  (MediaPlayer requirement) and is fully released on `onCleared`.

## 7. Testing strategy

| Suite | Location | Kind |
| --- | --- | --- |
| `EngineAnalysisTest` | `app/src/test/.../engine/analysis/` | Pure JVM: AWB gains, auto-tone, beat detection, SMPTE timecode, quantization idempotence, ripple-delete split semantics |
| `EngineRobolectricTest` | `app/src/test/.../engine/color/` | Robolectric with `@GraphicsMode(NATIVE)`: real `ColorMatrixColorFilter` over real bitmaps, crushed-shadows histogram, Room + WAV I/O |
| `AppInstrumentedTest` | `app/src/androidTest/` | Package/name sanity on device |

**Principle:** the engines are the source of truth; tests assert the real contracts
(measured values, real split semantics, true-black histogram bins), not assumed ones.

## 8. Build system

- Single module `:app`; versions centralized in `gradle/libs.versions.toml`.
- Gradle 9.3.1 wrapper committed; configuration cache + build cache enabled.
- Signing: `debug.keystore` at the repo root is used when present; otherwise the build
  falls back to the auto-provisioned debug keystore, so a fresh clone always builds.
- CI (`.github/workflows/android-ci.yml`): JDK 21 (Temurin) + SDK 36.1 on every push/PR
  to `main`; runs `:app:assembleDebug` and `:app:testDebugUnitTest`, uploads the APK and
  test results as artifacts.
