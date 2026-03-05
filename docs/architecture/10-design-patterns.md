# Design Patterns Used

## Introduction

Discord-like uses various design patterns to maintain clean, maintainable, and testable code. This document catalogs the patterns used throughout the application.

## 1. Factory Pattern

### Purpose

Encapsulate object creation logic, especially when creation involves validation or complex initialization.

### Usage

#### InviteFactory

Creates invite objects with auto-generated codes.

```java
@Service
public class InviteFactory {
    public Invite create(Long roomId, Long createdByUserId, Instant now) {
        String codeValue = generateCode();
        InviteCode code = new InviteCode(codeValue, createdByUserId);
        return new Invite(roomId, createdByUserId, code, now);
    }

    private String generateCode() {
        return UUID.randomUUID()
            .toString()
            .substring(0, 8)
            .toUpperCase();
    }
}
```

#### Domain Reconstitution

Domain objects use static factory methods for reconstitution.

```java
public class User extends BaseEntity {
    // Regular constructor for new objects
    public User(Username username, Email email, PasswordHash passwordHash, Instant createdAt) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = true;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    // Factory for reconstitution from persistence
    public static User reconstitute(Long id, Username username, Email email,
                                    PasswordHash passwordHash, boolean active,
                                    Instant createdAt, Instant updatedAt) {
        User user = new User();
        user.id = id;
        user.username = username;
        user.email = email;
        user.passwordHash = passwordHash;
        user.active = active;
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;
        return user;
    }
}
```

### Benefits

- Encapsulates creation logic
- Provides meaningful names for object creation
- Separates creation from use
- Enables reuse of complex creation logic

## 2. Decorator Pattern

### Purpose

Add behavior to an object dynamically without modifying its class.

### Usage

#### RateLimitedAuthService

Wraps AuthService to add rate limiting.

```java
@Primary
@Component
public class RateLimitedAuthService {
    private final AuthService authService;
    private final RateLimiter rateLimiter;

    public AuthResponse register(RegisterRequest request, String clientIp) {
        checkLimit(clientIp);
        return authService.register(request);
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        checkLimit(clientIp);
        return authService.login(request);
    }

    private void checkLimit(String key) {
        if (!rateLimiter.isAllowed(key, 5, 60)) {
            throw new RateLimitError(key);
        }
    }
}
```

### Benefits

- Open/Closed Principle - extend behavior without modifying original class
- Single Responsibility - separate concerns (auth vs rate limiting)
- Flexible - can add/remove decorators at runtime

## 3. Strategy Pattern

### Purpose

Define a family of algorithms, encapsulate each one, and make them interchangeable.

### Usage

#### PresencePolicy

Defines different presence behavior strategies.

```java
public interface PresencePolicy {
    boolean canTransitionTo(PresenceState from, PresenceState to);
    Duration getAutoAwayTimeout();
    Duration getOfflineTimeout();
    PresenceState inferStateFromActivity(Instant lastActivity);
}

@Service
@Primary
public class DefaultPresencePolicy implements PresencePolicy {
    private static final Duration AUTO_AWAY_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration OFFLINE_TIMEOUT = Duration.ofMinutes(15);

    @Override
    public boolean canTransitionTo(PresenceState from, PresenceState to) {
        // Business rules for state transitions
        return true;
    }

    @Override
    public Duration getAutoAwayTimeout() {
        return AUTO_AWAY_TIMEOUT;
    }

    @Override
    public Duration getOfflineTimeout() {
        return OFFLINE_TIMEOUT;
    }

    @Override
    public PresenceState inferStateFromActivity(Instant lastActivity) {
        Duration inactive = Duration.between(lastActivity, Instant.now());
        if (inactive.compareTo(getOfflineTimeout()) > 0) {
            return PresenceState.OFFLINE;
        } else if (inactive.compareTo(getAutoAwayTimeout()) > 0) {
            return PresenceState.AWAY;
        }
        return PresenceState.ONLINE;
    }
}
```

#### RoomAccessPolicy & MessageDeliveryPolicy

Similar strategy interfaces for room and message access.

### Benefits

