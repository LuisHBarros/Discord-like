# Project Structure Guide

## Introduction

This guide explains the project structure, naming conventions, and how to add new features to the Discord-like application.

## Module Organization

The project follows a **feature-based module structure** organized by bounded contexts:

```
src/main/java/com/luishbarros/discord_like/
├── DiscordLikeApplication.java
├── modules/
│   ├── identity/              # Identity & Authentication bounded context
│   │   ├── application/     # Application layer
│   │   │   ├── dto/        # Data Transfer Objects
│   │   │   └── service/    # Application services
│   │   ├── domain/          # Domain layer (no external dependencies)
│   │   │   ├── event/       # Domain events
│   │   │   ├── model/       # Entities, value objects, aggregates
│   │   │   │   ├── value_object/
│   │   │   │   └── error/
│   │   │   ├── ports/       # Domain ports (interfaces)
│   │   │   └── service/     # Domain services
│   │   └── infrastructure/ # Infrastructure layer
│   │       ├── adapters/    # Port implementations
│   │       ├── event/       # Event listeners
│   │       ├── http/        # REST controllers
│   │       ├── persistence/  # JPA entities
│   │       └── security/    # Security components
│   ├── collaboration/        # Collaboration & Chat bounded context
│   │   └── ... (same structure as identity)
│   └── presence/            # Presence bounded context
│       └── ... (same structure as identity)
└── shared/                # Shared kernel
    ├── adapters/            # Shared infrastructure adapters
    │   ├── config/         # Spring configuration
    │   ├── http/            # Shared HTTP components
    │   ├── messaging/       # Kafka adapters
    │   ├── middleware/      # Cross-cutting middleware
    │   ├── presence/        # Presence store implementation
    │   └── ratelimit/       # Rate limiting
    ├── domain/              # Shared domain concepts
    │   ├── error/           # Base domain error
    │   └── model/           # BaseEntity
    └── ports/               # Cross-cutting ports
        ├── EventPublisher.java
        ├── EventListener.java
        ├── Broadcaster.java
        ├── PresenceStore.java
        └── RateLimiter.java
```

## Module Structure Pattern

Each bounded context follows the same structure:

```
module-name/
├── application/          # Application Layer
│   ├── dto/           # Request/Response DTOs
│   ├── factory/        # Factory classes for complex objects
│   └── service/        # Application services (use cases)
├── domain/              # Domain Layer (PURE - no dependencies)
│   ├── event/           # Domain events
│   ├── model/           # Domain model
│   │   ├── aggregate/   # Aggregate roots
│   │   ├── value_object/ # Value objects
│   │   └── error/       # Domain errors
│   ├── ports/           # Domain ports (interfaces)
│   │   ├── repository/  # Repository interfaces
│   │   └── services/    # Service interfaces
│   └── service/         # Domain services
└── infrastructure/      # Infrastructure Layer
    ├── adapter/         # Port implementations
    ├── encryption/      # Encryption services
    ├── event/           # Event listeners (driving adapters)
    ├── http/            # REST controllers (driving adapters)
    ├── persistence/      # JPA/Hibernate setup
    │   ├── entity/      # JPA entities
    │   └── repository/  # Spring Data repositories
    └── websocket/       # WebSocket handlers
```

## Collaboration Module Structure

The collaboration module includes E2EE and Conversation features:

