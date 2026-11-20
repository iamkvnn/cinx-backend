# Authentication, authorization and subscription rollout

Status: implemented local-unit execution; production stabilization deferred

Phases C-F have repository implementations for catalog, subscriptions,
entitlement projections/quota and Learning access. Live/Testcontainers and
production scenarios below remain deferred and unrun.

Last reviewed: 2026-07-31

## 1. Scope and release posture

This plan turns the following target documents into a staged implementation:

- [authentication and authorization](./authentication-authorization.md);
- [authorization matrix](./authorization-matrix.yaml);
- [plans, subscriptions, entitlements and quotas](../commerce/plans-subscriptions-entitlements-quotas.md);
- [current final code review](../reviews/final-code-review.md).

The current worktree is not a clean release baseline. In particular, the
review found exploitable authorization gaps, an unauthenticated payment
callback boundary, a failing auth test and a reporting compile failure. Phase
0 is mandatory; do not hide those failures behind the larger redesign.

This rollout assumes there is no released production data. Development
databases may be reset during an agreed cutover, but applied Flyway migration
files are still immutable. Add the next migration in every module.

## 2. Rollout principles

1. Deny by default. A route without an authorization-matrix entry is not ready.
2. Deploy contract-compatible readers/consumers before new writers/producers.
3. Store a domain mutation, inbox/outbox record and local projection change in
   the same transaction where required.
4. User and service identities are separate trust boundaries.
5. Plan/entitlement/quota enforcement is separate from security permission.
6. Use local entitlement projections for runtime decisions.
7. Keep every phase deployable and observable before enabling the next phase.
8. Roll back behavior with flags or a forward fix; do not roll back a database
   after new-format writes without a tested reverse path.
9. No phase is complete from unit tests alone when it changes persistence,
   broker behavior, Keycloak or a provider callback.

Suggested temporary rollout switches:

```text
cinx.security.permission-claims-enabled
cinx.security.permission-enforcement-enabled
cinx.security.internal-service-jwt-required
cinx.notification.websocket-auth-required
cinx.subscription.billing-enabled
cinx.entitlement.projection-consumer-enabled
cinx.entitlement.enforcement-enabled
cinx.quota.enforcement-enabled
```

Flags MUST be typed configuration with secure production defaults, owned by
the affected service and removed after cutover. They are rollout controls, not
permanent authorization bypasses.

## 3. Product decision record

Recorded decisions:

- pure SPA with OIDC Authorization Code + PKCE and bearer access tokens;
- immediate status/version denial for account/role/permission reduction;
- subscription includes selected course catalogs and platform/AI features;
- learner and instructor product lines use eligibility policies, never role
  grants, and share one v1 `BASE` slot;
- Stripe is the preferred adapter, customer market is Vietnam and presentment
  currency is VND;
- v1 enables individual USER subjects only while reserving a subject key for
  later organization work;
- plan names, prices, policies, features and quotas are versioned commerce data,
  not application constants.

Before enabling paid production behavior, record approval for:

- the merchant legal-entity/Stripe account country and verified VND recurring
  capability; Vietnam is not currently listed for direct Stripe accounts;
- exact plan names, prices, feature/quota values and translations;
- launch trial/grace policy and provider retry settings;
- proration, refund, dispute and eligibility-loss billing behavior;
- tax/invoice obligations, course eligibility and instructor compensation;
- retention for billing, entitlement, quota and audit records;
- timing for organization seats, add-on slots and overage.

The decision record should identify product owner, effective date and affected
contract version. Do not encode unresolved choices as scattered conditionals.

## 4. Phase overview

