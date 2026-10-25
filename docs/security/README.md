# CINX security and commercial-entitlement documentation

Thư mục này mô tả target architecture để triển khai authentication,
authorization, plan, subscription, feature entitlement và quota cho CINX.
Đây là thiết kế đích; code và contracts hiện tại chưa triển khai đầy đủ các
quy tắc trong tài liệu.

## Tài liệu

1. [Authentication and authorization](./authentication-authorization.md)
   mô tả trust boundary, Keycloak, JWT, multi-role, permissions, ownership,
   service identity, WebSocket và audit.
2. [Authorization matrix](./authorization-matrix.yaml) là inventory
   machine-readable cho các operation quan trọng và các operation mới;
   [JSON Schema](./authorization-matrix.schema.json) kiểm tra cấu trúc.
3. [Plans, subscriptions, entitlements and quotas](../commerce/plans-subscriptions-entitlements-quotas.md)
   mô tả commerce model, lifecycle, APIs, events, projections và quota;
   [catalog mẫu](../commerce/plan-catalog-example.yaml) minh họa learner và
   instructor tiers bằng dữ liệu có version.
4. [Rollout and verification](./rollout-verification.md) mô tả thứ tự triển
   khai, acceptance criteria, cutover và exact verification commands.

Tài liệu nền:

- [Service catalog](../architecture/service-catalog.yaml)
- [Interaction map](../architecture/interaction-map.md)
- [Final code review](../reviews/final-code-review.md)
- [AsyncAPI contract](../../contracts/asyncapi/cinx-events.yaml)
- [Internal OpenAPI contract](../../contracts/openapi/cinx-internal.yaml)

## Mức độ normative

- Các từ `MUST`, `MUST NOT`, `SHOULD`, `SHOULD NOT` biểu diễn yêu cầu bắt
  buộc hoặc khuyến nghị mạnh của target design.
- `authorization-matrix.yaml` là catalog cần được hoàn tất cho mọi operation
  trước release. Bản hiện tại bao phủ các trust boundary, lỗ hổng critical và
  flow mới; trường `coverage` công khai phần chưa hoàn tất.
- `final-code-review.md` là snapshot của worktree tại thời điểm review, không
  phải implementation contract.
- Khi implementation bắt đầu, mọi thay đổi public/internal/event contract
  phải được cập nhật đồng thời trong OpenAPI, AsyncAPI, producer, consumer và
  tests.

## Quyết định đã chốt cho target design

1. `auth` DB là single writer/source of truth cho account status và
   user-to-role assignments.
2. Keycloak sở hữu credential, login, sessions và token issuance; assignments
   trong Keycloak là projection được `auth` reconcile.
3. Role/permission/composite definitions nằm trong canonical
   `contracts/security/cinx-authorization.yaml`; Keycloak realm config được
   generate hoặc semantic-validate từ catalog này. OAuth client, flow và token
   mapper tiếp tục nằm trong realm config version-controlled.
4. Plan không phải role. `commerce` sở hữu plan, subscription, billing
   lifecycle và effective feature-grant snapshot.
5. Service sử dụng feature sở hữu local entitlement projection và quota
   usage/reservations của feature đó.
6. Authorization dùng RBAC/PBAC cho capability và ABAC/ReBAC cho ownership,
   relationship, resource state; plan entitlement và quota là lớp riêng.
7. User token và service token có trust model, audience và permissions riêng.
8. Frontend v1 là pure SPA dùng Authorization Code + PKCE; API dùng bearer
   access token, không dùng application session cookie.
9. Suspension, role revoke và permission reduction dùng required-version/status
   denial với mục tiêu chặn trong tối đa 5 giây; không chờ JWT hết hạn.
10. Subscription bao gồm course catalog được chọn và platform/AI features.
11. Plan học viên/giảng viên là product line và purchase eligibility. Mua plan
    giảng viên không bao giờ cấp `INSTRUCTOR` hoặc bất kỳ authority nào.

## Baseline subscription v1

Đã xác nhận:

- chỉ enable subject `USER`; schema có subject identifier để mở rộng
  organization sau nhưng chưa có seat/membership trong v1;
- một slot `BASE` cho mỗi user; catalog có product line `LEARNING` và `CREATOR`;
- data-managed `DEFAULT_USER_BASE` binding; catalog mẫu trỏ tới `LEARNER_FREE`;
- monthly/yearly recurring plans;
- course catalog và platform/AI feature đều có thể nằm trong plan;
- plan/price/billing policy/feature/quota được quản trị bằng dữ liệu có version,
  không fix cứng trong application code;
- Stripe-hosted Checkout là target cho SPA, thị trường khách hàng `VN`, currency
  `VND`;
- chưa enable team/family, add-on, trial hoặc overage trong v1;
- upgrade có hiệu lực sau provider confirmation; downgrade/cancel cuối kỳ;
- quota là hard limit và reset theo UTC period khai báo cho từng feature;
- operation tốn chi phí như AI dùng reserve/commit/release;
- payment provider nằm sau provider-specific adapter.

Catalog/giá/quota minh họa nằm tại
[`plan-catalog-example.yaml`](../commerce/plan-catalog-example.yaml). Đây là
example bị disable, không phải giá production.

Điểm chặn production còn lại: [Stripe global availability](https://stripe.com/global)
hiện không liệt kê Việt Nam là quốc gia mở merchant account trực tiếp. Cần xác
nhận pháp nhân/tài khoản Stripe ở quốc gia được hỗ trợ và kiểm tra VND recurring
trên chính account đó. Các phê duyệt
còn lại về giá, grace/trial, thuế, refund, instructor compensation và retention
được liệt kê trong
[`plans-subscriptions-entitlements-quotas.md`](../commerce/plans-subscriptions-entitlements-quotas.md#21-confirmed-inputs-and-remaining-approvals).

## Chính sách test hiện tại

Trong giai đoạn phát triển hiện tại, gate chỉ gồm unit test, compile và
static/contract validation. Integration profile, Testcontainers, live
database/broker/Keycloak/Stripe và E2E được hoãn đến khi dự án ổn định. Các
scenario/command đó vẫn được giữ trong rollout dưới dạng deferred backlog và
không được báo là đã pass khi chưa chạy.
