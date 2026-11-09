# Android 4-Batch Final Acceptance Report

**Project:** AI Cinematic Anime Engine — Android

**Repository:** `bnaisolution/ai-anime-android`

**Local repository:** `/data/data/com.termux/files/home/anime-audit/ai-anime-android`

**Branch:** `opencode/android-4-batch-testing`

**Baseline revision:** `0ddea5c9d1c5a335f349366d24c37649b2879115`

**Tested code revision:** `b01ee3c9094c71fb42a22e865b3bfc9ba078d518`

**Working tree:** the tested revision was clean; the current working tree contains two uncommitted Android test-source changes (`Batch2TestHarness.kt`, `Batch2FullControlCoverageTest.kt`) plus this report/evidence update. The test-source changes are not part of the tested revision.

**Report date:** 2026-11-08 (updated 2026-11-09)

**Inventory correction (2026-11-09):** source-derived interactive controls are **205**, not 206. `CanonViewerScreen.kt` has one `TabRow` plus two `Tab` calls (3 tab occurrences), so the prior 206 total overcounted CanonViewer by one. Tabs are 14, not 15.

**Final decision:** **CONDITIONALLY ACCEPTED**

## 1. Executive summary

All automated Android 4-batch acceptance gates passed against the exact pushed revision `b01ee3c9094c71fb42a22e865b3bfc9ba078d518`:

- JVM unit tests: **PASS** — 20/20.
- Android lint: **PASS** — completed with 25 findings (14 warnings, 11 information, 0 errors); lint remains informational.
- Debug APK and instrumentation APK: **PASS** — assembled by the API 34 emulator job.
- Compose instrumentation: **PASS** — 81/81, 0 failures, 0 errors, 0 skipped.
- Maestro smoke: **PASS** — 2/2 flows.
- GitHub Actions workflow `android-tests.yml`: **SUCCESS** — run `42536627981`.

The final decision is **CONDITIONALLY ACCEPTED** rather than unconditional acceptance because these checks remain outside the executed automated scope:

1. 178 of 205 interactive controls are covered by screen smoke rather than individual behavior assertions; a 35-method control-level expansion exists locally but has not been compiled or executed.
2. Manual accessibility checks (TalkBack, font scaling, contrast, RTL, Switch Access) were not performed.
3. Backend-enforced runtime authorization was not exercised; the backend was not modified or deployed.
4. No physical-device test was performed.

No open confirmed defect remains from the executed suites.

## 2. Repository state

| Item | Value |
|---|---|
| Branch | `opencode/android-4-batch-testing` |
| Baseline HEAD | `0ddea5c9d1c5a335f349366d24c37649b2879115` |
| Tested code HEAD | `b01ee3c9094c71fb42a22e865b3bfc9ba078d518` |
| Branch pushed | Yes, testing branch only |
| Merge to `main` | None |
| Production deployment | None |
| `PROJECT_STATE.md` | Not modified |
| Backend/CPU worker/n8n/Supabase/AWS | Not modified |

The tested revision contains the complete Batch 1–4 implementation and the fixes for all failures found during cloud execution. This report is committed after the tested revision and does not change application or test code.

## 3. Verified baseline inventory

The historical baseline was rechecked against current source:

| Item | Historical | Current source | Status |
|---|---:|---:|---|
| Screen files | 33 | 33 | MATCH |
| Composable declarations | 37 | 37 | MATCH |
| Unique routes | 35 | 35 | MATCH |
| Duplicate route declarations | 2 | 2 (`auth`, `studio`) | VERIFIED CONDITIONAL |
| Parameterized routes | 2 | 2 | MATCH |
| Interactive controls | 205 | 205 | MATCH (corrected from 206) |
| Actions | 99 | 99 | MATCH |
| Fields | 56 | 56 | MATCH |
| Dialogs | 13 | 13 | MATCH |
| Menus | 23 | 23 | MATCH |
| Tabs | 14 | 14 | MATCH (corrected from 15) |
| List surfaces | 24 | 24 | MATCH |
| State references | 152 | 152 | MATCH |
| Android instrumentation tests | 81 | 81 | MATCH |
| JVM tests executed | 20 | 20 | MATCH |

Inventory guard result:

```text
screen files: 33
unique routes: 35
inventory file: docs/audit/ANDROID_PHASE_1_TEST_INVENTORY.md
OK: every screen file and unique route is present in the inventory.
```

