# CINX authentication and authorization specification

Status: **proposed target design**

Applies to: gateway, Keycloak, auth, common-security, all Java resource
servers, recommendation HTTP/SSE and notification WebSocket.

This document is normative for the target implementation. It deliberately
describes a breaking pre-release cutover; current code still has the gaps
listed in the final review.

## 1. Goals and non-goals

### Goals

- One authentication contract for browser, Java APIs, Python APIs and
  WebSocket.
- Additive multi-role assignments with stable fine-grained permissions.
- Permission checks at application boundaries plus ownership/resource-state
  checks in the owning service.
- Explicit service identity for every `/internal/**` operation.
- A clear separation between security permission, subscription feature access
  and quota.
- Auditable role/status changes, with Keycloak as the owner of roles and sessions.
- Consistent `401`, `403`, `404`, `409` and `429` behavior.

### Non-goals

- This design does not implement a generic policy-language engine.
- Runtime user-created custom roles are not supported in v1.
- Plan tiers are not security roles.
- The gateway is not the final authorization boundary.
- Network location, RabbitMQ access or an `X-User-Id` header is not proof of
  identity.

## 2. Terminology

| Term | Meaning |
|---|---|
| Authentication | Proves who the caller is and which client issued the request. |
| Role | Human-manageable bundle such as `INSTRUCTOR` or `COMMUNITY_MODERATOR`. |
| Permission | Stable application capability such as `learning.course.update.own`. |
| OAuth scope | Protocol/client consent scope such as `openid`; not a business permission. |
| Relationship | Link between subject and object, for example course owner or session owner. |
| Attribute | Subject, object or environment state used by a policy, for example course status. |
| Entitlement | Feature access granted by a commercial subscription. |
| Quota | Consumable limit attached to a feature entitlement. |
| User token | Access token representing a human user, with `token_kind=user`. |
| Service token | Client-credentials token representing a backend, with `token_kind=service`. |

`ROLE_*`, `PERM_*` and `SCOPE_*` are Spring runtime authority namespaces.
Their prefixes MUST NOT be stored in business tables:

```text
stored role:       INSTRUCTOR
JWT role:          INSTRUCTOR
runtime authority: ROLE_INSTRUCTOR

stored permission:       learning.course.update.own
JWT permission:          learning.course.update.own
runtime authority:       PERM_learning.course.update.own
```

## 3. Source of truth and single-writer rules

| State | Source of truth | Projection/consumer |
|---|---|---|
| Credentials, MFA, login sessions | Keycloak | None |
| Account status (`ACTIVE`/`BANNED`, ban fields) | Auth DB (`identity_account`) | Keycloak `enabled` flag, set by auth in the same command |
| User-to-role assignment | Keycloak realm roles | Token `roles`/`permissions` claims; auth announces changes |
| Time of the last authorization change | Auth DB (`identity_account.changed_at`) | Token freshness check, event ordering |
| CINX user id | Keycloak `sub` | Every service; there is no separate id |
| Role/permission definitions and composites | Canonical version-controlled authorization catalog | Auth validation and Keycloak realm composites |
| OAuth clients, flows and token mappers | Version-controlled Keycloak realm config | Deployed Keycloak |
| Effective token claims | Keycloak | Gateway and resource services |
| Resource ownership and state | Owning service DB | None |
| Plan/subscription/feature grants | Commerce DB | Local feature projections |
| Quota usage/reservations | Feature-consuming service DB | Reporting metrics only |

Keycloak owns realm roles. `LEARNER` comes from the realm default roles, so
every new user has it from the first token. Production administrators change
ordinary CINX role assignments through auth (`PUT /api/v1/identity/accounts/{userId}/roles`),
which writes Keycloak through the admin API and records the audit row and the
event. `INSTRUCTOR` is added and removed by auth from the user-domain
`user.instructor.verified` / `user.instructor.terminated` events.
`BREAK_GLASS_ADMIN` needs MFA, alerting and post-use review, and has no expiry:
time-limited role grants do not exist, so the role is removed by an explicit
revoke. A separate Keycloak infrastructure administrator may repair the
identity platform, but SHOULD NOT create ordinary CINX role assignments by
hand; auth does not detect or reconcile such a change, and it takes effect on
the user's next token.

Auth DB does not need an `account_permission` table in v1. Effective
permissions are derived from version-controlled role composites. Direct
per-user permission grants SHOULD NOT be introduced until a real use case
cannot be represented by a role.

## 4. Authorization decision model

CINX uses a hybrid model:

- RBAC manages role assignment.
- PBAC checks stable `PERM_*` capabilities.
- ReBAC checks ownership and relationships.
- ABAC checks resource and environment attributes.
- Commercial entitlement and quota are independent decision layers.

A role can be an input to commercial purchase eligibility without becoming a
commercial grant. For example, a creator plan requires a pre-existing verified
`INSTRUCTOR` role, but payment never creates that role. Conversely, losing a
plan never revokes `INSTRUCTOR`; only the user/auth partnership workflow can do
that. Every premium instructor operation still requires both its security
permission/relationship and any declared feature/quota.

