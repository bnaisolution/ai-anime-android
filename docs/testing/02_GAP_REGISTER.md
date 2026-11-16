# Hybrid E2E Gap Register

Status: Phase 0 blockers recorded.

| ID | Severity | Screen/Phase | Expected | Actual | Evidence | Action |
|---|---|---|---|---|---|---|
| GAP-001 | P0 | Phase 0 / S3-S7 | Backend exposes `project_format` in prompt metadata and Story Bible | Deployed image was older than commit `bde3582` | Earlier meta returned `project_format=null`; after deploy it returns `SERIES` | CLOSED: backend `bde3582` deployed and verified on 2026-11-16 |
| GAP-002 | P0 | Phase E / J3 | Test account has a healthy worker session for real generation | Test account has zero infrastructure configs and zero Kaggle sessions; stale global session is unreachable | `user_infrastructure=0`; `kaggle_sessions=0` for test user; stored endpoint `/health` fetch failed | Configure worker keys through the app Settings screen, then start a fresh session |
| GAP-003 | P1 | Phase 0 / Settings | Worker and storage readiness is verified end-to-end | CPU worker URL, TTS, FFmpeg, and Telegram storage are not configured for the test account | Only one Groq provider config exists globally; no worker/storage configs | Enter worker and Telegram settings in the app for the test account before J3 |
| GAP-004 | P1 | S13 / Settings | All requested AI providers can be configured from the app | App/backend support Groq and Gemini; NVIDIA NIM is not supported | Settings screen has Groq/Gemini fields only; backend has no NIM provider path | Add NVIDIA NIM provider support if it must be configured from the app |
| GAP-005 | P1 | S13 / Settings | CPU worker can be configured from the app | App saves GitHub repo/token but sends an empty `worker_url`; backend requires a worker URL | `github_worker.worker_url = ""` in settings payload | CLOSED: CPU worker URL field and persisted `github_worker.worker_url` shipped in Phase 1 |

## Severity Guide

- P0: journey block, data loss, security.
- P1: feature incorrect or missing, workaround exists.
- P2: UX/polish.

## Rules

- Every gap needs evidence: screenshot, video, API JSON, log snippet, or run link.
- Failed and negative-case data must be preserved for evidence.
- Do not mark a gap closed without a re-run or explicit verification.
| GAP-006 | P0 | Phase 2 | Tenant database registry exists in the live DB | Migration `011` exists with rollback but is not applied; no DB DSN is available in local/EC2 env | Source review and env inventory | Apply `011` using the migration runbook after supplying an approved DB migration DSN |
| GAP-007 | P0 | Phase 5 | n8n/workers read/write the selected tenant data plane | Gateway remains central-only; project cutover index is prepared but not used by generation | `workflowGateway.routes.ts` fetches central Supabase only | Implement tenant-aware Postgres gateway and validate all 14 workflows before enabling cutover |
| GAP-008 | P1 | Phase 4 | Cutover verifies all core project data | Migration copies projects only, not seasons/scenes/episodes/prompts/memory | `migrateProjectsToTenant` copies `anime_data.projects` | Extend copy/checksum per table or keep cutover disabled |
| GAP-009 | P1 | Phase 7 | Wallet funds and pricing are operational | Wallet tables/RPCs/preflight exist; no payment funding, prices, tax, or payout | `wallet.service.ts`, migration `011` | Provide business decisions and payment provider before enabling billing |
