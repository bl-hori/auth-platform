# Authorization Core Specification

## Overview
The Authorization Core capability provides high-performance authorization decision services, supporting REST and gRPC APIs with sub-10ms latency.

## Requirements

### Requirement: Authorization Decision API
The system SHALL provide APIs to make authorization decisions based on subject, action, resource, and context.

#### Scenario: Successful authorization check
- **WHEN** a client requests authorization with valid subject, action, and resource
- **THEN** the system evaluates applicable policies
- **AND** returns decision (allow/deny) within 10ms (p95)
- **AND** includes decision reasoning if requested
- **AND** logs the decision to audit trail

#### Scenario: Authorization with attributes
- **WHEN** a client includes subject and resource attributes in the request
- **THEN** the system evaluates ABAC policies using the attributes
- **AND** returns decision based on attribute matching
- **AND** includes which attributes were used in decision reasoning

#### Scenario: Missing required fields
- **WHEN** a client omits required fields (subject or action)
- **THEN** the system returns HTTP 400 Bad Request
- **AND** includes validation error details
- **AND** does NOT log to audit trail

### Requirement: Batch Authorization
The system SHALL support batch authorization requests to minimize network overhead.

#### Scenario: Batch authorization success
- **WHEN** a client submits multiple authorization requests in one API call
- **THEN** the system processes all requests in parallel
- **AND** returns individual decisions for each request
- **AND** includes summary statistics (total, allowed, denied)
- **AND** completes within 50ms for 10 requests (p95)

#### Scenario: Batch with partial failures
- **WHEN** a batch contains some invalid requests
- **THEN** the system processes valid requests normally
- **AND** returns errors for invalid requests
- **AND** includes error details for each failed request
- **AND** does NOT fail the entire batch

### Requirement: Multi-Layer Caching
The system SHALL implement multi-layer caching to achieve sub-10ms latency targets.

#### Scenario: Cache hit on L1 (in-memory)
- **WHEN** an authorization decision is requested for a cached entry
- **THEN** the decision is returned from application memory cache
- **AND** response time is under 2ms
- **AND** cache hit is recorded in metrics

#### Scenario: Cache hit on L2 (Redis)
- **WHEN** an L1 cache miss occurs but L2 has the entry
- **THEN** the decision is retrieved from Redis
- **AND** response time is under 5ms
- **AND** the entry is promoted to L1 cache

#### Scenario: Cache miss - full policy evaluation
- **WHEN** both L1 and L2 cache miss
- **THEN** the system evaluates policies from PDP
- **AND** response time is under 10ms (p95)
- **AND** the result is stored in both L1 and L2 caches

### Requirement: Cache Invalidation
The system SHALL invalidate caches when policies or permissions change to ensure consistency.

#### Scenario: Policy update invalidation
- **WHEN** a policy is updated or activated
- **THEN** all related cache entries are invalidated immediately
- **AND** invalidation propagates to all instances within 1 second
- **AND** subsequent requests use the updated policy

#### Scenario: User role change invalidation
- **WHEN** a user's role assignment changes
- **THEN** all authorization cache entries for that user are cleared
- **AND** the user's next request uses updated permissions
- **AND** the cache invalidation is logged

### Requirement: Decision Reasoning
The system SHALL provide detailed reasoning for authorization decisions to aid debugging and compliance.

#### Scenario: Allow decision with reasoning
- **WHEN** an authorization request is allowed
- **THEN** the response includes list of satisfied policies
- **AND** explains why each policy allowed the action
- **AND** includes relevant attribute values used in decision

#### Scenario: Deny decision with reasoning
- **WHEN** an authorization request is denied
- **THEN** the response explains which policies were evaluated
- **AND** identifies why each policy denied the action
- **AND** suggests required permissions or attributes

### Requirement: High Availability
The system SHALL maintain 99.99% uptime through redundancy and failover mechanisms.

#### Scenario: PDP instance failure
- **WHEN** a PDP instance becomes unavailable
- **THEN** requests are automatically routed to healthy instances
- **AND** no authorization requests fail
- **AND** response time increases by less than 5ms
- **AND** the failure is detected within 10 seconds

#### Scenario: Redis cache failure
- **WHEN** Redis becomes unavailable
- **THEN** the system falls back to direct PDP evaluation
- **AND** authorization decisions continue to work
- **AND** response times may increase but stay under 15ms (p95)
- **AND** alerts are triggered for operations team

### Requirement: Rate Limiting
The system SHALL enforce rate limits to prevent abuse and ensure fair resource usage.

#### Scenario: Within rate limit
- **WHEN** a client makes requests within their quota
- **THEN** all requests are processed normally
- **AND** remaining quota is included in response headers
- **AND** no throttling occurs

#### Scenario: Rate limit exceeded
- **WHEN** a client exceeds their rate limit
- **THEN** the system returns HTTP 429 Too Many Requests
- **AND** includes Retry-After header with wait time
- **AND** logs the rate limit violation
- **AND** does NOT process the authorization request

### Requirement: gRPC Support
The system SHALL provide gRPC API for low-latency service-to-service communication.

#### Scenario: gRPC authorization request
- **WHEN** a service makes a gRPC authorization call
- **THEN** the system processes using Protocol Buffers
- **AND** response time is under 5ms (p95)
- **AND** supports bidirectional streaming for batch requests

#### Scenario: gRPC error handling
- **WHEN** a gRPC request fails validation
- **THEN** the system returns appropriate gRPC status code
- **AND** includes error details in metadata
- **AND** maintains connection for subsequent requests

### Requirement: Context-Aware Authorization
The system SHALL support context-based authorization using environmental attributes.

#### Scenario: Time-based authorization
- **WHEN** a policy includes time-of-day restrictions
- **THEN** the system evaluates based on current timestamp
- **AND** denies access outside allowed time windows
- **AND** includes time information in decision reasoning

#### Scenario: IP-based authorization
- **WHEN** a policy includes IP address restrictions
- **THEN** the system extracts client IP from request context
- **AND** evaluates against allowed IP ranges or geolocation
- **AND** denies access from unauthorized networks

#### Scenario: Device-based authorization
- **WHEN** a policy requires specific device attributes
- **THEN** the system evaluates device fingerprint or user-agent
- **AND** allows only from approved device types
- **AND** logs device information in audit trail

### Requirement: Monitoring and Metrics
The system SHALL expose comprehensive metrics for observability.

#### Scenario: Request metrics
- **WHEN** authorization requests are processed
- **THEN** the system records request count, latency, and decision
- **AND** exposes metrics via Prometheus endpoint
- **AND** includes labels for organization, policy type, and decision

#### Scenario: Cache metrics
- **WHEN** cache operations occur
- **THEN** the system tracks hit rate, miss rate, and evictions
- **AND** provides metrics per cache layer (L1, L2)
- **AND** enables alerting on degraded cache performance

## Non-Functional Requirements

### Performance
- Authorization API SHALL respond within 10ms (p95)
- The system SHALL support 100,000+ requests per second
- Batch API SHALL process 10 requests within 50ms (p95)

### Availability
- Uptime SHALL be 99.99% (< 52.56 minutes downtime per year)
- Failover SHALL occur within 10 seconds
- No single point of failure in authorization path

### Security
- All API calls SHALL be authenticated
- TLS 1.3+ SHALL be required for all connections
- mTLS SHALL be used for service-to-service communication

### Scalability
- The system SHALL scale horizontally to handle increased load
- Auto-scaling SHALL trigger at 70% CPU/memory utilization
- Maximum response time degradation during scaling: 20%
