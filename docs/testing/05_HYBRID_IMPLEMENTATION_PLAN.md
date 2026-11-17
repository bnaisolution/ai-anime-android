# Final Hybrid Implementation Plan

Status: phases 2-7 implementation committed; live BYOD validation, cutover, and real-key E2E remain gated.
Date: 2026-11-17

## Implementation Progress (2026-11-17)

- Phase 2: encrypted tenant database registry, audit, feature flags, and resolver implemented; migration `011` pending live apply.
- Phase 3: DSN validation/test/connect/health/disconnect/provision APIs and Android Database-tab controls implemented; provisioning currently creates `projects` only, so live test DSN and full project-table DDL remain pending.
- Phase 4: per-project copy and rollback index implemented; full table copy/checksum and production routing remain gated.
- Phase 5: tenant-aware gateway committed in backend `75faa00`; project BYOD routing, central catalog fallback, and system-scope fan-out implemented; live BYOD/workflow validation pending.
- Phase 6: owner tenant database registry, filters, audit preview, health recheck, wallet visibility, and read-only environment flag status added; migration retry/actions and runtime toggles remain.
- Phase 7: wallet tables, credit/debit RPCs, and balance preflight added; platform-managed remains disabled.
- Phase 8: private-host rejection, encryption, audit, and fail-closed errors added; runtime isolation tests remain.
- Phase 9: blocked on real provider, worker, Telegram, and BYOD test credentials.

## Goal

Build the app in stages without blocking on paid infrastructure:

- Now: central anime database + app-entered user keys.
- Later: optional BYOD user database.
- Last: platform-managed database/GPU/CPU + billing.

Owner and user panels live in the same app, protected by backend roles.

## Locked Decisions

1. Auth Supabase project stays for login/session and assistant-app data only.
2. Anime Supabase project is the central anime database.
3. No third Supabase project is needed now.
4. Platform-managed hosting, GPU, CPU, and billing stay disabled until the owner enables them.
5. User-owned keys are entered through the app and encrypted per user in the backend.
6. Supabase service-role keys stay backend-only and are never entered in the app.
7. Settings screen becomes the user/workspace admin panel.
8. Owner Console is a separate role-gated screen in the same app.
9. BYOD database support is Postgres/Supabase only, using a direct Postgres DSN.
10. BYOD is optional and feature-flagged. Central DB remains the default until cutover.

## Target Architecture

```text
Android App
  |
  | JWT + role
  v
Fastify Backend
  |
  +--> Auth Supabase Project
  |      login/session + assistant app data
  |
  +--> Anime Central Supabase Project
  |      tenant registry, encrypted DB credentials, global catalogs, jobs, audit
  |
  +--> Tenant Resolver
          |
          +--> Central Anime DB (default)
          +--> User BYOD DB (optional, later)
          |
          +--> n8n / CPU worker via backend DB gateway
```

## Data Ownership

### Auth project
- Login/session
- Assistant app data
- Do not store anime project data here.

### Anime central project
- Anime tenants and members
- Encrypted user provider keys
- Encrypted BYOD database credentials
- Global prompt templates, voice/model catalogs
- Job/command queue and audit
- Feature flags, plans, quotas

### User BYOD database
- Projects
- Story Bible
- Seasons
- Characters
- Scenes
- Episodes/scripts
- Project prompts
- Memory/lore
- Media metadata

Media files remain in Telegram/storage.

## Settings UX Requirements

### AI & Vision
- Primary and fallback providers are selectable, not free text.
- Dropdown shows only providers the user has added.
- Dropdown includes "Add more AI provider".
- New users see only "Add AI Provider".
- Adding a provider opens an API-key form; after saving, it appears in the list.
- Selecting a provider reveals a model dropdown.
- Fallback follows the same flow.
- Groq and Gemini are available now; more providers are roadmap items until backend support is added.

### Compute Workers
GPU worker options:
1. Kaggle Account 1
2. Kaggle Account 2
3. Add your own GPU
4. Platform GPU (disabled, coming soon)

CPU worker options:
1. GitHub CPU Worker
2. Own CPU Worker
3. Platform CPU Worker (disabled, coming soon)

Each option opens its own credential fields.

### Storage
- Storage provider is selected from a dropdown.
- Telegram is active now.
- S3-compatible and custom HTTP are backend-ready but UI wiring is a later phase.
- More providers remain roadmap items.

### Database
- Separate Database tab.
- Shows Central Anime Database as active.
- My Database and Platform-Managed Database are visible but disabled until their phases.
- Shows connection health and a Test Connection action.
- Owner sees an Owner Console entry.

## Roles And Panels

### User panel
Same Settings screen:
- AI & Vision
- Compute Workers
- Storage & Policy
- Database
- Session

User can:
- Enter AI provider keys.
- Enter Kaggle or CPU worker settings.
- Enter Telegram storage settings.
- View current database mode and health.
- Connect a BYOD database when enabled.
- Test, save, migrate, disconnect.

