# Android Batch 2 — Screen Coverage

**Status:** IMPLEMENTED — 33/33 SCREEN TESTS PRESENT; EXECUTION BLOCKED (ENVIRONMENT)

**Baseline:** `0ddea5c9d1c5a335f349366d24c37649b2879115`

## Method

Each `*Screen.kt` file was inspected and given a screen-root `testTag` from `TestTags.Screen`. `Batch2ScreenCoverageTest.kt` contains one test per screen. Each test sets the screen inside the Batch 2 fake container and asserts:

1. The screen-root tag exists.
2. The expected title or first meaningful content string exists.

The expected strings were statically checked against UI source. This does not prove the string is rendered in the fake `Resource.Empty` state; runtime execution is blocked.

## Status legend

- `BLOCKED` — test source exists, but Android instrumentation cannot compile/run in this environment.
- `NOT IMPLEMENTED` — no test source exists for that behavior.
- `PASS` / `FAIL` — reserved for actual runtime execution. Not used for screen tests in this report.

## Coverage matrix

Counts are source occurrences from `docs/audit/ANDROID_PHASE_1_TEST_INVENTORY.md`: Actions / Fields / Dialogs / Menus / Tabs. `Lists` and `States` are separate. `Explicit` means the screen has at least one control-level interaction assertion beyond root/title smoke coverage.

| Screen file | Route(s) | Root tag | A/F/D/M/T | Lists | States | Expected text | Explicit control test | Screen status |
|---|---|---|---|---:|---:|---|---|---|
| `AdminQueuesScreen.kt` | admin_queues | `ADMIN_QUEUES` | 0/0/1/0/0 | 0 | 2 | Pipeline Queues | No | BLOCKED |
| `AIBrainScreen.kt` | ai_brain | `AI_BRAIN` | 3/1/0/0/0 | 1 | 1 | AI Studio Director | No | BLOCKED |
| `AnalyticsScreen.kt` | analytics | `ANALYTICS` | 0/0/0/0/0 | 0 | 4 | Production & Audience Analytics | No | BLOCKED |
| `AuditLogScreen.kt` | audit_log | `AUDIT_LOG` | 0/1/0/0/0 | 1 | 3 | Audit Activity Trail | No | BLOCKED |
| `AuthScreen.kt` | auth | `AUTH` | 4/2/0/0/0 | 0 | 5 | AI Cinematic Anime Studio | Yes | BLOCKED |
| `BibleScreen.kt` | bible | `BIBLE` | 2/7/0/0/2 | 0 | 20 | Story Bible | No | BLOCKED |
| `BudgetLedgerScreen.kt` | budget_ledger | `BUDGET_LEDGER` | 1/1/0/0/0 | 0 | 6 | Budget Ledger & Reservations | No | BLOCKED |
| `CanonViewerScreen.kt` | canon | `CANON` | 6/2/1/0/4 | 3 | 10 | Canon & Facts | No | BLOCKED |
| `CharacterDeepDiveScreen.kt` | character_deep_dive | `CHARACTER_DEEP_DIVE` | 2/0/0/2/2 | 2 | 9 | Select Character | No | BLOCKED |
| `CharacterDetailScreen.kt` | character_detail | `CHARACTER_DETAIL` | 5/0/0/4/0 | 0 | 0 | Character Profile | No | BLOCKED |
| `CharacterListScreen.kt` | characters | `CHARACTERS` | 7/3/1/0/0 | 2 | 3 | No characters in this project yet. | Yes | BLOCKED |
| `ContinuityCheckerScreen.kt` | continuity | `CONTINUITY` | 3/0/0/2/0 | 2 | 5 | Continuity Checker | No | BLOCKED |
| `DatabaseScreen.kt` | database | `DATABASE` | 1/0/0/0/0 | 0 | 0 | Database Connectivity | No | BLOCKED |
| `EpisodePlannerScreen.kt` | episodes | `EPISODES` | 5/5/1/7/0 | 0 | 8 | Episode Planner | No | BLOCKED |
| `JobsMonitorScreen.kt` | jobs_monitor | `JOBS_MONITOR` | 2/0/1/0/3 | 2 | 6 | Pipeline Jobs | No | BLOCKED |
| `LoreExplorerScreen.kt` | lore_explorer | `LORE_EXPLORER` | 5/2/1/0/0 | 2 | 6 | World Wiki | No | BLOCKED |
| `MediaLibraryScreen.kt` | media_library | `MEDIA_LIBRARY` | 2/0/0/0/0 | 1 | 4 | Project Media Library | No | BLOCKED |
| `NotificationScreen.kt` | notifications | `NOTIFICATIONS` | 1/0/0/0/0 | 1 | 3 | Notifications | No | BLOCKED |
| `OwnerOverviewScreen.kt` | owner_overview | `OWNER_OVERVIEW` | 3/0/1/0/0 | 0 | 0 | Owner Overview | Yes | BLOCKED |
| `ProductionScreen.kt` | production, production/{cmdId} | `PRODUCTION` | 6/0/0/0/0 | 1 | 1 | Production Pipeline | Yes | BLOCKED |
| `ProjectDetailScreen.kt` | project_detail | `PROJECT_DETAIL` | 7/4/1/0/0 | 0 | 12 | Project Detail | Yes | BLOCKED |
| `PromptStudioScreen.kt` | prompt_studio | `PROMPT_STUDIO` | 5/2/1/0/2 | 0 | 2 | Prompt Studio | Yes | BLOCKED |
| `PublishScreen.kt` | publish | `PUBLISH` | 5/3/0/2/0 | 0 | 0 | Publish Studio | No | BLOCKED |
| `QCScreen.kt` | qc | `QC` | 3/1/0/3/0 | 0 | 1 | Quality Control (QC Gate) | No | BLOCKED |
| `QuickToolsScreen.kt` | quick_tools | `QUICK_TOOLS` | 2/3/0/0/0 | 0 | 11 | Quick Generate (Ad-Hoc Preview) | No | BLOCKED |
| `QuotaDashboardScreen.kt` | quota_dashboard | `QUOTA_DASHBOARD` | 0/0/0/0/0 | 0 | 4 | Quota Governor Dashboard (WF-12) | No | BLOCKED |
| `ReviewInboxScreen.kt` | review_inbox | `REVIEW_INBOX` | 4/1/1/0/0 | 1 | 5 | Review & Approvals | No | BLOCKED |
| `SceneDetailScreen.kt` | scenes, scenes/{epNo} | `SCENES` | 5/3/1/3/0 | 1 | 6 | Scene Inspector | No | BLOCKED |
| `SettingsScreen.kt` | settings | `SETTINGS` | 2/12/0/0/2 | 0 | 5 | Studio Infrastructure & Settings | Yes | BLOCKED |
| `StudioDashboardScreen.kt` | studio | `STUDIO` | 5/1/2/0/0 | 2 | 2 | Create Project | Yes | BLOCKED |
| `VisionAnalysisScreen.kt` | vision_analysis | `VISION_ANALYSIS` | 1/2/0/0/0 | 0 | 3 | Vision DNA Analyzer | No | BLOCKED |
| `WorkerManagerScreen.kt` | workers | `WORKERS` | 1/0/0/0/0 | 1 | 3 | Compute Worker Registry | No | BLOCKED |
| `WorkflowManagerScreen.kt` | workflows | `WORKFLOWS` | 1/0/0/0/0 | 1 | 2 | Workflow Manager (14 Frozen Workflows) | No | BLOCKED |