### Duplicate-route investigation

`AppNavGraph.kt` has two conditional `NavHost` branches:

- Unauthenticated branch: `auth` and `studio`.
- Authenticated main branch: `auth` and `studio` plus the other 33 route entries.

The duplicate `auth`/`studio` declarations are in separate conditional graphs, not duplicate registrations in one active graph. Runtime tests confirm:

- `duplicateAuthDeclarationDoesNotExposeAuthWhenAuthenticated` — PASS.
- `duplicateStudioDeclarationDoesNotBypassAuthGate` — PASS.

These are conditional gating paths, not confirmed defects.

## 4. Changes implemented

### Production/source changes

- Added repository contracts split by feature, with fail-fast defaults for test fakes.
- Added `AppContainer`, `DefaultAppContainer`, `LocalAppContainer`, and `AppViewModelFactory`.
- Added screen-root test tags to all 33 screens.
- Added stable tags for tested fields/actions in Studio, Prompt Studio, Review, Character List, Bible, and Episode Planner.
- Added bottom-navigation and drawer-navigation item tag prefixes.
- Fixed three repository defects:
  1. `submitQuickVideo` forwards the supplied `durationSec` instead of always sending 5 seconds.
  2. `fetchSystemPrompts` returns `Resource.Empty` for null/empty payloads.
  3. `fetchCharacters` normalizes null/blank voice IDs to `"UNSET"` instead of `"null"`.
- Fixed the Owner emergency-stop dialog cancel button so it exposes `TestTags.Dialog.CANCEL`.
- Fixed Prompt Studio edit action layout: the section header no longer consumes the full row width, so the `Enter Edit Mode` control is reachable.

### Test source

- 33 screen render/smoke tests.
- 10 shared-component tests.
- 8 interaction tests.
- 11 navigation tests.
- 5 accessibility semantics tests.
- 10 control-behavior tests using a recording fake repository.
- 4 preserved P0 semantics tests.
- 8 JVM repository behavior tests.

### CI and device automation

- Updated `.github/workflows/android-tests.yml` with gating JVM, informational lint, API 34 x86_64 instrumentation, Maestro smoke, and summary jobs.
- Added `.github/workflows/android-maestro.yml` for future default-branch use.
- Added `.maestro/01-launch.yaml` and `.maestro/02-auth-validation.yaml`.
- Hardened Maestro startup with an optional system-ANR dismissal and extended visibility waits.

## 5. Batch 1 — Testing foundation and JVM/API tests

**Status: PASS.**

| Check | Result | Status |
|---|---|---|
| Repository contract split | Implemented | PASS |
| DI/container isolation | Implemented | PASS |
| Token handling tests | 4/4 passed | PASS |
| API contract tests | 4/4 passed | PASS |
| Repository contract tests | 3/3 passed | PASS |
| Repository behavior tests | 8/8 passed | PASS |
| Retrofit client isolation | 1/1 passed | PASS |
| Total JVM tests | 20/20 passed | PASS |
| Inventory guard | Passed | PASS |
| Cloud JVM execution | Run `42536627981` | PASS |

### JVM evidence

| Test class | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `TokenManagerTest` | 4 | 0 | 0 | 0 |
| `AnimeApiContractTest` | 4 | 0 | 0 | 0 |
| `AnimeRepositoryApiContractTest` | 3 | 0 | 0 | 0 |
| `AnimeRepositoryBehaviorTest` | 8 | 0 | 0 | 0 |
| `RetrofitClientIsolationTest` | 1 | 0 | 0 | 0 |
| **Total** | **20** | **0** | **0** | **0** |

Evidence:

- `reports/android-testing/evidence/local-jvm/`
- `reports/android-testing/evidence/ci-42536627981/unit/`
- `reports/android-testing/evidence/ci-42536627981/run-metadata.json`

Local execution command:

```bash
./gradlew :app:testDebugUnitTest --no-daemon --rerun-tasks \
  -x :app:processDebugResources \
  -x :app:bundleDebugClassesToRuntimeJar \
  -I /data/data/com.termux/files/usr/tmp/opencode/test-classpath.init.gradle
```

The local command is a Termux JVM-only workaround using the pre-existing stub `R.jar`. Cloud execution in run `42536627981` is the authoritative JVM result.

## 6. Batch 2 — Screens, controls, navigation, accessibility

