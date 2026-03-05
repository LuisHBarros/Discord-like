# Infrastructure Adapters

## Introduction

Infrastructure adapters implement the ports defined in the domain layer, handling technical concerns like database access, messaging, encryption, and external service integration.

## Adapter Categories

### 1. Persistence Adapters

#### JPA Configuration

```java
@Configuration
@EnableJpaRepositories(basePackages = "com.luishbarros.discord_like")
public class JpaConfig {
    // Auto-configured by Spring Boot
}
```

#### JPA Entities

**UserJpaEntity**

```java
@Entity
@Table(name = "users")
public class UserJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
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

**MessageJpaEntity**

```java
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_room_id", columnList = "room_id"),
    @Index(name = "idx_messages_created_at", columnList = "created_at")
})
public class MessageJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;  // Encrypted ciphertext

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant editedAt;

    public static MessageJpaEntity fromDomain(Message message) {
        MessageJpaEntity entity = new MessageJpaEntity();
        entity.setId(message.getId());
        entity.setSenderId(message.getSenderId());
        entity.setRoomId(message.getRoomId());
        entity.setContent(message.getContent().ciphertext());
        entity.setCreatedAt(message.getCreatedAt());
        entity.setEditedAt(message.getEditedAt());
        return entity;
    }

    public Message toDomain() {
        return Message.reconstitute(
            id,
            senderId,
            roomId,
            new MessageContent(content),
            createdAt,
            editedAt
        );
    }
}
```

### 2. Security Adapters

#### JWT Token Provider

```java
@Service
public class JwtTokenProvider implements TokenProvider {
    private final SecretKey accessKey;
    private final SecretKey refreshKey;

