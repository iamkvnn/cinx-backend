# CINX Microservices

## Overview

CINX is a domain-oriented backend platform for an online learning ecosystem. The target deployable topology is:

- `keycloak`
- `auth`
- `user`
- `commerce`
- `learning`
- `reporting`
- `notification`
- `recommendation`
- `subtitle-ai`
- `gateway`
- `eureka`

The frontend talks to resource-based public APIs through `gateway`; it does not need to know the deployable boundaries.
Service-to-service contracts live in `contracts/`, and the architecture notes in `docs/`.

## Tech Stack

- Java 21, Spring Boot 3.5.x, Spring Cloud 2025.0.1
- MySQL 8.0, database per deployable
- RabbitMQ 3 with transactional outbox/inbox patterns
- Keycloak for authentication, roles, PKCE and identity brokering
- Netflix Eureka and Spring Cloud Gateway
- Docker and Docker Compose

## Repository Layout

```text
cinx/
├── keycloak/        # Realm import and Keycloak image
├── common/          # Shared MVC/JPA/messaging utilities
├── common-security/ # Servlet-neutral JWT authority conversion
├── eureka/          # Eureka service registry
├── gateway/         # Resource-based API Gateway
├── contracts/       # OpenAPI, AsyncAPI, entitlement and authorization contracts
├── mysql-init/      # Database initialization scripts
└── docker-compose.yml
```

## Run Locally

```bash
for file in mysql rabbitmq; do cp env/dev/$file.env.example env/dev/$file.env; done
docker compose up -d mysql rabbitmq
mvn clean install -DskipTests
mvn spring-boot:run -pl eureka -Dspring-boot.run.profiles=local
mvn spring-boot:run -pl gateway -Dspring-boot.run.profiles=local
```

Files matching `env/**/*.env` are ignored by Git; only the `*.env.example` templates are committed.

## Verification

```bash
mvn test
scripts/check-env.sh
docker compose config --quiet
```