- Interchangeable algorithms
- Open/Closed Principle - easy to add new strategies
- Separates algorithm from context

## 4. Builder Pattern

### Purpose

Construct complex objects step by step.

### Usage

#### DTOs with Builder-like Constructors

While not using traditional builders, the pattern is implicit in DTO construction.

```java
// Record-based construction (implicit builder)
CreateRoomRequest request = new CreateRoomRequest("General Chat");

// Or using @ValidatedRequest with @Valid in controllers
@PostMapping("/rooms")
public ResponseEntity<RoomResponse> createRoom(
    @Valid @RequestBody CreateRoomRequest request
) {
    Room room = roomService.createRoom(request.name(), userId, Instant.now());
    return ResponseEntity.ok(RoomResponse.fromDomain(room));
}
```

### Benefits

- Readable object creation
- Step-by-step construction
- Handles optional parameters gracefully

## 5. Observer Pattern

### Purpose

Define a one-to-many dependency so that when one object changes state, all dependents are notified.

### Usage

#### Domain Events & Event Listeners

```java
// Publisher (Subject)
@Service
public class RoomService {
    private final EventPublisher eventPublisher;

    public Room createRoom(String name, Long ownerId, Instant now) {
        Room room = new Room(new RoomName(name), ownerId, now);
        Room saved = roomRepository.save(room);
        eventPublisher.publish(RoomEvents.roomCreated(saved, now));
        return saved;
    }
}

// Observers (Listeners)
@Component
public class RoomEventListener {
    @KafkaListener(topics = "room-events")
    public void handle(RoomEvents event) {
        broadcaster.broadcast(event);
    }
}

// Multiple observers can subscribe to same events
@Component
public class RoomAnalyticsListener {
    @KafkaListener(topics = "room-events")
    public void handle(RoomEvents event) {
        analytics.track(event);
    }
}
```

### Benefits

- Loose coupling between publisher and subscribers
- Multiple subscribers can react to same event
- Easy to add new subscribers

## 6. Repository Pattern

### Purpose

Abstract data access, providing collection-like interface to domain objects.

### Usage

#### Repository Interface (Domain Port)

```java
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

#### JPA Implementation (Infrastructure Adapter)

```java
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

### Benefits

- Abstracts data access details
- Testable (can mock repositories)
- Switchable implementations
- Centralizes data access logic

## 7. Adapter Pattern

### Purpose

Convert the interface of a class into another interface clients expect.

### Usage

#### JPA Repository Adapters

```java
// Domain Port
public interface UserRepository {
    User save(User user);
}

// Infrastructure Adapter
@Component
public class JpaUserRepository implements UserRepository {
    private final SpringDataUserRepository jpa;  // Spring Data JPA

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserJpaEntity.fromDomain(user);
        return jpa.save(entity).toDomain();
    }
}
```

#### Event Publisher Adapter

```java
// Domain Port
public interface EventPublisher {
    void publish(Object event);
}

// Infrastructure Adapter
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

### Benefits

- Enables incompatible interfaces to work together
- Separates domain from infrastructure
- Enables swapping implementations

## 8. Dependency Inversion Principle

### Purpose

Depend on abstractions, not concretions.

### Usage

#### Application Service Depends on Ports

```java
@Service
public class RoomService {
    // Depends on port interface, not concrete implementation
    private final RoomRepository roomRepository;
    private final EventPublisher eventPublisher;

    public RoomService(
        RoomRepository roomRepository,  // Interface
        EventPublisher eventPublisher   // Interface
    ) {
        this.roomRepository = roomRepository;
        this.eventPublisher = eventPublisher;
    }
}
```

#### Domain Layer Defines Ports

```java
// Domain defines the contract
public interface UserRepository {
    User save(User user);
}

// Infrastructure provides implementation
@Component
public class JpaUserRepository implements UserRepository { }
```

### Benefits

- Loose coupling
- Easy to test (mock dependencies)
- Swappable implementations
- Clear layer boundaries

## 9. Template Method Pattern

### Purpose

Define the skeleton of an algorithm, deferring some steps to subclasses.

### Usage

#### BaseEntity

```java
public abstract class BaseEntity {
    protected Long id;

