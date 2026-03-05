# Bounded Contexts

## Introduction

In Domain-Driven Design, a **Bounded Context** is a specific part of the domain logic where particular terms and rules apply consistently. Bounded contexts define clear boundaries around different parts of the system, each with its own ubiquitous language and model.

## Bounded Contexts in Discord-like

Discord-like is organized into three primary bounded contexts:

```
┌─────────────────────────────────────────────────────────────────┐
│                        Discord-like System                        │
│                                                                  │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │
│  │   Identity     │  │ Collaboration  │  │   Presence     │    │
│  │    Context     │  │    Context     │  │    Context     │    │
│  │                │  │                │  │                │    │
│  │  • Users       │  │  • Rooms       │  │  • Online/     │    │
│  │  • Auth        │  │  • Messages    │  │    Offline     │    │
│  │  • Profiles    │  │  • Invites     │  │  • Activity    │    │
│  │                │  │  • Membership   │  │  • States      │    │
│  └────────┬───────┘  └────────┬───────┘  └────────┬───────┘    │
│           │                  │                    │              │
│           └──────────────────┼────────────────────┘              │
│                              │                                   │
│                    ┌─────────┴─────────┐                        │
│                    │   Shared Kernel   │                        │
│                    │ • BaseEntity      │                        │
│                    │ • DomainError     │                        │
│                    │ • Ports           │                        │
│                    └───────────────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

## 1. Identity Context

### Purpose

Handles user registration, authentication, profile management, and account lifecycle.

### Core Concepts

- **User** - The primary entity representing a system user
- **Authentication** - Login, logout, token management
- **Profile** - User profile information
- **Account Status** - Active/inactive state management

### Ubiquitous Language

| Term | Definition |
|------|------------|
| User | An entity with credentials and profile information |
| Username | Unique identifier for a user (3-50 chars, alphanumeric) |
| Email | User's email address, must be unique |
| Password Hash | Argon2id hash of the user's password |
| Access Token | JWT token for API authentication (15 min TTL) |
| Refresh Token | JWT token for token refresh (7 days TTL) |
| Active | User account status indicating they can authenticate |

### Domain Model

```java
// Entities
User extends BaseEntity {
    Username username;
    Email email;
    PasswordHash passwordHash;
    boolean active;
    Instant createdAt;
    Instant updatedAt;
}

// Value Objects
Username(String value)      // 3-50 chars, alphanumeric
Email(String value)          // Valid email format
PasswordHash(String value)   // Argon2id hash

// Domain Events
UserEvents {
    REGISTERED,
    PASSWORD_CHANGED,
    DEACTIVATED,
    ACTIVATED
}

// Domain Errors
DuplicateEmailError
InvalidCredentialsError
InvalidTokenError
UserNotFoundError
InvalidUserError
```

### Boundaries

**What Identity Context Owns:**
- User entity and profile data
- Authentication and authorization
- Password management
- Account activation/deactivation

**What Identity Context Doesn't Own:**
- Room membership (owned by Collaboration)
- Messages (owned by Collaboration)
- Presence status (owned by Presence)

### Context Relationships

- **Upstream to Collaboration** - Provides User ID for room membership and messaging
- **Upstream to Presence** - Provides User ID for presence tracking
- **No downstream dependencies** - Identity context is self-contained

### Module Structure

```
modules/identity/
├── application/
│   ├── dto/
│   │   ├── AuthResponse.java
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── RefreshRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   └── UserResponse.java
│   └── service/
│       ├── AuthService.java          # Auth use cases
│       └── UserService.java          # User management use cases
├── domain/
│   ├── event/
│   │   └── UserEvents.java
│   ├── model/
│   │   ├── User.java
│   │   ├── value_object/
│   │   │   ├── Username.java
│   │   │   ├── Email.java
│   │   │   └── PasswordHash.java
│   │   └── error/
│   │       ├── DuplicateEmailError.java
│   │       ├── InvalidCredentialsError.java
│   │       ├── InvalidTokenError.java
│   │       ├── UserNotFoundError.java
│   │       └── InvalidUserError.java
│   └── ports/
│       ├── repository/
│       │   └── UserRepository.java
│       ├── PasswordHasher.java
│       ├── TokenProvider.java
│       └── TokenBlacklist.java
└── infrastructure/
    ├── adapters/
    │   ├── JpaUserRepository.java
    │   └── InMemoryUserRepository.java
    ├── event/
    │   └── UserEventListener.java
    ├── http/
    │   ├── AuthController.java
    │   └── UserController.java
    ├── persistence/
    │   ├── entity/
    │   │   └── UserJpaEntity.java
    │   └── repository/
    │       └── SpringDataUserRepository.java
    └── security/
        ├── Argon2PasswordHasher.java
        ├── JwtTokenProvider.java
        ├── InMemoryTokenBlacklist.java
        ├── RedisTokenBlacklist.java
        ├── JwtFilter.java
        └── AuthenticatedUser.java
