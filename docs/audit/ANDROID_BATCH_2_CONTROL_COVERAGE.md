# Android Batch 2 — Control Coverage

**Status:** PARTIALLY IMPLEMENTED — 27 OF 206 SCREEN CONTROLS EXPLICITLY ASSERTED; 179 NOT INDIVIDUALLY ASSERTED; EXECUTION BLOCKED

**Baseline:** `0ddea5c9d1c5a335f349366d24c37649b2879115`

## Method

Controls are counted from the source-derived inventory in `docs/audit/ANDROID_PHASE_1_TEST_INVENTORY.md`, not from raw `onClick` grep counts. The inventory counts:

- Actions: `PrimaryButton`, `SecondaryButton`, `Button`, `OutlinedButton`, `TextButton`, `IconButton`, `FilledTonalButton`, `ExtendedFloatingActionButton`, `FloatingActionButton`.
- Fields: `OutlinedTextField`, `TextField`, and related input controls.
- Dialogs: `AlertDialog`, `ConfirmationDialog`, `ModalBottomSheet` usage.
- Menus: dropdown menus and menu items.
- Tabs: `Tab`, `TabRow`, `ScrollableTabRow`.
- Lists and state references are reported separately and are not included in the 206 interactive-control total.

A control is marked `BLOCKED` when a control-level assertion exists but cannot run because instrumentation is blocked. A control is marked `NOT IMPLEMENTED` when no control-level assertion exists and only the screen smoke test is present.

## Totals

| Control type | Count |
|---|---:|
| Actions | 99 |
| Fields | 56 |
| Dialogs | 13 |
| Menus | 23 |
| Tabs | 15 |
| **Interactive controls (actions + fields + dialogs + menus + tabs)** | **206** |
| Explicitly asserted screen controls | 27 |
| Not individually asserted screen controls | 179 |
| List surfaces (separate) | 24 |
| State references (separate) | 152 |

## Status summary

| Status | Controls | Meaning |
|---|---:|---|
| PASS | 0 | No runtime UI execution occurred. |
| FAIL | 0 | No runtime UI execution occurred. |
| BLOCKED | 27 | Explicit test source exists; instrumentation execution blocked. |
| NOT IMPLEMENTED | 179 | Screen smoke only; no control-level assertion. |

## Explicitly asserted screen controls

| Screen | Control | Test source | Status |
|---|---|---|---|
| Auth | Email field | P0 semantics + screen test | BLOCKED |
| Auth | Password field | P0 semantics + screen test | BLOCKED |
| Auth | Submit | P0 semantics + interaction test | BLOCKED |
| Studio dashboard | Create project FAB | Interaction test | BLOCKED |
| Studio dashboard | Project name field | Control behavior test | BLOCKED |
| Studio dashboard | Create confirm | Control behavior test | BLOCKED |
| Settings | Compute Workers tab | Interaction test | BLOCKED |
| Settings | Save & verify | Control behavior test | BLOCKED |
| Prompt Studio | EPISODE category tab | Interaction test | BLOCKED |
| Prompt Studio | Enter edit mode | Control behavior test | BLOCKED |
| Prompt Studio | Prompt text field | Control behavior test | BLOCKED |
| Prompt Studio | Save category prompt | Control behavior test | BLOCKED |
| Production | Pause | Interaction test tag assertion | BLOCKED |
| Production | Cancel | Interaction test tag assertion + behavior test | BLOCKED |
| Production | Sync | Interaction test tag assertion | BLOCKED |
| Project detail | Edit project settings | Interaction test | BLOCKED |
| Project detail | Save settings | Control behavior test | BLOCKED |
| Owner overview | Emergency stop | Interaction test | BLOCKED |
| Character list | Add Character | Interaction test | BLOCKED |
| Character list | Character name field | Control behavior test | BLOCKED |
| Character list | Create character confirm | Control behavior test | BLOCKED |
| Review inbox | Approve | Control behavior test | BLOCKED |
| Review inbox | Reject | Control behavior test | BLOCKED |
| Review inbox | Reject reason field | Control behavior test | BLOCKED |
| Review inbox | Confirm reject | Control behavior test | BLOCKED |
| Bible | Save Bible draft | Control behavior test | BLOCKED |
| Episode planner | Save script & metadata | Control behavior test | BLOCKED |

