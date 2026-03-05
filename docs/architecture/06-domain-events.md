# Domain Events

## Introduction

Domain Events represent something that happened in the domain that domain experts care about. They are facts about the past that other parts of the system may need to know about. Domain events enable loose coupling between bounded contexts and support event-driven architecture.

## Event Design Principles

1. **Represent Facts** - Events describe what happened, not what should happen
2. **Immutable** - Events cannot be modified after creation
3. **Past Tense** - Event names use past tense (UserRegistered, MessageCreated)
4. **Rich Information** - Events contain all relevant data needed by consumers
5. **No Side Effects** - Publishing an event should not have side effects

## Event Types in Discord-like

```
┌─────────────────────────────────────────────────────────────────┐
│                    Domain Events by Context                      │
│                                                                  │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │
│  │   Identity     │  │ Collaboration  │  │   Presence     │    │
│  │    Context     │  │    Context     │  │    Context     │    │
│  │                │  │                │  │                │    │
│  │ • Registered   │  │ • RoomCreated  │  │ • UserCame     │    │
│  │ • PasswordChgd │  │ • MemberJoined │  │   Online       │    │
│  │ • Deactivated  │  │ • MemberLeft   │  │ • UserWent     │    │
│  │ • Activated    │  │ • RoomUpdated  │  │   Offline      │    │
│  │                │  │ • RoomDeleted  │  │ • UserStateChanged │ │
│  │                │  │ • MsgCreated   │  │                │    │
│  │                │  │ • MsgEdited    │  │                │    │
│  │                │  │ • MsgDeleted   │  │                │    │
│  │                │  │ • InviteCreated│  │                │    │
│  │                │  │ • InviteAccepted│  │                │    │
│  └────────────────┘  └────────────────┘  └────────────────┘    │
│                                                                  │
│                         ┌────────────┐                          │
│                         │   Kafka    │                          │
│                         │ Event Bus  │                          │
│                         └────────────┘                          │
└─────────────────────────────────────────────────────────────────┘
```

## Identity Context Events

### UserEvents

