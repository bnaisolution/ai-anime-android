# Hybrid E2E State

Last updated: 2026-11-16

## Current Phase

Phase 1 - App Settings and Panel Shell. Plan ready; coding not started.

Phase 0 is complete with conditions. Final hybrid implementation plan is in `docs/testing/05_HYBRID_IMPLEMENTATION_PLAN.md`.

Result: Backend `bde3582` is deployed and `project_format` is verified. Real generation is NO-GO until the test account gets worker and storage keys through the app Settings screen.

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
- CPU worker URL field is missing; app sends an empty `worker_url`.
- APK baseline: build run `43880136923` success; SHA-256 `b906a4ecf05b18644440329af00b658cf7fd2aca6f0c2531fb7551b57b3db4e8`.
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

Get approval on the 5 open decisions in `05_HYBRID_IMPLEMENTATION_PLAN.md`, then implement Phase 1:
- Settings Database tab with central-DB status and BYOD disabled.
- CPU worker URL field.
- Role-gated Owner Console shell.
- Backend tenant registry/resolver interface with central default and feature flags off.

After Phase 1 passes GHA, configure the test account through the app Settings screen and start real generation E2E.
