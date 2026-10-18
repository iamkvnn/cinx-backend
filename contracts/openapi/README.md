# CINX Internal OpenAPI Contract

[`cinx-internal.yaml`](./cinx-internal.yaml) describes only synchronous
`/internal/**` operations with an active OpenFeign caller among Auth, User,
Learning, and Notification. It is not a catalog of every internal controller
method.

## Covered operations

| Operation | Owner | Consumers | Provider |
|---|---|---|---|
| `GET /internal/identity/permissions/{permission}/user-ids` | Auth | Notification | `InternalIdentityController` |
| `GET /internal/identity/accounts/{userId}/authorization-state` | Auth | Gateway, Notification | `InternalIdentityController` |
| `GET /internal/entitlements/users/{userId}` | Commerce | Learning, Recommendation, Assistant | `InternalEntitlementController` |
| `GET /internal/users/{id}` | User | Learning, Notification | `InternalUserController` |
| `GET /internal/user-ids?ids=...` | User | Learning | `InternalUserController` |
| `GET /internal/courses/{id}` | Learning | Notification | `InternalCourseController` |
| `GET /internal/courses/{id}/instructor-ids` | Learning | Notification | `InternalCourseController` |
| `GET /internal/enrollments/courses/{courseId}/users` | Learning | Notification | `NotificationProjectionController` |

Operations such as internal user creation, metrics, curriculum, quiz, and
rating updates are intentionally omitted because no active Feign client in this
service set calls them. Add an operation when its provider and first consumer
are introduced together.

Every operation carries:

- `x-cinx-owner`: the service that owns and serves the contract.
- `x-cinx-consumers`: services that currently call it.
- `x-cinx-provider` and `x-cinx-client(s)`: source paths used to verify drift.

## Trust boundary

The gateway denies `/internal/**`; callers resolve Java services with Eureka
and OpenFeign. The owning services use a higher-priority service-token chain
for these paths and require the exact internal permission. Never expose these
paths through a gateway route.

## Known client-view limitation

### User list client view

The User provider returns the full `user.dto.UserDto`. Learning's catalog and
certificate clients intentionally deserialize smaller views. The community
`UserSummaryResponse` also declares a `role` property, but the provider DTO does
not supply `role`, so that field is currently null. The contract follows the
provider DTO and does not invent the missing field.

Notification's User and Course client DTOs are also narrower read views; their
fields are subsets of the provider responses in this contract.

## Change rules

When changing one of these operations:

1. Update the provider DTO/controller and every listed Feign client together.
2. Preserve backward compatibility or introduce a new path/version.
3. Update this contract and add provider/consumer contract tests in the same
   change.
4. Search the full repository for the path and DTO before removing a consumer.
5. Keep `/internal/**` absent from gateway routes.

## Validation

The YAML can be parsed locally with PyYAML:

```powershell
python -c "import pathlib,yaml; yaml.safe_load(pathlib.Path('contracts/openapi/cinx-internal.yaml').read_text(encoding='utf-8')); print('OpenAPI YAML OK')"
```

CI validates this document with Redocly CLI using the minimal ruleset so
OpenAPI structure and local `$ref` targets cannot silently drift.
