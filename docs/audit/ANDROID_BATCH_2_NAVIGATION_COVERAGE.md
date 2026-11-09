# Android Batch 2 — Navigation Coverage

**Status:** SUPERSEDED — 35/35 ROUTES PASSED IN CLOUD RUN `42577400016`

**Baseline:** `0ddea5c9d1c5a335f349366d24c37649b2879115`

**Post-run note (2026-11-09):** The route table below is the pre-run snapshot. Final cloud run `42577400016` passed all 35 route/navigation tests as part of the 116/116 instrumentation result.

## Method

Navigation coverage is derived from `app/src/main/java/org/dpdns/bnai/anime/ui/navigation/AppNavGraph.kt` and asserted in `Batch2NavigationTest.kt`. The test file covers:

- auth start destination without token;
- studio start destination with token;
- drawer open and workspace items;
- bottom navigation to Episodes;
- back navigation to Studio;
- parameterized production route;
- parameterized scenes route;
- invalid scene argument fallback;
- duplicate `auth` and `studio` declarations;
- a loop over all 35 unique routes.

Each route assertion checks the destination's screen-root tag. No route is marked `PASS` because instrumentation execution is blocked.

## Summary

| Item | Count |
|---|---:|
| Unique routes | 35 |
| Routes with test source | 35 |
| Duplicate route declarations | 2 (`auth`, `studio`) |
| Parameterized routes | 2 (`production/{cmdId}`, `scenes/{epNo}`) |
| Navigation tests | 11 |
| Runtime navigation PASS | 0 |
| Runtime navigation BLOCKED | 35 |

## Route matrix

`Loop` means the route is included in `everyRegisteredRouteOpensItsExpectedScreen`. `Specific` means a dedicated test also targets it.

| # | Route | Expected screen | Test source | Loop | Specific | Status |
|---:|---|---|---|---|---|---|
| 1 | `auth` | Auth | `authStartDestinationWithoutToken`, `duplicateAuthDeclarationIsReachableFromAuthenticatedGraph` | Yes | Yes | BLOCKED |
| 2 | `studio` | Studio dashboard | `studioStartDestinationWithToken`, `duplicateStudioDeclarationIsReachableFromAuthGraph` | Yes | Yes | BLOCKED |
| 3 | `project_detail` | Project detail | `drawerOpensAndExposesWorkspaceNavigation` | Yes | Yes | BLOCKED |
| 4 | `bible` | Story Bible | `backNavigationReturnsToStudio` path | Yes | No | BLOCKED |
| 5 | `characters` | Character list | route loop | Yes | No | BLOCKED |
| 6 | `character_detail` | Character detail | route loop | Yes | No | BLOCKED |
| 7 | `character_deep_dive` | Character deep dive | route loop | Yes | No | BLOCKED |
| 8 | `episodes` | Episode planner | `bottomNavigationOpensEpisodes`, `backNavigationReturnsToStudio` | Yes | Yes | BLOCKED |
| 9 | `production` | Production | route loop | Yes | No | BLOCKED |
| 10 | `production/{cmdId}` | Production | `parameterizedProductionRouteOpensProductionScreen` | Yes | Yes | BLOCKED |
| 11 | `scenes` | Scene inspector | route loop | Yes | No | BLOCKED |
| 12 | `scenes/{epNo}` | Scene inspector | `parameterizedScenesRouteOpensSceneInspector`, `invalidSceneArgumentFallsBackToEpisodeOne` | Yes | Yes | BLOCKED |
| 13 | `qc` | QC gate | route loop | Yes | No | BLOCKED |
| 14 | `publish` | Publish studio | route loop | Yes | No | BLOCKED |
| 15 | `review_inbox` | Review inbox | route loop | Yes | No | BLOCKED |
| 16 | `ai_brain` | AI Brain | route loop | Yes | No | BLOCKED |
| 17 | `quick_tools` | Quick tools | route loop | Yes | No | BLOCKED |
| 18 | `settings` | Settings | route loop | Yes | No | BLOCKED |
| 19 | `prompt_studio` | Prompt studio | route loop | Yes | No | BLOCKED |
| 20 | `owner_overview` | Owner overview | route loop | Yes | No | BLOCKED |
| 21 | `workflows` | Workflow manager | route loop | Yes | No | BLOCKED |
| 22 | `workers` | Worker manager | route loop | Yes | No | BLOCKED |
| 23 | `database` | Database | route loop | Yes | No | BLOCKED |
| 24 | `audit_log` | Audit log | route loop | Yes | No | BLOCKED |
| 25 | `analytics` | Analytics | route loop | Yes | No | BLOCKED |
| 26 | `media_library` | Media library | route loop | Yes | No | BLOCKED |
| 27 | `notifications` | Notifications | route loop | Yes | No | BLOCKED |
| 28 | `vision_analysis` | Vision analysis | route loop | Yes | No | BLOCKED |
| 29 | `jobs_monitor` | Jobs monitor | route loop | Yes | No | BLOCKED |
| 30 | `quota_dashboard` | Quota dashboard | route loop | Yes | No | BLOCKED |
| 31 | `canon` | Canon viewer | route loop | Yes | No | BLOCKED |
| 32 | `continuity` | Continuity checker | route loop | Yes | No | BLOCKED |
| 33 | `lore_explorer` | Lore explorer | route loop | Yes | No | BLOCKED |
| 34 | `admin_queues` | Admin queues | route loop | Yes | No | BLOCKED |
| 35 | `budget_ledger` | Budget ledger | route loop | Yes | No | BLOCKED |

