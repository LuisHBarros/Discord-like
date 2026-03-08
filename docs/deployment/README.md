# Deployment Guide

This guide covers deploying the Discord-like application in different environments.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Environment Setup](#environment-setup)
- [Docker Deployment](#docker-deployment)
- [Production Deployment](#production-deployment)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software

- **Docker** 20.10+ or Docker Desktop 4.0+
- **Docker Compose** 2.0+
- **Java** 21 (JDK) for building
- **Gradle** 8.x for building
- **Git** for cloning the repository

### System Requirements

- **CPU**: 2+ cores recommended
- **RAM**: 4GB minimum, 8GB recommended
- **Disk**: 20GB available space
- **Network**: Stable internet connection

### Environment Variables

The application can be configured via environment variables or `application.yaml`:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `JWT_ACCESS_SECRET` | dev default | JWT access token signing key (≥256-bit) |
| `JWT_REFRESH_SECRET` | dev default | JWT refresh token signing key (≥256-bit) |
| `ENCRYPTION_SECRET` | dev default | AES-GCM encryption key (base64, 32 bytes) |
| `SERVER_PORT` | `8000` | Application server port |

## Quick Start

### Local Development

```bash
# Clone repository
git clone https://github.com/your-org/discord-like.git
cd discord-like

# Start all services
docker-compose up -d

# Verify services are running
docker-compose ps

# Access Swagger UI
open http://localhost:8000/swagger-ui.html
```

### Application Status Check

```bash
# Check application health
curl http://localhost:8000/health

# Expected response:
# {"status":"UP"}
```

## Environment Setup

### Development Environment

For local development, use the provided `docker-compose.yml` which includes:

- **PostgreSQL 16-alpine** - Primary database
- **Redis 7.2-alpine** - Caching and presence store
- **Kafka 7.5.0** - Event bus with Zookeeper
- **Zipkin** - Distributed tracing
- **Prometheus** - Metrics collection

### Production Environment

For production deployment:

1. **Use strong secrets**: Generate secure random keys for JWT and encryption
2. **Configure persistence**: Set up PostgreSQL backups and recovery
3. **Enable SSL/TLS**: Configure HTTPS for external communication
4. **Set up monitoring**: Configure Prometheus and Grafana dashboards
5. **Load balancing**: Use a reverse proxy (nginx, HAProxy) or load balancer
6. **Resource limits**: Configure appropriate CPU and memory limits

## Docker Deployment

### Using Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Service Health Checks

```bash
# PostgreSQL
docker-compose exec -T postgres pg_isready -U postgres

# Redis
docker-compose exec -T redis redis-cli ping

# Kafka
docker-compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

### Volume Persistence

Important data volumes are persisted:

```yaml
volumes:
  postgres_data:    # PostgreSQL data
  redis_data:       # Redis data
  kafka_data:        # Kafka data
  prometheus_data:   # Prometheus metrics
```

**Backup Strategy**:
- Daily backups of `postgres_data`
- Export Redis RDB files regularly
- Configure log aggregation

## Production Deployment

### Security Checklist

- [ ] Generate secure JWT secrets (minimum 256 bits)
- [ ] Generate secure AES encryption key (32 bytes, base64)
- [ ] Configure firewall rules
- [ ] Enable HTTPS/TLS
- [ ] Set up SSL certificates
- [ ] Configure CORS for allowed origins
- [ ] Enable rate limiting
- [ ] Configure authentication middleware
- [ ] Set up audit logging
- [ ] Disable debug mode in production

### Configuration

#### application-production.yaml

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
server:
  port: ${SERVER_PORT:8000}

logging:
  level:
    root: INFO
    com.luishbarros.discord_like: WARN
  file:
    name: /var/log/discord-like/application.log
  pattern: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### Database Configuration

#### PostgreSQL Production Settings

```sql
-- Enable connection pooling
ALTER SYSTEM SET max_connections = 200;
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';

-- Configure WAL (Write-Ahead Logging)
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
```

### Backup Strategy

```bash
# Daily PostgreSQL backup
0 2 * * * /usr/bin/pg_dump -U postgres -d discord_like > /backup/discord-like-$(date +\%Y\%m\%d).sql

# Weekly full backup including schemas
0 3 * * 0 /usr/bin/pg_dumpall -U postgres > /backup/discord-like-full-$(date +\%Y\%m\%d).sql
```

## Monitoring

### Metrics Endpoints

The application exposes metrics at `/actuator/prometheus`:

- **JVM Metrics**: Memory, GC, threads
- **HTTP Metrics**: Request count, response time, error rate
- **Database Metrics**: Connection pool, query time
- **Kafka Metrics**: Producer/consumer lag, throughput

### Key Metrics to Monitor

| Metric | Description | Alert Threshold |
|--------|-------------|----------------|
| `jvm_memory_used_bytes` | JVM heap memory usage | > 80% of max |
| `http_server_requests_seconds_count` | Request rate | Spike detection |
| `http_server_requests_seconds` | Request latency | > 500ms (p95) |
| `hikaricp_connections_active` | DB connections | > 80% of max |
| `kafka_consumer_lag` | Consumer lag | > 1000 messages |

### Log Aggregation

Configure centralized logging:

```yaml
# logback-spring.xml
<appender name="LOKI" class="ch.qos.logback.classic.net.logback.core.ConsoleAppender">
    <encoder class="ch.qos.logback.core.encoder.Encoder">
        <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>

<root level="INFO">
    <appender-ref ref="LOKI" />
</root>
```

### Alerting Rules

Example Prometheus alert rules:

```yaml
groups:
  - name: discord_like_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status="500"}[5m]) > 10
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"

      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_sum[5m])) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "95th percentile latency > 500ms"

      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool nearly exhausted"
