# Repository Pattern

## Introduction

The Repository pattern provides collection-like access to domain objects while abstracting the underlying data store. In DDD, repositories are part of the infrastructure layer that implements domain ports for data persistence.

## Repository Design Principles

1. **Domain Port Interface** - Repository interfaces are defined in the domain layer
2. **Collection-Like** - Methods resemble collection operations (save, find, delete)
3. **Aggregate Roots Only** - Repositories work with aggregate roots, not internal entities
4. **Hide Implementation** - Concrete implementations are in infrastructure layer
5. **Transaction Boundaries** - Repository operations are transactional

## Repository Interfaces (Domain Ports)

### Identity Context Repositories

#### UserRepository

```java
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

**Purpose**: Manage user aggregate persistence.

**Methods**:
- `save()` - Create or update user
- `findById()` - Find user by ID
- `findByEmail()` - Find user by email (for authentication)
- `existsByEmail()` - Check if email exists (for validation)

### Collaboration Context Repositories

#### RoomRepository

```java
public interface RoomRepository {
    Room save(Room room);
    Optional<Room> findById(Long id);
    Optional<Room> findByInviteCode(String inviteCode);
    List<Room> findByMemberId(Long memberId);
    void deleteById(Long id);
    boolean existsById(Long id);
}
```

**Purpose**: Manage room aggregate persistence.

**Methods**:
- `save()` - Create or update room
- `findById()` - Find room by ID
- `findByInviteCode()` - Find room by invite code
- `findByMemberId()` - Find all rooms for a member
- `deleteById()` - Delete room by ID
- `existsById()` - Check if room exists

#### MessageRepository

```java
public interface MessageRepository {
    Message save(Message message);
    Optional<Message> findById(Long id);
    Optional<Message> findByIdAndRoomId(Long messageId, Long roomId);
    List<Message> findByRoomId(Long roomId);
    List<Message> findByRoomId(Long roomId, int limit, int offset);
    List<Message> findByRoomIdBefore(Long roomId, Long beforeMessageId, int limit);
    void deleteById(Long id);
    void deleteByRoomId(Long roomId);
    long countByRoomId(Long roomId);
}
```

**Purpose**: Manage message aggregate persistence.

**Methods**:
- `save()` - Create or update message
- `findById()` - Find message by ID
- `findByIdAndRoomId()` - Find message in specific room
- `findByRoomId()` - Find all messages in room
- `findByRoomId(limit, offset)` - Paginated messages
- `findByRoomIdBefore()` - Cursor-based pagination
- `deleteById()` - Delete message by ID
- `deleteByRoomId()` - Delete all messages in room
- `countByRoomId()` - Count messages in room

#### InviteRepository

```java
public interface InviteRepository {
    Invite save(Invite invite);
    Optional<Invite> findById(Long id);
    Optional<Invite> findByCode(String code);
    Optional<Invite> findByRoomIdAndCode(Long roomId, String code);
    List<Invite> findByRoomId(Long roomId);
    void deleteById(Long id);
    void deleteByRoomId(Long roomId);
}
```

**Purpose**: Manage invite aggregate persistence.

**Methods**:
- `save()` - Create or update invite
- `findById()` - Find invite by ID
- `findByCode()` - Find invite by code
- `findByRoomIdAndCode()` - Find invite in specific room
- `findByRoomId()` - Find all invites for room
- `deleteById()` - Delete invite by ID
- `deleteByRoomId()` - Delete all invites for room

### Presence Context Repositories

#### PresenceRepository

```java
public interface PresenceRepository {
    UserPresence save(UserPresence presence);
    Optional<UserPresence> findById(Long id);
    Optional<UserPresence> findByUserId(Long userId);
    Set<Long> getOnlineUserIds();
    void deleteById(Long id);
}
```

**Purpose**: Manage user presence aggregate persistence.

**Methods**:
- `save()` - Create or update presence
- `findById()` - Find presence by ID
- `findByUserId()` - Find presence by user ID
- `getOnlineUserIds()` - Get all online user IDs
- `deleteById()` - Delete presence by ID

## Repository Implementations (Infrastructure Adapters)

### JPA-Based Implementations

#### JpaUserRepository

```java
@Component
public class JpaUserRepository implements UserRepository {
    private final SpringDataUserRepository jpa;

