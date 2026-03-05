# Domain Aggregates

## Introduction

In Domain-Driven Design, an **Aggregate** is a cluster of domain objects that can be treated as a single unit. The aggregate defines a consistency boundary and transaction scope - changes within an aggregate must be transactionally consistent.

## Aggregate Design Principles

### 1. Single Root Entity

Each aggregate has exactly one root entity that is the only entry point for external references.

### 2. Consistency Boundary

All invariants (business rules) within an aggregate are enforced as a single transaction.

### 3. External References by ID Only

Aggregates reference other aggregates only by their root ID, never by object reference.

### 4. Small and Focused

Aggregates should be kept small to maximize performance and minimize lock contention.

### 5. Protect Invariants

Business rules are enforced within the aggregate root and cannot be violated.

## Aggregates in Discord-like

```
┌─────────────────────────────────────────────────────────────────┐
│                      Aggregates by Context                        │
│                                                                  │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │
│  │   Identity     │  │ Collaboration  │  │   Presence     │    │
│  │    Context     │  │    Context     │  │    Context     │    │
│  │                │  │                │  │                │    │
│  │  ┌──────────┐  │  │  ┌──────────┐  │  │  ┌──────────┐  │    │
│  │  │  User    │  │  │  │  Room    │  │  │  │ Presence │  │    │
│  │  │ (Root)   │  │  │  │  (Root)  │  │  │  │  (Root)  │  │    │
│  │  └──────────┘  │  │  └──────────┘  │  │  └──────────┘  │    │
│  │                │  │  ┌──────────┐  │  │                │    │
│  │                │  │  │Conv.     │  │  │                │    │
│  │                │  │  │(Root)    │  │  │                │    │
│  │                │  │  └──────────┘  │  │                │    │
│  │                │  │  ┌──────────┐  │  │                │    │
│  │                │  │  │Message   │  │  │                │    │
│  │                │  │  │(Root)    │  │  │                │    │
│  │                │  │  └──────────┘  │  │                │    │
│  │                │  │  ┌──────────┐  │  │                │    │
│  │                │  │  │Invite    │  │  │                │    │
│  │                │  │  │(Root)    │  │  │                │    │
│  │                │  │  └──────────┘  │  │                │    │
│  └────────────────┘  └────────────────┘  └────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## 1. User Aggregate (Identity Context)

### Purpose

Manages user credentials, profile information, and account lifecycle.

### Root Entity

```java
public class User extends BaseEntity {
    private Username username;
    private Email email;
    private PasswordHash passwordHash;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
```

### Invariants

1. **Username Uniqueness** - Enforced at repository level
2. **Email Uniqueness** - Enforced at repository level
3. **Active User Can Change Password** - Cannot change password if inactive
4. **Inactive User Cannot Be Deactivated** - Cannot deactivate already inactive user
5. **Active User Cannot Be Activated** - Cannot activate already active user

### Behavior

```java
// Constructor for new user
public User(Username username, Email email, PasswordHash passwordHash, Instant createdAt) {
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.active = true;
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
}

// Factory for reconstruction from persistence
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

// Domain behavior
public void changePassword(PasswordHash newPasswordHash, Instant updatedAt) {
    if (!active) {
        throw new InvalidUserError("Cannot change password of inactive user");
    }
    this.passwordHash = newPasswordHash;
    this.updatedAt = updatedAt;
}

public void deactivate(Instant updatedAt) {
    if (!active) {
        throw new InvalidUserError("User is already inactive");
    }
    this.active = false;
    this.updatedAt = updatedAt;
}

public void activate(Instant updatedAt) {
    if (active) {
        throw new InvalidUserError("User is already active");
    }
    this.active = true;
    this.updatedAt = updatedAt;
}
```

### Access Control

- Only the aggregate root can modify internal state
- No direct field access from outside the aggregate
- All mutations go through methods that enforce invariants

### Transaction Boundary

All changes to a User entity are atomic within a single transaction.

## 2. Room Aggregate (Collaboration Context)

### Purpose

Manages room metadata, membership, and access control.

### Root Entity

```java
public class Room extends BaseEntity {
    private RoomName name;
    private Long ownerId;
    private final Set<Long> memberIds = new HashSet<>();
    private Instant createdAt;
    private Instant updatedAt;
}
```

### Invariants

1. **Owner is Always a Member** - Owner ID is added to memberIds on creation
2. **Cannot Remove Last Member** - Room must have at least one member
3. **Cannot Remove Owner** - Owner cannot leave room
4. **Member Validation** - User must be member to access room
5. **Owner Validation** - Only owner can perform certain actions

### Behavior

```java
// Constructor for new room
public Room(RoomName name, Long ownerId, Instant createdAt) {
    this.name = name;
    this.ownerId = ownerId;
    this.memberIds.add(ownerId);  // Invariant: owner is always a member
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
}

// Factory for reconstruction
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

// Domain behavior
public void setName(RoomName name, Instant updatedAt) {
    this.name = name;
    this.updatedAt = updatedAt;
}

public void addMember(Long userId, Instant updatedAt) {
    if (userId == null) {
        throw new InvalidRoomError("User ID cannot be null");
    }
    this.memberIds.add(userId);
    this.updatedAt = updatedAt;
}

public void removeMember(Long userId, Instant updatedAt) {
    // Invariant: cannot remove last member
    if (this.memberIds.size() <= 1) {
        throw new InvalidRoomError("Cannot remove last member");
    }
    // Invariant: cannot remove owner
    if (this.ownerId.equals(userId)) {
        throw new InvalidRoomError("Cannot remove room owner");
    }
    this.memberIds.remove(userId);
    this.updatedAt = updatedAt;
}

// Queries
public boolean isMember(Long userId) {
    return this.memberIds.contains(userId);
}

public boolean isOwner(Long userId) {
    return this.ownerId.equals(userId);
}

public Set<Long> getMemberIds() {
    return Set.copyOf(memberIds);  // Defensive copy
}
```

### External References

- References users by ID only (Long userId)
- Does not load User entities to maintain consistency boundary

### Transaction Boundary

All membership changes are atomic within a single transaction.

## 3. Message Aggregate (Collaboration Context)

### Purpose

Manages individual messages within rooms.

### Root Entity

```java
public class Message extends BaseEntity {
    private Long senderId;
    private Long roomId;
    private MessageContent content;
    private Instant createdAt;
    private Instant editedAt;
}
```

### Invariants

1. **Content Cannot Be Empty** - Message must have content
2. **Content Must Be Encrypted** - Stored content must be valid ciphertext
3. **Sender Cannot Change** - Message sender ID is immutable
4. **Room Cannot Change** - Message room ID is immutable

### Behavior

```java
// Constructor for new message
public Message(Long senderId, Long roomId, MessageContent content, Instant createdAt) {
    this.senderId = senderId;
    this.roomId = roomId;
    this.content = content;
    this.createdAt = createdAt;
    this.editedAt = null;
}

// Factory for reconstruction
public static Message reconstitute(Long id, Long senderId, Long roomId,
                                   MessageContent content, Instant createdAt, Instant editedAt) {
    Message message = new Message();
    message.id = id;
    message.senderId = senderId;
    message.roomId = roomId;
    message.content = content;
    message.createdAt = createdAt;
    message.editedAt = editedAt;
    return message;
}

// Domain behavior
public void edit(MessageContent newContent, Instant editedAt) {
    this.content = newContent;
    this.editedAt = editedAt;
}

// Queries
public boolean isEdited() {
    return editedAt != null;
}
```

### External References

- References sender by ID (Long senderId)
- References room by ID (Long roomId)
- Does not load User or Room entities

### Transaction Boundary

Message creation and editing are atomic within a single transaction.

## 4. Invite Aggregate (Collaboration Context)

### Purpose

Manages room invitations with time-limited codes.

### Root Entity

```java
public class Invite extends BaseEntity {
    private Long roomId;
    private InviteCode code;
    private Long createdByUserId;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean used;
}
```

### Invariants

1. **Code Uniqueness** - Each invite has a unique code
2. **Time-Bound** - Invites expire after 24 hours
3. **Single Use** - Each invite can only be used once
4. **Room Validity** - Room must exist for invite to be valid

### Behavior

```java
// Constructor for new invite
public Invite(Long roomId, Long createdByUserId, InviteCode code, Instant createdAt) {
    this.roomId = roomId;
    this.createdByUserId = createdByUserId;
    this.code = code;
    this.createdAt = createdAt;
    this.expiresAt = createdAt.plus(24, ChronoUnit.HOURS);
    this.used = false;
}

// Factory for reconstruction
public static Invite reconstitute(Long id, Long roomId, Long createdByUserId,
                                  InviteCode code, Instant createdAt,
                                  Instant expiresAt, boolean used) {
    Invite invite = new Invite();
    invite.id = id;
    invite.roomId = roomId;
    invite.createdByUserId = createdByUserId;
    invite.code = code;
    invite.createdAt = createdAt;
    invite.expiresAt = expiresAt;
    invite.used = used;
    return invite;
}

// Domain behavior
public void markAsUsed() {
    if (used) {
        throw new InvalidInviteCodeError("Invite has already been used");
    }
    this.used = true;
}

public boolean isValid() {
    return !used && Instant.now().isBefore(expiresAt);
}

public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
}

