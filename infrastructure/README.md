# Infrastructure

This directory contains infrastructure configuration for the Authorization Platform.

## Local Development

### Prerequisites
- Docker Desktop or Docker Engine 24+
- Docker Compose V2

### Quick Start

```bash
# Start all services
docker-compose up -d

# Check service health
docker-compose ps

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

### Services

| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL | 5432 | Primary database for auth platform |
| Redis | 6379 | L2 cache and session storage |
| OPA | 8181 | Policy evaluation engine |

### Database Access

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U authplatform -d authplatform

# Run SQL file
docker-compose exec -T postgres psql -U authplatform -d authplatform < schema.sql
```

### Redis Access

```bash
# Connect to Redis CLI
docker-compose exec redis redis-cli

# Monitor Redis commands
docker-compose exec redis redis-cli MONITOR

# Check cache statistics
docker-compose exec redis redis-cli INFO stats
```

### OPA Access

```bash
# Check OPA health
curl http://localhost:8181/health

# Query policy data
curl http://localhost:8181/v1/data

# Load policy bundle
curl -X PUT http://localhost:8181/v1/policies/rbac \
  --data-binary @policies/rbac.rego
```

## Production Deployment

See `kubernetes/` directory for Kubernetes manifests.