    protected BaseEntity() {}

    protected BaseEntity(Long id) {
        this.id = id;
    }

    // Template method for equality
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity other)) return false;
        if (id == null || other.id == null) return false;
        return id.equals(other.id);
    }

    // Template method for hash code
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
```

### Benefits

- Reusable algorithm structure
- Consistent behavior across subclasses
- Reduces code duplication

## 10. Value Object Pattern

### Purpose

Represent descriptive aspects of the domain with no identity.

### Usage

#### Record-based Value Objects

```java
public record Username(String value) {
    public Username {
        if (value == null || value.isBlank()) {
            throw InvalidUserError.invalidUsername("Username cannot be blank");
        }
    }
}

public record Email(String value) {
    public Email {
        if (!isValidEmail(value)) {
            throw InvalidUserError.invalidEmail("Invalid email format");
        }
    }
}
```

### Benefits

- Immutable by design
- Self-validating
- Value-based equality
- Type safety

## 11. Aggregate Pattern

### Purpose

Cluster related objects as a unit, defining transaction boundaries.

### Usage

#### Room Aggregate

```java
public class Room extends BaseEntity {
    private RoomName name;
    private Long ownerId;
    private final Set<Long> memberIds = new HashSet<>();

    // Aggregate root methods enforce invariants
    public void addMember(Long userId, Instant updatedAt) {
        if (userId == null) {
            throw new InvalidRoomError("User ID cannot be null");
        }
        this.memberIds.add(userId);
        this.updatedAt = updatedAt;
    }

    public void removeMember(Long userId, Instant updatedAt) {
        if (this.memberIds.size() <= 1) {
            throw new InvalidRoomError("Cannot remove last member");
        }
        if (this.ownerId.equals(userId)) {
            throw new InvalidRoomError("Cannot remove room owner");
        }
        this.memberIds.remove(userId);
        this.updatedAt = updatedAt;
    }
}
```

### Benefits

- Enforces invariants
- Defines transaction boundaries
- Ensures consistency
- Clear ownership

## 12. Domain Event Pattern

### Purpose

Represent something that happened in the domain.

### Usage

#### Event Records

```java
public record UserEvents(
    Long userId,
    String username,
    String email,
    Instant occurredAt,
    EventType type
) {
    public enum EventType {
        REGISTERED,
        PASSWORD_CHANGED,
        DEACTIVATED,
        ACTIVATED
    }

    public static UserEvents registered(User user, Instant occurredAt) {
        return new UserEvents(
            user.getId(),
            user.getUsername().value(),
            user.getEmail().value(),
            occurredAt,
            EventType.REGISTERED
        );
    }
}
```

### Benefits

- Loose coupling between contexts
- Audit trail
- Asynchronous processing
- Multiple subscribers

## Pattern Summary

| Pattern | Purpose | Example |
|---------|---------|---------|
| Factory | Encapsulate object creation | InviteFactory, reconstitute() |
| Decorator | Add behavior dynamically | RateLimitedAuthService |
| Strategy | Interchangeable algorithms | PresencePolicy |
| Builder | Step-by-step construction | DTOs |
| Observer | Notify dependents of changes | Domain Events |
| Repository | Abstract data access | UserRepository |
| Adapter | Convert interfaces | JpaUserRepository |
| Dependency Inversion | Depend on abstractions | Port interfaces |
| Template Method | Define algorithm skeleton | BaseEntity |
| Value Object | Descriptive domain concepts | Username, Email |
| Aggregate | Transaction boundary | Room, User |
| Domain Event | Represent domain facts | UserEvents |

## Benefits of Using Patterns

1. **Maintainability** - Patterns provide proven solutions
2. **Readability** - Patterns are recognizable to experienced developers
3. **Flexibility** - Patterns enable easy modification
4. **Testability** - Patterns support testing
5. **Communication** - Patterns provide common vocabulary

## Next Steps

- [Project Structure](../development/01-project-structure.md) - Learn file organization
- [Testing Guide](../development/02-testing.md) - Understand testing approach
