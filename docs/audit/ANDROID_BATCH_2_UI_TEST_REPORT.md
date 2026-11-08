# Android Batch 2 — Compose UI Test Report

**Status:** IMPLEMENTED — 77 BATCH 2 TEST SOURCES COMPILE WITH STUB R.JAR; INSTRUMENTATION EXECUTION BLOCKED (ENVIRONMENT)

**Baseline:** `0ddea5c9d1c5a335f349366d24c37649b2879115`

**Branch:** `main`

**Scope:** Batch 2. Batch 3/4 work is reported separately. No backend, CPU Worker, n8n, Supabase, AI Assistant, production credential, or security changes were made.

## Summary

Batch 2 adds Compose UI test source for all 33 screen files, shared component states and dialogs, navigation, and accessibility. Production Kotlin compilation passes after the screen-tag edits. Android test Kotlin compilation passes only with a temporary stub `R.jar` and with Android resource tasks excluded. Android instrumentation compilation and execution remain blocked by the host environment, not by an asserted application defect.

No screen or control is marked `PASS` for runtime UI behavior. The only `PASS` results in this report are compile-time/static checks.

## Test source inventory

| Test file | Tests | Purpose | Status |
|---|---:|---|---|
| `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2ScreenCoverageTest.kt` | 33 | One render/smoke test per screen; asserts screen-root tag and expected title/content text | Implemented; execution BLOCKED |
| `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2SharedComponentTest.kt` | 10 | Loading, empty, error, success, retry, confirmation, unsaved changes, project selector | Implemented; execution BLOCKED |
| `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2InteractionTest.kt` | 8 | Form validation, dialogs, tabs, and production control tags | Implemented; execution BLOCKED |
| `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2NavigationTest.kt` | 11 | Auth/studio start, drawer, bottom nav, back, parameterized routes, duplicate routes, all 35 routes | Implemented; execution BLOCKED |
| `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2AccessibilityTest.kt` | 5 | Icon labels, retry label, project refresh label, dialog text semantics, state tags | Implemented; execution BLOCKED |
| `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2ControlBehaviorTest.kt` | 10 | Recorded repository calls for create, approve/reject, prompt save, settings save, project settings, production stop, character add, Bible save, episode metadata save | Implemented; execution BLOCKED |
| `app/src/androidTest/java/org/dpdns/bnai/anime/ui/P0ComposeSemanticsTest.kt` | 4 | Existing Batch 1 P0 semantics tests; preserved | Existing; execution BLOCKED |

**Batch 2 test count:** 77 (33 + 10 + 8 + 11 + 5 + 10)

**Total Android test source count:** 81 (77 Batch 2 + 4 existing P0)

## Coverage summary

| Area | Count | Status |
|---|---:|---|
| Screen files with screen-root test tag | 33 / 33 | Implemented |
| Screen render tests | 33 | Implemented; execution BLOCKED |
| Unique routes | 35 | Navigation test source implemented; execution BLOCKED |
| Duplicate route declarations | 2 (`auth`, `studio`) | Explicitly covered by test source; execution BLOCKED |
| Parameterized routes | 2 (`production/{cmdId}`, `scenes/{epNo}`) | Explicitly covered by test source; execution BLOCKED |
| Interactive screen controls identified | 206 | 27 explicitly asserted; 179 not individually asserted |
| Shared component control assertions | 12 | Implemented; execution BLOCKED |
| Accessibility tests | 5 | Implemented; execution BLOCKED |
| TalkBack, font scaling, contrast, RTL, switch access | 0 | NOT IMPLEMENTED |

## Validation evidence

### Production Kotlin compile — PASS

Command:

```bash
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk \
ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk \
./gradlew :app:compileDebugKotlin --no-daemon -x :app:processDebugResources
```

Result: `BUILD SUCCESSFUL in 3m 15s` on the latest run (production plus Android-test Kotlin compile). Warnings only; no errors.

### Android test Kotlin compile — PASS (COMPILE-ONLY, STUB R.JAR)

Command:

```bash
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk \
ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk \
./gradlew :app:compileDebugAndroidTestKotlin --no-daemon \
  -x :app:processDebugResources \
  -x :app:processDebugAndroidTestResources
```

Result: `BUILD SUCCESSFUL in 3m 15s` for `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` with the pre-existing stub `R.jar` and resource-task exclusions. The stub is at:

`app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debugAndroidTest/processDebugAndroidTestResources/R.jar`

This validates Kotlin test-source compilation only. It does **not** validate Android resource processing, instrumentation packaging, installation, or execution. The stub is not evidence of instrumentation success.

### Standard Android test compile — BLOCKED

Without the stub/exclusions, `:app:compileDebugAndroidTestKotlin` fails before test compilation because `:app:processDebugAndroidTestResources` requires AAPT2. The host is `aarch64`; the SDK `build-tools/34.0.0/aapt2` binary is x86_64 and fails with `cannot execute binary file: Exec format error`. Gradle reports AAPT2 daemon startup failure / `syntax error: unexpected '('`.

### JVM unit tests — PASS (PREVIOUSLY EXECUTED)

Command:

```bash
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk \
ANDROID_SDK_ROOT=/data/data/com.termux/files/home/android-sdk \
./gradlew :app:testDebugUnitTest --no-daemon \
  -x :app:processDebugResources \
  -x :app:bundleDebugClassesToRuntimeJar \
  -I /data/data/com.termux/files/usr/tmp/opencode/test-classpath.init.gradle
```

Result: `BUILD SUCCESSFUL` with `--rerun-tasks`; 20 JVM tests executed, 0 failures, 0 errors, 0 skipped. The 8 new repository behavior tests cover quick-video duration, empty prompts, null voice IDs, project selection, and HTTP failure mapping. XML evidence is in `docs/audit/evidence/local-jvm/`.

### Lint — BLOCKED

`:app:lintDebug` was not successfully executed because Android resource processing is blocked by AAPT2. No lint pass is claimed.

## Test-source defects found and fixed

The first Android test compile with the stub exposed test-source errors, not application errors:

- `assertExists` and `assertDoesNotExist` were imported as extension functions; they are `SemanticsNodeInteraction` members. Imports were removed from all affected test files.
- `AdminQueuesScreen` and `DatabaseScreen` were imported from `ui.advanced`; both are in `ui.owner`.
- `AnalyticsScreen` was imported from `ui.advanced`; it is in `ui.analytics`.

After these fixes, Android test Kotlin compilation passed with the stub.

## Application defects

Three repository defects were confirmed and fixed during the expanded Batch 2 work:

- `submitQuickVideo(projectId, prompt, durationSec)` ignored `durationSec` and always sent 5 seconds. It now forwards the requested duration through `triggerQuickMedia`; a JVM test asserts 12 seconds reaches the request body and the 5-second default applies when omitted.
- `fetchSystemPrompts` returned `Resource.Success(emptyList())` for empty/null payloads. It now returns `Resource.Empty`; two JVM tests cover both cases.
- `fetchCharacters` converted a present-but-null `voice_config.voice_id` into the literal string `"null"`. It now resolves to `"UNSET"`; a JVM test covers it.

The following application-source corrections were made while adding screen tags:

- Moved misplaced screen tags from dialogs/bottom sheets to main screen roots in `AdminQueuesScreen.kt`, `JobsMonitorScreen.kt`, and `CharacterListScreen.kt`.
- Fixed duplicate `modifier` arguments introduced during tag insertion in 19 screen files.
- Production compilation passes after these edits.

These are test-enablement fixes; they are not evidence of a runtime UI defect.

## Explicitly asserted controls

Screen-level controls with explicit interaction assertions (27 of 206):

| Screen | Control |
|---|---|
| Auth | Email field, password field, submit |
| Studio dashboard | Create project FAB |
| Settings | Compute Workers tab |
| Prompt Studio | EPISODE category tab |
| Production | Pause, cancel, sync |
| Project detail | Edit project settings |
| Owner overview | Emergency stop |
| Character list | Add Character |

Shared component controls with explicit assertions (12):

- Loading state, empty state, error state, success state.
- Retry action and retry content description.
- Confirmation dialog confirm and cancel.
- Unsaved changes discard and keep editing.
- Project selector and project refresh content description.

The remaining 179 identified screen-level controls have screen smoke coverage only and are marked `NOT IMPLEMENTED` for control-level automation. They are not marked `PASS`.

## Environment blockers

- Host: `aarch64`.
- SDK AAPT2: `build-tools/34.0.0/aapt2`, x86_64.
- Direct AAPT2 execution: `cannot execute binary file: Exec format error`.
- Gradle resource tasks fail with AAPT2 daemon startup failure / `syntax error: unexpected '('`.
- No emulator, physical device, Maestro, screenshot comparison, or Compose UI runtime execution available.
- No new-code CI run because commit/push was not permitted.

## Files added or changed

Added:

- `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2TestHarness.kt`
- `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2ScreenCoverageTest.kt`
- `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2SharedComponentTest.kt`
- `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2InteractionTest.kt`
- `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2NavigationTest.kt`
- `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2AccessibilityTest.kt`
- `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2ControlBehaviorTest.kt`
- Recording fake support in `Batch2TestHarness.kt`
- Five Batch 2 audit reports in `docs/audit/`.

Changed:

- `app/src/main/java/org/dpdns/bnai/anime/ui/testing/TestTags.kt`: added `TestTags.Screen` with 33 constants.
- All 33 `*Screen.kt` files: added screen-root `testTag`.
- Test imports/package references corrected after compile feedback.

No commit or push was performed.

## Final Batch 2 status

- Test source: **IMPLEMENTED** (77 Batch 2 tests).
- Production Kotlin compile: **PASS**.
- Android test Kotlin compile with stub: **PASS (compile-only)**.
- Standard Android resource/instrumentation compile: **BLOCKED (environment)**.
- Instrumentation execution: **BLOCKED (environment)**.
- Lint: **BLOCKED (environment)**.
- Runtime UI pass/fail: **NOT CLAIMED**.
- Application defects: **3 CONFIRMED AND FIXED** (quick-video duration, empty prompt resource, null voice ID).
