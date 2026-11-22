# ADR 0001: The assistant service

- **Status:** Accepted for phase 0
- **Date:** 2026-10-08
- **Builds on:** [research-agent-platform.md](../research/research-agent-platform.md)

## Context

The learning assistant lives inside `recommendation`. It has one persona (learner), nine fixed intents behind an LLM router, five tools, one write (save a learning path), and it forwards the user's bearer token to learning. The platform now needs an assistant for learners, instructors and staff, with writes that other people approve and with per-capability evaluation and kill switches. Growing the intent enum in `recommendation` does not scale to that, and it mixes ranking with agent orchestration.

## Decision

1. **A new deployable, `assistant`**, owns agent sessions and turns, pending actions and their audit, capability switches and quotas, AI policy acceptance and (from phase 1) the knowledge index and learner memory. `recommendation` keeps ranking, search and its projections. The old agent endpoints stay until cinx-web has moved over.
2. **Stack:** Python 3.12, FastAPI, Pydantic AI 2.54 for the agent loop, PostgreSQL 17 with pgvector, DBOS 3.2 for durable workflows, Alembic for schema.
   - Pydantic AI was chosen over LangGraph and a hand-written loop. It is model-agnostic. It validates structured output and retries on a bad schema, which matters with open-weight models. It passes typed dependencies to tools without putting them in the prompt.
   - LangGraph's checkpointed graphs are not needed while each capability is a single agent loop.
3. **Model:** any OpenAI-compatible Chat Completions endpoint, self-hosted vLLM today. A capability asks for a tier (`default`, `fast`); configuration maps the tier to a model. Moving to a hosted provider is a configuration change.
4. **One agent loop, persona by page.**
   - The placement (the page the panel is on) picks the persona. The persona's `assistant.agent.use.<persona>` permission is required.
   - The capability registry, declared in code, gives the model a catalog. The model calls `select_capability`, and only then are that capability's tools loaded (a dynamic toolset).
   - The model plans (`share_plan`), chooses tools and recovers from tool errors on its own. Code bounds which capabilities exist for the caller, the step and tool-call limits, and what a write does.
5. **Writes are pending actions, decoupled from the run.**
   - A write tool validates its payload and stores a `pending_action`, then returns `PENDING_APPROVAL` to the model. Nothing reaches a domain service yet.
   - A permitted person approves it: the user themself (`self`), or a holder of `approver_permission` (`permission`). They may approve later and on another device.
   - Execution then runs in a DBOS workflow keyed by the action id, with the action's `Idempotency-Key`, and every transition is written to the append-only `action_audit`.
   - This is used instead of Pydantic AI's in-run approval pause, because an approver is often not the person whose run proposed the action.
6. **Identity.**
   - The user's token is never forwarded. Tools call services with a token obtained through Keycloak standard token exchange (V2) by the confidential client `cinx-assistant`. Its audience is `cinx-api` today.
   - Writes use the **approver's** token at approval time. No token is ever stored, not even in DBOS state. A workflow recovered after a crash finds none and ends the action as `REAPPROVAL_REQUIRED`.
   - The service account `cinx-assistant-service` exists only for the auth authorization-state lookup. It is a separate client so the `token_kind=user` mapper of `cinx-assistant` can never apply to a service-account token.
   - Keycloak 26.6.1 has no delegation (`act`) in V2, so "the agent acted for user X, approved by Y" is recorded in `action_audit`, not in the token.
7. **Events** go through a transactional outbox in the assistant's Postgres. `assistant.action.decided` is announced when an action reaches a final state.

## Consequences

- Postgres becomes a second database engine in a MySQL estate. It needs its own backup and monitoring.
- `cinx-web` tokens now carry the audience `cinx-assistant` in addition to `cinx-api`.
- Token exchange must be verified against a running Keycloak 26.6.1 before phase 0 exits:
  - the exchanged token's claims (`uid`, `permissions`, `token_kind`, `authz_ver`);
  - its audience;
  - whether services that check `azp` accept it.
- Actions that require MFA cannot be approved without a step-up token from the approver. This is also why the agent never runs MFA-protected admin operations itself.
- Per-capability daily quotas are counted locally (`capability_usage`). Wiring them to the commerce entitlement catalog is phase 0 backlog.

Note (2026-11-14): auth no longer serves an authorization-state lookup. Token freshness is checked at the gateway from Redis (`docs/security/authentication-authorization.md`, section 18), and `cinx-assistant-service` is used for the learning, commerce and other internal APIs.

Note (2026-10-29): the `uid` claim and `authz_ver` no longer exist. The principal is `sub`, and the freshness check compares the token's `iat` with the `changedAt` of the auth authorization state (`docs/security/authentication-authorization.md`, section 18).
