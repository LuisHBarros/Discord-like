# Application Services

## Introduction

Application services orchestrate use cases and coordinate between domain objects, repositories, and infrastructure. They contain no business logic - they delegate to domain objects and infrastructure through ports.

## Application Service Responsibilities

1. **Use Case Orchestration** - Coordinate domain operations
2. **Transaction Management** - Define transaction boundaries
3. **Event Publishing** - Publish domain events
4. **DTO Mapping** - Convert between DTOs and domain objects
5. **Error Handling** - Handle domain errors appropriately

## Service Examples

### Identity Context Services

#### AuthService

Handles authentication and user registration.

```java
@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final EventPublisher eventPublisher;

    public AuthResponse register(RegisterRequest request) {
        // 1. Validate business rule
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailError(request.email());
        }

        // 2. Use domain objects
        String passwordHash = passwordHasher.hash(request.password());
        User user = new User(
            new Username(request.username()),
            new Email(request.email()),
            new PasswordHash(passwordHash),
            Instant.now()
        );

        // 3. Persist via repository
        User saved = userRepository.save(user);

        // 4. Publish domain event
        eventPublisher.publish(UserEvents.registered(saved, Instant.now()));

        // 5. Return response
        String accessToken = tokenProvider.generateAccessToken(saved.getId());
        String refreshToken = tokenProvider.generateRefreshToken(saved.getId());
        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new InvalidCredentialsError("Invalid email or password"));

        if (!passwordHasher.verify(request.password(), user.getPasswordHash().value())) {
            throw new InvalidCredentialsError("Invalid email or password");
        }

        String accessToken = tokenProvider.generateAccessToken(user.getId());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());
        return new AuthResponse(accessToken, refreshToken);
    }

    public void logout(String accessToken, String refreshToken) {
        tokenBlacklist.add(accessToken);
        tokenBlacklist.add(refreshToken);
    }
}
```

#### UserService

Handles user profile management.

```java
@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;

    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundError(userId));
        return UserResponse.fromDomain(user);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundError(userId));

        if (!passwordHasher.verify(currentPassword, user.getPasswordHash().value())) {
            throw new InvalidCredentialsError("Current password is incorrect");
        }

        user.changePassword(new PasswordHash(passwordHasher.hash(newPassword)), Instant.now());
        userRepository.save(user);
        eventPublisher.publish(UserEvents.passwordChanged(user, Instant.now()));
    }

    public void deactivate(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundError(userId));

        user.deactivate(Instant.now());
        userRepository.save(user);
        eventPublisher.publish(UserEvents.deactivated(user, Instant.now()));
    }

    public void activate(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundError(userId));

        user.activate(Instant.now());
        userRepository.save(user);
        eventPublisher.publish(UserEvents.activated(user, Instant.now()));
    }
}
```

### Collaboration Context Services

#### RoomService

Handles room management and membership.

```java
@Service
@Transactional
public class RoomService {
    private final RoomRepository roomRepository;
    private final EventPublisher eventPublisher;
    private final RoomMembershipValidator membershipValidator;

    public Room createRoom(String name, Long ownerId, Instant now) {
        Room room = new Room(new RoomName(name), ownerId, now);
        Room saved = roomRepository.save(room);
        eventPublisher.publish(RoomEvents.roomCreated(saved, now));
        return saved;
    }

    public Room findById(Long roomId, Long userId) {
        return membershipValidator.validateAndGetRoom(roomId, userId);
    }

    public List<Room> findByMemberId(Long userId) {
        return roomRepository.findByMemberId(userId);
    }

    public void addMember(Long roomId, Long invitedUserId, Instant now) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RoomNotFoundError(roomId.toString()));
        room.addMember(invitedUserId, now);
        roomRepository.save(room);
        eventPublisher.publish(RoomEvents.memberJoined(roomId, invitedUserId, now));
    }

    public void leaveRoom(Long roomId, Long userId, Instant now) {
        Room room = membershipValidator.validateAndGetRoom(roomId, userId);
        if (room.isOwner(userId)) {
            throw new InvalidRoomError("Room owner cannot leave room");
        }
        room.removeMember(userId, now);
        roomRepository.save(room);
        eventPublisher.publish(RoomEvents.memberLeft(roomId, userId, now));
    }

    private void validateOwnership(Room room, Long userId) {
        if (!room.isOwner(userId)) {
            throw new ForbiddenError("Only room owner can perform this action");
        }
    }
}
```

#### MessageService

Handles message creation and management.

```java
@Service
@Transactional
public class MessageService {
    private final MessageRepository messageRepository;
    private final EventPublisher eventPublisher;
    private final EncryptionService encryptionService;
    private final RoomMembershipValidator roomValidator;

    public Message createMessage(Long senderId, Long roomId, String plaintext, Instant now) {
        // 1. Validate membership
        roomValidator.validateAndGetRoom(roomId, senderId);

        // 2. Encrypt content
        String ciphertext = encryptionService.encrypt(plaintext);

        // 3. Create message
        Message message = new Message(senderId, roomId, new MessageContent(ciphertext), now);
        Message saved = messageRepository.save(message);

        // 4. Publish event
        eventPublisher.publish(MessageEvents.created(saved));
        return saved;
    }

    public List<Message> findByRoomId(Long roomId, Long userId, int limit, int offset) {
        roomValidator.validateAndGetRoom(roomId, userId);
        return messageRepository.findByRoomId(roomId, limit, offset);
    }

    public Message updateMessage(Long senderId, Long roomId, Long messageId, String plaintext, Instant now) {
        roomValidator.validateAndGetRoom(roomId, senderId);
        Message message = messageRepository
            .findByIdAndRoomId(messageId, roomId)
            .orElseThrow(() -> new InvalidMessageError("Message not found in this room"));

        validateOwnership(message, senderId);

        String ciphertext = encryptionService.encrypt(plaintext);
        message.edit(new MessageContent(ciphertext), now);
        messageRepository.save(message);
        eventPublisher.publish(MessageEvents.edited(message));
        return message;
    }

    private void validateOwnership(Message message, Long senderId) {
        if (!message.getSenderId().equals(senderId)) {
            throw new ForbiddenError("Cannot modify message you didn't send");
        }
    }
}
```

