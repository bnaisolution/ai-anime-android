# Android Phase 1 — Testing Architecture

**Status:** IMPLEMENTED — JVM COMPILE/TEST VALIDATED; ANDROID INSTRUMENTATION BLOCKED

**Baseline:** `0ddea5c9d1c5a335f349366d24c37649b2879115`

## Design

Phase 1 introduces seams for testing without changing production API behavior or adding a DI framework.

| Layer | Phase 1 mechanism | Status |
|---|---|---|
| Repository contract | `AnimeRepositoryApi` split into feature interfaces and a composite contract | IMPLEMENTED |
| Production repository | `AnimeRepository(apiService, tokenManager)` implements the composite contract | IMPLEMENTED |
| Fakes | Feature interfaces provide fail-fast default methods; tests override only required operations | IMPLEMENTED |
| DI | Manual `AppContainer` / `DefaultAppContainer` | IMPLEMENTED |
| Compose access | `LocalAppContainer` CompositionLocal | IMPLEMENTED |
| ViewModels | Constructor injection through `AppViewModelFactory`; no global repository lookup | IMPLEMENTED |
| Auth | `AuthViewModel.Factory(AppContainer)` uses container services | IMPLEMENTED |
| HTTP | `RetrofitClient.create(baseUrl, tokenManager, client)` supports isolated clients; cache reset for tests | IMPLEMENTED |
| Session state | `TokenManager` supports instance clearing and companion reset for tests | IMPLEMENTED |
| Compose semantics | Central `TestTags`; P0 and shared components expose stable tags | IMPLEMENTED |
| JVM tests | Repository contract, Retrofit isolation, and repository behavior tests | IMPLEMENTED; 20 PASSED LOCALLY |
| Instrumentation semantics | `P0ComposeSemanticsTest` and Batch 2 Compose tests | IMPLEMENTED; COMPILE-ONLY WITH STUB; EXECUTION BLOCKED |
| Inventory guard | `scripts/check_test_inventory.py` checks 33 screens and 35 unique routes | IMPLEMENTED |
| Emulator/UI execution | GitHub Actions emulator job | CONFIGURED IN BATCH 3; NOT RUN |
| Maestro | End-to-end smoke flows | CONFIGURED IN BATCH 3; NOT RUN |
| Screenshot tests | Visual regression | PLANNED |
| Release APK smoke | Installed-app smoke test | PLANNED |
| Post-publication testing | Isolated staging/production smoke with approved read-only actions | PLANNED |

## Safety rules for future layers

- No test backdoors, hardcoded credentials, or auth bypasses in the APK.
- Test data must use isolated accounts/projects.
- Production smoke tests must be read-only unless explicitly approved.
- Destructive production actions remain prohibited.
- New screens/routes must be added to the inventory before test coverage is considered complete.

## Local execution limitation

The Termux host is `aarch64`, while the installed Android SDK `aapt2` is x86_64. Local Gradle resource processing fails with `Exec format error`; this is an environment blocker, not a source failure.

Phase 1 source was nevertheless validated locally with a stub `R.jar` and `-x :app:processDebugResources`:

- `:app:compileDebugKotlin` — passed with warnings only.
- `:app:compileDebugUnitTestKotlin` — passed with warnings only.
- `:app:testDebugUnitTest` — 20 tests passed with a temporary test-classpath workaround.
- `:app:compileDebugAndroidTestKotlin` — passed with the stub and `-x :app:processDebugAndroidTestResources`; execution remains blocked.

Instrumentation execution, lint, emulator/UI tests, screenshots, Maestro, and new-code CI remain unvalidated locally. Historical CI remains the baseline execution evidence until a compatible local SDK or CI run validates the full Android build.
