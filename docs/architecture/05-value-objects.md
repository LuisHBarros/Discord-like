# Value Objects

## Introduction

In Domain-Driven Design, a **Value Object** is an immutable object that represents a descriptive aspect of the domain with no conceptual identity. Value objects are defined by their attributes rather than an identity, and two value objects with the same attributes are considered equal.

## Value Object Characteristics

1. **Immutable** - Cannot be modified after creation
2. **No Identity** - Identified by their attributes, not an ID
3. **Value-Based Equality** - Equal if all attributes are equal
4. **Self-Validating** - Validate their state on construction
5. **Side-Effect Free** - Methods do not modify state

## Value Objects in Discord-like

```
┌─────────────────────────────────────────────────────────────────┐
│                    Value Objects by Context                      │
│                                                                  │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │
│  │   Identity     │  │ Collaboration  │  │   Presence     │    │
│  │    Context     │  │    Context     │  │    Context     │    │
│  │                │  │                │  │                │    │
│  │  • Username    │  │  • RoomName    │  │  • Presence   │    │
│  │  • Email       │  │  • Message    │  │    State       │    │
│  │  • Password    │  │    Content    │  │  • LastSeen    │    │
│  │    Hash        │  │  • Membership  │  │                │    │
│  │                │  │  • InviteCode  │  │                │    │
│  └────────────────┘  └────────────────┘  └────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## Identity Context Value Objects

### 1. Username

Represents a user's unique identifier with validation rules.

```java
public record Username(String value) {
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_-]+$";

    public Username {
        if (value == null || value.isBlank()) {
            throw InvalidUserError.invalidUsername("Username cannot be blank");
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw InvalidUserError.invalidUsername(
                "Username must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters"
            );
        }
        if (!value.matches(USERNAME_PATTERN)) {
            throw InvalidUserError.invalidUsername(
                "Username can only contain letters, numbers, underscores, and hyphens"
            );
        }
    }
}
```

**Rules:**
- 3-50 characters
- Alphanumeric, underscore, hyphen only
- Not blank
- No leading/trailing whitespace

**Equality:**
```java
Username u1 = new Username("john_doe");
Username u2 = new Username("john_doe");
assert u1.equals(u2);  // true - value equality
assert u1.value().equals(u2.value());  // true
```

### 2. Email

Represents a validated email address.

```java
public record Email(String value) {
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public Email {
        if (value == null || value.isBlank()) {
            throw InvalidUserError.invalidEmail("Email cannot be blank");
        }
        if (!value.matches(EMAIL_PATTERN)) {
            throw InvalidUserError.invalidEmail("Invalid email format");
        }
    }
}
```

**Rules:**
- Valid email format
- Not blank
- Standard email validation pattern

**Usage:**
```java
Email email = new Email("user@example.com");
String emailValue = email.value();  // "user@example.com"
```

### 3. PasswordHash

Represents an Argon2id password hash.

```java
public record PasswordHash(String value) {
    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw InvalidUserError.invalidPassword("Password hash cannot be blank");
        }
    }
}
```

**Rules:**
- Not blank
- Contains Argon2id hash string

**Note:** The PasswordHash value object does not contain the plaintext password. The actual password is hashed by the `PasswordHasher` port before creating this value object.

## Collaboration Context Value Objects

### 1. RoomName

Represents a room's name with length constraints.

```java
public record RoomName(String value) {
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 100;

    public RoomName {
        if (value == null || value.isBlank()) {
            throw InvalidRoomError.invalidName("Room name cannot be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw InvalidRoomError.invalidName(
                "Room name cannot exceed " + MAX_LENGTH + " characters"
            );
        }
    }
}
```

**Rules:**
- 1-100 characters
- Not blank

**Usage:**
```java
RoomName name = new RoomName("General Chat");
Room room = new Room(name, ownerId, Instant.now());
```

### 2. MessageContent

Represents encrypted message content.

```java
public record MessageContent(String ciphertext) {
    private static final int MIN_CIPHERTEXT_LENGTH = 12; // IV + at least some data

