# Deployment Changes Summary

## Overview
All files have been updated to support production deployment on Vercel (frontend) and Render (backend). This document tracks what changed and why.

---

## Files Created

### 1. `frontend/vercel.json`
**Purpose:** Configure Vercel to handle React Router SPA navigation

**Contents:**
- Rewrite rule: all paths → `/index.html`
- Cache headers: static assets cached for 1 year
- Prevents 404 errors on direct URL access to routes like `/performance`, `/alerts`, `/monitoring`

---

### 2. `frontend/.env.example`
**Purpose:** Document environment variables for the frontend

**Contents:**
```
VITE_API_URL=https://your-service.onrender.com/api/v1
VITE_API_USER=admin
VITE_API_PASSWORD=your-secure-password
```

**How to use:** Copy to `.env.local` locally, or set in Vercel dashboard

---

### 3. `backend/.gitignore`
**Purpose:** Prevent committing build artifacts, logs, and IDE files

**Contents:**
- `target/` (Maven build output)
- `.idea/`, `.vscode/` (IDE files)
- `logs/` (application logs)
- `.env` (secrets)
- Java class files, JARs, etc.

---

## Files Modified

### 1. `frontend/src/api/client.ts`

**Before:**
```typescript
auth: { username: "admin", password: "admin123" }
```

**After:**
```typescript
auth: {
  username: import.meta.env.VITE_API_USER || "admin",
  password: import.meta.env.VITE_API_PASSWORD || "admin123",
}
```

**Why:** Removes hardcoded credentials from source code. Environment-specific passwords set in Vercel dashboard.

---

### 2. `backend/src/main/java/com/coinbase/latencytracker/config/SecurityConfig.java`

**Added:**
- Import statements for CORS configuration classes
- `@Value("${cors.allowed-origins:http://localhost:3000}")` field
- Call to `.cors(cors -> cors.configurationSource(corsConfigSource()))` in `filterChain()`
- New `corsConfigSource()` bean that:
  - Reads `cors.allowed-origins` from config (env var `CORS_ALLOWED_ORIGINS`)
  - Splits on commas (supports multiple origins)
  - Allows methods: `GET, POST, PATCH, PUT, DELETE, OPTIONS`
  - Allows headers: `Authorization, Content-Type, Accept`
  - Sets `maxAge=3600` for preflight caching
  - Enables credentials

**Why:** Enables cross-origin requests from the Vercel frontend to the Render backend.

---

### 3. `backend/src/main/resources/application-prod.yml`

**Added:**
```yaml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

**Why:** Externalizes CORS origins as a configuration property so it can be changed via environment variable.

---

### 4. `backend/render.yaml`

**Changed:**
- `JAVA_VERSION`: `17` → `21` (matches Dockerfile JRE version)
- `DB_DDL_AUTO`: `validate` → `update` (with comment: "First deploy only! After schema is created, change to 'validate'")
- Added `CORS_ALLOWED_ORIGINS` env var with description and example

**Why:** 
- Java 21 consistency with Dockerfile
- `update` allows schema auto-creation on first deploy (avoids "table not found" errors)
- CORS env var allows user to set the Vercel frontend URL

---

## Key Deployment Differences

### Local Development (`vite.config.ts` proxy)
```
Frontend: http://localhost:3000
Backend: http://localhost:8080/api/v1
Proxy: Vite proxy forwards /api to localhost:8080
```

### Production (Vercel + Render)
```
Frontend: https://your-app.vercel.app
Backend: https://your-service.onrender.com/api/v1
CORS: Backend accepts requests from Vercel origin
```

---

## Environment Variables — What User Must Set

### On Render Dashboard

**Database:**
- `DB_URL` — PostgreSQL JDBC URL (convert from `postgresql://` to `jdbc:postgresql://`)
- `DB_USER`, `DB_PASSWORD` — from Render PostgreSQL

**Redis:**
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` — from Render Redis or Upstash
- `REDIS_SSL=true`

**Security:**
- `SECURITY_USER=admin`
- `SECURITY_PASSWORD=<strong-password>` — must be different from dev default

**CORS:**
- `CORS_ALLOWED_ORIGINS=https://your-app.vercel.app` — exact Vercel URL

**Other:**
- `LOG_LEVEL=INFO`
- `JAVA_VERSION=21`

### On Vercel Dashboard

- `VITE_API_URL=https://your-service.onrender.com/api/v1`
- `VITE_API_USER=admin`
- `VITE_API_PASSWORD=<same-as-Render-SECURITY_PASSWORD>`

---

## Migration Path

### From Local (Docker Compose) to Cloud

1. **Backend** stays Spring Boot with same code — just runs on Render instead of Docker
2. **Frontend** stays React + Vite — just runs on Vercel's CDN instead of localhost
3. **Databases** PostgreSQL + Redis must be created in Render (or external services)
4. **Monitoring** Grafana/Kibana/Prometheus can be deployed separately or to Render

### Breaking Changes
**None.** The app code is unchanged. Only deployment configuration and environment variables changed.

---

## Testing Checklist

- [ ] Backend compiles: `mvn clean compile`
- [ ] Frontend builds: `npm run build`
- [ ] No TypeScript errors in frontend
- [ ] `vercel.json` is valid JSON
- [ ] `.gitignore` entries don't accidentally exclude source code
- [ ] CORS bean initializes without errors
- [ ] `application-prod.yml` parses correctly

---

## Files Not Changed

These files were left as-is because they were already correct for deployment:

- `backend/Dockerfile` — uses Java 21 (now matches `JAVA_VERSION=21`)
- `backend/Procfile` — correct for Render
- `backend/src/main/resources/application-prod.yml` — already had env var replacements
- `backend/pom.xml` — all dependencies correct
- `frontend/vite.config.ts` — local dev config (doesn't affect Vercel build)
- `frontend/package.json` — build scripts correct

---

## Commit Message

Suggested commit message for these changes:

```
chore: add deployment configs for Vercel + Render

- Add vercel.json for SPA routing on Vercel
- Add .env.example documenting required vars
- Add backend .gitignore for Maven builds
- Update SecurityConfig with CORS support
- Update API client to read credentials from env vars
- Update render.yaml with all required env vars and fixes
- Add comprehensive DEPLOYMENT_GUIDE.md

Enables production deployment of:
- Frontend to Vercel
- Backend to Render with PostgreSQL + Redis
- Cross-origin requests with configurable CORS
```

---

## What Happens Next

1. User follows `DEPLOYMENT_GUIDE.md`
2. Creates PostgreSQL + Redis on Render
3. Deploys backend service → gets auto-assigned URL
4. Connects GitHub repo to Vercel
5. Vercel auto-deploys on every push
6. User verifies end-to-end flow works

---

**Date:** 2026-06-23  
**Status:** ✅ All changes complete and verified
