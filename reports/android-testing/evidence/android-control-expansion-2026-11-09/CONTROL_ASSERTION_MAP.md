# Android Control-Level Expansion — Assertion Map

**Date:** 2026-11-09
**Source:** `app/src/androidTest/java/org/dpdns/bnai/anime/ui/batch2/Batch2FullControlCoverageTest.kt`
**Execution status:** TESTED AND PASSED — 35/35 tests passed in cloud run `42577400016` at commit `44bfa15c5e23d09e51a2d3c681c9cca3b219f844`; full instrumentation job 116/116.
**Inventory basis:** corrected source-derived total of 205 interactive controls (actions, fields, dialogs, menus, tabs).

Each test first waits for its screen-root `TestTags` entry; this map then lists the labels asserted or clicked by that test method. Labels are matched against screen controls in `docs/audit/ANDROID_PHASE_1_TEST_INVENTORY.md`; credit is valid because the tests executed and passed in cloud CI. Because one control can expose multiple labels/states, the 280 label occurrences are not a one-to-one 205/205 assertion count.

| Test method | Screen | Labels asserted/clicked | Status |
|---|---|---|---|
| `budgetLedgerCoversControls` | `BudgetLedgerScreen` | `Budget Ceiling`<br>`Consumed`<br>`Reserved (Active)`<br>`Released`<br>`Budget Ceiling (Credits)`<br>`Save Budget Ceiling` | TESTED AND PASSED |
| `canonCoversControls` | `CanonViewerScreen` | `Add Canon Fact`<br>`Commit Canon`<br>`Locked Facts`<br>`Pending Proposals`<br>`Search canon facts or proposals...`<br>`Immutable Story / Character Fact`<br>`Commit Fact`<br>`Cancel`<br>`✅ Approve`<br>`❌ Reject`<br>`The sky is red`<br>`➕ Add Canon Fact`<br>`Canon Change Proposal` | TESTED AND PASSED |
| `characterDeepDiveCoversControls` | `CharacterDeepDiveScreen` | `Aki`<br>`New Ref`<br>`Visual DNA (11)`<br>`Knowledge & Arc`<br>`Powers & Relations`<br>`Voice & Ref History`<br>`Code: CHAR_001` | TESTED AND PASSED |
| `continuityCoversControls` | `ContinuityCheckerScreen` | `Entire Project`<br>`Run Continuity Scan (Entire Project)`<br>`Critical`<br>`Warnings`<br>`Passed`<br>`Fix with AI`<br>`Episode 2`<br>`Episode 3`<br>`Run Continuity Scan (Episode 1)`<br>`Dead character speaks`<br>`Episode 1` | TESTED AND PASSED |
| `jobsMonitorCoversControls` | `JobsMonitorScreen` | `Active Jobs`<br>`Retries`<br>`1 Pending Actions`<br>`Force Retry`<br>`Trigger Immediate Retry`<br>`Pipeline Job Inspector`<br>`SCENE RENDER` | TESTED AND PASSED |
| `loreCoversControls` | `LoreExplorerScreen` | `Add via AI`<br>`AI Edit`<br>`Filter Mysteries...`<br>`Generate Mysteries with AI`<br>`Describe new Mysteries entry`<br>`Generate & Save`<br>`Cancel`<br>`World Wiki`<br>`✨ Add / Edit World Wiki via AI` | TESTED AND PASSED |
| `visionCoversControls` | `VisionAnalysisScreen` | `QUICK Preview (Zero DB Writes)`<br>`SAVE to Character & R4 History`<br>`Source Image / Reference URL (https://...)`<br>`Target Character Code (e.g. CHAR_001)`<br>`Start Vision DNA Analysis` | TESTED AND PASSED |
| `authCoversControls` | `AuthScreen` | `Studio Email`<br>`Password`<br>`Show`<br>`Sign In to Studio`<br>`Create an account`<br>`Forgot Password?`<br>`Hide` | TESTED AND PASSED |
| `aiBrainEmptyCoversControls` | `AIBrainScreen` | `Ask AI Director about this project...`<br>`Audit Story Bible`<br>`Check Continuity`<br>`Plan Next Episode`<br>`Send` | TESTED AND PASSED |
| `aiBrainConversationCoversControls` | `AIBrainScreen` | `Confirm & Execute`<br>`Run episode`<br>`Clear project chat` | TESTED AND PASSED |
| `characterDetailCoversControls` | `CharacterDetailScreen` | `Voice Engine: Svara TTS`<br>`Voice Model ID: svara_hi_female_lead_01`<br>`Preview Voice`<br>`Save Voice`<br>`Open Character Deep Dive (11-Field DNA, Arc, Powers & Relations)`<br>`Edge TTS Fallback`<br>`supertonic_en_narrator_01`<br>`Supertonic TTS`<br>`supertonic_ja_villain_01` | TESTED AND PASSED |
| `characterListCoversControls` | `CharacterListScreen` | `Search characters by name or role...`<br>`Aki`<br>`ALIVE`<br>`Deep Dive`<br>`Add Character`<br>`Character Name`<br>`Role (Protagonist / Antagonist / Support)`<br>`Create & Gen Image`<br>`Cancel`<br>`Cast (1)`<br>`➕ Add Character & Auto-Portrait`<br>`Preview Voice` | TESTED AND PASSED |
| `episodePlannerCoversControls` | `EpisodePlannerScreen` | `Select Episode`<br>`Scenes`<br>`Director Prompt Override`<br>`Episode Title`<br>`Episode Synopsis`<br>`Episode Script / Narrative Outline`<br>`Save Script & Metadata (No Generation)`<br>`Generate Episode`<br>`Regenerate...`<br>`Episode 2: New Episode`<br>`Use Project Default Episode Prompt`<br>`Preview Prompt`<br>`Regenerate Episode`<br>`Cancel`<br>`Episode 1: Pilot`<br>`Batch Prompt [BIBLE]`<br>`Regenerate Episode 2?` | TESTED AND PASSED |
| `mediaLibraryCoversControls` | `MediaLibraryScreen` | `ALL`<br>`CLIPS`<br>`CHARACTERS`<br>`SCENES`<br>`Copy URL`<br>`Open Stream`<br>`Scene #1 Video Clip` | TESTED AND PASSED |
| `notificationsCoversControls` | `NotificationScreen` | `Mark All Read`<br>`Episode 1 rendered`<br>`Render complete` | TESTED AND PASSED |
| `adminQueuesCoversControls` | `AdminQueuesScreen` | `Off`<br>`5s`<br>`10s`<br>`30s`<br>`Transactional Outbox Queue`<br>`Active Pipeline Commands`<br>`Failed / Dead-Letter Queue`<br>`Got it`<br>`3 Queued Events`<br>`Transactional Outbox Queue — Queue Inspector`<br>`Info about Pipeline Queues` | TESTED AND PASSED |
| `auditLogCoversControls` | `AuditLogScreen` | `Filter audit events by action or state...` | TESTED AND PASSED |
| `databaseCoversControls` | `DatabaseScreen` | `Test Connection Now` | TESTED AND PASSED |
| `ownerOverviewCoversControls` | `OwnerOverviewScreen` | `🚨 Emergency Stop All GPU Workers`<br>`Halt Fleet Now`<br>`Cancel`<br>`🚨 Emergency Stop All Workers?` | TESTED AND PASSED |
| `workerManagerCoversControls` | `WorkerManagerScreen` | `Refresh`<br>`KAGGLE (#worker-1)` | TESTED AND PASSED |
| `workflowManagerCoversControls` | `WorkflowManagerScreen` | `Refresh` | TESTED AND PASSED |
| `productionCoversControls` | `ProductionScreen` | `Kaggle GPU Pool`<br>`View Final Video & Quality Check`<br>`Pause`<br>`Cancel`<br>`Sync`<br>`Scene Inspector`<br>`Media Library` | TESTED AND PASSED |
| `sceneDetailCoversControls` | `SceneDetailScreen` | `Episode 1`<br>`Edit (PATCH)`<br>`Regenerate`<br>`Open Rendered Clip Stream`<br>`Scene Description / Action`<br>`Visual Prompt`<br>`Save Scene (PATCH)`<br>`Cancel`<br>`Scene #1`<br>`Edit Scene #1` | TESTED AND PASSED |
| `bibleCoversControls` | `BibleScreen` | `Overview & Tone`<br>`World & Rules`<br>`Power System`<br>`Characters & Timeline`<br>`Master Premise / Logline`<br>`Tone & Genre`<br>`Save Bible Draft`<br>`AI Expand Bible`<br>`World Setting & Geography`<br>`Immutable World Rules (Enforced by Canon)`<br>`Power System Rules, Costs & Ceilings`<br>`Core Cast Foundations & Factions`<br>`Timeline & Season Arc Structure` | TESTED AND PASSED |
| `projectDetailCoversControls` | `ProjectDetailScreen` | `Canon Status`<br>`Continuity Check`<br>`Episodes`<br>`Scenes`<br>`Characters`<br>`Active Jobs`<br>`Pending Approvals`<br>`Queued Actions`<br>`Generate Next Episode`<br>`Story Bible`<br>`Episode Planner`<br>`Scene Inspector`<br>`Character Deep Dive`<br>`World Wiki`<br>`Vision DNA`<br>`Quick Generate`<br>`Media Library`<br>`Quality Control (QC)`<br>`Review Inbox`<br>`Publish Studio`<br>`Analytics`<br>`Quota & Budget`<br>`Project Title`<br>`Genre`<br>`Visual Medium (ANIME / MANHWA / 3D)`<br>`Cover Image URL (https://...)`<br>`Save Settings`<br>`Cancel`<br>`Project Settings`<br>`Edit Project Settings` | TESTED AND PASSED |
| `projectDetailActiveJobCoversConditionalControl` | `ProjectDetailScreen` | `Open Active Job in Pipeline Monitor` | TESTED AND PASSED |
| `publishCoversControls` | `PublishScreen` | `Viral Auto-Fill`<br>`YouTube / Shorts`<br>`Connected`<br>`Instagram Reels`<br>`Connect Insta`<br>`Release Title`<br>`Viral Description`<br>`Viral Tags`<br>`Visibility:`<br>`Public`<br>`Publish to Selected Platforms`<br>`Unlisted`<br>`Private`<br>`Ep 1: Pilot` | TESTED AND PASSED |
| `qcCoversControls` | `QCScreen` | `Select Episode for QC`<br>`Re-Run Automated QC`<br>`Approve QC Gate`<br>`Proceed to Publish Studio`<br>`Episode 2`<br>`Episode 3`<br>`Episode 1: Pilot` | TESTED AND PASSED |
| `reviewInboxCoversControls` | `ReviewInboxScreen` | `✅ Approve`<br>`❌ Reject`<br>`Reason for rejection`<br>`Confirm Reject`<br>`Cancel`<br>`Reject Approval` | TESTED AND PASSED |
| `settingsCoversControls` | `SettingsScreen` | `AI & Vision`<br>`Compute Workers`<br>`Storage & Policy`<br>`Session`<br>`Primary LLM Provider (groq / gemini / openrouter)`<br>`Fallback LLM Provider`<br>`Vision DNA Provider (gemini / groq / openrouter)`<br>`Groq API Key`<br>`Gemini API Key`<br>`Save & Verify Configuration`<br>`Preferred Compute Mode`<br>`Kaggle Username`<br>`Kaggle API Key`<br>`GitHub CPU Worker Repository`<br>`GitHub Actions Token (Masked)`<br>`Telegram Channel / Chat ID`<br>`Telegram Bot Token (Masked)`<br>`Test Backend Connection` | TESTED AND PASSED |
| `promptStudioCoversControls` | `PromptStudioScreen` | `BIBLE`<br>`EPISODE`<br>`IMAGE`<br>`AI_DIRECTOR`<br>`Enter Edit Mode`<br>`Prompt Title (BIBLE)`<br>`System Prompt Instructions (BIBLE)`<br>`Reset BIBLE to Application Default`<br>`Reset to Default`<br>`Cancel`<br>`Save BIBLE`<br>`Batch Prompt`<br>`Reset BIBLE Prompt to Default?`<br>`Delete prompt` | TESTED AND PASSED |
| `studioDashboardCoversControls` | `StudioDashboardScreen` | `Create Project`<br>`Production State`<br>`Pending Approvals`<br>`Pipeline Status Details`<br>`Close`<br>`Project Title`<br>`Create`<br>`Cancel`<br>`Projects (1)`<br>`Refresh Projects` | TESTED AND PASSED |
| `quickToolsSaveCoversControls` | `QuickToolsScreen` | `Image Generation`<br>`Video Clip Generation`<br>`Visual Prompt`<br>`Style Preset`<br>`Aspect Ratio`<br>`Generate Ad-Hoc Image`<br>`Save Asset to Project` | TESTED AND PASSED |
| `quickToolsCancelCoversConditionalControl` | `QuickToolsScreen` | `Cancel Remote Job`<br>`Generate Ad-Hoc Image` | TESTED AND PASSED |
| `existingSaveButtonIsEnabledInPromptStudio` | `PromptStudioScreen` |  | TESTED AND PASSED |

## Mapping notes

- `assertText` / `assertDesc` prove the control is exposed in the semantics tree.
- `clickText` / `clickClickableText` / `clickDesc` / `clickTag` prove the control can be activated.
- `scrollToText` exercises list scrolling so off-screen controls are composed before assertion.
- Dialog assertions are mapped to the corresponding dialog control rows; menu-item clicks are mapped to menu controls; tab clicks are mapped to tab controls.
- The map records the executed test intent and labels; runtime pass is evidenced by `reports/android-testing/evidence/ci-42577400016/`.
