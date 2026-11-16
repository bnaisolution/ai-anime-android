# Hybrid E2E Gap Register

Status: Phase 0 blockers recorded.

| ID | Severity | Screen/Phase | Expected | Actual | Evidence | Action |
|---|---|---|---|---|---|---|
| GAP-001 | P0 | Phase 0 / S3-S7 | Backend exposes `project_format` in prompt metadata and Story Bible | Deployed image was older than commit `bde3582` | Earlier meta returned `project_format=null`; after deploy it returns `SERIES` | CLOSED: backend `bde3582` deployed and verified on 2026-11-16 |
| GAP-002 | P0 | Phase E / J3 | Test account has a healthy worker session for real generation | Test account has zero infrastructure configs and zero Kaggle sessions; stale global session is unreachable | `user_infrastructure=0`; `kaggle_sessions=0` for test user; stored endpoint `/health` fetch failed | Configure worker keys through the app Settings screen, then start a fresh session |
| GAP-003 | P1 | Phase 0 / Settings | Worker and storage readiness is verified end-to-end | CPU worker URL, TTS, FFmpeg, and Telegram storage are not configured for the test account | Only one Groq provider config exists globally; no worker/storage configs | Enter worker and Telegram settings in the app for the test account before J3 |
| GAP-004 | P1 | S13 / Settings | All requested AI providers can be configured from the app | App/backend support Groq and Gemini; NVIDIA NIM is not supported | Settings screen has Groq/Gemini fields only; backend has no NIM provider path | Add NVIDIA NIM provider support if it must be configured from the app |
| GAP-005 | P1 | S13 / Settings | CPU worker can be configured from the app | App saves GitHub repo/token but sends an empty `worker_url`; backend requires a worker URL | `github_worker.worker_url = ""` in settings payload | Add a CPU worker URL field and persist it through the app |

## Severity Guide

- P0: journey block, data loss, security.
- P1: feature incorrect or missing, workaround exists.
- P2: UX/polish.

## Rules

- Every gap needs evidence: screenshot, video, API JSON, log snippet, or run link.
- Failed and negative-case data must be preserved for evidence.
- Do not mark a gap closed without a re-run or explicit verification.