    public MessageContent {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw InvalidMessageError.emptyContent();
        }
        if (ciphertext.length() < MIN_CIPHERTEXT_LENGTH) {
            throw InvalidMessageError.invalidFormat("Ciphertext too short");
        }
    }
}
```

**Rules:**
- Minimum 12 characters (IV + data)
- Not blank
- Contains AES-GCM encrypted ciphertext

**Important:** This value object always contains encrypted ciphertext, never plaintext. Plaintext is encrypted by the `EncryptionService` before creating this value object.

**Usage:**
```java
String ciphertext = encryptionService.encrypt("Hello, world!");
MessageContent content = new MessageContent(ciphertext);
Message message = new Message(senderId, roomId, content, Instant.now());
```

### 3. InviteCode

Represents a room invitation code.

```java
public record InviteCode(
    String value,
    Long createdByUserId
) {
    public InviteCode {
        if (value == null || value.isBlank()) {
            throw InvalidInviteCodeError.emptyCodeValue();
        }
    }
}
```

**Rules:**
- Not blank
- 8-character uppercase string (generated from UUID)
- Associated with creator user ID

**Generation:**
```java
// InviteFactory generates codes
private String generateCode() {
    return UUID.randomUUID()
        .toString()
        .substring(0, 8)
        .toUpperCase();
}

// Example: "A1B2C3D4"
```

**Usage:**
```java
InviteCode code = new InviteCode("A1B2C3D4", userId);
Invite invite = new Invite(roomId, createdByUserId, code, Instant.now());
```

### 4. Membership

Represents a user's membership in a room.

```java
public record Membership(Long userId, String role, Instant joinedAt) {
    public enum Role {
        OWNER,
        MEMBER
    }

    public Membership {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (role == null) {
            role = Role.MEMBER.name();
        }
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }

    public static Membership owner(Long userId) {
        return new Membership(userId, Role.OWNER.name(), Instant.now());
    }

    public static Membership member(Long userId) {
        return new Membership(userId, Role.MEMBER.name(), Instant.now());
    }

    public boolean isOwner() {
        return Role.OWNER.name().equals(role);
    }
}
```

**Rules:**
- User ID is required
- Role defaults to MEMBER if not specified
- JoinedAt defaults to current time if not specified

**Factory Methods:**
```java
Membership owner = Membership.owner(userId);     // Creates owner membership
Membership member = Membership.member(userId);   // Creates regular membership
```

**Usage:**
```java
Membership membership = new Membership(userId, "MEMBER", Instant.now());
if (membership.isOwner()) {
    // Owner-specific logic
}
```

## Presence Context Value Objects

### 1. PresenceState

Enum representing user presence status.

```java
public enum PresenceState {
    ONLINE,
    OFFLINE,
    AWAY,
    BUSY,
    INVISIBLE;

    public boolean isOnline() {
        return this == ONLINE;
    }

    public static PresenceState fromString(String state) {
        if (state == null || state.isBlank()) {
            return OFFLINE;
        }
        try {
            return PresenceState.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw InvalidPresenceError.invalidState("Invalid presence state: " + state);
        }
    }
}
```

**States:**
- `ONLINE` - User is actively using the platform
- `OFFLINE` - User is not logged in
- `AWAY` - User is logged in but away from keyboard
- `BUSY` - User is busy and should not be disturbed
- `INVISIBLE` - User appears offline to others

**Usage:**
```java
PresenceState state = PresenceState.ONLINE;
if (state.isOnline()) {
    // User is online
}

PresenceState fromString = PresenceState.fromString("online");  // ONLINE
```

### 2. LastSeen

Represents the timestamp of a user's last activity.

```java
public record LastSeen(Instant timestamp) {
    public LastSeen {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public static LastSeen now() {
        return new LastSeen(Instant.now());
    }

    public boolean isOlderThan(Duration duration) {
        return timestamp.isBefore(Instant.now().minus(duration));
    }
}
```

**Features:**
- Default to current time if not specified
- Factory method for current time
- Method to check if older than a duration

**Usage:**
```java
LastSeen lastSeen = LastSeen.now();
if (lastSeen.isOlderThan(Duration.ofMinutes(5))) {
    // User has been inactive for 5+ minutes
}
```

**In Presence Aggregate:**
```java
public class UserPresence extends BaseEntity {
    private PresenceState state;
    private LastSeen lastSeen;

