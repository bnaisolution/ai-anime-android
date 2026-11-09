# Android Batch 1 — Testing Foundation Report

**Status:** IMPLEMENTED — LOCAL JVM TESTS PASS; STANDARD ANDROID BUILD/INSTRUMENTATION BLOCKED (ENVIRONMENT)

**Baseline:** `0ddea5c9d1c5a335f349366d24c37649b2879115`

**Inventory correction (2026-11-09):** source-derived interactive controls are **205**, not 206; tabs are **14**, not 15. `CanonViewerScreen.kt` has one `TabRow` plus two `Tab` calls. See `ANDROID_BATCH_2_CONTROL_COVERAGE.md` and `ANDROID_PHASE_1_TEST_INVENTORY.md`.

**Branch:** `main`

**Scope:** Android repository only. No backend, CPU Worker, n8n, Supabase, AI Assistant, production credential, or production infrastructure changes.

## 1. Objective

Establish a trustworthy Android testing foundation before adding broad UI coverage:

- Make production Kotlin compile reproducibly.
- Separate repository contracts from the concrete Retrofit implementation.
- Add JVM tests for auth, API contracts, repository behavior, and client isolation.
- Inventory every screen, route, control, list surface, and state reference.
- Preserve the existing P0 Compose semantics tests.
- Record the real environment blocker instead of fabricating Android build evidence.

## 2. Deliverables

| Deliverable | Status | Evidence |
|---|---|---|
| Test inventory | IMPLEMENTED | `docs/audit/ANDROID_PHASE_1_TEST_INVENTORY.md` |
| Test architecture/blocker record | IMPLEMENTED | `docs/audit/ANDROID_PHASE_1_TESTING_ARCHITECTURE.md` |
| Phase 1 implementation report | IMPLEMENTED | `ANDROID_PHASE_1_IMPLEMENTATION_REPORT.md` |
| Repository API contracts | IMPLEMENTED | `AnimeRepositoryApi.kt` |
| Dependency container | IMPLEMENTED | `core/di/AppContainer.kt`, `ui/LocalAppContainer.kt` |
| ViewModel factory | IMPLEMENTED | `ui/AppViewModelFactory.kt` |
| JVM auth tests | PASS | `TokenManagerTest.kt` — 4 tests |
| JVM API contract tests | PASS | `AnimeApiContractTest.kt` — 4 tests |
| JVM repository contract tests | PASS | `AnimeRepositoryApiContractTest.kt` — 3 tests |
| JVM client isolation test | PASS | `RetrofitClientIsolationTest.kt` — 1 test |
| JVM repository behavior tests | PASS | `AnimeRepositoryBehaviorTest.kt` — 8 tests |
| P0 Compose semantics tests | IMPLEMENTED; BLOCKED | `P0ComposeSemanticsTest.kt` — 4 tests |
| Inventory guard | PASS | `scripts/check_test_inventory.py` |

## 3. Inventory

| Item | Count |
|---|---:|
| Screen files (`*Screen.kt`) | 33 |
| `composable(...)` declarations | 37 |
| Unique routes | 35 |
| Duplicate route names | 2 (`auth`, `studio`) |
| Parameterized routes | 2 (`production/{cmdId}`, `scenes/{epNo}`) |
| Interactive controls | 206 |
| Actions | 99 |
| Fields | 56 |
| Dialogs | 13 |
| Menus | 23 |
| Tabs | 15 |
| List surfaces | 24 |
| State references | 152 |

The guard passes:

```text
screen files: 33
unique routes: 35
inventory file: docs/audit/ANDROID_PHASE_1_TEST_INVENTORY.md
OK: every screen file and unique route is present in the inventory.
```

## 4. JVM test execution

Command:

```bash
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk \
ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk \
./gradlew :app:testDebugUnitTest --no-daemon --rerun-tasks \
  -x :app:processDebugResources \
  -x :app:bundleDebugClassesToRuntimeJar \
  -I /data/data/com.termux/files/usr/tmp/opencode/test-classpath.init.gradle
```

Result: `BUILD SUCCESSFUL`.

| Test class | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `TokenManagerTest` | 4 | 0 | 0 | 0 |
| `AnimeApiContractTest` | 4 | 0 | 0 | 0 |
| `AnimeRepositoryApiContractTest` | 3 | 0 | 0 | 0 |
| `AnimeRepositoryBehaviorTest` | 8 | 0 | 0 | 0 |
| `RetrofitClientIsolationTest` | 1 | 0 | 0 | 0 |
| **Total** | **20** | **0** | **0** | **0** |

XML evidence: `docs/audit/evidence/local-jvm/`.

This is a Termux JVM-only workaround. It uses the pre-existing stub `R.jar` and excludes blocked resource tasks. It is not evidence that the Android APK builds or that instrumentation runs.

## 5. Remote baseline CI evidence

Baseline commit CI:

| Workflow/run | Result | Evidence |
|---|---|---|
| Run `42462816185` — unit/lint | SUCCESS | `docs/audit/evidence/ci-baseline/unit`, `.../lint` |
| Run `42462816117` — debug APK | SUCCESS | `docs/audit/evidence/ci-baseline/apk/app-debug.apk.sha256` |

The remote unit job reported 8 tests, 0 failures/errors/skipped. The difference from the local 20 is expected: `AnimeRepositoryApiContractTest.kt` and `RetrofitClientIsolationTest.kt` were untracked locally, and `AnimeRepositoryBehaviorTest.kt` was added afterward. No local change was pushed.

## 6. Environment blocker

- Host: `aarch64` Termux.
- SDK AAPT2: `build-tools/34.0.0/aapt2`, x86_64.
- Direct AAPT2 execution: `cannot execute binary file: Exec format error`.
- Gradle resource tasks fail with AAPT2 daemon startup failure / `syntax error: unexpected '('`.
- No emulator, physical device, `adb`, Maestro, or screenshot execution locally.
- Standard `assembleDebug`, `lintDebug`, `connectedDebugAndroidTest`, and instrumentation packaging are therefore blocked locally.

Permanent execution must use GitHub Actions Ubuntu x86_64 runners, as configured in Batch 3.

## 7. Defects and fixes

The expanded Batch 1/2 work confirmed and fixed three repository defects:

1. `submitQuickVideo` ignored `durationSec`; it always sent 5 seconds.
2. `fetchSystemPrompts` returned `Success(emptyList())` for empty/null payloads instead of `Empty`.
3. `fetchCharacters` could expose the literal string `"null"` as a voice ID.

Each fix has a JVM regression test.

## 8. Batch 1 acceptance

| Area | Status |
|---|---|
| Production Kotlin compile | PASS (with resource-task exclusion and stub; compile-only) |
| JVM tests | PASS (20/20) |
| API/repository contract separation | PASS |
| Inventory guard | PASS |
| Remote baseline CI | PASS at baseline commit |
| Standard Android resource/build | BLOCKED |
| Instrumentation execution | BLOCKED |
| Lint execution | BLOCKED |
| Android UI runtime PASS | NOT CLAIMED |

**Batch 1 verdict:** Foundation is implemented and locally verified at the JVM/compile/static level. Full Android acceptance remains blocked pending the Batch 3 Ubuntu emulator workflow and a pushed revision.
