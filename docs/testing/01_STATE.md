# Hybrid E2E State

Last updated: 2026-11-18

## Current Phase

Phases 2-7 implementation code is committed; live BYOD validation passed for the dedicated test tenant. WF-03 is validated end-to-end on BYOD with Telegram-backed image, TTS, GPU video, and CPU FFmpeg output. Workflow validation is 13/14 on BYOD; WF-08 is blocked only by a missing YouTube OAuth refresh token. BYOD-first project creation and signed Telegram playback are live; read-only hardening drills pass 12/12. Pause/rotation/restore drills and real generation remain gated.

Phase 0 is complete with conditions. Phase 1 UI is CI-verified. Final hybrid implementation plan is in `docs/testing/05_HYBRID_IMPLEMENTATION_PLAN.md`.

Result: Backend `fd64916` is deployed on EC2 as `anime-backend:fd64916`; Backend Tests run `44047559530` is green. Migration `011` is applied to the central DB, the dedicated test account has tenant `f91f5074-08c0-4f47-925c-8ad1ae7a5fa1`, and `Test BYOD` (`cnooyjoeymbuvfxnlfjs`) is connected through pooler `aws-0-ap-northeast-1.pooler.supabase.com`. Live connect/provision/health passed with schema v2 (31 tables incl `schema_migrations`); smoke project `d946a6a6-02e3-45b4-9a88-61674a186306` passed cutover, rollback, and re-migrate. Backend `fd64916` also fixes the gateway TLS path so BYOD reads no longer fail with `self-signed certificate in certificate chain`; the live gateway read returned the smoke project. Runtime BYOD is enabled; platform DB/billing remain disabled. Android `ac14492` records this evidence; Android Tests `44054066519` and APK `44054066825` are green. The earlier `d1c6ee4` Android Tests failure was a Maestro launch race: the emulator killed the app immediately after `stopApp` + `launchApp clearState`. Removing the redundant `stopApp` fixed it. BYOD validation passes 13/14 workflows, and flagged project creation now writes tenant-first with a central shadow and project index. WF-08 remains blocked by the missing YouTube OAuth refresh token. Real generation is NO-GO until the test account gets worker and storage keys through the app Settings screen.

## Phase 2-7 Implementation Status

| Phase | Status | What exists | Remaining gate |
|---|---|---|---|
| 2 Tenant registry | APPLIED / LIVE | `anime_tenant_databases`, audit, encrypted DSN, flags, resolver; migration `011` applied and verified | Audited runtime feature toggles |
| 3 BYOD connect | LIVE TEST PASS | DSN validation, test, connect, disconnect, health, full 30-table provisioning; test tenant connected and healthy | Pause/unpause, rotation, restore drills |
| 4 Optional cutover | LIVE PASS | Per-project copy, row-count verification, 7-day rollback; smoke cutover/rollback/re-migrate passed; **BYOD-first project creation now writes tenant DB + central shadow + project index** behind `ANIME_BYOD_FIRST_ENABLED` | Broad rollout and checksums |
| 5 n8n/worker routing | LIVE PASS 13/14 | Project BYOD routing, central catalog fallback, tenant SQL gateway, system-scope fan-out; 13 of 14 workflows pass on BYOD; signed Telegram playback works for private BYOD media; WF-08 blocked only by missing YouTube OAuth refresh token | WF-08 credential; error-callback drills |
| 6 Owner console | PARTIAL | Tenant memberships, tenant DB filters, audit preview, health recheck, schema repair, wallet visibility, read-only environment flags; queue cards use the live nested contract; usage/routing/replication are data-driven; owner routes are client-guarded | Per-project migration retry action and runtime-mutable feature toggles; device verification |
| 7 Platform-managed billing | SCAFFOLDED / DISABLED | Wallet tables, credit/debit RPCs, balance preflight | Pricing/provider decisions, payment funding, explicit owner enablement |
| 8 Hardening | PARTIAL | DSN rejection, encryption, audit, fail-closed routing, cross-tenant index checks, paused/DB-down denial, 7-day rollback guard; read-only drills pass 12/12 (signed playback tamper/unsigned rejection, owner 403, tenant binding/health) | Live pause/rotation/restore drills and full workflow fault injection |
| 9 E2E rollout | BLOCKED | Existing smoke harness and docs | Real keys/worker/Telegram + canary tenant |

## Locked Decisions

- Staging / dedicated test account.
- Real generation only, no stubs.
- Conditional cleanup: pass data may be deleted; fail/negative data preserved until manual approval.
- Free stack: CPU worker (Savera TTS + FFmpeg), Kaggle free tier (39h/week), Telegram storage.
- SaaS key model: user-owned provider keys are entered in the app, encrypted per user by the backend, and resolved per user at generation time.
- Supabase service-role key stays backend-only and must never be entered in the app.
- Staged hybrid model:
  - Now: central DB + app-entered BYOD keys; platform-managed DB/GPU/CPU/billing stays disabled.
  - Later: platform-managed database/GPU/CPU and billing, enabled by the owner only.
  - BYOD interfaces will be designed now; full user-DB cutover happens after the app is proven.
- Owner and user panels live in the same app, gated by backend role. A separate web admin can come later.
- Existing Settings screen becomes the user/workspace admin panel. Add a Database tab for BYOD connection, health, migration, and disconnect.
- Platform-wide Owner Console stays a separate role-gated screen: tenants, DB health, migrations, audit, quotas.

## Phase 0 Checklist

