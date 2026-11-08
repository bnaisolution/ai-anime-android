# Android 4-Batch Final Acceptance Report

**Project:** AI Cinematic Anime Engine — Android

**Repository:** `bnaisolution/ai-anime-android`

**Local repository:** `/data/data/com.termux/files/home/anime-audit/ai-anime-android`

**Branch:** `opencode/android-4-batch-testing`

**Baseline revision:** `0ddea5c9d1c5a335f349366d24c37649b2879115`

**Tested code revision:** `b01ee3c9094c71fb42a22e865b3bfc9ba078d518`

**Working tree:** clean at the tested revision; this report is a docs-only follow-up commit

**Report date:** 2026-11-08

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

1. 179 of 206 interactive controls are covered by screen smoke rather than individual behavior assertions.
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
| Interactive controls | 206 | 206 | MATCH |
| Actions | 99 | 99 | MATCH |
| Fields | 56 | 56 | MATCH |
| Dialogs | 13 | 13 | MATCH |
| Menus | 23 | 23 | MATCH |
| Tabs | 15 | 15 | MATCH |
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
| Tabs | 15 |
| **Interactive controls** | **206** |
| Explicitly asserted | 27 |
| Screen-smoke only | 179 |

The 33 screen tests prove each screen renders and exposes its root tag. The 27 explicitly asserted controls have behavior or semantics assertions. The remaining 179 controls are not individually asserted; this is the primary residual coverage risk.

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
| `CanonViewerScreen.kt` | 6 | 2 | 1 | 0 | 4 | 13 | 0 | 13 |
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
| **Total** | **99** | **56** | **13** | **23** | **15** | **206** | **27** | **179** |

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

## 18. Remaining defects and risks

No open confirmed defect remains from the executed JVM, lint, instrumentation, or Maestro suites.

Residual risks:

1. **Control coverage:** 179 of 206 interactive controls are not individually asserted. A defect isolated to one of those controls could escape the current suite.
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

1. Add individual behavior assertions for high-risk controls among the 179 screen-smoke-only controls.
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
