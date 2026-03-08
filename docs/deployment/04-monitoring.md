# Monitoring and Observability

This guide covers monitoring the Discord-like application using Prometheus and Grafana.

## Table of Contents

- [Overview](#overview)
- [Prometheus Configuration](#prometheus-configuration)
- [Grafana Setup](#grafana-setup)
- [Key Metrics](#key-metrics)
- [Alerts](#alerts)
- [Dashboards](#dashboards)

## Overview

The Discord-like application exposes metrics via Spring Boot Actuator at `/actuator/prometheus`. Prometheus collects these metrics every 15 seconds.

### Metrics Stack

- **Prometheus** - Metrics collection and storage
- **Grafana** - Visualization and dashboards
- **Spring Boot Actuator** - Application metrics endpoint

### Metrics Architecture

```
┌─────────────┐
│  Discord-like│
│  Application│
└──────┬──────┘
       │
       │  /actuator/prometheus
       │
┌──────▼──────┐
│   Prometheus   │  Scrapes every 15s
└──────┬──────┘
       │
       │
┌──────▼──────┐
│    Grafana   │  Visualizes metrics
└──────────────┘
```

## Prometheus Configuration

### Configuration File

The `prometheus.yml` file configures Prometheus to scrape application metrics:

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    monitor: 'discord-like-monitor'

scrape_configs:
  - job_name: 'discord-like'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['discord-like:8000']
    scrape_interval: 15s
    scrape_timeout: 10s

# Data retention
retention:
  time:
    days: 15d
```

### Targets

Prometheus scrapes metrics from:

| Target | Port | Path | Metrics |
|--------|-------|------|---------|
| discord-like | 8000 | `/actuator/prometheus` | JVM, HTTP, Kafka, DB |

### Data Storage

Prometheus stores metrics for 15 days by default. Adjust retention based on storage capacity:

```yaml
# For production with more storage
retention:
  time:
    days: 30d

# For testing with limited storage
retention:
  time:
    days: 2d
```

## Grafana Setup

### Installation

```bash
# Docker Compose already includes Grafana
# Access Grafana at http://localhost:3000
# Default credentials: admin/admin

# Change password on first login
```

### Prometheus Data Source

Configure Grafana to connect to Prometheus:

1. Navigate to **Configuration → Data Sources**
2. Click **Add data source**
3. Select **Prometheus**
4. Configure:
   - **Name**: `Prometheus`
   - **URL**: `http://prometheus:9090`
   - **Access**: `Server (default)`
   - **Scrape interval**: `15s`

5. Click **Save & Test**

### Importing Dashboards

Import the provided dashboards:

1. Navigate to **Dashboards → Import**
2. Upload dashboard JSON files
3. Select Prometheus data source
4. Click **Import**

## Key Metrics

### JVM Metrics

| Metric | Description | Healthy Range |
|--------|-------------|--------------|
| `jvm_memory_used_bytes` | Heap memory usage | < 80% of max |
| `jvm_memory_max_bytes` | Maximum heap memory | - |
| `jvm_gc_pause_seconds_count` | GC pause count | Minimal |
| `jvm_threads_live_threads` | Active thread count | < 200 |
| `jvm_classes_loaded_classes` | Loaded classes | - |

### HTTP Metrics

| Metric | Description | Healthy Range |
|--------|-------------|--------------|
| `http_server_requests_seconds_count` | Total request count | - |
| `http_server_requests_seconds_sum` | Total request time | - |
| `tomcat_sessions_active_current_sessions` | Active sessions | - |
| `http_server_requests_seconds{exception="none"}` | Successful requests | High % |
| `http_server_requests_seconds{status="500"}` | Error requests | Low % |

### Database Metrics

| Metric | Description | Healthy Range |
|--------|-------------|--------------|
| `hikaricp_connections_active` | Active DB connections | < 80% of max |
| `hikaricp_connections_max` | Maximum connections | - |
| `hikaricp_connections_min` | Minimum connections | - |
| `hikaricp_connections_pending` | Pending connections | 0 |
| `hikaricp_connections_idle` | Idle connections | < 50% |

### Kafka Metrics

| Metric | Description | Healthy Range |
|--------|-------------|--------------|
| `kafka_producer_record_send_total` | Total messages sent | - |
| `kafka_producer_record_send_rate` | Send rate | - |
| `kafka_consumer_records_consumed_total` | Total messages consumed | - |
| `kafka_consumer_records_lag_max` | Consumer lag | < 1000 |

### Cache Metrics

| Metric | Description | Healthy Range |
|--------|-------------|--------------|
| `cache_gets_total` | Total cache gets | - |
| `cache_hits_total` | Total cache hits | High % |
| `cache_misses_total` | Total cache misses | Low % |
| `cache_put_total` | Total cache puts | - |

### Process Metrics

| Metric | Description | Healthy Range |
|--------|-------------|--------------|
| `process_cpu_usage` | CPU usage | < 80% |
| `process_start_time_seconds` | Process uptime | - |
| `system_cpu_count` | Available CPU cores | - |
| `jvm_memory_committed_bytes` | Committed memory | < 90% of max |

## Alerts

### Prometheus Alert Rules

Configure alerts in `prometheus.yml`:

```yaml
# Add to prometheus.yml
rule_files:
  - '/etc/prometheus/alerts.yml'

groups:
  - name: discord_like_alerts
    rules:
      # Application Health
      - alert: ApplicationDown
        expr: up{job="discord-like"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Application is down"
          description: "Discord-like has been down for more than 1 minute"

      # High Error Rate
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status="500"}[5m]) > 10
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "More than 10 errors per second for the last 5 minutes"

      # High Latency
      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_sum[5m])) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High latency detected"
          description: "95th percentile latency > 500ms for the last 5 minutes"

      # Memory Pressure
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage"
          description: "JVM heap usage > 85%"

      # Database Connection Issues
      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "More than 90% of connections in use"

      # Consumer Lag
      - alert: KafkaConsumerLag
        expr: kafka_consumer_records_lag_max > 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Kafka consumer lag detected"
          description: "Consumer is more than 1000 messages behind"

      # Thread Count
      - alert: HighThreadCount
        expr: jvm_threads_live_threads > 200
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High thread count"
          description: "More than 200 threads running"
```

### Alert Notifications

Configure alert delivery:

**Email Alerts**:
```yaml
# In alerts.yml
receivers:
  - name: 'email-receiver'
    email_configs:
      - to: 'alerts@example.com'
        from: 'prometheus@example.com'
        smarthost: 'smtp.gmail.com'
        auth_username: 'alerts@example.com'
        auth_password: 'password'
```

**Slack/Webhook Alerts**:
```yaml
receivers:
  - name: 'slack-receiver'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
        channel: '#alerts'
```

## Dashboards

### Dashboard 1: Application Overview

**File**: `dashboards/application-overview.json`

**Panels**:
1. Application Status (UP/DOWN)
2. Request Rate (req/sec)
3. Response Time (ms)
4. Error Rate (%)
5. Active Sessions
6. JVM Memory (used/max)
7. JVM Threads
8. CPU Usage (%)

**Queries**:
```promql
# Application Status
up{job="discord-like"}

# Request Rate
rate(http_server_requests_seconds_count[5m])

# Response Time (p95)
histogram_quantile(0.95, rate(http_server_requests_seconds_sum[5m]))

# Error Rate
rate(http_server_requests_seconds_count{status="500"}[5m]) / rate(http_server_requests_seconds_count[5m]) * 100

# JVM Memory
jvm_memory_used_bytes / jvm_memory_max_bytes * 100

# JVM Threads
jvm_threads_live_threads

# CPU Usage
process_cpu_usage * 100
```

### Dashboard 2: Database Performance

**File**: `dashboards/database-performance.json`

**Panels**:
1. Active Connections
2. Idle Connections
3. Connection Usage (%)
4. Query Time (avg)
5. Connection Time (avg)
6. Slow Queries (count)

**Queries**:
```promql
# Active Connections
hikaricp_connections_active

# Idle Connections
hikaricp_connections_idle

# Connection Usage
hikaricp_connections_active / hikaricp_connections_max * 100

# Query Time (from PostgreSQL logs)
increase(postgresql_stat_statements_exec_time_seconds_sum[5m])

# Connection Time
increase(postgresql_stat_statements_execution_time_seconds_sum[5m])

# Slow Queries (queries > 1s)
rate(postgresql_stat_statements_exec_time_seconds_bucket{le="1.0"}[5m]) * 60
```

### Dashboard 3: JVM Performance

**File**: `dashboards/jvm-performance.json`

**Panels**:
1. Heap Memory (used/max)
2. Non-Heap Memory
3. GC Count
4. GC Time (pause seconds)
5. Class Loading
6. Thread Count
7. CPU Load

**Queries**:
```promql
# Heap Memory
jvm_memory_used_bytes{area="heap"}
jvm_memory_max_bytes{area="heap"}

# GC Pauses
rate(jvm_gc_pause_seconds_count[5m])

# GC Time
jvm_gc_pause_seconds_sum

# Loaded Classes
jvm_classes_loaded_classes

# Threads
jvm_threads_peak_threads
jvm_threads_live_threads
jvm_threads_daemon_threads
```

### Dashboard 4: Kafka Performance

**File**: `dashboards/kafka-performance.json`

**Panels**:
1. Messages Sent (rate)
2. Messages Consumed (rate)
3. Consumer Lag (max)
4. Producer Record Send Rate
5. Consumer Records Consumed
6. Request/Response Error Rate

**Queries**:
```promql
# Messages Sent Rate
rate(kafka_producer_record_send_total[5m])

# Messages Consumed Rate
rate(kafka_consumer_records_consumed_total[5m])

# Consumer Lag
kafka_consumer_records_lag_max

# Producer Error Rate
rate(kafka_producer_record_send_total{error!=""}[5m])

# Consumer Error Rate
rate(kafka_consumer_records_consumed_total{error!=""}[5m])
```

### Dashboard 5: Cache Performance

**File**: `dashboards/cache-performance.json`

**Panels**:
1. Cache Hits (rate)
2. Cache Misses (rate)
3. Hit Rate (%)
4. Evictions (rate)
5. Gets (rate)
6. Puts (rate)

**Queries**:
```promql
# Cache Hit Rate
rate(cache_hits_total[5m])

# Cache Miss Rate
rate(cache_misses_total[5m])

# Hit Rate
cache_hits_total / (cache_hits_total + cache_misses_total) * 100

# Eviction Rate
rate(cache_evictions_total[5m])

# Get Rate
rate(cache_gets_total[5m])

# Put Rate
rate(cache_put_total[5m])
```

## Log Aggregation

### Application Logs

Configure structured logging for log aggregation:

```yaml
# application.yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  logback:
    rollingpolicy: time
    max-size: 100MB
    max-history: 30
```

### Log Formats

**JSON Logs** (recommended for parsing):
```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "level": "INFO",
  "logger": "com.luishbarros.discord_like",
  "message": "User logged in",
  "thread": "http-nio-8080-exec-1",
  "context": "auth"
}
```

**Access Logs**:
```
127.0.0.1 - - [08/Jan/2024:12:00:00 +0000] "GET /users/me HTTP/1.1" 200 45
```

## Performance Tuning

### JVM Optimization

Adjust JVM parameters for containerized environment:

```yaml
# In docker-compose.yml
environment:
  - JAVA_OPTS=-Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/
```

### Database Connection Pooling

Optimize HikariCP settings:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

### Caching Strategy

Optimize cache configuration:

```yaml
spring:
  cache:
    redis:
      time-to-live: 3600000
      cache-null-values: false
      use-key-prefix: true
      key-prefix: "discord-like:"
```

### Prometheus Optimization

Adjust scraping for production:

```yaml
# prometheus.yml
global:
  scrape_interval: 30s
  evaluation_interval: 30s
  external_labels:
    environment: 'production'

scrape_configs:
  - job_name: 'discord-like'
    sample_limit: 10000
    metrics_path: '/actuator/prometheus/prometheus'
    static_configs:
      - targets: ['discord-like:8000']
```

## Next Steps

- [Deployment Guide](./README.md) - General deployment guide
- [Docker Setup](./01-docker.md) - Docker deployment instructions
- [Production Configuration](./03-production.md) - Production-specific settings
- [Architecture Documentation](../../architecture/README.md) - Application architecture