The normative evaluation order is:

```text
1. Verify signature, issuer, audience, algorithm and exp/nbf.
2. Verify caller type: user, service or provider webhook.
3. Check required PERM_* capability.
4. Check ownership/relationship.
5. Check resource/business attributes.
6. Check active commercial feature entitlement when required.
7. Atomically reserve or consume quota when required.
8. Perform the use case and persist audit/outbox state.
```

Example:

```text
Can update course =
  PERM_learning.course.update.any
  OR (
    PERM_learning.course.update.own
    AND active course membership for principal.sub has role OWNER, MANAGER or EDITOR
    AND course.status in [DRAFT, REJECTED]
  )
```

Example with subscription:

```text
Can use advanced recommendation =
  PERM_recommendation.agent.use
  AND session.userId == principal.sub
  AND feature recommendation.agent.advanced is active
  AND recommendation.agent.requests quota can be reserved
```

## 5. Trust boundaries

```mermaid
flowchart LR
    Browser -->|Authorization Code + PKCE| Keycloak
    Keycloak -->|user access token| Browser
    Browser -->|Bearer token| Gateway
    Gateway -->|validated user token| Service[Owning resource service]
    Service --> Permission[Permission check]
    Permission --> Policy[Ownership and state policy]
    Policy --> Feature[Subscription feature check]
    Feature --> Quota[Atomic quota operation]
    Quota --> UseCase[Business use case]

    Caller[Caller service] -->|client_credentials| Keycloak
    Keycloak -->|target-audience service token| Caller
    Caller -->|Bearer service token| Internal[/internal endpoint/]

    Provider[Payment provider] -->|signed raw webhook| Commerce
```

Every resource service validates its token again. Gateway validation is
defense in depth and coarse route protection, not delegated authorization.

## 6. Browser authentication

The web client MUST use OpenID Connect Authorization Code with PKCE `S256`:

1. Redirect to the Keycloak authorization endpoint.
2. Use exact registered redirect URIs outside localhost.
3. Bind transaction-specific PKCE, `state` and OIDC `nonce`.
4. Exchange the code directly with Keycloak.
5. Send only the access token to CINX APIs.
6. Keep refresh tokens in the client security boundary; never forward them to
   APIs or place them in URLs/logs.

The confirmed v1 frontend is a pure SPA and `cinx-web` is a public OIDC client:

- it has no client secret;
- access/refresh tokens stay in memory and are not written to URLs, logs or
  persistent browser storage;
- CINX APIs accept bearer access tokens, not an application session cookie;
- API CSRF protection may be disabled only for this stateless bearer-token
  chain; explicit CORS origins remain mandatory;
- logout clears in-memory credentials and invokes the Keycloak end-session
  flow;
- WebSocket authentication follows the STOMP CONNECT/reconnect contract below.

Implicit and Resource Owner Password Credentials flows MUST remain disabled.
The current realm already disables direct access grants for `cinx-web` and
uses PKCE. Production configuration MUST replace wildcard redirect URIs with
exact deployed URIs.

The access-token lifetime is currently 300 seconds. Keep a short lifetime;
refresh-token rotation/reuse detection SHOULD be enabled for deployed clients.

Reference: [OAuth 2.0 Security Best Current Practice, RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html).

## 7. Identity onboarding

The CINX user id is the Keycloak subject, always:

```text
userId = Keycloak sub
```

There is no `uid` claim or user-profile attribute. The flow is:

1. User registers/logs in through Keycloak. The first token already carries
   `LEARNER` (realm default role) and its permissions, so it works on every
   route.
2. `GET /api/v1/profiles/me` returns `404` until a profile exists; the web app
   then routes to `/onboarding`.
3. `POST /api/v1/profiles/me/onboarding` derives identity from the token and
   creates the user profile. The SPA keeps the same token; no re-sign-in.
4. Auth takes no part in onboarding and creates no account row. An
   `identity_account` row (`user_id` = `sub`) is created lazily on the first
   admin action or instructor event for that user.
5. Instructor application is a separate user-domain workflow. Only a verified
   application adds `INSTRUCTOR`; it does not replace `LEARNER`.

`INSTRUCTOR_APPLICANT` MUST NOT remain an authorization role.
Granting `INSTRUCTOR` only makes instructor-targeted plans eligible. It never
buys or upgrades one. Buying, renewing, canceling or adjusting a subscription
never calls an auth role-grant API and cannot create `INSTRUCTOR`.

## 8. Keycloak target configuration

### Clients