```
modules/collaboration/
├── application/
│   ├── dto/           # CreateRoomRequest, UpdateRoomRequest, JoinRoomRequest,
│   │                  # SendMessageRequest, RoomResponse, MessageResponse,
│   │                  # InviteResponse, UpdateMessageRequest
│   ├── factory/       # InviteFactory (generates 8-char uppercase invite codes)
│   └── service/       # RoomService, MessageService, InviteService,
│                      # E2EEKeyManagementService, ConversationService
├── domain/
│   ├── event/       # RoomEvents, MessageEvents, InviteEvents, EncryptionEvents
│   ├── model/
│   │   ├── aggregate/  # Room, Message, Invite, Conversation
│   │   ├── error/       # RoomEncryptionError
│   │   └── value_object/  # InviteCode, MessageContent, RoomName, Membership
│   ├── ports/         # EncryptionService, RoomEncryptionStateRepository, ConversationRepository
│   └── repository/    # RoomRepository, MessageRepository, InviteRepository
└── infrastructure/
    ├── adapter/       # RoomRepositoryAdapter, MessageRepositoryAdapter, InviteRepositoryAdapter,
    │                  # RoomEncryptionStateRepositoryAdapter, ConversationRepositoryAdapter
    ├── encryption/    # AesEncryptionService (AES/GCM/NoPadding, random 12-byte IV,
    │                  # IV prepended to ciphertext, key from ENCRYPTION_SECRET env var),
    │                  # E2EEKeyManagementService (X25519 key exchange, key rotation)
    ├── event/         # InviteEventListener, MessageEventListener, RoomEventListener
    │                  # (Kafka consumers → broadcast to WebSocket)
    ├── http/          # RoomController, MessageController, E2EEController
    ├── persistence/
    │   ├── entity/    # RoomJpaEntity, MessageJpaEntity, InviteJpaEntity,
    │   │              # RoomEncryptionStateJpaEntity, ConversationJpaEntity
    │   └── repository/ # RoomJpaRepository, MessageJpaRepository, InviteJpaRepository,
    │                  # RoomEncryptionStateJpaRepository, ConversationJpaRepository
    └── websocket/       # WebSocket handlers
```

## Naming Conventions

### Packages

- **Domain**: `domain.model`, `domain.value_object`, `domain.error`, `domain.event`, `domain.ports`
- **Application**: `application.dto`, `application.service`, `application.factory`
- **Infrastructure**: `infrastructure.http`, `infrastructure.persistence`, `infrastructure.event`

### Classes

#### Domain Layer

**Entities**: Noun, singular, represents a domain concept
- `User`, `Room`, `Message`, `Invite`

**Value Objects**: Noun, singular, represents a value
- `Username`, `Email`, `PasswordHash`, `RoomName`, `MessageContent`, `InviteCode`

**Aggregates**: Entity that is the root of an aggregate
- `User`, `Room`, `Message`, `Invite`, `Conversation`, `UserPresence`

**Domain Events**: `[Aggregate]Events`
- `UserEvents`, `RoomEvents`, `MessageEvents`, `InviteEvents`, `PresenceEvents`, `EncryptionEvents`

**Domain Errors**: Descriptive, ends with `Error`
- `InvalidUserError`, `InvalidRoomError`, `InvalidMessageError`, `ForbiddenError`

**Repository Ports**: `[Aggregate]Repository`
- `UserRepository`, `RoomRepository`, `MessageRepository`, `InviteRepository`

#### Application Layer

**Application Services**: `[Aggregate]Service`
- `AuthService`, `UserService`, `RoomService`, `MessageService`, `InviteService`

**DTOs**: Descriptive, ends with `Request` or `Response`
- `RegisterRequest`, `LoginRequest`, `CreateRoomRequest`, `RoomResponse`, `MessageResponse`

**Factories**: `[Aggregate]Factory`
- `InviteFactory`

#### Infrastructure Layer

**Adapters**: `[Port]Adapter` or `[Port]Impl`
- `JpaUserRepository`, `RoomRepositoryAdapter`, `KafkaEventPublisher`, `AesEncryptionService`

**Controllers**: `[Aggregate]Controller`
- `AuthController`, `UserController`, `RoomController`, `MessageController`

**Event Listeners**: `[Aggregate]EventListener`
- `UserEventListener`, `RoomEventListener`, `MessageEventListener`

**JPA Entities**: `[Aggregate]JpaEntity`
- `UserJpaEntity`, `RoomJpaEntity`, `MessageJpaEntity`, `InviteJpaEntity`