    public void setOnline() {
        this.state = PresenceState.ONLINE;
        this.lastSeen = LastSeen.now();  // Update last seen
    }
}
```

## Value Object Implementation Patterns

### 1. Java Records

Modern Java records are perfect for value objects:

```java
public record Username(String value) {
    // Compact constructor for validation
    public Username {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
    }
}
```

**Benefits:**
- Immutable by default
- Automatic `equals()`, `hashCode()`, `toString()`
- Compact syntax
- Value-based equality

### 2. Self-Validation

Value objects validate themselves on construction:

```java
public record Email(String value) {
    public Email {
        if (value == null || !isValidEmail(value)) {
            throw new IllegalArgumentException("Invalid email");
        }
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
```

**Benefits:**
- Always in a valid state
- Cannot create invalid instances
- Fail fast

### 3. Factory Methods

Static factory methods for common cases:

```java
public record Membership(Long userId, String role, Instant joinedAt) {
    public static Membership owner(Long userId) {
        return new Membership(userId, Role.OWNER.name(), Instant.now());
    }

    public static Membership member(Long userId) {
        return new Membership(userId, Role.MEMBER.name(), Instant.now());
    }
}
```

**Benefits:**
- Intent-revealing method names
- Simplifies object creation
- Encapsulates creation logic

### 4. Behavior in Value Objects

Value objects can have behavior beyond validation:

```java
public record LastSeen(Instant timestamp) {
    public boolean isOlderThan(Duration duration) {
        return timestamp.isBefore(Instant.now().minus(duration));
    }

    public Duration timeSinceNow() {
        return Duration.between(timestamp, Instant.now());
    }
}
```

**Benefits:**
- Encapsulates behavior with data
- Reduces primitive obsession
- Improves readability

## Value Object vs Entity

| Aspect | Value Object | Entity |
|--------|--------------|--------|
| Identity | No identity (value-based) | Has unique identity |
| Mutability | Immutable | Mutable |
| Equality | All attributes equal | ID equality |
| Lifecycle | Created/destroyed with parent | Independent lifecycle |
| Persistence | Embedded in entity | Persisted separately |

**Example:**

```java
// Value Object - identified by value
Username u1 = new Username("john");
Username u2 = new Username("john");
assert u1.equals(u2);  // true

// Entity - identified by ID
User user1 = new User(/*...*/);
user1.setId(1L);
User user2 = new User(/*...*/);
user2.setId(2L);
assert !user1.equals(user2);  // false - different IDs
```

## Benefits of Value Objects

### 1. Type Safety

Prevents primitive obsession and stringly-typed code:

```java
// ❌ Primitive obsession
public void sendMessage(Long senderId, Long roomId, String content, String roomName) { }

// ✅ Value objects
public void sendMessage(Long senderId, Long roomId, MessageContent content, RoomName roomName) { }
```

### 2. Self-Validating

Guarantees valid state:

```java
Email email = new Email("invalid-email");  // Throws exception
// Can never have an invalid Email instance
```

### 3. Immutability

No side effects, thread-safe:

```java
Username username = new Username("john");
// Cannot modify username.value - it's final
```

### 4. Improved Readability

Intent is clear:

```java
// ❌ What does this mean?
public class User {
    private String s1;  // username?
    private String s2;  // email?
}

// ✅ Clear and obvious
public class User {
    private Username username;
    private Email email;
}
```

### 5. Reduced Duplication

Validation logic is in one place:

```java
public record Email(String value) {
    public Email {
        if (!isValidEmail(value)) throw new IllegalArgumentException();
    }
}
// Validation used everywhere Email is created
```

## Value Object Best Practices

### 1. Make Immutable

Use records or final fields:

```java
public record Username(String value) { }  // Immutable
```

### 2. Self-Validate

Validate in compact constructor:

```java
public record Username(String value) {
    public Username {
        if (value == null || value.isBlank()) throw new IllegalArgumentException();
    }
}
```

### 3. Override Equality (if not using records)

For custom classes:

```java
public class Username {
    private final String value;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Username other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
```

### 4. Don't Add Identity

Value objects should not have IDs:

```java
// ❌ Don't do this
public record Username(Long id, String value) { }

// ✅ Do this
public record Username(String value) { }
```

### 5. Keep Small

Value objects should be focused and small:

```java
// ✅ Good - single responsibility
public record Username(String value) { }
public record Email(String value) { }

// ❌ Bad - multiple responsibilities
public record UserCredentials(String username, String email, String passwordHash) { }
```

## When to Use Value Objects

Use value objects when:

1. The concept has no identity (identified by attributes)
2. The concept should be immutable
3. The concept needs validation
4. The concept has behavior
5. You want to avoid primitive obsession

**Examples from Discord-like:**
- Username, Email, PasswordHash - User attributes
- RoomName - Room attribute
- MessageContent - Message attribute
- InviteCode - Invite attribute
- PresenceState - Enum value object
- LastSeen - Timestamp with behavior

## Summary

Value objects in Discord-like:

- **Immutable** - Cannot be modified after creation
- **Self-Validating** - Validate on construction
- **Value-Based Equality** - Equal if attributes are equal
- **No Identity** - Identified by attributes, not ID
- **Encapsulate Behavior** - Can have methods beyond getters

Using value objects improves type safety, reduces bugs, and makes the code more readable and maintainable.

## Next Steps

- [Domain Events](./06-domain-events.md) - Learn how domain events communicate changes
- [Aggregates](./04-aggregates.md) - See how value objects are used in aggregates
- [Application Services](./08-application-services.md) - Understand use case orchestration
