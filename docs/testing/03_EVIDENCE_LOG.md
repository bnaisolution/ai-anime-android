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
| 2026-11-17 | Phase 3 | `Test BYOD` pooler connection | PASS | Supabase project `cnooyjoeymbuvfxnlfjs` | Direct host is IPv6-only and unreachable from EC2; pooler `aws-0-ap-northeast-1.pooler.supabase.com` works; backend `testTenantDsn` returns ok. |
| 2026-11-17 | Phase 3-5 | `Test BYOD` tenant schema v2 | PASS | Backend `ad84103`; PostgreSQL 17.12 | 31 `anime_data` tables incl `schema_migrations`; reapply idempotent; TLS fix `ad84103` deployed; local suite 169 unit, 109 route, 136 static, 22 regression; rollback tag `anime-backend:rollback-pre-ad84103`. |
| 2026-11-17 | Phase 2 | Live migration `011` apply | PASS | Central pooler `aws-0-ap-south-1`; log `migration_011_apply.log` | Registry/audit/project-index/wallet-ledger tables, wallet RPCs, RLS, and flag columns verified; runtime BYOD enabled. |
| 2026-11-17 | Phase 3 | Live test tenant + BYOD connect | PASS | `POST /v1/anime/tenants`; `GET /v1/anime/tenants/me/database` | Tenant `f91f5074-08c0-4f47-925c-8ad1ae7a5fa1`; `mode=byod`, `status=active`, `dsn_configured=true`, `schema_version=2`; health 1339 ms. |
| 2026-11-17 | Phase 3-5 | Live cutover/rollback/re-migrate | PASS | Smoke project `d946a6a6-02e3-45b4-9a88-61674a186306` | `rows_copied=1`; index `byod`; BYOD project row present; rollback `rolled_back=1`; re-migrate `migrated=1`. All 14 workflows still pending. |
| 2026-11-17 | Phase 5 | Gateway TLS fix + live read | PASS | Backend `fd64916`; GHA `44047559530`; live gateway read | `tenantClientConnection()` used in gateway; BYOD read returned `200` with the smoke project; deployed as `anime-backend:fd64916`. |
| 2026-11-17 | Phase 5 | All 14 workflows on BYOD | PENDING | Not yet run | Requires real worker/Telegram/provider configuration and write/callback validation. |
| 2026-11-17 | Phase 3-4 | BYOD-first project creation | FAIL | Source review | New projects still insert into central and lists read central; GAP-013 opened. |
| 2026-11-17 | Phase 1 | Android `d1c6ee4` APK + tests | PARTIAL | GHA `44033033460`, `44033033317` | APK green; Android Tests failed twice at `02-auth-validation` because the emulator killed the app immediately after `stopApp` + `launchApp clearState` (blank screen). |
| 2026-11-17 | Phase 1 | Android `ac14492` Maestro hardening | PASS | GHA `44054066519`, `44054066825` | Removed redundant `stopApp`; Android Tests and APK green; launch, auth-validation, and media-playback flows pass. |
| 2026-11-17 | Phase 5/9 | WF-03 BYOD end-to-end with Telegram media | PASS | n8n execution `122045`; backend `anime-backend:gpu-tgmedia-20261117` | Real image, TTS, GPU video, and CPU FFmpeg master; 193,947-byte master; `clips.checkpoint_stage=MIXED`; episode `current_state=MIXED`; success callback accepted. Fixed resume validation, unsupported `*_file_size_bytes` clip writes, and `audio_usage_log.scene_order`. |
| 2026-11-18 | Phase 5 | BYOD workflow validation sweep | PARTIAL PASS 13/14 | `byod-workflow-validation.json`; n8n executions `122504`-`122537` | Fixed gateway `lt`/`gt` SQL, tenant DB pool/concurrency (`max=4`), internal rate-limit exemption, WF-01 expression braces, WF-02 `sfx_hints` JSON encoding, WF-11 invalid `episode_no`, WF-13 callback fallback. WF-08 fails only on YouTube OAuth `refresh_token` missing. |
| 2026-11-18 | Phase 4 | BYOD-first project creation | PASS | Project `d153f5a1-8731-4184-aeb8-3d412eb8423d`; tenant `f91f5074-08c0-4f47-925c-8ad1ae7a5fa1` | Live POST `/v1/anime/projects` with `ANIME_BYOD_FIRST_ENABLED=true`; tenant row readable, central shadow present, index `data_plane=byod`; list API includes the project. |
| 2026-11-18 | Phase 5 | Signed Telegram playback | PASS | BYOD clip `a8033fee-c25e-474d-ad86-9560347c4b25` | Media API returned 14 clips with `playback_url`; signed fetch returned HTTP 200 `video/mp4` 158,908 bytes; no-token fetch still 401. |
| 2026-11-18 | Phase 5-6 | Backend image `script2-20261118b` | PASS | EC2 `ai-backend`; full suite | Rebuilt image from updated source; health 200; 173 unit + 109 route + 138 static + 22 regression pass. |
| 2026-11-18 | Phase 6 | Owner console contract fixes | PASS | Android source | Queue UI reads nested `outbox`/`commands` payload; owner routes now client-guarded; signed release workflow added. |
| 2026-11-18 | Phase 8 | Read-only hardening drills | PASS 12/12 | `scripts/e2e/script3_hardening_drills.py` | Health, login, identity, tenant binding/health, media read (14 clips), 14 signed URLs, signed fetch 200, unsigned 401, tampered token 401, non-owner owner-route 403. |
| 2026-11-18 | Phase 6 | Owner console contract polish | PASS (source) | `OwnerOverviewScreen.kt`, `AdminQueuesScreen.kt`, `AppNavGraph.kt` | Nested queue counts, data-driven usage/routing/replication, and client owner guard; device verification pending GHA. |
| 2026-11-18 | Release | Signed CI workflow | READY | `.github/workflows/android-release.yml`; `app/build.gradle.kts` | Manual workflow builds signed APK/AAB from four repository secrets and uploads checksums; blocked only on keystore/Play credentials. |

## Evidence Rules

- Never store passwords, tokens, service-role keys, or private keys here.
- Store only paths, run IDs, checksums, and redacted output.
- Preserve failed/negative evidence until manual cleanup approval.
