# Hexagonal Architecture (Ports & Adapters)

## Introduction

Hexagonal Architecture, also known as Ports & Adapters, is an architectural pattern that separates an application into layers based on their responsibilities. This pattern ensures that the core business logic (domain) remains independent of external concerns like databases, web frameworks, and messaging systems.

## Core Concept

The hexagonal architecture organizes code into concentric layers:

1. **Domain Layer** - Core business logic and rules
2. **Application Layer** - Use case orchestration
3. **Infrastructure Layer** - Technical implementations
4. **Interfaces Layer** - External API endpoints

### The "Hexagon"

The "hexagon" represents the application boundary. The inside contains domain logic, while the outside contains technical details. Ports are the interfaces that cross this boundary.

```
                    ┌─────────────────────────┐
                    │     Interfaces Layer    │
                    │  (Controllers, WS, etc) │
                    └───────────┬─────────────┘
                                │
                    ┌───────────┴─────────────┐
                    │   Application Layer      │
                    │  (Use Cases, DTOs)       │
                    └───────────┬─────────────┘
                                │
                    ┌───────────┴─────────────┐
                    │     Domain Layer        │
                    │ (Entities, Value Objects)│
                    └───────────┬─────────────┘
                                │
              ┌─────────────────┼─────────────────┐
              │                 │                 │
    ┌─────────┴─────┐  ┌────────┴────────┐  ┌───┴──────┐
    │  Infrastructure│  │  Infrastructure │  │Infrastructure│
    │  (Adapters)    │  │  (Adapters)     │  │(Adapters)    │
    │                │  │                 │  │              │
    │  Database      │  │  Messaging      │  │  External    │
    │  (PostgreSQL)  │  │  (Kafka)        │  │  Services    │
    └────────────────┘  └─────────────────┘  └──────────────┘
```

## Dependency Flow

The key principle of hexagonal architecture is **inward dependency**:

```
Infrastructure → Ports ← Application ← Domain
```

- **Domain** depends on nothing
- **Application** depends on Domain
- **Infrastructure** depends on Application (via ports)
- **Interfaces** depends on Application

This ensures the domain remains pure and testable.

## Ports and Adapters

### Ports

Ports are interfaces defined in the domain layer that define contracts for external interactions. They are of two types:

#### 1. Driving Ports (Primary/Inbound)

Ports that the application implements for external actors to drive the application:

- Application service interfaces
- Use case interfaces
- Command/query interfaces

Example from Presence module:

```java
// TrackPresenceUseCase.java
public interface TrackPresenceUseCase {
    void setOnline(Long userId);
    void setOffline(Long userId);
    void setPresenceState(Long userId, PresenceState state);
    void updateLastActivity(Long userId);
}

// QueryPresenceUseCase.java
public interface QueryPresenceUseCase {
    PresenceStatus getPresenceStatus(Long userId);
    Set<PresenceStatus> getOnlineUsers();
    Set<Long> getOnlineUserIds();
}
```

#### 2. Driven Ports (Secondary/Outbound)

Ports that the application depends on for infrastructure concerns:

- Repository interfaces
- Service interfaces
- Publisher interfaces

Example:

```java
// UserRepository.java (Driven Port)
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

// EventPublisher.java (Driven Port)
public interface EventPublisher {
    void publish(Object event);
}

// EncryptionService.java (Driven Port)
public interface EncryptionService {
    String encrypt(String plaintext);
    String decrypt(String ciphertext);
}
```

### Adapters

Adapters are concrete implementations of ports. They handle the technical details of external systems.

#### 1. Driving Adapters (Primary)

Adapters that drive the application by calling driving ports:

- REST Controllers
- WebSocket Handlers
- Event Listeners
- CLI Commands

Example:

```java
// AuthController.java (Driving Adapter)
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final RateLimitedAuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest httpRequest
    ) {
        String clientIp = httpRequest.getRemoteAddr();
        AuthResponse response = authService.register(request, clientIp);
        return ResponseEntity.ok(response);
    }
}
```

#### 2. Driven Adapters (Secondary)

Adapters that implement driven ports and handle external system integration:

- JPA Repository implementations
- Kafka Event Publisher
- Redis Presence Store
- External Service Clients

Example:

```java
// JpaUserRepository.java (Driven Adapter)
@Component
public class JpaUserRepository implements UserRepository {
    private final SpringDataUserRepository jpa;

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserJpaEntity.fromDomain(user);
        return jpa.save(entity).toDomain();
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id).map(UserJpaEntity::toDomain);
    }
}

// KafkaEventPublisher.java (Driven Adapter)
@Service
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(Object event) {
        String topicName = getTopicForEvent(event);
        kafkaTemplate.send(topicName, event);
    }
}
```

## Implementation in Discord-like

### Layer Structure