### Presence Context Services

#### PresenceService

Handles user presence tracking.

```java
@Service
public class PresenceService implements TrackPresenceUseCase, QueryPresenceUseCase {
    private final PresenceRepository presenceRepository;
    private final EventPublisher eventPublisher;

    @Override
    public void setOnline(Long userId) {
        boolean isNewPresence = !presenceRepository.findByUserId(userId).isPresent();
        UserPresence presence = presenceRepository.findByUserId(userId)
            .orElseGet(() -> new UserPresence(userId, PresenceState.ONLINE, Instant.now()));

        PresenceState previousState = presence.getState();
        presence.setOnline();
        presenceRepository.save(presence);

        if (isNewPresence || previousState != PresenceState.ONLINE) {
            eventPublisher.publish(PresenceEvents.userCameOnline(presence, Instant.now()));
        }
    }

    @Override
    public void setOffline(Long userId) {
        boolean isNewPresence = !presenceRepository.findByUserId(userId).isPresent();
        UserPresence presence = presenceRepository.findByUserId(userId)
            .orElse(new UserPresence(userId, PresenceState.OFFLINE, Instant.now()));

        PresenceState previousState = presence.getState();
        presence.setOffline();
        presenceRepository.save(presence);

        if (isNewPresence || previousState != PresenceState.OFFLINE) {
            eventPublisher.publish(PresenceEvents.userWentOffline(presence, Instant.now()));
        }
    }

    @Override
    public PresenceStatus getPresenceStatus(Long userId) {
        return presenceRepository.findByUserId(userId)
            .map(p -> PresenceStatus.fromDomain(
                p.getUserId(),
                p.getState().name(),
                p.getLastSeen().timestamp(),
                null
            ))
            .orElse(PresenceStatus.fromDomain(
                userId,
                PresenceState.OFFLINE.name(),
                Instant.now(),
                null
            ));
    }

    @Override
    public Set<Long> getOnlineUserIds() {
        return presenceRepository.getOnlineUserIds();
    }
}
```

## DTOs (Data Transfer Objects)

### Request DTOs

```java
public record RegisterRequest(String username, String email, String password) {
    public RegisterRequest {
        // Validation via @Valid in controller
    }
}

public record LoginRequest(String email, String password) { }

public record CreateRoomRequest(String name) { }

public record SendMessageRequest(String content) { }

public record JoinRoomRequest(String inviteCode) { }
```

### Response DTOs

```java
public record AuthResponse(String accessToken, String refreshToken) { }

public record UserResponse(Long id, String username, String email, boolean active) {
    public static UserResponse fromDomain(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername().value(),
            user.getEmail().value(),
            user.isActive()
        );
    }
}

public record RoomResponse(Long id, String name, Long ownerId, Set<Long> memberIds) {
    public static RoomResponse fromDomain(Room room) {
        return new RoomResponse(
            room.getId(),
            room.getName().value(),
            room.getOwnerId(),
            room.getMemberIds()
        );
    }
}
```

## Service Characteristics

### Thin Services

Application services should be thin - orchestrate, don't implement business logic:

```java
// ✅ Good - delegates to domain
public void deactivate(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundError(userId));
    user.deactivate(Instant.now());  // Domain logic
    userRepository.save(user);
    eventPublisher.publish(UserEvents.deactivated(user, Instant.now()));
}

// ❌ Bad - business logic in service
public void deactivate(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(...);
    if (user.isActive()) {
        user.setActive(false);
        user.setUpdatedAt(Instant.now());
    }
    userRepository.save(user);
}
```

### Use Case Focused

Each service method should represent a single use case:

```java
// ✅ Good - clear use cases
public AuthResponse register(RegisterRequest request)
public AuthResponse login(LoginRequest request)
public void logout(String accessToken, String refreshToken)
public void changePassword(Long userId, String currentPassword, String newPassword)

// ❌ Bad - unclear responsibility
public AuthResponse handleAuth(AuthRequest request)
public void manageUser(Long userId, UserAction action)
```

### Transactional

Services define transaction boundaries:

```java
@Service
@Transactional
public class RoomService {
    // All methods run in a transaction
}
```

## Service Best Practices

1. **No Business Logic** - Delegate to domain objects
2. **Use Domain Ports** - Depend on interfaces, not implementations
3. **Publish Events** - Publish after successful state change
4. **Handle Errors** - Let domain errors propagate
5. **Keep Methods Small** - Each method should do one thing

## Benefits

1. **Clear Use Cases** - Services represent business operations
2. **Testability** - Easy to mock dependencies
3. **Separation of Concerns** - Orchestration separated from logic
4. **Transaction Management** - Clear transaction boundaries

## Next Steps

- [Infrastructure Adapters](./09-infrastructure-adapters.md) - See infrastructure implementation
- [Design Patterns](./10-design-patterns.md) - Explore patterns used
- [Project Structure](../development/01-project-structure.md) - Learn file organization
