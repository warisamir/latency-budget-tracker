# Latency Budget Tracker

Production-grade observability system that tracks, budgets, and alerts on latency across every stage of a request lifecycle — built to Coinbase infrastructure standards.

---

## Architecture

```
HTTP Request
     │
     ▼
Spring Boot API  ──── OpenTelemetry ────► Jaeger
     │
     ├── Stage 1: Authentication     (budget: 20ms)
     ├── Stage 2: Validation         (budget: 10ms)
     ├── Stage 3: Business Logic     (budget: 50ms)
     ├── Stage 4: Cache Access       (budget: 10ms)  ──► Redis
     ├── Stage 5: Database Query     (budget: 40ms)  ──► PostgreSQL
     └── Stage 6: Response Serialization (budget: 5ms)
          │
          ▼
     Latency Budget Engine (Strategy Pattern)
          │
          ├── BudgetResult (OK / LOW / MEDIUM / HIGH / CRITICAL)
          ├── AlertEvent (Spring Events)
          │    └── AlertEventListener ──► NotificationService (Slack/Email)
          ├── Micrometer Metrics ──────────────────────────► Prometheus ──► Grafana
          └── LatencyRecord (PostgreSQL)
               └── RegressionDetectionService (scheduled, P95/P99 comparison)
```

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| API | Spring Boot 3.2 |
| Language | Java 21 |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Tracing | OpenTelemetry → Jaeger |
| Metrics | Micrometer → Prometheus → Grafana |
| Circuit Breaker | Resilience4j |
| Dashboard | React 18 + TypeScript |
| Deploy | Docker Compose / Kubernetes |

---

## Project Structure

```
latency-budget-tracker/
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/coinbase/latencytracker/
│       │   ├── LatencyTrackerApplication.java        # @EnableScheduling @EnableAsync
│       │   ├── config/
│       │   │   ├── OpenTelemetryConfig.java           # OTel SDK + OTLP exporter
│       │   │   ├── RedisConfig.java                   # Redis template + cache manager
│       │   │   ├── SecurityConfig.java                # Spring Security (Basic Auth)
│       │   │   └── LatencyBudgetProperties.java       # Typed config properties
│       │   ├── entity/
│       │   │   ├── LatencyRecord.java                 # Stage measurement record
│       │   │   ├── Alert.java                         # Alert entity
│       │   │   └── LatencyRegression.java             # Regression detection record
│       │   ├── repository/                            # JPA repositories + native P95/P99 queries
│       │   ├── dto/                                   # Request/Response/Stats DTOs
│       │   ├── budget/
│       │   │   ├── BudgetStrategy.java                # Strategy interface
│       │   │   ├── AbstractBudgetStrategy.java        # Severity classification logic
│       │   │   ├── BudgetResult.java                  # Immutable result record
│       │   │   ├── LatencyBudgetEngine.java            # Engine (Strategy Pattern)
│       │   │   └── strategies/                        # One strategy per stage
│       │   ├── tracing/
│       │   │   └── TracingService.java                # Span creation + TimedResult<T>
│       │   ├── metrics/
│       │   │   └── LatencyMetrics.java                # Micrometer timers/counters/gauges
│       │   ├── alerting/
│       │   │   ├── AlertEvent.java                    # Spring ApplicationEvent
│       │   │   ├── AlertEventPublisher.java
│       │   │   ├── AlertEventListener.java            # @EventListener @Async (deduplication)
│       │   │   └── NotificationService.java           # Slack/Email dispatch
│       │   ├── service/
│       │   │   ├── TransactionService.java            # 6-stage pipeline + circuit breakers
│       │   │   ├── AlertService.java
│       │   │   ├── LatencyStatsService.java           # P50/P95/P99 aggregation
│       │   │   └── RegressionDetectionService.java   # Scheduled P95 regression detector
│       │   ├── controller/                            # REST endpoints
│       │   └── exception/                             # Global error handler
│       └── test/
│           ├── unit/budget/      AbstractBudgetStrategyTest, BudgetResultTest, LatencyBudgetEngineTest
│           ├── unit/alerting/    AlertEventListenerTest
│           ├── unit/service/     TransactionServiceTest, AlertServiceTest
│           └── integration/      TransactionControllerIntegrationTest (full Spring context)
├── frontend/
│   └── src/
│       ├── api/client.ts         # Axios API client
│       ├── types/index.ts        # TypeScript types
│       └── components/Dashboard.tsx  # Live P50/P95/P99 dashboard
├── infra/
│   ├── docker-compose.yml        # All services (app, postgres, redis, jaeger, prometheus, grafana)
│   ├── prometheus/prometheus.yml
│   └── grafana/
│       ├── provisioning/         # Auto-provision datasources + dashboards
│       └── dashboards/latency-overview.json
└── k8s/                          # Kubernetes manifests (add as needed)
```

