# Kubernetes Deployment Guide

This directory contains Kubernetes manifests for deploying the Auth Platform to GKE (Google Kubernetes Engine) or any Kubernetes cluster.

## 📁 Directory Structure

```
k8s/
├── base/                           # Base Kubernetes manifests
│   ├── namespace.yaml              # Namespace definition
│   ├── configmap.yaml              # Application configuration
│   ├── secrets.yaml.example        # Secret template (DO NOT COMMIT ACTUAL SECRETS)
│   ├── postgres-statefulset.yaml   # PostgreSQL database
│   ├── redis-deployment.yaml       # Redis cache
│   ├── opa-deployment.yaml         # Open Policy Agent
│   ├── backend-deployment.yaml     # Spring Boot backend
│   ├── frontend-deployment.yaml    # Next.js frontend
│   └── ingress.yaml                # Ingress configuration
├── staging/                        # Staging environment overrides (future)
├── production/                     # Production environment overrides (future)
├── deploy.sh                       # Automated deployment script
└── README.md                       # This file
```

## 🚀 Quick Start

### Prerequisites

1. **kubectl** installed and configured
   ```bash
   # Install kubectl (macOS)
   brew install kubectl

   # Install kubectl (Linux)
   curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
   chmod +x kubectl && sudo mv kubectl /usr/local/bin/
   ```

2. **GKE cluster** created and kubectl context set
   ```bash
   # Create GKE cluster (example)
   gcloud container clusters create auth-platform-staging \
     --zone=asia-northeast1-a \
     --num-nodes=3 \
     --machine-type=e2-standard-2 \
     --enable-autoscaling \
     --min-nodes=2 \
     --max-nodes=5

   # Get cluster credentials
   gcloud container clusters get-credentials auth-platform-staging \
     --zone=asia-northeast1-a
   ```

3. **Docker images** built and pushed to GCR
   ```bash
   # Build and push backend
   cd backend
   docker build -t gcr.io/YOUR_PROJECT_ID/auth-platform-backend:latest .
   docker push gcr.io/YOUR_PROJECT_ID/auth-platform-backend:latest

   # Build and push frontend
   cd ../frontend
   docker build -t gcr.io/YOUR_PROJECT_ID/auth-platform-frontend:latest .
   docker push gcr.io/YOUR_PROJECT_ID/auth-platform-frontend:latest
   ```

### Step-by-Step Deployment

#### 1. Create Secrets

```bash
# Copy the example secrets file
cp k8s/base/secrets.yaml.example k8s/base/secrets.yaml

# Edit secrets.yaml and update with your actual values (base64 encoded)
# To encode: echo -n 'your-password' | base64
vi k8s/base/secrets.yaml

# Apply secrets
kubectl apply -f k8s/base/secrets.yaml
```

**IMPORTANT**: Never commit `secrets.yaml` to Git! It's already in `.gitignore`.

#### 2. Update Configuration

Edit `k8s/base/configmap.yaml` if needed:
- Database connection settings
- Redis connection settings
- OPA URL
- Application-specific settings

Edit `k8s/base/backend-deployment.yaml` and `k8s/base/frontend-deployment.yaml`:
- Replace `PROJECT_ID` with your GCP project ID in image references
- Example: `gcr.io/my-gcp-project/auth-platform-backend:latest`

Edit `k8s/base/ingress.yaml`:
- Replace domain names with your actual domains
- Configure TLS/SSL certificates

#### 3. Deploy Using Script

```bash
# Make the script executable (if not already)
chmod +x k8s/deploy.sh

# Run deployment
./k8s/deploy.sh staging
```

#### 4. Verify Deployment

```bash
# Check all resources
kubectl get all -n auth-platform-staging

# Check pods status
kubectl get pods -n auth-platform-staging

# Check ingress
kubectl get ingress -n auth-platform-staging

# Get ingress IP address
kubectl get ingress auth-platform-ingress -n auth-platform-staging \
  -o jsonpath='{.status.loadBalancer.ingress[0].ip}'
```

#### 5. View Logs

```bash
# Backend logs
kubectl logs -f deployment/backend -n auth-platform-staging

# Frontend logs
kubectl logs -f deployment/frontend -n auth-platform-staging

# PostgreSQL logs
kubectl logs -f statefulset/postgres -n auth-platform-staging

# OPA logs
kubectl logs -f deployment/opa -n auth-platform-staging
```

## 🔧 Manual Deployment (Alternative)

If you prefer manual deployment instead of using the script:

