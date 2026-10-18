# Running cinx locally: what goes wrong

**Last verified:** 2026-10-18 against the working tree (`application-local.yaml` of each service)

The commands are in `CLAUDE.md` § Commands. This guide is the part they do not say: what
breaks a first bring-up, and how to know the stack is really up.

## Before the first start: one preflight, one list

Check everything first and fix the gaps together; starting services one at a time and fixing
each failure as it appears took over an hour once.

- **JDK 21.** Lombok 1.18.36 does not compile on newer JDKs (`ExceptionInInitializerError:
  com.sun.tools.javac.code.TypeTag :: UNKNOWN`). Point `JAVA_HOME` at a JDK 21 for every `mvn`.
- **Env names.** `scripts/check-env.sh` compares the `${VAR}` placeholders with the
  `*.env.example` templates. The `local` profile reads `../env/local.env`.
- **Ports.** gateway 9090, eureka 8761, auth 8888, user 8089, commerce 8087, learning 8084,
  notification 8086, reporting 8091, recommendation 8090, Keycloak 8180 on the host.
- **Realm.** The `cinx-auth-admin` client needs the `view-realm` role
  (`keycloak/realm/cinx-realm.json`).

## Running on the host

- Infrastructure runs in Docker, services on the host with the `local` profile, which points
  Keycloak, Eureka and internal URLs at `localhost`. A value copied from `env/dev/*.env`
  carries Docker hostnames and breaks a host run.
- Business time is UTC. Each service's `*Application.main` sets the default zone to UTC; a new
  service must do the same, or rows and the outbox schedule shift by the host's offset.
- A secret with special characters can differ between a shell-sourced env file and the
  container. When a service cannot reach MySQL, compare against what the container was given.

## When is it up

Not when the processes start: sign in once through Keycloak and read one authenticated
endpoint through the gateway. Only that proves the issuer, the audience and the routes agree.
