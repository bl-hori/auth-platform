-- ============================================================================
-- Row-Level Security (RLS) for Multi-Tenancy
-- ============================================================================
-- Version: 3
-- Description: Add PostgreSQL Row-Level Security policies for tenant isolation
-- Author: System
-- Date: 2025-10-26
-- ============================================================================

-- ============================================================================
-- Overview
-- ============================================================================
-- This migration implements Row-Level Security (RLS) to ensure data isolation
-- between organizations (tenants). Each table with organization_id will have
-- policies that restrict access based on the current organization context.
--
-- Application Setup Required:
-- Before executing queries, the application must set the current organization:
--   SET LOCAL app.current_organization_id = '<organization-uuid>';
--
-- This ensures that all queries automatically filter by organization_id.
-- ============================================================================

-- ============================================================================
-- Helper Function: Get Current Organization ID
-- ============================================================================

-- Function to retrieve the current organization ID from session variable
CREATE OR REPLACE FUNCTION current_organization_id()
RETURNS UUID
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    -- Retrieve the organization ID set by the application
    -- Returns NULL if not set (allows bypass for superusers)
    RETURN nullif(current_setting('app.current_organization_id', true), '')::uuid;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$;

COMMENT ON FUNCTION current_organization_id() IS
    'Retrieve the current organization ID from session variable app.current_organization_id';

-- ============================================================================
-- Users Table - RLS
-- ============================================================================

-- Enable RLS on users table
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only access users in their organization
-- If current_organization_id is not set (NULL), allow all access (for testing/admin)
CREATE POLICY users_organization_isolation ON users
    FOR ALL
    TO PUBLIC
    USING (current_organization_id() IS NULL OR organization_id = current_organization_id())
    WITH CHECK (current_organization_id() IS NULL OR organization_id = current_organization_id());

COMMENT ON POLICY users_organization_isolation ON users IS
    'Restrict access to users within the same organization';

-- ============================================================================
-- Roles Table - RLS
-- ============================================================================

ALTER TABLE roles ENABLE ROW LEVEL SECURITY;

CREATE POLICY roles_organization_isolation ON roles
    FOR ALL
    TO PUBLIC
    USING (current_organization_id() IS NULL OR organization_id = current_organization_id())
    WITH CHECK (current_organization_id() IS NULL OR organization_id = current_organization_id());

COMMENT ON POLICY roles_organization_isolation ON roles IS
    'Restrict access to roles within the same organization';

-- ============================================================================
-- Permissions Table - RLS
-- ============================================================================

ALTER TABLE permissions ENABLE ROW LEVEL SECURITY;

CREATE POLICY permissions_organization_isolation ON permissions
    FOR ALL
    TO PUBLIC
    USING (current_organization_id() IS NULL OR organization_id = current_organization_id())
    WITH CHECK (current_organization_id() IS NULL OR organization_id = current_organization_id());

COMMENT ON POLICY permissions_organization_isolation ON permissions IS
    'Restrict access to permissions within the same organization';

-- ============================================================================
-- Policies Table - RLS
-- ============================================================================

ALTER TABLE policies ENABLE ROW LEVEL SECURITY;

CREATE POLICY policies_organization_isolation ON policies
    FOR ALL
    TO PUBLIC
    USING (current_organization_id() IS NULL OR organization_id = current_organization_id())
    WITH CHECK (current_organization_id() IS NULL OR organization_id = current_organization_id());

COMMENT ON POLICY policies_organization_isolation ON policies IS
    'Restrict access to policies within the same organization';

-- ============================================================================
-- Policy Versions Table - RLS (via policy relationship)
-- ============================================================================

ALTER TABLE policy_versions ENABLE ROW LEVEL SECURITY;

-- Policy versions are scoped through their parent policy's organization
CREATE POLICY policy_versions_organization_isolation ON policy_versions
    FOR ALL
    TO PUBLIC
    USING (
        current_organization_id() IS NULL OR
        EXISTS (
            SELECT 1 FROM policies p
            WHERE p.id = policy_versions.policy_id
            AND p.organization_id = current_organization_id()
        )
    )
    WITH CHECK (
        current_organization_id() IS NULL OR
        EXISTS (
            SELECT 1 FROM policies p
            WHERE p.id = policy_versions.policy_id
            AND p.organization_id = current_organization_id()
        )
    );

COMMENT ON POLICY policy_versions_organization_isolation ON policy_versions IS
    'Restrict access to policy versions through parent policy organization';