| Phase | Outcome | Production enablement |
|---|---|---|
| 0 | Existing release blockers closed and trustworthy green baseline | no new feature |
| 1 | Keycloak/JWT permission foundation and exhaustive route inventory | permission claims observed |
| 2 | Keycloak-owned roles, thin auth admin layer, token freshness | auth announces changes |
| 3 | Permissions, ownership, internal JWT and WebSocket enforced | security model active |
| 4 | Plan/subscription/billing aggregate and provider boundary | billing sandbox only, then limited production |
| 5 | Entitlement event, local projections and course access model | subscription feature access active |
| 6 | Concurrency-safe local quotas | metered features active |
| 7 | Cleanup, resilience, audit and release certification | general availability |

## 5. Phase 0 — repair the baseline

### Work

1. Fix the reporting `BigDecimal` compile error.
2. Move the unused Mockito stub in the auth test to only the tests that need it.
3. Authenticate the one-time order webhooks
   `/api/v1/payments/webhooks/{stripe,momo,vnpay}` using each gateway's
   signature, replay protection, allowlist and durable event deduplication
   (done: the generic `callbacks/{provider}` endpoint was removed).
   Recurring billing later uses `/api/v1/billing/webhooks/{provider}` with a
   separate normalized command; both may share verified-webhook infrastructure.
4. Add immediate permission and ownership checks for:
   - community moderation;
   - course sections/curriculum movement;
   - course images and other nested content;
   - recommendation user/session routes.
5. Remove notification test-send endpoints from non-local profiles or protect
   them with `notification.test.send.any`.
6. Require authentication and destination rules for notification WebSocket.
7. Fix the high-risk outbox/inbox/retry issues listed in the final review
   before relying on payment or entitlement events.

Temporary permission checks may use the current role model only as a short
bridge. The service-layer ownership checks are not temporary.

### Gate

- Root Maven unit build passes from a clean checkout.
- Payment sandbox rejects invalid signature, old timestamp and replay.
- Anonymous, missing-permission, wrong-owner and allowed tests exist for each
  critical operation.
- Current known failures are removed from the review baseline rather than
  merely skipped.

## 6. Phase 1 — permission and token foundation

### 6.1 Complete the operation inventory

For every controller mapping, WebSocket destination, provider webhook and
active internal caller:

1. Add or reference one operation in
   [`authorization-matrix.yaml`](./authorization-matrix.yaml).
2. Identify owner, caller type, authentication mechanism, exact audience,
   permission, ownership/relationship, state attributes, entitlement and
   quota.
3. Search for every caller before changing a route.
4. Validate structure against
   [`authorization-matrix.schema.json`](./authorization-matrix.schema.json).
5. Add a semantic inventory check that splits pipe-delimited methods and fails
   CI when a concrete controller operation matches zero or more than one
   policy, references an unknown permission/feature, or violates caller/client
   assignments. Explicit public operations remain mapped.

The current matrix intentionally covers critical and target flows, not every
existing endpoint. Exhaustive coverage is a release gate.

### 6.2 Keycloak configuration

Add as version-controlled authorization/realm configuration:

- canonical `contracts/security/cinx-authorization.yaml` with policy version,
  permissions, exact human bundles and exact service grants;
- `cinx-api` resource client;
- target-specific internal audiences;
- one confidential client/service account per real calling service;
- granular permission client roles;
- additive human composite roles;
- protocol mappers for `roles`, `permissions`, `token_kind` and audience;
- staff MFA flows;
- least-privilege `cinx-auth-admin` service account.

Realm validation must assert configuration semantics, not only valid JSON:

- no wildcard web redirect URI in production;
- public web client has no client secret;
- direct-access password grant is disabled unless explicitly required;
- `PLATFORM_ADMIN` does not include Keycloak `realm-management`;
- service accounts receive only their target permissions/audiences;
- browser tokens cannot acquire internal service permissions.
- realm composites and service grants exactly match the canonical catalog.

### 6.3 Converter compatibility sequence

1. Deploy `common-security` converter support for `permissions` ->
   `PERM_*`, exact CINX role filtering and service-token claims.
2. Keep current compatible `ROLE_*` behavior while old tokens may exist.
3. Configure Keycloak to issue new claims including `acr`, `amr` and
   `auth_time`, then inspect decoded test tokens.
