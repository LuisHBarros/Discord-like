# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Discord-like is a real-time communication application built with Spring Boot 4.0.2 and Java 21. It uses PostgreSQL for persistence, Redis for caching and presence, Kafka for domain event publishing, and WebSocket for real-time messaging. Messages are encrypted at rest using AES-GCM (server-side key).

## Build Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.luishbarros.discord_like.SomeTest"

# Clean build
./gradlew clean build
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Architecture

The project follows **Hexagonal Architecture (Ports & Adapters)** with **Domain-Driven Design** principles, organized into **feature-based bounded contexts**.

### Dependency Flow

```
Infrastructure → Ports ← Application ← Domain
```

- Domain layer has zero external dependencies
- Ports define interfaces (in `domain/ports/`)
- Application services orchestrate domain logic via ports
- Infrastructure provides concrete implementations

### Bounded Contexts

The project has three bounded contexts:

1. **Identity** — Authentication, user management, and authorization
2. **Collaboration** — Rooms, messaging, and invites
3. **Presence** — User online status and activity tracking

### Module: `identity`

```
modules/identity/
├── application/
│   ├── dto/           # LoginRequest, RegisterRequest, RefreshRequest, AuthResponse,
│   │                  # UserResponse, ChangePasswordRequest
│   └── service/       # AuthService, UserService
├── domain/
│   ├── event/         # UserEvents (REGISTERED, PASSWORD_CHANGED, DEACTIVATED, ACTIVATED)
│   ├── model/
│   │   ├── error/     # InvalidCredentialsError, InvalidTokenError, UserNotFoundError,
│   │   │              # DuplicateEmailError, InvalidUserError
│   │   └── value_object/  # Username, Email, PasswordHash
│   └── ports/
│       ├── repository/    # UserRepository
│       └── other/         # PasswordHasher, TokenProvider, TokenBlacklist
└── infrastructure/
    ├── adapters/      # JpaUserRepository (domain port adapter)
    ├── event/         # UserEventListener (Kafka consumer, user-events topic)
    ├── http/          # AuthController, UserController
    ├── persistence/
    │   ├── entity/    # UserJpaEntity (with toDomain() / fromDomain())
    │   └── repository/ # SpringDataUserRepository (Spring Data interface)
    └── security/      # JwtFilter, JwtTokenProvider, Argon2PasswordHasher,
                       # InMemoryTokenBlacklist, AuthenticatedUser,
                       # RedisTokenBlacklist (@Primary)
```

### Module: `collaboration`

```
modules/collaboration/
├── application/
│   ├── dto/           # CreateRoomRequest, UpdateRoomRequest, JoinRoomRequest,
│   │                  # SendMessageRequest, RoomResponse, MessageResponse,
│   │                  # InviteResponse, UpdateMessageRequest
│   ├── factory/       # InviteFactory (generates 8-char uppercase invite codes)
│   └── service/       # RoomService, MessageService, InviteService,
│                      # E2EEKeyManagementService, ConversationService
├── domain/
│   ├── error/         # InvalidRoomError, ForbiddenError, InvalidInviteCodeError,
│   │                  # InvalidMessageError, RoomNotFoundError, UserNotInRoomError,
│   │                  # EncryptionException
│   ├── event/         # RoomEvents, MessageEvents, InviteEvents (static factory methods)
│   ├── model/
│   │   ├── aggregate/  # Room, Message, Invite, Conversation
│   │   ├── error/       # RoomEncryptionError
│   │   └── value_object/  # InviteCode, MessageContent, RoomName, Membership
│   ├── ports/         # EncryptionService, RoomEncryptionStateRepository, ConversationRepository
│   │   └── repository/    # RoomRepository, MessageRepository, InviteRepository
│   └── service/       # RoomAccessPolicy, MessageDeliveryPolicy, RoomMembershipValidator
└── infrastructure/
    ├── adapter/       # RoomRepositoryAdapter, MessageRepositoryAdapter, InviteRepositoryAdapter,
    │                  # RoomEncryptionStateRepositoryAdapter, ConversationRepositoryAdapter
    ├── encryption/    # AesEncryptionService (AES/GCM/NoPadding, random 12-byte IV,
    │                  # IV prepended to ciphertext, key from ENCRYPTION_SECRET env var),
    │                  # E2EEKeyManagementService (X25519 key exchange, key rotation)
    ├── event/         # InviteEventListener, MessageEventListener, RoomEventListener
    │                  # (Kafka consumers → broadcast to WebSocket)
    ├── http/          # RoomController, MessageController, E2EEController
    └── persistence/
        ├── entity/    # RoomJpaEntity, MessageJpaEntity, InviteJpaEntity,
        │              # RoomEncryptionStateJpaEntity, ConversationJpaEntity
        └── repository/ # RoomJpaRepository, MessageJpaRepository, InviteJpaRepository,
                        # RoomEncryptionStateJpaRepository, ConversationJpaRepository
```