**Status: PASS — 81/81 instrumentation tests.**

### Instrumentation results

| Test class | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `Batch2ScreenCoverageTest` | 33 | 0 | 0 | 0 |
| `Batch2SharedComponentTest` | 10 | 0 | 0 | 0 |
| `Batch2InteractionTest` | 8 | 0 | 0 | 0 |
| `Batch2NavigationTest` | 11 | 0 | 0 | 0 |
| `Batch2AccessibilityTest` | 5 | 0 | 0 | 0 |
| `Batch2ControlBehaviorTest` | 10 | 0 | 0 | 0 |
| `P0ComposeSemanticsTest` | 4 | 0 | 0 | 0 |
| **Total** | **81** | **0** | **0** | **0** |

Evidence:

- `reports/android-testing/evidence/ci-42536627981/instrumentation/TEST-emulator-5554 - 14-_app-.xml`
- `reports/android-testing/evidence/ci-42536627981/instrumentation/logcat/`
- `reports/android-testing/evidence/ci-42536627981/instrumentation/test-results.log`

### Control coverage

| Control type | Count |
|---|---:|
| Actions | 99 |
| Fields | 56 |
| Dialogs | 13 |
| Menus | 23 |
| Tabs | 14 |
| **Interactive controls** | **205** |
| Explicitly asserted | 27 |
| Screen-smoke only | 178 |

The 33 screen tests prove each screen renders and exposes its root tag. The 27 previously executed controls have behavior or semantics assertions. The remaining 178 controls are not individually asserted by the executed suite; this is the primary residual coverage risk. A new 35-method control-level test source exists in the working tree but is not part of the tested revision and has not been compiled or executed.

### Per-screen control matrix

| Screen | Actions | Fields | Dialogs | Menus | Tabs | Total | Explicit | Unasserted |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `AdminQueuesScreen.kt` | 0 | 0 | 1 | 0 | 0 | 1 | 0 | 1 |
| `AIBrainScreen.kt` | 3 | 1 | 0 | 0 | 0 | 4 | 0 | 4 |
| `AnalyticsScreen.kt` | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `AuditLogScreen.kt` | 0 | 1 | 0 | 0 | 0 | 1 | 0 | 1 |
| `AuthScreen.kt` | 4 | 2 | 0 | 0 | 0 | 6 | 3 | 3 |
| `BibleScreen.kt` | 2 | 7 | 0 | 0 | 2 | 11 | 1 | 10 |
| `BudgetLedgerScreen.kt` | 1 | 1 | 0 | 0 | 0 | 2 | 0 | 2 |
| `CanonViewerScreen.kt` | 6 | 2 | 1 | 0 | 3 | 12 | 0 | 12 |
| `CharacterDeepDiveScreen.kt` | 2 | 0 | 0 | 2 | 2 | 6 | 0 | 6 |
| `CharacterDetailScreen.kt` | 5 | 0 | 0 | 4 | 0 | 9 | 0 | 9 |
| `CharacterListScreen.kt` | 7 | 3 | 1 | 0 | 0 | 11 | 3 | 8 |
| `ContinuityCheckerScreen.kt` | 3 | 0 | 0 | 2 | 0 | 5 | 0 | 5 |
| `DatabaseScreen.kt` | 1 | 0 | 0 | 0 | 0 | 1 | 0 | 1 |
| `EpisodePlannerScreen.kt` | 5 | 5 | 1 | 7 | 0 | 18 | 1 | 17 |
| `JobsMonitorScreen.kt` | 2 | 0 | 1 | 0 | 3 | 6 | 0 | 6 |
| `LoreExplorerScreen.kt` | 5 | 2 | 1 | 0 | 0 | 8 | 0 | 8 |
| `MediaLibraryScreen.kt` | 2 | 0 | 0 | 0 | 0 | 2 | 0 | 2 |
| `NotificationScreen.kt` | 1 | 0 | 0 | 0 | 0 | 1 | 0 | 1 |
| `OwnerOverviewScreen.kt` | 3 | 0 | 1 | 0 | 0 | 4 | 1 | 3 |
| `ProductionScreen.kt` | 6 | 0 | 0 | 0 | 0 | 6 | 3 | 3 |
| `ProjectDetailScreen.kt` | 7 | 4 | 1 | 0 | 0 | 12 | 2 | 10 |
| `PromptStudioScreen.kt` | 5 | 2 | 1 | 0 | 2 | 10 | 4 | 6 |
| `PublishScreen.kt` | 5 | 3 | 0 | 2 | 0 | 10 | 0 | 10 |
| `QCScreen.kt` | 3 | 1 | 0 | 3 | 0 | 7 | 0 | 7 |
| `QuickToolsScreen.kt` | 2 | 3 | 0 | 0 | 0 | 5 | 0 | 5 |
| `QuotaDashboardScreen.kt` | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `ReviewInboxScreen.kt` | 4 | 1 | 1 | 0 | 0 | 6 | 4 | 2 |
| `SceneDetailScreen.kt` | 5 | 3 | 1 | 3 | 0 | 12 | 0 | 12 |
| `SettingsScreen.kt` | 2 | 12 | 0 | 0 | 2 | 16 | 2 | 14 |
| `StudioDashboardScreen.kt` | 5 | 1 | 2 | 0 | 0 | 8 | 3 | 5 |
| `VisionAnalysisScreen.kt` | 1 | 2 | 0 | 0 | 0 | 3 | 0 | 3 |
| `WorkerManagerScreen.kt` | 1 | 0 | 0 | 0 | 0 | 1 | 0 | 1 |
| `WorkflowManagerScreen.kt` | 1 | 0 | 0 | 0 | 0 | 1 | 0 | 1 |
| **Total** | **99** | **56** | **13** | **23** | **14** | **205** | **27** | **178** |

