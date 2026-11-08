# Android Batch 3 — Cloud Testing and CI Report

**Status:** CONFIGURED — NOT EXECUTED FOR LOCAL CHANGES (NO PUSH; WORKFLOW DISPATCH RUNS BASELINE ONLY)

**Baseline:** `0ddea5c9d1c5a335f349366d24c37649b2879115`

**Branch:** `main`

**Scope:** Android repository CI/test configuration only. No backend, worker, n8n, Supabase, AI Assistant, credential, or production infrastructure changes.

## 1. Objective

Move Android verification off the incompatible `aarch64` host and onto GitHub Actions Ubuntu x86_64 runners:

- JVM unit tests remain gating.
- Android lint remains informational.
- Compose instrumentation runs on an API 34 emulator.
- Maestro smoke flows validate launch and auth validation.
- Artifacts include test reports and emulator logcat.
- Summaries distinguish PASS/FAIL/BLOCKED/NOT RUN.

## 2. Workflow changes

### 2.1 `.github/workflows/android-tests.yml`

Updated from a unit/lint-only workflow to include:

| Job | Runner | Purpose | Gate |
|---|---|---|---|
| `unit` | `ubuntu-latest` | `testDebugUnitTest` | Gating; skipped tests prevent PASS |
| `lint` | `ubuntu-latest` | `lintDebug` | Informational |
| `instrumentation` | `ubuntu-latest` + API 34 x86_64 emulator | `connectedDebugAndroidTest` | Gating when run |
| `android-summary` | `ubuntu-latest` | Honest summary table | Always runs |

Instrumentation job details:

- `ReactiveCircus/android-emulator-runner@v2`
- API level `34`
- Target `google_apis`
- Architecture `x86_64`
- Profile `pixel_6`
- Animations disabled
- Headless SwiftShader emulator options
- KVM setup for hardware acceleration
- Uploads:
  - `app/build/reports/androidTests/connected`
  - `app/build/outputs/androidTest-results/connected`
  - `app/build/outputs/logs` (logcat)

The summary now reports instrumentation separately and explicitly says that instrumentation PASS requires real emulator execution; compilation is not treated as execution.

### 2.2 `.github/workflows/android-maestro.yml`

New manual workflow:

- Builds/installs the debug APK with `installDebug`.
- Starts an API 34 x86_64 emulator.
- Installs Maestro from `https://get.maestro.mobile.dev`.
- Runs `maestro test .maestro`.
- Uploads `.maestro` and `~/.maestro/tests` artifacts.
- Writes a Maestro summary.

### 2.3 `.github/workflows/android-build.yml`

Unchanged. It continues to build and checksum the debug APK on `push` to `main` and manual dispatch.

## 3. Maestro flows

| Flow | Purpose |
|---|---|
| `.maestro/01-launch.yaml` | Launch app with clear state; assert studio title and sign-in button |
| `.maestro/02-auth-validation.yaml` | Tap sign-in with empty fields; assert `Email and password are required.` |

The flows intentionally avoid credentials and backend state. They validate app launch and client-side auth validation only.

## 4. Validation performed

| Check | Result | Status |
|---|---|---|
| Workflow files created/updated | Yes | CONFIGURED |
| Maestro flow files created | 2 | CONFIGURED |
| YAML syntax lint | Not run — no `yamllint`/`actionlint`/PyYAML/js-yaml locally | NOT RUN |
| Manual YAML review | Completed | PASS (manual) |
| GitHub Actions instrumentation execution | Not run — local changes not pushed | NOT RUN |
| Maestro execution | Not run — no local emulator/Maestro | NOT RUN |
| New-code remote CI | Not run — no push permitted | NOT RUN |

Manual review confirmed:

- Jobs are under the top-level `jobs:` key.
- `needs` references exist: `unit`, `lint`, `instrumentation`.
- Workflow expressions use existing job outputs (`unit.outputs.skipped`, `lint.outputs.lint_outcome`).
- Emulator action inputs are valid for `ReactiveCircus/android-emulator-runner@v2`.
- Artifact paths are repository-relative.
- Maestro `appId` matches `applicationId = "org.dpdns.bnai.anime"`.

YAML was not machine-validated. A future push should confirm the first workflow run before treating it as stable.

## 5. Why no new-code CI result exists

The repository has uncommitted Batch 1–4 changes. The permitted scope forbids commit/push. `workflow_dispatch` can only execute the remote branch, so it would test the baseline, not these changes.

Therefore:

- Baseline CI evidence remains valid for `0ddea5c9d1c5a335f349366d24c37649b2879115`.
- New-code CI is **PENDING**, not PASS.
- Emulator and Maestro results are **NOT RUN**, not PASS.
- No CI result is inferred from local compilation.

## 6. Acceptance criteria for the first real run

The Batch 3 workflow should be considered accepted only when:

1. `unit` completes with zero failures, errors, and skipped tests.
2. `lint` completes and its report is uploaded; lint may remain informational.
3. `instrumentation` starts an API 34 emulator and runs `connectedDebugAndroidTest`.
4. Instrumentation report and logcat artifacts are uploaded.
5. `android-summary` shows the actual instrumentation result.
6. Maestro launch and auth-validation flows pass on the emulator.
7. No artifact upload silently misses expected files.

## 7. Batch 3 verdict

**CONFIGURED, NOT EXECUTED.** The cloud execution path now exists and is honest about gating and evidence. It must not be described as passing until a pushed revision runs successfully.
