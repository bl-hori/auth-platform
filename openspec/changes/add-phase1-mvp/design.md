# Design Document: Phase 1 MVP

## Context
The authorization platform is being built from scratch with comprehensive specifications already defined. The MVP must deliver core value quickly while establishing a solid foundation for future phases.

Key stakeholders:
- Development team (2-3 engineers)
- Pilot application teams (3 early adopters)
- Security team (compliance requirements)
- Operations team (deployment and monitoring)

Constraints:
- 12-week delivery timeline
- Must achieve <10ms p95 authorization latency
- Must support multi-tenancy from day one
- Limited team size requires simplicity-first approach

## Goals / Non-Goals

### Goals
1. **Functional Goals**
   - Deliver working RBAC authorization system
   - Enable application integration via REST API
   - Provide web UI for policy/user management
   - Ensure audit trail for compliance

2. **Non-Functional Goals**
   - Achieve <10ms p95 latency for authorization API
   - Support 10,000 requests/second
   - Maintain 99.9% uptime
   - 80%+ test coverage

3. **Strategic Goals**
   - Validate core architecture decisions
   - Gather real-world usage feedback
   - Build team expertise in OPA/Rego
   - Establish CI/CD and operational practices

### Non-Goals (Explicitly Deferred)
- ABAC and ReBAC support → Phase 2
- SCIM synchronization → Phase 2
- gRPC API → Phase 2
- GitOps integration → Phase 2
- Multi-region deployment → Phase 3
- Advanced analytics → Phase 3
- No-code policy editor → Phase 3

## Decisions

### Decision 1: Monolith over Microservices for MVP
**Choice**: Implement as Spring Boot monolith instead of microservices

**Rationale**:
- Simpler deployment and operations (single deployable)
- Faster development (no inter-service communication overhead)
- Easier debugging and testing
- Team can gain experience before tackling microservices complexity
- Performance is easier to optimize in monolith
- Can refactor to microservices in Phase 2+ if needed

**Alternatives Considered**:
- Microservices from day one: Rejected due to complexity, longer timeline, and team size
- Modular monolith: Chosen approach - clear module boundaries to enable future extraction

**Implementation**:
```
backend/
├── api/              # REST controllers (thin layer)
├── service/          # Business logic (module per capability)
│   ├── authorization/
│   ├── policy/
│   ├── identity/
│   └── audit/
├── domain/           # Domain models and repositories
├── infrastructure/   # OPA, Redis, PostgreSQL clients
└── config/           # Spring configuration
```

### Decision 2: PostgreSQL over TimescaleDB for MVP Audit Logs
**Choice**: Use PostgreSQL with partitioning instead of TimescaleDB

**Rationale**:
- One less technology to operate and learn
- PostgreSQL table partitioning sufficient for 90-day retention
- Can migrate to TimescaleDB in Phase 2 when scale increases
- Simplifies local development environment

**Alternatives Considered**:
- TimescaleDB from start: Deferred to Phase 2
- NoSQL (Elasticsearch): Too complex for MVP needs

**Implementation**:
- Use PostgreSQL range partitioning on timestamp column
- Create monthly partitions automatically
- Query performance adequate for 90-day window

### Decision 3: OPA via REST API, Not Embedded
**Choice**: Deploy OPA as sidecar container, communicate via REST

**Rationale**:
- Independent scaling of OPA instances
- Easier OPA version upgrades without app redeployment
- Leverage OPA's built-in caching and optimization
- Clear separation of concerns

**Alternatives Considered**:
- Embedded OPA library: Tighter coupling, harder to scale independently
- OPA server cluster: Too complex for MVP

**Implementation**:
```yaml
# Kubernetes Pod
spec:
  containers:
  - name: app
    image: auth-platform:latest
  - name: opa
    image: openpolicyagent/opa:0.60
    args: ["run", "--server", "--bundle"]
```

### Decision 4: Multi-Layer Caching Strategy
**Choice**: Caffeine (L1) + Redis (L2) + PostgreSQL

**Rationale**:
- L1 (Caffeine): Ultra-fast in-memory cache (sub-millisecond)
- L2 (Redis): Shared cache across instances, slower but still fast (<5ms)
- Enables horizontal scaling while maintaining cache coherence
- Cache invalidation via Redis Pub/Sub

**Alternatives Considered**:
- Single-layer cache: Insufficient for latency target
- Hazelcast: More complex than needed for MVP

**Implementation**:
```java
@Cacheable(cacheNames = "authorization",
           cacheManager = "caffeineCacheManager")
public AuthorizationDecision authorize(AuthRequest req) {
    return redis.get(cacheKey)
        .orElseGet(() -> evaluateWithOPA(req));
}
```

