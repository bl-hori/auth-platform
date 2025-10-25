# Audit Logging Spec Delta (Phase 1 MVP)

## ADDED Requirements

### Requirement: PostgreSQL-Based Audit Storage
The system SHALL store audit logs in PostgreSQL with table partitioning.

#### Scenario: Log authorization decision to PostgreSQL
- **WHEN** an authorization decision is made
- **THEN** the system writes to audit_logs table asynchronously
- **AND** uses monthly partitions based on timestamp
- **AND** write completes within 10ms (async, doesn't block response)

#### Scenario: Query recent audit logs
- **WHEN** querying logs from the last 7 days
- **THEN** the system uses current partition only
- **AND** query completes within 1 second
- **AND** results are paginated (default 50, max 500)

### Requirement: Basic Audit Log Search
The system SHALL support simple time-range and filter-based search.

#### Scenario: Search by time range
- **WHEN** filtering audit logs by date range
- **THEN** the system queries relevant partitions only
- **AND** returns results sorted by timestamp descending
- **AND** includes pagination metadata

#### Scenario: Filter by user
- **WHEN** searching for all actions by a specific user
- **THEN** the system uses user_id index
- **AND** returns all authorization and admin events
- **AND** completes within 2 seconds for 90-day range

#### Scenario: Filter by decision outcome
- **WHEN** filtering for denied authorization attempts
- **THEN** the system filters by decision field
- **AND** highlights potential security issues
- **AND** supports exporting to CSV for further analysis

### Requirement: Simple CSV Export
The system SHALL support exporting audit logs to CSV format.

#### Scenario: Export filtered results
- **WHEN** an auditor exports filtered audit logs
- **THEN** the system generates CSV with all displayed columns
- **AND** limits export to 10,000 records maximum
- **AND** download completes within 30 seconds

### Requirement: 90-Day Retention Policy
The system SHALL retain audit logs for 90 days in hot storage.

#### Scenario: Automatic partition cleanup
- **WHEN** a partition exceeds 90 days old
- **THEN** a background job drops the partition
- **AND** the cleanup is logged itself
- **AND** cleanup runs daily at 2 AM

## MODIFIED Requirements

### Requirement: Audit Log Storage Technology
The system SHALL use PostgreSQL in MVP (MODIFIED from TimescaleDB).

**Original**: Use TimescaleDB for optimized time-series queries and compression
**Modified**: Use PostgreSQL with native table partitioning; migrate to TimescaleDB in Phase 2

#### Scenario: Monthly partition management
- **WHEN** a new month begins
- **THEN** the system auto-creates new partition
- **AND** partitions are created proactively (1 month ahead)
- **AND** old partitions are dropped after retention period

### Requirement: Audit Log Streaming (Simplified)
The system SHALL support basic log export only, not real-time streaming.

**Original**: Real-time streaming to SIEM via Syslog, HTTP webhooks, and Kafka
**Modified**: Batch export only via CSV download; streaming deferred to Phase 2

#### Scenario: Manual log export for SIEM
- **WHEN** exporting logs for SIEM ingestion
- **THEN** administrator downloads CSV
- **AND** manually imports to SIEM system
- **AND** process repeated as needed (not automated)

### Requirement: Tamper-Proof Logging (Simplified)
The system SHALL use append-only table without cryptographic integrity.

**Original**: Cryptographic hash chains for tamper detection
**Modified**: Database-level append-only constraint only; hash chains deferred to Phase 2

#### Scenario: Append-only audit table
- **WHEN** audit log is written
- **THEN** PostgreSQL enforces no UPDATE/DELETE via triggers
- **AND** only INSERT operations are allowed
- **AND** attempts to modify logs are blocked and alerted

## REMOVED Requirements

### Requirement: Real-Time SIEM Streaming
**Removed from Phase 1 MVP** - Deferred to Phase 2

**Reason**: Requires implementing multiple protocols (Syslog, HTTP webhook, Kafka), retry logic, and monitoring. Manual export is sufficient for MVP compliance needs.

**Migration**: When streaming is added in Phase 2:
- Historical logs remain in PostgreSQL
- Future logs stream in real-time
- Batch export remains available as alternative

### Requirement: Cryptographic Hash Chains
**Removed from Phase 1 MVP** - Deferred to Phase 2

**Reason**: Adds complexity in log writing and verification. Database-level append-only protection provides basic tamper resistance for MVP.

**Migration**: When added in Phase 2:
- New logs will include hash pointers
- Historical logs will be hashed in batch process
- Verification API will be implemented

### Requirement: Automated Compliance Reports
**Removed from Phase 1 MVP** - Deferred to Phase 3

**Reason**: Report generation (PDF, charts, executive summaries) requires significant development. Manual CSV export allows auditors to create reports using familiar tools.

**Migration**: Phase 3 will add report templates and scheduled generation.

### Requirement: Anomaly Detection
**Removed from Phase 1 MVP** - Deferred to Phase 2

**Reason**: Requires baseline profiling, statistical analysis, and alerting infrastructure. Manual review of denied authorization attempts is sufficient for MVP.

**Migration**: Phase 2 will add ML-based anomaly detection using historical audit logs.

### Requirement: Cold Storage Archival
**Removed from Phase 1 MVP** - Deferred to Phase 2

**Reason**: 90-day retention in PostgreSQL is sufficient for MVP compliance. Long-term archival (S3 Glacier) adds complexity.

**Migration**: When added in Phase 2:
- Partitions older than 90 days will be exported to S3
- Metadata index will be maintained for discovery
- Restore process will be implemented

## ADDED Non-Functional Requirements

### Performance (MVP-Specific)
- Async log writes SHALL complete within 10ms (p99)
- Log queries SHALL return within 2 seconds for 30-day range
- Pagination SHALL handle up to 10,000 results efficiently

### Storage (MVP-Specific)
- Estimate 1KB per audit log entry
- 90-day retention for 10,000 req/day = ~900MB per organization
- Plan for 10 organizations = ~10GB total audit storage

### Availability (MVP-Specific)
- Audit logging SHALL NOT block authorization responses
- Async logging with in-memory buffer (10,000 entries)
- Logs persisted every 5 seconds or on buffer full

### Security (MVP-Specific)
- Audit table protected by database trigger (no UPDATE/DELETE)
- Access to audit logs requires elevated admin privileges
- Audit log access itself is audited