```bash
# 1. Create namespace
kubectl apply -f k8s/base/namespace.yaml

# 2. Create secrets
kubectl apply -f k8s/base/secrets.yaml

# 3. Create ConfigMap
kubectl apply -f k8s/base/configmap.yaml

# 4. Deploy infrastructure
kubectl apply -f k8s/base/postgres-statefulset.yaml
kubectl apply -f k8s/base/redis-deployment.yaml
kubectl apply -f k8s/base/opa-deployment.yaml

# 5. Wait for infrastructure to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n auth-platform-staging --timeout=300s
kubectl wait --for=condition=ready pod -l app=opa -n auth-platform-staging --timeout=180s

# 6. Deploy application
kubectl apply -f k8s/base/backend-deployment.yaml
kubectl apply -f k8s/base/frontend-deployment.yaml

# 7. Wait for application to be ready
kubectl wait --for=condition=ready pod -l app=backend -n auth-platform-staging --timeout=300s
kubectl wait --for=condition=ready pod -l app=frontend -n auth-platform-staging --timeout=180s

# 8. Deploy Ingress
kubectl apply -f k8s/base/ingress.yaml
```

## 🔄 Updating the Application

### Rolling Update

```bash
# Update backend
kubectl set image deployment/backend \
  backend=gcr.io/YOUR_PROJECT_ID/auth-platform-backend:v1.1.0 \
  -n auth-platform-staging

# Update frontend
kubectl set image deployment/frontend \
  frontend=gcr.io/YOUR_PROJECT_ID/auth-platform-frontend:v1.1.0 \
  -n auth-platform-staging

# Check rollout status
kubectl rollout status deployment/backend -n auth-platform-staging
kubectl rollout status deployment/frontend -n auth-platform-staging
```

### Rollback

```bash
# Rollback backend to previous version
kubectl rollout undo deployment/backend -n auth-platform-staging

# Rollback to specific revision
kubectl rollout undo deployment/backend --to-revision=2 -n auth-platform-staging

# Check rollout history
kubectl rollout history deployment/backend -n auth-platform-staging
```

## 🐛 Troubleshooting

### Pods not starting

```bash
# Describe pod to see events
kubectl describe pod <pod-name> -n auth-platform-staging

# Check pod logs
kubectl logs <pod-name> -n auth-platform-staging

# Get previous container logs (if crashed)
kubectl logs <pod-name> --previous -n auth-platform-staging
```

### Database connection issues

```bash
# Test PostgreSQL connection from a pod
kubectl run -it --rm debug --image=postgres:15-alpine --restart=Never -n auth-platform-staging -- \
  psql -h postgres-service -U authplatform -d authplatform

# Check PostgreSQL service
kubectl get svc postgres-service -n auth-platform-staging
kubectl describe svc postgres-service -n auth-platform-staging
```

### Backend health check failing

```bash
# Port-forward to backend and test locally
kubectl port-forward deployment/backend 8080:8080 -n auth-platform-staging

# In another terminal
curl http://localhost:8080/actuator/health
```

### Ingress not working

```bash
# Check ingress status
kubectl describe ingress auth-platform-ingress -n auth-platform-staging

# Check ingress controller logs (if using NGINX)
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller
```

## 🔐 Security Considerations

1. **Secrets Management**
   - Never commit secrets to Git
   - Use Google Secret Manager for production
   - Rotate credentials regularly

2. **Network Policies**
   - Implement network policies to restrict pod-to-pod communication
   - Use private GKE clusters for production

3. **RBAC**
   - Configure proper Role-Based Access Control
   - Use service accounts with minimal permissions

4. **TLS/SSL**
   - Always use HTTPS in production
   - Use cert-manager for automatic certificate management

## 📊 Monitoring

```bash
# CPU and memory usage
kubectl top pods -n auth-platform-staging
kubectl top nodes

# Resource quotas
kubectl describe resourcequota -n auth-platform-staging

# Events
kubectl get events -n auth-platform-staging --sort-by='.lastTimestamp'
```

## 🧹 Cleanup

```bash
# Delete all resources in the namespace
kubectl delete namespace auth-platform-staging

# Or delete individual resources
kubectl delete -f k8s/base/ingress.yaml
kubectl delete -f k8s/base/frontend-deployment.yaml
kubectl delete -f k8s/base/backend-deployment.yaml
kubectl delete -f k8s/base/opa-deployment.yaml
kubectl delete -f k8s/base/redis-deployment.yaml
kubectl delete -f k8s/base/postgres-statefulset.yaml
kubectl delete -f k8s/base/configmap.yaml
kubectl delete -f k8s/base/secrets.yaml
kubectl delete -f k8s/base/namespace.yaml
```

## 📚 Next Steps

1. **Configure DNS**: Point your domain to the Ingress IP address
2. **Setup SSL**: Configure TLS certificates (Let's Encrypt with cert-manager)
3. **Enable Monitoring**: Install Prometheus and Grafana
4. **Setup Alerting**: Configure alerting rules for critical metrics
5. **Backup Strategy**: Implement automated database backups
6. **CI/CD Integration**: Automate deployments with GitHub Actions

## 📖 Additional Resources

- [GKE Documentation](https://cloud.google.com/kubernetes-engine/docs)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
- [cert-manager Documentation](https://cert-manager.io/docs/)
- [NGINX Ingress Controller](https://kubernetes.github.io/ingress-nginx/)