**Cache TTLs**:
- L1 (Caffeine): 10 seconds, max 10,000 entries
- L2 (Redis): 5 minutes, max 1M entries
- Invalidation: Immediate on policy/permission changes

### Decision 5: RBAC Implementation Strategy
**Choice**: Policy-based RBAC (policies written in Rego) instead of hardcoded logic

**Rationale**:
- Consistent with overall policy-driven architecture
- Easier to extend to ABAC in Phase 2
- Allows customization per organization
- Leverages OPA's optimization

**Alternatives Considered**:
- Hardcoded RBAC in Java: Less flexible, harder to customize
- Spring Security ACL: Not aligned with policy-as-code vision

**Implementation**:
```rego
package authz.rbac

import future.keywords.if
import future.keywords.in

default allow := false

allow if {
    user_has_permission
}

user_has_permission if {
    some role in data.user_roles[input.subject.id]
    some permission in data.role_permissions[role]
    permission.action == input.action
    permission.resource_type == input.resource.type
}
```

### Decision 6: API Versioning Strategy
**Choice**: URL path versioning (`/v1/`, `/v2/`)

**Rationale**:
- Clear and explicit
- Easy to route and deprecate
- Industry standard
- Supports parallel version operation

**Alternatives Considered**:
- Header versioning: Less visible, harder to test
- Query parameter: Not RESTful

**Implementation**:
- All endpoints start with `/v1/`
- Plan for `/v2/` in Phase 2 for breaking changes
- Maintain v1 for at least 12 months after v2 release

### Decision 7: Frontend Technology
**Choice**: Next.js 15 with App Router and Server Components

**Rationale**:
- Modern React framework with excellent DX
- App Router provides better performance
- Server Components reduce client bundle size
- Built-in API routes for BFF pattern if needed
- TypeScript support out of the box

**Alternatives Considered**:
- Create React App: Deprecated, less performant
- Vite + React: More configuration needed
- Remix: Less ecosystem maturity

**Component Library**: shadcn/ui (not a library, reusable components)
- Copy-paste approach gives full control
- Built on Radix UI (accessibility)
- Tailwind CSS integration

### Decision 8: Testing Strategy
**Choice**: Comprehensive automated testing at all levels

**Test Pyramid**:
```
       E2E (5%)
      /        \     Playwright - Critical user flows
    Integration (20%)
   /            \    Spring Boot Test + Testcontainers
  Component (25%)
 /              \    React Testing Library
Unit (50%)
─────────────────   JUnit 5 + Jest
```

**Key Decisions**:
- Use Testcontainers for integration tests (real PostgreSQL, Redis)
- Mock OPA in unit tests, use real OPA in integration tests
- E2E tests run in CI on every PR
- Load tests run weekly, must maintain SLOs

### Decision 9: Observability Stack
**Choice**: Prometheus + Grafana + Structured Logging

**Rationale**:
- Industry standard, mature ecosystem
- Prometheus pull model fits Kubernetes well
- Structured logs (JSON) enable log aggregation
- OpenTelemetry for future distributed tracing

**Metrics to Track**:
- Authorization request rate, latency (p50, p95, p99)
- Cache hit/miss rates (L1, L2)
- OPA policy evaluation time
- Database query performance
- Error rates by endpoint

**Alerts**:
- P95 latency > 15ms for 5 minutes → Page on-call
- Error rate > 1% for 5 minutes → Page on-call
- Cache hit rate < 70% → Warning

### Decision 10: Deployment Strategy
**Choice**: Kubernetes with blue-green deployments

**Rationale**:
- Industry standard container orchestration
- Built-in service discovery, load balancing
- Blue-green enables zero-downtime deployments
- Rollback is instant (switch traffic back)

**Deployment Configuration**:
- Min 3 replicas for HA
- HPA: Scale from 3 to 10 based on CPU (70% target)
- Resource requests: 500m CPU, 1Gi memory
- Resource limits: 2 CPU, 4Gi memory
- Readiness probe: /actuator/health/readiness
- Liveness probe: /actuator/health/liveness

## Risks / Trade-offs

### Risk 1: OPA Performance Under Load
**Description**: OPA evaluation time may exceed budget under high load

**Probability**: Medium
**Impact**: High (blocks <10ms SLO)

**Mitigation**:
- Early load testing to validate OPA performance
- Optimize Rego policies for performance
- Increase cache TTLs if needed
- Consider pre-compiled bundles for faster loading

**Fallback**:
- If OPA is bottleneck, implement critical path in Java (trade-off: less flexible)

### Risk 2: Cache Invalidation Complexity
**Description**: Distributed cache invalidation may have race conditions or delays

