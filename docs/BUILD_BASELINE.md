# Build Baseline

Audit date: 2026-10-31

## Verified baseline

- Repository: `nut2rq/SubN3xt-Android`
- Default branch at audit: `main`
- Baseline commit: `d487a0533f0738b3551712a7076752e8aeedb833`
- Workflow: `.github/workflows/android-apk.yml`
- Workflow run: [Run #12](https://github.com/nut2rq/SubN3xt-Android/actions/runs/41059977235)
- Run ID: `41059977235`
- Conclusion: `success`
- Job: `build`
- Successful steps: checkout, Java 17 setup, Gradle 8.9 setup, `:app:assembleDebug`, APK artifact upload.
- Artifact: `SubN3xt-Studio-debug`
- Artifact size reported by GitHub: 46,652,318 bytes.
- Artifact was not expired at audit time.

## What this proves

The audited source revision can be compiled into a debug APK by the current GitHub Actions workflow, and the workflow successfully uploaded an APK artifact.

## What this does not prove

- APK installation or launch on a physical device.
- Whisper native runtime/model compatibility.
- Speech recognition quality or timing accuracy.
- SRT output correctness.
- Translation model download or translation correctness.
- Memory, thermal, battery or long-duration stability.

## Workflow currently configured

1. Trigger on push to `main` or manual dispatch.
2. Checkout source.
3. Set up Java 17.
4. Set up Gradle 8.9.
5. Run `gradle --no-daemon :app:assembleDebug --stacktrace`.
6. Upload `app/build/outputs/apk/debug/*.apk` as `SubN3xt-Studio-debug`.

No unit-test, lint, or static-analysis task is currently run.

## Build history note

Run #12 succeeded after Run #11 failed at Kotlin compilation. The failed run should remain part of the audit history; it is not evidence that the latest audited commit fails. The current baseline is Run #12 at commit `d487a05`.

## Next build-system improvements

- Decide whether to add and commit the Gradle wrapper.
- Add a separate test task to CI when pure Kotlin domain code/tests are introduced.
- Preserve build logs and test reports as artifacts on failure.
- Keep APK artifact upload dependent on successful build.
- Add release signing/tagging only after physical-device QA.