| Client | Type | Purpose |
|---|---|---|
| `cinx-web` | Public, Authorization Code + PKCE | Browser login |
| `cinx-api` | Resource client | User-facing permission catalog and audience |
| `cinx-auth-admin` | Confidential service account | Restricted Keycloak user administration only |
| `cinx-gateway-service` | Confidential | Gateway token-freshness state reads only |
| `cinx-auth-service` | Confidential | Auth outbound service calls |
| `cinx-user-service` | Confidential | User outbound service calls |
| `cinx-commerce-service` | Confidential | Commerce outbound service calls |
| `cinx-learning-service` | Confidential | Learning outbound service calls |
| `cinx-notification-service` | Confidential | Notification outbound service calls |
| `cinx-reporting-service` | Confidential | Reporting outbound service calls |
| `cinx-recommendation-service` | Confidential | Recommendation outbound service calls |

Create target-specific internal resource/audiences:

```text
cinx-auth-internal
cinx-user-internal
cinx-commerce-internal
cinx-learning-internal
cinx-notification-internal
cinx-reporting-internal
```

Only create a client/caller mapping when a real call exists. Never reuse one
shared client secret for all services.

Each `cinx-<target>-internal` resource owns its internal permission client
roles. An audience/permission mapper flattens only the service account's roles
for that target into the explicit `permissions` claim. `cinx-web` has no role
scope mapping for internal clients and cannot request an internal audience.

### Roles and permissions

Human-facing application roles are additive composite roles:

| Role | Purpose |
|---|---|
| `LEARNER` | Default user persona |
| `INSTRUCTOR` | Manage own courses and learner submissions |
| `INSTRUCTOR_OPERATIONS` | Review instructor applications and partnerships |
| `CONTENT_REVIEWER` | Review and publish course content |
| `CATALOG_MANAGER` | Manage commercial plans and course subscription policy |
| `COMMUNITY_MODERATOR` | Moderate reports and community content |
| `POLICY_MANAGER` | Manage policy content |
| `PROMOTION_MANAGER` | Manage vouchers and promotions |
| `SUPPORT_AGENT` | Limited cross-user support reads/actions |
| `IDENTITY_ADMIN` | Account status and role administration |
| `FINANCE_OPERATOR` | Reconciliation/refund/finance operations |
| `REPORT_ANALYST` | Reporting reads |
| `NOTIFICATION_OPERATOR` | Send audited platform broadcasts |
| `AI_OPERATOR` | Restricted recommendation support/debug operations |
| `PLATFORM_ADMIN` | Exact composite defined in the authorization catalog |
| `BREAK_GLASS_ADMIN` | Emergency-only, MFA, removed by an explicit revoke |

User-facing permissions are client roles of `cinx-api`. Human roles are
composites of those client roles. Internal permissions are client roles of
the target resource client such as `cinx-auth-internal`; they are never roles
of `cinx-api`.

The canonical catalog MUST be one version-controlled machine-readable file,
for example:

```text
contracts/security/cinx-authorization.yaml
```

It owns `policyVersion`, permission definitions, exact role bundles, target
internal resources and service-account grants. Keycloak realm composites are
generated from or semantically validated against this catalog. Auth loads the
same artifact to validate grants, calculate permission membership and include
the policy version in audit/events. Do not maintain a second hand-written
role-to-permission map in Java.

Target v1 bundles:

```yaml
roles:
  LEARNER:
    permissions:
      - user.profile.read.self
      - user.profile.update.self
      - user.instructor.apply.self
      - commerce.cart.manage.self
      - commerce.wishlist.manage.self
      - commerce.order.create.self
      - commerce.order.read.self
      - commerce.subscription.read.self
      - commerce.subscription.change.self
      - learning.progress.read.self
      - learning.progress.write.self
      - recommendation.agent.use
      - recommendation.session.read.self
      - recommendation.session.write.self
      - notification.read.self
      - notification.update.self

  INSTRUCTOR:
    permissions:
      - learning.course.create
      - learning.course.update.own
      - learning.curriculum.update.own
      - learning.progress.read.own-course
      - learning.submission.grade.own
      - learning.analytics.read.own

  INSTRUCTOR_OPERATIONS:
    permissions:
      - user.instructor.review.any

  CONTENT_REVIEWER:
    permissions:
      - learning.course.review.any
      - learning.course.publish.any

  CATALOG_MANAGER:
    permissions:
      - commerce.plan.manage.any
      - learning.course.subscription-policy.manage.any

  COMMUNITY_MODERATOR:
    permissions:
      - learning.community.moderate.any

  POLICY_MANAGER:
    permissions:
      - user.policy.manage.any
      - user.policy.publish.any

  PROMOTION_MANAGER:
    permissions:
      - commerce.voucher.manage.any

  SUPPORT_AGENT:
    permissions:
      - auth.account.read.any
      - user.profile.read.any
      - commerce.order.read.any
      - recommendation.user.read.any

  IDENTITY_ADMIN:
    permissions:
      - auth.account.read.any
      - auth.account.suspend.any
      - auth.account.reactivate.any
      - auth.role.assign.any

  FINANCE_OPERATOR:
    permissions:
      - commerce.order.read.any
      - commerce.subscription.adjust.any
      - commerce.payment.reconcile.any
      - commerce.refund.manage.any
      - reporting.finance.read

  REPORT_ANALYST:
    permissions:
      - learning.analytics.read.any
      - reporting.dashboard.read

  NOTIFICATION_OPERATOR:
    permissions:
      - notification.broadcast.send.any

  AI_OPERATOR:
    permissions:
      - recommendation.user.read.any
      - recommendation.agent.debug

  PLATFORM_ADMIN:
    includes:
      - INSTRUCTOR_OPERATIONS
      - CONTENT_REVIEWER
      - CATALOG_MANAGER
      - COMMUNITY_MODERATOR
      - POLICY_MANAGER
      - PROMOTION_MANAGER
      - SUPPORT_AGENT
      - IDENTITY_ADMIN
      - FINANCE_OPERATOR
      - REPORT_ANALYST
      - NOTIFICATION_OPERATOR
      - AI_OPERATOR

  BREAK_GLASS_ADMIN:
    includes: [PLATFORM_ADMIN]
    permissions:
      - user.profile.update.any
      - learning.course.update.any
```

