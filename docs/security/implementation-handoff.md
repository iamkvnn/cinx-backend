# Handoff cho agent triển khai auth, authorization và subscription

Status: Phase C-F local implementation complete; production stabilization deferred.

The repository contains the feature registry, Commerce catalog/subscription
provider port, entitlement projections/quota reservation and Learning access
cutover. `cinx.subscription.billing-enabled=false` remains the deployment
default; live provider/broker/database, canary and GA checks are deferred.

Mục tiêu là để agent triển khai theo các contract đã chốt, chia nhỏ thay đổi thành PR có thể review/rollback, và không biến plan thành quyền hệ thống.

## 1. Tài liệu phải đọc trước mỗi phase

1. Root `AGENTS.md`, rồi `AGENTS.md` của module sắp sửa đổi.
2. `docs/architecture/service-catalog.yaml` để xác nhận ownership.
3. [Authentication and authorization](./authentication-authorization.md), [authorization matrix](./authorization-matrix.yaml), [subscription design](../commerce/plans-subscriptions-entitlements-quotas.md), [rollout](./rollout-verification.md), và [catalog minh họa](../commerce/plan-catalog-example.yaml).
4. Các OpenAPI/AsyncAPI hiện có và producer/consumer đang chạy trước khi đổi route/event.

Trước khi sửa, agent MUST chạy `git status --short`, bảo toàn thay đổi đã có, rồi dùng `rg` để tìm endpoint, event, routing key, schema, producer, consumer và test liền kề. Catalog YAML là input disabled, không được nạp trực tiếp vào production.

## 2. Invariants không được phá vỡ

- `auth` là source of truth cho account status, role và permission; Keycloak là identity/token infrastructure. `commerce` không ghi Keycloak, không cấp/thu hồi `ROLE_*`, `PERM_*`, `SCOPE_*`.
- Plan chỉ cấp feature/quota thương mại. Operation premium vẫn phải qua permission, ownership/relationship và resource state trước entitlement/quota.
- V1 chỉ có `USER` subject, một slot `BASE`, một base plan hiện hành. Không tạo API organization, seat, member-beneficiary hay add-on.
- Service tiêu thụ không branch theo `planCode`; chỉ dùng feature key từ registry và local entitlement snapshot.
- SPA chỉ gửi opaque `offerId`, allowlisted relative `returnPath` và idempotency key; server quyết định plan, price, currency, provider và eligibility. Stripe redirect không phải bằng chứng thanh toán.
- Mutation liên service dùng outbox/inbox, idempotent, at-least-once; controller không publish broker trực tiếp.
- Flyway migration chỉ thêm version mới. Không sửa migration đã áp dụng và không dùng `ddl-auto` thay migration.

## 3. Gate trước khi viết billing live

Chỉ bật Stripe live sau khi product owner xác nhận pháp nhân/quốc gia Stripe merchant account và khả năng recurring VND trên account thực tế; catalog production (giá, tax, trial, grace, refund, eligibility-loss, course eligibility, quota); API version, webhook rotation và payment methods.

Nếu thiếu quyết định, agent vẫn có thể làm contract/model/test với provider port và Stripe test mode, nhưng MUST giữ `cinx.subscription.billing-enabled=false` và không bật offer paid/live.

## 4. Thứ tự triển khai bắt buộc

Mỗi phase là một hoặc vài PR nhỏ. Không gộp phase kế tiếp vào cùng PR chỉ vì chạm cùng module.

### A. Auth foundation và immediate revocation

- Chốt authorization catalog/realm configuration, issuer/audience/claims và service-client grant.
- Hoàn thiện converter tại `common-security`; không giữ implementation song song trong `common`.
- Thêm `identity_account.changed_at`, event `auth.authorization.changed` v3 và endpoint internal `authorization-state` (status, changedAt) theo matrix.
- Gateway/resource service deny token có `iat` trước `changedAt` (hoặc tài khoản không ACTIVE), cache tối đa 5 giây.

Exit: token sai issuer/audience/kind/client/permission bị từ chối; mọi consumer converter được test; rehydration từ chối browser token; contract/producer/consumer thay đổi cùng nhau.

### B. Matrix enforcement và ownership

- Áp dụng operation trong matrix vào security chains, controller/service policy và ownership checks.
- Test anonymous, invalid token, missing permission, wrong-owner, sai service audience/client/permission và success.

Exit: endpoint nhạy cảm không chỉ dựa gateway/path `userId`; `@SecurityRequirement` không bị coi là runtime authorization.

### C. Catalog và feature registry (commerce, chưa thanh toán)

- Tạo version-controlled feature registry và validator. DB catalog chỉ tham chiếu key đã đăng ký.
- Thêm migrations cho product line, immutable plan/billing-policy version, provider product/price mapping, offer, system binding và publication job.
- Xây admin draft/validate/publish theo matrix, MFA/audit và optimistic locking.
- Tạo schema/semantic validator cho catalog import: duplicate key, locale, policy version, feature type/unit/reset, offer-price-plan relation, VND zero decimal, tax và binding target.

Exit: giá/copy/quota đổi bằng draft version + publish, không bằng `if (planCode)`; `DEFAULT_USER_BASE` và `CREATOR_ELIGIBILITY_LOSS_BASE` resolve từ data; chỉ offer có provider references reconciled mới saleable.

### D. Subscription lifecycle và provider port