// Queries
public boolean isUsed() {
    return used;
}
```

### External References

- References room by ID (Long roomId)
- References creator by ID (Long createdByUserId)

### Transaction Boundary

Invite creation and usage are atomic within a single transaction.

## 5. Conversation Aggregate (Collaboration Context)

### Purpose

Manages the collection of messages within a room as a single unit.

### Root Entity

```java
public class Conversation extends BaseEntity {
    private Long roomId;
    private final SortedSet<Message> messages = new TreeSet<>(
        (m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt())
    );
    private Instant lastActivityAt;
    private static final int MAX_MESSAGES = 10000;
}
```

### Invariants

1. **Message Limit** - Cannot exceed 10,000 messages
2. **Chronological Order** - Messages are maintained in creation order
3. **Activity Tracking** - Last activity timestamp is updated on changes
4. **Message Ownership** - Only sender can edit/delete their messages

### Behavior

```java
// Constructor for new conversation
public Conversation(Long roomId, Instant createdAt) {
    this.roomId = roomId;
    this.lastActivityAt = createdAt;
}

// Factory for reconstruction
public static Conversation reconstitute(Long id, Long roomId, SortedSet<Message> messages,
                                       Instant lastActivityAt) {
    Conversation conversation = new Conversation();
    conversation.id = id;
    conversation.roomId = roomId;
    conversation.messages.addAll(messages);
    conversation.lastActivityAt = lastActivityAt;
    return conversation;
}

