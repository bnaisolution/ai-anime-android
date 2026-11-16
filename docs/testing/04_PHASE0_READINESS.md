# Phase 0 Readiness Report

Date: 2026-11-16
Status: CONDITIONAL GO
Real generation: NO-GO

Update: backend `bde3582` was deployed after this report was first drafted. `project_format` now passes. Worker configuration is still the remaining blocker.

## Summary

Phase 0 readiness checks are complete. The EC2 server, backend, n8n, and authentication path are reachable. A dedicated test account was created and the project API smoke test passed.

Real generation is not ready yet. The deployed backend is older than the latest commit, and the test account has no worker configuration.

## Results

| Check | Result | Notes |
|---|---|---|
| EC2 SSH | PASS | `anime-ec2` reachable as `ec2-user`. |
| Backend health | PASS | `GET /health` returned 200. |
| n8n health | PASS | `/healthz` returned 200. |
| Canonical workflows | PASS | StoryWF-00 through StoryWF-13 are active. |
| Dedicated test account | PASS | Confirmed account created in the auth Supabase project. |
| Login and identity | PASS | Login and `/api/v1/auth/me` returned the same user id. |
| Project create/read/delete | PASS | API smoke project created, read back, and deleted. |
| Prompt metadata `project_format` | PASS after deploy | Returns `SERIES` after backend `bde3582` deployment. |
| Story Bible `project_format` | PASS after deploy | New project persists `project_format=SERIES`. |
| Test-account worker config | FAIL | 0 worker infrastructure rows; 0 Kaggle sessions. |
| Stored Kaggle tunnel | FAIL | Last updated 2026-09-30 and `/health` fetch failed. |
| CPU/TTS/FFmpeg config | FAIL | No worker, TTS, or FFmpeg configuration found. |
| Telegram storage | NOT VERIFIED | Backend env has Telegram settings, test-account storage not verified. |
| Disk | PASS | 20G total, 9.2G used, 11G free. |
| APK baseline | PASS | Build run `43880136923`; SHA-256 `b906a4ecf05b18644440329af00b658cf7fd2aca6f0c2531fb7551b57b3db4e8`. |

## Blockers

### CLOSED - Backend deployment was stale

- Backend commit `bde3582` has been deployed to EC2.
- `/v1/anime/prompts/library/meta` now returns `project_format=SERIES`.
- New projects persist `project_format=SERIES` in Story Bible.
- Rollback image tag: `anime-backend:rollback-pre-bde3582`.

### P0 - Real generation has no worker

- Test account has 0 rows in `user_infrastructure`.
- Test account has 0 rows in `kaggle_sessions`.
- The only stored Kaggle session is stale and its tunnel health check failed.
- No CPU worker, TTS, or FFmpeg configuration is present for the test account.

Impact: episode generation cannot pass `ensureKaggleWorkerForUser` and real generation is blocked.

Required action: configure the test account with either:
- Kaggle username/API key and a fresh kernel, or
- GitHub CPU worker URL/token and the required TTS/FFmpeg worker URLs.

## Go/No-Go

- Phase A harness work: GO.
- Phase B-D UI/API work: GO. Backend deployment is verified.
- Phase E and J3 real generation: NO-GO until worker configuration is complete.
- No stubs. Blocked generation will be reported as BLOCKED.

## Decisions Needed

1. Enter keys through the app for the dedicated test account:
   - Groq and Gemini keys
   - Kaggle username/API key, or CPU worker URL/token
   - Telegram bot token/chat id
2. Confirm Supabase usage/quota baseline before generation runs.

## Evidence

- `docs/testing/03_EVIDENCE_LOG.md`
- `docs/testing/02_GAP_REGISTER.md`
- Test credentials stored outside git:
  - Termux: `~/.config/e2e/test_account.json`
  - EC2: `/home/ec2-user/.e2e_test_account.json`
