# Flyway migrations

**Last verified:** 2026-10-18

Migrations live in `<service>/src/main/resources/db/migration/` (auth, user, commerce,
learning, notification, reporting). The rules on what a migration may change are in
`AGENTS.md` § Data, time, idempotency, and secrets; this guide is about adding one safely.

## Taking the next version

Sort numerically, not as text, or `V10` lands before `V2` and the next number is wrong:

```bash
ls <service>/src/main/resources/db/migration | sort -t_ -k1.2 -n | tail -1
```

A duplicate version fails the service at start-up.

## Renaming or replacing a file

Flyway also reads the copy Maven left in `target/classes/db/migration/`. After renaming a
migration, delete the old name there too (or run `mvn clean`), or Flyway sees both.

## Never edit an applied migration

Changing an applied file changes its checksum, and every database that already ran it
refuses to start until it is repaired. Add the next migration instead. If a checksum change
is unavoidable, say so in the change's risks so every developer knows to repair their database.
