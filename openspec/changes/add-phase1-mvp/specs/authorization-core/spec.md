# Authorization Core Spec Delta (Phase 1 MVP)

## ADDED Requirements

### Requirement: REST-Only Authorization API
The system SHALL provide REST API for authorization decisions in MVP.

#### Scenario: Basic authorization check via REST
- **WHEN** a client sends POST /v1/authorize with subject, action, resource
- **THEN** the system evaluates RBAC policies
- **AND** returns decision within 10ms (p95) for cached requests
- **AND** returns decision within 50ms (p95) for uncached requests
- **AND** logs the decision to audit trail

#### Scenario: Simple batch authorization
- **WHEN** a client sends up to 10 authorization requests in one call
- **THEN** the system processes requests in parallel
- **AND** returns individual decision for each request
- **AND** completes within 100ms (p95)

### Requirement: Two-Layer Caching (Simplified)
The system SHALL implement L1 (Caffeine) and L2 (Redis) caching.

#### Scenario: L1 cache hit (in-memory)
- **WHEN** authorization decision is in application cache
- **THEN** response time is under 2ms
- **AND** no database or OPA call is made
- **AND** cache hit metric is incremented

#### Scenario: L2 cache hit (Redis)
- **WHEN** L1 misses but L2 (Redis) has the entry
- **THEN** response time is under 5ms
- **AND** entry is promoted to L1 cache
- **AND** L2 hit metric is incremented

### Requirement: Basic Rate Limiting
The system SHALL implement per-API-key rate limiting.

#### Scenario: Rate limit enforcement
- **WHEN** an API key exceeds 1,000 requests per minute
- **THEN** subsequent requests return HTTP 429
- **AND** Retry-After header indicates wait time
- **AND** violation is logged

## MODIFIED Requirements

### Requirement: API Protocol Support
The system SHALL support REST API only in MVP (MODIFIED from REST + gRPC).

**Original**: Provide both REST and gRPC APIs
**Modified**: REST API only; gRPC deferred to Phase 2

#### Scenario: REST authorization endpoint
- **WHEN** client calls POST /v1/authorize
- **THEN** system processes request via HTTP
- **AND** returns JSON response
- **AND** supports standard HTTP headers (auth, content-type)

### Requirement: Decision Reasoning (Simplified)
The system SHALL provide basic reasoning in MVP.

**Original**: Comprehensive reasoning with all evaluated policies, attribute usage, and suggestions
**Modified**: Simple reasoning showing which role(s) granted access

#### Scenario: Allow decision with basic reasoning
- **WHEN** authorization is allowed
- **THEN** response includes list of roles that granted access
- **AND** includes the permission that matched
- **AND** omits detailed policy evaluation trace (deferred to Phase 2)

## REMOVED Requirements

### Requirement: gRPC API Support
**Removed from Phase 1 MVP** - Deferred to Phase 2

**Reason**: REST API is sufficient for initial integrations. gRPC adds protocol complexity and requires additional client library development.

**Migration**: When gRPC is added in Phase 2, both protocols will coexist with identical semantics.

### Requirement: Advanced Context-Aware Authorization
**Removed from Phase 1 MVP** - Simplified context support only

**Reason**: Time-based, IP-based, and device-based authorization require additional context processing complexity. Basic context (timestamp, IP) is logged but not used in policy evaluation.

**Migration**: Phase 2 will add context attributes to policy evaluation engine.

### Requirement: Streaming Authorization
**Removed from Phase 1 MVP** - Deferred to Phase 2+

**Reason**: Batch API covers most use cases. True streaming (bidirectional gRPC streams) is niche requirement for MVP.

## ADDED Non-Functional Requirements

### Performance (MVP-Specific)
- Authorization API SHALL respond within 10ms (p95) for cached requests
- Authorization API SHALL respond within 50ms (p95) for uncached requests
- System SHALL support 10,000 requests per second sustained
- Batch API SHALL process 10 requests within 100ms (p95)

### Availability (MVP-Specific)
- Uptime SHALL be 99.9% (relaxed from 99.99%)
- Acceptable downtime: 43.2 minutes per month
- Failover SHALL occur within 30 seconds (relaxed from 10 seconds)

### Scalability (MVP-Specific)
- Horizontal scaling from 3 to 10 replicas via HPA
- Auto-scaling trigger at 70% CPU utilization
- Support up to 100,000 concurrent sessions
