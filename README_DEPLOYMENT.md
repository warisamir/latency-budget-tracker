# Latency Budget Tracker — Deployment Ready ✅

Your app is now fully configured for production deployment on **Vercel (Frontend)** and **Render (Backend)**.

---

## 📋 Quick Start

### For Local Development
```bash
# Terminal 1: Backend
cd backend
mvn spring-boot:run

# Terminal 2: Frontend  
cd frontend
npm install
npm run start

# Visit http://localhost:3000
```

### For Production Deployment
Follow the **[DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)** — it has step-by-step instructions for:
1. Setting up PostgreSQL + Redis on Render
2. Deploying backend to Render
3. Deploying frontend to Vercel
4. Configuring environment variables
5. Testing end-to-end

---

## 📁 What Changed

All changes are in these files — **no breaking changes to the app logic**:

| File | Change | Why |
|------|--------|-----|
| `frontend/vercel.json` | ✨ Created | SPA routing on Vercel |
| `frontend/.env.example` | ✨ Created | Document env vars |
| `frontend/.env.local.example` | ✨ Created | Local dev env template |
| `frontend/src/api/client.ts` | 🔧 Updated | Use env var credentials |
| `backend/SecurityConfig.java` | 🔧 Updated | Add CORS support |
| `backend/application-prod.yml` | 🔧 Updated | Add CORS config |
| `backend/render.yaml` | 🔧 Updated | Add CORS, fix Java version |
| `backend/.gitignore` | ✨ Created | Prevent committing build files |

**See [DEPLOYMENT_CHANGES.md](./DEPLOYMENT_CHANGES.md) for detailed before/after.**

---

## 🎯 Key Features

✅ **Cross-origin ready** — CORS configured for Vercel ↔ Render  
✅ **Environment-based config** — Credentials from env vars, not hardcoded  
✅ **SPA routing** — React Router works on Vercel's CDN  
✅ **Production-ready** — All 7 charts, 3 pages, Grafana/Kibana integration  
✅ **Database migrations** — Auto-create schema on first deploy  
✅ **Health checks** — Render knows when service is ready  

---

## 🚀 Deployment Paths

### Path 1: Vercel + Render (Recommended)
- **Frontend:** Vercel (free tier includes unlimited deployments)
- **Backend:** Render (free tier sleeps after 15 min inactivity, paid tier always-on)
- **Databases:** Render PostgreSQL + Redis
- **Estimated Cost:** Free → $13/mo for prod

### Path 2: Docker Compose (Development)
```bash
cd backend
docker-compose up -d
# Visit http://localhost:3000 (Grafana)
# Your app at http://localhost:8080
```

### Path 3: AWS/GCP/Azure (Advanced)
The app works on any platform. Use `Dockerfile` + environment variables.

---

## 🔐 Security Notes

1. **Change default credentials:**
   - Local default: `admin` / `admin123`
   - Production: Set `SECURITY_PASSWORD` to a strong password on Render

2. **HTTPS enforced:**
   - Vercel: Free SSL included
   - Render: Free SSL included
   - Databases: SSL connections required by default

3. **Secrets management:**
   - ✅ API credentials in environment variables (not source code)
   - ✅ Database passwords in Render secrets (not `.env` files)
   - ✅ CORS whitelist prevents unauthorized frontend origins

---

## 📊 Included Components

### Frontend Pages
- **Dashboard** — Overview metrics, stage health, alerts
- **Performance** — 7 advanced charts (bar, area, radar, pie, compliance)
- **Alerts** — Alert table with filtering, severity distribution
- **Monitoring** — System health + embedded Grafana/Kibana dashboards

### Backend APIs
- `GET /api/v1/latency/stats?window=1h|24h|7d` — Aggregate stats
- `GET /api/v1/latency/history?window=24h&buckets=24` — Trend data for charts
- `GET /api/v1/alerts` (paginated)
- `GET /api/v1/alerts/critical`
- `PATCH /api/v1/alerts/{id}/resolve`
- `GET /api/v1/health` — System health

### Monitoring (Pre-configured)
- **Grafana** — 4 dashboards (Latency, API Health, JVM, DB/Cache)
- **Kibana** — Log search with trace ID correlation
- **Prometheus** — Metrics scraper
- **Elasticsearch** — Log storage

---

## 📱 Screenshots & Demo

After deployment:
- Frontend: `https://your-app.vercel.app`
- Backend API: `https://your-service.onrender.com/api/v1`
- Grafana: Embedded in `/monitoring` page
- Kibana: Links in `/monitoring` page

---

## 🐛 Troubleshooting

**Q: Frontend shows 404 on `/performance`**  
A: Check `vercel.json` exists. Vercel uses it for SPA routing.

**Q: CORS error in browser console**  
A: Verify `CORS_ALLOWED_ORIGINS` on Render matches your Vercel URL exactly. Restart the backend.

**Q: Backend can't connect to database**  
A: Check `DB_URL` format is `jdbc:postgresql://host/db?sslmode=require`. Give PostgreSQL 5-10 min to initialize.

**Q: Login fails (401 Unauthorized)**  
A: Verify frontend credentials match backend. Set `VITE_API_PASSWORD` on Vercel = `SECURITY_PASSWORD` on Render.

See **[DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md#troubleshooting)** for more.

---

## 📚 Documentation

1. **[DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)** ← Start here for production deployment
2. **[DEPLOYMENT_CHANGES.md](./DEPLOYMENT_CHANGES.md)** ← See what changed and why
3. **[CLAUDE.md](./backend/CLAUDE.md)** ← Backend architecture & patterns
4. **[MONITORING_SETUP.md](./backend/MONITORING_SETUP.md)** ← Grafana/Kibana guide

---

## ✅ Pre-Deployment Checklist

- [ ] Backend compiles: `mvn clean compile` ✅
- [ ] Frontend builds: `npm run build` ✅
- [ ] TypeScript no errors ✅
- [ ] `vercel.json` and `.env.example` present ✅
- [ ] SecurityConfig includes CORS bean ✅
- [ ] `render.yaml` has all env vars ✅
- [ ] `.gitignore` excludes build artifacts ✅
- [ ] GitHub repo synced ✅
- [ ] Render account created ✅
- [ ] Vercel account created ✅

---

## 🎓 Next Steps

1. **Read** [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)
2. **Create** PostgreSQL + Redis on Render
3. **Deploy** backend to Render (takes ~10-15 min first build)
4. **Deploy** frontend to Vercel (takes ~5 min)
5. **Test** end-to-end flow
6. **Configure** Grafana dashboards and alerts
7. **Monitor** in production!

---

## 📞 Support

- **Render Docs:** https://render.com/docs
- **Vercel Docs:** https://vercel.com/docs
- **Spring Boot:** https://spring.io/projects/spring-boot
- **React/Vite:** https://vitejs.dev/guide

---

**Version:** 1.0.0  
**Last Updated:** 2026-06-23  
**Status:** 🚀 Production-Ready

Happy deploying! 🎉
