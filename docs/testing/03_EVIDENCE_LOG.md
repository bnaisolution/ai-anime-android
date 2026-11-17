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
| 2026-11-16 | Phase 2-7 | `npm run build` + `npm test` (backend `b154413`) | PASS | Local Termux | 152 unit, 101 route, 131 static, 22 regression; no failures. |
| 2026-11-16 | Phase 2 | Migration `011_hybrid_byod_wallet.sql` static review | PASS | Source | Additive tables/RPCs, RLS, rollback pair, feature flags default false. |
| 2026-11-16 | Phase 3 | DSN validation unit suite | PASS | `test/unit/tenantDatabase.validation.test.js` | Rejects non-Postgres, private hosts, missing auth, bad SSL; fingerprint stable across password rotation. |
| 2026-11-16 | Phase 3-7 | Route registration + Android contract static | PASS | New hybrid tests | Tenant DB/wallet routes and Android DSN/owner-registry wiring present. |
| 2026-11-16 | Phase 1-7 | Backend `b154413` | PASS | GHA `43980517504` | All gating jobs green; isolated DB tests skipped. |
| 2026-11-16 | Phase 1-7 | Android `27fccff` APK + tests | PASS | GHA `43980517754`, `43980517645` | Build APK green; Android Tests green after Maestro-only rerun. |
| 2026-11-17 | Phase 5 | Tenant gateway implementation | PASS | Backend `75faa00` | Project BYOD routing, tenant SQL translation, central catalog fallback, system fan-out; system write injection fixed. |
| 2026-11-17 | Phase 5 | `npm run build` + full `npm test` | PASS | Local Termux | 160 unit, 105 route, 135 static, 22 regression; no failures. |
| 2026-11-17 | Phase 5 | Backend gateway CI | PASS | GHA `43984422766` | All gating jobs green; isolated DB tests skipped. |
| 2026-11-17 | Phase 6 | Owner tenant DB filters/features backend | PASS | Backend `ec782e2`; GHA `44004718519` | Status/mode/provider/host filters, audit preview, health recheck, read-only env flags, owner-only guard. |
| 2026-11-17 | Phase 6 | Android owner filters/audit/recheck | PASS | Android `ca02e21`; GHA `44004693568`, `44004693535` | Android Tests and APK green; Maestro passed without rerun. |
| 2026-11-17 | Phase 6 | Backend local full suite | PASS | Local Termux | 160 unit, 108 route, 136 static, 22 regression; no failures. |
| 2026-11-17 | Phase 3-5 | Full tenant schema and data copy | PASS | Backend `71c1e0d`; GHA `44010169513` | 30 project tables plus schema migrations; 168 unit, 108 route, 136 static, 22 regression. |
| 2026-11-17 | Phase 3-5 | Local PostgreSQL schema/copy validation | PASS | PostgreSQL 18.6 on Termux | 31 `anime_data` tables including `schema_migrations`; reapply idempotent; legacy projects-only upgrade passed; synthetic copy preserved JSONB and excluded `pending_actions`. |
| 2026-11-17 | Phase 6 | Owner schema repair | PASS | Backend `71c1e0d`; Android `d0de7cb` | `POST /v1/anime/owner/tenant-databases/:tenantId/provision`; Android Tests `44010191940`; APK `44010191939`. |
| 2026-11-17 | Phase 8 | Fail-closed and rollback hardening | PASS | `test/unit/tenantDatabase.runtime.test.js` | Disabled BYOD, paused/DB-down, missing DSN, mismatched tenant index, and expired 7-day rollback all fail closed. |
| 2026-11-17 | Phase 2-5 | EC2 backend deploy `71c1e0d` | PASS | `anime-backend:71c1e0d` on EC2 | `/health` 200; n8n reaches `http://backend:3000`; test-account login, projects, and prompt-meta pass; hybrid routes return 401 unauthenticated; rollback tag `ec2-user-backend:rollback-pre-71c1e0d`. |
| 2026-11-17 | Phase 5 | Live BYOD/workflow validation | PENDING | No BYOD DSN; migration `011` not applied | Backend `71c1e0d` is deployed and healthy; gateway remains disabled by feature flag. |
| 2026-11-17 | Phase 2 | Live migration `011` apply | PENDING | No DB DSN available in Termux/EC2 env | Tables absent until applied; feature flags remain off. |

## Evidence Rules

- Never store passwords, tokens, service-role keys, or private keys here.
- Store only paths, run IDs, checksums, and redacted output.
- Preserve failed/negative evidence until manual cleanup approval.
