-- ============================================================================
-- Performance Optimization Indexes
-- ============================================================================
-- Version: 2
-- Description: Additional indexes for query performance optimization
-- Author: System
-- Date: 2025-10-26
-- ============================================================================

-- ============================================================================
-- Organizations - Additional Indexes
-- ============================================================================

-- Soft delete queries optimization
CREATE INDEX IF NOT EXISTS idx_organizations_deleted_at
    ON organizations(deleted_at)
    WHERE deleted_at IS NOT NULL;

COMMENT ON INDEX idx_organizations_deleted_at IS
    'Optimize queries for soft-deleted organizations';

-- ============================================================================
-- Users - Additional Indexes
-- ============================================================================

-- Organization + email composite index for login queries
CREATE INDEX IF NOT EXISTS idx_users_org_email
    ON users(organization_id, email)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX idx_users_org_email IS
    'Optimize user authentication queries by organization and email';

-- ============================================================================
-- Roles - Additional Indexes
-- ============================================================================

-- Organization + name composite index for role lookup
CREATE INDEX IF NOT EXISTS idx_roles_org_name
    ON roles(organization_id, name)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX idx_roles_org_name IS
    'Optimize role lookup by organization and name';

-- ============================================================================
-- Policies - Additional Indexes
-- ============================================================================

-- Organization + status composite index for active policy queries
CREATE INDEX IF NOT EXISTS idx_policies_org_status
    ON policies(organization_id, status)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX idx_policies_org_status IS
    'Optimize queries for active policies by organization';

-- ============================================================================
-- Policy Versions - Additional Indexes
-- ============================================================================

-- Policy + version composite index for version lookup
CREATE INDEX IF NOT EXISTS idx_policy_versions_policy_version
    ON policy_versions(policy_id, version);

COMMENT ON INDEX idx_policy_versions_policy_version IS
    'Optimize version lookup for specific policy versions';

-- Checksum index for deduplication queries
CREATE INDEX IF NOT EXISTS idx_policy_versions_checksum
    ON policy_versions(policy_id, checksum);

COMMENT ON INDEX idx_policy_versions_checksum IS
    'Optimize checksum-based deduplication queries';

-- ============================================================================
-- Permissions - Additional Indexes
-- ============================================================================

-- Organization + action composite index for permission checks
CREATE INDEX IF NOT EXISTS idx_permissions_org_action
    ON permissions(organization_id, action);

COMMENT ON INDEX idx_permissions_org_action IS
    'Optimize permission checks by organization and action';

-- Resource type + action for resource-level permission queries
CREATE INDEX IF NOT EXISTS idx_permissions_resource_action
    ON permissions(resource_type, action);

COMMENT ON INDEX idx_permissions_resource_action IS
    'Optimize resource-level permission queries';

-- ============================================================================
-- Audit Logs - Additional Indexes
-- ============================================================================

-- Organization + timestamp composite index for time-range queries (DESC for recent-first)
CREATE INDEX IF NOT EXISTS idx_audit_logs_org_timestamp
    ON audit_logs(organization_id, timestamp DESC);

COMMENT ON INDEX idx_audit_logs_org_timestamp IS
    'Optimize time-range queries for audit logs by organization (recent first)';

-- Actor (user) + timestamp composite index for user activity queries
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_timestamp
    ON audit_logs(actor_id, timestamp DESC)
    WHERE actor_id IS NOT NULL;

COMMENT ON INDEX idx_audit_logs_actor_timestamp IS
    'Optimize user activity tracking queries (recent first)';

-- Decision index for authorization decision analysis
CREATE INDEX IF NOT EXISTS idx_audit_logs_decision
    ON audit_logs(decision)
    WHERE decision IS NOT NULL;

COMMENT ON INDEX idx_audit_logs_decision IS
    'Optimize queries filtering by authorization decision (allow/deny/error)';

-- Event type + timestamp for event-specific time-range queries
CREATE INDEX IF NOT EXISTS idx_audit_logs_event_timestamp
    ON audit_logs(event_type, timestamp DESC);

COMMENT ON INDEX idx_audit_logs_event_timestamp IS
    'Optimize event-specific time-range queries';

-- Organization + decision + timestamp for security monitoring
CREATE INDEX IF NOT EXISTS idx_audit_logs_org_decision_time
    ON audit_logs(organization_id, decision, timestamp DESC)
    WHERE decision IS NOT NULL;

COMMENT ON INDEX idx_audit_logs_org_decision_time IS
    'Optimize security monitoring queries (denied access tracking)';

-- ============================================================================
-- User Roles - Additional Indexes
-- ============================================================================

-- Organization-scoped user role queries (via user)
CREATE INDEX IF NOT EXISTS idx_user_roles_org_user
    ON user_roles(user_id, role_id, granted_at);

COMMENT ON INDEX idx_user_roles_org_user IS
    'Optimize active role assignment queries by user';

-- Resource-scoped role assignments with expiration check
CREATE INDEX IF NOT EXISTS idx_user_roles_resource_expires
    ON user_roles(resource_type, resource_id, expires_at)
    WHERE resource_type IS NOT NULL AND expires_at IS NOT NULL;

COMMENT ON INDEX idx_user_roles_resource_expires IS
    'Optimize resource-scoped role queries with expiration check';

-- ============================================================================
-- Role Permissions - Additional Indexes
-- ============================================================================

-- Composite index for efficient permission resolution
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_perm
    ON role_permissions(role_id, permission_id);

COMMENT ON INDEX idx_role_permissions_role_perm IS
    'Optimize active permission resolution for roles';

-- ============================================================================
-- Statistics Update
-- ============================================================================

-- Analyze tables to update query planner statistics
ANALYZE organizations;
ANALYZE users;
ANALYZE roles;
ANALYZE permissions;
ANALYZE user_roles;
ANALYZE role_permissions;
ANALYZE policies;
ANALYZE policy_versions;
ANALYZE audit_logs;

-- ============================================================================
-- Verification Queries
-- ============================================================================

-- View all indexes created by this migration
COMMENT ON COLUMN pg_indexes.indexname IS
    'Use SELECT * FROM pg_indexes WHERE schemaname = ''public'' ORDER BY tablename, indexname to view all indexes';
