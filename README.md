# Lumina Studio

[![Android CI](https://github.com/CTO-DRS/Lumina-Studio/actions/workflows/android-ci.yml/badge.svg)](https://github.com/CTO-DRS/Lumina-Studio/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Min API](https://img.shields.io/badge/minSdk-24-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-purple)

**Cinematic 4K video editor for Android** — Kotlin + Jetpack Compose, built on a single
principle: *every pipeline is real*. No simulated progress bars, no seeded demo data, no
placeholder algorithms. What the UI reports is what the hardware and the algorithms
actually measured.

## Highlights

- **Frame-accurate editing** — SMPTE timecode math, ripple-delete with real split semantics
- **GPU color grading** — one shared 4×5 color matrix drives both the live preview and the exported file
- **Real signal analysis** — pixel histograms, Gray-World AWB, Laplacian sharpness, onset (beat) detection from decoded PCM
- **Honest empty states** — the app starts with zero projects; everything on screen comes from real media on the device
- **Procedural sound design** — 7 sound effects synthesized into real WAV files, no bundled binary assets

## Real engines (what actually runs)

| Engine | Package | What it really does |
| --- | --- | --- |
| `CinematicVideoExporter` | `engine.export` | MediaCodec decode → OpenGL ES ColorMatrix grade (same matrix as the live preview) → MediaCodec encode → MediaMuxer MP4; original audio passed through sample-by-sample; progress derived from real encoded frame counts |
| `ShareExporter` | `engine.export` | Real file share sheet for exported media |
| `MediaAnalysisEngine` | `engine.analysis` | Rec.601 luma histogram, percentiles, Gray-World AWB channel means, Laplacian-variance sharpness, Sobel edge mask, ≥95-IRE overexposure mask |
| `SmartComputationalEngine` | `engine.analysis` | Analysis-driven smart tools: AWB correction, smart auto-tone, beat-aligned splitting — all computed from real pixels/PCM |
| `FrameAccurateTimecodeEngine` | `engine.analysis` | SMPTE timecode conversion, quantize-to-frame, ripple-delete split math |
| `AudioGraphEngine` | `engine.audio` | MediaExtractor + MediaCodec PCM decode, RMS envelope, adaptive-threshold onset detection, real WAV file writer |
| `SfxSynthesizer` | `engine.audio` | 7 sound effects synthesized algorithmically into real WAV files, previewed through MediaPlayer |
| `PreviewAudioController` | `engine.audio` | Real video-audio playback: seek, playback speed, volume, master mute |
| `ColorMatrixEngine` | `engine.color` | Shared 4×5 color pipeline (brightness/contrast/saturation/temperature/tint/vibrance) |
| `ImageProcessingEngine` | `engine.color` | Real bitmap processing (rotate, crop, enhance) for captured frames |
| `VisualEffectsEngine` | `engine.color` | Compose DrawScope effects driven by real per-frame pixel masks |
| `RealMediaManager` | `engine.media` | Real metadata extraction, save-to-gallery, share, registered export registration via MediaStore |

The app starts with **zero projects** — no fake seeding. Everything you see in the
gallery, timeline, VU meter and export sheet comes from real media files on the device.

## Architecture

```
com.lumina.studio
├── MainActivity                  # single-activity Compose host
├── data/                         # persistence layer
│   ├── db/                       #   Room (v4): projects + editing sessions
│   ├── model/                    #   entities, timeline model, enums
│   └── repository/               #   project & session repositories
├── engine/                       # real media engines (no UI dependencies)
│   ├── analysis/                 #   pixels, smart tools, timecode math
│   ├── audio/                    #   PCM decode, beats, SFX, playback
│   ├── color/                    #   color matrix, bitmap ops, frame effects
│   ├── export/                   #   MediaCodec export + share
│   └── media/                    #   MediaStore I/O, metadata, save/share
└── ui/                           # Compose presentation layer
    ├── MainAppScreen             #   top-level scaffold + navigation host
    ├── editor/                   #   editor screen + persisted studio prefs
    ├── viewmodels/               #   StudioViewModel, GalleryViewModel
    ├── screens/                  #   dashboard, gallery, projects, AI lab, settings
    ├── components/               #   timeline, canvas, VU meter, trim slider, sheets
    │   └── tools/                #   grading, crop, filters, FX, mixer panels
    ├── navigation/               #   route definitions
    └── theme/                    #   Material 3 theme
```

A deeper walkthrough — engine-by-engine inputs/outputs, data flow, threading and the
testing strategy — lives in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Build

| Requirement | Version |
| --- | --- |
| JDK | 17+ (verified with Temurin 21) |
| Android SDK Platform | 36.1 |
| Build Tools | 36.0.0 |
| Gradle | 9.3.1 (wrapper included) |
| AGP / Kotlin | 9.1.1 / 2.2.10 |

```bash
./gradlew :app:assembleDebug      # produces app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest  # runs the full unit-test suite
```

Debug signing uses the project-local `debug.keystore` when present and otherwise falls
back to the standard auto-provisioned debug keystore, so a fresh clone builds out of
the box. Every push to `main` is built and tested by
[Android CI](.github/workflows/android-ci.yml).

## Verified state

- `:app:assembleDebug` succeeds (≈22 MB debug APK)
- **32/32 unit tests pass** — pure-JVM algorithm tests (AWB, auto-tone, beat detection,
  timecode/ripple math) plus Robolectric NATIVE-graphics tests over real bitmaps,
  real Room databases and real WAV files
- Continuous integration green on JDK 21 / API 36.1

## Downloads

Prebuilt artifacts are published on the
[Releases page](https://github.com/CTO-DRS/Lumina-Studio/releases):

- `LuminaStudio-v1.0-debug.apk` — verified debug build ([v1.0.0](https://github.com/CTO-DRS/Lumina-Studio/releases/tag/v1.0.0))
- Source snapshots (`lumina-studio-*.zip`) per release

## License

[MIT](LICENSE) © 2026 CTO-DRS
