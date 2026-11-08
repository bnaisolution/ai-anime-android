# Android Phase 1 — Source-Derived Test Inventory

**Status:** IMPLEMENTED (source-derived inventory)

**Baseline:** `0ddea5c9d1c5a335f349366d24c37649b2879115`

**Batch 2 note:** Batch 2 added `TestTags.Screen.<CONSTANT>` to all 33 screen files. The `Test tags` column below reflects the Phase 1 pre-Batch-2 state; see `ANDROID_BATCH_2_SCREEN_COVERAGE.md` for current screen-root tags.

**Sources inspected:**

- `app/src/main/java/org/dpdns/bnai/anime/ui/**/*Screen.kt`
- `app/src/main/java/org/dpdns/bnai/anime/ui/navigation/AppNavGraph.kt`
- `app/src/main/java/org/dpdns/bnai/anime/ui/components/SharedComponents.kt`
- `app/src/main/java/org/dpdns/bnai/anime/ui/designsystem/DesignSystem.kt`
- `app/src/main/java/org/dpdns/bnai/anime/ui/testing/TestTags.kt`

This inventory is generated from current source, not from historical screenshots or the older interaction estimate. The older `onClick` grep count is not a count of unique controls and is intentionally not used as the control inventory.

## Counts

| Item | Count |
|---|---:|
| Screen files (`*Screen.kt`) | 33 |
| `composable(...)` declarations | 37 |
| Unique routes | 35 |
| Duplicate route declarations | 2 (`auth`, `studio`) |
| Parameterized routes | 2 (`production/{cmdId}`, `scenes/{epNo}`) |
| Shared P0 component files | 3 (`SharedComponents.kt`, `DesignSystem.kt`, `TestTags.kt`) |

## Screen and route inventory

Counts below are source occurrences in each screen file. “Actions” includes `PrimaryButton`, `SecondaryButton`, `Button`, `OutlinedButton`, `TextButton`, `IconButton`, `FilledTonalButton`, `ExtendedFloatingActionButton`, and `FloatingActionButton`. “States” counts `ResourceStateBox`, `Resource.Loading`, `Resource.Error`, `Resource.Empty`, `isLoading`, and `errorMessage` references.