Database settings are workspace-level, not project-level. The UI must say: "Applies to all projects in this workspace."

### Owner Console
Separate screen, visible only when backend role is owner/admin:
- Tenant list
- Per-tenant database mode and health
- Schema version and migration status
- Worker health
- Quotas
- Audit log
- Platform-managed feature toggles
- Billing preview (later)

Security:
- UI role check is convenience only.
- Every owner route must use backend `requireOwner`.
- Normal users get 403 even if they open the route directly.
- Owner actions are audited.

## Phases

### Phase 0 - Baseline
Status: mostly complete.

Done:
- Backend `bde3582` deployed.
- Test account created.
- Login and project API smoke pass.
- Central/anime and auth Supabase roles confirmed.

Remaining:
- Confirm app-entered keys flow.
- Confirm worker setup for the test account.

### Phase 1 - App Settings And Panel Shell (now)
Goal: make the app usable and complete the user/owner panel shell without blocking on BYOD.

Tasks:
1. Settings screen:
   - Add Database tab.
   - Show current mode: Central Anime DB, My Database (coming soon), Platform-Managed (coming soon).
   - Show DB health and schema version.
   - Add note that DB settings apply to the whole workspace.
2. Compute Workers:
   - Add CPU worker URL field.
   - Save and hydrate `github_worker.worker_url`.
   - Keep repo/token fields.
3. Owner Console:
   - Add role-gated navigation.
   - Read-only tenant list, DB health, worker health.
   - Hide for normal users; backend 403 enforcement.
4. Tests:
   - Compose tests for Database tab and role gating.
   - Test tags: `DB_TAB`, `DB_MODE`, `DB_TEST`, `DB_SAVE`, `OWNER_CONSOLE`, `OWNER_TENANTS`.
5. E2E:
   - Test account logs in.
   - Keys are saved from app.
   - Backend returns masked values.
   - Real generation can start.

Gate:
- Settings Database tab visible.
- CPU worker URL persists.
- Owner Console owner-only.
- Normal user blocked by backend.
- App builds and tests pass in GHA.

### Phase 2 - Tenant Registry And Resolver Interface (now, no cutover)
Goal: prepare backend for BYOD without changing current data location.

Tasks:
1. Add central tables:
   - `tenant_databases`
   - `tenant_db_audit`
2. Encryption:
   - Reuse `ANIME_ENCRYPTION_KEY`.
   - Encrypt DSN before storage.
   - Never return raw DSN.
3. Add `TenantDbResolver`:
   - `getTenantDb(tenantId)` returns central client by default.
   - No behavior change for existing projects.
4. Add feature flags:
   - `BYOD_ENABLED=false`
   - `PLATFORM_DB_ENABLED=false`
5. Add health endpoint for current data plane.

Gate:
- Existing project APIs unchanged.
- Feature flags off.
- No direct DSN exposure.

### Phase 3 - BYOD Connect And Provision (later)
Goal: let a workspace connect its own Postgres/Supabase database.

Tasks:
1. APIs:
   - `GET /v1/anime/tenants/me/database`
   - `POST /v1/anime/tenants/me/database/test`
   - `POST /v1/anime/tenants/me/database/connect`
   - `POST /v1/anime/tenants/me/database/migrate`
   - `DELETE /v1/anime/tenants/me/database`
2. Validation:
   - HTTPS/SSL required.
   - Least-privilege DB role.
   - Reject superuser roles where possible.
   - Connection timeout and clear errors.
3. Provisioning:
   - Create `anime_data` schema.
   - Create all 30 gateway project tables and `schema_migrations`.
   - Add season-aware uniqueness and project indexes.
   - Reapply safely to legacy projects-only databases.
4. UI:
   - Enable Database tab BYOD fields.
   - Test Connection.
   - Save Database.
   - Health state: connected, paused, error.
5. Failure handling:
   - User DB paused/unreachable: show action, no central fallback.
   - Disconnect: workspace becomes read-only or disabled per policy.

Gate:
- Test Supabase/Postgres connects.
- Schema provisions.
- Health check works.
- Invalid credentials rejected.
- No secret in logs.

### Phase 4 - Optional BYOD Cutover (later)
Goal: route new projects to the user DB. Migration is optional, never forced.

Important:
- A user who provides a database before creating projects writes directly to that database from day one. No migration is needed.
- Migration exists only for a user who started on the central database and later chooses to connect their own database.
- A BYOD-only user is never moved automatically.

Tasks:
1. Route new projects to user DB when BYOD is active.
2. Keep central DB for workspaces without BYOD.
3. Optional migration for existing central projects:
   - Copy all 30 project data tables, excluding transient `pending_actions`.
   - Preserve ids, timestamps, and JSONB values with idempotent upserts.
   - Verify per-table row counts before switching.
   - Switch tenant mapping atomically.
   - Keep central rollback copy for 7 days.
4. UI:
   - "Start fresh" is the default.
   - "Move existing projects" is optional and opt-in per project.
   - Progress and verification screen.