---

## Running Locally

### 1. Start all infrastructure

```bash
cd infra
docker-compose up -d
```

Services will be available at:
| Service | URL |
|---------|-----|
| Spring Boot API | http://localhost:8080 |
| Jaeger UI | http://localhost:16686 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (admin/admin123) |
| React Dashboard | http://localhost:3000 |

### 2. Run React dashboard

```bash
cd frontend
npm install
npm start
```

### 3. Run tests

```bash
cd backend
mvn test
```

---

## REST API

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/transactions` | ✓ | Run traced transaction, get latency report |
| `GET`  | `/api/v1/latency/stats?window=1h` | ✓ | P50/P95/P99 per stage |
| `GET`  | `/api/v1/alerts` | ✓ | Active alerts (paginated) |
| `GET`  | `/api/v1/alerts/critical` | ✓ | HIGH + CRITICAL alerts |
| `PATCH`| `/api/v1/alerts/{id}/resolve` | ✓ | Resolve an alert |
| `GET`  | `/api/v1/health` | — | Health check |
| `GET`  | `/actuator/prometheus` | — | Prometheus scrape endpoint |

### Example transaction request

```bash
curl -u admin:admin123 -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "fromCurrency": "BTC",
    "toCurrency": "USD",
    "amount": 1.5
  }'
```

---

## Latency Budgets

| Stage | Budget |
|-------|--------|
| Authentication | 20ms |
| Validation | 10ms |
| Business Logic | 50ms |
| Cache Access (Redis) | 10ms |
| Database Query | 40ms |
| Response Serialization | 5ms |
| **Total** | **135ms** |

### Severity Classification

| Deviation | Severity |
|-----------|----------|
| ≤ 0% (within budget) | OK |
| 0–24.9% over | LOW |
| 25–74.9% over | MEDIUM |
| 75–149.9% over | HIGH |
| ≥ 150% over | CRITICAL |

---

## Test Coverage

### Unit Tests
- `AbstractBudgetStrategyTest` — boundary tests for all severity thresholds, zero latency, Long.MAX_VALUE overflow guard
- `BudgetResultTest` — happy/edge/equality tests
- `LatencyBudgetEngineTest` — all stages, evaluateAll, empty map, extreme latency
- `AlertEventListenerTest` — deduplication cooldown, notification failure recovery, severity mapping
- `AlertServiceTest` — happy path, 404 not found, already-resolved idempotency, DB failure propagation
- `TransactionServiceTest` — valid requests, cache hit/miss, 6-stage recording, auth failure, validation failure, same currency pair, negative amount, DB save failure

### Integration Tests
- `TransactionControllerIntegrationTest` — full Spring context (H2 + mocked Redis), validates HTTP status codes, validation error field names, security (401 without auth), alert resolution 404

---

## Key Design Decisions (for interviews)

1. **Strategy Pattern** — each stage has its own `BudgetStrategy` bean, swappable and testable independently
2. **Spring Events** — decouples alert creation from transaction processing; `@Async` prevents blocking
3. **Circuit Breaker** — Resilience4j on Redis and DB; graceful degradation on infrastructure failure
4. **OTel spans** — every stage emits a named span with `latency.ms`, `budget.exceeded`, `deviation_percent` attributes, visible in Jaeger
5. **Micrometer gauges** — live deviation % exposed as Prometheus gauge per stage, enabling Grafana alerting
6. **Native P95/P99** — `PERCENTILE_CONT` PostgreSQL native query, not application-side approximation
7. **Deduplication** — alerts suppressed within configurable cooldown window, prevents alert storms
8. **Regression detection** — compares current P95 against 1h/24h/7d baselines on a scheduled job
