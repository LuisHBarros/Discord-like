# Discord-like Architecture Documentation

This directory contains comprehensive documentation about the architecture and design of the Discord-like application.

## Project Overview

Discord-like is a real-time communication platform built with Spring Boot 4.0.2 and Java 21. The application follows **Hexagonal Architecture (Ports & Adapters)** with **Domain-Driven Design (DDD)** principles, organized into feature-based modules.

### Key Technologies

- **Spring Boot 4.0.2** - Application framework
- **PostgreSQL** - Primary database
- **Redis** - Caching, presence store, rate limiting
- **Kafka** - Domain event bus
- **WebSocket** - Real-time messaging
- **JWT** - Authentication
- **Argon2id** - Password hashing
- **AES-GCM** - Message encryption

### Architecture Highlights

- **Hexagonal Architecture** - Clean separation of concerns
- **Domain-Driven Design** - Rich domain models with business logic
- **Event-Driven** - Asynchronous communication via Kafka
- **Feature-Based Modules** - Identity, Collaboration, Presence bounded contexts
- **Repository Pattern** - Abstract data access
- **Value Objects** - Immutable domain concepts
- **Aggregates** - Transaction boundaries with invariants
- **Domain Events** - Loose coupling between contexts

## Documentation Structure

### Architecture

- [01-overview.md](./architecture/01-overview.md) - High-level architecture, technology stack, deployment view
- [02-hexagonal-architecture.md](./architecture/02-hexagonal-architecture.md) - Hexagonal/Ports & Adapters pattern
- [03-bounded-contexts.md](./architecture/03-bounded-contexts.md) - Bounded contexts and context mapping
- [04-aggregates.md](./architecture/04-aggregates.md) - Domain aggregates and invariants
- [05-value-objects.md](./architecture/05-value-objects.md) - Value objects and immutability
- [06-domain-events.md](./architecture/06-domain-events.md) - Domain events and event sourcing
- [07-repositories.md](./architecture/07-repositories.md) - Repository pattern and data access
- [08-application-services.md](./architecture/08-application-services.md) - Application services and use cases
- [09-infrastructure-adapters.md](./architecture/09-infrastructure-adapters.md) - Infrastructure layer implementation
- [10-design-patterns.md](./architecture/10-design-patterns.md) - Design patterns used

### Development

- [01-project-structure.md](./development/01-project-structure.md) - Project structure guide
- [02-testing.md](./development/02-testing.md) - Testing approach and coverage

### Deployment

- [README.md](./deployment/README.md) - Deployment overview and quick start
- [01-docker.md](./deployment/01-docker.md) - Docker deployment guide
- [03-production.md](./deployment/03-production.md) - Production configuration and security
- [04-monitoring.md](./deployment/04-monitoring.md) - Prometheus and Grafana monitoring setup

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Interfaces Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │  Controllers │  │   WebSocket  │  │  Event List. │              │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘              │
└─────────┼────────────────┼────────────────┼─────────────────────────┘
          │                │                │
┌─────────┼────────────────┼────────────────┼─────────────────────────┐
│         ▼                ▼                ▼                         │
│                    Application Layer                              │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │           Application Services (Use Cases)                   │  │
│  └────────────────────────┬────────────────────────────────────┘  │
└───────────────────────────┼──────────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────────┐
│                           ▼                                       │
│                        Domain Layer                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │   Entities   │  │ Value Objects│  │  Aggregates  │              │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
│  ┌──────────────┐  ┌──────────────┐                           │    │
│  │ Domain Svc.  │  │  Domain Evts │                           │    │
│  └──────────────┘  └──────────────┘                           │    │
└───────────────────────────┼──────────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────────┐
│                           ▼                                       │
│                      Infrastructure Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │   JPA/Hibernate│ │  Kafka/Redis │ │   WebSocket  │              │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
└─────────────────────────────────────────────────────────────────────┘
```

## Module Structure

```
src/main/java/com/luishbarros/discord_like/
├── modules/
│   ├── identity/          # Identity & Authentication bounded context
│   │   ├── application/
│   │   ├── domain/
│   │   └── infrastructure/
│   ├── collaboration/     # Chat & Rooms bounded context
│   │   ├── application/
│   │   ├── domain/
│   │   └── infrastructure/
│   └── presence/          # User presence bounded context
│       ├── application/
│       ├── domain/
│       └── infrastructure/
└── shared/               # Shared kernel
    ├── adapters/        # Infrastructure adapters
    ├── domain/          # Shared domain concepts
    └── ports/           # Cross-cutting ports
```

## Getting Started

For build commands and configuration details, see the project's [CLAUDE.md](../CLAUDE.md).

## Quick Links

- **Architecture**: Start with [Overview](./architecture/01-overview.md)
- **Domain Model**: Learn about [Aggregates](./architecture/04-aggregates.md) and [Value Objects](./architecture/05-value-objects.md)
- **Event System**: Understand [Domain Events](./architecture/06-domain-events.md)
- **Development**: See [Project Structure](./development/01-project-structure.md)