Represents user lifecycle events.

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

    // Factory methods for each event type
    public static UserEvents registered(User user, Instant occurredAt) {
        return new UserEvents(
            user.getId(),
            user.getUsername().value(),
            user.getEmail().value(),
            occurredAt,
            EventType.REGISTERED
        );
    }

    public static UserEvents passwordChanged(User user, Instant occurredAt) {
        return new UserEvents(
            user.getId(),
            user.getUsername().value(),
            user.getEmail().value(),
            occurredAt,
            EventType.PASSWORD_CHANGED
        );
    }

    public static UserEvents deactivated(User user, Instant occurredAt) {
        return new UserEvents(
            user.getId(),
            user.getUsername().value(),
            user.getEmail().value(),
            occurredAt,
            EventType.DEACTIVATED
        );
    }

    public static UserEvents activated(User user, Instant occurredAt) {
        return new UserEvents(
            user.getId(),
            user.getUsername().value(),
            user.getEmail().value(),
            occurredAt,
            EventType.ACTIVATED
        );
    }
}
```

### When Published

| Event | Trigger | Consumer Purpose |
|-------|---------|------------------|
| REGISTERED | User completes registration | Welcome emails, profile creation |
| PASSWORD_CHANGED | User changes password | Security notifications, logout other sessions |
| DEACTIVATED | User deactivates account | Cleanup processes, notifications |
| ACTIVATED | User reactivates account | Restoration processes, notifications |

### Example Usage

```java
@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    public AuthResponse register(RegisterRequest request) {
        User user = new User(
            new Username(request.username()),
            new Email(request.email()),
            new PasswordHash(passwordHasher.hash(request.password())),
            Instant.now()
        );
        User saved = userRepository.save(user);

        // Publish domain event
        eventPublisher.publish(UserEvents.registered(saved, Instant.now()));

        return new AuthResponse(accessToken, refreshToken);
    }
}
```

## Collaboration Context Events

### RoomEvents

Represents room lifecycle and membership changes.

```java
public record RoomEvents(
    Long roomId,
    Long userId,
    Instant occurredAt,
    EventType type
) {
    public enum EventType {
        ROOM_CREATED,
        MEMBER_JOINED,
        MEMBER_LEFT,
        ROOM_UPDATED,
        ROOM_DELETED
    }

    public static RoomEvents roomCreated(Room room, Instant occurredAt) {
        return new RoomEvents(
            room.getId(),
            null,
            occurredAt,
            EventType.ROOM_CREATED
        );
    }

    public static RoomEvents memberJoined(Long roomId, Long userId, Instant occurredAt) {
        return new RoomEvents(
            roomId,
            userId,
            occurredAt,
            EventType.MEMBER_JOINED
        );
    }

    public static RoomEvents memberLeft(Long roomId, Long userId, Instant occurredAt) {
        return new RoomEvents(
            roomId,
            userId,
            occurredAt,
            EventType.MEMBER_LEFT
        );
    }

    public static RoomEvents roomUpdated(Long roomId, Long userId, Instant occurredAt) {
        return new RoomEvents(
            roomId,
            userId,
            occurredAt,
            EventType.ROOM_UPDATED
        );
    }

    public static RoomEvents roomDeleted(Long roomId, Long userId, Instant occurredAt) {
        return new RoomEvents(
            roomId,
            userId,
            occurredAt,
            EventType.ROOM_DELETED
        );
    }
}
```

### MessageEvents

Represents message lifecycle events.

```java
public record MessageEvents(
    Long messageId,
    Long roomId,
    Long senderId,
    String ciphertext,
    Instant editedAt,
    EventType type
) {
    public enum EventType {
        CREATED,
        EDITED,
        DELETED
    }

    public static MessageEvents created(Message message) {
        return new MessageEvents(
            message.getId(),
            message.getRoomId(),
            message.getSenderId(),
            message.getContent().ciphertext(),
            null,
            EventType.CREATED
        );
    }

    public static MessageEvents edited(Message message) {
        return new MessageEvents(
            message.getId(),
            message.getRoomId(),
            message.getSenderId(),
            message.getContent().ciphertext(),
            message.getEditedAt(),
            EventType.EDITED
        );
    }

    public static MessageEvents deleted(Message message) {
        return new MessageEvents(
            message.getId(),
            message.getRoomId(),
            message.getSenderId(),
            message.getContent().ciphertext(),
            null,
            EventType.DELETED
        );
    }
}
```

### InviteEvents

Represents invitation lifecycle events.

```java
public record InviteEvents(
    Long inviteId,
    Long roomId,
    Long createdByUserId,
    String code,
    Instant createdAt,
    Instant expiresAt,
    EventType type
) {
    public enum EventType {
        CREATED,
        ACCEPTED,
        EXPIRED
    }

    public static InviteEvents created(Invite invite) {
        return new InviteEvents(
            invite.getId(),
            invite.getRoomId(),
            invite.getCreatedByUserId(),
            invite.getCode().value(),
            invite.getCreatedAt(),
            invite.getExpiresAt(),
            EventType.CREATED
        );
    }

    public static InviteEvents accepted(Invite invite) {
        return new InviteEvents(
            invite.getId(),
            invite.getRoomId(),
            invite.getCreatedByUserId(),
            invite.getCode().value(),
            invite.getCreatedAt(),
            invite.getExpiresAt(),
            EventType.ACCEPTED
        );
    }

    public static InviteEvents expired(Invite invite) {
        return new InviteEvents(
            invite.getId(),
            invite.getRoomId(),
            invite.getCreatedByUserId(),
            invite.getCode().value(),
            invite.getCreatedAt(),
            invite.getExpiresAt(),
            EventType.EXPIRED
        );
    }
}
```

## Presence Context Events

### PresenceEvents

Represents user presence state changes.

```java
public record PresenceEvents(
    String eventId,
    Long userId,
    PresenceState state,
    Instant occurredAt,
    EventType type
) {
    public enum EventType {
        USER_CAME_ONLINE,
        USER_WENT_OFFLINE,
        USER_STATE_CHANGED
    }

    public PresenceEvents {
        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
        }
    }

    public static PresenceEvents userCameOnline(UserPresence presence, Instant occurredAt) {
        return new PresenceEvents(
            UUID.randomUUID().toString(),
            presence.getUserId(),
            presence.getState(),
            occurredAt,
            EventType.USER_CAME_ONLINE
        );
    }

    public static PresenceEvents userWentOffline(UserPresence presence, Instant occurredAt) {
        return new PresenceEvents(
            UUID.randomUUID().toString(),
            presence.getUserId(),
            presence.getState(),
            occurredAt,
            EventType.USER_WENT_OFFLINE
        );
    }

    public static PresenceEvents userStateChanged(UserPresence presence, Instant occurredAt) {
        return new PresenceEvents(
            UUID.randomUUID().toString(),
            presence.getUserId(),
            presence.getState(),
            occurredAt,
            EventType.USER_STATE_CHANGED
        );
    }
}
```

## Event Publishing

### EventPublisher Port

The `EventPublisher` is a domain port for publishing events:

```java
public interface EventPublisher {
    void publish(Object event);
}
```

### Kafka Implementation

The `KafkaEventPublisher` implements the port:

```java
@Service
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(Object event) {
        String topicName = getTopicForEvent(event);
        kafkaTemplate.send(topicName, event);
    }

    private String getTopicForEvent(Object event) {
        return switch (event.getClass().getSimpleName()) {
            case "RoomEvents"     -> "room-events";
            case "MessageEvents"  -> "message-events";
            case "InviteEvents"   -> "invite-events";
            case "UserEvents"     -> "user-events";
            case "PresenceEvents" -> "presence-events";
            default               -> "domain-events";
        };
    }
}
```

### Event Publishing Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     Event Publishing Flow                        │
│                                                                  │
│  ┌──────────────┐                                                │
│  │ Application  │                                                │
│  │  Service     │                                                │
│  └──────┬───────┘                                                │
│         │                                                         │
│         │ publish(event)                                         │
│         ▼                                                         │
│  ┌──────────────┐                                                │
│  │EventPublisher│ (Port)                                         │
│  └──────┬───────┘                                                │
│         │                                                         │
│         │ send(topic, event)                                    │
│         ▼                                                         │
│  ┌──────────────┐                                                │
│  │  Kafka       │                                                │
│  │  Producer    │                                                │
│  └──────┬───────┘                                                │
│         │                                                         │
│         │ publish to topic                                       │
│         ▼                                                         │
│  ┌──────────────┐                                                │
│  │   Kafka      │                                                │
│  │   Broker     │                                                │
│  └──────────────┘                                                │
└─────────────────────────────────────────────────────────────────┘
```

