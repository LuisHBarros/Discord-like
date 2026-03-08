# Docker Deployment Guide

This guide covers deploying the Discord-like application using Docker and Docker Compose.

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Service Configuration](#service-configuration)
- [Building Images](#building-images)
- [Running Services](#running-services)
- [Development Workflow](#development-workflow)
- [Production Considerations](#production-considerations)

## Overview

The application uses Docker Compose to orchestrate the following services:

- **discord-like** - Main application (Spring Boot)
- **postgres** - PostgreSQL 16 database
- **redis** - Redis 7.2 cache
- **kafka** - Apache Kafka 7.5 event bus
- **zookeeper** - Kafka coordination service
- **zipkin** - Distributed tracing
- **prometheus** - Metrics collection

## Quick Start

### Prerequisites

1. Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) or Docker Engine
2. Clone the repository
3. Navigate to project root

```bash
git clone https://github.com/your-org/discord-like.git
cd discord-like
```

### Start All Services

```bash
# Start all services in detached mode
docker-compose up -d

# View service status
docker-compose ps

# View logs
docker-compose logs -f
```

### Verify Deployment

```bash
# Check application health
curl http://localhost:8000/health

# Expected response
{"status":"UP"}

# Access Swagger UI
open http://localhost:8000/swagger-ui.html
```

## Service Configuration

### PostgreSQL

**Image**: `postgres:16-alpine`  
**Port**: `5432` (host) → `5432` (container)  
**Volume**: `postgres_data` - Persistent data storage  
**Health Check**: `pg_isready -U postgres`

**Configuration**:
```yaml
postgres:
  image: postgres:16-alpine
  container_name: discord_like_postgres
  environment:
    POSTGRES_USER: postgres
    POSTGRES_PASSWORD: postgres
    POSTGRES_DB: discord_like
  ports:
    - "5432:5432"
  volumes:
    - postgres_data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U postgres"]
    interval: 5s
    timeout: 5s
    retries: 5
```

### Redis

**Image**: `redis:7.2-alpine`  
**Port**: `6379` (host) → `6379` (container)  
**Volume**: Not persistent (development mode)  
**Health Check**: `redis-cli ping`

**Configuration**:
```yaml
redis:
  image: redis:7.2-alpine
  container_name: discord_like_redis
  ports:
    - "6379:6379"
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 5s
    timeout: 3s
    retries: 5
```

### Kafka & Zookeeper

**Image**: `confluentinc/cp-kafka:7.5.0`  
**Ports**: `9092` (host), `29092` (host)  
**Internal Network**: `PLAINTEXT://discord_like_kafka:29092`

**Configuration**:
```yaml
zookeeper:
  image: confluentinc/cp-zookeeper:7.5.0
  container_name: zookeeper
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181
    ZOOKEEPER_TICK_TIME: 2000
  ports:
    - "2181:2181"

kafka:
  image: confluentinc/cp-kafka:7.5.0
  container_name: discord_like_kafka
  depends_on:
    - zookeeper
  ports:
    - "9092:9092"
    - "29092:29092"
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://discord_like_kafka:29092,PLAINTEXT_HOST://localhost:9092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
    KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

### Prometheus

**Image**: `prom/prometheus:latest`  
**Port**: `9090` (host) → `9090` (container)  
**Config**: `/etc/prometheus/prometheus.yml` from host  
**Metrics Endpoint**: `http://discord-like:8000/actuator/prometheus`

**Configuration**:
```yaml
prometheus:
  image: prom/prometheus:latest
  container_name: discord_like_prometheus
  ports:
    - "9090:9090"
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
```

## Building Images

### Build Application Image

```bash
# Build using Dockerfile
docker build -t discord-like:latest .

# Or use Gradle
./gradlew bootBuildImage
```

### Dockerfile Overview

```dockerfile
# Multi-stage build for optimized image size
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew .
COPY gradle .
COPY settings.gradle .
COPY src src

# Run tests
RUN chmod +x gradlew
RUN ./gradlew build -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Running Services

### Start Individual Services

```bash
# Start only database
docker-compose up -d postgres redis

# Start only application
docker-compose up -d discord-like

# Start with specific service dependency
docker-compose up -d kafka zookeeper
```

### Stop and Restart Services

```bash
# Stop all services
docker-compose stop

# Restart all services
docker-compose restart

# Stop and remove containers
docker-compose down

# Stop and remove containers and volumes
docker-compose down -v
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f discord-like

# Last 100 lines
docker-compose logs --tail=100 discord-like

# Follow with timestamp
docker-compose logs -f --timestamps discord-like
```

## Development Workflow

### Hot Reload

The application uses Spring DevTools for hot reloading:

```bash
# The application automatically restarts on classpath changes
# No manual rebuild required in development mode
```

### Database Migrations

```bash
# Hibernate auto-creates schema in development
# Check DDL in logs
docker-compose logs discord-like | grep -i "ddl-auto"
```

### Debugging

```bash
# Enable debug mode
docker-compose up -d

# Remote debugging with IntelliJ IDEA
# Configure remote JVM debugging on port 5005
# Set JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

## Production Considerations

### Resource Limits

Add resource constraints to `docker-compose.yml`:

```yaml
discord-like:
  deploy:
    resources:
      limits:
        cpus: '2.0'
        memory: 2G
      reservations:
        cpus: '1.0'
        memory: 1G
```

### Health Checks

Configure health check intervals and timeouts:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

### Log Rotation

Prevent excessive log growth:

```yaml
discord-like:
  logging:
    driver: "json-file"
    options:
      max-size: "10m"
      max-file: "3"
```

### Volume Backups

Implement regular backup strategy:

```bash
# Backup PostgreSQL daily
docker-compose exec postgres pg_dump -U postgres -d discord_like > backup.sql

# Copy backup from container
docker cp discord-like_postgres:/backup.sql ./backup-$(date +%Y%m%d).sql
```

### Security Hardening

1. **Use specific versions**: Pin image versions (`postgres:16-alpine`)
2. **Run as non-root**: Add `USER appuser` to Dockerfile
3. **Read-only filesystem**: Mount volumes as read-only where possible
4. **Scan for vulnerabilities**: Run `docker scan discord-like:latest`
5. **Use secrets management**: Use Docker secrets or Kubernetes secrets

## Troubleshooting

### Port Conflicts

If services fail to start due to port conflicts:

```bash
# Check what's using ports
netstat -an | grep LISTEN

# Change ports in docker-compose.yml
# Example: Change 5432 to 5433
ports:
  - "5433:5432"
```

### Connection Issues

If application cannot connect to services:

```bash
# Verify service is running
docker-compose ps

# Test service connectivity
docker-compose exec postgres ping -c 1 localhost

# Check network
docker network inspect discord-like_default
```

### Memory Issues

If containers are being killed due to OOM:

```bash
# Check container memory usage
docker stats discord-like

# Increase memory limits
# Reduce heap size in Dockerfile
# Add JAVA_OPTS environment variable
```

### Container Restart Loops

If containers continuously restart:

```bash
# Check exit code
docker-compose ps

# View logs
docker-compose logs --tail=50 <service>

# Check health check configuration
```

## Performance Tuning

### JVM Options

Optimize JVM for containerized environment:

```yaml
discord-like:
  environment:
    - JAVA_OPTS=-Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### Database Pooling

Configure HikariCP connection pool:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Caching Configuration

Optimize Redis caching:

```yaml
spring:
  cache:
    redis:
      time-to-live: 3600000  # 1 hour
      cache-null-values: false
      use-key-prefix: true
```

## Next Steps

- [Deployment Overview](./README.md) - General deployment guide
- [Production Configuration](./03-production.md) - Production-specific settings
- [Monitoring Setup](./04-monitoring.md) - Prometheus and Grafana configuration
- [Architecture Documentation](../architecture/README.md) - Application architecture
