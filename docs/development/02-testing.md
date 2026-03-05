# Testing Guide

## Introduction

This guide explains the testing approach, test coverage, and how to write tests for the Discord-like application.

## Testing Strategy

The application uses a **layered testing approach**:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Testing Pyramid                             │
│                                                                  │
│         ┌──────────────┐                                       │
│         │  E2E Tests   │         (Manual/Selenium)            │
│         │   (Few)     │                                       │
│         └──────────────┘                                       │
│                  ▲                                             │
│         ┌──────────────┐                                       │
│         │ Integration  │         (Testcontainers)              │
│         │   Tests      │                                       │
│         │   (Some)     │                                       │
│         └──────────────┘                                       │
│                  ▲                                             │
│         ┌──────────────┐                                       │
│         │   Unit Tests │         (JUnit + Mockito)            │
│         │   (Many)     │                                       │
│         └──────────────┘                                       │
└─────────────────────────────────────────────────────────────────┘
```

### Test Types

1. **Unit Tests** - Test individual components in isolation
2. **Integration Tests** - Test component interactions
3. **End-to-End Tests** - Test complete user flows

## Testing Stack

- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **AssertJ** - Assertion library
- **Spring Boot Test** - Spring testing support
- **Spring Security Test** - Security testing
- **Testcontainers** - Integration testing with real containers (planned)
- **H2** - In-memory database for tests

## Test Structure

```
src/test/java/com/luishbarros/discord_like/
├── modules/
│   ├── identity/
│   │   ├── application/
│   │   │   └── service/
│   │   │       ├── AuthServiceTest.java
│   │   │       └── UserServiceTest.java
│   │   └── infrastructure/
│   │       └── persistence/
│   │           └── repository/
│   │               └── UserRepositoryAdapterTest.java
│   ├── collaboration/
│   │   ├── application/
│   │   │   └── service/
│   │   │       ├── RoomServiceTest.java
│   │   │       ├── MessageServiceTest.java
│   │   │       └── InviteServiceTest.java
│   │   └── infrastructure/
│   │       ├── adapter/
│   │       │   └── InviteRepositoryAdapterTest.java
│   │       ├── persistence/
│   │       │   └── repository/
│   │       │       └── RoomJpaRepositoryTest.java
│   │       └── websocket/
│   │           └── WebSocketSessionManagerTest.java
│   └── presence/
│       ├── application/
│       │   └── service/
│       │       └── PresenceServiceTest.java
│       └── infrastructure/
│           └── adapter/
│               └── PresenceRepositoryAdapterTest.java
└── shared/
    └── adapters/
        └── middleware/
            └── DomainErrorHandlerTest.java
```

## Writing Unit Tests

### Domain Tests

Domain tests verify business logic without external dependencies:

```java
package com.luishbarros.discord_like.modules.identity.domain.model;

import com.luishbarros.discord_like.modules.identity.domain.model.error.InvalidUserError;
import com.luishbarros.discord_like.modules.identity.domain.model.value_object.Email;
import com.luishbarros.discord_like.modules.identity.domain.model.value_object.PasswordHash;
import com.luishbarros.discord_like.modules.identity.domain.model.value_object.Username;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    @DisplayName("Should create user with valid data")
    void shouldCreateUserWithValidData() {
        // Given
        Username username = new Username("john_doe");
        Email email = new Email("john@example.com");
        PasswordHash passwordHash = new PasswordHash("hashed_password");
        Instant now = Instant.now();

        // When
        User user = new User(username, email, passwordHash, now);

        // Then
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should throw error when changing password of inactive user")
    void shouldThrowErrorWhenChangingPasswordOfInactiveUser() {
        // Given
        User user = new User(
                new Username("john_doe"),
                new Email("john@example.com"),
                new PasswordHash("hashed_password"),
                Instant.now()
        );
        user.deactivate(Instant.now());

        // When/Then
        assertThatThrownBy(() ->
                user.changePassword(new PasswordHash("new_hash"), Instant.now())
        )
                .isInstanceOf(InvalidUserError.class)
                .hasMessage("Cannot change password of inactive user");
    }

    @Test
    @DisplayName("Should throw error when activating already active user")
    void shouldThrowErrorWhenActivatingAlreadyActiveUser() {
        // Given
        User user = new User(
                new Username("john_doe"),
                new Email("john@example.com"),
                new PasswordHash("hashed_password"),
                Instant.now()
        );

        // When/Then
        assertThatThrownBy(() -> user.activate(Instant.now()))
                .isInstanceOf(InvalidUserError.class)
                .hasMessage("User is already active");
    }
}
```

### Value Object Tests

Value object tests verify validation logic:

```java
package com.luishbarros.discord_like.modules.identity.domain.model.value_object;

