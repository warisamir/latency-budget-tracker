# Render Deployment Guide

This guide covers deploying the Latency Budget Tracker backend to Render.com.

## Prerequisites

- Render.com account
- GitHub repository with this code
- PostgreSQL database (Render can provision)
- Redis instance (Render can provision or use third-party)

## Step-by-Step Deployment

### 1. Create PostgreSQL Database on Render

1. Go to **Render Dashboard** → **Databases** → **New Database**
2. Select **PostgreSQL**
3. Configure:
   - **Database Name:** `latencydb`
   - **Region:** Select closest to your users
   - **Instance Type:** Starter (for development) or higher for production
4. Click **Create Database**
5. Copy the connection details (Internal Database URL)

### 2. Create Redis Instance

**Option A: Render Redis (Recommended)**
1. Go to **Render Dashboard** → **Redis** → **New Redis**
2. Configure:
   - **Name:** `latency-tracker-redis`
   - **Region:** Same as database
   - **Instance Type:** Starter
3. Click **Create Redis**
4. Copy the connection string

**Option B: External Redis (e.g., Redis Cloud)**
- Get your Redis URL from provider
- Format: `redis://:[password]@[host]:[port]`

### 3. Create Web Service on Render

1. Go to **Render Dashboard** → **New** → **Web Service**
2. Connect your GitHub repository
3. Configure:
   - **Name:** `latency-budget-tracker`
   - **Environment:** `Java`
   - **Region:** Same as database/Redis
   - **Build Command:** `mvn clean package -DskipTests`
   - **Start Command:** `java -Dspring.profiles.active=prod -jar target/*.jar`
   - **Instance Type:** Starter or higher

### 4. Set Environment Variables

In the **Environment Variables** section, add:

#### Database Configuration
```
DB_URL=postgres://[user]:[password]@[host]:[port]/latencydb
DB_USER=[postgres_username]
DB_PASSWORD=[postgres_password]
DB_POOL_SIZE=15
DB_DDL_AUTO=validate
```

#### Redis Configuration
```
REDIS_HOST=[redis_host]
REDIS_PORT=[redis_port]
REDIS_PASSWORD=[redis_password_if_required]
REDIS_SSL=true
```

#### Security
```
SECURITY_USER=admin
SECURITY_PASSWORD=[STRONG_PASSWORD_HERE]
```

#### Application Configuration
```
LOG_LEVEL=INFO
TRACING_SAMPLE_RATE=0.1
SERVER_PORT=8080
JAVA_OPTS=-Xmx512m -Xms256m
```

#### Optional Observability
```
OTEL_ENDPOINT=http://localhost:4317
OTEL_TRACES_EXPORTER=none
OTEL_METRICS_EXPORTER=none
OTEL_LOGS_EXPORTER=none
```

### 5. Health Check Configuration

Set up the health check endpoint:
- **Health Check Path:** `/actuator/health/ready`
- **Health Check Interval:** `30` seconds
- **Health Check Timeout:** `10` seconds

### 6. Deploy

1. Click **Create Web Service**
2. Render will automatically build and deploy
3. Monitor the deployment in the **Logs** section
4. Once deployed, you'll get a URL: `https://latency-budget-tracker-xxxx.onrender.com`

## Verifying Deployment

### Check Health
```bash
curl https://latency-budget-tracker-xxxx.onrender.com/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "database": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

### Check Metrics
```bash
curl https://latency-budget-tracker-xxxx.onrender.com/actuator/prometheus
```

### View Logs
1. Go to **Render Dashboard** → Your Service → **Logs**
2. Filter by log level (INFO, WARN, ERROR)
3. Check for startup messages and errors

## Database Migration

On first deployment:
1. Database schema will be created automatically via Hibernate
2. In `application-prod.yml`, `ddl-auto: validate` ensures no accidental changes
3. For schema updates, set `DB_DDL_AUTO=update` temporarily, then revert to `validate`

## Monitoring & Debugging

### Access Logs
Render shows logs in real-time. Look for:
- Application startup messages
- Connection pool initialization
- Any connection errors

### Common Issues

**"Database connection failed"**
- Verify DB_URL format is correct
- Check database is accessible from Render
- Ensure database credentials are correct
- Check IP allowlist on database

**"Redis connection failed"**
- Verify REDIS_HOST and REDIS_PORT are correct
- Check REDIS_SSL setting
- Verify Redis is running and accessible

**"Port already in use"**
- Don't set SERVER_PORT manually; Render assigns it
- The app listens on the port assigned by Render

**"Out of Memory"**
- Increase `JAVA_OPTS=-Xmx1024m`
- Adjust database pool size (DB_POOL_SIZE)

### Performance Tuning

**For High Traffic:**
```
JAVA_OPTS=-Xmx1024m -Xms512m -XX:+UseG1GC
DB_POOL_SIZE=30
```

**For Low Traffic:**
```
JAVA_OPTS=-Xmx256m -Xms128m
DB_POOL_SIZE=8
```

## Auto-Restart & Scaling

- **Restart:** Render automatically restarts on crash
- **Scaling:** Upgrade Instance Type for higher traffic
- **Enable Auto-Scale:** Available on paid plans

## Continuous Deployment

Once set up, Render watches your GitHub repository:
1. Push to `main` branch → Auto-deployment triggered
2. Render runs build command
3. If build succeeds, service is updated
4. Logs show deployment progress

To disable auto-deploy:
1. Go to **Service Settings** → **GitHub Connection**
2. Toggle **Auto Deploy** off

## Troubleshooting Deployment

### Build Fails
1. Check **Build Logs** in Render
2. Common issues:
   - `mvn` not in build environment
   - Java version mismatch
   - Missing dependencies

**Fix:** Ensure `pom.xml` is correct and Java 17+ is available

### Service Crashes on Startup
1. Check **Runtime Logs**
2. Look for:
   - `Exception` stacktraces
   - Connection failures
   - Missing environment variables

**Fix:** Verify all required env vars are set correctly

### No Data Persisting
- Check database connection: `/actuator/health`
- Verify DDL mode is not `create-drop` in production
- Check Redis is working: `/api/v1/health`

## Rollback

If deployment fails:
1. Go to **Render Dashboard** → Service → **Deployments**
2. Select previous working deployment
3. Click **Deploy**

## Cost Considerations

- **PostgreSQL Starter:** ~$9/month
- **Redis Starter:** ~$4/month
- **Web Service Starter:** ~$7/month
- **Total:** ~$20/month for basic production setup

Upgrade instance types for higher traffic.

## Maintenance

### Regular Tasks
- Monitor logs for errors
- Check database size (grow as needed)
- Review Redis memory usage
- Update Java version when Spring Boot updates

### Updating Application
1. Push code to GitHub
2. Render auto-deploys
3. Monitor logs to verify success

### Updating Dependencies
1. Update `pom.xml`
2. Test locally: `mvn clean package`
3. Push to GitHub
4. Render rebuilds and deploys

## Support & Resources

- [Render Documentation](https://render.com/docs)
- [Spring Boot on Render](https://render.com/docs/deploy-spring)
- [PostgreSQL on Render](https://render.com/docs/databases)
- [Redis on Render](https://render.com/docs/redis)

## Next Steps

1. **Monitor Logs:** Watch for any startup errors
2. **Test Endpoints:** Hit all API endpoints to verify functionality
3. **Load Test:** Optional - test with expected traffic
4. **Set Up Alerts:** Configure email notifications for errors
5. **Document API:** Generate API docs for frontend team
