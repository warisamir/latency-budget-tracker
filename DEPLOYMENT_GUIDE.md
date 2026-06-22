# Deployment Guide — Vercel (Frontend) + Render (Backend)

## Overview

This guide walks you through deploying the Latency Budget Tracker:
- **Frontend** (React + Vite) → **Vercel**
- **Backend** (Spring Boot + PostgreSQL + Redis) → **Render**

---

## Prerequisites

1. GitHub account with the repo pushed
2. Vercel account (free tier works)
3. Render account (free tier works for testing; paid for production)
4. PostgreSQL + Redis instances (Render managed or external)

---

## Step 1: Prepare the Backend (Render)

### 1.1 Create Render Resources

**PostgreSQL Database:**
1. Log in to [Render Dashboard](https://dashboard.render.com)
2. Click **New +** → **PostgreSQL**
3. Set:
   - Name: `latencydb`
   - Database: `latencydb`
   - User: `postgres` (or your choice)
   - Leave region as default
4. Click **Create Database**
5. Copy the **Internal Database URL** (starts with `postgresql://`)

**Redis Cache:**
1. Click **New +** → **Redis** (or use Upstash)
2. Set:
   - Name: `latency-redis`
   - Leave region as default
3. Click **Create Redis**
4. Copy the **Redis URL** and extract: `host`, `port`, `password`

### 1.2 Create Web Service

1. Click **New +** → **Web Service**
2. Connect your GitHub repo
3. Set:
   - **Name:** `latency-budget-tracker`
   - **Environment:** `Docker` (Render will build using the Dockerfile)
   - **Build Command:** `docker build -t myimage .`
   - **Start Command:** `docker run -p 8080:8080 myimage`
   - **Plan:** Free (or paid if needed)

4. Before deploying, add these **Environment Variables** in the dashboard:

   **Database (convert PostgreSQL URL to JDBC format):**
   ```
   DB_URL=jdbc:postgresql://host:port/dbname?sslmode=require
   DB_USER=postgres
   DB_PASSWORD=<your-postgres-password>
   DB_POOL_SIZE=15
   DB_DDL_AUTO=update
   ```

   **Redis:**
   ```
   REDIS_HOST=redis-host.com
   REDIS_PORT=6379
   REDIS_PASSWORD=<redis-password>
   REDIS_SSL=true
   ```

   **Security:**
   ```
   SECURITY_USER=admin
   SECURITY_PASSWORD=<strong-password-here>
   ```

   **CORS (frontend URL):**
   ```
   CORS_ALLOWED_ORIGINS=https://your-app.vercel.app
   ```

   **Observability:**
   ```
   LOG_LEVEL=INFO
   TRACING_SAMPLE_RATE=0.1
   ```

   **JVM:**
   ```
   JAVA_VERSION=21
   JAVA_OPTS=-Xmx512m -Xms256m
   ```

5. Click **Deploy**
6. Wait for the build to complete (~10-15 minutes for the first build)

### 1.3 Verify Backend

Once deployed:
- Open the Render dashboard and find your service URL (e.g., `https://latency-budget-tracker.onrender.com`)
- Hit `https://latency-budget-tracker.onrender.com/api/v1/health`
- You should see: `{"status":"UP"}`

**Note:** After first deploy, **change `DB_DDL_AUTO` to `validate`** in the Environment Variables to prevent accidental schema modifications.

---

## Step 2: Prepare the Frontend (Vercel)

### 2.1 Create Vercel Project

1. Go to [Vercel Dashboard](https://vercel.com/dashboard)
2. Click **Add New** → **Project**
3. Import your GitHub repository
4. Set:
   - **Project Name:** `latency-budget-tracker-ui`
   - **Framework Preset:** `Vite`
   - **Root Directory:** `frontend`
   - **Build Command:** `npm run build`
   - **Output Directory:** `dist`

### 2.2 Add Environment Variables

In the Vercel project settings, add these **Environment Variables**:

```
VITE_API_URL=https://latency-budget-tracker.onrender.com/api/v1
VITE_API_USER=admin
VITE_API_PASSWORD=<same-as-SECURITY_PASSWORD-on-Render>
```

Replace the Render URL with your actual backend URL.

### 2.3 Deploy

Click **Deploy**. Vercel will:
1. Clone the repo
2. Install dependencies
3. Build with Vite
4. Deploy to the global CDN

Once deployed, you'll get a public URL like `https://latency-budget-tracker-ui.vercel.app`.

---

## Step 3: Final Configuration

### 3.1 Update Backend CORS

1. Go to Render dashboard for `latency-budget-tracker`
2. Go to **Environment** settings
3. Edit `CORS_ALLOWED_ORIGINS` → set to your Vercel URL: `https://latency-budget-tracker-ui.vercel.app`
4. Click **Save**
5. The service will auto-restart

### 3.2 Verify End-to-End

1. Open your Vercel app: `https://latency-budget-tracker-ui.vercel.app`
2. Navigate to **Dashboard** or **Performance**
3. Open DevTools (F12) → **Network** tab
4. Refresh the page
5. Check that API calls to `/api/v1/latency/stats` return `200 OK`
6. Try creating a transaction — it should succeed with no CORS errors

---

## Troubleshooting

### Frontend: 404 on Direct URLs

**Symptom:** Navigating to `/performance` or `/alerts` shows a 404.

**Cause:** `vercel.json` missing or not configured.

**Fix:** Ensure `frontend/vercel.json` exists with the SPA rewrite rule.

---

### Frontend: CORS Error in Console

**Symptom:** Browser console shows `No 'Access-Control-Allow-Origin' header`.

**Cause:** Backend CORS not configured or `CORS_ALLOWED_ORIGINS` doesn't match Vercel URL.

**Fix:**
1. Verify `backend/src/main/java/.../config/SecurityConfig.java` has `corsConfigSource()` bean
2. Check Render env var `CORS_ALLOWED_ORIGINS` matches your Vercel URL exactly
3. Restart the Render service

---

### Backend: Database Connection Failed

**Symptom:** Render logs show `SQLException: Connection refused`.

**Cause:** `DB_URL` format is wrong, or PostgreSQL is not ready.

**Fix:**
1. Verify `DB_URL` format: `jdbc:postgresql://host:port/dbname?sslmode=require`
2. Check PostgreSQL is running: in Render dashboard, verify the database status
3. Wait 5-10 minutes after creating database (they need time to initialize)

---

### Backend: DDL Auto Error on First Deploy

**Symptom:** Logs show `Table latency_records not found` or similar.

**Cause:** `DB_DDL_AUTO=validate` but schema doesn't exist yet.

**Fix:** 
1. Set `DB_DDL_AUTO=update` for the first deploy (default in `render.yaml`)
2. After first successful deployment and logs show `HHH000424: Disabling contextual LOB creation`, change it to `validate`

---

### API Credentials Not Working

**Symptom:** 401 Unauthorized when calling API endpoints.

**Cause:** Frontend credentials don't match backend `SECURITY_PASSWORD`.

**Fix:**
1. Verify `VITE_API_USER` and `VITE_API_PASSWORD` in Vercel match Render's `SECURITY_USER` and `SECURITY_PASSWORD`
2. Redeploy both frontend and backend after changing

---

## Environment Variable Reference

### Render (Backend)

| Variable | Example | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://pg.onrender.com/db?sslmode=require` | PostgreSQL JDBC URL |
| `DB_USER` | `postgres` | Database user |
| `DB_PASSWORD` | `***` | Database password |
| `DB_POOL_SIZE` | `15` | HikariCP pool size |
| `DB_DDL_AUTO` | `validate` | Hibernate DDL mode (update on first deploy, validate after) |
| `REDIS_HOST` | `redis.onrender.com` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | `***` | Redis password |
| `REDIS_SSL` | `true` | Enable SSL for Redis |
| `SECURITY_USER` | `admin` | Basic auth username |
| `SECURITY_PASSWORD` | `***` | Basic auth password (must be strong) |
| `CORS_ALLOWED_ORIGINS` | `https://your-app.vercel.app` | Comma-separated frontend URLs |
| `LOG_LEVEL` | `INFO` | Log level (DEBUG for dev, INFO for prod) |
| `TRACING_SAMPLE_RATE` | `0.1` | OpenTelemetry sample rate (0-1) |
| `JAVA_VERSION` | `21` | Java version |
| `JAVA_OPTS` | `-Xmx512m -Xms256m` | JVM options |

### Vercel (Frontend)

| Variable | Example | Description |
|----------|---------|-------------|
| `VITE_API_URL` | `https://your-service.onrender.com/api/v1` | Backend API base URL |
| `VITE_API_USER` | `admin` | API basic auth username |
| `VITE_API_PASSWORD` | `***` | API basic auth password |

---

## Production Checklist

- [ ] PostgreSQL database created and healthy
- [ ] Redis cache created and healthy
- [ ] Backend deployed to Render (service URL noted)
- [ ] Backend env vars set (DB, Redis, Security, CORS)
- [ ] Backend health check passes (`/api/v1/health` returns 200)
- [ ] Frontend deployed to Vercel (app URL noted)
- [ ] Frontend env vars set (API URL, credentials)
- [ ] Frontend app loads (check DevTools for no 404 or CORS errors)
- [ ] End-to-end flow tested (transaction, stats, alerts)
- [ ] `DB_DDL_AUTO` changed from `update` to `validate`
- [ ] SECURITY_PASSWORD changed from default
- [ ] Monitoring dashboards accessible

---

## Support

For issues:
1. Check Render logs: go to the service → **Logs**
2. Check Vercel logs: go to the project → **Deployments** → click latest → **Runtime logs**
3. Open browser DevTools → **Network** and **Console** tabs
4. Check `.env` files are not committed (add to `.gitignore`)

---

## What to Do Next

1. **Set up Grafana & Kibana monitoring** (configured in `backend/docker-compose.yml`, can be deployed separately)
2. **Add alerting rules** in the app (go to `/monitoring` → embedded Grafana dashboards)
3. **Configure log shipping** to Kibana or another log aggregator
4. **Set up SSL certificates** (Vercel auto-manages this; Render provides free SSL)
5. **Implement custom domain** (point DNS to both Vercel and Render)

---

**Last Updated:** 2026-06-23  
**Status:** ✅ Deployment-ready
