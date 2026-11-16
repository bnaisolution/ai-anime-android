# Hybrid E2E Master Plan

Status: APPROVED (execution not started until Phase 0 gate)
Last updated: 2026-11-16

## Goal

Prove the real Android-to-backend-to-n8n-to-worker journey:

project create -> settings -> Story Bible -> seasons -> Prompt Studio -> characters -> scenes -> episode generation -> job/media.

Android UI runs only on GitHub Actions emulator + Maestro. No local Android build/test.

## Locked Decisions

1. Environment: staging / dedicated test account.
2. Generation: real generation only, no stubs.
3. Cleanup:
   - Happy-path data may be deleted by cleanup script after the run passes.
   - Failed or negative-case data must be preserved for debugging and gap evidence.
   - Preserved data is deleted manually only after approval.
4. Free infrastructure:
   - CPU worker: Savera TTS + FFmpeg.
   - Kaggle: free tier, 39 hours/week.
   - Storage: Telegram, free.

## Execution Model

Each screen follows this loop:

1. Screen charter.
2. Test fixture.
3. Maestro UI flow.
4. Backend API assertion.
5. Negative/edge case.
6. Evidence capture.
7. Gap register update.

Each phase has a gate. A gate passes only when:
- Happy path is green.
- API state matches UI state.
- Negative cases pass.
- Evidence is attached.
- No open P0 gap.

## Phases, Screens, Gates

| Phase | Screens | Gate |
|---|---|---|
| A. Foundation | S0 Auth/Launch, harness, GHA workflow | G-A |
| B. Project Core | S1 Dashboard, S2 Projects, S3 Create/Settings, S4 Project Details | G-B |
| C. Story & Seasons | S5 Story Bible, S6 Seasons List, S7 Season Detail | G-C |
| D. Prompt Studio | S8 Prompt Studio | G-D |
| E. Production | S9 Characters, S10 Scenes, S11 Episode Generation, S12 Jobs/Media | G-E |
| F. Ops & Secondary | S13 Settings/Infrastructure, S14 Publishing/Export, S15 Memory/Lore/Quota | G-F |
| G. E2E Journeys | J1, J2, J3 | G-G |
| H. Hardening | Security, negative, failure, recovery | G-H |
| I. Signoff | Full regression, APK, report | G-I |

## Screen Charters

### S0 - Auth / Launch
- Launch, login, invalid login, empty fields, session persistence, logout.
- API: token valid, `/me` identity matches.
- Negative: wrong password, expired/invalid token.

### S1 - Dashboard
- Dashboard load, summary counts, navigation, empty/error state.
- API: dashboard data matches backend.

### S2 - Projects List
- List, search/filter, open project, empty state, refresh.
- API: project list is user-scoped; cross-user project is invisible.

### S3 - Create Project + Settings
- Name, genres, visual medium, project format, total seasons, episodes/season, episode duration.
- Validation and duplicate-submit protection.
- API: read-back matches exactly; `project_format` metadata matches.

### S4 - Project Details
- Summary, format, seasons card, navigation to bible/seasons/studio, refresh after return.
- API: project detail and season count match.

### S5 - Story Bible
- View, generate/approve, edit/save, version/history if available, seasons card.
- API: bible fields, seasons array, version/approval state persist.
- Negative: empty bible, invalid edit, generate failure, approve without pending action.

### S6 - Seasons List
- Auto-create from total seasons, cards, lock badge, open season.
- API: `story_bible.seasons[]` matches; numbers/ranges match.
- Negative: decreasing count must not delete season data; duplicate/missing season checks.

### S7 - Season Detail
- Title, arc, goals, status, episode range, genres, lock/unlock, blueprint.
- Prompt overrides: SEASON, EPISODE, IMAGE, AI_DIRECTOR.
- Effective genres and missing-prompt state.
- API: season object, genres, lock, override ids persist.
- Negative: edit locked season, invalid range, invalid genre, missing prompt.

### S8 - Prompt Studio
- Tabs: BIBLE, SEASON, EPISODE, IMAGE, AI_DIRECTOR, GENRE, VISUAL_MEDIUM.
- Search, suggestions, Browse all, multi-select, Apply, Clear.
- Selected-first ordering; format default = project format; blank/universal format behavior.
- Active/built-in/default chips; project genres read-only; season effective genres.
- Season prompt overrides summary; `Use for <Season>` and reset.
- Return from season refreshes context.
- API: prompt meta, filtered list, season override, active state, project format.
- Negative: locked season assignment, missing prompt, clear filters, no results, special characters.