    public JpaUserRepository(SpringDataUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserJpaEntity.fromDomain(user);
        return jpa.save(entity).toDomain();
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }
}
```

#### RoomRepositoryAdapter

```java
@Component
public class RoomRepositoryAdapter implements RoomRepository {
    private final RoomJpaRepository jpaRepository;

    public RoomRepositoryAdapter(RoomJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Room save(Room room) {
        RoomJpaEntity entity = RoomJpaEntity.fromDomain(room);
        RoomJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Room> findById(Long id) {
        return jpaRepository.findById(id).map(RoomJpaEntity::toDomain);
    }

    @Override
    public Optional<Room> findByInviteCode(String inviteCode) {
        return jpaRepository.findByInviteCode(inviteCode).map(RoomJpaEntity::toDomain);
    }

    @Override
    public List<Room> findByMemberId(Long memberId) {
        return jpaRepository.findByMemberId(memberId).stream()
            .map(RoomJpaEntity::toDomain)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
```

#### MessageRepositoryAdapter

```java
@Component
public class MessageRepositoryAdapter implements MessageRepository {
    private final MessageJpaRepository jpaRepository;

    public MessageRepositoryAdapter(MessageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Message save(Message message) {
        MessageJpaEntity entity = MessageJpaEntity.fromDomain(message);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Message> findById(Long id) {
        return jpaRepository.findById(id).map(MessageJpaEntity::toDomain);
    }

    @Override
    public Optional<Message> findByIdAndRoomId(Long messageId, Long roomId) {
        return jpaRepository.findByIdAndRoomId(messageId, roomId)
            .map(MessageJpaEntity::toDomain);
    }

    @Override
    public List<Message> findByRoomId(Long roomId) {
        return jpaRepository.findByRoomId(roomId).stream()
            .map(MessageJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<Message> findByRoomId(Long roomId, int limit, int offset) {
        return jpaRepository.findByRoomId(roomId, limit, offset).stream()
            .map(MessageJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<Message> findByRoomIdBefore(Long roomId, Long beforeMessageId, int limit) {
        return jpaRepository.findByRoomIdBefore(roomId, beforeMessageId, limit).stream()
            .map(MessageJpaEntity::toDomain)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByRoomId(Long roomId) {
        jpaRepository.deleteByRoomId(roomId);
    }

    @Override
    public long countByRoomId(Long roomId) {
        return jpaRepository.countByRoomId(roomId);
    }
}
```

### In-Memory Implementations (for Testing)

#### InMemoryUserRepository

```java
@Component
@Profile("test")
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> storage = new ConcurrentHashMap<>();
    private final Map<String, Long> emailIndex = new ConcurrentHashMap<>();
    private AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user = new User(user.getUsername(), user.getEmail(), user.getPasswordHash(), Instant.now());
            user.setId(idGenerator.getAndIncrement());
        }
        storage.put(user.getId(), user);
        emailIndex.put(user.getEmail().value(), user.getId());
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        Long id = emailIndex.get(email);
        return id != null ? Optional.ofNullable(storage.get(id)) : Optional.empty();
    }

    @Override
    public boolean existsByEmail(String email) {
        return emailIndex.containsKey(email);
    }
}
```

## Entity-to-Domain Mapping

### Pattern Overview

JPA entities are mapped to domain objects via `toDomain()` and `fromDomain()` methods.

### User Example

#### Domain Object

```java
public class User extends BaseEntity {
    private Username username;
    private Email email;
    private PasswordHash passwordHash;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    // Reconstitution factory
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

#### JPA Entity

```java
@Entity
@Table(name = "users")
public class UserJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // Domain to Entity
    public static UserJpaEntity fromDomain(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setUsername(user.getUsername().value());
        entity.setEmail(user.getEmail().value());
        entity.setPasswordHash(user.getPasswordHash().value());
        entity.setActive(user.isActive());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }

    // Entity to Domain
    public User toDomain() {
        return User.reconstitute(
            id,
            new Username(username),
            new Email(email),
            new PasswordHash(passwordHash),
            active,
            createdAt,
            updatedAt
        );
    }
}
```

#### Repository Usage

```java
@Override
public User save(User user) {
    UserJpaEntity entity = UserJpaEntity.fromDomain(user);
    return jpaRepository.save(entity).toDomain();
}
```

### Room Example

#### Domain Object

```java
public class Room extends BaseEntity {
    private RoomName name;
    private Long ownerId;
    private final Set<Long> memberIds = new HashSet<>();
    private Instant createdAt;
    private Instant updatedAt;

    public static Room reconstitute(Long id, RoomName name, Long ownerId,
                                    Set<Long> memberIds, Instant createdAt, Instant updatedAt) {
        Room room = new Room();
        room.id = id;
        room.name = name;
        room.ownerId = ownerId;
        room.memberIds.addAll(memberIds);
        room.createdAt = createdAt;
        room.updatedAt = updatedAt;
        return room;
    }
}
```

#### JPA Entity

```java
@Entity
@Table(name = "rooms")
public class RoomJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long ownerId;

    @ElementCollection
    @CollectionTable(name = "room_members", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "user_id")
    private Set<Long> memberIds = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static RoomJpaEntity fromDomain(Room room) {
        RoomJpaEntity entity = new RoomJpaEntity();
        entity.setId(room.getId());
        entity.setName(room.getName().value());
        entity.setOwnerId(room.getOwnerId());
        entity.setMemberIds(new HashSet<>(room.getMemberIds()));
        entity.setCreatedAt(room.getCreatedAt());
        entity.setUpdatedAt(room.getUpdatedAt());
        return entity;
    }

    public Room toDomain() {
        return Room.reconstitute(
            id,
            new RoomName(name),
            ownerId,
            Set.copyOf(memberIds),
            createdAt,
            updatedAt
        );
    }
}
```

## Transaction Management

### Application Service Transaction Boundary

```java
@Service
@Transactional
public class RoomService {
    private final RoomRepository roomRepository;

    public Room createRoom(String name, Long ownerId, Instant now) {
        Room room = new Room(new RoomName(name), ownerId, now);
        Room saved = roomRepository.save(room);  // Within transaction
        eventPublisher.publish(RoomEvents.roomCreated(saved, now));
        return saved;
    }

    public void addMember(Long roomId, Long invitedUserId, Instant now) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RoomNotFoundError(roomId.toString()));
        room.addMember(invitedUserId, now);
        roomRepository.save(room);  // Within transaction
        eventPublisher.publish(RoomEvents.memberJoined(roomId, invitedUserId, now));
    }
}
```

### Transaction Characteristics

- **Atomicity** - All operations in a transaction succeed or fail together
- **Consistency** - Database transitions from one valid state to another
- **Isolation** - Concurrent transactions don't interfere
- **Durability** - Committed changes persist

## Pagination Strategies

### Offset-Based Pagination

```java
// Repository interface
List<Message> findByRoomId(Long roomId, int limit, int offset);

// JPA implementation
@Query("SELECT m FROM MessageJpaEntity m WHERE m.roomId = :roomId ORDER BY m.createdAt DESC")
List<MessageJpaEntity> findByRoomId(@Param("roomId") Long roomId, int limit, int offset);
```

**Pros**:
- Simple to implement
- Easy to understand
- Works with standard SQL

**Cons**:
- Performance degrades with large offsets
- Can miss or duplicate items if data changes during pagination

### Cursor-Based Pagination

```java
// Repository interface
List<Message> findByRoomIdBefore(Long roomId, Long beforeMessageId, int limit);

// JPA implementation
@Query("SELECT m FROM MessageJpaEntity m WHERE m.roomId = :roomId AND m.id < :beforeMessageId ORDER BY m.id DESC")
List<MessageJpaEntity> findByRoomIdBefore(@Param("roomId") Long roomId,
                                           @Param("beforeMessageId") Long beforeMessageId,
                                           Pageable pageable);
```

**Pros**:
- Consistent performance regardless of offset
- No missing or duplicate items
- Better for real-time applications

**Cons**:
- Slightly more complex
- Requires ordering column

**Usage in Discord-like**:
```java
// Get messages before a specific message
public List<Message> getMessagesBefore(Long roomId, Long beforeMessageId, int limit) {
    return messageRepository.findByRoomIdBefore(roomId, beforeMessageId, limit);
}
```

## Spring Data JPA Query Methods

### Method Name Queries

```java
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}

public interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, Long> {
    Optional<RoomJpaEntity> findByInviteCode(String inviteCode);
    List<RoomJpaEntity> findByMemberId(Long memberId);
}
```

### @Query Annotations

```java
public interface MessageJpaRepository extends JpaRepository<MessageJpaEntity, Long> {
    @Query("SELECT m FROM MessageJpaEntity m WHERE m.id = :id AND m.roomId = :roomId")
    Optional<MessageJpaEntity> findByIdAndRoomId(@Param("id") Long id, @Param("roomId") Long roomId);

    @Query("SELECT m FROM MessageJpaEntity m WHERE m.roomId = :roomId ORDER BY m.createdAt DESC")
    List<MessageJpaEntity> findByRoomId(@Param("roomId") Long roomId, Pageable pageable);

    @Query("SELECT m FROM MessageJpaEntity m WHERE m.roomId = :roomId AND m.id < :beforeMessageId ORDER BY m.id DESC")
    List<MessageJpaEntity> findByRoomIdBefore(@Param("roomId") Long roomId,
                                               @Param("beforeMessageId") Long beforeMessageId,
                                               Pageable pageable);

    long countByRoomId(Long roomId);

    @Modifying
    @Query("DELETE FROM MessageJpaEntity m WHERE m.roomId = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);
}
```

## Repository Best Practices

### 1. Work with Aggregates Only

```java
// ✅ Good - repository returns aggregate root
Room room = roomRepository.findById(roomId);

// ❌ Bad - accessing internal entities directly
Set<Member> members = roomRepository.findMembersByRoomId(roomId);
```

### 2. Return Domain Objects, Not Entities

```java
// ✅ Good - returns domain object
public User save(User user) {
    UserJpaEntity entity = UserJpaEntity.fromDomain(user);
    return jpaRepository.save(entity).toDomain();
}

// ❌ Bad - returns JPA entity
public UserJpaEntity save(UserJpaEntity entity) {
    return jpaRepository.save(entity);
}
```

### 3. Use Optional for Single Results

```java
// ✅ Good - uses Optional
Optional<User> findById(Long id);
Optional<User> findByEmail(String email);

// ❌ Bad - returns null
User findById(Long id);
User findByEmail(String email);
```

### 4. Keep Repository Interfaces Simple

```java
// ✅ Good - simple, collection-like interface
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

// ❌ Bad - complex business logic in repository
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    User findOrCreateByEmail(String email);
    User registerAndSendEmail(User user);
    List<User> findActiveUsersWithMoreThan10Messages();
}
```

### 5. Use Domain Ports for Abstraction

```java
// ✅ Good - domain port defined in domain layer
// modules/identity/domain/ports/repository/UserRepository.java
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
}

// ✅ Good - infrastructure adapter implements port
// modules/identity/infrastructure/adapters/JpaUserRepository.java
@Component
public class JpaUserRepository implements UserRepository {
    // Implementation...
}

// ❌ Bad - direct dependency on Spring Data in domain
public interface UserRepository extends JpaRepository<User, Long> {
    // Don't do this in domain layer!
}
```

## Benefits of Repository Pattern

### 1. Testability

Repositories can be mocked for testing:

```java
@Test
void shouldCreateUser() {
    // Given
    UserRepository mockRepo = mock(UserRepository.class);
    AuthService service = new AuthService(mockRepo, /*...*/);

    // When
    service.register(new RegisterRequest(/*...*/));

    // Then
    verify(mockRepo).save(any(User.class));
}
```

### 2. Flexibility

Implementation can be swapped:

```java
@Primary
@Component
public class JpaUserRepository implements UserRepository { }

@Component
@Profile("test")
public class InMemoryUserRepository implements UserRepository { }
```

### 3. Encapsulation

Data access details are hidden:

```java
// Application service doesn't know about JPA or SQL
Room room = roomRepository.findById(roomId);
```

### 4. Centralized Data Access

All data access goes through repositories:

```java
// Easy to add logging, caching, etc.
@Component
public class LoggingRoomRepository implements RoomRepository {
    private final RoomRepository delegate;

    @Override
    public Room save(Room room) {
        log.info("Saving room: {}", room.getId());
        return delegate.save(room);
    }
}
```

## Next Steps

- [Application Services](./08-application-services.md) - Learn how services use repositories
- [Infrastructure Adapters](./09-infrastructure-adapters.md) - See infrastructure implementation details
- [Design Patterns](./10-design-patterns.md) - Explore other patterns used