- [x] Create plan/state/gap/evidence files.
- [x] Verify EC2 SSH access.
- [x] Verify backend, n8n, worker, and tunnel readiness.
- [x] Create dedicated confirmed test account.
- [x] Verify login and identity.
- [x] Run project-create API smoke.
- [ ] Record quota/storage baseline.
- [x] Write Go/No-Go report.

## Phase 0 Results

- SSH: pass.
- Backend health `/health`: 200.
- n8n health: 200.
- Canonical StoryWF workflows: active.
- Test account: created in auth Supabase project, confirmed.
- Login, `/auth/me`, project create/read/delete: pass.
- Backend deployment: commit `bde3582` deployed; `project_format` now appears in metadata and Story Bible.
- Worker: no infrastructure configured for the test account; stale Kaggle session is unreachable.
- App Settings currently supports Groq, Gemini, Kaggle, GitHub CPU worker repo/token, and Telegram. NVIDIA NIM is not supported yet.
- CPU worker URL field and independent GPU/CPU choices are implemented in Settings.
- APK baseline: build run `43880136923` success at `d26f989`; SHA-256 `b906a4ecf05b18644440329af00b658cf7fd2aca6f0c2531fb7551b57b3db4e8`.
- Settings commit `b5769f6`: Android Tests run `43937558915` success after one Maestro rerun; Build APK run `43937558894` success.
- Owner tenant card commit `a6d1153` and state-doc commit `741b51c`: Android Tests run `43941767938` success; Build APK run `43941767857` success.
- Backend commit `cbdf6be`: Backend Tests run `43940241680` success.
- Phase 1 UI gate is satisfied; Phase 1 real-key/generation gate remains pending.
- Backend hybrid baseline `b154413`, Phase 5 gateway `75faa00`, Phase 6 owner filters `ec782e2`, and full tenant schema/copy `71c1e0d`: Backend Tests run `44010169513` success.
- Backend `fd64916` (`fix(tenant): apply explicit BYOD TLS options in gateway`): Backend Tests run `44047559530` success; deployed on EC2; live BYOD gateway read returned the smoke project.
- Migration `011` applied live via `aws-0-ap-south-1.pooler.supabase.com`; registry/audit/project-index/wallet-ledger tables, wallet RPCs, RLS, and flag columns verified.
- Dedicated test account now has owner tenant `f91f5074-08c0-4f47-925c-8ad1ae7a5fa1`; `GET /v1/anime/tenants/me/database` returns `mode=byod`, `status=active`, `dsn_configured=true`, `schema_version=2`.
- `Test BYOD` live: connect, provision v2, health (reachable, 1339 ms), smoke-project cutover (`rows_copied=1`), rollback, and re-migrate all passed; BYOD copy contains project `d946a6a6-02e3-45b4-9a88-61674a186306`.
- Android `d0de7cb` (`feat(owner): repair tenant schema from console`): Android Tests run `44010191940` success; Build APK run `44010191939` success.
- Android `d1c6ee4` (BYOD evidence docs): APK run `44033033460` success; Android Tests run `44033033317` failed twice because the emulator killed the app right after the redundant `stopApp` + `launchApp clearState`.
- Android `ac14492` removes that `stopApp`; Android Tests run `44054066519` and APK run `44054066825` are green. Maestro launch, auth-validation, and media-playback flows all pass.
- Local PostgreSQL 18.6 validation: tenant schema creates 30 data tables plus `anime_data.schema_migrations`; reapply is idempotent; a legacy projects-only database upgrades cleanly; synthetic projects, episodes, scenes, clips, and audit rows copy with JSONB preserved; `pending_actions` is intentionally not copied.
- Backend: `npm run build` and full `npm test` pass locally (169 unit, 109 route, 136 static, 22 regression). New tests cover PostgREST-to-SQL translation, BYOD project routing, system fan-out, owner filters, schema repair, read-only feature status, DSN validation, explicit TLS options, full-schema contracts, fail-closed routing, rollback expiry, and Android API/UI contracts.
- Phase 5 system writes no longer inject `project_id=system`; system gateway requests preserve their own scoped body.
- Hybrid migration `011_hybrid_byod_wallet.sql` has a paired rollback. It is additive and disabled by default.
- Disk: 20G total, 9.2G used, 11G free.

## Database Topology

- Auth Supabase project: login/session plus assistant-app data. It also has assistant `projects`, conversations, messages, and memory. Do not store anime central data here.
- Anime Supabase project: central anime database. It has anime projects, episodes, scenes, characters, prompts, infrastructure, and tenant tables.
- No third Supabase project is needed now.
- Future BYOD: user database is the data plane; the anime project remains the control plane. Auth project stays login/assistant only.

## Environment Notes

- Termux SSH config includes `anime-ec2` (`ec2-user`).
- Docker requires `sudo -n docker` on EC2.
- Known containers: `ai-backend`, `n8n`.
- Dedicated test credentials: `~/.config/e2e/test_account.json` (Termux) and `/home/ec2-user/.e2e_test_account.json` (EC2), mode 600.
- Secrets must never be printed to logs or committed.

## Next Step

1. Validate the remaining 13 workflows against the live BYOD test tenant, including gateway writes, system fan-out, `pending_actions` exclusion, and callbacks; WF-03 passed on 2026-11-17.
2. Implement BYOD-first project creation/list routing; new projects currently write central first and need explicit migration.
3. Run live cross-tenant denial, pause/unpause, DSN rotation, DB-down fail-closed, restore, and 7-day rollback drills.
4. Finish Phase 1 real-key evidence and the remaining Phase 3 pause/rotation tests.
5. Add per-project migration retry and audited runtime feature toggles to finish Phase 6.
6. Rotate the exposed production/BYOD passwords and configure worker/Telegram credentials for a real generation smoke.