4. Deploy gateway and resource services with the new converter.
5. Switch endpoint expressions to `PERM_*` only in Phase 3.
6. After at least the maximum access-token lifetime, remove the broad
   `realm_access.roles` fallback.

Tests MUST cover malformed claim shapes, blank values, duplicates,
infrastructure-role exclusion, wrong client, principal fallback, user token
and service token.

### Gate

- A user test token contains only intended CINX roles/permissions and
  `aud=cinx-api`.
- A service token contains `token_kind=service`, one target audience and the
  minimal endpoint permission.
- A sensitive-operation token proves the configured MFA ACR/method and recent
  `auth_time`; stale or insufficient MFA produces the step-up error.
- Wrong issuer/audience/token kind is rejected at both gateway/resource
  boundary where applicable.
- All affected converter consumers compile/test together.

## 7. Phase 2 — Keycloak-owned roles and thin auth admin layer

### Work

1. Add the next auth Flyway migration (`V4__keycloak_owned_roles.sql`):
   - `identity_account` keeps `user_id` (the Keycloak `sub`), status, ban
     fields and `changed_at`;
   - drop the role, sync-job, authorization-version and email/subject columns
     and tables;
   - keep `authorization_audit`.
2. Replace single-role APIs with set-based role operations that call the
   Keycloak admin API synchronously.
3. Make Keycloak the owner of realm roles (`LEARNER` from the realm default
   roles); auth records status, `changed_at`, audit and the outbox event in the
   same DB transaction as the Keycloak call.
4. Keep credentials, sessions and token issuance in Keycloak.
5. Remove `INSTRUCTOR_APPLICANT` from authorization roles. User service owns
   the application workflow; verification/termination events make auth add or
   remove `INSTRUCTOR` in Keycloak.
6. Set the Keycloak user `enabled` flag on suspend/ban and reactivate.
7. Serve `GET /internal/identity/accounts/{userId}/authorization-state`
   (`status`, `changedAt`) and deploy the freshness check (iat against
   `changedAt`, cache of at most five seconds) in the gateway, `common` and the
   Python services.
8. Publish `auth.authorization.changed` version 3 with roles, permissions and
   `changedAt`; consumers order by `changedAt`.

Because the system is unreleased, dev data can be recreated. Still test both a
fresh schema and the migration path supported by the repository.

### Event deployment order

1. Update AsyncAPI and compatibility tests.
2. Add consumer schemas, inboxes and handlers that understand the new version.
3. Deploy all consumers with the binding active.
4. Deploy the auth producer behind a publish switch.
5. Publish test events, verify projections and DLQ metrics.
6. Enable publication.
7. Retire the old event only after no active consumer depends on it.

### Gate

- Concurrent role commands do not lose another role.
- Duplicate commands/events are successful no-ops.
- A role/status command fails as a whole when the Keycloak admin call fails.
- A suspended user cannot sign in or refresh; reactivation restores both.
- A token issued before `changed_at` (floored to the second) is rejected with
  `401 TOKEN_AUTHORIZATION_STALE`; the silent renew gets the new roles.
- User service approval cannot grant arbitrary roles.
- Suspension and role reduction meet the measured five-second denial objective;
  stale/unavailable state fails closed on protected operations.
- A user's first token works on every route, with no onboarding exemption.

## 8. Phase 3 — enforce authorization at every boundary

### 8.1 User HTTP

- Replace broad `hasRole('ADMIN')` and role-only endpoint checks with exact
  `PERM_*`.
- Move cross-user/staff routes under `/api/v1/admin/**` and migrate the
  ambiguous internal batch path to `/internal/user-ids`, updating every caller,
  gateway/OpenAPI entry and the service catalog atomically.
- Keep relationship and resource-state checks in the owning application
  service.
- Derive actor from `sub`; never authorize a path/body identity.
- Use `404` concealment only where the matrix explicitly requires it.
- Add service-layer tests so a controller refactor cannot bypass ownership.