```

## Troubleshooting

### Common Issues

#### Docker Compose Won't Start

**Symptom**: Services fail to start or exit immediately

**Solutions**:
1. Check port conflicts: `netstat -an | grep LISTEN`
2. Verify Docker Desktop is running
3. Check disk space: `docker system df`
4. Review logs: `docker-compose logs <service>`

#### Database Connection Failed

**Symptom**: Application cannot connect to PostgreSQL

**Solutions**:
1. Verify PostgreSQL is running: `docker-compose ps postgres`
2. Check credentials in environment variables
3. Test connection manually: `psql -h localhost -U postgres -d discord_like`
4. Check firewall rules

#### Kafka Connection Failed

**Symptom**: Event publishing/consuming not working

**Solutions**:
1. Verify Kafka is running: `docker-compose ps kafka`
2. Check Zookeeper is running: `docker-compose ps zookeeper`
3. Test topics: `docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list`
4. Review Kafka logs: `docker-compose logs kafka`

#### Memory Issues

**Symptom**: Out of memory errors, high GC pressure

**Solutions**:
1. Increase container memory limits in `docker-compose.yml`
2. Tune JVM parameters in Dockerfile
3. Enable heap dump analysis
4. Review and optimize query performance

#### Slow Performance

**Symptom**: High latency, slow response times

**Solutions**:
1. Check database query performance with `EXPLAIN ANALYZE`
2. Verify Redis cache hit ratio
3. Monitor Kafka consumer lag
4. Check network bandwidth
5. Profile application with Java Flight Recorder

### Getting Help

If issues persist:

1. Check application logs: `docker-compose logs -f --tail=100 discord-like`
2. Review [Architecture Documentation](../architecture/README.md)
3. Examine [Test Coverage](../development/02-testing.md)
4. Create issue on GitHub with full stack trace and environment details

### Support Contacts

- **Documentation**: See [README.md](../README.md)
- **Architecture**: [Architecture Guide](../architecture/README.md)
- **Development**: [Project Structure](../development/01-project-structure.md)
- **API Reference**: Access Swagger UI at `/swagger-ui.html`

---

**Next Steps**:

- [Docker Deployment Guide](./01-docker.md) - Detailed Docker setup instructions
- [Production Configuration](./03-production.md) - Production-specific settings
- [Monitoring Setup](./04-monitoring.md) - Prometheus and Grafana configuration
