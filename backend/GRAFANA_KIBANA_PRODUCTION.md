# Grafana & Kibana Production Setup

This guide covers deploying Grafana and Kibana to production (Render, AWS, or other cloud providers).

## Architecture Options

### Option 1: Managed Services (Recommended)
- **Grafana:** Grafana Cloud (SaaS)
- **Logs:** Elastic Cloud or DataDog
- **Cost:** ~$50-150/month
- **Benefit:** No ops overhead, automatic backups, high availability

### Option 2: Self-Hosted on Kubernetes
- **Grafana:** Helm chart
- **Logs:** Elasticsearch + Kibana
- **Cost:** Variable (compute + storage)
- **Benefit:** Full control, can be cheaper at scale

### Option 3: AWS Services
- **Metrics:** CloudWatch + Grafana
- **Logs:** CloudWatch Logs or OpenSearch
- **Cost:** Pay-per-use
- **Benefit:** Tight AWS integration

---

## Option 1: Grafana Cloud + Elasticsearch Cloud

### Step 1: Grafana Cloud Setup

1. Go to [grafana.com/auth/sign-up](https://grafana.com/auth/sign-up)
2. Create account (free tier available)
3. Create new Prometheus data source
4. Get Prometheus remote write URL

### Step 2: Application Configuration

**Update `application-prod.yml`:**

```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
  # Add remote write for Grafana Cloud
  prometheus:
    pushgateway:
      base-url: https://prometheus-blocks-prod-xxxx.grafana-blocks.grafana.net/api/prom/push
      headers:
        Authorization: "Bearer YOUR_GRAFANA_API_TOKEN"
```

**Environment variables:**

```bash
GRAFANA_CLOUD_URL=https://prometheus-blocks-prod-xxxx.grafana-blocks.grafana.net
GRAFANA_CLOUD_API_TOKEN=your_grafana_api_token_here
GRAFANA_CLOUD_ORG_ID=your_org_id
```

### Step 3: Elasticsearch Cloud Setup

1. Go to [elastic.co](https://elastic.co) → Create Deployment
2. Choose **Elasticsearch** + **Kibana**
3. Get connection endpoint and credentials
4. Create API key for authentication

### Step 4: Send Logs to Elasticsearch Cloud

**Update `application-prod.yml`:**

```yaml
spring:
  elasticsearch:
    rest:
      uris: 
        - "https://your-deployment.es.us-west-1.aws.found.io"
      username: elastic
      password: "${ELASTICSEARCH_PASSWORD}"
```

**In `logback-spring.xml`:**

```xml
<property name="ELASTICSEARCH_HOST">${ELASTICSEARCH_HOST:-your-deployment.es.us-west-1.aws.found.io}</property>
<property name="ELASTICSEARCH_PORT">443</property>
<property name="ELASTICSEARCH_PROTOCOL">https</property>
```

**Environment variables:**

```bash
ELASTICSEARCH_HOST=your-deployment.es.us-west-1.aws.found.io
ELASTICSEARCH_PASSWORD=your_elastic_password
```

---

## Option 2: Self-Hosted on Kubernetes

### Prerequisites
- Kubernetes cluster (EKS, GKE, AKS)
- `kubectl` configured
- Helm 3+

### Step 1: Add Helm Repositories

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo add elastic https://helm.elastic.co
helm repo update
```

### Step 2: Install Prometheus

```bash
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --values prometheus-values.yaml
```

**prometheus-values.yaml:**

```yaml
prometheus:
  prometheusSpec:
    retention: 30d
    storageSpec:
      volumeClaimTemplate:
        spec:
          storageClassName: gp2
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: 100Gi

grafana:
  enabled: true
  adminPassword: $(openssl rand -base64 32)
  persistence:
    enabled: true
    storageClassName: gp2
    size: 10Gi
  ingress:
    enabled: true
    hosts:
      - grafana.yourdomain.com
```

### Step 3: Install Elasticsearch & Kibana

```bash
helm install elasticsearch elastic/elasticsearch \
  --namespace monitoring \
  --values elasticsearch-values.yaml

helm install kibana elastic/kibana \
  --namespace monitoring \
  --values kibana-values.yaml
```

**elasticsearch-values.yaml:**

```yaml
replicas: 3
resources:
  limits:
    memory: "2Gi"
  requests:
    memory: "1Gi"
    cpu: "1000m"

persistence:
  enabled: true
  storageClassName: gp2
  size: 100Gi

auth:
  enabled: true
  elastic:
    password: "$(openssl rand -base64 32)"
```

**kibana-values.yaml:**

```yaml
replicas: 2
resources:
  limits:
    memory: "1Gi"
  requests:
    memory: "512Mi"

ingress:
  enabled: true
  hosts:
    - kibana.yourdomain.com

elasticsearch:
  hosts:
    - "https://elasticsearch-master:9200"
  username: elastic
  password: "${ELASTICSEARCH_PASSWORD}"
```

### Step 4: Configure Application to Send Logs

Configure application deployment to send logs to Elasticsearch:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: latency-tracker
spec:
  template:
    spec:
      containers:
      - name: app
        env:
        - name: ELASTICSEARCH_HOST
          value: "elasticsearch-master.monitoring.svc.cluster.local"
        - name: ELASTICSEARCH_PORT
          value: "9200"
        - name: ELASTICSEARCH_SCHEME
          value: "https"
```

---

## Option 3: AWS Services

### Step 1: CloudWatch Logs (Automatic)

Spring Boot automatically sends logs to CloudWatch if running on EC2/ECS/Lambda.

**Enable in `application-prod.yml`:**

```yaml
logging:
  level:
    com.coinbase.latencytracker: INFO
  # Logs automatically go to CloudWatch
```

### Step 2: CloudWatch Metrics → Grafana

1. Create IAM user for Grafana
2. Add policy: `CloudWatchReadOnlyAccess`
3. In Grafana Cloud, add CloudWatch data source
4. Create dashboards querying CloudWatch

### Step 3: X-Ray Tracing (Optional)

For distributed tracing:

```yaml
# application-prod.yml
management:
  tracing:
    sampling:
      probability: 0.1
```

Add dependency:

```xml
<dependency>
    <groupId>software.amazon.xray</groupId>
    <artifactId>aws-xray-recorder-sdk-spring</artifactId>
</dependency>
```

---

## Render-Specific Setup

### For Grafana

1. Create separate Render service for Grafana
2. Use PostgreSQL on Render for Grafana database
3. Expose only through private endpoint or VPN

```bash
# Render service config
runtime: docker
build-command: docker build -t grafana:latest .
start-command: grafana-server --config=/etc/grafana/grafana.ini

env:
  GF_SECURITY_ADMIN_USER: admin
  GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD}
  GF_DATABASE_TYPE: postgres
  GF_DATABASE_HOST: ${POSTGRES_HOST}
  GF_DATABASE_USER: ${POSTGRES_USER}
  GF_DATABASE_PASSWORD: ${POSTGRES_PASSWORD}
```

### For Kibana

1. Use Elasticsearch Cloud (not self-hosted on Render)
2. Kibana connects to Elasticsearch Cloud
3. Forward logs from app to Elasticsearch Cloud

**Dockerfile for Kibana:**

```dockerfile
FROM docker.elastic.co/kibana/kibana:8.10.0

ENV ELASTICSEARCH_HOSTS=https://your-deployment.es.us-west-1.aws.found.io
ENV ELASTICSEARCH_USERNAME=elastic
ENV ELASTICSEARCH_PASSWORD=${KIBANA_ES_PASSWORD}
```

---

## Accessing Dashboards

### Local (Docker Compose)
- Grafana: http://localhost:3000
- Kibana: http://localhost:5601
- Prometheus: http://localhost:9090

### Production Render
- Render doesn't expose internal IPs by default
- Options:
  1. **Private service** (only accessible to other Render services)
  2. **Public with authentication** (strong password required)
  3. **VPN** (set up WireGuard/OpenVPN on Render)

### Production AWS/K8s
- Use Ingress with SSL/TLS
- Require authentication (OAuth, LDAP)
- IP whitelist for internal access

---

## Monitoring Dashboards

### Essential Dashboards

1. **Application Overview**
   - Request rate
   - Error rate
   - Latency percentiles
   - Budget violations

2. **Operational**
   - JVM memory usage
   - Database connections
   - Circuit breaker status
   - Cache hit rate

3. **Business Metrics**
   - Transaction count
   - Revenue (if applicable)
   - User activity
   - Feature usage

4. **Infrastructure**
   - CPU usage
   - Memory usage
   - Network I/O
   - Disk space

### Creating Alerts

**Example: Alert on high error rate**

```
alert: HighErrorRate
expr: (rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])) > 0.01
for: 5m
annotations:
  summary: "Error rate > 1% for 5 minutes"
  dashboard: "http://grafana.example.com/api/v1/dashboards/uid/api-health"
```

**Notification channels:**
- Email
- Slack
- PagerDuty
- Webhook
- Opsgenie

---

## Cost Optimization

### Metrics (Prometheus)
- Retention: 30 days default
- Can reduce to 7 days if archiving
- ~5-10GB per month for single app

### Logs (Elasticsearch)
- Index retention: 7-30 days
- ~5-20GB per month depending on log volume
- Hot-warm-cold tiering for older data

### Sampling
- Default: 10% trace sampling
- Can reduce to 1% in production
- Keep 100% for errors

### Cost Estimates (Grafana Cloud)
- Prometheus: $12/month (10GB storage)
- Grafana: Free - $299/month
- Elasticsearch: $95/month (5GB storage)
- **Total: ~$100-400/month**

---

## Security Considerations

### Credentials
- Store in environment variables
- Use managed secrets (Render, AWS Secrets Manager)
- Rotate API keys monthly

### Network
- Keep Prometheus/Kibana internal (no public access)
- Use VPN for external access
- Enable TLS for all connections

### Authentication
- Grafana: Strong password + 2FA
- Kibana: Basic auth or SAML
- API tokens: Rotate regularly

### Data Privacy
- GDPR: Delete PII from logs
- Sanitize sensitive data
- Use data masking for credentials

---

## Troubleshooting

### Metrics Not Appearing
1. Check Prometheus targets: `/api/v1/targets`
2. Verify scrape endpoint: `/actuator/prometheus`
3. Check network connectivity
4. Verify credentials/authentication

### Logs Not Appearing
1. Check Elasticsearch cluster health
2. Verify app can reach Elasticsearch
3. Check logback configuration
4. Verify index patterns in Kibana

### High Costs
1. Reduce retention period
2. Reduce sampling rate
3. Filter unnecessary metrics
4. Use log level configuration

### Dashboard Slow
1. Optimize PromQL queries
2. Reduce time range
3. Increase scrape interval
4. Archive old data

---

## References

- [Grafana Cloud Docs](https://grafana.com/docs/grafana-cloud/)
- [Elasticsearch Cloud Docs](https://www.elastic.co/guide/en/cloud/current/)
- [Prometheus Operator Helm](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack)
- [AWS CloudWatch Docs](https://docs.aws.amazon.com/cloudwatch/)
- [Render Documentation](https://render.com/docs)

---

## Support

For issues:
1. Check service health dashboards
2. Review logs in Kibana
3. Check metrics in Prometheus
4. Contact cloud provider support
5. Check documentation links above