## Event Consumption

### Event Listener Port

The `EventListener` is a domain port for consuming events:

```java
public interface EventListener<T> {
    void handle(T event);
}
```

### Kafka Implementation

Event listeners consume from Kafka topics:

```java
@Component
public class UserEventListener implements EventListener<UserEvents> {
    private final Broadcaster broadcaster;

    @KafkaListener(topics = "user-events")
    public void handle(UserEvents event) {
        broadcaster.broadcast(event);
    }
}

@Component
public class RoomEventListener implements EventListener<RoomEvents> {
    private final Broadcaster broadcaster;

    @KafkaListener(topics = "room-events")
    public void handle(RoomEvents event) {
        broadcaster.broadcast(event);
    }
}

@Component
public class MessageEventListener implements EventListener<MessageEvents> {
    private final Broadcaster broadcaster;

    @KafkaListener(topics = "message-events")
    public void handle(MessageEvents event) {
        broadcaster.broadcast(event);
    }
}
```

### Event Broadcasting

Events are broadcast to WebSocket clients:

```java
public interface Broadcaster {
    void broadcast(Object event);
}

@Component
public class KafkaBroadcaster implements Broadcaster {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcast(Object event) {
        String destination = getDestinationForEvent(event);
        messagingTemplate.convertAndSend(destination, event);
    }
}
```

## Event-Driven Flow