import com.luishbarros.discord_like.modules.identity.domain.model.error.InvalidUserError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    @DisplayName("Should create email with valid address")
    void shouldCreateEmailWithValidAddress() {
        // Given
        String emailValue = "user@example.com";

        // When
        Email email = new Email(emailValue);

        // Then
        assertThat(email.value()).isEqualTo(emailValue);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "invalid-email",
            "@example.com",
            "user@",
            "user@.com"
    })
    @DisplayName("Should throw error for invalid email")
    void shouldThrowErrorForInvalidEmail(String invalidEmail) {
        // When/Then
        assertThatThrownBy(() -> new Email(invalidEmail))
                .isInstanceOf(InvalidUserError.class)
                .hasMessageContaining("Invalid email format");
    }
}
```

### Application Service Tests

Application service tests use mocks for dependencies:

```java
package com.luishbarros.discord_like.modules.identity.application.service;

import com.luishbarros.discord_like.modules.identity.application.dto.LoginRequest;
import com.luishbarros.discord_like.modules.identity.application.dto.RegisterRequest;
import com.luishbarros.discord_like.modules.identity.domain.model.User;
import com.luishbarros.discord_like.modules.identity.domain.model.error.DuplicateEmailError;
import com.luishbarros.discord_like.modules.identity.domain.model.error.InvalidCredentialsError;
import com.luishbarros.discord_like.modules.identity.domain.model.value_object.Email;
import com.luishbarros.discord_like.modules.identity.domain.model.value_object.PasswordHash;
import com.luishbarros.discord_like.modules.identity.domain.model.value_object.Username;
import com.luishbarros.discord_like.modules.identity.domain.ports.PasswordHasher;
import com.luishbarros.discord_like.modules.identity.domain.ports.TokenProvider;
import com.luishbarros.discord_like.modules.identity.domain.ports.TokenBlacklist;
import com.luishbarros.discord_like.modules.identity.domain.ports.repository.UserRepository;
import com.luishbarros.discord_like.shared.ports.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() {
        // Given
        RegisterRequest request = new RegisterRequest(
                "john_doe",
                "john@example.com",
                "password123"
        );

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(false);
        when(passwordHasher.hash(request.password()))
                .thenReturn("hashed_password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });
        when(tokenProvider.generateAccessToken(any(Long.class)))
                .thenReturn("access_token");
        when(tokenProvider.generateRefreshToken(any(Long.class)))
                .thenReturn("refresh_token");

        // When
        var response = authService.register(request);

        // Then
        assertThat(response.accessToken()).isEqualTo("access_token");
        assertThat(response.refreshToken()).isEqualTo("refresh_token");

        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should throw error when registering with duplicate email")
    void shouldThrowErrorWhenRegisteringWithDuplicateEmail() {
        // Given
        RegisterRequest request = new RegisterRequest(
                "john_doe",
                "john@example.com",
                "password123"
        );

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateEmailError.class);

        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfullyWithValidCredentials() {
        // Given
        LoginRequest request = new LoginRequest("john@example.com", "password123");
        User user = new User(
                new Username("john_doe"),
                new Email(request.email()),
                new PasswordHash("hashed_password"),
                Instant.now()
        );
        user.setId(1L);

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));
        when(passwordHasher.verify(request.password(), "hashed_password"))
                .thenReturn(true);
        when(tokenProvider.generateAccessToken(1L))
                .thenReturn("access_token");
        when(tokenProvider.generateRefreshToken(1L))
                .thenReturn("refresh_token");

        // When
        var response = authService.login(request);

        // Then
        assertThat(response.accessToken()).isEqualTo("access_token");
        assertThat(response.refreshToken()).isEqualTo("refresh_token");

        verify(tokenProvider).generateAccessToken(1L);
        verify(tokenProvider).generateRefreshToken(1L);
    }

    @Test
    @DisplayName("Should throw error when logging in with invalid credentials")
    void shouldThrowErrorWhenLoggingInWithInvalidCredentials() {
        // Given
        LoginRequest request = new LoginRequest("john@example.com", "wrong_password");
        User user = new User(
                new Username("john_doe"),
                new Email(request.email()),
                new PasswordHash("hashed_password"),
                Instant.now()
        );
        user.setId(1L);

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));
        when(passwordHasher.verify(request.password(), "hashed_password"))
                .thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsError.class);

        verify(tokenProvider, never()).generateAccessToken(any(Long.class));
    }
}
```

### Repository Adapter Tests

Repository adapter tests verify persistence logic:

```java
package com.luishbarros.discord_like.modules.collaboration.infrastructure.adapter;

