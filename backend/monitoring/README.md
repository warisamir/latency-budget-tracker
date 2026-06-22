# Monitoring Infrastructure

Complete monitoring stack with Grafana, Prometheus, and Kibana/Elasticsearch.

## Directory Structure

```
monitoring/
├── prometheus.yml                    # Prometheus scrape configuration
└── grafana/
    ├── provisioning/
    │   ├── datasources/
    │   │   └── datasources.yml      # Grafana data sources config
    │   └── dashboards/
    │       └── dashboards.yml       # Dashboard provisioning config
    └── dashboards/
        ├── latency-budget-overview.json   # Main latency dashboard
        ├── jvm-metrics.json              # JVM monitoring dashboard
        ├── api-health.json               # API health & circuit breakers
        └── database-redis.json           # Database & cache monitoring
```

## Quick Start

```bash
# From backend root directory
docker-compose up -d

# Verify services are running
docker-compose ps

# View logs
docker-compose logs -f app
```

## Services

| Service | Port | URL | Credentials |
|---------|------|-----|-------------|
| Grafana | 3000 | http://localhost:3000 | admin / admin123 |
| Kibana | 5601 | http://localhost:5601 | None |
| Prometheus | 9090 | http://localhost:9090 | None |
| Elasticsearch | 9200 | http://localhost:9200 | None |
| Application | 8080 | http://localhost:8080 | N/A |
| PostgreSQL | 5432 | N/A | latencyuser / latencypass123 |
| Redis | 6379 | N/A | None |

## Dashboards

### 1. Latency Budget Overview
**Type:** Metrics (Prometheus)  
**Key Panels:**
- Budget violation rate by stage
- Request latency percentiles (P50, P95, P99)
- Budget compliance gauge
- Alert count by severity

**Default Time Range:** Last 1 hour

### 2. JVM Metrics
**Type:** Metrics (Prometheus)  
**Key Panels:**
- Heap memory usage
- Thread count (live, peak, daemon)
- GC collection rate
- GC pause time

**Alerts to Watch:**
- Heap usage > 80% of max
- GC pause time spikes

### 3. API Health & Circuit Breakers
**Type:** Metrics (Prometheus)  
**Key Panels:**
- HTTP requests by status
- 5xx error rate
- Database circuit breaker status
- Redis circuit breaker status

**Alerts to Watch:**
- 5xx error rate > 1%
- Circuit breaker state = OPEN

### 4. Database & Cache Monitoring
**Type:** Metrics (Prometheus)  
**Key Panels:**
- Database connection pool
- SQL query rate
- Query latency percentiles
- Redis command rate

**Alerts to Watch:**
- Active connections > 80% of max
- Query P95 latency > 500ms

## Logs (Kibana/Elasticsearch)

### Index Pattern
- **Pattern:** `latency-*`
- **Time Field:** `@timestamp`

### Key Fields
- `level` - Log level (INFO, WARN, ERROR, DEBUG)
- `logger` - Logger name
- `message` - Log message
- `traceId` - Request trace ID
- `spanId` - Span ID for correlation
- `thread` - Thread name

### Common Searches

**Find errors:**
```
level:ERROR
```

**Find alerts:**
```
logger:"AlertService" AND message:"Alert"
```

**Find budget violations:**
```
logger:"LatencyBudgetEngine" AND "Budget exceeded"
```

**Trace a request:**
```
traceId:"<trace-id-from-log>"
```

## Prometheus Metrics

### Application Metrics
- `http_server_requests_seconds` - HTTP request duration histogram
- `latency_budget_violations_total` - Total budget violations counter
- `latency_budget_exceeded` - Budget exceeded status gauge

### JVM Metrics
- `jvm_memory_used_bytes` - Memory usage
- `jvm_threads_live_threads` - Active thread count
- `jvm_gc_collections_count_total` - GC collection count
- `jvm_gc_pause_seconds_*` - GC pause time