### 8.2 Internal HTTP

Roll out caller by caller:

1. Create caller client registration and minimal target permission.
2. Add OAuth2 client-credentials token injection.
3. Add the target's higher-priority `/internal/**` filter chain.
4. Validate issuer, exact target audience, `token_kind=service`, allowed client
   and permission.
5. Verify wrong user token, wrong service, wrong audience and missing
   permission all fail.
6. Enable required enforcement only after every listed caller is migrated.

Gateway continues to deny `/internal/**`. Deployment network policy remains
defense in depth, not the only control.

### 8.3 WebSocket

- Authenticate STOMP `CONNECT`.
- Use the common principal conversion.
- Configure explicit origin allowlist.
- Allow subscribe only to `/user/queue/notifications`.
- Deny direct broker destinations and client sends.
- Reconnect with a fresh credential at expiry.

### 8.4 Provider callbacks

Provider webhook authorization remains signature-based and is excluded from
user JWT enforcement. Browser success/failure redirect has no mutation power.

### Gate

- Authorization matrix is exhaustive.
- Endpoint tests cover anonymous `401`, invalid token `401`, missing permission
  `403`, wrong owner `404/403` per matrix and success.
- Internal matrix tests cover user token, service token for wrong audience,
  wrong client, wrong permission and valid caller.
- WebSocket CONNECT/SUBSCRIBE/SEND tests pass.
- Audit records exist for security-critical, financial and moderation actions.

## 9. Phase 4 — plans, subscriptions and billing

### Work order

1. Approve the feature registry, composition rules and illustrative catalog in
   `docs/commerce/plan-catalog-example.yaml`.
2. Add commerce migrations for:
   - USER subscription subjects and subject entitlement state;
   - product lines, one `BASE` slot, immutable plan/billing-policy versions,
     localized copy, offers, prices and entitlements;
   - subscriptions/current subject pointers/scheduled changes;
   - provider references and publication jobs;
   - auth eligibility projection/inbox;
   - idempotent commands;
   - webhook inbox;
   - audit and outbox state.
3. Consume full auth authorization snapshots for plan eligibility with inbox
   and monotonic version handling.
4. Implement audited draft/validate/publish plan workflow. Stripe Product/Price
   provisioning runs as idempotent retryable work outside the DB transaction;
   only a reconciled version becomes saleable.
5. Seed the illustrative `LEARNER_FREE` and the audited `DEFAULT_USER_BASE`
   system binding using a repeatable, reviewed mechanism.
6. Implement free provisioning from onboarding without automatic plan changes
   when `INSTRUCTOR` is later granted.
7. Implement the Stripe port/adapter and hosted Checkout Session flow for the
   SPA, pinning API version and handling VND with exponent zero.
8. Implement self-service read/checkout-session/change/cancel/resume APIs.
9. Implement staff adjustment with MFA, reason and expiry; it cannot bypass
   eligibility or mutate roles.
10. Add explicit gateway routes, public/admin OpenAPI and service-catalog
   ownership for plans, subscriptions and billing webhooks; keep
   `/internal/**` absent from gateway.
11. Recheck eligibility before provider-confirmed activation and implement
    provider reconciliation for pending/non-terminal records.
12. Exercise the full flow in Stripe sandbox/test mode.
13. Before live enablement, verify the merchant entity/account country, bank,
    tax setup, VND recurring capability and payment methods on the actual Stripe
    account. Test mode is not evidence that a Vietnam entity can go live.

Keep `cinx.subscription.billing-enabled=false` until merchant eligibility,
callback verification, reconciliation and the deferred stabilization integration
suite pass. A fake/local adapter may support development but MUST be impossible
to select in production.

### Gate

- Published product-line eligibility/slot, plan versions, billing-policy
  versions, prices and entitlements cannot mutate.
- Only USER subjects are accepted and one current `BASE` subscription holds
  under concurrency.
