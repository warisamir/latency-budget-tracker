# Monitoring Setup Guide - Grafana & Kibana

This guide covers setting up comprehensive monitoring with Grafana (metrics) and Kibana (logs) using Docker Compose.

## Quick Start

```bash
# Start all monitoring services
docker-compose up -d

# Wait for services to be ready (30-60 seconds)
docker-compose ps

# Access services
# Grafana:       http://localhost:3000  (admin/admin123)
# Kibana:        http://localhost:5601
# Prometheus:    http://localhost:9090
# Elasticsearch: http://localhost:9200
# App:           http://localhost:8080
```

## 📊 Grafana Setup (Metrics Visualization)

### Accessing Grafana

1. Open [http://localhost:3000](http://localhost:3000)
2. Login: **admin** / **admin123**
3. Go to **Configuration** → **Data Sources** (should auto-populate with Prometheus)

### Available Dashboards

Three pre-configured dashboards are automatically provisioned:

#### 1. **Latency Budget Overview** (Main Dashboard)
- Budget violation rate by stage
- Request latency percentiles (P50, P95, P99)
- Budget compliance status
- Alert count by severity
- Request rate by endpoint

**Key Metrics Tracked:**
- `latency_budget_violations_total` - Total budget violations
- `http_server_requests_seconds` - Request latency histogram
- `latency_budget_exceeded` - Budget exceeded status

**Use Case:** Monitor overall application health and latency compliance

#### 2. **JVM Metrics**
- Heap memory usage (used vs max)
- Thread count (live, peak, daemon)
- Garbage collection rate
- GC pause time

**Key Metrics Tracked:**
- `jvm_memory_used_bytes` - Current memory usage
- `jvm_threads_live_threads` - Active thread count
- `jvm_gc_collections_count_total` - GC collections

**Use Case:** Monitor JVM health and identify memory/GC issues

#### 3. **API Health & Circuit Breakers**
- HTTP requests by status code
- 5xx error rate
- Database circuit breaker status
- Redis circuit breaker status
- Circuit breaker call count

**Key Metrics Tracked:**
- `http_server_requests_seconds_count` - Request count
- `resilience4j_circuitbreaker_state` - Circuit breaker state
- `resilience4j_circuitbreaker_calls_total` - CB call count

**Use Case:** Monitor API availability and resilience

### Creating Custom Dashboards

1. Click **+** → **Dashboard**
2. Add panels by clicking **Add new panel**
3. Select **Prometheus** as data source
4. Enter PromQL query (examples below)
5. Configure visualization type
6. Save dashboard

### Useful PromQL Queries

**Request Latency:**
```
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

**Error Rate:**
```
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])
```

**Database Connection Pool Usage:**
```
hikaricp_connections_active / hikaricp_connections_max
```

**Redis Commands/sec:**
```
rate(redis_commands_processed_total[5m])
```

**Budget Violations by Stage:**
```
increase(latency_budget_violations_total[1h]) by (stage, severity)
```

---

## 🔍 Kibana Setup (Log Analysis)

### Accessing Kibana

1. Open [http://localhost:5601](http://localhost:5601)
2. Create index pattern (first access)

### Creating Index Pattern

1. Go to **Stack Management** → **Index Patterns**
2. Click **Create Index Pattern**
3. Index pattern name: `latency-*`
4. Time field: `@timestamp`
5. Click **Create Index Pattern**

### Exploring Logs

#### Discover
1. Go to **Discover**
2. Select `latency-*` index pattern
3. View logs in real-time
4. Filter by:
   - Level (WARN, ERROR, etc.)
   - Logger name
   - Trace ID
   - Thread

#### Creating Saved Searches

**Find all warnings:**
```
level:WARN
```

**Find all errors in AlertService:**
```
level:ERROR AND logger:"AlertService"
```

**Find requests with specific trace ID:**
```
traceId:"abc-123-xyz"
```

**Find budget violations:**
```
message:"Budget exceeded"
```

**Find slow operations (>500ms):**
```
duration:>500
```

#### Creating Dashboards

1. Go to **Dashboards** → **Create Dashboard**
2. Add visualizations:
   - Error count over time
   - Warning logs by logger
   - Request duration histogram
   - Trace correlation analysis

### Useful Filters

**By Severity:**
- `level:ERROR` - Errors only
- `level:WARN` - Warnings only
- `level:INFO` - Info logs

**By Component:**
- `logger:AlertService`
- `logger:LatencyBudgetEngine`
- `logger:TransactionService`

**By Activity:**
- `message:"Budget exceeded"`
- `message:"Alert resolved"`
- `message:"Circuit breaker"`

### Creating Alerts in Kibana

1. Go to **Discover** or **Dashboards**
2. Click **Create Alert**
3. Set condition (e.g., `level:ERROR` AND count > 5 in last 15 min)
4. Set notification (email, Slack, webhook)

---

## 🔗 Log Correlation

### Trace ID Propagation

Every request includes a `traceId` and `spanId` in logs:

```
2026-06-22T14:30:45.123Z [http-nio-8080-exec-1] [abc123-xyz789] INFO AlertService - Alert resolved
```

**To follow a request:**
1. Note the `traceId` from log
2. In Kibana, search: `traceId:"abc123-xyz789"`
3. See all logs for that transaction

### Example: Tracking a Budget Violation

1. **Prometheus Alert** → Budget exceeded at 14:30:45
2. **Grafana Dashboard** → See violation spike
3. **Kibana** → Search for budget violation logs at that time
4. **Find Trace ID** → Follow transaction lifecycle
5. **Analyze** → Each service's processing time

---

## 📈 Prometheus Scrape Configuration

The `prometheus.yml` is pre-configured to scrape:

- **App metrics** (http://app:8080/actuator/prometheus)
- **Prometheus self** (http://prometheus:9090/metrics)
- **Optional:** Redis, PostgreSQL exporters

### Adding Custom Metrics

In your Spring Boot app, use Micrometer:

```java
@Component
class CustomMetrics {
    private final MeterRegistry meterRegistry;