    public JwtTokenProvider(
            @Value("${jwt.access.secret}") String accessSecret,
            @Value("${jwt.refresh.secret}") String refreshSecret
    ) {
        this.accessKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecret));
        this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecret));
    }

    public String generateAccessToken(Long userId) {
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Instant.now())
            .expiration(Instant.now().plus(15, ChronoUnit.MINUTES))
            .signWith(accessKey)
            .compact();
    }

    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Instant.now())
            .expiration(Instant.now().plus(7, ChronoUnit.DAYS))
            .signWith(refreshKey)
            .compact();
    }

    public Long validateAccessToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(accessKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public Long validateRefreshToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(refreshKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return Long.parseLong(claims.getSubject());
    }
}
```

#### JWT Filter

```java
@Component
public class JwtFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;
    private final TokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        String token = extractToken(request);

        if (token != null) {
            if (tokenBlacklist.isBlacklisted(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            try {
                Long userId = tokenProvider.validateAccessToken(token);
                Authentication auth = new UsernamePasswordAuthenticationToken(
                    userId, null, Collections.emptyList()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

#### Argon2 Password Hasher

```java
@Service
public class Argon2PasswordHasher implements PasswordHasher {
    private static final int ITERATIONS = 3;
    private static final int MEMORY = 65536;
    private static final int PARALLELISM = 4;

    @Override
    public String hash(String password) {
        Argon2 argon2 = Argon2Factory.create(
            Argon2Factory.Argon2Types.ARGON2id,
            ITERATIONS,
            MEMORY,
            PARALLELISM
        );
        return argon2.hash(32, 16, password.toCharArray());
    }

    @Override
    public boolean verify(String password, String hash) {
        Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
        return argon2.verify(hash, password.toCharArray());
    }
}
```

### 3. Messaging Adapters

#### Kafka Event Publisher

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

#### Kafka Event Listeners

```java
@Component
public class UserEventListener {
    private final Broadcaster broadcaster;

    @KafkaListener(topics = "user-events")
    public void handle(UserEvents event) {
        broadcaster.broadcast(event);
    }
}
```

#### Kafka Broadcaster

```java
@Service
public class KafkaBroadcaster implements Broadcaster {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcast(Object event) {
        String destination = getDestinationForEvent(event);
        messagingTemplate.convertAndSend(destination, event);
    }

    private String getDestinationForEvent(Object event) {
        return switch (event.getClass().getSimpleName()) {
            case "UserEvents"     -> "/topic/user-events";
            case "RoomEvents"     -> "/topic/room-events";
            case "MessageEvents"  -> "/topic/message-events";
            case "PresenceEvents" -> "/topic/presence-events";
            default               -> "/topic/domain-events";
        };
    }
}
```

### 4. WebSocket Adapters

#### WebSocket Handler

```java
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final WebSocketSessionManager sessionManager;
    private final JwtTokenProvider tokenProvider;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        Long roomId = extractRoomId(session);

        sessionManager.addSession(roomId, userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        IncomingMessage incoming = parseMessage(message.getPayload());
        Long userId = extractUserId(session);

        switch (incoming.type()) {
            case "message" -> handleMessage(userId, incoming);
            case "ping" -> handlePing(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = extractUserId(session);
        Long roomId = extractRoomId(session);

        sessionManager.removeSession(roomId, userId, session);
    }
}
```

#### WebSocket Session Manager

```java
@Component
public class WebSocketSessionManager {
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, Set<WebSocketSession>>> roomSessions =
        new ConcurrentHashMap<>();

    public void addSession(Long roomId, Long userId, WebSocketSession session) {
        roomSessions
            .computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
            .add(session);
    }

    public void removeSession(Long roomId, Long userId, WebSocketSession session) {
        Map<Long, Set<WebSocketSession>> room = roomSessions.get(roomId);
        if (room != null) {
            Set<WebSocketSession> sessions = room.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    room.remove(userId);
                }
                if (room.isEmpty()) {
                    roomSessions.remove(roomId);
                }
            }
        }
    }

    public void broadcastToRoom(Long roomId, Object message) {
        Map<Long, Set<WebSocketSession>> room = roomSessions.get(roomId);
        if (room != null) {
            List.copyOf(room.values()).forEach(sessions ->
                List.copyOf(sessions).forEach(session ->
                    sendMessage(session, message)
                )
            );
        }
    }

    private void sendMessage(WebSocketSession session, Object message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(convertToJson(message)));
            }
        } catch (Exception e) {
            session.close();
        }
    }
}
```

### 5. Encryption Adapters

#### AES Encryption Service

```java
@Service
public class AesEncryptionService implements EncryptionService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;

    public AesEncryptionService(@Value("${encryption.secret}") String secretBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            byte[] encryptedBytes = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedBytes, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedBytes, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(ciphertext);

            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedBytes, 0, iv, 0, iv.length);

            // Extract actual ciphertext
            byte[] cipherText = new byte[encryptedBytes.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedBytes, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintext = cipher.doFinal(cipherText);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed", e);
        }
    }
}
```

### 6. Caching Adapters

#### Redis Token Blacklist

```java
@Service
@Primary
public class RedisTokenBlacklist implements TokenBlacklist {
    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration TTL = Duration.ofDays(7);

    @Override
    public void add(String token) {
        redisTemplate.opsForValue().set("blacklist:" + token, "1", TTL);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }
}
```

#### Redis Rate Limiter

```java
@Service
public class RedisRateLimiter implements RateLimiter {
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean isAllowed(String key, int maxRequests, long windowSeconds) {
        String redisKey = "ratelimit:" + key;
        Long currentCount = redisTemplate.opsForValue().increment(redisKey);

        if (currentCount == 1) {
            redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }

        return currentCount <= maxRequests;
    }
}
```

## Adapter Best Practices

1. **Implement Ports** - All adapters implement domain ports
2. **Hide Details** - Encapsulate technical complexity
3. **Handle Errors** - Convert technical errors to domain errors
4. **No Business Logic** - Keep logic in domain layer
5. **Testable** - Use dependency injection for testing

## Benefits

1. **Swappable Implementations** - Easy to change implementations
2. **Testability** - Mock adapters for testing
3. **Separation of Concerns** - Technical concerns isolated
4. **Flexibility** - Multiple implementations per port

## Next Steps

- [Design Patterns](./10-design-patterns.md) - Explore patterns used
- [Project Structure](../development/01-project-structure.md) - Learn file organization
