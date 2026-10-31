# SubN3xt Studio

Android-first, offline-first subtitle transcription and editing project.

## Current status

The repository is in **Phase 0 — Project Audit & Foundation**. The existing app can compile into a debug APK in GitHub Actions, but runtime Whisper quality/performance and end-to-end subtitle correctness have not yet been accepted.

- Latest verified CI baseline: [Run #12](https://github.com/nut2rq/SubN3xt-Android/actions/runs/41059977235)
- Audit branch: `phase-0/project-audit`
- Current implementation is an MVP, not yet a release-quality subtitle studio.

## Project documents

- [Architecture and current-state audit](docs/PROJECT_ARCHITECTURE.md)
- [Milestones and acceptance criteria](docs/DEVELOPMENT_PLAN.md)
- [Known issues and risks](docs/KNOWN_ISSUES.md)
- [Build baseline](docs/BUILD_BASELINE.md)

## Development rule

Work phase by phase. Do not start a later phase until the current phase's acceptance criteria are demonstrated. Every meaningful change must be committed and tested. A successful compile is not proof of runtime correctness.