    public CustomMetrics(MeterRegistry registry) {
        this.meterRegistry = registry;
    }

    public void recordLatencyStage(String stage, long durationMs) {
        meterRegistry.timer("latency.stage", "stage", stage)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

Query in Grafana:
```
histogram_quantile(0.95, rate(latency_stage_seconds_bucket[5m]))
```

---

## 🔧 Troubleshooting

### Elasticsearch Connection Failed
```bash
# Check Elasticsearch health
curl http://localhost:9200/_cluster/health

# Expected output:
# {"cluster_name":"...","status":"green","number_of_nodes":1...}
```

### No logs appearing in Kibana
1. Check index pattern exists: Stack Management → Index Patterns
2. Verify logs are being sent: `curl http://localhost:9200/_cat/indices`
3. Check app logs: `docker-compose logs app | grep "ELASTICSEARCH"`
4. Re-index if needed: In Kibana Stack Management

### Prometheus not scraping app
1. Check Prometheus targets: http://localhost:9090/targets
2. Look for `latency-app` job
3. If "DOWN", verify app is running: `docker-compose ps app`
4. Check connectivity: `docker-compose exec prometheus curl http://app:8080/actuator/prometheus`

### Grafana dashboards not loading
1. Check data sources configured: Configuration → Data Sources
2. Test Prometheus connection
3. Refresh dashboard (Ctrl+R)
4. Check Prometheus for metrics: http://localhost:9090/graph

---

## 📊 Dashboard Best Practices

### Latency Dashboard
- Set default time range to "Last 1 hour"
- Enable auto-refresh every 30 seconds
- Add threshold lines for budget limits
- Use colors to indicate violations

### Performance Dashboard
- Monitor memory usage trends
- Watch for GC pause spikes
- Track thread count increases
- Alert on heap approaching max

### Operational Dashboard
- Track error rate threshold
- Monitor circuit breaker status
- Watch critical service latencies
- Alert on budget violations

---

## 🚀 Production Deployment

### For Render

1. **Metrics:** Keep Prometheus in private VPC
2. **Logs:** Use managed Elasticsearch or third-party (e.g., LogStash Cloud)
3. **Access:** Use VPN or private endpoint for Grafana/Kibana
4. **Retention:** Set Elasticsearch retention to 7-30 days
5. **Alerts:** Configure PagerDuty, Slack webhooks

### Environment Variables

```bash
# For Elasticsearch (if external)
ELASTICSEARCH_HOST=es.example.com
ELASTICSEARCH_PORT=9200
ELASTICSEARCH_USER=elastic
ELASTICSEARCH_PASSWORD=secret

# For Prometheus scrape config
PROMETHEUS_RETENTION_DAYS=30
```

### Docker Compose Alternatives

**For Production:**
- Use managed services (Elasticsearch Cloud, Grafana Cloud)
- Or Kubernetes deployments
- Or AWS CloudWatch, DataDog, New Relic

---

## 📚 Additional Resources

- [Grafana Documentation](https://grafana.com/docs/grafana/latest/)
- [Kibana Documentation](https://www.elastic.co/guide/en/kibana/current/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Elasticsearch Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/)

---

## 🎯 Monitoring Checklist

### Daily
- [ ] Check error rate in Kibana
- [ ] Review latency percentiles in Grafana
- [ ] Verify circuit breakers are CLOSED
- [ ] Check JVM memory trends

### Weekly
- [ ] Review budget violation trends
- [ ] Check for recurring error patterns
- [ ] Monitor storage usage (logs, metrics)
- [ ] Update alert thresholds if needed

### Monthly
- [ ] Archive old logs
- [ ] Review and optimize metrics
- [ ] Update dashboards with new queries
- [ ] Capacity planning for growth

---

## Support

For issues:
1. Check service health: `docker-compose ps`
2. View logs: `docker-compose logs -f [service]`
3. Verify connectivity: `docker-compose exec [service] curl [endpoint]`
4. Check firewall rules for port access

**Port Reference:**
- 3000 - Grafana
- 5601 - Kibana
- 9090 - Prometheus
- 9200 - Elasticsearch
- 8080 - Application
- 5432 - PostgreSQL
- 6379 - Redis