### Navigation coverage

| Route group | Count | Test source | Result |
|---|---:|---|---|
| Auth/start | 1 | `authStartDestinationWithoutToken` | PASS |
| Studio/start | 1 | `studioStartDestinationWithToken` | PASS |
| Drawer/bottom navigation | 2 | Drawer, bottom nav, back stack | PASS |
| Parameterized | 2 | `production/{cmdId}`, `scenes/{epNo}` | PASS |
| Invalid arguments | 1 | Non-numeric scene fallback | PASS |
| Duplicate declarations | 2 | `auth`, `studio` in conditional graphs | PASS |
| All registered routes | 35 | `everyRegisteredRouteOpensItsExpectedScreen` | PASS |

### Accessibility coverage

| Area | Result |
|---|---|
| IconButton labels | Static source check: 13/13 present |
| FAB labels | Static source check: 4/4 present |
| Dialog button semantics | Runtime test PASS |
| Project refresh label | Runtime test PASS |
| Error/retry label | Runtime test PASS |
| Shared state tags | Runtime test PASS |
| Drawer/navigation icon labels | Runtime test PASS |
| TalkBack | NOT RUN |
| Font scaling | NOT RUN |
| Contrast | NOT RUN |
| RTL | NOT RUN |
| Switch Access | NOT RUN |

## 7. Batch 3 — Cloud build, emulator, Maestro

**Status: PASS.**

### Workflow configuration

| Workflow | Jobs | Result |
|---|---|---|
| `.github/workflows/android-tests.yml` | `unit`, `lint`, `instrumentation`, `maestro`, `android-summary` | SUCCESS |
| `.github/workflows/android-maestro.yml` | `maestro` | CONFIGURED; not dispatchable until on default branch |
| `.github/workflows/android-build.yml` | `build` | Existing, unchanged |

The instrumentation job uses `ReactiveCircus/android-emulator-runner@v2` with API 34, `google_apis`, `x86_64`, `pixel_6`, disabled animations, headless SwiftShader options, KVM setup, and artifact upload for connected test reports and logcat.

### Cloud execution status

| Item | Result | Status |
|---|---|---|
| Android SDK/resource processing on Ubuntu | Completed | PASS |
| Debug APK build | Completed | PASS |
| Test APK build | Completed | PASS |
| Lint | Completed; 25 findings, 0 errors | PASS (informational) |
| API 34 emulator startup | Completed | PASS |
| Connected instrumentation tests | 81/81 | PASS |
| Maestro launch flow | PASS | PASS |
| Maestro auth-validation flow | PASS | PASS |
| Logcat collection | 81 per-test logcat files | PASS |
| New-code GitHub Actions run ID | `42536627981` | PASS |
| New-code tested commit SHA | `b01ee3c9094c71fb42a22e865b3bfc9ba078d518` | PASS |

### Maestro flows