// Domain behavior
public void addMessage(Message message, Instant now) {
    // Invariant: message limit
    if (messages.size() >= MAX_MESSAGES) {
        throw InvalidMessageError.messageLimitExceeded(
            "Conversation has reached maximum message limit"
        );
    }
    messages.add(message);
    this.lastActivityAt = now;
}

public void updateMessage(Long messageId, MessageContent newContent, Instant now) {
    Message message = findMessage(messageId)
        .orElseThrow(() -> new RoomNotFoundError("Message not found"));
    message.edit(newContent, now);
    this.lastActivityAt = now;
}

public void deleteMessage(Long messageId, Instant now) {
    Message message = findMessage(messageId)
        .orElseThrow(() -> new RoomNotFoundError("Message not found"));
    messages.remove(message);
    this.lastActivityAt = now;
}

// Authorization queries
public boolean canEditMessage(Long messageId, Long userId) {
    return findMessage(messageId)
        .map(m -> m.getSenderId().equals(userId))
        .orElse(false);
}

public boolean canDeleteMessage(Long messageId, Long userId) {
    return findMessage(messageId)
        .map(m -> m.getSenderId().equals(userId))
        .orElse(false);
}

// Pagination queries
public List<Message> getMessages(int limit, int offset) {
    return messages.stream()
        .skip(offset)
        .limit(limit)
        .toList();
}

public List<Message> getMessagesBefore(Long messageId, int limit) {
    return messages.stream()
        .takeWhile(m -> m.getId().compareTo(messageId) < 0)
        .toList()
        .reversed()
        .stream()
        .limit(limit)
        .toList();
}

private Optional<Message> findMessage(Long messageId) {
    return messages.stream()
        .filter(m -> m.getId().equals(messageId))
        .findFirst();
}

// Getters with defensive copies
public SortedSet<Message> getMessages() {
    return Collections.unmodifiableSortedSet(messages);
}

