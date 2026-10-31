# Project Architecture & Audit

Audit date: 2026-10-31  
Repository: `nut2rq/SubN3xt-Android`  
Audited baseline: `d487a0533f0738b3551712a7076752e8aeedb833`

## 1. Executive summary

The current repository is a small Android Compose MVP. It has a working CI compile baseline, but most application logic is concentrated in one Kotlin file. It is not yet safe to expand features without first separating core data, transcription, subtitle formatting, and translation responsibilities.

**Decision:** Phase 0 is documentation and baseline preservation only. No production source code is changed in this audit branch.

## 2. Current repository shape

Current tracked files at the audited revision:

- `.github/workflows/android-apk.yml`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/subn3xt/studio/MainActivity.kt`
- `build.gradle.kts`
- `gradle.properties`
- `settings.gradle.kts`

There is no README, Gradle wrapper, test source set, or existing architecture documentation on the audited `main` revision. This branch adds project documentation.

## 3. Current implementation (observed)

### Android application

- Kotlin + Jetpack Compose.
- `minSdk 26`, `targetSdk 35`, `compileSdk 35`.
- Java 17 bytecode target.
- One Activity and one large `MainActivity.kt` containing UI, media import, model download, Whisper invocation, translation, timestamp parsing, and SRT generation.
- Project metadata is stored in SharedPreferences.
- Subtitle cues are held in Compose state; no durable per-cue database persistence was found.
- Imported media is copied to app cache before transcription.

### Transcription

- Declared dependency: `dev.ffmpegkit-maintained:whisper-android:1.0.0`.
- Current model URL points to Whisper.cpp `ggml-base.bin`.
- Current app downloads a model if a local file larger than 100 MB is not present.
- The current language selector exposes only Auto, Thai, and English.
- The UI can show model-download percentage. During transcription it shows an indeterminate progress indicator.
- The app calls Whisper transcription as one operation and maps returned segments to subtitle cues.
- No chunk checkpointing, resume, durable job state, cancellation path, or device benchmark harness was found.

**Not yet verified:** actual inference success on the target POCO device, compatibility of the downloaded model with the packaged native runtime, language accuracy, speed, RAM, thermal behavior, and battery impact. A CI compile cannot establish these.

### Subtitle model and SRT

- Current `Cue` stores start and end as strings and has one text field.
- Original and translated text are not separate.
- Timestamp parsing/formatting helpers exist in `MainActivity.kt`.
- The current SRT builder visibly uses escaped backslash-n sequences in its output template and join separator (`"\\n"` in Kotlin source), rather than actual newline characters. This matches the malformed export shown during user testing.
- No automated SRT parser/exporter tests were found.

### Translation

- Declared dependency: Google ML Kit Translate `17.0.3`.
- Current UI/action is hard-coded to Chinese → Thai.
- Translation creates a translator in the per-cue helper and processes cues sequentially.
- There is no general provider interface, configurable source/target language UI, selected-cue translation, durable translation state, or accurate completed/total progress.
- The user's example contains English text, while the current translation action assumes Chinese source. This is a product behavior mismatch.

### CI / build

- Workflow: `.github/workflows/android-apk.yml`.
- Triggers: pushes to `main` and manual dispatch.
- Uses Java 17 and Gradle 8.9.
- Runs `:app:assembleDebug` and uploads the debug APK.
- It does not run unit tests, lint, or static analysis.
- No Gradle wrapper files were found in the repository tree.

## 4. Target architecture (incremental, not a big-bang rewrite)

Keep Android UI thin and move domain logic into testable modules/packages:

- `core/model`: project, media, job, cue, language and error models.
- `core/data`: repositories and persistence.
- `core/jobs`: job state machine, progress events, cancellation and recovery.
- `media`: SAF import, metadata, cache and lifecycle.
- `transcription/api`: engine interface and normalized result types.
- `transcription/whisper`: current Whisper adapter and model manager.
- `subtitle`: millisecond-based cue model, timestamp parser/formatter, SRT parser/exporter/validator.
- `translation/api`: provider interface and translation result/error types.
- `translation/mlkit`: first provider implementation, after supported languages and behavior are verified.
- `app`: Compose screens and navigation only.

Do not create every package in one large refactor. Extract and test one responsibility at a time, preserving a buildable state after each step.

## 5. Core domain decisions

1. Store subtitle times as integer milliseconds; convert to SRT timestamp strings only at import/export boundaries.
2. Keep `originalText` and `translatedText` separate.
3. Give each cue a stable ID; list position is not its identity.
4. Represent jobs with explicit states: queued, preparing, downloading_model, transcribing, translating, completed, failed, cancelled.
5. Progress must be typed and honest: determinate only when the engine reports measurable work; otherwise show an indeterminate state and stage label.
6. Persist each completed transcription chunk and its checkpoint before reporting it as safely completed.
7. Keep the UI independent of a particular Whisper or translation provider.

## 6. Non-goals for Phase 0

- No UI redesign.
- No additional language features.
- No translator rewrite.
- No Whisper model change.
- No speculative progress percentage.
- No broad rewrite of MainActivity.

Phase 0 establishes a shared, evidence-based map. Phase 1 will prove the riskiest runtime path on a real device.
