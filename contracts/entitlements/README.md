# Entitlement feature registry

`cinx-features.yaml` is the canonical allowlist for commercial feature keys.
Commerce plan data may reference only registered keys. A feature is evaluated
after authentication, permission, ownership and resource-state checks; it
never grants a role or permission.

The JSON Schema validates shape. CI and the Commerce catalog validator must
also enforce these semantic rules:

- keys are unique, lowercase dot-separated identifiers and never use an
  authentication namespace;
- `BOOLEAN` entries have no unit or reset policy;
- `QUOTA` entries have exactly one unit and at least one reset policy;
- a saleable feature has at least one operation binding;
- a feature's operation binding owner matches its registry owner;
- only `ANY_TRUE` boolean and `MAX` quota composition is supported in v1.

The registry intentionally contains future feature keys whose `saleable` flag
is false until the authorization matrix has a concrete operation and the
owning service implements its enforcement boundary.