```

## 2. Collaboration Context

### Purpose

Manages rooms, messaging, invitations, and membership for real-time collaboration.

### Core Concepts

- **Room** - A space for conversations
- **Message** - Individual communications within rooms
- **Invite** - Mechanism for adding users to rooms
- **Membership** - User participation in rooms
- **Conversation** - Aggregate managing messages in a room

### Ubiquitous Language

| Term | Definition |
|------|------------|
| Room | A named space for group conversations |
| Message | An encrypted communication sent by a user to a room |
| Invite Code | 8-character code for room invitation (24h TTL) |
| Member | A user who has joined a room |
| Owner | The user who created and owns a room |
| Conversation | The collection of messages in a room |

### Domain Model

```java
// Aggregates
Conversation extends BaseEntity {
    Long roomId;
    SortedSet<Message> messages;
    Instant lastActivityAt;
}

// Entities
Room extends BaseEntity {
    RoomName name;
    Long ownerId;
    Set<Long> memberIds;
    Instant createdAt;
    Instant updatedAt;
}

Message extends BaseEntity {
    Long senderId;
    Long roomId;
    MessageContent content;
    Instant createdAt;
    Instant editedAt;
}

Invite extends BaseEntity {
    Long roomId;
    InviteCode code;
    Long createdByUserId;
    Instant expiresAt;
}

// Value Objects
RoomName(String value)           // 1-100 chars
InviteCode(String value, Long createdByUserId)
MessageContent(String ciphertext) // AES-GCM encrypted content
Membership(Long userId, String role, Instant joinedAt)

// Domain Events
RoomEvents {
    ROOM_CREATED,
    MEMBER_JOINED,
    MEMBER_LEFT,
    ROOM_UPDATED,
    ROOM_DELETED
}

MessageEvents {
    CREATED,
    EDITED,
    DELETED
}

InviteEvents {
    CREATED,
    ACCEPTED,
    EXPIRED
}

// Domain Services
RoomMembershipValidator - Validates room membership
RoomAccessPolicy - Defines access rules
MessageDeliveryPolicy - Defines message delivery rules