- Two concurrent commands with different idempotency keys cannot create two
  non-terminal provider subscriptions; provider retries use the stable CINX
  command ID.
- All lifecycle transitions and illegal transitions are tested.
- Same idempotency key/fingerprint returns the same result; a different
  fingerprint returns conflict.
- Provider callback tests cover signature, timestamp, replay, duplicate,
  payload mismatch and out-of-order state.
- VND `99,000` maps to Stripe minor amount `99000`, and fractional VND is
  rejected.
- A learner cannot start a creator checkout; a verified instructor can; role
  loss between Checkout and callback prevents activation.
- No subscription command, webhook or adjustment changes auth DB/Keycloak or
  emits a role-grant command.
- No access activates from a browser redirect.
- Eligible free fallback, grace and upgrade/downgrade/cancel timing match the
  published billing-policy version.

## 10. Phase 5 — entitlement projections and course access

### Contract-first deployment

1. Add feature registry validation in CI.
2. Add
   `commerce.subscription.entitlements.changed` to AsyncAPI with examples,
   standard headers, retry/DLQ and compatibility metadata.
3. Add a persistent per-user entitlement-version parent row, child feature
   rows and inbox migrations to every consumer.
4. For recommendation, add an Alembic baseline before the projection.
5. Implement handlers with atomic inbox claim and monotonic version handling.
6. Deploy consumers and bindings while the producer switch is off.
7. Deploy commerce snapshot publisher/outbox relay.
8. Publish synthetic/sandbox snapshots and verify each projection.
9. Enable real publication.
10. Enable local entitlement enforcement per service.

Consumers deploy before the producer. A full snapshot with a higher
per-user `entitlementVersion`, backed by the USER subject entitlement state,
replaces local state; duplicate/lower versions are successful no-ops. A
per-subscription aggregate version is insufficient when the `BASE` pointer
switches from a paid record back to an older free record. The parent
version/freshness row remains when `features=[]`; the inbox is only event-ID
deduplication and never substitutes for that current pointer.

### Learning cutover

1. Add the next learning migration for `course_access_policy`.
2. Replace `isInSubscription` with explicit:
   `FREE`, `PURCHASE_ONLY`, `SUBSCRIPTION_ONLY`,
   `PURCHASE_OR_SUBSCRIPTION`.
3. Restrict commercial policy changes to
   `learning.course.subscription-policy.manage.any`.
4. Separate permanent purchase/admin grants from enrollment.
5. Preserve progress/certificates independently from current access.
6. Update order-paid consumer to create an idempotent permanent purchase
   grant.
7. Centralize one course access resolver and use it for lesson, assessment,
   subtitle/download and enrollment boundaries.

For a data reset, seed every course with an explicit access policy. Never rely
on a nullable Boolean/default that silently opens protected content.

### Gate

- Each consumer converges after duplicate, out-of-order and replayed snapshots.
- An empty-feature snapshot advances the parent version, removes stale child
  rows and still prevents an older non-empty snapshot from being reapplied.
- Broker outage does not lose a committed commerce change.
- Projection lag and staleness are observable.
- Every access model is tested for anonymous, owner, permanent buyer, active
  subscriber, expired subscriber and unrelated user.
- Purchase survives expiry; progress survives expiry; expired subscription
  does not permit new protected access.
- Commerce unavailability does not break a request with a fresh local
  projection.

## 11. Phase 6 — quota enforcement

### Work

1. Add quota counter, reservation and ledger migration in each metered service.
2. Implement a reusable service-local quota component, not a shared database.
3. Reserve in a short transaction, execute work, then commit/release.
4. Use a unique user/feature/business-operation key plus a versioned canonical
   request fingerprint; same key with different inputs must return conflict.
5. Make `COMMIT`, `RELEASE` and `EXPIRE` compare-and-set transitions from
   `RESERVED`, with the counter and deterministic ledger command written in the
   same transaction.
