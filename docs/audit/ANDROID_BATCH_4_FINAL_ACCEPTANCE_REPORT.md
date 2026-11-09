# Android Batch 4 — Regression and Final Acceptance Report

**Status:** PARTIAL ACCEPTANCE — JVM/COMPILE/STATIC PASS; ANDROID BUILD, INSTRUMENTATION, LINT, AND NEW-CODE CI BLOCKED/NOT RUN

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
| Standard Android resource/build | AAPT2 exec failure | Host evidence | BLOCKED |
| Android lint | Not executed | AAPT2 blocker | BLOCKED |
| Compose instrumentation | Not executed | No emulator/device | NOT RUN |
| Maestro flows | Not executed | No local Maestro/emulator | NOT RUN |
| New-code GitHub Actions | Not executed | No push permitted | NOT RUN |
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
| Screen coverage | 33 | BLOCKED |
| Shared components | 10 | BLOCKED |
| Interaction | 8 | BLOCKED |
| Navigation | 11 | BLOCKED |
| Accessibility | 5 | BLOCKED |
| Control behavior | 10 | BLOCKED |
| Existing P0 semantics | 4 | BLOCKED |
| **Total Android test sources** | **81** | **NOT RUN** |

Control coverage: 27 of 206 screen controls have explicit assertions; 179 remain screen-smoke only. This is partial coverage, not full acceptance.

## 5. Blockers and residual risk

### Environment blockers

- Host `aarch64` cannot execute x86_64 AAPT2.
- `:app:processDebugResources` and standard instrumentation packaging fail locally.
- No local emulator, `adb`, physical device, Maestro, or screenshot execution.
- No local `yamllint`, `actionlint`, PyYAML, or js-yaml for workflow linting.

### Process blockers

- No commit/push means new-code GitHub Actions cannot run.
- Workflow dispatch would test the remote baseline, not the local changes.
- No new-code CI PASS, emulator PASS, Maestro PASS, or physical-device PASS can be claimed.

### Residual risk

- 81 Android test sources compile but have never executed.
- 179 of 206 controls lack explicit assertions.
- Accessibility runtime behavior (TalkBack, font scaling, contrast, RTL, switch access) is not verified.
- The Batch 3 workflow and Maestro flows are manually reviewed but not machine-validated or executed.
- Remote baseline CI success does not validate local uncommitted changes.
- Additional repository behavior defects may remain outside the 8 tested scenarios.

## 6. Release acceptance decision

| Acceptance question | Answer |
|---|---|
| Is the JVM/repository foundation verified? | YES — 20/20 pass |
| Is production Kotlin compilable? | YES — compile-only with resource exclusion |
| Is Android-test source compilable? | YES — compile-only with stub and exclusion |
| Is the APK verified for these local changes? | NO — BLOCKED |
| Is instrumentation verified? | NO — NOT RUN |
| Is lint verified? | NO — BLOCKED |
| Is new-code CI verified? | NO — NOT RUN |
| Is full UI/accessibility acceptance complete? | NO — partial |
| Is the app approved for production release? | NO |

**Batch 4 verdict:** **PARTIAL ACCEPTANCE.** The codebase has a verified JVM foundation and compile/static readiness, but not full Android acceptance. The next real gate is a pushed revision running `.github/workflows/android-tests.yml` and `.github/workflows/android-maestro.yml` successfully.