| Flow | Purpose | Result |
|---|---|---|
| `.maestro/01-launch.yaml` | Launch and assert studio title/sign-in | PASS |
| `.maestro/02-auth-validation.yaml` | Empty sign-in and assert validation error | PASS |

Run `42535478325` exposed a Maestro startup flake: a Pixel Launcher ANR dialog covered the app during launch. The flows now optionally dismiss the system "Wait" action and use extended visibility waits. Run `42536627981` passed both flows.

## 8. Batch 4 — Regression and final acceptance

| Area | Result | Status |
|---|---|---|
| JVM regression | 20/20 pass | PASS |
| Repository defect regressions | 8/8 pass | PASS |
| Production Kotlin compile | Successful on cloud | PASS |
| Android-test Kotlin compile | Successful on cloud | PASS |
| Inventory guard | 33/35 | PASS |
| `git diff --check` | Clean | PASS |
| Android lint | 25 findings, 0 errors | PASS (informational) |
| Instrumentation execution | 81/81 | PASS |
| Emulator | API 34 x86_64 | PASS |
| Maestro | 2/2 flows | PASS |
| New-code CI | Run `42536627981` | PASS |
| Physical-device testing | No device | NOT RUN |

### Failure-to-fix history

| Run | SHA | Result | Failures | Resolution |
|---|---|---|---|---|
| `42532679247` | `8f26ad4` | CANCELLED | Maestro workflow not dispatchable from testing branch | Integrated Maestro into `android-tests.yml` |
| `42532828312` | `021af6b` | FAILURE | 10 instrumentation; Maestro PATH | Fixed selectors/tags/nav; invoked Maestro via `$HOME/.maestro/bin/maestro` |
| `42534330579` | `571f139` | FAILURE | 2 instrumentation | Tagged Owner cancel button; hardened Prompt Studio save test |
| `42535478325` | `6864062` | FAILURE | 1 instrumentation; Maestro ANR | Fixed Prompt Studio edit layout; hardened Maestro startup |
| `42536627981` | `b01ee3c` | SUCCESS | 0 | Final acceptance run |

## 9. Security verification

| Check | Result | Status |
|---|---|---|
| No auth bypass/test backdoor introduced | Verified by diff review | PASS (static) |
| Token/session validation unchanged | No weakening change | PASS (static) |
| UI visibility not used as authorization boundary | No change to backend authorization | PASS (static) |
| No production credentials in tests | No credentials added | PASS (static) |
| Isolated test data | Recording/fake repositories used | PASS (static) |
| Backend authorization independently enforced | Backend not modified | PASS (static) |
| Runtime authorization behavior | Not exercised against backend | NOT RUN |

## 10. JVM test results

| Metric | Result |
|---|---:|
| Executed | 20 |
| Passed | 20 |
| Failed | 0 |
| Errors | 0 |
| Skipped | 0 |
| Authoritative environment | GitHub Actions Ubuntu, JDK 17 |

Evidence: `reports/android-testing/evidence/ci-42536627981/unit/`.

## 11. Android build and lint results

| Item | Result | Status |
|---|---|---|
| Production Kotlin compile | Cloud build successful | PASS |
| Android-test Kotlin compile | Cloud build successful | PASS |
| Debug APK assembly | Completed by emulator job | PASS |
| Instrumentation APK assembly | Completed by emulator job | PASS |
| Lint | 25 findings: 14 warnings, 11 information, 0 errors | PASS (informational) |
| Local standard resource processing | AAPT2 x86_64 on aarch64 | BLOCKED locally only |

Lint evidence: `reports/android-testing/evidence/ci-42536627981/lint/`.

## 12. Instrumentation results

| Item | Result | Status |
|---|---|---|
| Instrumentation test sources | 81 | IMPLEMENTED |
| Connected execution | 81 executed | PASS |
| Test result XML | 81 tests, 0 failures, 0 errors, 0 skipped | PASS |
| Failure diagnostics | None in final run | PASS |
| Per-test logcat | 81 files | PASS |

## 13. Emulator results

| Item | Result | Status |
|---|---|---|
| API 34 emulator startup | `google_apis`, `x86_64`, `pixel_6` | PASS |
| Application installation | Completed | PASS |
| Application launch | Completed | PASS |
| Instrumentation execution | Completed | PASS |
| Logcat | Collected | PASS |

## 14. Maestro results