### Module: `presence`

```
modules/presence/
├── application/
│   ├── dto/           # PresenceStatus
│   ├── ports/
│   │   └── in/        # TrackPresenceUseCase, QueryPresenceUseCase
│   └── service/       # PresenceService
├── domain/
│   ├── event/         # PresenceEvents (USER_CAME_ONLINE, USER_WENT_OFFLINE, USER_STATE_CHANGED)
│   ├── model/
│   │   ├── aggregate/  # UserPresence
│   │   ├── error/       # InvalidPresenceError
│   │   └── value_object/ # PresenceState, LastSeen
│   ├── ports/
│   │   └── repository/    # PresenceRepository
│   └── service/       # PresencePolicy, DefaultPresencePolicy
└── infrastructure/
    ├── adapter/       # InMemoryPresenceRepository, RedisPresenceRepositoryAdapter
    ├── event/         # PresenceEventListener (Kafka consumer)
    └── http/          # PresenceController
```

### Shared

```
shared/
├── adapters/
│   ├── config/      # SecurityConfig, WebSocketConfig, RedisConfig, JacksonConfig, OpenApiConfig
│   ├── http/        # HealthController
│   ├── messaging/   # KafkaEventPublisher, KafkaBroadcaster
│   ├── middleware/  # DomainErrorHandler (@RestControllerAdvice)
│   ├── presence/    # RedisPresenceStore, InMemoryRateLimiter
│   └── ratelimit/   # RedisRateLimiter, RateLimitedAuthService
├── domain/
│   ├── error/       # DomainError (abstract base), RateLimitError
│   └── model/       # BaseEntity (abstract domain base with Long id)
└── ports/           # RateLimiter, Broadcaster, PresenceStore, EventPublisher, EventListener<T>
```

### Key Patterns

**Domain reconstitution:** `Room`, `Message`, `Invite`, `User`, and `UserPresence` domain objects use a `static reconstitute(id, ...)` factory method for restoring persisted state. The regular constructor initialises a fresh aggregate (id starts as null until persisted). JPA entities call `reconstitute()` in `toDomain()`, and adapters always return `jpaRepository.save(...).toDomain()` so the caller receives the DB-generated ID.

**Domain errors:** All domain errors extend `DomainError(code, message)` and are caught globally by `DomainErrorHandler`. Add new error classes by extending `DomainError` and registering the code in `DomainErrorHandler.mapErrorToStatus()`. Static factory methods (e.g. `InvalidMessageError.emptyContent()`) are preferred over raw constructors for named error cases.

**Message encryption:** `MessageService` encrypts plaintext with `EncryptionService.encrypt()` before persisting. The stored and transmitted value is always ciphertext. `AesEncryptionService` uses AES/GCM/NoPadding with a random 12-byte IV prepended to each ciphertext.

**Event publishing:** `KafkaEventPublisher` (in `shared/adapters/messaging/`) routes events to Kafka topics based on the event's class simple name: `RoomEvents` → `room-events`, `MessageEvents` → `message-events`, `InviteEvents` → `invite-events`, `UserEvents` → `user-events`, `PresenceEvents` → `presence-events`.

**Event consuming:** Each domain area has a dedicated `*EventListener` Kafka consumer in `infrastructure/event/` that deserializes events and broadcasts them to WebSocket clients via the `Broadcaster` port.

