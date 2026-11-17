# Hybrid E2E State

Last updated: 2026-11-17

## Current Phase

Phases 2-7 implementation code is committed; live cutover, BYOD validation, and real generation remain gated.

Phase 0 is complete with conditions. Phase 1 UI is CI-verified. Final hybrid implementation plan is in `docs/testing/05_HYBRID_IMPLEMENTATION_PLAN.md`.

Result: Backend `71c1e0d` now provisions all 30 gateway project tables, copies every project data table (excluding transient `pending_actions`), verifies copied row counts, and adds owner schema repair; Backend Tests run `44010169513` is green. Android `d0de7cb` adds the owner schema-repair action; Android Tests run `44010191940` and APK run `44010191939` are green. Migration `011` is still not applied; BYOD and platform billing remain disabled. Real generation is NO-GO until the test account gets worker and storage keys through the app Settings screen.

## Phase 2-7 Implementation Status

| Phase | Status | What exists | Remaining gate |
|---|---|---|---|
| 2 Tenant registry | IMPLEMENTED / MIGRATION PENDING | `anime_tenant_databases`, audit, encrypted DSN, flags, resolver | Apply migration `011` to live DB |
| 3 BYOD connect | IMPLEMENTED / LIVE-TEST PENDING | DSN validation, test, connect, disconnect, health, provisioning of all 30 gateway project tables | Test DSN and pause/unpause |
| 4 Optional cutover | IMPLEMENTED / LIVE-TEST PENDING | Per-project copy of all 30 tables, upserts, per-table row-count verification, 7-day rollback window | Live cutover/rollback with a real BYOD DSN |
| 5 n8n/worker routing | IMPLEMENTED / LIVE-TEST PENDING | Project BYOD routing, central catalog fallback, tenant SQL gateway, system-scope fan-out | Apply migration `011`, test with a live BYOD DSN, validate all 14 workflows |
| 6 Owner console | PARTIAL | Tenant memberships, tenant DB filters, audit preview, health recheck, schema repair, wallet visibility, read-only environment flag status | Per-project migration retry action and runtime-mutable feature toggles |
| 7 Platform-managed billing | SCAFFOLDED / DISABLED | Wallet tables, credit/debit RPCs, balance preflight | Pricing/provider decisions, payment funding, explicit owner enablement |
| 8 Hardening | PARTIAL | DSN rejection, encryption, audit, fail-closed routing, cross-tenant index checks, paused/DB-down denial, 7-day rollback guard | Live pause/rotation/restore drills and full workflow fault injection |
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
- Android `d0de7cb` (`feat(owner): repair tenant schema from console`): Android Tests run `44010191940` success; Build APK run `44010191939` success.
- Local PostgreSQL 18.6 validation: tenant schema creates 30 data tables plus `anime_data.schema_migrations`; reapply is idempotent; a legacy projects-only database upgrades cleanly; synthetic projects, episodes, scenes, clips, and audit rows copy with JSONB preserved; `pending_actions` is intentionally not copied.
- Backend: `npm run build` and full `npm test` pass locally (168 unit, 108 route, 136 static, 22 regression). New tests cover PostgREST-to-SQL translation, BYOD project routing, system fan-out, owner filters, schema repair, read-only feature status, DSN validation, full-schema contracts, fail-closed routing, rollback expiry, and Android API/UI contracts.
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

1. Apply migration `011` to the central anime database using the migration runbook; verify tables, RPCs, and rollback availability.
2. Keep `ANIME_BYOD_ENABLED=false` and `ANIME_PLATFORM_DB_ENABLED=false` until a BYOD test DSN passes connect/health/provision and pause/unpause.
3. Deploy backend `71c1e0d` only after migration `011` is applied, then validate all 14 workflows against a BYOD test DSN.
4. Finish Phase 1 real-key evidence and Phase 3 live BYOD testing.
5. Add per-project migration retry and runtime feature toggles to finish Phase 6.
6. Run live BYOD cutover/rollback and pause/rotation/restore drills before enabling production cutover.