6. Add reservation expiry and a monitored recovery job.
7. For streaming/external cost, document when partial output commits usage.
8. Add `429 QUOTA_EXCEEDED`, reset metadata and `Retry-After`.
9. Enable quota first in shadow mode:
   calculate/log a would-deny metric without exposing sensitive details.
10. Compare counters with expected usage, then enable hard enforcement.

Shadow mode MUST NOT call an expensive provider without an actual safety/rate
limit. It only delays contractual quota denial.

### Gate

- Two concurrent calls for the last unit cannot both reserve.
- Retried operation does not double-charge.
- Reusing an operation key with a different fingerprint or units returns
  `409`; it never reuses the original reservation.
- Crash after reserve is recovered.
- A late commit racing expiry cannot both release and consume quota; exactly one
  terminal compare-and-set and ledger entry wins.
- Upgrade raises limit without resetting usage.
- Scheduled downgrade uses the next period.
- Immediate revoke prevents new reservations.
- Boundary/reset tests use injected `Clock`.

## 12. Phase 7 — hardening and cleanup

1. Remove legacy realm-role fallback and rollout flags.
2. Remove `isInSubscription`, single-role APIs and old event handlers.
3. Prove all internal routes are absent from gateway routing.
4. Run entitlement replay/reconciliation and resolve version gaps.
5. Verify bounded retry, publisher confirms, returns and every DLQ.
6. Set retention/cleanup jobs for webhook inbox, auth audit, outbox/inbox,
   quota ledger and expired reservations.
7. Exercise break-glass access and post-use review.
8. Perform production-like load/concurrency/security tests.
9. Update the service catalog, interaction map, runbooks and release review
    with implemented rather than proposed behavior.

## 13. Exact verification commands

Run from repository root unless the command changes directory.

Current development gate: unit tests, compilation and static/contract
validation only. Do not pass `-Integration`, activate `-Pintegration`, start
Testcontainers or depend on live databases, brokers, Keycloak or Stripe unless
the user explicitly requests the deferred stabilization phase.

### Repository-aware Java verification

Use the repository helper:

```powershell
.\scripts\verify-change.ps1 common-security,gateway,auth,user
.\scripts\verify-change.ps1 commerce,learning,notification,reporting
```

Equivalent Maven commands:

```powershell
mvn -pl common-security,gateway,auth,user -am test
mvn -pl commerce,learning,notification,reporting -am test
mvn test
```

For a change scoped to one module, still use `-am`. Changes to
`common-security` require all gateway/resource-server consumers to be
compiled/tested. Event changes require the producer and every consumer.

### Contracts and Keycloak

```powershell
python -m json.tool keycloak/realm/cinx-realm.json
npx --yes @asyncapi/cli@5.0.7 validate contracts/asyncapi/cinx-events.yaml
npx --yes @redocly/cli@2.36.0 lint --extends=minimal contracts/openapi/cinx-internal.yaml
```

Add schema validation for `contracts/entitlements/cinx-features.yaml` when the
registry is introduced. Contract CI must run producer/consumer tests as well
as document lint.

### Python services

```powershell
Set-Location recommendation
python -m compileall app
python -m pytest

Set-Location ..\subtitle-ai
python -m compileall app
python -m unittest discover -s tests
```

Run them inside the supported image/runtime when local Python does not match
the project. A missing dependency is not a passing test.

### Compose

Using an existing ignored local environment file:

```powershell
docker compose --env-file env/compose.env config --quiet
```

Do not read or print secrets during verification.

### Deferred stabilization commands

Keep these commands as backlog; they are not part of the current PR Definition
of Done:

```powershell
.\scripts\verify-change.ps1 auth,user,commerce,learning,notification,reporting -Integration
mvn -pl auth,user,commerce,learning,notification,reporting -am -Pintegration verify
mvn -Pintegration verify
```

## 14. Deferred integration scenarios