Initial service-account grants:

| Target resource | Caller | Permission |
|---|---|---|
| `cinx-auth-internal` | `cinx-notification-service` | `auth.internal.permission-members.read` |
| `cinx-auth-internal` | `cinx-gateway-service`, `cinx-user-service`, `cinx-commerce-service`, `cinx-learning-service`, `cinx-notification-service`, `cinx-reporting-service`, `cinx-recommendation-service` | `auth.internal.authorization-state.read` |
| `cinx-user-internal` | `cinx-learning-service`, `cinx-notification-service` | `user.internal.profile.read` |
| `cinx-commerce-internal` | `cinx-learning-service`, `cinx-recommendation-service` | `commerce.internal.entitlement.read` |
| `cinx-learning-internal` | `cinx-commerce-service`, `cinx-notification-service`, `cinx-recommendation-service` | `learning.internal.course.read` |
| `cinx-learning-internal` | `cinx-notification-service`, `cinx-reporting-service` | `learning.internal.enrollment.read` |

Add a caller only when an active contract and call exist. The authorization
matrix and internal OpenAPI must agree with this table before release.

Role-assignment delegation is also catalog policy:

- `LEARNER` comes from the Keycloak realm default roles; no command grants it.
- `INSTRUCTOR` is granted/revoked only from a verified user-domain
  partnership workflow.
- `IDENTITY_ADMIN` may grant ordinary staff bundles to another user with MFA,
  reason and audit, but cannot grant to self.
- `PLATFORM_ADMIN` requires two distinct approved actors, at least one current
  platform administrator, and last-admin protection.
- `BREAK_GLASS_ADMIN` requires two-person approval, incident reference,
  immediate alert and post-use review. The role has no automatic expiry.
- No human grant flow can grant a service/internal permission.

`auth.role.assign.any` authorizes entry into this policy; it does not mean the
caller may grant every role.

`LEARNER` and `INSTRUCTOR` remain additive; granting `INSTRUCTOR` does not
implicitly remove or replace `LEARNER`. `notification.test.send.any` has no
production human bundle because test endpoints are disabled in production.
Internal permissions have service-account grants in the same catalog and can
never be included by a human role, `PLATFORM_ADMIN` or
`BREAK_GLASS_ADMIN`.

`PLATFORM_ADMIN` MUST NOT imply Keycloak `realm-management`.

The `cinx-auth-admin` service account currently has broad view/query
permissions. Reduce it to user lookup, role mapping and enable/disable
operations actually used by auth.