public int getMessageCount() {
    return messages.size();
}
```

### External References

- References room by ID (Long roomId)
- Contains Message entities (not references)

### Transaction Boundary

All message operations within a conversation are atomic.

## 6. UserPresence Aggregate (Presence Context)

### Purpose

Manages user presence state and activity tracking.

### Root Entity

```java
public class UserPresence extends BaseEntity {
    private Long userId;
    private PresenceState state;
    private LastSeen lastSeen;
}
```

### Invariants

1. **One Presence Per User** - Each user has exactly one presence record
2. **State Validity** - Presence state must be valid
3. **Last Seen Accuracy** - Last seen timestamp reflects most recent activity

### Behavior

```java
// Constructor for new presence
public UserPresence(Long userId, PresenceState state, Instant createdAt) {
    this.userId = userId;
    this.state = state;
    this.lastSeen = new LastSeen(createdAt);
}

// Factory for reconstruction
public static UserPresence reconstitute(Long id, Long userId, PresenceState state,
                                       LastSeen lastSeen) {
    UserPresence presence = new UserPresence();
    presence.id = id;
    presence.userId = userId;
    presence.state = state;
    presence.lastSeen = lastSeen;
    return presence;
}

// Domain behavior
public void setOnline() {
    this.state = PresenceState.ONLINE;
    this.lastSeen = LastSeen.now();
}

public void setOffline() {
    this.state = PresenceState.OFFLINE;
    this.lastSeen = LastSeen.now();
}

public void setState(PresenceState newState) {
    this.state = newState;
    this.lastSeen = LastSeen.now();
}

public void updateLastActivity() {
    this.lastSeen = LastSeen.now();
}

// Queries
public PresenceState getState() {
    return state;
}

public LastSeen getLastSeen() {
    return lastSeen;
}
```

### External References

- References user by ID (Long userId)
- Does not load User entity

### Transaction Boundary

All presence state changes are atomic within a single transaction.

## Aggregate Relationships

### Reference by ID

Aggregates reference each other only by root entity ID:

```
┌──────────────┐         ┌──────────────┐
│    User      │         │     Room     │
│    (Root)    │◄────────│    (Root)    │
│  ID: Long    │  userId  │  ID: Long    │
│              │         │  ownerId:    │
│              │         │  Long        │
└──────────────┘         └──────────────┘

┌──────────────┐         ┌──────────────┐
│    Room      │         │    Message   │
│    (Root)    │◄────────│    (Root)    │
│  ID: Long    │  roomId  │  ID: Long    │
│              │         │  senderId:   │
│              │         │  Long        │
└──────────────┘         └──────────────┘
```

### No Object References

Aggregates do NOT hold object references to other aggregates:

```java
// ❌ WRONG - Violates aggregate boundary
public class Room {
    private Set<User> members;  // Don't do this!
}

// ✅ CORRECT - Reference by ID only
public class Room {
    private Set<Long> memberIds;  // Correct!
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
        Room saved = roomRepository.save(room);  // Single transaction
        eventPublisher.publish(RoomEvents.roomCreated(saved, now));
        return saved;
    }
}
```

### Aggregate Consistency

- All invariants are enforced within the aggregate
- Repository saves entire aggregate atomically
- No partial updates or inconsistent states

## Aggregate Design Rules

1. **Root is Only Entry Point** - External entities only reference the root
2. **Protect Invariants** - All mutations go through methods that enforce rules
3. **Keep Small** - Small aggregates perform better and scale better
4. **Reference by ID** - Never hold object references to other aggregates
5. **Use Factories** - Static factory methods for reconstruction
6. **Encapsulate Collections** - Return defensive copies or unmodifiable views

## Benefits

1. **Consistency** - Invariants are guaranteed within the aggregate
2. **Performance** - Small aggregates minimize lock contention
3. **Scalability** - Clear boundaries enable distributed systems
4. **Testability** - Aggregates can be tested in isolation
5. **Maintainability** - Clear boundaries make the code easier to understand

## Next Steps

- [Value Objects](./05-value-objects.md) - Learn about immutable value objects within aggregates
- [Domain Events](./06-domain-events.md) - Understand how aggregates publish events
- [Repositories](./07-repositories.md) - See how aggregates are persisted