**Rate limiting:** `RateLimitedAuthService` is a decorator wrapping `AuthService` that enforces a 5-requests/60s window per client IP using `RedisRateLimiter`. Both `AuthController` and `UserController` inject `RateLimitedAuthService`.

**Invite flow:** `InviteFactory` creates an `Invite` with an 8-char UUID-derived code and a 24-hour TTL. `InviteService.acceptInvite()` validates expiry, then delegates to `RoomService.addMember()`.

**Presence tracking:** The `presence` module uses `PresenceService` to track user states (ONLINE, OFFLINE, IDLE, DO_NOT_DISTURB). Presence is stored in Redis via `RedisPresenceRepositoryAdapter`. Presence events are published to `presence-events` topic and consumed by `PresenceEventListener`. Presence state changes are also broadcast to WebSocket clients.

**Presence integration with WebSocket:** Set online on WebSocket connect via `PresenceService.setOnline(userId)`, offline on disconnect via `PresenceService.setOffline(userId)`. The `RedisPresenceStore` tracks online users in a Redis Set (`presence:online`) via SADD/SREM/SISMEMBER/SMEMBERS.

**WebSocket session management:** `WebSocketSessionManager` keeps an in-memory `ConcurrentHashMap` of sessions per room. `broadcastToRoom` iterates over a `List.copyOf` snapshot and removes stale/closed sessions inline, cleaning up empty room entries automatically.

**Domain policies:** The `collaboration` module uses policy interfaces (`RoomAccessPolicy`, `MessageDeliveryPolicy`) to encapsulate cross-cutting domain rules. `DefaultRoomAccessPolicy` and `DefaultMessageDeliveryPolicy` provide the standard implementations. This allows for easy extension and testing of access control and message delivery logic.

**Use case ports:** The `presence` module demonstrates the use case pattern with `TrackPresenceUseCase` and `QueryPresenceUseCase` interfaces in `application/ports/in/`. Application services implement these use case ports, providing a clear contract for interaction.

**Conversation aggregate memory trade-off:** The `Conversation` aggregate loads up to 10,000 messages into memory using a `SortedSet`. This design prioritizes domain completeness and invariant enforcement over memory efficiency. Each active room consumes approximately 2-5MB. For large-scale deployments, consider migrating to lazy loading or a repository-based approach to reduce memory footprint while maintaining domain invariants through application service layer validation.

## Key Technologies

- **Spring Boot 4.0.2** with Spring Data JPA, Spring Data Redis, Spring Security, Spring WebSocket
- **SpringDoc OpenAPI** — Swagger UI at `/swagger-ui.html` with JWT Bearer auth support
- **PostgreSQL** — primary database; `ddl-auto: update` (schema auto-managed by Hibernate); all entities use `GenerationType.IDENTITY`
- **Redis** — token blacklist, rate limiting, and presence store
- **Kafka** — domain event bus; producers via `KafkaEventPublisher`, consumers via `*EventListener` classes
- **JJWT 0.12.6** — JWT access/refresh token generation and validation
- **BouncyCastle 1.80** — Argon2id password hashing

## API Endpoints

### Auth (`/auth`)
- `POST /auth/register` — Register new user (rate-limited per IP)
- `POST /auth/login` — Login, returns access + refresh tokens (rate-limited per IP)
- `POST /auth/refresh` — Refresh access token
- `POST /auth/logout` — Blacklist access + refresh tokens

### Users (`/users`)
- `GET /users/me` — Current user profile
- `PATCH /users/me/password` — Change password
- `PATCH /users/me/deactivate` — Deactivate account
- `PATCH /users/me/activate` — Activate account

### Rooms (`/rooms`)
- `POST /rooms` — Create room
- `GET /rooms` — List user's rooms
- `GET /rooms/{id}` — Room details
- `PATCH /rooms/{id}` — Rename room (owner only)
- `DELETE /rooms/{id}` — Delete room (owner only)
- `POST /rooms/join` — Join via invite code
- `POST /rooms/{id}/leave` — Leave room
- `POST /rooms/{id}/invite/regenerate` — Generate new invite
- `GET /rooms/{id}/members` — List member IDs
- `DELETE /rooms/{id}/members/{memberId}` — Kick member (owner only)