Keycloak supports realm roles, client roles, composite roles and service
account roles; see the
[Keycloak Server Administration Guide](https://www.keycloak.org/docs/latest/server_admin/).

### MFA

- Email verification is required for normal accounts.
- Staff roles MUST use an MFA-required authentication flow.
- `FINANCE_OPERATOR`, `IDENTITY_ADMIN`, `PLATFORM_ADMIN` and
  `BREAK_GLASS_ADMIN` SHOULD require phishing-resistant WebAuthn when
  operationally possible.
- Break-glass access MUST have alerting and post-use review.

`mfaRequired: true` in the authorization matrix is a runtime step-up rule, not
documentation only:

```text
acr       = urn:cinx:loa:2
amr       contains an approved MFA method
auth_time is no older than 15 minutes
```

Keycloak emits `acr`, `amr` and `auth_time` in the access token. A shared
authorization component validates the configured ACR, allowed method and
freshness. If the token is valid but insufficient/stale, return
`403 MFA_STEP_UP_REQUIRED`; the SPA starts a fresh OIDC authorization request
with the required `acr_values` and `max_age`. The 15-minute default is
configuration and must be identical across gateway documentation and
resource services. A staff role alone never proves recent MFA.

## 9. Permission catalog

Permission names use:

```text
<service>.<resource>.<action>[.<scope>]
```

Initial catalog:

```text
auth.account.read.any
auth.account.suspend.any
auth.account.reactivate.any
auth.role.assign.any
auth.internal.permission-members.read
auth.internal.authorization-state.read

user.profile.read.self
user.profile.read.any
user.profile.update.self
user.profile.update.any
user.instructor.apply.self
user.instructor.review.any
user.policy.manage.any
user.policy.publish.any
user.internal.profile.read

commerce.cart.manage.self
commerce.wishlist.manage.self
commerce.order.create.self
commerce.order.read.self
commerce.order.read.any
commerce.voucher.manage.any
commerce.plan.manage.any
commerce.subscription.read.self
commerce.subscription.change.self
commerce.subscription.adjust.any
commerce.payment.reconcile.any
commerce.refund.manage.any
commerce.internal.entitlement.read

learning.course.create
learning.course.update.own
learning.course.update.any
learning.curriculum.update.own
learning.course.review.any
learning.course.publish.any
learning.course.subscription-policy.manage.any
learning.progress.read.self
learning.progress.write.self
learning.progress.read.own-course
learning.submission.grade.own
learning.community.moderate.any
learning.analytics.read.own
learning.analytics.read.any
learning.internal.course.read
learning.internal.enrollment.read

recommendation.agent.use
recommendation.session.read.self
recommendation.session.write.self
recommendation.user.read.any
recommendation.agent.debug

notification.read.self
notification.update.self
notification.test.send.any
notification.broadcast.send.any

reporting.dashboard.read
reporting.finance.read
```

The machine-readable operation mapping is in
[`authorization-matrix.yaml`](./authorization-matrix.yaml). Every new
operation MUST be added there before implementation is complete.

## 10. JWT contracts

### User access token

```json
{
  "iss": "https://identity.example/realms/cinx",
  "sub": "keycloak-subject",
  "aud": ["cinx-api"],
  "azp": "cinx-web",
  "token_kind": "user",
  "roles": ["LEARNER", "INSTRUCTOR"],
  "permissions": [
    "learning.course.create",
    "learning.course.update.own"
  ],
  "scope": "openid profile email",
  "acr": "urn:cinx:loa:2",
  "amr": ["pwd", "otp"],
  "auth_time": 1785479100,
  "exp": 1785480000
}
```

### Service token

```json
{
  "iss": "https://identity.example/realms/cinx",
  "sub": "service-account-cinx-notification-service",
  "azp": "cinx-notification-service",
  "aud": ["cinx-auth-internal"],
  "token_kind": "service",
  "permissions": [
    "auth.internal.permission-members.read"
  ],
  "exp": 1785480000
}
```

### Claim rules

- `sub` is the CINX user id and `Authentication.name`. There is no `uid`
  claim.
- `roles` contains only CINX human/composite role names.
- `permissions` contains effective application permission names without
  prefixes.
- `scope` remains a space-delimited OAuth scope claim.
- `iat` is compared with the user's last authorization change (section 18);
  no per-user version or deadline claim exists.
- `acr`, `amr` and `auth_time` support runtime step-up checks.
- A plan claim, if emitted for UI convenience, MUST NOT become an authority or
  the final entitlement decision.
- Permission/role claims belong in the access token. They are not required in
  the ID token.

### Validation

All resource servers MUST validate:

- configured signing algorithm and signature;
- exact issuer;
- required target audience;
- `exp` and `nbf`;
- `token_kind`;
- caller/client constraints for internal routes.

Public user chains accept `token_kind=user` and `aud=cinx-api`. Internal
chains accept only `token_kind=service` and their target internal audience.
A service token MUST NOT be accepted as a human user merely because it has a
valid Keycloak signature.

A user token needs only `sub`; a new user's first token is valid on every
route.

## 11. `CinxJwtAuthenticationConverter`

`common-security` remains the single owner. Target mapping:

```text
roles[]       -> ROLE_*
permissions[] -> PERM_*
scope         -> SCOPE_*
sub           -> principal
```

Implementation requirements:

1. Preserve direct roles and documented realm-role compatibility during the
   cutover.
2. Only admit known CINX roles; do not create `ROLE_offline_access`,
   `ROLE_uma_authorization` or other infrastructure authorities.
3. Parse `permissions` as a collection of nonblank strings matching the
   permission naming pattern.
4. Prefer the explicit `permissions` claim. A configured fallback may read
   `resource_access[expectedResourceClient].roles` only for the resource
   expected by that filter chain: `cinx-api` for user APIs or the exact target
   internal client for service APIs. It MUST NOT merge arbitrary clients.
5. Deduplicate authorities.
6. Never map plan, subscription or feature keys to `GrantedAuthority`.
7. Keep the converter transport-neutral; service-specific policies do not
   belong in `common-security`.

Required tests:

- direct and exact-client roles/permissions;
- `sub` as the principal;
- malformed, blank and duplicate claims;
- scope never becomes a role/permission;
- infrastructure roles are excluded;
- plan/subscription claims are ignored;
- service token contains permissions but no human role requirement.

## 12. Enforcement in Java and Python

### Gateway

- Deny `/internal/**`.
- Validate user tokens for protected public routes.
- Keep only truly public catalog/policy/identity-provider endpoints
  anonymous.
- Do not rely on gateway routing to authorize a domain operation.

### Java services

Use request-level catch-all authentication and method-level permission checks:

```java
@PreAuthorize("""
    hasAnyAuthority(
        'PERM_learning.course.update.own',
        'PERM_learning.course.update.any'
    )
    """)
public CourseResponse updateCourse(
        String currentUserId,
        String courseId,
        UpdateCourseRequest request
) {
    Course course = courseRepository.findByIdOrThrow(courseId);
    courseAuthorizationPolicy.ensureCanUpdate(currentUserId, course);
    // mutation
}
```

Keep nontrivial ownership/resource-state logic in a policy bean or application
service within the owning service transaction. Avoid long, repository-calling
SpEL expressions. Spring method security supports fine-grained
`hasAuthority` and programmatic policies:
[Spring Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html).

`hasRole('ADMIN')` MUST NOT remain a universal bypass. Staff-any behavior uses
an explicit `.any` permission.

### Recommendation

- JWT is mandatory for protected routes and SSE handshake.
- Validate signature, issuer, audience, time and `token_kind=user`.
- Principal is `sub`. The token freshness check of section 18 applies
  (`recommendation/app/agent/auth.py`).
- Session/proposal queries use `(session_id, user_id)`.
- `/users/me` is the normal user API; arbitrary user reads require
  `recommendation.user.read.any`.
- Subscription and quota checks use local projections, not unverified token
  claims.
- Authentication, session ownership, entitlement and quota reservation finish
  before creating the `StreamingResponse`/sending SSE headers. Denials are
  normal HTTP ProblemDetail responses; only failures after dispatch become SSE
  error events and follow quota settlement policy.

## 13. Internal HTTP security

Each service with internal routes needs a higher-priority filter chain:

```text
/internal/**
  require service JWT
  require target internal audience
  require token_kind=service
  require endpoint-specific PERM_*
  deny all unmatched operations
```

The ordinary public chain MUST reject service tokens for user-owned commands.
The internal chain MUST reject browser/user tokens.

Outbound Feign/RestClient calls use OAuth2 client credentials and cache/renew
tokens by client registration. `X-User-Id`, when needed for actor context, is
accepted only after caller service authentication and is never authorization
proof.

User-context calls have a different contract:

- Forward the verified user token only when the target operation is a public
  user operation and the token audience permits it.
- Do not silently replace a user call with an all-powerful service token.
- If both user and service identity are required later, define token exchange
  or an explicit actor-context contract rather than trusting a header.

Example capability lookup:

```text
GET /internal/identity/permissions/{permission}/user-ids

audience:   cinx-auth-internal
token kind: service
permission: auth.internal.permission-members.read
```

Token freshness state:

```text
GET /internal/identity/accounts/{userId}/authorization-state

audience:   cinx-auth-internal
token kind: service
permission: auth.internal.authorization-state.read
response:   userId, status, changedAt, evaluatedAt
```

Callers use this only to check a token against the user's last authorization
change (section 18). It is not an endpoint for enumerating users or delegating
role management.

## 14. WebSocket authentication and authorization

Notification WebSocket MUST:

- use configured allowed origins, never `"*"`;
- require a bearer token in STOMP `CONNECT`;
- reject missing, invalid or expired tokens;
- use `CinxJwtAuthenticationConverter` so principal equals `sub`;
- permit subscription only to `/user/queue/notifications`;
- deny direct subscription to broker `/queue/**` and `/topic/**`;
- deny arbitrary client `SEND` to broker destinations;
- end with a deny-all message authorization rule.

Do not place access tokens in query strings. If a supported browser client
cannot send secure STOMP CONNECT headers, add an authenticated endpoint that
exchanges a user access token for a short-lived, one-time,
notification-audience WebSocket ticket.

The client reconnects with a fresh credential when the access token expires.
HTTP authentication alone is insufficient because WebSocket messages require
destination-level authorization.

## 15. Provider webhook authentication

Payment/billing callbacks are not user operations:

- They remain anonymous at the user-JWT layer.
- Provider name is allowlisted.
- A provider-specific adapter receives raw bytes and headers.
- Signature, timestamp and replay window are verified before parsing.
- Normalized provider event ID is durably deduplicated.
- Provider object version/timestamp prevents stale out-of-order transitions.
- A browser success redirect never activates an order/subscription.
- Webhook mutation, inbox record and outbox event commit atomically.

Never protect a webhook with `ROLE_ADMIN` or treat knowledge of order ID and
amount as authentication.

## 16. Auth persistence and admin commands

Auth is a thin admin layer over Keycloak. Roles live in Keycloak realm roles;
auth stores only status, ban state and the time of the last change.

```text
identity_account
  user_id PK                 Keycloak sub
  status                     ACTIVE | BANNED
  changed_at nullable        last role/status change
  ban fields                 reason, type, banned_at, ban_expires_at
  created_at
  updated_at
  version

authorization_audit
  id
  actor_type
  actor_id
  target_user_id
  action
  role_code nullable
  before_json
  after_json
  authorization_policy_version
  request_id
  trace_id
  occurred_at
```

A row is created lazily on the first admin action or instructor event for a
user, never at onboarding. There is no role table, sync-job table,
authorization version, reconciler or session revocation.

A role/status command (`IdentityService`):

1. Locks or creates the account row.
2. Calls Keycloak synchronously through the admin API (`KeycloakAdminClient`):
   role add/remove, or `enabled=false` on suspend/ban and `enabled=true` on
   reactivate. A Keycloak failure fails the command and nothing is committed.
3. In the same DB transaction sets `identity_account.changed_at`, writes the
   `authorization_audit` row and enqueues the `auth.authorization.changed`
   outbox event.

A disabled Keycloak user cannot sign in or refresh. A role change reaches a
user's next token through the freshness check in section 18.

`INSTRUCTOR` is added and removed from `user.instructor.verified` and
`user.instructor.terminated`. Auth no longer consumes `user.profile.onboarded`
and has no link endpoint. `PUT /api/v1/identity/accounts/{userId}/roles` takes
`{roles, reason}` with no expiry, and rejects `LEARNER`, `INSTRUCTOR`,
`PLATFORM_ADMIN` and `BREAK_GLASS_ADMIN`: those need a dedicated workflow. The
two-person approval described in section 8 for the last two is not built yet.

`GET /api/v1/identity/accounts/{userId}` returns
`{userId, email, status, roles, banExpiresAt, changedAt}`; email and roles are
read from Keycloak.

The legacy credential table in auth SHOULD be removed in the next migration;
Keycloak is the only password/OTP owner.

## 17. Authorization event

Routing key:

```text
auth.authorization.changed
```

Header:

```text
eventType: AuthAuthorizationChanged
eventVersion: 3
aggregateType: IdentityAccount
aggregateId: <userId>
```

Payload:

```json
{
  "userId": "user-123",
  "status": "ACTIVE",
  "roles": ["LEARNER", "INSTRUCTOR"],
  "permissions": [
    "learning.course.create",
    "learning.course.update.own"
  ],
  "changedAt": "2026-07-31T10:00:00Z"
}
```

Auth announces only changes, so a consumer treats a user it never heard of as
`ACTIVE`. Consumers use durable inbox processing and apply only a snapshot
whose `changedAt` is newer than the stored one (commerce
`SubscriptionLifecycleListener`, reporting `ReportingEventListener`).
Notification recipient lookup uses capability-based membership, for example
`learning.course.review.any`; notification preference remains a separate
notification-owned filter. `/internal/identity/permissions/{permission}/user-ids`
resolves the permission to catalog roles, then to the enabled Keycloak users
holding each role directly.

Commerce is also an authorized consumer of the status/roles snapshot for plan
eligibility. It checks that projection before creating provider state and
again before activating an instructor-targeted plan. The projection is
read-only commercial input: commerce never publishes a role command or treats
payment as identity verification.

A catalog change alters effective permissions through the Keycloak realm
composites. No campaign recalculates users; each user picks the change up on
the next token, and a permission removal is visible to the user at the next
renewal at the latest (token lifetime in section 6).

## 18. Token freshness

Token freshness covers account suspension/ban and role changes. Auth records
the time of each change in `identity_account.changed_at` and serves it from:

```text
GET /internal/identity/accounts/{userId}/authorization-state
  -> {userId, status, changedAt, evaluatedAt}
```

An unknown user is `ACTIVE` with `changedAt` null. The gateway
(`AuthorizationFreshnessWebFilter`), Java resource servers
(`AuthorizationFreshnessVerifier` and its filter in `common`) and the Python
services (`recommendation/app/agent/auth.py`, `assistant/app/security/auth.py`)
read this state, cached for at most five seconds, and allow a user token when:

```text
status == ACTIVE AND (changedAt == null OR iat >= floor_to_second(changedAt))
```

Otherwise they answer `401 TOKEN_AUTHORIZATION_STALE`. `changedAt` is floored
to the second because `iat` has whole seconds and the web retries a 401 only
once after a silent renew (`TokenFreshness`). The web renews silently on that
401 and obtains the new roles; a refresh-token grant re-runs the Keycloak
protocol mappers. A banned user cannot renew, because the Keycloak user is
disabled.

A protected operation whose state is stale or unavailable performs the
authenticated state read or fails closed; it never silently falls back to
token expiry. Permission increases do not invalidate a token; the user renews
to obtain the new permission set. A fresh token that no longer has the
operation permission receives the normal `403 PERMISSION_DENIED`.

## 19. HTTP failure semantics

All HTTP failures use `application/problem+json`.

| Situation | Status | Example code |
|---|---:|---|
| Missing, invalid or expired credential | 401 | `UNAUTHORIZED` |
| Token predates the last authorization change, or account is inactive | 401 | `TOKEN_AUTHORIZATION_STALE` |
| Authenticated but missing permission | 403 | `PERMISSION_DENIED` |
| Valid token but MFA level/freshness is insufficient | 403 | `MFA_STEP_UP_REQUIRED` |
| Missing commercial feature | 403 | `FEATURE_NOT_ENTITLED` |
| Wrong owner where existence may be concealed | 404 | `RESOURCE_NOT_FOUND` |
| Invalid state transition or idempotency fingerprint conflict | 409 | `STATE_CONFLICT` |
| Quota exhausted | 429 | `QUOTA_EXCEEDED` |
| Invalid request | 400 | `VALIDATION_FAILED` |

An OpenAPI annotation never replaces runtime enforcement.

## 20. Audit and observability

High-risk audit fields:

```text
occurredAt
action
decision
reasonCode
actorType
actorId
clientId
targetType
targetId
permission
relationshipResult
featureKey
subscriptionId
quotaOperationId
requestId
traceId
before/after summary
```

Do not store tokens, client secrets, raw webhook bodies or unnecessary PII.
Audit is mandatory for role/status changes, course review/publish/moderation,
plan publication, subscription override/refund, entitlement override, quota
adjustment, reconciliation and break-glass use.

Metrics use bounded labels and MUST NOT use `userId` as a label:

```text
security_authentication_failures_total{service,reason}
security_authorization_denials_total{service,permission,reason}
service_token_rejections_total{service,caller,reason}
websocket_authentication_failures_total{reason}
webhook_signature_failures_total{provider,reason}
```

Alert on repeated wrong-audience service tokens, failed Keycloak admin calls,
break-glass access and audit persistence failures.

## 21. Migration and implementation checklist

1. Finish the operation inventory in `authorization-matrix.yaml`.
2. Add the canonical authorization catalog, then generate or semantically
   validate `cinx-api`, exact composite roles, service clients, target
   audiences and token mappers in the realm config.
3. Add a new auth Flyway migration for the status/`changed_at` account and
   audit tables (`V4__keycloak_owned_roles.sql` drops the role and sync
   tables); do not edit applied migrations.
4. Remove `INSTRUCTOR_APPLICANT` from Keycloak roles and move application
   state to user.
5. Replace single-role APIs with idempotent role-set operations that call
   the Keycloak admin API.
6. Extend `CinxJwtAuthenticationConverter` and all decoder validators.
7. Add service-token filter chains and outbound OAuth2 clients.
8. Replace `hasRole('ADMIN')` and role bypasses with permissions and policy
   checks.
9. Secure Recommendation identity/session ownership.
10. Secure WebSocket CONNECT and destinations.
11. Add the authorization event v3 to AsyncAPI, producer, consumers and tests.
12. Reset/reimport the dev realm and recreate dev DBs after migrations. No
    backward-compatibility adapter is required because there is no release,
    but migration files already applied MUST remain unchanged.

## 22. Required verification

For the current development phase, these behaviors are verified with unit tests,
test fixtures and mocked/fake Keycloak/service adapters. Live Keycloak,
Testcontainers and end-to-end integration verification are deferred until the
project-stabilization phase and are not a current PR gate.

### Token and converter

- Valid user token yields expected `ROLE_*`, `PERM_*`, `SCOPE_*` and principal.
- Wrong issuer/audience/algorithm, expired and not-yet-valid tokens fail.
- User token fails internal chain; service token fails user chain.
- Infrastructure roles and plan claims do not become authorities.

### Endpoint matrix

For every protected operation:

```text
no token                     -> 401
valid token, no permission   -> 403
own permission, wrong owner  -> 403 or concealed 404
own permission, owner        -> success
any permission               -> success
wrong resource state         -> documented denial
```

### Keycloak behavior

- PKCE and exact production redirect URI.
- Direct grant and implicit grant disabled.
- Composite roles emit exact permission claims.
- Service account emits only target-specific permissions/audience.
- A role/status command fails as a whole when the Keycloak admin call fails.
- Suspend disables the Keycloak user; reactivate enables it.
- A token issued before `changed_at` (to the second) gets
  `401 TOKEN_AUTHORIZATION_STALE`; a renewed token passes.

### Internal and WebSocket

- Human token cannot call `/internal/**`.
- Wrong caller, audience or service permission fails.
- Missing/invalid WebSocket CONNECT fails.
- Foreign origin and unauthorized SEND/SUBSCRIBE fail.
- Only the authenticated user's notification queue is visible.

## 23. Confirmed and remaining decisions

Confirmed for v1:

- frontend is a pure SPA using Authorization Code + PKCE and bearer access
  tokens;
- privilege reduction uses the status/`changedAt` token freshness check with a
  five-second cache;
- learner/instructor plan segmentation is purchase eligibility only and cannot
  grant an authorization role.

The remaining authorization decision is whether role/permission bundles remain
config-as-code. This specification assumes they do. A runtime custom-role
builder requires moving definitions/mappings into auth DB and is outside v1.