import com.luishbarros.discord_like.modules.collaboration.domain.model.Invite;
import com.luishbarros.discord_like.modules.collaboration.domain.model.value_object.InviteCode;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity.InviteJpaEntity;
import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.repository.InviteJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteRepositoryAdapterTest {

    @Mock
    private InviteJpaRepository jpaRepository;

    @InjectMocks
    private InviteRepositoryAdapter adapter;

    @Test
    @DisplayName("Should save invite and return domain object")
    void shouldSaveInviteAndReturnDomainObject() {
        // Given
        Invite invite = new Invite(1L, 1L, new InviteCode("ABC123", 1L), Instant.now());
        InviteJpaEntity jpaEntity = InviteJpaEntity.fromDomain(invite);
        InviteJpaEntity savedEntity = new InviteJpaEntity(
                1L, 1L, 1L, "ABC123", Instant.now(), Instant.now().plusSeconds(86400)
        );
        savedEntity.setId(1L);

        when(jpaRepository.save(any(InviteJpaEntity.class)))
                .thenReturn(savedEntity);

        // When
        Invite result = adapter.save(invite);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRoomId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should find invite by code")
    void shouldFindInviteByCode() {
        // Given
        InviteCode code = new InviteCode("ABC123", 1L);
        InviteJpaEntity jpaEntity = new InviteJpaEntity(
                1L, 1L, 1L, "ABC123", Instant.now(), Instant.now().plusSeconds(86400)
        );

        when(jpaRepository.findByCode("ABC123"))
                .thenReturn(Optional.of(jpaEntity));

        // When
        Optional<Invite> result = adapter.findByCode("ABC123");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getCode().value()).isEqualTo("ABC123");
    }
}
```

## Integration Tests

Integration tests verify component interactions with real dependencies:

```java
package com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.repository;