## Duplicate route declarations

`AppNavGraph.kt` declares `auth` and `studio` twice: once in the unauthenticated graph and once in the authenticated graph.

| Duplicate route | Declarations | Test coverage | Status |
|---|---|---|---|
| `auth` | Lines 158 and 401 | `authStartDestinationWithoutToken`; `duplicateAuthDeclarationIsReachableFromAuthenticatedGraph`; route loop | BLOCKED |
| `studio` | Lines 159 and 402 | `studioStartDestinationWithToken`; `duplicateStudioDeclarationIsReachableFromAuthGraph`; route loop | BLOCKED |

## Parameterized routes

| Route | Argument handling | Test coverage | Status |
|---|---|---|---|
| `production/{cmdId}` | `cmdId` passed to `ProductionScreen` | `parameterizedProductionRouteOpensProductionScreen`; route loop with `production/cmd-1` | BLOCKED |
| `scenes/{epNo}` | `epNo` parsed with fallback to 1 | `parameterizedScenesRouteOpensSceneInspector`; `invalidSceneArgumentFallsBackToEpisodeOne`; route loop with `scenes/3` | BLOCKED |

## Navigation behaviors not runtime-verified

- Start destination selection after token restore from real storage.
- Drawer item click behavior for every drawer item.
- Bottom navigation selected-state semantics.
- Back-stack preservation after deep links.
- Up/back behavior from dialogs and bottom sheets.
- Role-gated drawer visibility for owner vs non-owner.
- Tablet `NavigationRail` behavior.
- Deep links and external intents.
- Process death and state restoration.
- Navigation after repository errors or auth expiry.

## Static verification performed

- `AppNavGraph.kt` contains 37 `composable(...)` declarations across 35 unique routes.
- All 35 unique routes are present in the test loop.
- Each route maps to a screen-root tag added in Batch 2.
- Duplicate and parameterized routes have dedicated test source.
- Android test Kotlin compilation passes with the stub `R.jar` and resource tasks excluded.

## Runtime verification not performed

- Instrumentation compilation through the normal Android resource pipeline.
- Test APK assembly.
- Device/emulator installation.
- Navigation execution.
- Back-stack assertions.
- Screenshot/visual checks.

## Final navigation status

**35/35 route/navigation tests passed in cloud run `42577400016`.**
