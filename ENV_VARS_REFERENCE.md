# Environment Variables Reference

Quick lookup for all env vars needed for production deployment.

---

## Render Backend Service

### Database Configuration

```
DB_URL=jdbc:postgresql://your-host.onrender.com:5432/latencydb?sslmode=require
DB_USER=postgres
DB_PASSWORD=<your-password>
DB_POOL_SIZE=15
DB_DDL_AUTO=validate    # Set to 'update' on first deploy, then 'validate'
```

**Note on DB_URL:**
- Render provides: `postgresql://user:pass@host/db`
- Convert to: `jdbc:postgresql://host/db?sslmode=require`

---

### Redis Configuration

```
REDIS_HOST=redis-host.onrender.com
REDIS_PORT=6379
REDIS_PASSWORD=<your-password>
REDIS_SSL=true
```

---

### Security Configuration

```
SECURITY_USER=admin
SECURITY_PASSWORD=<strong-password-8+-chars>
```

**Important:** Change from default `admin123`.

---

### CORS Configuration

```
CORS_ALLOWED_ORIGINS=https://your-app.vercel.app
```

**Note:** This must match your Vercel deployment URL exactly.

For multiple origins (dev + prod):
```
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://your-app.vercel.app
```

---

### Observability Configuration

```
LOG_LEVEL=INFO              # DEBUG for dev, INFO for prod
TRACING_SAMPLE_RATE=0.1     # 10% sampling, 1.0 = 100%
OTEL_ENDPOINT=http://localhost:4317
```

---

### JVM Configuration

```
JAVA_VERSION=21
JAVA_OPTS=-Xmx512m -Xms256m
SERVER_PORT=8080
```

---

## Vercel Frontend Project

### API Configuration

```
VITE_API_URL=https://your-service.onrender.com/api/v1
VITE_API_USER=admin
VITE_API_PASSWORD=<same-as-SECURITY_PASSWORD-on-Render>
```

**How to set in Vercel:**
1. Project Settings → Environment Variables
2. Add each key with its value
3. Redeploy

---

## Local Development (.env.local)

For local testing before pushing:

```
# .env.local (frontend)
VITE_API_URL=http://localhost:8080/api/v1
VITE_API_USER=admin
VITE_API_PASSWORD=admin123
```

Or with Vite proxy (simpler):

```
# .env.local (frontend)
VITE_API_URL=/api/v1    # Proxied to localhost:8080 by vite.config.ts
VITE_API_USER=admin
VITE_API_PASSWORD=admin123
```

Backend defaults (application.yml):
- Database: H2 in-memory
- Redis: localhost:6379
- Security: admin / admin123

---

## Environment Variable Groups (Render Dashboard)

### Database
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `DB_POOL_SIZE`
- `DB_DDL_AUTO`

### Redis
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `REDIS_SSL`

### Security
- `SECURITY_USER`
- `SECURITY_PASSWORD`
- `CORS_ALLOWED_ORIGINS`

### JVM
- `JAVA_VERSION`
- `JAVA_OPTS`
- `SERVER_PORT`

### Observability
- `LOG_LEVEL`
- `TRACING_SAMPLE_RATE`
- `OTEL_ENDPOINT`

---

## Quick Copy-Paste Templates

### Render Service (First Deploy)

```
DB_URL=jdbc:postgresql://[HOST]:5432/latencydb?sslmode=require
DB_USER=postgres
DB_PASSWORD=[PASSWORD]
DB_POOL_SIZE=15
DB_DDL_AUTO=update

REDIS_HOST=[HOST]
REDIS_PORT=6379
REDIS_PASSWORD=[PASSWORD]
REDIS_SSL=true

SECURITY_USER=admin
SECURITY_PASSWORD=[STRONG-PASSWORD]
CORS_ALLOWED_ORIGINS=https://[APP-NAME].vercel.app

LOG_LEVEL=INFO
TRACING_SAMPLE_RATE=0.1
JAVA_VERSION=21
JAVA_OPTS=-Xmx512m -Xms256m
SERVER_PORT=8080
```

### Vercel Project

```
VITE_API_URL=https://[SERVICE-NAME].onrender.com/api/v1
VITE_API_USER=admin
VITE_API_PASSWORD=[STRONG-PASSWORD]
```

---

## Converting Database URL Formats

### From Render PostgreSQL

Render provides:
```
postgresql://postgres:abc123xyz@dpg-abc123.onrender.com/latencydb
```

Convert to Spring Boot JDBC format:
```
jdbc:postgresql://dpg-abc123.onrender.com:5432/latencydb?sslmode=require
```

**Rules:**
- Remove protocol prefix `postgresql://`
- Add `jdbc:postgresql://`
- Extract username:password (not used in URL, set separately as `DB_USER` / `DB_PASSWORD`)
- Add `?sslmode=require` at end
- Default port 5432 can be omitted or explicit

---

## Validation Checklist

Before deploying, verify:

- [ ] `DB_URL` format: starts with `jdbc:postgresql://`
- [ ] `SECURITY_PASSWORD` is different from default `admin123`
- [ ] `CORS_ALLOWED_ORIGINS` matches Vercel URL exactly (HTTPS)
- [ ] `VITE_API_URL` points to Render backend (HTTPS)
- [ ] `VITE_API_PASSWORD` matches `SECURITY_PASSWORD`
- [ ] `REDIS_HOST` and `REDIS_PORT` are correct
- [ ] `DB_DDL_AUTO=update` (first deploy only)
- [ ] `LOG_LEVEL=INFO` (production, not DEBUG)

---

## Common Mistakes

❌ `DB_URL=postgresql://...` (missing `jdbc:`)  
✅ `DB_URL=jdbc:postgresql://...`

❌ `CORS_ALLOWED_ORIGINS=https://your-app.vercel.com` (typo: .com not .app)  
✅ `CORS_ALLOWED_ORIGINS=https://your-app.vercel.app`

❌ `VITE_API_PASSWORD=admin123` (default dev password)  
✅ `VITE_API_PASSWORD=<strong-production-password>`

❌ `SECURITY_PASSWORD=admin123` (default, matches hardcoded default)  
✅ `SECURITY_PASSWORD=<unique-strong-password>`

---

## After First Successful Deploy

1. Login to Render dashboard
2. Go to Environment tab
3. Change `DB_DDL_AUTO` from `update` to `validate`
4. Restart the service

This prevents accidental schema modifications in production.

---

**Reference Date:** 2026-06-23  
**Status:** Production Ready