// Domain Errors
InvalidRoomError
InvalidMessageError
InvalidInviteCodeError
RoomNotFoundError
UserNotInRoomError
ForbiddenError
EncryptionException
```

### Boundaries

**What Collaboration Context Owns:**
- Rooms and room metadata
- Messages and message encryption
- Invites and invite codes
- Room membership
- Message editing/deletion

**What Collaboration Context Doesn't Own:**
- User profile data (references User ID only)
- User presence (uses User ID from Presence context)
- Authentication (receives authenticated User ID)

### Context Relationships

- **Downstream to Identity** - References User IDs, receives authenticated user
- **Downstream to Presence** - Publishes events that affect presence
- **Upstream to Presence** - Consumes presence events for real-time features

### Module Structure

```
modules/collaboration/
├── application/
│   ├── dto/
│   │   ├── CreateRoomRequest.java
│   │   ├── UpdateRoomRequest.java
│   │   ├── JoinRoomRequest.java
│   │   ├── SendMessageRequest.java
│   │   ├── UpdateMessageRequest.java
│   │   ├── RoomResponse.java
│   │   ├── MessageResponse.java
│   │   └── InviteResponse.java
│   ├── factory/
│   │   └── InviteFactory.java
│   └── service/
│       ├── RoomService.java
│       ├── MessageService.java
│       └── InviteService.java
├── domain/
│   ├── event/
│   │   ├── RoomEvents.java
│   │   ├── MessageEvents.java
│   │   └── InviteEvents.java
│   ├── model/
│   │   ├── Room.java
│   │   ├── Message.java
│   │   ├── Invite.java
│   │   ├── UserRef.java
│   │   ├── aggregate/
│   │   │   └── Conversation.java
│   │   ├── value_object/
│   │   │   ├── RoomName.java
│   │   │   ├── MessageContent.java
│   │   │   ├── Membership.java
│   │   │   └── InviteCode.java
│   │   └── error/
│   │       ├── InvalidRoomError.java
│   │       ├── InvalidMessageError.java
│   │       ├── InvalidInviteCodeError.java
│   │       ├── RoomNotFoundError.java
│   │       ├── UserNotInRoomError.java
│   │       ├── ForbiddenError.java
│   │       └── EncryptionException.java
│   ├── ports/
│   │   ├── repository/
│   │   │   ├── RoomRepository.java
│   │   │   ├── MessageRepository.java
│   │   │   └── InviteRepository.java
│   │   └── EncryptionService.java
│   └── service/
│       ├── RoomMembershipValidator.java
│       ├── RoomAccessPolicy.java
│       └── MessageDeliveryPolicy.java
└── infrastructure/
    ├── adapter/
    │   ├── RoomRepositoryAdapter.java
    │   ├── MessageRepositoryAdapter.java
    │   ├── InviteRepositoryAdapter.java
    │   └── InMemoryRoomRepository.java
    ├── encryption/
    │   └── AesEncryptionService.java
    ├── event/
    │   ├── RoomEventListener.java
    │   ├── MessageEventListener.java
    │   └── InviteEventListener.java
    ├── http/
    │   ├── RoomController.java
    │   └── MessageController.java
    ├── persistence/
    │   ├── entity/
    │   │   ├── RoomJpaEntity.java
    │   │   ├── MessageJpaEntity.java
    │   │   └── InviteJpaEntity.java
    │   └── repository/
    │       ├── RoomJpaRepository.java
    │       ├── MessageJpaRepository.java
    │       └── InviteJpaRepository.java
    └── websocket/
        ├── ChatWebSocketHandler.java
        ├── WebSocketSessionManager.java
        └── dto/
            ├── ConnectResponse.java
            ├── ErrorResponse.java
            ├── IncomingMessage.java
            └── OutgoingMessage.java
```

## 3. Presence Context

### Purpose

Tracks and manages user online/offline status and activity states.

### Core Concepts

- **User Presence** - The current state of a user's availability
- **Presence State** - Online, offline, away, busy, invisible
- **Last Seen** - Timestamp of last user activity
- **Activity Tracking** - Updates based on user interactions

### Ubiquitous Language

| Term | Definition |
|------|------------|
| User Presence | The aggregate tracking a user's availability state |
| Presence State | Current status: ONLINE, OFFLINE, AWAY, BUSY, INVISIBLE |
| Last Seen | Timestamp of the last user activity |
| Online State | User is actively using the platform |
| Offline State | User is not currently using the platform |

### Domain Model

```java
// Aggregates
UserPresence extends BaseEntity {
    Long userId;
    PresenceState state;
    LastSeen lastSeen;
}

// Value Objects
PresenceState enum {
    ONLINE,
    OFFLINE,
    AWAY,
    BUSY,
    INVISIBLE
}

LastSeen(Instant timestamp) {
    static LastSeen now();
    boolean isOlderThan(Duration duration);
}

// Domain Events
PresenceEvents {
    USER_CAME_ONLINE,
    USER_WENT_OFFLINE,
    USER_STATE_CHANGED
}

// Domain Services
PresencePolicy {
    boolean canTransitionTo(PresenceState from, PresenceState to);
    Duration getAutoAwayTimeout();
    Duration getOfflineTimeout();
    PresenceState inferStateFromActivity(Instant lastActivity);
}

