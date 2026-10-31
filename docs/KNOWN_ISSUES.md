# Known Issues & Risk Register

Audit baseline: `d487a0533f0738b3551712a7076752e8aeedb833`  
Status date: 2026-10-31

Severity: Critical = blocks trustworthy core output; High = major reliability/product gap; Medium = maintainability or future risk.

| ID | Severity | Area | Finding | Evidence / impact | Planned phase |
|---|---|---|---|---|---|
| SN-001 | Critical | SRT export | Output template and separator use literal escaped `\\n` sequences instead of real line breaks | Exported file displays backslash-n text; user screenshot confirms malformed SRT | Phase 3 (a small isolated regression fix may be made earlier only if needed for Phase 1 output) |
| SN-002 | Critical | Whisper runtime | No verified on-device transcription acceptance evidence | CI proves compilation only; actual native inference/model compatibility and Thai quality are unknown | Phase 1 |
| SN-003 | High | Progress | Model download has byte-based percentage, but transcription progress is indeterminate | Current Whisper call returns as one operation; no chunk-level progress/checkpoint | Phase 2 |
| SN-004 | High | Persistence | Subtitle cues live in Compose state and are not durably saved | Process death/recreation can lose edits/transcription results | Phase 2/3 |
| SN-005 | High | Data model | Cue timestamps are strings; one text field only | Parsing and editing are fragile; original/translation cannot be safely separated | Phase 3 |
| SN-006 | High | Translation | UI and implementation hard-code Chinese → Thai | English example text and other source/target pairs are unsupported by this flow | Phase 4 |
| SN-007 | High | Translation lifecycle | Translator is created for each cue by helper | Repeated model/client setup is inefficient; no batch/job lifecycle | Phase 4 |
| SN-008 | High | Language selection | Whisper selector exposes Auto, Thai and English only | Chinese/Japanese manual selection absent; UI copy does not fully reflect actual selector | Phase 1 |
| SN-009 | High | Job lifecycle | No durable job state, chunk checkpoint, resume or explicit cancel path | Long jobs may be lost if app/process stops | Phase 2 |
| SN-010 | Medium | Project storage | Project list uses SharedPreferences JSON | No relational model, migration strategy or cue-level persistence | Phase 2/3 |
| SN-011 | Medium | CI | Workflow compiles and uploads APK but does not run tests/lint | Regressions can reach APK build unnoticed | Phase 0/3 |
| SN-012 | Medium | Build reproducibility | No Gradle wrapper files found | Local build requires an independently installed Gradle version | Phase 0 |
| SN-013 | Medium | Runtime resource use | No thermal/battery monitoring or long-run guardrails | Extended inference may heat device and consume battery | Phase 2 |
| SN-014 | Medium | Audio validation | Import path filters WAV/MP3/FLAC by MIME and filename extension; no clear preflight inspection of codec/sample rate/duration | Some valid files may be rejected or fail only after processing starts | Phase 1 |

## Important distinction

The SRT defect is directly visible in source and corroborated by the user's exported-file screenshot. Whisper accuracy, model compatibility, speed, thermal behavior and translation quality remain **unverified risks**, not confirmed runtime failures, until tested on the target phone.

## Defect handling policy

- Do not silently patch multiple unrelated issues together.
- Add a minimal regression test before or alongside each deterministic bug fix.
- Record test evidence and the commit that resolves each issue.
- Keep runtime hypotheses labeled as hypotheses until reproduced.
