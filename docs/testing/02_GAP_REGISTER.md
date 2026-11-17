# Hybrid E2E Gap Register

Status: Phase 0 blockers recorded.

| ID | Severity | Screen/Phase | Expected | Actual | Evidence | Action |
|---|---|---|---|---|---|---|
| GAP-001 | P0 | Phase 0 / S3-S7 | Backend exposes `project_format` in prompt metadata and Story Bible | Deployed image was older than commit `bde3582` | Earlier meta returned `project_format=null`; after deploy it returns `SERIES` | CLOSED: backend `bde3582` deployed and verified on 2026-11-16 |
| GAP-002 | P0 | Phase E / J3 | Test account has a healthy worker session for real generation | Test account has zero infrastructure configs and zero Kaggle sessions; stale global session is unreachable | `user_infrastructure=0`; `kaggle_sessions=0` for test user; stored endpoint `/health` fetch failed | Configure worker keys through the app Settings screen, then start a fresh session |
| GAP-003 | P1 | Phase 0 / Settings | Worker and storage readiness is verified end-to-end | CPU worker URL, TTS, FFmpeg, and Telegram storage are not configured for the test account | Only one Groq provider config exists globally; no worker/storage configs | Enter worker and Telegram settings in the app for the test account before J3 |
| GAP-004 | P1 | S13 / Settings | All requested AI providers can be configured from the app | App/backend support Groq and Gemini; NVIDIA NIM is not supported | Settings screen has Groq/Gemini fields only; backend has no NIM provider path | Add NVIDIA NIM provider support if it must be configured from the app |
| GAP-005 | P1 | S13 / Settings | CPU worker can be configured from the app | App saves GitHub repo/token but sends an empty `worker_url`; backend requires a worker URL | `github_worker.worker_url = ""` in settings payload | CLOSED: CPU worker URL field and persisted `github_worker.worker_url` shipped in Phase 1 |
| GAP-006 | P0 | Phase 2 | Tenant database registry exists in the live DB | CLOSED: migration `011` applied through `aws-0-ap-south-1.pooler.supabase.com`; registry/audit/project-index/wallet-ledger tables, wallet RPCs, RLS, and flag columns verified | Pooler apply log; DB queries; test tenant connected | CLOSED: apply and schema verification passed on 2026-11-17 |
| GAP-007 | P0 | Phase 5 | n8n/workers read/write the selected tenant data plane | PARTIAL: test tenant `f91f5074-08c0-4f47-925c-8ad1ae7a5fa1` is connected to `Test BYOD`; connect/provision/health and smoke-project cutover/rollback/re-migrate passed; gateway TLS fix `fd64916` deployed and live read passed. All 14 workflows and writes/callbacks are not yet validated | `GET /v1/anime/tenants/me/database`; health 1339 ms; index `byod`; BYOD project row present; GHA `44047559530` | Validate gateway writes/callbacks and all 14 workflows on the BYOD plane |
| GAP-008 | P1 | Phase 4 | Cutover verifies all core project data | CLOSED: migration copies all 30 project data tables, upserts by id, excludes transient `pending_actions`, and verifies per-table row counts | Backend `71c1e0d`; local PostgreSQL 18.6 synthetic copy passed projects/episodes/scenes/clips/audit_log with JSONB preserved | CLOSED: live cutover remains under GAP-007 |
| GAP-009 | P1 | Phase 7 | Wallet funds and pricing are operational | Wallet tables/RPCs/preflight exist; no payment funding, prices, tax, or payout | `wallet.service.ts`, migration `011` | Provide business decisions and payment provider before enabling billing |
| GAP-010 | P0 | Phase 3-5 | BYOD schema supports every gateway-allowed project table | CLOSED: `TENANT_SCHEMA_SQL` creates all 30 gateway project tables, season-aware unique indexes, project indexes, and schema version 2; legacy projects-only databases upgrade in place | Backend `71c1e0d`; `tenantSchema.ts`; local PostgreSQL 18.6 create/upgrade/idempotence checks passed | CLOSED: live provisioning remains under GAP-007 |
| GAP-011 | P1 | Phase 6 | Owner can change feature toggles safely | Owner console shows read-only environment status; changing BYOD/platform/billing requires an env update and restart | `GET /v1/anime/owner/features`; `ownerFeatureSummary` | Add audited DB-backed toggles with explicit confirmation before enabling runtime changes |
| GAP-012 | P2 | Phase 6 | Owner can retry tenant schema/migration actions | Owner can recheck health and repair the tenant schema, but per-project data migration retry remains user-side | `POST /v1/anime/owner/tenant-databases/:tenantId/provision`; Android `d0de7cb` | Add a project-scoped owner migration retry only if operations need it |
| GAP-013 | P1 | Phase 3-4 | BYOD-first users write new projects directly to their database | `createProjectSynchronous` inserts into central `projects` and `listProjects` reads central; new projects need an explicit per-project migration even when BYOD is active | Source review of `reads/projects.service.ts`; live test tenant connected | Add tenant-aware project creation/list routing or document migration-required behavior before claiming BYOD-first |

## Severity Guide

- P0: journey block, data loss, security.
- P1: feature incorrect or missing, workaround exists.
- P2: UX/polish.

## Rules

- Every gap needs evidence: screenshot, video, API JSON, log snippet, or run link.
- Failed and negative-case data must be preserved for evidence.
- Do not mark a gap closed without a re-run or explicit verification.
