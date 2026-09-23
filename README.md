# Lumina Studio

Cinematic 4K video editor for Android — Kotlin + Jetpack Compose.

Every pipeline in this app is a **real implementation**: no simulated progress bars, no
seeded demo data, no placeholder algorithms. What the UI reports is what the hardware
and the algorithms actually measured.

## Real engines (what actually runs)

| Engine | What it really does |
| --- | --- |
| `CinematicVideoExporter` | MediaCodec decode → OpenGL ES ColorMatrix grade (same 4×5 matrix as the live preview) → MediaCodec encode → MediaMuxer MP4, original audio passed through sample-by-sample, progress derived from real encoded frame counts |
| `MediaAnalysisEngine` | Real pixel statistics: Rec.601 luma histogram, percentiles, Gray-World AWB channel means, Laplacian-variance sharpness, Sobel edge mask, ≥95-IRE overexposure mask |
| `AudioGraphEngine` | MediaExtractor + MediaCodec PCM decode, RMS envelope, adaptive-threshold onset (beat) detection, real WAV file writer |
| `SfxSynthesizer` | 7 sound effects synthesized algorithmically into real WAV files, previewed through MediaPlayer |
| `PreviewAudioController` | Real video-audio playback bound to a dummy surface: seek, playback speed, volume, master mute |
| `ColorMatrixEngine` / `FrameAccurateTimecodeEngine` | Shared 4×5 color pipeline and SMPTE frame-accurate timecode/ripple-delete math |

The app starts with **zero projects** — no fake seeding. Everything you see in the
gallery, timeline, VU meter and export sheet comes from real media files on the device.

## Build

Requirements: JDK 17+ (tested with JDK 21), Android SDK Platform 36.1, Gradle wrapper
included (Gradle 9.3.1, AGP 9.1.1, Kotlin 2.2.10).

```bash
./gradlew :app:assembleDebug      # produces app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest  # runs the full unit-test suite
```

Debug signing uses the project-local `debug.keystore` when present and otherwise falls
back to the standard auto-provisioned debug keystore, so a fresh clone builds out of
the box.

## Verified state

- `:app:assembleDebug` succeeds (≈22 MB debug APK)
- 32/32 unit tests pass — pure-JVM algorithm tests (AWB, auto-tone, beat detection,
  timecode/ripple math) plus Robolectric NATIVE-graphics tests over real bitmaps,
  real Room databases and real WAV files