| Flow | Result | Status |
|---|---|---|
| Launch smoke | Title and sign-in visible | PASS |
| Auth validation | Empty submit shows validation error | PASS |

## 15. GitHub Actions run IDs and URLs

| Run ID | Workflow | Commit SHA | Result | URL |
|---|---|---|---|---|
| `42462816185` | Android Unit Tests | `0ddea5c9d1c5a335f349366d24c37649b2879115` | SUCCESS (baseline) | `https://github.com/bnaisolution/ai-anime-android/actions/runs/42462816185` |
| `42462816117` | Build Anime APK | `0ddea5c9d1c5a335f349366d24c37649b2879115` | SUCCESS (baseline) | `https://github.com/bnaisolution/ai-anime-android/actions/runs/42462816117` |
| `42532679247` | `android-tests.yml` | `8f26ad4` | CANCELLED | `https://github.com/bnaisolution/ai-anime-android/actions/runs/42532679247` |
| `42532828312` | `android-tests.yml` | `021af6b` | FAILURE | `https://github.com/bnaisolution/ai-anime-android/actions/runs/42532828312` |
| `42534330579` | `android-tests.yml` | `571f139` | FAILURE | `https://github.com/bnaisolution/ai-anime-android/actions/runs/42534330579` |
| `42535478325` | `android-tests.yml` | `6864062` | FAILURE | `https://github.com/bnaisolution/ai-anime-android/actions/runs/42535478325` |
| `42536627981` | `android-tests.yml` | `b01ee3c` | SUCCESS | `https://github.com/bnaisolution/ai-anime-android/actions/runs/42536627981` |

The authoritative final run is `42536627981`.

## 16. Exact tested commit SHA

- Tested code SHA: `b01ee3c9094c71fb42a22e865b3bfc9ba078d518`.
- Branch: `opencode/android-4-batch-testing`.
- This report is a docs-only commit after the tested code SHA.
- No merge to `main` and no deployment occurred.

## 17. Evidence file references

| Evidence | Path |
|---|---|
| Final run metadata | `reports/android-testing/evidence/ci-42536627981/run-metadata.json` |
| Final JVM XML | `reports/android-testing/evidence/ci-42536627981/unit/` |
| Final instrumentation XML | `reports/android-testing/evidence/ci-42536627981/instrumentation/` |
| Final per-test logcat | `reports/android-testing/evidence/ci-42536627981/instrumentation/logcat/` |
| Final lint report | `reports/android-testing/evidence/ci-42536627981/lint/` |
| Final Maestro logs/artifacts | `reports/android-testing/evidence/ci-42536627981/maestro/` |
| Local JVM XML | `reports/android-testing/evidence/local-jvm/` |
| Local checks | `reports/android-testing/evidence/local-checks.txt` |
| Compile log | `reports/android-testing/evidence/gradle-compile.log` |
| Baseline unit artifacts | `reports/android-testing/evidence/ci-baseline/unit/` |
| Baseline lint artifacts | `reports/android-testing/evidence/ci-baseline/lint/` |
| Baseline APK checksum | `reports/android-testing/evidence/ci-baseline/apk/app-debug.apk.sha256` |
| Baseline run metadata | `reports/android-testing/evidence/ci-baseline/run-metadata.json` |
| Backend CI run `42458161277` | `reports/android-testing/evidence/ci-backend-42458161277/` |
| Worker CI run `42457744833` | `reports/android-testing/evidence/ci-worker-42457744833/` |
| n8n static audit | `reports/android-testing/evidence/n8n-static-2026-11-09/` |
| Android control-expansion assertion map | `reports/android-testing/evidence/android-control-expansion-2026-11-09/CONTROL_ASSERTION_MAP.md` |

## 18. Remaining defects and risks

No open confirmed defect remains from the executed JVM, lint, instrumentation, or Maestro suites.

Residual risks:

1. **Control coverage:** 178 of 205 interactive controls are not individually asserted by the executed suite. A defect isolated to one of those controls could escape the current suite.
2. **Manual accessibility:** TalkBack, font scaling, contrast, RTL, and Switch Access were not executed.
3. **Backend authorization runtime:** Android navigation gating passed, but backend-enforced authorization was not exercised because the backend was not modified or deployed.
4. **Physical device:** No OEM/physical-device test was performed.
5. **Emulator flake:** The Maestro startup ANR was mitigated, not eliminated. The optional "Wait" dismissal and extended waits reduce recurrence.