import com.luishbarros.discord_like.modules.collaboration.infrastructure.persistence.entity.RoomJpaEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RoomJpaRepositoryTest {

    @Autowired
    private RoomJpaRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should save and find room")
    void shouldSaveAndFindRoom() {
        // Given
        RoomJpaEntity entity = new RoomJpaEntity(
                null, "Test Room", 1L, Set.of(1L), Instant.now(), Instant.now()
        );

        // When
        RoomJpaEntity saved = repository.save(entity);
        entityManager.flush();
        entityManager.clear();

        // Then
        RoomJpaEntity found = repository.findById(saved.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Test Room");
        assertThat(found.getOwnerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should find rooms by member ID")
    void shouldFindRoomsByMemberId() {
        // Given
        RoomJpaEntity room1 = new RoomJpaEntity(
                null, "Room 1", 1L, Set.of(1L, 2L), Instant.now(), Instant.now()
        );
        RoomJpaEntity room2 = new RoomJpaEntity(
                null, "Room 2", 2L, Set.of(1L, 3L), Instant.now(), Instant.now()
        );
        repository.save(room1);
        repository.save(room2);
        entityManager.flush();
        entityManager.clear();

        // When
        var rooms = repository.findByMemberId(1L);

        // Then
        assertThat(rooms).hasSize(2);
    }
}
```

## Test Configuration

### Test Properties

```properties
# src/test/resources/application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
```

### Spring Boot Test Annotations

```java
@SpringBootTest  // Full Spring context
@WebMvcTest    // Web layer only
@DataJpaTest   // JPA layer only
@ExtendWith(MockitoExtension.class)  // Mockito integration
```

## Test Best Practices

### 1. Arrange-Act-Assert Pattern

```java
@Test
void shouldDoSomething() {
    // Arrange - Set up test data and mocks
    when(mockRepository.findById(1L))
            .thenReturn(Optional.of(entity));

    // Act - Execute the method under test
    var result = service.getById(1L);

    // Assert - Verify the result
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
}
```

### 2. Descriptive Test Names

```java
// Good - Descriptive
void shouldThrowErrorWhenChangingPasswordOfInactiveUser()
void shouldRegisterUserSuccessfullyWithValidData()

// Bad - Vague
void testChangePassword()
void testRegister()
```

### 3. Test One Thing

```java
// Good - Single assertion
@Test
void shouldThrowErrorForNullValue() {
    assertThatThrownBy(() -> new ValueObject(null))
            .isInstanceOf(InvalidValueError.class);
}

// Bad - Multiple assertions
@Test
void shouldTestEverything() {
    // Test too many things...
}
```

### 4. Use Given-When-Then Comments

```java
@Test
void shouldAuthenticateUserWithValidCredentials() {
    // Given - Set up test data
    User user = new User(/* ... */);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

    // When - Execute the method
    var result = authService.login(request);

    // Then - Verify the result
    assertThat(result).isNotNull();
    verify(tokenProvider).generateAccessToken(user.getId());
}
```

### 5. Mock Only External Dependencies

```java
// Good - Mock repository (external)
@Mock
private UserRepository userRepository;

// Bad - Mock domain objects (should test real behavior)
@Mock
private User user;
```

### 6. Use Test Fixtures

```java
class TestFixtures {
    static User validUser() {
        return new User(
                new Username("john_doe"),
                new Email("john@example.com"),
                new PasswordHash("hashed_password"),
                Instant.now()
        );
    }
}

class UserTest {
    @Test
    void shouldActivateUser() {
        User user = TestFixtures.validUser();
        // Test...
    }
}
```

### 7. Use AssertJ for Readable Assertions

```java
// Good - AssertJ
assertThat(user.getEmail().value()).isEqualTo("john@example.com");
assertThat(user.isActive()).isTrue();
assertThat(user.getMemberIds()).hasSize(3);
assertThat(room.getMemberIds()).contains(1L);

// Bad - JUnit
assertEquals(user.getEmail().value(), "john@example.com");
assertTrue(user.isActive());
```

## Test Coverage

### Current Coverage

| Module | Layer | Test Coverage |
|--------|--------|---------------|
| Identity | Domain | High (entities, value objects) |
| Identity | Application | High (services) |
| Identity | Infrastructure | Medium (adapters) |
| Collaboration | Domain | High (entities, value objects) |
| Collaboration | Application | High (services) |
| Collaboration | Infrastructure | Medium (adapters, WebSocket) |
| Presence | Domain | Medium |
| Presence | Application | Medium |
| Presence | Infrastructure | Low |
| Shared | Middleware | High |

### Coverage Goals

- **Domain Layer**: 90%+ coverage
- **Application Layer**: 80%+ coverage
- **Infrastructure Layer**: 60%+ coverage
- **Overall**: 75%+ coverage

## Running Tests

### Run All Tests

```bash
./gradlew test
```

### Run Specific Test Class

```bash
./gradlew test --tests "com.luishbarros.discord_like.modules.identity.application.service.AuthServiceTest"
```

### Run Specific Test Method

```bash
./gradlew test --tests "com.luishbarros.discord_like.modules.identity.application.service.AuthServiceTest.shouldRegisterUserSuccessfully"
```

### Run Tests with Coverage Report

```bash
./gradlew test jacocoTestReport
```

## Common Testing Scenarios

### 1. Testing Domain Invariants

```java
@Test
void shouldEnforceRoomMemberLimit() {
    Room room = new Room(new RoomName("Test"), 1L, Instant.now());

    // Add max members
    for (long i = 2; i <= 100; i++) {
        room.addMember(i, Instant.now());
    }

    // Should throw when adding beyond limit
    assertThatThrownBy(() -> room.addMember(101L, Instant.now()))
            .isInstanceOf(InvalidRoomError.class);
}
```

### 2. Testing Event Publishing

```java
@Test
void shouldPublishEventWhenCreatingRoom() {
    // Given
    when(roomRepository.save(any(Room.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    roomService.createRoom("Test", 1L, Instant.now());

    // Then
    verify(eventPublisher).publish(any(RoomEvents.class));
}
```

### 3. Testing Error Handling

```java
@Test
void shouldHandleInvalidEmailError() {
    // Given
    String invalidEmail = "not-an-email";

    // When/Then
    assertThatThrownBy(() -> new Email(invalidEmail))
            .isInstanceOf(InvalidUserError.class)
            .hasMessageContaining("Invalid email format");
}
```

### 4. Testing Repository Behavior

```java
@Test
void shouldReturnEmptyOptionalWhenNotFound() {
    // When
    Optional<User> result = userRepository.findById(999L);

    // Then
    assertThat(result).isEmpty();
}
```

## Continuous Integration

Tests run automatically on CI/CD pipeline:

```yaml
# .github/workflows/test.yml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Run tests
        run: ./gradlew test
      - name: Generate coverage report
        run: ./gradlew jacocoTestReport
```

## Summary

Testing in Discord-like:

- **Layered approach** with unit, integration, and E2E tests
- **Mockito** for mocking external dependencies
- **AssertJ** for readable assertions
- **Testcontainers** for integration testing (planned)
- **Clear structure** with tests mirroring source code
- **High coverage goals** for critical business logic

Follow these practices to maintain test quality and ensure code reliability.

## Next Steps

- [Project Structure](./01-project-structure.md) - Learn about code organization
- [Domain Events](../architecture/06-domain-events.md) - Understand event-driven testing
- [Aggregates](../architecture/04-aggregates.md) - Learn aggregate testing patterns