```
src/main/java/com/luishbarros/discord_like/
├── modules/
│   ├── identity/
│   │   ├── application/          # Application Layer
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   └── service/         # Application Services (Driving Ports)
│   │   ├── domain/              # Domain Layer
│   │   │   ├── event/           # Domain Events
│   │   │   ├── model/           # Entities, Value Objects, Aggregates
│   │   │   ├── ports/           # Ports (Repository, Services)
│   │   │   └── service/         # Domain Services
│   │   └── infrastructure/       # Infrastructure Layer
│   │       ├── adapters/        # Driven Adapters
│   │       ├── event/           # Event Listeners (Driving Adapters)
│   │       ├── http/            # REST Controllers (Driving Adapters)
│   │       └── security/        # Security Adapters
│   ├── collaboration/
│   │   └── ... (same structure)
│   └── presence/
│       └── ... (same structure)
└── shared/
    ├── adapters/                # Shared Infrastructure Adapters
    ├── domain/                  # Shared Domain Concepts
    └── ports/                   # Cross-cutting Ports
```

### Key Patterns

#### 1. Domain Isolation

Domain classes have zero dependencies on Spring or external libraries:

```java
// User.java - Pure domain class
public class User extends BaseEntity {
    private Username username;
    private Email email;
    private PasswordHash passwordHash;
    private boolean active;

    protected User() {}

    public User(Username username, Email email, PasswordHash passwordHash, Instant createdAt) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = true;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void changePassword(PasswordHash newPasswordHash, Instant updatedAt) {
        if (!active) {
            throw new InvalidUserError("Cannot change password of inactive user");
        }
        this.passwordHash = newPasswordHash;
        this.updatedAt = updatedAt;
    }
}
```

#### 2. Repository Pattern

Repository interfaces in domain, JPA adapters in infrastructure:

```java
// Domain Port
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

// Infrastructure Adapter
@Component
public class JpaUserRepository implements UserRepository {
    private final SpringDataUserRepository jpa;

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserJpaEntity.fromDomain(user);
        return jpa.save(entity).toDomain();
    }
}
```

#### 3. Domain Event Publishing

Application services publish events via the EventPublisher port:

```java
@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    public AuthResponse register(RegisterRequest request) {
        User user = new User(/* ... */);
        User saved = userRepository.save(user);
        eventPublisher.publish(UserEvents.registered(saved, Instant.now()));
        return new AuthResponse(accessToken, refreshToken);
    }
}
```

#### 4. Event Listeners

Infrastructure adapters consume events and broadcast:

```java
@Component
public class UserEventListener implements EventListener<UserEvents> {
    private final Broadcaster broadcaster;

    @KafkaListener(topics = "user-events")
    public void handle(UserEvents event) {
        broadcaster.broadcast(event);
    }
}
```

## Benefits

### 1. Testability

Domain logic can be tested without any infrastructure:

```java
class UserTest {
    @Test
    void cannotChangePasswordWhenInactive() {
        User user = new User(/* ... */);
        user.deactivate(Instant.now());

        assertThrows(InvalidUserError.class,
            () -> user.changePassword(new PasswordHash("hash"), Instant.now()));
    }
}
```

### 2. Flexibility

Infrastructure can be swapped without changing domain logic:

```java
// In-memory for tests
@Component
@Profile("test")
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> storage = new ConcurrentHashMap<>();
    // Implementation...
}

// JPA for production
@Component
@Primary
public class JpaUserRepository implements UserRepository {
    // Implementation...
}
```

### 3. Maintainability

Clear separation makes the codebase easier to navigate and modify:

- Domain changes stay in domain layer
- Infrastructure changes stay in infrastructure layer
- Application logic stays in application layer

### 4. Loose Coupling

Modules communicate through well-defined interfaces (ports):

```java
// Collaboration module depends only on User ID, not User entity
public class RoomService {
    public void addMember(Long roomId, Long invitedUserId, Instant now) {
        // Works with any user implementation
    }
}
```

## Trade-offs

### 1. Complexity

- More layers and interfaces
- Increased boilerplate code
- Steeper learning curve

**Mitigation**: The structure is consistent across modules, reducing cognitive load.

### 2. Performance

- Additional abstraction layers
- Potential object mapping overhead

**Mitigation**: Mapping is minimal and only at boundaries; most work happens in domain.

### 3. Development Time

- More files and interfaces to create
- Requires thinking in layers

**Mitigation**: Consistent patterns and code generation can help.

## Best Practices

### 1. Keep Domain Pure

- No Spring annotations in domain classes
- No external dependencies in domain
- Domain logic stays in domain layer

### 2. Use Ports for All External Interactions

- Database access via Repository ports
- Messaging via Publisher/Listener ports
- External services via Service ports

### 3. Application Services are Thin

- Orchestrate use cases
- No business logic
- Coordinate between domain and infrastructure

### 4. Infrastructure is Swappable

- Implement ports, don't extend them
- Use dependency injection
- Support multiple implementations

## Summary

Hexagonal architecture in Discord-like provides:

- **Clean separation** between business logic and technical concerns
- **Testability** of domain logic without infrastructure
- **Flexibility** to swap implementations
- **Maintainability** through clear layer boundaries
- **Loose coupling** between modules via ports

The investment in this architecture pays off in long-term maintainability and the ability to evolve the system as requirements change.

## Next Steps

- [Bounded Contexts](./03-bounded-contexts.md) - Learn how contexts are defined
- [Aggregates](./04-aggregates.md) - Understand transaction boundaries
- [Value Objects](./05-value-objects.md) - Explore immutable domain concepts