## Shared component controls

These are not included in the 206 screen-control total.

| Component | Control | Assertion | Status |
|---|---|---|---|
| `ResourceStateBox` | Loading state | Tag `state.loading` | BLOCKED |
| `ResourceStateBox` | Empty state | Tag `state.empty` | BLOCKED |
| `ResourceStateBox` | Error state | Tag `state.error` | BLOCKED |
| `ResourceStateBox` | Success content | Content text | BLOCKED |
| `ResourceStateBox` | Retry action | Click + content description `Retry` | BLOCKED |
| `ConfirmationDialog` | Confirm | Tag `dialog.confirm` + click | BLOCKED |
| `ConfirmationDialog` | Cancel | Tag `dialog.cancel` + click | BLOCKED |
| `UnsavedChangesDialog` | Discard | Tag `dialog.unsaved.discard` | BLOCKED |
| `UnsavedChangesDialog` | Keep editing | Tag `dialog.unsaved.keep_editing` | BLOCKED |
| `ProjectSelectorBar` | Selector | Tag `nav.project_selector` | BLOCKED |
| `ProjectSelectorBar` | Refresh | Content description `Refresh project data` | BLOCKED |
| Navigation shell | Drawer / back / AI Director FAB | Content descriptions asserted | BLOCKED |

## Per-screen control matrix

`Explicit` is the number of screen controls with a control-level assertion. `Unasserted` is the remainder. The status columns apply to every control counted in that row.