| Screen file | Route(s) | Actions | Fields | Dialogs | Menus | Lists | Tabs | States | Test tags |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|
| `AdminQueuesScreen.kt` | `admin_queues` | 0 | 0 | 1 | 0 | 0 | 0 | 2 | — |
| `AIBrainScreen.kt` | `ai_brain` | 3 | 1 | 0 | 0 | 1 | 0 | 1 | — |
| `AnalyticsScreen.kt` | `analytics` | 0 | 0 | 0 | 0 | 0 | 0 | 4 | — |
| `AuditLogScreen.kt` | `audit_log` | 0 | 1 | 0 | 0 | 1 | 0 | 3 | — |
| `AuthScreen.kt` | `auth` | 4 | 2 | 0 | 0 | 0 | 0 | 5 | `TestTags.Auth.EMAIL`, `TestTags.Auth.PASSWORD`, `TestTags.Auth.SUBMIT` |
| `BibleScreen.kt` | `bible` | 2 | 7 | 0 | 0 | 0 | 2 | 20 | — |
| `BudgetLedgerScreen.kt` | `budget_ledger` | 1 | 1 | 0 | 0 | 0 | 0 | 6 | — |
| `CanonViewerScreen.kt` | `canon` | 6 | 2 | 1 | 0 | 3 | 4 | 10 | — |
| `CharacterDeepDiveScreen.kt` | `character_deep_dive` | 2 | 0 | 0 | 2 | 2 | 2 | 9 | — |
| `CharacterDetailScreen.kt` | `character_detail` | 5 | 0 | 0 | 4 | 0 | 0 | 0 | — |
| `CharacterListScreen.kt` | `characters` | 7 | 3 | 1 | 0 | 2 | 0 | 3 | — |
| `ContinuityCheckerScreen.kt` | `continuity` | 3 | 0 | 0 | 2 | 2 | 0 | 5 | — |
| `DatabaseScreen.kt` | `database` | 1 | 0 | 0 | 0 | 0 | 0 | 0 | — |
| `EpisodePlannerScreen.kt` | `episodes` | 5 | 5 | 1 | 7 | 0 | 0 | 8 | — |
| `JobsMonitorScreen.kt` | `jobs_monitor` | 2 | 0 | 1 | 0 | 2 | 3 | 6 | — |
| `LoreExplorerScreen.kt` | `lore_explorer` | 5 | 2 | 1 | 0 | 2 | 0 | 6 | — |
| `MediaLibraryScreen.kt` | `media_library` | 2 | 0 | 0 | 0 | 1 | 0 | 4 | — |
| `NotificationScreen.kt` | `notifications` | 1 | 0 | 0 | 0 | 1 | 0 | 3 | — |
| `OwnerOverviewScreen.kt` | `owner_overview` | 3 | 0 | 1 | 0 | 0 | 0 | 0 | `TestTags.Dialog.CONFIRM`, `TestTags.Owner.EMERGENCY_STOP` |
| `ProductionScreen.kt` | `production`, `production/{cmdId}` | 6 | 0 | 0 | 0 | 1 | 0 | 1 | `TestTags.Production.PAUSE`, `TestTags.Production.CANCEL`, `TestTags.Production.SYNC` |
| `ProjectDetailScreen.kt` | `project_detail` | 7 | 4 | 1 | 0 | 0 | 0 | 12 | `TestTags.ProjectDetail.SAVE_SETTINGS`, `TestTags.ProjectDetail.SETTINGS_BUTTON` |
| `PromptStudioScreen.kt` | `prompt_studio` | 5 | 2 | 1 | 0 | 0 | 2 | 2 | `TestTags.item(TestTags.PromptStudio.TAB_PREFIX, cat)`, `TestTags.PromptStudio.EDIT`, `TestTags.PromptStudio.SAVE`, `TestTags.PromptStudio.RESET` |
| `PublishScreen.kt` | `publish` | 5 | 3 | 0 | 2 | 0 | 0 | 0 | — |
| `QCScreen.kt` | `qc` | 3 | 1 | 0 | 3 | 0 | 0 | 1 | — |
| `QuickToolsScreen.kt` | `quick_tools` | 2 | 3 | 0 | 0 | 0 | 0 | 11 | — |
| `QuotaDashboardScreen.kt` | `quota_dashboard` | 0 | 0 | 0 | 0 | 0 | 0 | 4 | — |
| `ReviewInboxScreen.kt` | `review_inbox` | 4 | 1 | 1 | 0 | 1 | 0 | 5 | `TestTags.Review.APPROVE`, `TestTags.Review.REJECT` |
| `SceneDetailScreen.kt` | `scenes`, `scenes/{epNo}` | 5 | 3 | 1 | 3 | 1 | 0 | 6 | — |
| `SettingsScreen.kt` | `settings` | 2 | 12 | 0 | 0 | 0 | 2 | 5 | `TestTags.Settings.SAVE` |
| `StudioDashboardScreen.kt` | `studio` | 5 | 1 | 2 | 0 | 2 | 0 | 2 | `TestTags.Studio.CREATE_PROJECT`, `TestTags.Studio.PROJECT_LIST`, `TestTags.item(TestTags.Studio.PROJECT_ITEM_PREFIX, project.id)` |
| `VisionAnalysisScreen.kt` | `vision_analysis` | 1 | 2 | 0 | 0 | 0 | 0 | 3 | — |
| `WorkerManagerScreen.kt` | `workers` | 1 | 0 | 0 | 0 | 1 | 0 | 3 | — |
| `WorkflowManagerScreen.kt` | `workflows` | 1 | 0 | 0 | 0 | 1 | 0 | 2 | — |

## Shared P0 component tags

| Component | Tags |
|---|---|
| `ResourceStateBox` | `state.loading`, `state.empty`, `state.error`, `state.retry` |
| `ConfirmationDialog` | `dialog.confirm`, `dialog.cancel` |
| `UnsavedChangesDialog` | `dialog.unsaved.discard`, `dialog.unsaved.keep_editing` |
| `ProjectSelectorBar` | `nav.project_selector`, `nav.project_refresh` |
| `GlobalAiDirectorBottomSheet` | `ai_director.sheet`, `ai_director.send` |
| Navigation shell | `nav.drawer`, `nav.ai_director_fab` |
| Owner emergency stop | `screen.owner.emergency_stop` |

## New-screen and new-route guard

`scripts/check_test_inventory.py` compares current source against this document.

Run:

```bash
python3 scripts/check_test_inventory.py
```

The check fails when:

- a `*Screen.kt` file has no matching row in this inventory, or
- an `AppNavGraph.kt` `composable("...")` route is absent from the route list in this document.

This is intentionally lightweight and does not require Android execution.

## Limitations

- The table records source occurrences, not runtime-visible unique controls.
- A control that appears inside a loop or reusable composable may render multiple times at runtime.
- Non-P0 screens currently have no dedicated test tags; the inventory records their controls and async states so later phases can add targeted tests without re-auditing the whole app.