## Adding a New Feature

### Step 1: Define Domain Model

Create the domain entity and value objects:

```java
// modules/new-feature/domain/model/NewEntity.java
package com.luishbarros.discord_like.modules.new_feature.domain.model;

import com.luishbarros.discord_like.shared.domain.model.BaseEntity;
import java.time.Instant;

public class NewEntity extends BaseEntity {
    private NewValueObject value;
    private Instant createdAt;
    private Instant updatedAt;

    protected NewEntity() {}

    public NewEntity(NewValueObject value, Instant createdAt) {
        this.value = value;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static NewEntity reconstitute(Long id, NewValueObject value,
                                         Instant createdAt, Instant updatedAt) {
        NewEntity entity = new NewEntity();
        entity.id = id;
        entity.value = value;
        entity.createdAt = createdAt;
        entity.updatedAt = updatedAt;
        return entity;
    }

    public NewValueObject getValue() { return value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

```java
// modules/new-feature/domain/model/value_object/NewValueObject.java
package com.luishbarros.discord_like.modules.new_feature.domain.model.value_object;

import com.luishbarros.discord_like.modules.new_feature.domain.error.NewFeatureError;

public record NewValueObject(String value) {
    public NewValueObject {
        if (value == null || value.isBlank()) {
            throw NewFeatureError.invalidValue("Value cannot be blank");
        }
        if (value.length() > 100) {
            throw NewFeatureError.invalidValue("Value too long");
        }
    }
}
```

### Step 2: Define Repository Port

```java
// modules/new-feature/domain/ports/repository/NewEntityRepository.java
package com.luishbarros.discord_like.modules.new_feature.domain.ports.repository;

import com.luishbarros.discord_like.modules.new_feature.domain.model.NewEntity;
import java.util.List;
import java.util.Optional;

public interface NewEntityRepository {
    NewEntity save(NewEntity entity);
    Optional<NewEntity> findById(Long id);
    List<NewEntity> findAll();
    void deleteById(Long id);
    boolean existsById(Long id);
}
```

### Step 3: Define Domain Errors

```java
// modules/new-feature/domain/model/error/NewFeatureError.java
package com.luishbarros.discord_like.modules.new_feature.domain.model.error;

import com.luishbarros.discord_like.shared.domain.error.DomainError;

public class NewFeatureError extends DomainError {
    private NewFeatureError(String code, String message) {
        super(code, message);
    }

    public static NewFeatureError invalidValue(String message) {
        return new NewFeatureError("INVALID_VALUE", message);
    }

    public static NewFeatureError notFound(String id) {
        return new NewFeatureError("NOT_FOUND", "Entity not found: " + id);
    }
}
```

### Step 4: Create Application Service

```java
// modules/new-feature/application/service/NewEntityService.java
package com.luishbarros.discord_like.modules.new_feature.application.service;