| Screen file | Actions | Fields | Dialogs | Menus | Tabs | Total | Explicit | Explicit status | Unasserted | Unasserted status |
|---|---:|---:|---:|---:|---:|---:|---:|---|---:|---|
| `AdminQueuesScreen.kt` | 0 | 0 | 1 | 0 | 0 | 1 | 0 | BLOCKED | 1 | NOT IMPLEMENTED |
| `AIBrainScreen.kt` | 3 | 1 | 0 | 0 | 0 | 4 | 0 | BLOCKED | 4 | NOT IMPLEMENTED |
| `AnalyticsScreen.kt` | 0 | 0 | 0 | 0 | 0 | 0 | 0 | BLOCKED | 0 | NOT IMPLEMENTED |
| `AuditLogScreen.kt` | 0 | 1 | 0 | 0 | 0 | 1 | 0 | BLOCKED | 1 | NOT IMPLEMENTED |
| `AuthScreen.kt` | 4 | 2 | 0 | 0 | 0 | 6 | 3 | BLOCKED | 3 | NOT IMPLEMENTED |
| `BibleScreen.kt` | 2 | 7 | 0 | 0 | 2 | 11 | 1 | BLOCKED | 10 | NOT IMPLEMENTED |
| `BudgetLedgerScreen.kt` | 1 | 1 | 0 | 0 | 0 | 2 | 0 | BLOCKED | 2 | NOT IMPLEMENTED |
| `CanonViewerScreen.kt` | 6 | 2 | 1 | 0 | 4 | 13 | 0 | BLOCKED | 13 | NOT IMPLEMENTED |
| `CharacterDeepDiveScreen.kt` | 2 | 0 | 0 | 2 | 2 | 6 | 0 | BLOCKED | 6 | NOT IMPLEMENTED |
| `CharacterDetailScreen.kt` | 5 | 0 | 0 | 4 | 0 | 9 | 0 | BLOCKED | 9 | NOT IMPLEMENTED |
| `CharacterListScreen.kt` | 7 | 3 | 1 | 0 | 0 | 11 | 3 | BLOCKED | 8 | NOT IMPLEMENTED |
| `ContinuityCheckerScreen.kt` | 3 | 0 | 0 | 2 | 0 | 5 | 0 | BLOCKED | 5 | NOT IMPLEMENTED |
| `DatabaseScreen.kt` | 1 | 0 | 0 | 0 | 0 | 1 | 0 | BLOCKED | 1 | NOT IMPLEMENTED |
| `EpisodePlannerScreen.kt` | 5 | 5 | 1 | 7 | 0 | 18 | 1 | BLOCKED | 17 | NOT IMPLEMENTED |
| `JobsMonitorScreen.kt` | 2 | 0 | 1 | 0 | 3 | 6 | 0 | BLOCKED | 6 | NOT IMPLEMENTED |
| `LoreExplorerScreen.kt` | 5 | 2 | 1 | 0 | 0 | 8 | 0 | BLOCKED | 8 | NOT IMPLEMENTED |
| `MediaLibraryScreen.kt` | 2 | 0 | 0 | 0 | 0 | 2 | 0 | BLOCKED | 2 | NOT IMPLEMENTED |
| `NotificationScreen.kt` | 1 | 0 | 0 | 0 | 0 | 1 | 0 | BLOCKED | 1 | NOT IMPLEMENTED |
| `OwnerOverviewScreen.kt` | 3 | 0 | 1 | 0 | 0 | 4 | 1 | BLOCKED | 3 | NOT IMPLEMENTED |
| `ProductionScreen.kt` | 6 | 0 | 0 | 0 | 0 | 6 | 3 | BLOCKED | 3 | NOT IMPLEMENTED |
| `ProjectDetailScreen.kt` | 7 | 4 | 1 | 0 | 0 | 12 | 2 | BLOCKED | 10 | NOT IMPLEMENTED |
| `PromptStudioScreen.kt` | 5 | 2 | 1 | 0 | 2 | 10 | 4 | BLOCKED | 6 | NOT IMPLEMENTED |
| `PublishScreen.kt` | 5 | 3 | 0 | 2 | 0 | 10 | 0 | BLOCKED | 10 | NOT IMPLEMENTED |
| `QCScreen.kt` | 3 | 1 | 0 | 3 | 0 | 7 | 0 | BLOCKED | 7 | NOT IMPLEMENTED |
| `QuickToolsScreen.kt` | 2 | 3 | 0 | 0 | 0 | 5 | 0 | BLOCKED | 5 | NOT IMPLEMENTED |
| `QuotaDashboardScreen.kt` | 0 | 0 | 0 | 0 | 0 | 0 | 0 | BLOCKED | 0 | NOT IMPLEMENTED |
| `ReviewInboxScreen.kt` | 4 | 1 | 1 | 0 | 0 | 6 | 4 | BLOCKED | 2 | NOT IMPLEMENTED |
| `SceneDetailScreen.kt` | 5 | 3 | 1 | 3 | 0 | 12 | 0 | BLOCKED | 12 | NOT IMPLEMENTED |
| `SettingsScreen.kt` | 2 | 12 | 0 | 0 | 2 | 16 | 2 | BLOCKED | 14 | NOT IMPLEMENTED |
| `StudioDashboardScreen.kt` | 5 | 1 | 2 | 0 | 0 | 8 | 3 | BLOCKED | 5 | NOT IMPLEMENTED |
| `VisionAnalysisScreen.kt` | 1 | 2 | 0 | 0 | 0 | 3 | 0 | BLOCKED | 3 | NOT IMPLEMENTED |
| `WorkerManagerScreen.kt` | 1 | 0 | 0 | 0 | 0 | 1 | 0 | BLOCKED | 1 | NOT IMPLEMENTED |
| `WorkflowManagerScreen.kt` | 1 | 0 | 0 | 0 | 0 | 1 | 0 | BLOCKED | 1 | NOT IMPLEMENTED |
| **Total** | **99** | **56** | **13** | **23** | **15** | **206** | **27** | **BLOCKED** | **179** | **NOT IMPLEMENTED** |

## Notable control gaps

- Owner/admin-only controls are not exercised with owner and non-owner roles separately.
- Dialog confirm actions are not executed except for shared `ConfirmationDialog` test source; they are blocked from runtime execution.
- Destructive actions such as emergency stop, delete prompt, and character deletion are not runtime-tested.
- Form validation is only asserted for empty Auth submission; other forms are not validated.
- Dropdown and menu item selection is not asserted.
- Tabs are asserted for Settings and Prompt Studio only.
- List item click and long-press behavior is not asserted.
- Disabled/loading button behavior is not asserted.
- No control test verifies that an action actually mutates repository state, because the fake repository returns `Resource.Empty` for most methods.
- No tablet/rail layout control coverage.

## Final control status

**27 controls have explicit test source but are BLOCKED from execution; 179 controls are NOT IMPLEMENTED at control level. No control is marked PASS or FAIL.**
