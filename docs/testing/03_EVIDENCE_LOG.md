# Hybrid E2E Evidence Log

| Date | Phase/Screen | Run/Command | Result | Artifact/Link | Notes |
|---|---|---|---|---|---|
| 2026-11-16 | Phase 0 | EC2 SSH smoke | PASS | Termux shell | `anime-ec2` login successful; hostname and user verified. |
| 2026-11-16 | Phase 0 | Docker inventory | PASS | EC2 `sudo -n docker ps` | `ai-backend` and `n8n` running. |
| 2026-11-16 | Phase 0 | Backend and n8n health | PASS | EC2 localhost | Backend `/health` 200; n8n `/healthz` 200. |
| 2026-11-16 | Phase 0 | Canonical n8n workflows | PASS | `n8n list:workflow --active=true` | StoryWF-00 through StoryWF-13 are active. |
| 2026-11-16 | Phase 0 | Dedicated test account | PASS | Supabase auth admin | Confirmed account created in auth project; credentials stored mode 600. |
| 2026-11-16 | Phase 0 | Auth and project API smoke | PASS | EC2 localhost API | Login, `/me`, project create/read/delete all passed. |
| 2026-11-16 | Phase 0 | Prompt metadata project format | FAIL | `GET /v1/anime/prompts/library/meta` | `project_format=null`; deployed image predates commit `bde3582`. |
| 2026-11-16 | Phase 0 | Test-account worker readiness | FAIL | `user_infrastructure`, `kaggle_sessions` | Test account has 0 worker configs and 0 sessions. |
| 2026-11-16 | Phase 0 | Stale Kaggle tunnel | FAIL | Stored `api_endpoint` health check | Endpoint fetch failed; last update 2026-09-30. |
| 2026-11-16 | Phase 0 | APK baseline | PASS | GHA run `43880136923` | SHA-256 `b906a4ecf05b18644440329af00b658cf7fd2aca6f0c2531fb7551b57b3db4e8`. |
| 2026-11-16 | Phase 0 | Backend deploy `bde3582` | PASS | EC2 `docker compose up -d --build backend` | Backup created; old image tagged `anime-backend:rollback-pre-bde3582`; health 200. |
| 2026-11-16 | Phase 0 | Post-deploy project format | PASS | API smoke | Meta `project_format=SERIES`; Story Bible `project_format=SERIES`; smoke project deleted. |
| 2026-11-16 | Phase 0 | GAP-001 closure | CLOSED | Post-deploy smoke | `project_format` verified after redeploy. |

## Evidence Rules

- Never store passwords, tokens, service-role keys, or private keys here.
- Store only paths, run IDs, checksums, and redacted output.
- Preserve failed/negative evidence until manual cleanup approval.
