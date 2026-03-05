# System Overview

## Introduction

Discord-like is a real-time communication platform designed to provide instant messaging, room-based collaboration, and user presence tracking. The system is built using **Hexagonal Architecture (Ports & Adapters)** combined with **Domain-Driven Design (DDD)** principles to ensure maintainability, testability, and scalability.

## System Goals

### Primary Goals

1. **Real-time Communication** - Enable instant messaging with WebSocket support
2. **Secure Authentication** - JWT-based authentication with token blacklisting
3. **Room-Based Collaboration** - Organize conversations into rooms with membership management
4. **User Presence** - Track and display user online/offline status
5. **Data Security** - Encrypt messages at rest using AES-GCM
6. **Scalability** - Event-driven architecture with Kafka for loose coupling
7. **High Availability** - Redis for caching and presence tracking

### Non-Functional Requirements

- **Latency**: < 100ms for message delivery
- **Availability**: 99.5% uptime target
- **Security**: OWASP Top 10 compliance
- **Scalability**: Horizontal scaling via stateless services
- **Maintainability**: Clear module boundaries and testability

## Technology Stack

### Core Framework

- **Spring Boot 4.0.2** - Application framework
- **Java 21** - Programming language
- **Spring Data JPA** - ORM and data access
- **Spring Security** - Security framework
- **Spring WebSocket** - Real-time communication
- **Spring Kafka** - Message broker integration

### Data Storage

- **PostgreSQL** - Primary relational database
- **Redis** - Caching, presence store, rate limiting, token blacklist
- **Kafka** - Domain event bus

### Security

- **JJWT 0.12.6** - JWT token generation and validation
- **Argon2id (BouncyCastle 1.80)** - Password hashing
- **AES-GCM** - Message encryption (256-bit key)

### Documentation & Testing

- **SpringDoc OpenAPI 2.8.6** - API documentation
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **AssertJ** - Assertions library

### API Protocol

- **REST** - Primary API (JSON)
- **WebSocket** - Real-time messaging
- **Kafka** - Event streaming

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Web App    │  │  Mobile App  │  │  Desktop App │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
└─────────┼────────────────┼────────────────┼────────────────────┘
          │                │                │
          └────────────────┼────────────────┘
                           │ HTTPS/WSS
┌──────────────────────────┼──────────────────────────────────────┐
│                           ▼                                       │
│                    Load Balancer                                 │
└───────────────────────────┼──────────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────────┐
│                           ▼                                       │
│              Discord-like Application (xN)                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │           Spring Boot Application                          │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │         Bounded Contexts                           │  │  │
│  │  │  ┌──────────┐  ┌──────────┐  ┌──────────┐         │  │  │
│  │  │  │ Identity │  │Collabor. │  │ Presence │         │  │  │
│  │  │  └──────────┘  └──────────┘  └──────────┘         │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
└───────────────────────────┼──────────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────────┐
│                           ▼                                       │
│                     Infrastructure                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ PostgreSQL   │  │    Redis     │  │    Kafka     │          │
│  │   Primary    │  │  (Cache+Presence+RateLimit)  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### Deployment Components

1. **Application Servers** - Stateless Spring Boot instances
2. **Load Balancer** - Distributes requests across instances
3. **PostgreSQL** - Primary data store with read replicas
4. **Redis Cluster** - Distributed caching and presence
5. **Kafka Cluster** - Distributed event streaming
6. **WebSocket Gateway** - Real-time message delivery

## Key Architectural Principles

### 1. Hexagonal Architecture

The application follows the Ports & Adapters pattern:

- **Domain Layer** - Pure business logic, no external dependencies
- **Application Layer** - Use case orchestration
- **Infrastructure Layer** - Technical concerns (DB, messaging, etc.)
- **Interfaces Layer** - HTTP controllers, WebSocket handlers

Dependency flow: `Infrastructure → Ports ← Application ← Domain`

### 2. Domain-Driven Design

- **Bounded Contexts** - Identity, Collaboration, Presence
- **Ubiquitous Language** - Consistent terminology
- **Aggregates** - Transaction boundaries with invariants
- **Value Objects** - Immutable domain concepts
- **Domain Events** - Loose coupling between contexts
- **Repositories** - Collection-like data access

### 3. Event-Driven Architecture

- **Domain Events** - Business facts captured as events
- **Kafka** - Event bus for async communication
- **Event Listeners** - React to domain events
- **Eventual Consistency** - Acceptable across bounded contexts

### 4. Clean Code Practices

- **SOLID Principles** - Single responsibility, open/closed, etc.
- **Dependency Inversion** - Depend on abstractions (ports)
- **Separation of Concerns** - Clear layer boundaries
- **Immutability** - Value objects are immutable
- **Encapsulation** - Invariants protected within aggregates

## Module Organization

### Bounded Contexts

1. **Identity Context** - User registration, authentication, profile management
2. **Collaboration Context** - Rooms, messages, invites, membership
3. **Presence Context** - User online/offline status and activity tracking

### Shared Kernel

- **BaseEntity** - Common entity base class
- **DomainError** - Base class for domain errors
- **Ports** - Cross-cutting interfaces (EventPublisher, Broadcaster, etc.)
- **Adapters** - Shared infrastructure (Redis, Kafka, etc.)

## Data Flow

### REST API Flow

```
Client → Controller → Application Service → Domain Model → Repository → Database
                    ↓                ↓
               Domain Events → Kafka → Event Listeners → WebSocket
```

### WebSocket Flow

```
Client → WebSocket Handler → Application Service → Domain Model → Repository
                          ↓                 ↓
                     Domain Events → Kafka → Event Listeners → WebSocket Clients
```

## Security Architecture

### Authentication Flow

1. User submits credentials (email/password)
2. Password verified using Argon2id hash
3. JWT access token (15 min) + refresh token (7 days) generated
4. Tokens stored in Redis blacklist on logout
5. WebSocket connections authenticated via JWT handshake

### Authorization

- **JWT Filter** - Validates access tokens on REST endpoints
- **WebSocket Interceptor** - Validates tokens on connection
- **Domain-Level Authorization** - Business rules in aggregates/services
- **Rate Limiting** - 5 requests/60s per IP on auth endpoints

### Data Protection

- **Password Hashing** - Argon2id (memory-hard, salted)
- **Message Encryption** - AES-GCM (server-side, random IV per message)
- **Token Security** - Secure random signing keys, blacklisting on logout
- **HTTPS Required** - All endpoints require TLS in production

## Scalability Considerations

### Horizontal Scaling

- Stateless application servers
- Shared Redis for session/presence
- Shared Kafka for events
- Database connection pooling

### Caching Strategy

- Redis for token blacklist
- Redis for presence store
- Potential room-level caching

### Performance Optimization

- Database connection pooling
- WebSocket connection pooling
- Batch event processing
- Cursor-based pagination for messages

## Monitoring & Observability

- **Health Checks** - `/health` endpoint
- **API Documentation** - Swagger UI at `/swagger-ui.html`
- **Logging** - Application and domain events
- **Metrics** - Potential integration with Micrometer/Prometheus

## Next Steps

- [Hexagonal Architecture](./02-hexagonal-architecture.md) - Deep dive into the architecture pattern
- [Bounded Contexts](./03-bounded-contexts.md) - Context boundaries and mapping
- [Aggregates](./04-aggregates.md) - Domain aggregates and invariants