### Presence (`/presence`)
- `GET /presence/{userId}` — Get user's presence status
- `GET /presence/online` — List all online users with status
- `GET /presence/online/ids` — List online user IDs only
- `POST /presence/{userId}/online` — Set user online
- `POST /presence/{userId}/offline` — Set user offline
- `POST /presence/{userId}/state` — Set user presence state (ONLINE, OFFLINE, IDLE, DO_NOT_DISTURB)
- `POST /presence/{userId}/activity` — Update last activity timestamp

### Messages (`/rooms/{roomId}/messages`)
- `POST /rooms/{roomId}/messages` — Send message (plaintext in, ciphertext stored)
- `GET /rooms/{roomId}/messages` — Paginated messages (`limit`, `offset` params)
- `GET /rooms/{roomId}/messages/before/{messageId}` — Cursor-based older messages
- `PATCH /rooms/{roomId}/messages/{messageId}` — Edit own message
- `DELETE /rooms/{roomId}/messages/{messageId}` — Delete own message

### WebSocket
- `ws://localhost:8000/ws/rooms/{roomId}?token={jwt}` — Real-time room connection

Incoming message types: `message`, `join_room`, `leave_room`, `ping`

### Health / Docs
- `GET /health`
- `GET /swagger-ui.html` — Swagger UI

## Configuration

`application.yaml` defaults (override with env vars):

| Env var | Default | Description |
|---|---|---|
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `JWT_ACCESS_SECRET` | dev default | Access token signing key (≥256-bit) |
| `JWT_REFRESH_SECRET` | dev default | Refresh token signing key (≥256-bit) |
| `ENCRYPTION_SECRET` | dev default | AES-GCM encryption key (base64, 32 bytes) |

JWT access token TTL: 15 minutes. Refresh token TTL: 7 days. Server port: `8000`.

## Domain Error Codes → HTTP Status

| Code | Status |
|---|---|
| `INVALID_CREDENTIALS`, `UNAUTHORIZED` | 401 |
| `FORBIDDEN`, `USER_NOT_IN_ROOM` | 403 |
| `NOT_FOUND`, `ROOM_NOT_FOUND`, `USER_NOT_FOUND`, `MESSAGE_NOT_FOUND` | 404 |
| `DUPLICATE_EMAIL`, `CONFLICT` | 409 |
| `RATE_LIMITED` | 429 |
| `VALIDATION_ERROR`, `INVALID_MESSAGE`, `INVALID_ROOM`, `INVALID_USER`, `INVALID_INVITE_CODE`, `ENCRYPTION_ERROR` | 400 |
| anything else | 500 |

`MethodArgumentNotValidException` (Spring `@Valid` failures) is also caught and returned as `VALIDATION_ERROR` / 400, with field-level messages joined by `; `.

## Additional Documentation

Comprehensive architecture documentation is available in the `docs/` folder:
- **docs/README.md** — Main documentation index with architecture diagrams
- **docs/architecture/** — Detailed architecture documentation covering hexagonal architecture, bounded contexts, aggregates, value objects, domain events, repositories, and design patterns
- **docs/development/** — Development guides for project structure and testing

## Test Coverage

| Layer | Classes |
|---|---|
| Application services | `AuthServiceTest`, `UserServiceTest`, `RoomServiceTest`, `MessageServiceTest`, `InviteServiceTest`, `PresenceServiceTest`, `E2EEKeyManagementServiceTest`, `ConversationServiceTest` |
| Infrastructure adapters | `InviteRepositoryAdapterTest`, `RoomJpaRepositoryTest`, `ConversationRepositoryAdapterTest` |
| WebSocket | `WebSocketSessionManagerTest`, `ChatWebSocketHandlerTest` |
| Middleware | `DomainErrorHandlerTest` |
| Domain models | `RoomTest`, `MessageTest`, `InviteTest`, `UserTest`, `UserPresenceTest`, `ConversationTest` |
| Cache & Tracing | `RoomServiceCacheTest`, `TracingConfigTest` |

**Test Coverage: 97.6% (165/169 tests passing)**

Integration tests use Testcontainers with PostgreSQL, Redis, and Kafka. Testcontainers configuration uses `withReuse(false)` to ensure test isolation.