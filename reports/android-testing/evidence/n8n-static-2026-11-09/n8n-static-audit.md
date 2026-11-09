# n8n Canonical Workflow Static Audit

**Date:** 2026-11-09
**Canonical directory:** `ai-anime-backend/n8n-workflows/`
**Workflow count:** 14
**Runtime execution history/API access:** BLOCKED — not available in this environment
**Scope:** static JSON inspection only; no workflow was activated, deactivated, modified, or executed.

| Code | File | Workflow name | n8n ID | Active | Nodes | Connections | Trigger types | Webhook paths | Credential objects | retryOnFail/maxTries nodes | continueOnFail nodes | Disabled nodes | Connection integrity |
|---|---|---|---|---|---:|---:|---|---|---:|---:|---:|---:|---|
| WF-00 | `StoryWF-00_Main_Router.json` | `StoryWF-00_Main_Router` | `U4pLz7pXmG2OqXlt` | true | 34 | 33 | webhook | api/v1/router | 0 | 0 | 15 | 0 | PASS |
| WF-01 | `StoryWF-01.json` | `StoryWF-01` | `UeEXeimGGwqIh84T` | true | 33 | 32 | — | — | 0 | 0 | 25 | 0 | PASS |
| WF-02 | `storywf02.json` | `storywf02` | `qRUdhg1PNDK2wJk6` | true | 25 | 24 | — | — | 0 | 0 | 17 | 0 | PASS |
| WF-03 | `storywf03.json` | `storywf03` | `s4RGuFSDlevgD5nC` | true | 38 | 37 | — | — | 0 | 0 | 21 | 0 | PASS |
| WF-04 | `storywf04.json` | `storywf04` | `8q9tF3VE0mLUZQfa` | true | 30 | 29 | — | — | 0 | 0 | 17 | 0 | PASS |
| WF-05 | `storywf05.json` | `storywf05` | `ADPseu4KBfVveYrq` | true | 16 | 15 | — | — | 0 | 0 | 7 | 0 | PASS |
| WF-06 | `storywf06.json` | `storywf06` | `WAB7tCTJSUilOp07` | true | 17 | 16 | — | — | 0 | 0 | 10 | 0 | PASS |
| WF-07 | `Storywf07.json` | `Storywf07` | `zvw63UrbObZhFUiW` | true | 27 | 26 | — | — | 0 | 0 | 13 | 0 | PASS |
| WF-08 | `Storywf08.json` | `Storywf08` | `MrWrz58aRUVTzpLr` | true | 22 | 21 | — | — | 0 | 0 | 10 | 0 | PASS |
| WF-09 | `storywf09.json` | `storywf09` | `gT1c0Hpvrwyf9YS6` | true | 14 | 13 | — | — | 0 | 0 | 7 | 0 | PASS |
| WF-10 | `StoryWf10.json` | `StoryWf10` | `swOEbQV3iNUNmMwu` | true | 14 | 13 | — | — | 0 | 0 | 6 | 0 | PASS |
| WF-11 | `Storywf11.json` | `Storywf11` | `baRqhI9JSob9m5oD` | true | 15 | 14 | — | — | 0 | 0 | 9 | 0 | PASS |
| WF-12 | `StoryWf12.json` | `StoryWf12` | `2fcuca0HkGxGIYMv` | true | 8 | 7 | — | — | 0 | 0 | 3 | 0 | PASS |
| WF-13 | `storywf13.json` | `storywf13` | `jI39p4vr4CnWhpgw` | true | 13 | 12 | scheduleTrigger | — | 0 | 0 | 4 | 0 | PASS |

## Static findings

- Exactly 14 canonical workflow JSON files were found in `n8n-workflows/`.
- All 14 workflow IDs are unique.
- All 14 workflows have `active: true` in the JSON metadata.
- No workflow JSON contains a `credentials` object.
- All connection source/target references resolve to declared nodes.
- No node declares `retryOnFail` or `maxTries`; retry behavior is therefore not configured in the JSON metadata.
- Many nodes set `continueOnFail`; this can allow downstream execution after a node error and is a runtime risk that cannot be validated statically.
- Runtime activation state, execution history, error rates, and actual credential binding remain **BLOCKED** without approved n8n API/instance access.

## Status

| Check | Result | Status |
|---|---|---|
| Canonical 14-file set | 14 JSON files | PASS (static) |
| Unique workflow IDs | 14/14 unique | PASS (static) |
| JSON metadata active=true | 14/14 | PASS (static) |
| No credential objects in JSON | 14/14 | PASS (static) |
| Connection integrity | 14/14 | PASS (static) |
| Retry configuration | none declared | NOT TESTED / risk |
| Runtime execution history | unavailable | BLOCKED |
| End-to-end execution | not executed | NOT TESTED |

## Evidence

- `n8n-static-audit.json` — machine-readable per-workflow metadata.
- Source directory: `ai-anime-backend/n8n-workflows/`.
