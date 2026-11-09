# Android Batch 4 — Regression and Final Acceptance Report

**Status:** SUPERSEDED — FINAL CLOUD RUN `42577400016` PASSED JVM, LINT, APK, INSTRUMENTATION, AND MAESTRO; MANUAL ACCESSIBILITY/PHYSICAL DEVICE/INTEGRATION REMAIN UNTESTED OR BLOCKED

**Superseded (2026-11-09):** Cloud execution later ran on the testing branch. Final green run `42577400016` at `44bfa15c5e23d09e51a2d3c681c9cca3b219f844`: 20/20 JVM, 116/116 instrumentation (including 35 control-level tests), 2/2 Maestro, lint 0 errors. See `reports/android-testing/evidence/ci-42577400016/`.

**Baseline:** `0ddea5c9d1c5a335f349366d24c37649b2879115`

**Inventory correction (2026-11-09):** source-derived interactive controls are **205**, not 206; tabs are **14**, not 15. `CanonViewerScreen.kt` has one `TabRow` plus two `Tab` calls. See `ANDROID_BATCH_2_CONTROL_COVERAGE.md` and `ANDROID_PHASE_1_TEST_INVENTORY.md`.

**Branch:** `main`

**Scope:** Android repository only. No commit, push, deployment, credential change, or external-system mutation.

## 1. Acceptance summary

| Layer | Result | Evidence | Honest status |
|---|---|---|---|
| JVM unit tests | 20/20 pass | `docs/audit/evidence/local-jvm/` | PASS |
| Repository behavior regressions | 8/8 pass | `AnimeRepositoryBehaviorTest.kt` | PASS |
| Production Kotlin compile | Build successful | Gradle log; resource tasks excluded | PASS (compile-only) |
| Android-test Kotlin compile | Build successful | Gradle log; stub `R.jar`; resource tasks excluded | PASS (compile-only) |
| Inventory guard | 33 screens / 35 routes | `scripts/check_test_inventory.py` | PASS |
| Whitespace/diff check | Clean | `git diff --check` | PASS |
| Remote baseline unit CI | 8 tests, 0 failures | Run `42462816185` artifacts | PASS (baseline only) |
| Remote baseline APK CI | APK built + checksum | Run `42462816117` artifact | PASS (baseline only) |
| Standard Android resource/build | AAPT2 exec failure locally; cloud build successful | Host evidence; run `42577400016` | BLOCKED locally; PASS in cloud |
| Android lint | 25 findings, 0 errors | Run `42577400016` | PASS (informational) |
| Compose instrumentation | 116/116 | Run `42577400016` | PASS |
| Maestro flows | 2/2 | Run `42577400016` | PASS |
| New-code GitHub Actions | SUCCESS | Run `42577400016` | PASS |
| Physical-device acceptance | Not executed | No device | NOT RUN |

## 2. Regression evidence

### 2.1 JVM suite

| Test class | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `TokenManagerTest` | 4 | 0 | 0 | 0 |
| `AnimeApiContractTest` | 4 | 0 | 0 | 0 |
| `AnimeRepositoryApiContractTest` | 3 | 0 | 0 | 0 |
| `AnimeRepositoryBehaviorTest` | 8 | 0 | 0 | 0 |
| `RetrofitClientIsolationTest` | 1 | 0 | 0 | 0 |
| **Total** | **20** | **0** | **0** | **0** |

Command:

```bash
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk \
ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk \
./gradlew :app:testDebugUnitTest --no-daemon --rerun-tasks \
  -x :app:processDebugResources \
  -x :app:bundleDebugClassesToRuntimeJar \
  -I /data/data/com.termux/files/usr/tmp/opencode/test-classpath.init.gradle
```

### 2.2 Compile checks

Production plus Android-test Kotlin:

```bash
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk \
ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk \
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin \
  --no-daemon \
  -x :app:processDebugResources \
  -x :app:processDebugAndroidTestResources
```

Result: `BUILD SUCCESSFUL in 3m 15s`, warnings only. This used the pre-existing stub `R.jar`; it is not an Android build or instrumentation result.

