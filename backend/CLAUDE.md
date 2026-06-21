# Latency Budget Tracker - Backend Documentation

## Project Overview

A production-grade Spring Boot application that monitors and tracks latency budgets across request processing stages. Uses strategy pattern for budget evaluation, event-driven alerts, and comprehensive observability.

**Tech Stack:**
- Spring Boot 3.2.5 (Java 17)
- PostgreSQL (production), H2 (dev)
- Redis (caching, session management)
- OpenTelemetry + Micrometer (tracing & metrics)
- Resilience4j (circuit breaker)

---

## Architecture & Design Patterns

### 1. **Strategy Pattern** (Budget Evaluation)
- **Location:** `budget/` package
- **Core:** `BudgetStrategy` interface + `AbstractBudgetStrategy`
- **Implementations:** `AuthenticationBudgetStrategy`, `DatabaseQueryBudgetStrategy`, etc.
- **Orchestrator:** `LatencyBudgetEngine` (collects all strategies, evaluates by stage)
- **Usage:** Register all strategies as `@Component`, engine auto-collects via constructor injection

### 2. **Listener/Observer Pattern** (Event-Driven Alerts)
- **Location:** `alerting/` package
- **Publisher:** `AlertEventPublisher` (fires `AlertEvent`)
- **Listeners:** `AlertEventListener` (consumes events, publishes notifications)
- **Usage:** Decouples alert generation from notification delivery

### 3. **Service Layer Pattern**
- **Location:** `service/` package
- **Responsibility:** Business logic, data transformations, orchestration
- **No dependencies:** Controllers → Services → Repositories
- **Transactions:** Service methods handle `@Transactional` concerns

### 4. **Repository Pattern** (Data Access)
- **Location:** `repository/` package
- **Implementation:** Spring Data JPA with custom queries
- **Queries:** Use `@Query` for complex operations, naming conventions for simple ones

### 5. **DTO Pattern** (Data Transfer)
- **Location:** `dto/` package
- **Usage:** Controllers accept/return DTOs, never raw entities
- **Mapping:** Services handle Entity ↔ DTO transformations

---

## Logging Standards

### Required Annotations
**Every service and controller MUST have:**
```java
@Slf4j
@Service
public class MyService {
    // Implementation
}
```

### Logging Utility
Use `LoggerUtil` (in `util/` package) for consistent contextual logging:

```java
// Debug logs (auto-skipped if level < DEBUG)
LoggerUtil.debug(log, "Processing transaction {}", transactionId);

// Info logs
LoggerUtil.info(log, "Alert resolved - id={}, stage={}", alertId, stage);

// Warnings
LoggerUtil.warn(log, "Budget exceeded - actual={}ms budget={}ms", actual, budget);

// Errors (with exception)
LoggerUtil.error(log, "Failed to process transaction {}", e, transactionId);

// Performance tracking
long start = System.currentTimeMillis();
// ... do work ...
LoggerUtil.logPerformance(log, "operationName", System.currentTimeMillis() - start);
```

### Trace Context
LoggerUtil automatically injects `traceId` and `spanId` into all logs via MDC:
```
2026-06-22T14:30:45.123Z [Thread-1] [abc123def/xyz] INFO MyService - Processing transaction
                                      ^^^^^^^^^^^^^^^^ Auto-injected trace context
```

### Controller Logging Pattern
```java
@RestController
@Slf4j
public class MyController {

    @GetMapping("/{id}")
    public ResponseEntity<Dto> getById(@PathVariable Long id) {
        long start = System.currentTimeMillis();
        LoggerUtil.debug(log, "Fetching resource: {}", id);

        Dto result = service.getById(id);

        LoggerUtil.logPerformance(log, "getById", System.currentTimeMillis() - start);
        return ResponseEntity.ok(result);
    }
}
```

### Service Logging Pattern
```java
@Service
@Slf4j
public class MyService {

    public Dto process(Long id) {
        LoggerUtil.debug(log, "Processing id={}", id);
        try {
            Dto result = repository.findById(id)
                    .orElseThrow(() -> {
                        LoggerUtil.warn(log, "Resource not found: {}", id);
                        return new NotFoundException("Not found: " + id);
                    });

            LoggerUtil.info(log, "Successfully processed: {}", id);
            return result;
        } catch (Exception e) {
            LoggerUtil.error(log, "Error processing id={}", e, id);
            throw e;
        }
    }
}
```

---

## Configuration Management

### Environment-Based Profiles

**Development (default):**
```bash
# Uses: application.yml
# Database: H2 in-memory
# Log Level: DEBUG
# Redis: localhost:6379
```

**Production (Render deployment):**
```bash
# Uses: application-prod.yml
# Database: PostgreSQL (via DB_URL env var)
# Log Level: INFO
# Redis: Via REDIS_HOST env var
```

### Required Environment Variables (Render)

**Database:**
- `DB_URL` - PostgreSQL connection string
- `DB_USER` - Database username
- `DB_PASSWORD` - Database password
- `DB_POOL_SIZE` - Connection pool size (default: 15)
- `DB_DDL_AUTO` - Hibernate DDL mode (default: validate)

**Cache:**
- `REDIS_HOST` - Redis server hostname
- `REDIS_PORT` - Redis port (default: 6379)
- `REDIS_PASSWORD` - Redis password (optional)

**Security:**
- `SECURITY_USER` - Admin username
- `SECURITY_PASSWORD` - Admin password (must be strong in production)

**Observability:**
- `LOG_LEVEL` - Logging level (default: INFO)
- `TRACING_SAMPLE_RATE` - OpenTelemetry sample rate (default: 0.1)
- `OTEL_ENDPOINT` - OpenTelemetry collector endpoint (optional)

**Server:**
- `SERVER_PORT` - HTTP port (default: 8080)