### Complete Event Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    Complete Event Flow                          │
│                                                                  │
│  ┌──────────────┐  1. Create/Update/Delete                     │
│  │  Controller  │────────────────────┐                          │
│  └──────┬───────┘                    │                          │
│         │                             │                          │
│         ▼                             │                          │
│  ┌──────────────┐                     │                          │
│  │ Application  │  2. Execute use case                          │
│  │  Service     │────────────────────┼──────────────────────┐   │
│  └──────┬───────┘                    │                      │   │
│         │                             │                      │   │
│         ▼                             │                      │   │
│  ┌──────────────┐                     │                      │   │
│  │   Domain     │  3. Domain changes  │                      │   │
│  │   Model      │                     │                      │   │
│  └──────┬───────┘                     │                      │   │
│         │                             │                      │   │
│         │ 4. Create event             │                      │   │
│         ▼                             │                      │   │
│  ┌──────────────┐                     │                      │   │
│  │EventPublisher│  5. Publish to Kafka│                      │   │
│  └──────┬───────┘                     │                      │   │
│         │                             │                      │   │
│         ▼                             │                      │   │
│  ┌──────────────┐                     │                      │   │
│  │   Kafka      │────────────────────┼──────────────────┐   │   │
│  │   Broker     │                     │                  │   │   │
│  └──────────────┘                     │                  │   │   │
│                                        │                  │   │   │
│         ┌──────────────────────────────┼──────────────────┼───┘   │
│         │                              │                  │       │
│         ▼                              ▼                  ▼       │
│  ┌──────────────┐              ┌──────────────┐   ┌──────────────┐
│  │ Event        │ 6. Consume   │ Event        │   │  External    │
│  │ Listener     │◄─────────────│ Listener     │   │  System      │
│  │ (WebSocket)  │              │ (External)   │   │              │
│  └──────┬───────┘              └──────────────┘   └──────────────┘
│         │
│         ▼
│  ┌──────────────┐ 7. Broadcast to clients
│  │  WebSocket   │
│  │  Clients     │
│  └──────────────┘
└─────────────────────────────────────────────────────────────────┘
```

## Event Example Scenarios

### Scenario 1: User Registration

```java
// 1. User registers via REST API
POST /auth/register { "username": "john", "email": "john@example.com", "password": "secret" }

// 2. AuthService handles request
public AuthResponse register(RegisterRequest request) {
    User user = new User(/*...*/);
    User saved = userRepository.save(user);

    // 3. Publish UserEvents.REGISTERED
    eventPublisher.publish(UserEvents.registered(saved, Instant.now()));

    return new AuthResponse(/*...*/);
}

// 4. Kafka publishes to "user-events" topic

// 5. UserEventListener consumes event
@KafkaListener(topics = "user-events")
public void handle(UserEvents event) {
    broadcaster.broadcast(event);
}

// 6. Event broadcasted to WebSocket clients
// 7. Other systems can also consume the event (e.g., email service)
```

### Scenario 2: Message Sent

```java
// 1. User sends message via WebSocket or REST
POST /rooms/{roomId}/messages { "content": "Hello!" }

// 2. MessageService handles request
public Message createMessage(Long senderId, Long roomId, String plaintext, Instant now) {
    roomValidator.validateAndGetRoom(roomId, senderId);
    String ciphertext = encryptionService.encrypt(plaintext);
    Message message = new Message(senderId, roomId, new MessageContent(ciphertext), now);
    Message saved = messageRepository.save(message);

    // 3. Publish MessageEvents.CREATED
    eventPublisher.publish(MessageEvents.created(saved));

    return saved;
}

// 4. Kafka publishes to "message-events" topic

// 5. MessageEventListener consumes event
@KafkaListener(topics = "message-events")
public void handle(MessageEvents event) {
    broadcaster.broadcast(event);
}

// 6. Event broadcasted to all clients in the room
// 7. Presence system updates user's last activity
```

### Scenario 3: User Goes Online

```java
// 1. User connects via WebSocket
ws://localhost:8000/ws/rooms/{roomId}?token={jwt}

// 2. WebSocket handler authenticates user
// 3. PresenceService updates state
public void setOnline(Long userId) {
    UserPresence presence = presenceRepository.findByUserId(userId)
        .orElseGet(() -> new UserPresence(userId, PresenceState.ONLINE, Instant.now()));

    presence.setOnline();
    presenceRepository.save(presence);

    // 4. Publish PresenceEvents.USER_CAME_ONLINE
    eventPublisher.publish(PresenceEvents.userCameOnline(presence, Instant.now()));
}

// 5. Kafka publishes to "presence-events" topic

// 6. PresenceEventListener consumes event
@KafkaListener(topics = "presence-events")
public void handle(PresenceEvents event) {
    broadcaster.broadcast(event);
}

