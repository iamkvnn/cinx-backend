# Recommendation migrations

`0001_baseline` anchors the pre-Alembic schema. `0002_entitlement_quota` owns
all entitlement projection, inbox and quota tables plus additive course access
columns. Runtime schema mutation is rejected; apply these revisions with
Alembic in deployment tooling before starting the service. Embedding rows are
never rebuilt by a migration.