### S9 - Characters
- Add, edit, delete, validation, list refresh, persistence.
- API: project-scoped create/update/delete and season/episode linkage if present.
- Negative: blank name, duplicate, delete referenced character.

### S10 - Scenes
- Scene plan create, edit/reorder if available, prompt/description, persistence.
- API: scene rows and order match; project scope enforced.
- Negative: empty scene, invalid order, missing project.

### S11 - Episode Generation
- Episode list/detail, episode prompt override, generate, approval, polling, success output.
- Failure and retry.
- API: command submit, workflow mapping, status, callback, output/media reference.
- Negative: missing prompt, worker unavailable, timeout, duplicate submit.
- Real pipeline only. If blocked, mark BLOCKED; do not fake success.

### S12 - Jobs / Media / Playback
- Job list/detail, retry, media playback, error state.
- API: job status, media asset access, retry command.
- Negative: cross-project job access, missing media, playback failure.

### S13 - Settings / Infrastructure
- Provider/API key settings, masked secrets, save/validate, legacy fallback.
- API: settings persist; secrets masked; no secret in logs.
- Negative: invalid key, missing provider, unauthorized access.

### S14 - Publishing / Export
- Export/publish screen if present, format selection, status.
- API: export job, publish gate.
- Negative: unapproved publish, missing media.

### S15 - Memory / Lore / Quota
- Memory list/add/edit, quota display, limit warning.
- API: memory scope, quota state, tenant isolation.
- Negative: cross-project memory, quota exceeded.

## End-to-End Journeys

### J1 - Project -> Story -> Seasons
Login -> create project with settings -> project details -> Story Bible generate/approve -> seasons auto-create -> season genres + lock -> API verify at every step.

### J2 - Prompt Studio Season Flow
Open Prompt Studio -> search/suggestion -> Browse all multi-select/Apply -> assign season prompt -> reset -> return refresh -> API verify override and active state.

### J3 - Production Flow
Characters -> scenes -> episode prompt override -> generate -> approval -> job polling -> output/media playback -> failure/retry check.

## GHA Hybrid Workflow

Planned workflow: `hybrid-e2e.yml`

Inputs:
- `suite`: screen or journey ID
- `fixture`: `E2E_MAIN` / `E2E_GEN`
- `generate`: `true/false`
- `retries`: default 1

Steps:
1. Build or download debug APK.
2. Start API 34 emulator.
3. Install APK.
4. Run backend API setup.
5. Export `PROJECT_ID`, `SEASON_NO`, `PROMPT_TITLE`, `COMMAND_ID`.
6. Run Maestro flow.
7. Run API assertions.
8. Upload screenshots, video, logs, API JSON.
9. Cleanup only after pass and approval rules.

Modular Maestro flows:
`login`, `dashboard`, `project_create`, `project_settings`, `bible`, `seasons`, `prompt_studio`, `characters`, `scenes`, `episode_generate`, `media`, `settings`.

## Test Data

- `E2E_MAIN`: 2 seasons, 2 episodes, minimum duration.
- `E2E_GEN`: 1 season, 1 episode, cheapest valid settings.
- `E2E_NEG`: security/negative tests.
- Unique names: `E2E_<suite>_<timestamp>`.
- Cleanup default: preserve. Delete only per locked cleanup rule.

## Gap Severity

- P0: journey block, data loss, security.
- P1: feature incorrect or missing, workaround exists.
- P2: UX/polish.

## Risks

- WF-03/Kaggle tunnel unavailable -> J3 blocked.
- Backend/test environment or DB access issue -> Phase A blocked.
- Supabase quota/exhaustion -> generation/media limit.
- Maestro flakiness -> retries and modular flows.
- Kaggle weekly 39-hour limit -> small fixtures and controlled runs.

## Execution Order

`G-A -> S0 -> S1 -> S2 -> S3 -> S4 -> G-B -> S5 -> S6 -> S7 -> G-C -> S8 -> G-D -> S9 -> S10 -> S11 -> S12 -> G-E -> S13 -> S14 -> S15 -> G-F -> J1 -> J2 -> J3 -> G-G -> Hardening -> G-H -> Signoff -> G-I`
