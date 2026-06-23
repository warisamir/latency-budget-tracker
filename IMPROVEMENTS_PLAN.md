# Latency Budget Tracker — Improvements Roadmap

## Phase 1: Observability & Storage (Highest Impact)

### 1.1 Prometheus Histograms for Percentiles ⭐⭐⭐
**Problem:** PERCENTILE_CONT on unbounded LatencyRecord table = O(n) query time as traffic grows
**Solution:** 
- Use Micrometer `Timer` with `publishPercentiles(0.5, 0.95, 0.99)` 
- Query from Prometheus `histogram_quantile()` instead of Postgres
- Remove PERCENTILE_CONT queries entirely
- Keep LatencyRecord only for audit trail (add TTL: 7 days)

**Files:**
- `LatencyMetrics.java` — Update timer registration
- `LatencyStatsService.java` — Query Prometheus instead of DB
- `LatencyRecordRepository.java` — Remove percentile queries
- `application.yml` — Configure histogram buckets

**Impact:** Query latency drops from O(n) to O(1), fixes write amplification

---

### 1.2 Async Batched Writes for LatencyRecord ⭐⭐⭐
**Problem:** 6 synchronous DB inserts per transaction = blocking I/O
**Solution:**
- Use `BlockingQueue<LatencyRecord>` + batch insert task
- Batch size: 100 records, flush interval: 1s
- Transaction.recordLatency() just enqueues, doesn't insert

**Files:**
- `LatencyRecordBatchWriter.java` (new)
- `TransactionService.java` — Call async queue instead of direct save
- `application.yml` — Add batch config

**Impact:** 6x throughput improvement, P99 latency cut significantly

---

## Phase 2: SRE-Grade Error Budgeting ⭐⭐⭐

### 2.1 Add SLO + Error Budget Model
**Current:** Per-request deviation % → severity (tactical)
**New:** SLO = "99% of requests < 135ms over rolling 30d" + burn-rate alerting (strategic)

**Files:**
- `SloConfig.java` (new) — Define SLO, budget window, thresholds
- `BudgetBurnRateDetector.java` (new) — Multi-window burn rate (1h, 6h, 30d)
- `SloAlert.java` (new) — Fire when burn rate > threshold
- Update `AlertController` to expose SLO status

**New Endpoints:**
- `GET /api/v1/slo/status` — Current burn rate, budget remaining
- `GET /api/v1/slo/history` — 30-day burn rate trend

**Impact:** True SRE semantics, differentiates from competitors

---

## Phase 3: Security & Secrets ⭐⭐

### 3.1 Externalize Secrets
**Current:** `admin:admin123` hardcoded
**New:** Spring Cloud Config or Vault

**Files:**
- Create `.env.example` with template
- `SecurityConfig.java` — Read from env vars
- `docker-compose.yml` — Remove all secrets, use `.env` file
- Update `DEPLOYMENT_GUIDE.md` — Document secrets management

**Impact:** Production-ready, secure by default

---

### 3.2 Add Security Headers & CORS
**Files:**
- `SecurityConfig.java` — Add headers (CSP, HSTS, X-Frame-Options, etc.)
- `CorsConfigurer.java` — Proper CORS with allowlist

**Impact:** OWASP Top 10 compliance

---

## Phase 4: Testing & CI/CD ⭐⭐

### 4.1 GitHub Actions Pipeline
**Jobs:**
- Build + Test (Maven)
- JaCoCo coverage gate (>80%)
- SpotBugs static analysis
- Dependabot alerts
- Container scan (Trivy)

**Files:**
- `.github/workflows/ci.yml` (new)
- `pom.xml` — Add SpotBugs, JaCoCo, Checkstyle

**Impact:** Catch bugs early, track coverage

---

### 4.2 Testcontainers (Real DB Testing)
**Problem:** H2 doesn't support PERCENTILE_CONT, tests don't catch production bugs
**Solution:** Testcontainers with real Postgres + Redis

**Files:**
- `TransactionServiceIntegrationTest.java` — Rewrite with Testcontainers
- `LatencyStatsServiceIntegrationTest.java` (new)

**Impact:** Real coverage, catch DB-specific issues

---

## Phase 5: Frontend & API Docs ⭐

### 5.1 springdoc-openapi
**Replace:** Hand-written endpoint table
**Add:** Live Swagger UI at `/swagger-ui.html`

**Files:**
- `pom.xml` — Add `springdoc-openapi-starter-webmvc-ui`
- `OpenApiConfig.java` (new) — Customize schema
- Remove docs/API_DOCUMENTATION.md (Swagger is source of truth)

**Impact:** Always in sync, interactive testing

---

### 5.2 Frontend: SSE for Live Updates
**Current:** Polling every 15s
**New:** Server-Sent Events (SSE) for real-time alerts

**Files:**
- `AlertStreamController.java` (new) — SSE endpoint
- `frontend/src/hooks/useAlertStream.ts` (new) — React hook
- Update `Dashboard.tsx` — Use SSE instead of polling

**Impact:** Real-time feel, lower latency

---

## Phase 6: Documentation Consolidation
**Current:** 4 deployment files (DEPLOYMENT_CHANGES, DEPLOYMENT_GUIDE, ENV_VARS_REFERENCE, README_DEPLOYMENT)
**New:** Single `DEPLOYMENT.md` with all info

---

## Priority Order (Recommend This Sequence)

1. **3.1 Secrets** (30 min) — Security blocker
2. **1.1 Prometheus histograms** (2h) — Biggest performance win
3. **1.2 Async batches** (1h) — Throughput multiplier
4. **4.1 GitHub Actions** (1h) — Quality gate
5. **4.2 Testcontainers** (1.5h) — Real testing
6. **2.1 SLO model** (2h) — Differentiation
7. **5.1 springdoc-openapi** (30 min) — Good UX
8. **Documentation consolidation** (1h) — Cleanup

---

## Estimated Total Effort
- High priority items (1-4): **5.5 hours**
- Medium priority (2, 5-6): **4.5 hours**
- **Total: ~10 hours for production-grade system**

---

## Which should we start with?