### 2.3 Static and inventory checks

```bash
python3 scripts/check_test_inventory.py
python3 -m py_compile scripts/check_test_inventory.py
git diff --check
```

Result: inventory OK (33 screens, 35 routes); Python compile OK; no whitespace errors.

## 3. Defects fixed and covered

| Defect | Fix | Regression test |
|---|---|---|
| `submitQuickVideo` ignored `durationSec` and always sent 5 seconds | Forward duration through `triggerQuickMedia` | `submitQuickVideoForwardsRequestedDuration`, `submitQuickVideoUsesFiveSecondDefaultWhenOmitted` |
| `fetchSystemPrompts` returned `Success(emptyList())` for empty/null data | Return `Resource.Empty` | `emptySystemPromptListMapsToEmptyResource`, `nullSystemPromptPayloadMapsToEmptyResource` |
| `fetchCharacters` could return literal `"null"` as voice ID | Normalize null/blank to `"UNSET"` | `nullVoiceIdDoesNotLeakLiteralNullString` |

Additional regression coverage: project HTTP failure maps to `Resource.Error` with status code; first project becomes active when none is selected.

## 4. Android test inventory

| Test group | Tests | Execution |
|---|---:|---|
| Screen coverage | 33 | PASS |
| Control-level coverage | 35 | PASS |
| Shared components | 10 | PASS |
| Interaction | 8 | PASS |
| Navigation | 11 | PASS |
| Accessibility | 5 | PASS |
| Control behavior | 10 | PASS |
| Existing P0 semantics | 4 | PASS |
| **Total Android test sources** | **116** | **PASS** |

Control coverage: the 35-test expansion asserts/clicks 280 label occurrences across all 33 screens. A one-to-one 205/205 count is not claimed because labels/states can map many-to-one.

## 5. Blockers and residual risk

### Environment blockers

- Host `aarch64` cannot execute x86_64 AAPT2.
- `:app:processDebugResources` and standard instrumentation packaging fail locally.
- No local emulator, `adb`, physical device, Maestro, or screenshot execution.
- No local `yamllint`, `actionlint`, PyYAML, or js-yaml for workflow linting.

### Process blockers

- The testing branch was pushed; new-code GitHub Actions ran as `42577400016`.
- Cloud dispatch executed the tested revision, not the baseline.
- New-code CI, emulator, Maestro, and lint are now PASS in cloud; physical-device testing remains NOT RUN.

### Residual risk

- 116 Android instrumentation tests executed and passed in cloud CI.
- The 35-test control suite asserts 280 label occurrences; it does not prove backend side effects/persistence.
- Accessibility runtime behavior (TalkBack, font scaling, contrast, RTL, switch access) is not verified.
- The Batch 3 workflow and Maestro flows are machine-validated by successful execution.
- Additional repository behavior defects may remain outside the 8 tested scenarios.

## 6. Release acceptance decision

| Acceptance question | Answer |
|---|---|
| Is the JVM/repository foundation verified? | YES — 20/20 pass |
| Is production Kotlin compilable? | YES — compile-only with resource exclusion |
| Is Android-test source compilable? | YES — compile-only with stub and exclusion |
| Is the APK verified for these local changes? | YES — cloud run `42577400016` built APK/test APK |
| Is instrumentation verified? | YES — 116/116 in cloud |
| Is lint verified? | YES — 25 findings, 0 errors |
| Is new-code CI verified? | YES — run `42577400016` SUCCESS |
| Is full UI/accessibility acceptance complete? | NO — manual accessibility/physical device remain untested |
| Is the app approved for production release? | NO — integration/accessibility layers remain blocked or untested |

**Batch 4 verdict:** **SUPERSEDED / PARTIAL ACCEPTANCE.** The codebase has a verified JVM foundation, cloud APK build, lint, 116/116 instrumentation, and 2/2 Maestro in run `42577400016`. Full release acceptance is still withheld because backend DB integration, n8n runtime, full E2E, manual accessibility, and physical-device validation remain untested or blocked.
