#!/usr/bin/env bash
set -euo pipefail

# Auth Platform - GKE Deployment Script
# Usage: ./deploy.sh [staging|production]

ENVIRONMENT="${1:-staging}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="${SCRIPT_DIR}/base"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi

    if ! command -v gcloud &> /dev/null; then
        log_warn "gcloud CLI is not installed. This is optional but recommended for GKE."
    fi

    log_info "Prerequisites check passed."
}

# Deploy to Kubernetes
deploy() {
    log_info "Deploying Auth Platform to ${ENVIRONMENT}..."

    # 1. Create namespace
    log_info "Creating namespace..."
    kubectl apply -f "${BASE_DIR}/namespace.yaml"

    # 2. Check if secrets exist
    if ! kubectl get secret auth-platform-secrets -n auth-platform-staging &> /dev/null; then
        log_error "Secrets not found! Please create secrets first:"
        log_error "  1. Copy k8s/base/secrets.yaml.example to k8s/base/secrets.yaml"
        log_error "  2. Update the values with actual credentials (base64 encoded)"
        log_error "  3. Run: kubectl apply -f k8s/base/secrets.yaml"
        exit 1
    fi

    # 3. Apply ConfigMap
    log_info "Applying ConfigMap..."
    kubectl apply -f "${BASE_DIR}/configmap.yaml"

    # 4. Deploy PostgreSQL
    log_info "Deploying PostgreSQL StatefulSet..."
    kubectl apply -f "${BASE_DIR}/postgres-statefulset.yaml"

    # Wait for PostgreSQL to be ready
    log_info "Waiting for PostgreSQL to be ready..."
    kubectl wait --for=condition=ready pod -l app=postgres -n auth-platform-staging --timeout=300s || true

    # 5. Deploy Redis
    log_info "Deploying Redis..."
    kubectl apply -f "${BASE_DIR}/redis-deployment.yaml"

    # 6. Deploy OPA
    log_info "Deploying OPA..."
    kubectl apply -f "${BASE_DIR}/opa-deployment.yaml"

    # Wait for OPA to be ready
    log_info "Waiting for OPA to be ready..."
    kubectl wait --for=condition=ready pod -l app=opa -n auth-platform-staging --timeout=180s || true

    # 7. Deploy Backend
    log_info "Deploying Backend..."
    kubectl apply -f "${BASE_DIR}/backend-deployment.yaml"

    # Wait for Backend to be ready
    log_info "Waiting for Backend to be ready..."
    kubectl wait --for=condition=ready pod -l app=backend -n auth-platform-staging --timeout=300s || true

    # 8. Deploy Frontend
    log_info "Deploying Frontend..."
    kubectl apply -f "${BASE_DIR}/frontend-deployment.yaml"

    # Wait for Frontend to be ready
    log_info "Waiting for Frontend to be ready..."
    kubectl wait --for=condition=ready pod -l app=frontend -n auth-platform-staging --timeout=180s || true

    # 9. Deploy Ingress
    log_info "Deploying Ingress..."
    kubectl apply -f "${BASE_DIR}/ingress.yaml"

    log_info "Deployment completed successfully!"
}

# Show deployment status
show_status() {
    log_info "Deployment Status:"
    echo ""
    kubectl get all -n auth-platform-staging
    echo ""
    log_info "Ingress Status:"
    kubectl get ingress -n auth-platform-staging
}

# Main execution
main() {
    log_info "Auth Platform Deployment Script"
    log_info "Environment: ${ENVIRONMENT}"
    echo ""

    check_prerequisites
    deploy
    echo ""
    show_status

    echo ""
    log_info "Deployment completed! Next steps:"
    echo "  1. Check pod status: kubectl get pods -n auth-platform-staging"
    echo "  2. View logs: kubectl logs -f deployment/backend -n auth-platform-staging"
    echo "  3. Access the application via Ingress endpoint"
    echo ""
    log_warn "Note: Update the Ingress domain names in k8s/base/ingress.yaml"
    log_warn "Note: Configure DNS to point to the Ingress IP address"
}

main