## Coverage totals

| Item | Count |
|---|---:|
| Screen files | 33 |
| Screen-root tags | 33 / 33 |
| Screen render tests | 33 |
| Screens with explicit control-level tests | 8 |
| Screens with smoke-only tests | 25 |
| Screens marked PASS at runtime | 0 |
| Screens marked BLOCKED | 33 |

## Per-screen behavior coverage

The 33 screen tests cover screen presence and primary title/content only. They do not prove:

- every loading/empty/error/success branch;
- every dialog open/confirm/cancel path;
- every form validation path;
- every menu/dropdown item;
- every tab;
- every list item action;
- role-gated owner/admin controls;
- tablet or compact layout differences.

Those behaviors are either covered by the shared/interaction tests, listed as control-level gaps, or marked `NOT IMPLEMENTED` in `ANDROID_BATCH_2_CONTROL_COVERAGE.md`.

## Static verification performed

- All 33 screen files contain exactly one matching `TestTags.Screen.<CONSTANT>` reference.
- `TestTags.Screen` contains 33 constants.
- Every expected title/content string used by the screen tests exists somewhere in current UI source.
- Production Kotlin compilation passes after tag insertion.
- Android test Kotlin compilation passes with the stub `R.jar` and resource tasks excluded.

## Runtime verification not performed

- Instrumentation compilation through the normal Android resource pipeline.
- Test APK assembly.
- Device/emulator installation.
- Test execution.
- Screenshot or visual comparison.
- Accessibility runtime checks.

## Final screen status

**33/33 screen tests implemented; 0 runtime PASS; 33 BLOCKED by the AAPT2/instrumentation environment.**