// Domain Errors
InvalidPresenceError
```

### Boundaries

**What Presence Context Owns:**
- User presence state
- Last seen timestamps
- Presence state transitions
- Activity tracking

**What Presence Context Doesn't Own:**
- User profile data (references User ID only)
- Room membership
- Messages

### Context Relationships

- **Downstream to Identity** - References User IDs only
- **Upstream to Collaboration** - Receives activity events from messaging
- **Upstream to Identity** - Receives events from authentication

### Module Structure

```
modules/presence/
├── application/
│   ├── dto/
│   │   └── PresenceStatus.java
│   ├── ports/
│   │   ├── in/
│   │   │   ├── TrackPresenceUseCase.java
│   │   │   └── QueryPresenceUseCase.java
│   │   └── out/
│   │       └── PresenceRepository.java
│   └── service/
│       └── PresenceService.java
├── domain/
│   ├── event/
│   │   └── PresenceEvents.java
│   ├── model/
│   │   ├── aggregate/
│   │   │   └── UserPresence.java
│   │   ├── value_object/
│   │   │   ├── PresenceState.java
│   │   │   └── LastSeen.java
│   │   └── error/
│   │       └── InvalidPresenceError.java
│   ├── ports/
│   │   └── repository/
│   │       └── PresenceRepository.java
│   └── service/
│       ├── PresencePolicy.java
│       └── DefaultPresencePolicy.java
└── infrastructure/
    ├── adapter/
    │   ├── InMemoryPresenceRepository.java
    │   └── RedisPresenceRepositoryAdapter.java
    ├── event/
    │   └── PresenceEventListener.java
    └── http/
        └── PresenceController.java
```

## Shared Kernel

The shared kernel contains concepts shared across all bounded contexts:

### Base Entity

```java
public abstract class BaseEntity {
    protected Long id;

    public Long getId() { return id; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity other)) return false;
        if (id == null || other.id == null) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
```

### Domain Error

```java
public abstract class DomainError extends RuntimeException {
    private final String code;

    protected DomainError(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
```

### Shared Ports

```java
public interface EventPublisher {
    void publish(Object event);
}

public interface Broadcaster {
    void broadcast(Object event);
}

public interface PresenceStore {
    void setOnline(Long userId);
    void setOffline(Long userId);
    Set<Long> getOnlineUserIds();
}

public interface RateLimiter {
    boolean isAllowed(String key, int maxRequests, long windowSeconds);
}
```

## Context Mapping

### Relationships Between Contexts

```
┌──────────────┐
│   Identity   │
│   Context    │
└──────┬───────┘
       │ User ID (Upstream)
       ▼
┌──────────────┐       publishes
│ Collaboration│◄──────────────────┐
│   Context    │                   │
└──────┬───────┘                   │
       │ User ID (Upstream)        │
       ▼                           │
┌──────────────┐       publishes   │
│  Presence    │◄──────────────────┤
│   Context    │                   │
└──────────────┘                   │
                                  │
                               ┌───┴────┐
                               │  Kafka │
                               │ Events │
                               └────────┘
```

### Integration Patterns

1. **Customer/Supplier (Identity → Collaboration)**
   - Identity supplies User IDs
   - Collaboration consumes User IDs
   - Relationship: Open Host Service

2. **Customer/Supplier (Identity → Presence)**
   - Identity supplies User IDs
   - Presence consumes User IDs
   - Relationship: Open Host Service

3. **Event-Driven (Collaboration ↔ Presence)**
   - Collaboration publishes message events
   - Presence publishes state change events
   - Relationship: Event-Driven Architecture

## Benefits of Bounded Contexts

### 1. Clear Boundaries

Each context has a clear responsibility and well-defined boundaries, making the system easier to understand and maintain.

### 2. Independent Evolution

Contexts can evolve independently as long as they respect their contracts (ports).

### 3. Team Autonomy

Different teams can work on different contexts with minimal coordination.

### 4. Scalability

Contexts can be deployed and scaled independently if needed.

### 5. Ubiquitous Language

Each context has its own language, reducing ambiguity and improving communication.

## Context Rules

1. **Respect Boundaries** - Never directly access another context's internal models
2. **Use IDs for References** - Reference entities by ID, not by object
3. **Define Clear Contracts** - Use ports to define interfaces between contexts
4. **Communicate via Events** - Use domain events for loose coupling
5. **Maintain Ubiquitous Language** - Use consistent terminology within each context

## Next Steps

- [Aggregates](./04-aggregates.md) - Learn about transaction boundaries within contexts
- [Domain Events](./06-domain-events.md) - Understand how contexts communicate
- [Value Objects](./05-value-objects.md) - Explore immutable domain concepts