## 19. Environment limitations

- Local host: Android mobile, Termux, `aarch64`.
- Local SDK AAPT2: x86_64, fails with `cannot execute binary file: Exec format error`.
- No local emulator, system image, `adb`, Maestro, or physical device.
- No local `yamllint`, `actionlint`, PyYAML, js-yaml, or Ruby.
- Cloud execution used GitHub Actions Ubuntu runners with API 34 x86_64 emulators.
- No production credentials or backend test environment used.
- No merge to `main` and no deployment performed.

## 20. Follow-up conditions

1. Add and execute individual behavior assertions for high-risk controls among the 178 screen-smoke-only controls.
2. Run manual accessibility checks (TalkBack, font scaling, contrast, RTL, Switch Access).
3. Execute backend authorization tests in an approved non-production environment.
4. Run a physical-device smoke pass before release.
5. Keep the Maestro startup mitigation under observation for emulator flake.

## 21. Final acceptance decision

**CONDITIONALLY ACCEPTED**

Reason: all automated Android 4-batch gates passed on the exact pushed revision `b01ee3c9094c71fb42a22e865b3bfc9ba078d518`:

- JVM: 20/20.
- Instrumentation: 81/81.
- Maestro: 2/2.
- Lint: completed, 0 errors.
- Cloud workflow: SUCCESS.

Acceptance is conditional on the follow-up items in Section 20: broader control-level coverage, manual accessibility, backend runtime authorization, and physical-device validation. No unconditional release acceptance is claimed.

## 22. Master continuation scope — backend, CPU worker, n8n, integration (2026-11-09)

This section extends the report beyond the Android-only automated scope. It does not replace the tested Android revision `b01ee3c9094c71fb42a22e865b3bfc9ba078d518`.

### 22.1 Repository state

| Repository | Local path | Branch | HEAD | Working tree |
|---|---|---|---|---|
| Android | `ai-anime-android` | `opencode/android-4-batch-testing` | `e82aa27f1e7efa296519044e13723e282b641418` | Two uncommitted test-source changes: `Batch2TestHarness.kt`, `Batch2FullControlCoverageTest.kt` |
| Backend | `ai-anime-backend` | `main` | `c6fc39865c44913eb0ba59065e99abff46f715fd` | Untracked `test-results/` |
| CPU worker | `anime-cpu-worker` | `main` | `da069d3bb30d2389992cc8de82a9b5bad1e31eea` | Untracked `test-results/`, `tests/__pycache__/` |

No production repository was modified, deployed, merged to `main`, or force-pushed.

### 22.2 Backend

**CI run:** `42458161277` on exact HEAD `c6fc39865c44913eb0ba59065e99abff46f715fd` — **SUCCESS**.

| Suite | Executed | Failures | Status |
|---|---:|---:|---|
| Unit | 36 | 0 | PASS |
| Routes | 60 | 0 | PASS |
| Static (local + sibling) | 27 | 0 | PASS |
| Regression | 19 | 0 | PASS |
| Database integration | 4 | — | BLOCKED |

The database integration job was skipped because `RUN_DB_TESTS` and a safe test database were unavailable. Production database credentials were not used. The four database integration tests remain **BLOCKED**, not passed.

Evidence: `reports/android-testing/evidence/ci-backend-42458161277/`.

### 22.3 CPU worker

**CI run:** `42457744833` on exact HEAD `da069d3bb30d2389992cc8de82a9b5bad1e31eea` — **SUCCESS**.

| Check | Result | Status |
|---|---:|---|
| Contract tests | 5/5 | PASS |
| FFmpeg media generation E2E | Not executed | NOT TESTED |
| TTS/audio E2E | Not executed | NOT TESTED |
| Backend callback/communication E2E | Not executed | NOT TESTED |

Evidence: `reports/android-testing/evidence/ci-worker-42457744833/`.

### 22.4 n8n canonical workflows

**Canonical directory:** `ai-anime-backend/n8n-workflows/` — exactly 14 JSON files.

