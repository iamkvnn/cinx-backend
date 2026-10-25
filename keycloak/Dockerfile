# The cinx login and email theme (keycloak/theme): Keycloakify packs it into a jar with Maven
FROM node:22-bookworm-slim AS theme
RUN apt-get update \
 && apt-get install -y --no-install-recommends maven openjdk-17-jdk-headless \
 && rm -rf /var/lib/apt/lists/* \
 && corepack enable
WORKDIR /theme
COPY keycloak/theme/package.json keycloak/theme/pnpm-lock.yaml ./
# postinstall copies the email templates into src/, which is not here yet
RUN pnpm install --frozen-lockfile --ignore-scripts
COPY keycloak/theme/ ./
RUN pnpm run postinstall && pnpm build-keycloak-theme

FROM quay.io/keycloak/keycloak:26.6.1 AS builder
ENV KC_DB=mysql
COPY --from=theme /theme/dist_keycloak/cinx-keycloak-theme.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.6.1
COPY --from=builder /opt/keycloak/ /opt/keycloak/