The scenarios below become mandatory after the project is declared stable and
before production release. They are not current development gates. Until then,
cover the same domain/security decisions as far as practical with focused unit
tests, mocks and fakes, and report the integration scenarios as unrun backlog.

### Authentication and authorization

- browser Authorization Code + PKCE login;
- issuer/audience/expiry/clock-skew validation;
- multi-role permission composition;
- malformed token claims fail closed;
- auth-to-Keycloak admin calls failing as a whole;
- measured status/`changedAt` token denial within the five-second objective;
- all matrix decisions, including self/own/any;
- internal client credentials for every caller;
- WebSocket origin, CONNECT, SUBSCRIBE, SEND and expiry.

### Subscription and provider

- fresh `LEARNER_FREE` provisioning and duplicate onboarding;
- Stripe Checkout command retry before/after provider timeout;
- server-resolved offer/Price, relative-return-path allowlist and SPA polling;
- VND zero-decimal amount conversion;
- learner/instructor plan eligibility and no role mutation from billing;
- instructor role revoke between checkout and callback;
- provider-signed activation;
- duplicate, replayed and stale provider event;
- upgrade, scheduled downgrade, cancel/resume and renewal failure;
- provider reconciliation repairs uncertain local state;
- full entitlement snapshot and every consumer;
- permanent purchase plus subscription overlap and expiry.

### Messaging

- broker unavailable during outbox relay;
- publisher nack/return does not mark published;
- consumer crash before/after commit;
- duplicate `eventId`;
- out-of-order per-user `entitlementVersion`;
- bounded retry reaches the declared DLQ;
- replay/reconciliation converges without duplicate effects;
- standard headers, `traceparent` and `X-Request-Id` propagate.

### Quota

- exact boundary and reset instant;
- concurrent last-unit race;
- duplicate operation key;
- duplicate key with mismatched fingerprint/units;
- reservation timeout/recovery;
- commit-versus-expire race and late commit after expiry;
- partial stream/provider failure settlement;
- upgrade/downgrade/revoke while usage exists.

## 15. Deployment and rollback order

For each new event or claim:

```text
contract
  -> tolerant reader/converter
  -> consumer schema + consumer
  -> producer/writer behind switch
  -> reconciliation
  -> enforcement
  -> remove legacy path
```

Safe rollback examples:

- stop new subscription checkout while continuing signed webhook processing
  and reconciliation;
- disable new entitlement production while consumers retain the last fresh
  projection;
- disable quota hard denial while retaining accounting, only when abuse/cost
  controls remain;
- revert an authorization expression only to a previously tested secure rule,
  never to `permitAll`.

Unsafe rollback examples:

- rolling back consumer code after new event/schema writes;
- turning off webhook processing while provider events continue;
- changing a plan version in place;
- deleting a failed inbox/outbox row to make a dashboard green;
- accepting a user token on an internal route.

Database rollback after destructive writes requires a designed restore/forward
fix. Prefer additive migrations and forward repair.

## 16. Release evidence checklist

The future production release candidate, after stabilization, must include:

- approved product decision record;
- complete authorization matrix;
- rendered OpenAPI/validated AsyncAPI/feature registry;
- Keycloak configuration diff and semantic validation;
- migration inventory and fresh/upgrade schema results;
- exact test commands and logs;
- provider sandbox evidence with redacted identifiers;
- producer/consumer compatibility results;
- security tests for every trust boundary;
- quota concurrency/load results;
- observability dashboard and alert links;
- reconciliation and incident runbooks;
- list of disabled flags, with all temporary bypass flags removed or justified.

Production release is allowed only when:

- all blockers in the current final review are closed;
- root unit gates and the deferred stabilization integration gates pass;
- no protected operation is absent from the matrix;
- billing access is activated only by verified provider state;
- service-to-service routes require service identity;
- entitlement projections converge under at-least-once delivery;
- purchase/progress retention semantics are proven;
- quota is concurrency-safe and idempotent;
- secrets and payment data are handled according to the documented boundary.