### Database Metrics (HikariCP)
- `hikaricp_connections_active` - Active connections
- `hikaricp_connections_idle` - Idle connections
- `hikaricp_connections_max` - Max connections

### Resilience4j Metrics
- `resilience4j_circuitbreaker_state` - Circuit breaker state
- `resilience4j_circuitbreaker_calls_total` - Call count

## Configuration

### Prometheus (`prometheus.yml`)
- Scrape interval: 15 seconds
- Evaluation interval: 15 seconds
- App scrape interval: 5 seconds (faster for accuracy)
- Retention: 30 days

### Grafana
- Auto-provisioning enabled
- Dashboards loaded from `dashboards/` directory
- Data sources auto-provisioned
- No sign-up allowed

### Logback (Elasticsearch)
- Development: Console logs only
- Production: Console + File + Elasticsearch
- Index pattern: `latency-${app-name}-${date}`
- Fields: timestamp, level, logger, message, exception, traceId, spanId

## Troubleshooting

### Grafana dashboards showing "No data"
1. Verify Prometheus is running: `docker-compose ps prometheus`
2. Check Prometheus targets: http://localhost:9090/targets
3. Ensure app metrics endpoint is working: `curl http://localhost:8080/actuator/prometheus`
4. Verify datasource is connected: Grafana → Configuration → Data Sources

### Kibana not showing logs
1. Create index pattern: http://localhost:5601 → Stack Management → Index Patterns
2. Verify logs in Elasticsearch: `curl http://localhost:9200/_cat/indices`
3. Check app is sending logs: `docker-compose logs app | grep ELASTICSEARCH`
4. Verify logback configuration in `application.yml`

### High memory usage
- Reduce Elasticsearch heap: `ES_JAVA_OPTS=-Xms256m -Xmx256m` in docker-compose.yml
- Reduce Prometheus retention: Set `--storage.tsdb.retention.time=7d`
- Clean up old indices in Elasticsearch

## Production Setup

For production, consider:

1. **Managed Services:**
   - Grafana Cloud (hosted Grafana + Prometheus)
   - Elasticsearch Cloud (managed Elasticsearch)
   - DataDog, New Relic, or other APM

2. **Self-Hosted:**
   - Run on separate Kubernetes cluster
   - Use persistent volumes for data
   - Set up backups and snapshots
   - Configure authentication (OIDC, LDAP)
   - Set up SSL/TLS certificates

3. **Important Settings:**
   - Restrict Prometheus/Kibana access to internal network only
   - Enable authentication on all services
   - Set up log retention policies
   - Configure backup and disaster recovery

## Monitoring Best Practices

1. **Alert on SLOs, not just errors**
   - Budget violations (primary)
   - Error rate (secondary)
   - Latency percentiles (tertiary)

2. **Retention Policy**
   - Metrics: 30 days (Prometheus)
   - Logs: 7-30 days (Elasticsearch)
   - Archived logs: S3/GCS for long-term

3. **Dashboard Organization**
   - One overview dashboard
   - Deep-dive dashboards per component
   - Team-specific views

4. **Log Correlation**
   - Always log trace IDs
   - Include relevant context (user ID, transaction ID)
   - Use structured logging (JSON format)

## Customization

### Adding New Dashboards
1. Create JSON file in `dashboards/` directory
2. Naming: `{component}-{metric}.json`
3. Auto-provisioned on next container restart

### Adding Custom Metrics
1. Inject `MeterRegistry` in Spring components
2. Create gauges, timers, counters
3. Metrics appear in Prometheus after ~15s
4. Create dashboards to visualize

### Modifying Prometheus Scrapes
1. Edit `prometheus.yml`
2. Restart Prometheus: `docker-compose restart prometheus`
3. Check targets: http://localhost:9090/targets

## References

- [Grafana Dashboards](https://grafana.com/grafana/dashboards/)
- [Prometheus PromQL](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Kibana Dashboards](https://www.elastic.co/guide/en/kibana/current/dashboard.html)
- [Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