### Activating Profiles

```bash
# Local development
java -jar app.jar  # Uses application.yml

# Production on Render
java -jar app.jar --spring.profiles.active=prod
```

Render automatically sets this via `Procfile`:
```
web: java -jar target/*.jar --spring.profiles.active=prod
```

---

## Code Standards

### Naming Conventions
- **Classes:** PascalCase (MyService, AlertController)
- **Methods:** camelCase (getActiveAlerts, resolveAlert)
- **Constants:** UPPER_SNAKE_CASE (MAX_POOL_SIZE)
- **Variables:** camelCase (actualLatencyMs, transactionId)

### Class Structure
1. Package declaration
2. Imports (grouped: java, javax, org, com)
3. Class annotations (@Service, @Slf4j, etc.)
4. Fields (private, final where possible)
5. Constructor (if needed)
6. Public methods
7. Private utility methods

### Validation & Error Handling
```java
// Validate inputs at service layer
if (actualMs < 0) {
    LoggerUtil.warn(log, "Invalid latency: {}", actualMs);
    throw new IllegalArgumentException("Latency cannot be negative");
}

// Return meaningful error responses
@ExceptionHandler(NotFoundException.class)
public ResponseEntity<ErrorResponse> handle(NotFoundException e) {
    LoggerUtil.warn(log, "Resource not found: {}", e.getMessage());
    return ResponseEntity.status(404)
            .body(new ErrorResponse("NOT_FOUND", e.getMessage()));
}
```

### Dependency Injection
Use constructor injection (via Lombok `@RequiredArgsConstructor`):
```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final MyRepository repository;
    private final OtherService otherService;
    // Lombok generates constructor automatically
}
```

---

## Testing Standards

### Unit Tests
- Location: `src/test/java`
- Naming: `MyServiceTest`, `MyControllerTest`
- Use: Mockito for mocks, JUnit 5
- Coverage target: >80% for services

```java
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private AlertService service;

    @Test
    void testResolveAlert_Success() {
        // Arrange
        Alert alert = new Alert();
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        // Act
        AlertDto result = service.resolveAlert(1L);

        // Assert
        assertTrue(result.resolved());
        verify(alertRepository).save(alert);
    }
}
```

### Integration Tests
- Use: Testcontainers for PostgreSQL, Redis
- Profiles: `application-test.yml`
- Naming: `*IntegrationTest`

```java
@SpringBootTest
@Testcontainers
class AlertServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(...)
            .withDatabaseName("testdb");

    // Tests using real database...
}
```

---

## Deployment Checklist (Render)

- [ ] Dockerfile uses `eclipse-temurin:21-jre-alpine`
- [ ] `pom.xml` includes spring-boot-maven-plugin
- [ ] `application-prod.yml` configured with env vars
- [ ] All environment variables documented in render.yaml/README
- [ ] Logging configuration set to INFO (not DEBUG)
- [ ] Circuit breaker thresholds reviewed for production
- [ ] Database migrations tested (use validate mode, not create)
- [ ] Health check endpoints exposed (/actuator/health)
- [ ] No hardcoded credentials in code (all via env vars)
- [ ] CORS configured if frontend is separate domain
- [ ] Rate limiting configured for public endpoints
- [ ] Error responses don't leak implementation details

---

## Common Patterns

### Adding a New Feature (End-to-End)

1. **Create Entity** (`entity/MyEntity.java`)
2. **Create Repository** (`repository/MyRepository.java`)
3. **Create DTO** (`dto/MyDto.java`)
4. **Create Service** (`service/MyService.java`) - add `@Slf4j`, use LoggerUtil
5. **Create Controller** (`controller/MyController.java`) - add `@Slf4j`, log requests/responses
6. **Write Tests** (`service/MyServiceTest.java`, `*IntegrationTest.java`)
7. **Update Configuration** if needed (`application.yml`)
8. **Document** in this file if new pattern introduced

### Handling Budget Violations

```java
// In LatencyBudgetEngine
BudgetResult result = strategy.evaluate(actualMs);
if (result.isExceeded()) {
    LoggerUtil.warn(log, "Budget exceeded: stage={}, actual={}ms, budget={}ms",
            stage, actualMs, result.getBudgetMs());
    
    alertPublisher.publishAlert(new AlertEvent(
            stage, result.getSeverity(), actualMs, result.getBudgetMs()
    ));
}
```

---

## Troubleshooting

### No data in production
- Check DB_URL, DB_USER, DB_PASSWORD
- Verify database migration ran (ddl-auto=validate requires schema)
- Check logs for connection errors

### Alerts not being sent
- Verify AlertEventListener is listening (check logs for registration)
- Check NotificationService configuration
- Ensure alerting.cooldown-minutes isn't too high

### High latencies in production
- Check circuit breaker status: `/actuator/health/circuitbreakers`
- Review slow query logs (enable with `hibernate.show_sql: true` in test only)
- Check Redis connectivity: `/actuator/health`

### Memory issues
- Adjust JVM heap: `JAVA_OPTS=-Xmx512m`
- Check connection pool: DB_POOL_SIZE shouldn't exceed available connections
- Monitor thread count: `/actuator/metrics/jvm.threads.live`

---

## Quick Reference

| Task | Command | File |
|------|---------|------|
| Run locally | `mvn spring-boot:run` | `pom.xml` |
| Run tests | `mvn test` | `src/test/` |
| Build Docker | `docker build -t tracker .` | `Dockerfile` |
| Check logs | See console output | Logs in `logs/` |
| View metrics | Visit `/actuator/prometheus` | `application.yml` |
| Health check | `/actuator/health` | Built-in |

---

## Contact & Questions

For code review or architectural questions, refer to the memory system or check git history for decision rationale.