**Probability**: Medium
**Impact**: Medium (stale authorization decisions)

**Mitigation**:
- Redis Pub/Sub for immediate invalidation broadcast
- Conservative TTLs (10s L1, 5min L2)
- Version policies (increment version on change)
- Circuit breaker to fail-closed on cache errors

**Fallback**:
- Reduce cache TTLs to minimize stale window
- Add manual cache flush API for emergencies

### Risk 3: Monolith Scalability Limits
**Description**: Single monolith may hit scaling limits before Phase 2

**Probability**: Low
**Impact**: Medium (need earlier microservices migration)

**Mitigation**:
- Design with clear module boundaries from start
- Use interfaces for all inter-module communication
- Measure performance regularly to detect issues early
- Stateless design enables horizontal scaling

**Fallback**:
- Extract authorization-core as first microservice if bottleneck identified
- Other modules can follow incrementally

### Risk 4: PostgreSQL Query Performance
**Description**: Complex RBAC queries may be slow on large datasets

**Probability**: Medium
**Impact**: Medium (affects API latency)

**Mitigation**:
- Comprehensive indexing strategy
- Query optimization with EXPLAIN ANALYZE
- Connection pooling (HikariCP)
- Read replicas for query load distribution

**Fallback**:
- Denormalize critical query paths
- Add materialized views for complex joins
- Migrate to TimescaleDB earlier if needed

### Risk 5: Team Learning Curve (OPA/Rego)
**Description**: Team unfamiliar with OPA and Rego language

**Probability**: High
**Impact**: Low (slows initial development)

**Mitigation**:
- Dedicated learning sprint (week 1)
- Pair programming for Rego development
- Build library of reusable Rego patterns
- External expert consultation available

**No Fallback**: OPA/Rego is core to architecture decision

## Migration Plan
N/A - This is the initial implementation with no migration from legacy system.

However, pilot applications will migrate from their existing authorization logic:

### Pilot Migration Steps
1. **Week 1**: Pilot team sets up development environment
2. **Week 2**: Model existing roles/permissions in auth platform
3. **Week 3**: Integrate SDK and replace authorization checks
4. **Week 4**: Parallel run (both old and new auth) with comparison
5. **Week 5**: Switch to auth platform, keep old code for rollback
6. **Week 6**: Remove old authorization code after validation

### Rollback Plan
If critical issue in production:
1. **Immediate**: Route traffic to blue environment (old version)
2. **Within 1 hour**: Identify and fix issue
3. **Within 4 hours**: Deploy hotfix to green environment
4. **After validation**: Route traffic back to green

## Open Questions

### Q1: Should we support custom attributes in MVP?
**Status**: ⚠️ To Be Decided
**Context**: Basic RBAC doesn't need attributes, but users may want custom fields
**Options**:
- A) No custom attributes in MVP (simplest)
- B) Allow JSONB storage but don't use in policies yet
- C) Full ABAC support (contradicts MVP scope)

**Recommendation**: Option B - store attributes but defer policy evaluation to Phase 2

### Q2: How to handle time-limited role assignments in MVP?
**Status**: ⚠️ To Be Decided
**Context**: Spec includes expires_at field, but implementation is complex
**Options**:
- A) Store expires_at but don't enforce (manual cleanup)
- B) Background job runs hourly to revoke expired roles
- C) Check expiration on every authorization (performance impact)

**Recommendation**: Option B - hourly cleanup job is acceptable for MVP

### Q3: Should we include policy testing API in MVP?
**Status**: ⚠️ To Be Decided
**Context**: Testing is in spec but may be low priority for pilots
**Options**:
- A) Skip testing API, use OPA playground for testing
- B) Include basic test endpoint (POST /policies/{id}/test)
- C) Full test suite management (too complex for MVP)

**Recommendation**: Option B - simple test endpoint is valuable and not complex

### Q4: What level of audit log detail for authorization decisions?
**Status**: ⚠️ To Be Decided
**Context**: Balance between compliance needs and storage/performance
**Options**:
- A) Minimal: user, resource, decision, timestamp
- B) Standard: + evaluated policies, decision reason
- C) Verbose: + full request/response payload

**Recommendation**: Option B - Standard detail strikes right balance

### Q5: Should we implement API rate limiting per user or per API key?
**Status**: ⚠️ To Be Decided
**Context**: API key is simpler, per-user is more granular
**Options**:
- A) Rate limit by API key only
- B) Rate limit by user ID (from token)
- C) Both (complex)

**Recommendation**: Option A - API key rate limiting for MVP

---

**Decision Deadline**: All open questions to be resolved by end of Week 1
**Decision Makers**: Tech Lead + Product Owner
**Review Cycle**: Design review after Week 2, 6, and 10