| Check | Result | Status |
|---|---|---|
| Canonical 14-file set | 14/14 | PASS (static) |
| Unique workflow IDs | 14/14 | PASS (static) |
| JSON metadata `active: true` | 14/14 | PASS (static) |
| Connection integrity | 14/14 | PASS (static) |
| Credential objects in JSON | 0 | PASS (static) |
| `retryOnFail` / `maxTries` declared | 0 nodes | NOT TESTED / risk |
| Runtime activation and execution history | API/instance access unavailable | BLOCKED |
| End-to-end workflow execution | Not executed | NOT TESTED |

The 14 workflow IDs are unique: `U4pLz7pXmG2OqXlt`, `UeEXeimGGwqIh84T`, `qRUdhg1PNDK2wJk6`, `s4RGuFSDlevgD5nC`, `8q9tF3VE0mLUZQfa`, `ADPseu4KBfVveYrq`, `WAB7tCTJSUilOp07`, `zvw63UrbObZhFUiW`, `MrWrz58aRUVTzpLr`, `gT1c0Hpvrwyf9YS6`, `swOEbQV3iNUNmMwu`, `baRqhI9JSob9m5oD`, `2fcuca0HkGxGIYMv`, `jI39p4vr4CnWhpgw`.

Static audit evidence: `reports/android-testing/evidence/n8n-static-2026-11-09/n8n-static-audit.md` and `.json`.

### 22.5 Android control-level expansion — implemented, not executed

A 35-method control-level test source has been written locally:

- `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2FullControlCoverageTest.kt`
- `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2TestHarness.kt` (expanded fake data and fake state)

The new tests wait for each screen-root `TestTags` entry and assert controls by text/content description, including dialogs, menus, tabs, conditional states, and scrollable lists. They have **not** been compiled, linted, or executed. Local Android build remains blocked by the Termux `aarch64` host and missing Android SDK/toolchain. Cloud execution requires an explicit branch push and workflow dispatch approval.

**Status: NOT TESTED.**

The 205-control inventory is corrected from 206: `CanonViewerScreen.kt` has one `TabRow` plus two `Tab` calls, not four tab occurrences. The executed suite remains 27 explicit controls and 178 screen-smoke-only controls. The new source is intended to reduce the 178 gap but cannot be credited until it passes cloud CI.

### 22.6 Accessibility and integration

| Area | Result | Status |
|---|---|---|
| TalkBack | Not performed | NOT TESTED |
| Font scaling | Not performed | NOT TESTED |
| Contrast | Not performed | NOT TESTED |
| RTL | Not performed | NOT TESTED |
| Switch Access | Not performed | NOT TESTED |
| Physical device | No device available | NOT TESTED |
| Android → Backend → n8n → Supabase/DB → Worker → Backend → Android | No approved deployed test environment, credentials, or execution logs | BLOCKED |

### 22.7 Consolidated status

| Scope | Status | Basis |
|---|---|---|
| Android JVM/lint/instrumentation/Maestro at `b01ee3c` | CONDITIONALLY ACCEPTED | Run `42536627981` green; 20 JVM, 81 instrumentation, 2 Maestro |
| Android new control-level tests | NOT TESTED | Source exists locally; not compiled or executed |
| Backend safe suites | PASS | Run `42458161277`; 36 unit, 60 routes, 27 static, 19 regression |
| Backend database integration | BLOCKED | 4 tests skipped; no safe test DB |
| Worker contract tests | PASS | Run `42457744833`; 5/5 |
| Worker media/TTS/backend E2E | NOT TESTED | Not executed |
| n8n static canonical audit | PASS | 14/14 static checks |
| n8n runtime execution | BLOCKED | No approved n8n API/instance access |
| End-to-end integration | BLOCKED | No deployed test environment or credentials |
| Manual accessibility | NOT TESTED | Not performed |

## 23. Consolidated final decision (2026-11-09)

**Android automated scope:** **CONDITIONALLY ACCEPTED** — unchanged from Section 21. The exact tested revision remains `b01ee3c9094c71fb42a22e865b3bfc9ba078d518` with run `42536627981` green.

**Full three-repository/n8n/integration scope:** **BLOCKED** — unconditional acceptance is not claimed. Backend database integration, n8n runtime execution, end-to-end Android→Backend→n8n→Supabase/DB→Worker→Backend→Android flow, the new Android control-level tests, and manual accessibility remain unexecuted or blocked.

**Overall decision:** **CONDITIONALLY ACCEPTED for the Android automated scope; BLOCKED for full integration acceptance.**

No blocked, skipped, or unexecuted check is reported as PASS.
