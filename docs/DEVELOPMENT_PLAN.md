# SubN3xt Development Plan

Status date: 2026-10-31  
Rule: a phase is not complete until its acceptance criteria are demonstrated and recorded.

## Phase 0 — Project Audit & Foundation

**Goal:** know exactly what exists, preserve a reproducible baseline, and agree on architecture.

Deliverables:
- Project overview and architecture audit.
- Known issues/risk register.
- Build baseline with exact commit and CI run.
- Phase gates and engineering rules.

Acceptance:
- [x] Repository and current source files inspected.
- [x] Current dependencies, manifest, workflow and implementation boundaries recorded.
- [x] Latest successful CI run and APK artifact verified.
- [ ] Audit documents reviewed and merged to main.
- [ ] A reproducible baseline build command/wrapper decision recorded.

No feature work during this phase.

## Phase 1 — Whisper Real-Device Proof of Concept

**Goal:** prove the riskiest technical assumption before building more product features.

Thin vertical slice only:
1. Select a local audio file through Android Storage Access Framework.
2. Validate/read the audio input.
3. Load the exact Whisper model with the packaged native runtime.
4. Transcribe on-device.
5. Normalize returned segment times to milliseconds.
6. Export a valid UTF-8 SRT file.

Test corpus:
- Short Thai speech with a human-verified transcript.
- Short Mandarin Chinese speech with a human-verified transcript.
- Short English speech with a human-verified transcript.
- Include quiet/noisy audio only after clean baseline clips pass.

Model comparison:
- Start with the model/runtime combination that is already wired into the app.
- Compare Tiny/Base and a Small quantized candidate only after confirming model format and native runtime compatibility.
- Do not assume a model is compatible just because it has a Whisper name or downloads successfully.

Record for every test:
- Device model, Android version and battery start/end.
- Audio duration, codec, sample rate and channel count.
- Model name, exact file size and quantization.
- Load time, inference time and real-time factor (inference seconds / audio seconds).
- Peak memory if measurable, thermal/battery observations.
- Transcript and human-reviewed error notes.

Acceptance:
- [ ] Repeatedly transcribes the same test clips without crash or native error.
- [ ] Produces non-empty, correctly ordered segments with plausible timestamps.
- [ ] Exports SRT that parses back successfully.
- [ ] Thai, Chinese and English behavior is measured; no language is claimed supported based only on a dropdown.
- [ ] Performance and quality results are documented on the target device.
- [ ] A model choice is made from measured results, not assumptions.

## Phase 2 — Reliable Transcription Engine

- Explicit job state machine.
- Chunking strategy based on verified runtime constraints.
- Persist completed chunks/checkpoints.
- Resume after process death/relaunch.
- Cancel safely and clean temporary resources.
- Model download integrity, retry and storage checks.
- Honest stage/progress reporting.
- Thermal/battery safeguards and user-visible warnings.

Acceptance: interrupted long jobs resume without losing already committed chunks or duplicating segments.

## Phase 3 — Subtitle Engine

- Stable cue IDs.
- Millisecond timestamps.
- SRT import/export with UTF-8 and CRLF/LF handling.
- Validation for malformed timestamps, blank cues, negative duration, ordering and overlaps.
- Unit tests for valid and invalid fixtures.
- Round-trip parse → export → parse tests.

Acceptance: deterministic, standards-compliant SRT round trips, including Thai/Chinese/English text.

## Phase 4 — Translation Engine

- Provider interface.
- One first provider selected after checking actual language support, offline requirements and licensing/size implications.
- Source/target selection and language detection behavior.
- Original and translated text kept separately.
- Translate all / selected.
- Batching, retries, cancellation and real progress.
- Timestamp preservation tests.

Acceptance: translation never overwrites original text or changes cue timing; supported language pairs are explicitly documented.

## Phase 5 — Subtitle Studio UI

- Editor built on stable domain models and repositories.
- Compact cue list, expandable editing and clear original/translation comparison.
- Job status and progress screens reflect real engine events.
- Accessible controls, large-file behavior and responsive layouts.
- No UI-only fake state.

Acceptance: UI scenarios pass against real persisted data and real engine/job states.

## Phase 6 — Release QA

- CI: compile, unit tests, lint/static checks and artifact upload.
- Device test matrix and regression checklist.
- App lifecycle, storage, thermal and battery checks.
- Versioning, release notes, signed release strategy and Git tags.
- Install and smoke-test the exact release APK on target device.

Acceptance: a tagged release APK passes all release gates; debug compile alone is insufficient.

## Engineering rules

1. One phase and one bounded change set at a time.
2. Keep `main` buildable; use focused branches for work.
3. Every bug fix gets a regression test where practical.
4. Never claim runtime behavior from compile success.
5. Never display fabricated percentages.
6. Preserve original media/transcript data when translating or editing.
7. Record decisions and test evidence in the repository.
8. Tag only APKs that have passed device QA, not every successful CI build.
