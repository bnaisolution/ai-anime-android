# Required Inputs To Execute All Phases

Purpose: one-time checklist so work can continue phase by phase without repeated questions.

Secrets rule:
- Do not paste passwords, API keys, service-role keys, or DSNs in chat.
- Put secrets in `~/.config/e2e/` on Termux with mode 600.
- Do not commit secrets.
- Supabase service-role key must never be added to the Android app.

## 1. Decisions

Answer these once.

| # | Decision | Recommended |
|---|---|---|
| 1 | CPU worker URL source | User enters manually in Settings |
| 2 | BYOD database type | Postgres/Supabase DSN only |
| 3 | Existing projects migration | Opt-in per project |
| 4 | User DB paused/unreachable behavior | Read-only + guided restore; no auto central fallback |
| 5 | Owner Console first version | Read-only health/tenants; actions later |
| 6 | Owner account | Dedicated staging owner account, not personal account |
| 7 | BYOD test database | Separate free Supabase/Postgres project |
| 8 | Phase 7 paid infra/billing | Deferred; no paid service without new approval |
| 9 | App release | Debug/internal APK now; Play Store later |
| 10 | BYOD migration | Optional only for existing central users; BYOD-first users need no migration |
| 11 | NVIDIA NIM | Skip until separately approved |
| 12 | BYOD quotas | No quotas for BYOD; platform-managed usage is pay-per-minute from wallet balance |

## 2. Accounts And Access

Needed now or soon:

- [ ] Test user account: already exists at `~/.config/e2e/test_account.json`.
- [ ] Dedicated staging owner account. Credentials in `~/.config/e2e/owner_account.json`.
- [ ] Owner role granted to the staging owner user.
- [ ] Confirm owner email + user_id (no password in chat).
- [ ] Normal user account for 403/role tests.
- [ ] GitHub token with repo/workflow access for the backend and Android repos.
- [ ] EC2 SSH access: `anime-ec2` user `ec2-user`.
- [ ] `sudo -n docker` access on EC2: already available.
- [ ] Anime Supabase service-role access on EC2: confirm it is already in backend env.
- [ ] Auth Supabase service-role access on EC2: confirm it is already in backend env.
- [ ] n8n admin API key or approval to modify workflows through EC2.
- [ ] CPU worker URL + token, or worker deployment/restart access.
- [ ] Kaggle username + API key for free GPU.
- [ ] Telegram bot token + private test chat/channel ID for storage.
- [ ] Groq API key for the test user.
- [ ] Gemini API key if vision/image flow is in E2E.
- [ ] BYOD test database DSN with create-schema/create-table privileges.
- [ ] BYOD test DB pause/unpause access for failure tests.
- [ ] Play Console + release keystore only if Play Store release is approved.

## 3. Secure File Formats

Create these files with mode 600.

`~/.config/e2e/owner_account.json`

```json
{
  "email": "...",
  "password": "...",
  "user_id": "..."
}
```

`~/.config/e2e/providers.json`

```json
{
  "groq_api_key": "...",
  "gemini_api_key": "...",
  "kaggle_username": "...",
  "kaggle_key": "...",
  "cpu_worker_url": "https://...",
  "cpu_worker_token": "...",
  "github_worker_repo": "owner/repo",
  "github_worker_token": "...",
  "telegram_bot_token": "...",
  "telegram_chat_id": "..."
}
```

`~/.config/e2e/byod.json`

```json
{
  "provider": "supabase",
  "dsn": "postgresql://user:password@host:5432/postgres?sslmode=require",
  "ssl": "require"
}
```

`~/.config/e2e/n8n.json`

```json
{
  "api_key": "...",
  "base_url": "https://..."
}
```

## 4. Approvals

Write "approved" next to each before the phase starts.

- [ ] Backend changes and redeploys to EC2.
- [ ] Android changes, GHA builds, and APK artifacts.
- [ ] Central anime DB schema migrations.
- [ ] Creating staging owner account and owner role.
- [ ] Creating/altering tenant registry rows.
- [ ] Modifying n8n workflows.
- [ ] BYOD schema creation in the test database.
- [ ] Migrating existing test project data to BYOD test DB.
- [ ] Deleting passed-test data.
- [ ] Preserving failed-test data until manual review.
- [ ] Keeping central rollback copy for 7 days.
- [ ] No paid services without separate approval.
- [ ] No NVIDIA NIM without separate approval.
- [ ] No Play Store release without separate approval.

## 5. Test Assets

- [ ] Sample project title and premise, or approval to reuse existing test project.
- [ ] 1-2 characters with names/descriptions.
- [ ] 1 season and 1 target episode.
- [ ] Preferred language and voice style.
- [ ] Telegram test channel for media upload.
- [ ] Acceptance criteria for "real generation passed".
- [ ] Owner tenant display name for Owner Console.

## 6. What Is Needed Per Phase

| Phase | Minimum inputs |
|---|---|
| 1 | Owner test account, normal test account, CPU worker URL decision, provider keys for smoke |
| 2 | Approval for central DB migration, encryption key confirmation |
| 3 | BYOD test DSN, approval to create schema, pause/unpause access |
| 4 | Approval to migrate data, rollback window, test project data |
| 5 | n8n API key/approval, CPU worker access, scoped-token design approval |
| 6 | Owner account, quota defaults, audit policy |
| 7 | Deferred: billing provider, prices, currency, tax, budget, payout account |
| 8 | Retention policy, backup policy, secret rotation approval |
| 9 | APK acceptance, canary tenant, release/signing decision |

## 7. What Can Start Without Waiting

- Phase 1 UI and backend shell.
- Database tab showing central DB status.
- CPU worker URL field.
- Owner Console read-only shell.
- Tests and GHA wiring.
- Phase 2 tenant registry and resolver scaffolding with feature flags off.

## 8. What Cannot Start Without Inputs

- Real E2E generation: needs provider/worker/Telegram keys.
- Owner Console data tests: needs owner role/account.
- BYOD connect/provision: needs test DB DSN.
- BYOD migration: needs approval and sample data.
- n8n routing changes: needs n8n access/approval.
- Billing/platform-managed: needs business decisions and budget approval.
- Play Store release: needs signing/Play Console access.
