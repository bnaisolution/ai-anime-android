# Android CI evidence — run 42577400016

**Repository:** `bnaisolution/ai-anime-android`
**Branch:** `opencode/android-4-batch-testing`
**Tested SHA:** `44bfa15c5e23d09e51a2d3c681c9cca3b219f844`
**Workflow:** `android-tests.yml` (`Android Unit Tests`)
**Result:** SUCCESS
**URL:** https://github.com/bnaisolution/ai-anime-android/actions/runs/42577400016

## Results

| Job | Result | Counts |
|---|---|---|
| JVM unit tests | PASS | 20/20, 0 failures, 0 errors, 0 skipped |
| Android lint | PASS (informational) | 25 findings: 14 warnings, 11 information, 0 errors |
| Debug/test APK build | PASS | Assembled by API 34 emulator job |
| Compose instrumentation | PASS | 116/116, 0 failures, 0 errors, 0 skipped |
| Maestro smoke | PASS | 2/2 flows completed |
| Android summary | PASS | Workflow conclusion `success` |

## Instrumentation breakdown

| Test class | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `Batch2ScreenCoverageTest` | 33 | 0 | 0 | 0 |
| `Batch2FullControlCoverageTest` | 35 | 0 | 0 | 0 |
| `Batch2SharedComponentTest` | 10 | 0 | 0 | 0 |
| `Batch2InteractionTest` | 8 | 0 | 0 | 0 |
| `Batch2NavigationTest` | 11 | 0 | 0 | 0 |
| `Batch2AccessibilityTest` | 5 | 0 | 0 | 0 |
| `Batch2ControlBehaviorTest` | 10 | 0 | 0 | 0 |
| `P0ComposeSemanticsTest` | 4 | 0 | 0 | 0 |
| **Total** | **116** | **0** | **0** | **0** |

## Maestro flows

| Flow | Result |
|---|---|
| `.maestro/01-launch.yaml` | COMPLETED |
| `.maestro/02-auth-validation.yaml` | COMPLETED |

Latest run directory: `maestro/2026-11-09_071455/`.

## Files

- `run-metadata.json` — run identity and result summary.
- `unit/TEST-*.xml` — JVM JUnit XML.
- `instrumentation/TEST-emulator-5554 - 14-_app-.xml` — connected instrumentation JUnit XML.
- `instrumentation/test-results.log` — UTP test log.
- `lint/lint-results-debug.xml` — Android lint XML.
- `maestro/2026-11-09_071455/` — Maestro commands, logs, hierarchy, screenshot, and manifests.

## Notes

- The run includes the 35-method `Batch2FullControlCoverageTest` control-level expansion.
- The Maestro artifact also contains an earlier stale run directory (`2026-11-09_062739`) from the cached `~/.maestro/tests` upload; only `2026-11-09_071455` is the authoritative final run and is copied here.
- No production credentials, backend deployment, merge to `main`, or force push were used.