5. Rollback:
   - Revert mapping to central.
   - No data deletion until approval.

Gate:
- New project stays in user DB.
- API read-back from user DB.
- Migration verified.
- Rollback tested.

### Phase 5 - n8n And Worker Routing (later)
Goal: keep user DB credentials out of n8n and workers.

Done:
- Backend `71c1e0d` adds project-to-data-plane resolution, tenant Postgres execution for a safe PostgREST subset, central catalog fallback, full tenant schema, and system-scope fan-out across central + active BYOD planes.
- System writes preserve their scoped body instead of injecting `project_id=system`.
- Cross-tenant project-index mismatch, paused databases, missing DSNs, and DB-down paths fail closed.

Remaining:
1. Add scoped tokens containing tenant and project.
2. Validate n8n workflow reads/writes against a live BYOD database.
3. Verify CPU/GPU worker gateway calls on both data planes.
4. Remove remaining hardcoded Supabase assumptions from workflows.
5. Run live cross-tenant and BYOD DB-down fault injection.

Gate:
- n8n cannot see BYOD credentials.
- Generation reads/writes user DB.
- Cross-tenant access denied.
- All 14 workflows validated against a live BYOD plane.

### Phase 6 - Full Owner Console (later)
Goal: platform-wide operations.

Done:
- Tenant database registry with `status`/`mode`/`provider`/host-database filters.
- Per-tenant mode, status, schema version, host, database, and last error.
- Audit preview plus owner-triggered health recheck with audit writes.
- Owner-triggered tenant schema repair with audit writes.
- Read-only environment status for BYOD, platform DB, platform-managed, and billing flags.
- Owner-only access with 403 for normal users.

Remaining:
1. Project-scoped data-migration retry action.
2. Worker health details.
3. Quota/usage detail views.
4. Full audit log navigation.
5. Runtime-mutable feature toggles with owner confirmation and audit.
6. Encryption key rotation status.

Gate:
- Owner can see tenant health.
- Owner can retry a migration.
- Normal user cannot access.
- Actions audited.

### Phase 7 - Platform-Managed And Billing (future)
Goal: add paid platform-managed capacity only when users exist.

Billing model:
- Pay per minute for platform GPU/CPU.
- User wallet balance is required before a platform job can be created.
- No quotas or limits on BYOD usage; the user pays their own providers directly.
- Platform usage is metered and deducted per minute.
- BYOD stays unlimited and free from the platform's perspective.

Tasks:
1. Platform database pool.
2. Platform GPU/CPU pool.
3. Wallet balance and per-minute metering.
4. Preflight balance check before job creation.
5. Owner approval gates.
6. Keep disabled by default.

Gate:
- Disabled by default.
- Owner can enable per tenant.
- Wallet balance is checked before a platform job starts.
- BYOD users are not limited by platform quota.

### Phase 8 - Hardening And Isolation (before BYOD rollout)
Done:
- Unit coverage for cross-tenant index mismatch, DB-down/paused denial, missing DSN, BYOD-disabled central resolution, and expired rollback.
- DSN private-host rejection, encryption, audit redaction, and timeouts.

Tasks:
1. Cross-tenant tests.
2. DB-down tests.
3. Paused Supabase test.
4. Credential rotation.
5. Connection pool limits.
6. Timeout/circuit breaker.
7. Audit and redaction tests.
8. Backup/restore and disconnect tests.

Gate:
- No cross-tenant leakage.
- No secret in logs.
- Failure isolated to one tenant.

### Phase 9 - E2E And Rollout
Tasks:
1. Screen-wise E2E with API assertions.
2. J1 project/story/seasons.
3. J2 prompt studio/season prompts.
4. J3 characters/scenes/episode generation.
5. Real generation with free Kaggle/CPU/Telegram.
6. APK checksum and GHA evidence.
7. Canary one tenant before general rollout.

Gate:
- Real generation passes.
- Owner/user panels pass.
- No P0 gaps.
- Rollback documented.

## What We Do Not Do Now

- No paid database.
- No paid GPU/CPU.
- No platform-managed hosting.
- No billing.
- No full BYOD cutover.
- No NVIDIA NIM until separately approved.
- No third Supabase project.

## Immediate Next Work

1. Finish Phase 1:
   - Database tab.
   - CPU worker URL.
   - Owner Console shell.
2. Run real generation with app-entered keys.
3. Only after app is stable, start Phase 2/3 BYOD.

## Resolved Decisions

1. CPU worker URL is entered manually in Settings.
2. BYOD uses Postgres/Supabase DSN only.
3. Existing-project migration is optional and opt-in per project. BYOD-first users need no migration.
4. User DB paused/unreachable: read-only plus guided restore; no automatic central fallback.
5. Owner Console first release is read-only; actions come later.
6. No quotas on BYOD usage.
7. Platform-managed usage is pay-per-minute from wallet balance; job creation requires sufficient balance.

