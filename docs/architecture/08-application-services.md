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

#### InviteService

Handles invite generation and management.

```java
@Service
@Transactional
public class InviteService {
    private final InviteRepository inviteRepository;
    private final RoomRepository roomRepository;
    private final InviteFactory inviteFactory;
    private final EventPublisher eventPublisher;

    public Invite generateInvite(Long roomId, Long roomId, Instant now) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundError(roomId.toString()));
        Invite invite = inviteFactory.createInvite(roomId, roomId, now);
        Invite saved = inviteRepository.save(invite);
        eventPublisher.publish(InviteEvents.generated(saved, now));
        return saved;
    }

    public Invite acceptInvite(String code, Long userId, Instant now) {
        Invite invite = inviteRepository.findByCode(code)
                .orElseThrow(() -> new InvalidInviteCodeError("Invalid or expired invite code"));

        if (invite.isExpired(now)) {
            throw new InvalidInviteCodeError("Invite code has expired");
        }

        invite.accept(userId, now);
        Invite updated = inviteRepository.save(invite);
        eventPublisher.publish(InviteEvents.accepted(updated, now));

        roomService.addMember(invite.roomId(), userId, now);
    }

    public void revokeInvite(String code, Long userId, Instant now) {
        Invite invite = inviteRepository.findByCode(code)
                .orElseThrow(() -> new InvalidInviteCodeError("Invalid invite code"));

        Room room = roomRepository.findById(invite.roomId())
                .orElseThrow(() -> new RoomNotFoundError(invite.roomId().toString()));

        if (!room.isOwner(userId)) {
            throw new ForbiddenError("Only room owner can revoke invites");
        }

        invite.revoke(now);
        inviteRepository.save(invite);
        eventPublisher.publish(InviteEvents.revoked(invite, now));
    }
}
```

#### E2EEKeyManagementService

Handles End-to-End Encryption (E2EE) for rooms using X25519 key exchange.

```java
@Service
@Transactional
public class E2EEKeyManagementService {
    private final RoomEncryptionStateRepository encryptionStateRepository;
    private final SecureRandom secureRandom;

    private static final String KEY_ALGORITHM = "X25519";
    private static final int KEY_SIZE = 256;

    public RoomEncryptionState enableE2EE(Long roomId, Long ownerId, byte[] ownerPublicKey) {
        // 1. Validate key size (X25519 requires 32 bytes)
        if (ownerPublicKey == null || ownerPublicKey.length != 32) {
            throw new IllegalArgumentException("Owner public key must be 32 bytes");
        }

        // 2. Generate ephemeral key pair for this room
        KeyPair ephemeralKeyPair = generateKeyPair();
        byte[] rawPublicKey = extractRawX25519KeyBytes(
                ephemeralKeyPair.getPublic().getEncoded());

        // 3. Generate room symmetric key (AES-256)
        byte[] roomKey = generateRoomKey();

        // 4. Encrypt room key for owner using ECDH (simplified)
        byte[] encryptedRoomKey = encryptRoomKeyForUser(roomKey, ownerPublicKey);

        // 5. Create encryption state
        RoomEncryptionState state = RoomEncryptionState.createE2EE(
                roomId,
                rawPublicKey,
                encryptedRoomKey
        );

        return encryptionStateRepository.save(state);
    }

    public RoomEncryptionState rotateRoomKey(Long roomId) {
        RoomEncryptionState state = encryptionStateRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("E2EE not enabled for room " + roomId));

        // Generate new room symmetric key
        byte[] newRoomKey = generateRoomKey();

        // Generate new ephemeral key pair
        KeyPair newEphemeralKeyPair = generateKeyPair();
        byte[] rawPublicKey = extractRawX25519KeyBytes(
                newEphemeralKeyPair.getPublic().getEncoded());

        // Encrypt new room key with room's public key
        byte[] encryptedRoomKey = encryptRoomKeyForUser(newRoomKey, state.roomPublicKey());

        // Rotate key
        RoomEncryptionState newState = state.rotateKey(
                rawPublicKey,
                encryptedRoomKey
        );

        return encryptionStateRepository.save(newState);
    }

    public byte[] encryptRoomKeyForMember(Long roomId, byte[] memberPublicKey) {
        RoomEncryptionState state = encryptionStateRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("E2EE not enabled for room " + roomId));

        // Generate ephemeral key pair for this distribution
        KeyPair ephemeralKeyPair = generateKeyPair();

        // Extract room key (in production, decrypt from state)
        byte[] roomKey = generateRoomKey();

        // Encrypt room key for member using ECDH (simplified)
        byte[] encryptedRoomKey = encryptRoomKeyForUser(roomKey, memberPublicKey);

        return encryptedRoomKey;
    }

    private byte[] extractRawX25519KeyBytes(byte[] encodedKey) {
        if (encodedKey.length == 32) {
            return encodedKey;
        }
        // Extract raw 32-byte key from ASN.1 encoded format
        int start = encodedKey.length - 32;
        byte[] rawKey = new byte[32];
        System.arraycopy(encodedKey, start, rawKey, 0, 32);
        return rawKey;
    }
}
```

**Key Points:**
- Uses X25519 curve for ECDH key exchange
- Encrypts room symmetric keys for each member
- Supports key rotation for forward secrecy
- Extracts raw 32-byte X25519 keys from ASN.1 encoded format

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