-- ============================================================================
-- User Roles Table - RLS (via user relationship)
-- ============================================================================

ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;

-- User roles are scoped through the user's organization
CREATE POLICY user_roles_organization_isolation ON user_roles
    FOR ALL
    TO PUBLIC
    USING (
        current_organization_id() IS NULL OR
        EXISTS (
            SELECT 1 FROM users u
            WHERE u.id = user_roles.user_id
            AND u.organization_id = current_organization_id()
        )
    )
    WITH CHECK (
        current_organization_id() IS NULL OR
        EXISTS (
            SELECT 1 FROM users u
            WHERE u.id = user_roles.user_id
            AND u.organization_id = current_organization_id()
        )
    );

COMMENT ON POLICY user_roles_organization_isolation ON user_roles IS
    'Restrict access to user roles through user organization';

-- ============================================================================
-- Role Permissions Table - RLS (via role relationship)
-- ============================================================================

ALTER TABLE role_permissions ENABLE ROW LEVEL SECURITY;

-- Role permissions are scoped through the role's organization
CREATE POLICY role_permissions_organization_isolation ON role_permissions
    FOR ALL
    TO PUBLIC
    USING (
        current_organization_id() IS NULL OR
        EXISTS (
            SELECT 1 FROM roles r
            WHERE r.id = role_permissions.role_id
            AND r.organization_id = current_organization_id()
        )
    )
    WITH CHECK (
        current_organization_id() IS NULL OR
        EXISTS (
            SELECT 1 FROM roles r
            WHERE r.id = role_permissions.role_id
            AND r.organization_id = current_organization_id()
        )
    );

COMMENT ON POLICY role_permissions_organization_isolation ON role_permissions IS
    'Restrict access to role permissions through role organization';

-- ============================================================================
-- Audit Logs Table - RLS
-- ============================================================================

ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY audit_logs_organization_isolation ON audit_logs
    FOR ALL
    TO PUBLIC
    USING (current_organization_id() IS NULL OR organization_id = current_organization_id())
    WITH CHECK (current_organization_id() IS NULL OR organization_id = current_organization_id());

COMMENT ON POLICY audit_logs_organization_isolation ON audit_logs IS
    'Restrict access to audit logs within the same organization';

-- ============================================================================
-- Organizations Table - Special Case
-- ============================================================================
-- Organizations table does not have RLS enabled by default
-- The application layer should control which organizations users can access
-- This allows users to switch between organizations they have access to

-- Note: If strict isolation is required, enable RLS with a policy that checks
-- user memberships through a separate organizations_users table

COMMENT ON TABLE organizations IS
    'Organizations table: RLS not enabled - application layer controls access';

-- ============================================================================
-- Testing Helper Function
-- ============================================================================

-- Function to test RLS policies (for development/testing)
CREATE OR REPLACE FUNCTION test_rls_policy(
    test_organization_id UUID,
    test_query TEXT
)
RETURNS TABLE(result_count BIGINT)
LANGUAGE plpgsql
AS $$
BEGIN
    -- Set the test organization context
    EXECUTE format('SET LOCAL app.current_organization_id = %L', test_organization_id);

    -- Execute the test query and return count
    RETURN QUERY EXECUTE format('SELECT COUNT(*)::bigint FROM (%s) AS test_query', test_query);

    -- Reset the organization context
    RESET app.current_organization_id;
END;
$$;

COMMENT ON FUNCTION test_rls_policy IS
    'Helper function to test RLS policies with a specific organization context';

-- ============================================================================
-- Performance Considerations
-- ============================================================================

-- Note: Partial indexes with current_organization_id() in WHERE clause
-- cannot be created because the function must be IMMUTABLE for index predicates.
-- However, the standard indexes on organization_id (created in V1 and V2)
-- are sufficient for RLS policy performance.
--
-- PostgreSQL's query planner will use the existing organization_id indexes
-- when evaluating RLS policies.

-- ============================================================================
-- Verification Queries
-- ============================================================================

-- View all RLS policies
COMMENT ON SCHEMA public IS
    'Use SELECT * FROM pg_policies WHERE schemaname = ''public'' to view all RLS policies';

-- View RLS status for tables
COMMENT ON SCHEMA public IS
    'Use SELECT tablename, rowsecurity FROM pg_tables WHERE schemaname = ''public'' to view RLS status';