// 7. All clients see user as online
```

## Event Best Practices

### 1. Use Descriptive Names

Events should clearly describe what happened:

```java
// ✅ Good - clear and descriptive
UserEvents.REGISTERED
MessageEvents.CREATED
RoomEvents.MEMBER_JOINED

// ❌ Bad - vague
UserEvents.CREATE
MessageEvents.SAVE
```

### 2. Include All Relevant Data

Events should contain all data needed by consumers:

```java
// ✅ Good - complete data
public record UserEvents(
    Long userId,
    String username,
    String email,
    Instant occurredAt,
    EventType type
) { }

// ❌ Bad - incomplete data
public record UserEvents(
    Long userId,
    Instant occurredAt,
    EventType type
) { }
```

### 3. Use Immutable Records

Events should be immutable:

```java
// ✅ Good - immutable record
public record UserEvents(Long userId, String username, /*...*/) { }

// ❌ Bad - mutable class
public class UserEvents {
    private Long userId;
    public void setUserId(Long userId) { this.userId = userId; }
}
```

### 4. Don't Include Secrets

Events should not contain sensitive information:

```java
// ✅ Good - no secrets
public record MessageEvents(
    Long messageId,
    Long roomId,
    Long senderId,
    String ciphertext,  // Encrypted, not plaintext
    /*...*/
) { }

// ❌ Bad - includes plaintext password
public record UserEvents(
    Long userId,
    String username,
    String password,  // Don't do this!
    /*...*/
) { }
```

### 5. Use Factory Methods

Factory methods improve readability:

```java
// ✅ Good - clear factory methods
UserEvents.registered(user, now);
MessageEvents.created(message);

// ❌ Bad - verbose constructor
new UserEvents(userId, username, email, now, EventType.REGISTERED);
```

## Event Ordering and Guarantees

### Kafka Guarantees

- **Ordering**: Messages within a partition are ordered
- **At-Least-Once**: Messages are delivered at least once
- **Persistence**: Messages are persisted to disk

### Event Ordering

Events are ordered within a partition by timestamp:

```
user-events partition:
[10:00:00] UserEvents.REGISTERED (userId=1)
[10:01:00] UserEvents.PASSWORD_CHANGED (userId=1)
[10:02:00] UserEvents.DEACTIVATED (userId=1)
```

## Event Schema Evolution

### Adding Fields

Events can evolve by adding fields:

```java
// Version 1
public record UserEvents(Long userId, String username, Instant occurredAt, EventType type) { }

// Version 2 - added email field
public record UserEvents(Long userId, String username, String email, Instant occurredAt, EventType type) { }
```

### Backward Compatibility

- New fields should be optional or have defaults
- Old consumers can ignore new fields
- Use schema registry for complex scenarios

## Benefits of Domain Events

### 1. Loose Coupling

Bounded contexts communicate via events, not direct calls:

```java
// Collaboration context doesn't directly call Presence context
eventPublisher.publish(MessageEvents.created(message));
// Presence context consumes the event independently
```

### 2. Asynchronous Processing

Time-consuming operations can be processed asynchronously:

```java
// Quick response to user
eventPublisher.publish(UserEvents.registered(user, now));
return new AuthResponse(/*...*/);

// Email service processes asynchronously
@KafkaListener(topics = "user-events")
public void handle(UserEvents event) {
    emailService.sendWelcomeEmail(event);
}
```

### 3. Audit Trail

Events provide a complete audit of what happened:

```java
// All events are stored in Kafka topics
// Can be replayed or analyzed
```

### 4. Multiple Consumers

Multiple consumers can react to the same event:

```java
// WebSocket listener broadcasts to clients
@KafkaListener(topics = "user-events")
public void handleForWebSocket(UserEvents event) {
    broadcaster.broadcast(event);
}

// Email listener sends notifications
@KafkaListener(topics = "user-events")
public void handleForEmail(UserEvents event) {
    emailService.sendNotification(event);
}

// Analytics listener tracks metrics
@KafkaListener(topics = "user-events")
public void handleForAnalytics(UserEvents event) {
    analytics.trackEvent(event);
}
```

## Next Steps

- [Repositories](./07-repositories.md) - Learn how aggregates are persisted
- [Application Services](./08-application-services.md) - Understand use case orchestration
- [Infrastructure Adapters](./09-infrastructure-adapters.md) - See how events are handled