import com.luishbarros.discord_like.modules.new_feature.domain.model.NewEntity;
import com.luishbarros.discord_like.modules.new_feature.domain.ports.repository.NewEntityRepository;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class NewEntityService {
    private final NewEntityRepository repository;
    private final EventPublisher eventPublisher;

    public NewEntityService(NewEntityRepository repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public NewEntity create(String value, Instant now) {
        NewEntity entity = new NewEntity(new NewValueObject(value), now);
        NewEntity saved = repository.save(entity);
        // Publish event if needed
        return saved;
    }

    public NewEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> NewFeatureError.notFound(id.toString()));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
```

### Step 5: Create DTOs

```java
// modules/new-feature/application/dto/CreateNewEntityRequest.java
package com.luishbarros.discord_like.modules.new_feature.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNewEntityRequest(
    @NotBlank(message = "Value cannot be blank")
    @Size(max = 100, message = "Value too long")
    String value
) {}
```

```java
// modules/new-feature/application/dto/NewEntityResponse.java
package com.luishbarros.discord_like.modules.new_feature.application.dto;

import java.time.Instant;

public record NewEntityResponse(
    Long id,
    String value,
    Instant createdAt,
    Instant updatedAt
) {
    public static NewEntityResponse fromDomain(NewEntity entity) {
        return new NewEntityResponse(
                entity.getId(),
                entity.getValue().value(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
```

### Step 6: Create JPA Entity

```java
// modules/new-feature/infrastructure/persistence/entity/NewEntityJpaEntity.java
package com.luishbarros.discord_like.modules.new_feature.infrastructure.persistence.entity;

import com.luishbarros.discord_like.modules.new_feature.domain.model.NewEntity;
import com.luishbarros.discord_like.modules.new_feature.domain.model.value_object.NewValueObject;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "new_entities")
public class NewEntityJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "value", nullable = false)
    private String value;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NewEntityJpaEntity() {}

    public NewEntityJpaEntity(Long id, String value, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.value = value;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getValue() { return value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setValue(String value) { this.value = value; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public NewEntity toDomain() {
        return NewEntity.reconstitute(
                id,
                new NewValueObject(value),
                createdAt,
                updatedAt
        );
    }

    public static NewEntityJpaEntity fromDomain(NewEntity entity) {
        return new NewEntityJpaEntity(
                entity.getId(),
                entity.getValue().value(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
```

### Step 7: Create Repository Adapter

```java
// modules/new-feature/infrastructure/adapter/NewEntityRepositoryAdapter.java
package com.luishbarros.discord_like.modules.new_feature.infrastructure.adapter;

import com.luishbarros.discord_like.modules.new_feature.domain.model.NewEntity;
import com.luishbarros.discord_like.modules.new_feature.domain.ports.repository.NewEntityRepository;
import com.luishbarros.discord_like.modules.new_feature.infrastructure.persistence.entity.NewEntityJpaEntity;
import com.luishbarros.discord_like.modules.new_feature.infrastructure.persistence.repository.SpringDataNewEntityRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class NewEntityRepositoryAdapter implements NewEntityRepository {
    private final SpringDataNewEntityRepository jpaRepository;

    public NewEntityRepositoryAdapter(SpringDataNewEntityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public NewEntity save(NewEntity entity) {
        NewEntityJpaEntity jpaEntity = NewEntityJpaEntity.fromDomain(entity);
        return jpaRepository.save(jpaEntity).toDomain();
    }

    @Override
    public Optional<NewEntity> findById(Long id) {
        return jpaRepository.findById(id).map(NewEntityJpaEntity::toDomain);
    }

    @Override
    public List<NewEntity> findAll() {
        return jpaRepository.findAll().stream()
                .map(NewEntityJpaEntity::toDomain)
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

### Step 8: Create Controller

```java
// modules/new-feature/infrastructure/http/NewEntityController.java
package com.luishbarros.discord_like.modules.new_feature.infrastructure.http;

import com.luishbarros.discord_like.modules.new_feature.application.dto.CreateNewEntityRequest;
import com.luishbarros.discord_like.modules.new_feature.application.dto.NewEntityResponse;
import com.luishbarros.discord_like.modules.new_feature.application.service.NewEntityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/new-entities")
public class NewEntityController {
    private final NewEntityService service;

    public NewEntityController(NewEntityService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<NewEntityResponse> create(
            @Valid @RequestBody CreateNewEntityRequest request
    ) {
        NewEntity entity = service.create(request.value(), Instant.now());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(NewEntityResponse.fromDomain(entity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewEntityResponse> getById(@PathVariable Long id) {
        NewEntity entity = service.findById(id);
        return ResponseEntity.ok(NewEntityResponse.fromDomain(entity));
    }

    @GetMapping
    public ResponseEntity<List<NewEntityResponse>> getAll() {
        return ResponseEntity.ok(
                service.findAll().stream()
                        .map(NewEntityResponse::fromDomain)
                        .toList()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Step 9: Register Error Handler

Update `DomainErrorHandler` to map your new error codes:

```java
// shared/adapters/middleware/DomainErrorHandler.java
private HttpStatus mapErrorToStatus(String code) {
    return switch (code) {
        // ... existing mappings ...
        case "INVALID_VALUE", "NOT_FOUND" -> HttpStatus.BAD_REQUEST;
        default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
}
```

## Layer Rules

### Domain Layer

**DO:**
- Keep pure (no external dependencies)
- Use value objects for type safety
- Enforce invariants in aggregates
- Use factory methods for object creation
- Define ports (interfaces)

**DON'T:**
- Use Spring annotations
- Depend on external libraries
- Put infrastructure concerns in domain
- Expose internal state directly

### Application Layer

**DO:**
- Orchestrate use cases
- Use DTOs for API contracts
- Publish domain events
- Keep services thin (no business logic)

**DON'T:**
- Put business logic in services
- Directly use infrastructure
- Skip validation
- Make services stateful

### Infrastructure Layer

**DO:**
- Implement domain ports
- Handle technical concerns
- Map between domain and infrastructure
- Use Spring components appropriately

**DON'T:**
- Put business logic in adapters
- Expose infrastructure to domain
- Skip error handling
- Violate layer boundaries

## File Organization

### Where to Put Code

| Code Type | Location |
|-----------|----------|
| Domain entities | `modules/{context}/domain/model/` |
| Value objects | `modules/{context}/domain/model/value_object/` |
| Domain events | `modules/{context}/domain/event/` |
| Domain errors | `modules/{context}/domain/model/error/` |
| Repository ports | `modules/{context}/domain/ports/repository/` |
| Service ports | `modules/{context}/domain/ports/services/` |
| Application services | `modules/{context}/application/service/` |
| DTOs | `modules/{context}/application/dto/` |
| JPA entities | `modules/{context}/infrastructure/persistence/entity/` |
| Spring Data repos | `modules/{context}/infrastructure/persistence/repository/` |
| Controllers | `modules/{context}/infrastructure/http/` |
| Event listeners | `modules/{context}/infrastructure/event/` |

## Testing Structure

```
src/test/java/com/luishbarros/discord_like/
├── modules/
│   ├── identity/
│   │   ├── application/
│   │   │   └── service/
│   │   │       └── AuthServiceTest.java
│   │   └── infrastructure/
│   │       └── persistence/
│   │           └── repository/
│   │               └── UserRepositoryAdapterTest.java
│   ├── collaboration/
│   │   └── ... (same pattern)
│   └── presence/
│       └── ... (same pattern)
└── shared/
    └── adapters/
        └── middleware/
            └── DomainErrorHandlerTest.java
```

## Best Practices

### 1. Consistent Package Structure

All modules follow the same structure, making navigation predictable.

### 2. Clear Dependencies

Dependencies flow inward: `Infrastructure → Ports ← Application ← Domain`

### 3. Use Ports for External Concerns

Never depend directly on external systems in domain layer.

### 4. Keep Domain Pure

Domain classes should have zero external dependencies.

### 5. Use Value Objects

Replace primitive types with value objects for better type safety.

### 6. Enforce Invariants

Business rules are enforced in domain, not in services.

### 7. Use Factory Methods

Complex object creation uses factory methods for clarity.

### 8. Test Each Layer

- Domain tests verify business logic
- Application tests verify use cases
- Infrastructure tests verify integration

## Summary

The project structure:

- **Feature-based modules** organized by bounded contexts
- **Consistent layering** across all modules
- **Clear naming conventions** for easy navigation
- **Hexagonal architecture** with domain isolation
- **Testable design** with clear boundaries

When adding features, follow the established patterns to maintain consistency and code quality.

## Next Steps

- [Testing Guide](./02-testing.md) - Learn how to write tests
- [Aggregates](../architecture/04-aggregates.md) - Understand aggregate design
- [Value Objects](../architecture/05-value-objects.md) - Learn value object patterns