- Tạo USER subject, current pointer, free assignment, subscription, command/idempotency, scheduled change, trial claim, grace bounds, quote, webhook inbox, audit/outbox.
- Implement provider-neutral port trước; Stripe adapter tạo hosted Checkout `mode=subscription`, idempotency từ command server và xác minh raw-body signature.
- Implement self APIs: read, entitlements, checkout-session, upgrade preview/confirm, downgrade, cancel/resume. Không nhận amount, Stripe Price ID, role hay feature từ browser.

Exit: concurrent onboarding chỉ tạo một current `BASE`; retry idempotency an toàn; payment chỉ activate sau webhook/reconciliation + eligibility recheck; role loss atomically mark `INELIGIBLE`, repoint free binding, increment entitlement version và write outbox; trial/grace có state/timestamp/policy rõ.

### E. Entitlement event, consumer projection và quota

- Thêm AsyncAPI snapshot `commerce.subscription.entitlements.changed` với aggregate `SubjectEntitlement`, `sources`, entitlement version và period riêng cho từng quota.
- Consumer dùng durable inbox, local projection, version ordering; không gọi commerce trong hot path.
- Service chạy metered feature sở hữu counter/reservation/ledger, reserve → execute → commit/release với transaction ngắn và injected `Clock`.

Exit: event cũ/duplicate không regress state; upgrade đổi limit current period nhưng không reset usage; quota `CALENDAR_MONTH` của annual plan vẫn mang period tháng explicit.

### F. Learning access, rollout và live gate

- Learning tách permanent purchase, enrollment/progress và subscription policy; không dùng `isInSubscription` làm runtime entitlement.
- Cập nhật gateway, OpenAPI, AsyncAPI, service catalog, dashboards, alert, DLQ/reconciliation runbook và kill switch.
- Canary/test mode trước. Integration gate được hoãn đến phase ổn định; chỉ
  production live sau khi phase đó hoàn tất.

Exit hiện tại: unit tests chứng minh permanent purchase sống qua expiry và
progress/certificate được giữ. Các acceptance scenario phụ thuộc DB/broker/
Keycloak/Stripe thật được ghi backlog cho stabilization, không chặn PR hiện tại.

## 5. Quy tắc contract và data cho mọi PR

1. Route/event đổi thì cập nhật OpenAPI/AsyncAPI, gateway route/binding, producer, mọi consumer và contract tests trong cùng thay đổi.
2. Write domain + outbox trong một transaction. Consumer claim inbox + projection/domain update trong một transaction, acknowledge sau commit.
3. Tiền dùng `BigDecimal`/`DECIMAL(19,2)` và currency registry. VND exponent 0: `99000` là 99.000 VND.
4. Business time dùng injected `Clock` và UTC; provider event có thể duplicate/out-of-order.
5. Không log/commit token, webhook secret, raw payment payload hay payment credential.
6. Staff financial operation cần granular permission, MFA, actor/reason và audit record; không dùng broad `ROLE_ADMIN`.

## 6. Definition of done cho một PR

- Liệt kê contract/migration/module bị ảnh hưởng và ownership.
- Migration mới cùng unit/static validation phù hợp; fresh-schema và upgrade
  verification được ghi rõ là deferred/unrun.
- Negative security, idempotency, duplicate/out-of-order event và retry unit
  test bằng mock/fake nếu scope có external call.
- Matrix/feature registry/OpenAPI/AsyncAPI được cập nhật nếu phạm vi yêu cầu.
- Báo command đã pass, fail hoặc chưa chạy; kèm rollback/kill-switch hoặc reconciliation strategy.

Gate hiện tại chỉ chạy unit test, compile và static/contract validation. Không
chạy `-Pintegration`, Testcontainers, live DB/broker/Keycloak/Stripe hoặc E2E
trừ khi owner chuyển dự án sang stabilization hoặc yêu cầu rõ trong task.

Agent MUST dừng hỏi thay vì tự quyết định legal entity, tax, refund, provider capability, public compatibility, ownership, delivery semantics hay durability.

## 7. Prompt giao một phase cho agent

```text
Bạn là implementation agent của CINX. Chỉ thực hiện <PHASE>: <SCOPE>.

Trước khi code: đọc root AGENTS.md, AGENTS.md gần nhất,
docs/architecture/service-catalog.yaml, docs/security/implementation-handoff.md,
authentication-authorization.md, authorization-matrix.yaml,
plans-subscriptions-entitlements-quotas.md và rollout-verification.md. Chạy
git status --short; bảo toàn thay đổi có sẵn. Dùng rg để tìm toàn bộ route,
event, producer, consumer, schema và test liên quan.

Trình bày kế hoạch theo file/contract/migration/test trước khi sửa. Không mở rộng
sang phase khác. Không hard-code planCode/price/role vào commercial behavior;
không để commerce thay đổi Keycloak role/permission; không sửa Flyway migration
đã áp dụng; không publish trực tiếp từ controller.

Nếu route/event đổi, cập nhật contract + mọi consumer/producer + tests trong
cùng thay đổi. Hiện chỉ chạy unit test/compile/static contract validation theo
AGENTS.md; ghi integration là deferred, không tự chạy -Pintegration/Testcontainers.
Báo chính xác command pass, fail hoặc chưa chạy. Dừng và hỏi khi thiếu quyết định về external/legal/provider,
public compatibility, ownership, delivery semantics hoặc durability.
```

Khi phase chưa được owner cho phép, agent chỉ được phân tích và đề xuất plan; không tạo code, migration hay contract "giả như đã tồn tại".
