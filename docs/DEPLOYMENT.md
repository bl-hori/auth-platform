# デプロイメントガイド

このガイドでは、Auth Platformを本番環境にデプロイする方法について説明します。

## 目次

- [デプロイメント概要](#デプロイメント概要)
- [環境構築](#環境構築)
- [Docker デプロイ](#docker-デプロイ)
- [Kubernetes デプロイ](#kubernetes-デプロイ)
- [環境変数設定](#環境変数設定)
- [セキュリティ設定](#セキュリティ設定)
- [モニタリング](#モニタリング)
- [バックアップとリストア](#バックアップとリストア)

## デプロイメント概要

### アーキテクチャ

```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │ HTTPS (443)
       ↓
┌──────────────┐
│  Nginx/ALB   │ ← ロードバランサー
└──────┬───────┘
       ├───────────────┬──────────────┐
       ↓               ↓              ↓
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│  Frontend   │ │  Frontend   │ │  Frontend   │
│  (Next.js)  │ │  (Next.js)  │ │  (Next.js)  │
└──────┬──────┘ └──────┬──────┘ └──────┬──────┘
       │               │              │
       └───────┬───────┴──────┬───────┘
               ↓              ↓
        ┌─────────────┐ ┌─────────────┐
        │   Backend   │ │   Backend   │
        │(Spring Boot)│ │(Spring Boot)│
        └──────┬──────┘ └──────┬──────┘
               │              │
               └──────┬───────┘
                      ↓
            ┌──────────────────┐
            │   PostgreSQL     │
            │   (Primary)      │
            └─────────┬────────┘
                      │
            ┌─────────┴────────┐
            │   PostgreSQL     │
            │   (Replica)      │
            └──────────────────┘

            ┌──────────────────┐
            │   Redis          │
            │   (Cluster)      │
            └──────────────────┘
```

### デプロイメントオプション

| オプション | 推奨環境 | 複雑度 | スケーラビリティ |
|-----------|---------|--------|----------------|
| Docker Compose | 開発・小規模 | 低 | 低 |
| Docker Swarm | 中規模 | 中 | 中 |
| Kubernetes | 本番・大規模 | 高 | 高 |
| AWS ECS/Fargate | AWS環境 | 中 | 高 |

## 環境構築

### 本番環境の要件

#### 最小構成

- **CPU**: 4 cores
- **RAM**: 8GB
- **Storage**: 50GB SSD
- **Network**: 100Mbps

#### 推奨構成

- **CPU**: 8 cores
- **RAM**: 16GB
- **Storage**: 100GB SSD (RAID 1)
- **Network**: 1Gbps
- **Load Balancer**: Nginx / ALB
- **Database**: PostgreSQL (Primary + Replica)
- **Cache**: Redis (Cluster mode)

## Docker デプロイ

### 本番用 Docker Compose

`infrastructure/docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: authplatform
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 4G
    restart: always

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 2G
    restart: always

  backend:
    image: auth-platform-backend:${VERSION}
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/authplatform
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      - postgres
      - redis
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '2'
          memory: 2G
    restart: always

  frontend:
    image: auth-platform-frontend:${VERSION}
    environment:
      NEXT_PUBLIC_API_URL: https://api.example.com
    depends_on:
      - backend
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '1'
          memory: 1G
    restart: always

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - frontend
      - backend
    restart: always

volumes:
  postgres_data:
  redis_data:
```

### デプロイ手順

```bash
# 1. イメージのビルド
cd backend
./gradlew bootJar
docker build -t auth-platform-backend:1.0.0 .

cd ../frontend
pnpm build
docker build -t auth-platform-frontend:1.0.0 .

# 2. 環境変数の設定
cp .env.example .env.prod
# .env.prodを編集

# 3. デプロイ
cd infrastructure
docker compose -f docker-compose.prod.yml up -d

# 4. ヘルスチェック
curl http://localhost:8080/actuator/health
curl http://localhost:3000/api/health

# 5. ログ確認
docker compose -f docker-compose.prod.yml logs -f
```

## Kubernetes デプロイ

### Kubernetes マニフェスト

`infrastructure/k8s/deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-platform-backend
  namespace: auth-platform
spec:
  replicas: 3
  selector:
    matchLabels:
      app: auth-platform-backend
  template:
    metadata:
      labels:
        app: auth-platform-backend
    spec:
      containers:
      - name: backend
        image: auth-platform-backend:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: auth-platform-secrets
              key: database-url
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: auth-platform-backend
  namespace: auth-platform
spec:
  selector:
    app: auth-platform-backend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-platform-frontend
  namespace: auth-platform
spec:
  replicas: 3
  selector:
    matchLabels:
      app: auth-platform-frontend
  template:
    metadata:
      labels:
        app: auth-platform-frontend
    spec:
      containers:
      - name: frontend
        image: auth-platform-frontend:1.0.0
        ports:
        - containerPort: 3000
        env:
        - name: NEXT_PUBLIC_API_URL
          value: "https://api.example.com"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: auth-platform-frontend
  namespace: auth-platform
spec:
  selector:
    app: auth-platform-frontend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 3000
  type: ClusterIP
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: auth-platform-ingress
  namespace: auth-platform
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - example.com
    - api.example.com
    secretName: auth-platform-tls
  rules:
  - host: example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: auth-platform-frontend
            port:
              number: 80
  - host: api.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: auth-platform-backend
            port:
              number: 80
```

### Kubernetesデプロイ手順

```bash
# 1. Namespaceの作成
kubectl create namespace auth-platform

# 2. Secretsの作成
kubectl create secret generic auth-platform-secrets \
  --from-literal=database-url='jdbc:postgresql://...' \
  --from-literal=database-username='...' \
  --from-literal=database-password='...' \
  --from-literal=redis-password='...' \
  --from-literal=jwt-secret='...' \
  -n auth-platform

# 3. ConfigMapの作成
kubectl create configmap auth-platform-config \
  --from-file=application.yml \
  -n auth-platform

# 4. デプロイ
kubectl apply -f infrastructure/k8s/

# 5. 確認
kubectl get pods -n auth-platform
kubectl get services -n auth-platform
kubectl get ingress -n auth-platform

# 6. ログ確認
kubectl logs -f deployment/auth-platform-backend -n auth-platform
```

## 環境変数設定

### Backend環境変数

```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/authplatform
SPRING_DATASOURCE_USERNAME=authuser
SPRING_DATASOURCE_PASSWORD=<strong-password>

# Redis
SPRING_DATA_REDIS_HOST=redis-host
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=<redis-password>

# JWT
JWT_SECRET=<256-bit-secret>
JWT_EXPIRATION=3600000

# CORS
CORS_ALLOWED_ORIGINS=https://example.com,https://app.example.com

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_AUTHPLATFORM=DEBUG

# Actuator
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,prometheus

# Profile
SPRING_PROFILES_ACTIVE=prod
```

### Frontend環境変数

```env
# API
NEXT_PUBLIC_API_URL=https://api.example.com

# Analytics (Optional)
NEXT_PUBLIC_GA_ID=G-XXXXXXXXXX

# Sentry (Optional)
NEXT_PUBLIC_SENTRY_DSN=https://...@sentry.io/...

# Environment
NODE_ENV=production
```

## セキュリティ設定

### SSL/TLS 証明書

#### Let's Encrypt (推奨)

```bash
# Cert-managerのインストール (Kubernetes)
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# ClusterIssuerの作成
kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
```

### ファイアウォール設定

```bash
# UFW (Ubuntu)
sudo ufw allow 22/tcp   # SSH
sudo ufw allow 80/tcp   # HTTP
sudo ufw allow 443/tcp  # HTTPS
sudo ufw enable

# iptables
sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 443 -j ACCEPT
sudo iptables -A INPUT -j DROP
```

### セキュリティヘッダー

Nginx設定 (`nginx/nginx.conf`):

```nginx
server {
    listen 443 ssl http2;
    server_name example.com;

    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;

    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';" always;

    location / {
        proxy_pass http://frontend:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## モニタリング

### Prometheus + Grafana

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'auth-platform-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']

  - job_name: 'auth-platform-frontend'
    static_configs:
      - targets: ['frontend:3000']
```

### アラート設定

```yaml
# alerts.yml
groups:
  - name: auth_platform
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High error rate detected"

      - alert: HighLatency
        expr: http_server_requests_seconds{quantile="0.95"} > 1
        for: 5m
        annotations:
          summary: "High latency detected"

      - alert: DatabaseDown
        expr: up{job="postgres"} == 0
        for: 1m
        annotations:
          summary: "PostgreSQL is down"
```

### ログ集約

```yaml
# Fluentd設定
<source>
  @type tail
  path /var/log/containers/*.log
  pos_file /var/log/fluentd-containers.log.pos
  tag kubernetes.*
  format json
</source>

<match kubernetes.**>
  @type elasticsearch
  host elasticsearch
  port 9200
  logstash_format true
</match>
```

## バックアップとリストア

### データベースバックアップ

```bash
# 手動バックアップ
docker exec postgres pg_dump -U authuser authplatform > backup_$(date +%Y%m%d_%H%M%S).sql

# 自動バックアップ (cron)
0 2 * * * /usr/local/bin/backup-database.sh
```

`backup-database.sh`:

```bash
#!/bin/bash
BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)
FILENAME="authplatform_${DATE}.sql.gz"

# Backup
docker exec postgres pg_dump -U authuser authplatform | gzip > "${BACKUP_DIR}/${FILENAME}"

# S3にアップロード
aws s3 cp "${BACKUP_DIR}/${FILENAME}" s3://backups/database/

# 7日以上古いバックアップを削除
find "${BACKUP_DIR}" -name "*.sql.gz" -mtime +7 -delete
```

### リストア

```bash
# データベースのリストア
gunzip < backup_20250101_020000.sql.gz | docker exec -i postgres psql -U authuser authplatform
```

## ロールバック

### Kubernetes

```bash
# デプロイメント履歴確認
kubectl rollout history deployment/auth-platform-backend -n auth-platform

# 前のバージョンにロールバック
kubectl rollout undo deployment/auth-platform-backend -n auth-platform

# 特定のリビジョンにロールバック
kubectl rollout undo deployment/auth-platform-backend --to-revision=2 -n auth-platform
```

### Docker Compose

```bash
# 古いイメージを使用
docker tag auth-platform-backend:1.0.0 auth-platform-backend:1.0.1
docker compose -f docker-compose.prod.yml up -d backend
```

## スケーリング

### 水平スケーリング (Kubernetes)

```bash
# レプリカ数を増やす
kubectl scale deployment/auth-platform-backend --replicas=5 -n auth-platform

# オートスケーリング設定
kubectl autoscale deployment/auth-platform-backend \
  --cpu-percent=70 \
  --min=3 \
  --max=10 \
  -n auth-platform
```

### 垂直スケーリング

```yaml
resources:
  requests:
    memory: "2Gi"
    cpu: "1000m"
  limits:
    memory: "4Gi"
    cpu: "2000m"
```

## チェックリスト

### デプロイ前

- [ ] 環境変数が正しく設定されている
- [ ] SSL証明書が有効
- [ ] データベースマイグレーションが完了
- [ ] バックアップが取得されている
- [ ] ヘルスチェックが正常
- [ ] 全テストが通過
- [ ] セキュリティスキャン完了

### デプロイ後

- [ ] アプリケーションが起動している
- [ ] ヘルスチェックが正常
- [ ] ログにエラーがない
- [ ] メトリクスが収集されている
- [ ] アラートが設定されている
- [ ] バックアップが動作している

## 次のステップ

- [Getting Started](./GETTING_STARTED.md) - 初期セットアップ
- [Troubleshooting](./TROUBLESHOOTING.md) - トラブルシューティング
- [Monitoring Guide](./MONITORING.md) - モニタリング詳細
