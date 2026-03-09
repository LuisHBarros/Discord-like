# Discord-Like

![CI](https://github.com/LuisHBarros/discord-like/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)
![License](https://img.shields.io/badge/license-MIT-blue)

A real-time chat platform built as a backend portfolio project, focused on clean architecture, Domain-Driven Design, and production-grade engineering practices.

---

## Overview

The project implements the core features of a communication platform — rooms, real-time messaging, end-to-end encryption, and user presence — as a domain for demonstrating concrete architectural decisions.

The choice of a **modular monolith** over microservices was deliberate: the domain does not justify the operational complexity of distributed services, and hexagonal architecture ensures bounded contexts remain decoupled and independently migratable in the future.

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Interfaces Layer                  │
│         Controllers │ WebSocket │ Event Listeners   │
└───────────────────────────┬─────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────┐
│                  Application Layer                  │
│            Application Services (Use Cases)         │
└───────────────────────────┬─────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────┐
│                    Domain Layer                     │
│    Entities │ Value Objects │ Aggregates │ Events   │
└───────────────────────────┬─────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────┐
│                Infrastructure Layer                 │
│         JPA/Hibernate │ Kafka/Redis │ WebSocket     │
└─────────────────────────────────────────────────────┘
```

### Bounded Contexts

| Context | Responsibility |
|---|---|
| **Identity** | Registration, JWT authentication, user management |
| **Collaboration** | Rooms, messages, invites, E2EE encryption |
| **Presence** | Online/offline status, last seen, state transitions |

Each context has its own isolated domain layer. Cross-context communication happens exclusively through domain events published to Kafka — no context imports domain classes from another.

### Design Decisions

**Why Hexagonal Architecture?**
The domain has no knowledge of Spring, JPA, or Kafka. Ports are pure interfaces inside `domain/ports`. This allows testing all business logic with mocks, without loading a Spring context.

**Why Kafka inside a monolith?**
Decoupling between contexts has value even within a single process. A `MessageService` that publishes `MessageEvents` does not know whether the consumer will update presence, send a push notification, or deliver via WebSocket. Kafka makes that contract explicit and makes a future migration to microservices straightforward.

**Why `reconstitute()` on aggregates?**
Construction and reconstitution are distinct operations. `new Room(name, ownerId)` enforces creation invariants. `Room.reconstitute(id, name, ownerId, ...)` restores persisted state without revalidating. The distinction is applied consistently across all aggregates in the project.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 3 |
| Database | PostgreSQL 15 + JPA/Hibernate |
| Cache | Redis 7 (`@Cacheable`, presence store, token blacklist) |
| Messaging | Apache Kafka (domain events, WebSocket distribution) |
| Real-time | WebSocket (Spring WebSocket) |
| Security | JWT (access + refresh), Argon2id, AES-256-GCM, ECDH |
| Observability | Prometheus (metrics), Zipkin (distributed tracing) |
| Testing | JUnit 5, Mockito, Testcontainers |
| CI/CD | GitHub Actions |
| Containerization | Docker, Docker Compose |

---

## Message Flow

```
POST /rooms/{id}/messages
        │
        ▼
MessageService.createMessage()
        │  encrypt(plaintext) → AES-256-GCM
        │  messageRepository.save()
        │
        ▼
eventPublisher.publish(MessageEvents.sent(...))
        │
        ▼
KafkaEventPublisher → topic "message-events"
        │
        ▼
MessageEventListener → KafkaBroadcaster
        │
        ▼
WebSocketDistributionEventListener
        │
        ▼
sessionManager.broadcastToRoom() → WebSocket
```

`EventPublisher` is a shared kernel interface — the domain has no knowledge that Kafka exists. The implementation can be swapped without touching any application service.

---

## Security

- **Passwords**: Argon2id with random salt
- **Tokens**: JWT with access token (15min) + refresh token (7d) + Redis blacklist
- **Messages**: AES-256-GCM with a random 12-byte IV per message
- **E2EE**: ECDH for key exchange + HKDF-SHA256 for key derivation + AES-256-GCM for encryption
- **Rate limiting**: `RateLimitedAuthService` decorator with Redis-backed counters
- **WebSocket**: `userId` extracted from the authenticated JWT session — never accepted as client input

---

## Testing

```
Layer                   Approach
──────────────────────────────────────────────────────
Domain models           Pure unit tests (no Spring)
Application services    Unit tests with Mockito (@Mock on ports)
Infrastructure adapters Integration tests with Testcontainers
WebSocket               Unit tests with session mocks
Middleware              Standalone MockMvc (isolated @WebMvcTest)
```

**145 tests** run in CI without additional infrastructure (PostgreSQL and Redis as GitHub Actions services, Kafka excluded via `ci` profile).

Tests requiring a full Spring context (Kafka, Testcontainers) are tagged with `@Tag("integration")` and run locally via `docker-compose`.

```bash
# Run unit tests (no infrastructure required)
./gradlew test -PexcludeTags="integration"

# Run all tests (requires docker-compose up)
./gradlew test
```

---

## Running Locally

### Prerequisites

- Java 21
- Docker and Docker Compose

### Starting the application

```bash
# Clone the repository
git clone https://github.com/LuisHBarros/discord-like.git
cd discord-like

# Start infrastructure services
docker-compose up -d

# Run the application
./gradlew bootRun
```

The API will be available at `http://localhost:8000`.
Swagger UI: `http://localhost:8000/swagger-ui.html`

### Environment variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `REDIS_HOST` | `localhost` | Redis host |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `JWT_ACCESS_SECRET` | — | JWT signing key (≥256 bits) |
| `JWT_REFRESH_SECRET` | — | JWT refresh signing key (≥256 bits) |
| `ENCRYPTION_SECRET` | — | AES-256 key in base64 (32 bytes) |

---

## Project Structure

```
src/main/java/com/luishbarros/discord_like/
├── modules/
│   ├── identity/           # Authentication and user management
│   │   ├── application/    # DTOs and application services
│   │   ├── domain/         # Entities, value objects, ports, errors
│   │   └── infrastructure/ # JPA, JWT, Argon2, controllers
│   ├── collaboration/      # Rooms, messages, invites
│   │   ├── application/
│   │   ├── domain/
│   │   └── infrastructure/ # JPA, Kafka, WebSocket, controllers
│   └── presence/           # User presence
│       ├── application/
│       ├── domain/
│       └── infrastructure/ # Redis, controllers
└── shared/                 # Shared kernel
    ├── adapters/           # Kafka, Redis, Prometheus, CORS
    ├── domain/             # BaseEntity, DomainEvent, DomainError
    └── ports/              # EventPublisher, Broadcaster, RateLimiter
```

---

## Architecture Documentation

Detailed documentation available in [`docs/`](./):

- [Overview](./architecture/01-overview.md)
- [Hexagonal Architecture](./architecture/02-hexagonal-architecture.md)
- [Bounded Contexts](./architecture/03-bounded-contexts.md)
- [Aggregates and Invariants](./architecture/04-aggregates.md)
- [Value Objects](./architecture/05-value-objects.md)
- [Domain Events](./architecture/06-domain-events.md)
- [Deployment Guide](./deployment/README.md)
- [Monitoring Setup](./deployment/04-monitoring.md)
- [CI/CD Setup](./deployment/CI_SETUP.md)

---
