-- ============================================================================
-- Authorization Platform - Core Schema Migration
-- Version: 1
-- Description: Create core tables for organizations, users, roles, permissions,
--              policies, and audit logging
-- ============================================================================

-- Enable UUID extension for generating UUIDs
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- Organization Management
-- ============================================================================

CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'deleted')),
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT organizations_name_unique UNIQUE (name)
);

CREATE INDEX idx_organizations_status ON organizations(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_organizations_created_at ON organizations(created_at);

COMMENT ON TABLE organizations IS 'Multi-tenant organizations for authorization platform';
COMMENT ON COLUMN organizations.settings IS 'Organization-specific configuration (JSONB)';

-- ============================================================================
-- User Identity Management
-- ============================================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    display_name VARCHAR(255),
    external_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'inactive', 'suspended', 'deleted')),
    attributes JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT users_org_email_unique UNIQUE (organization_id, email),
    CONSTRAINT users_org_username_unique UNIQUE (organization_id, username)
);

CREATE INDEX idx_users_organization_id ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_external_id ON users(external_id) WHERE external_id IS NOT NULL;
CREATE INDEX idx_users_attributes ON users USING GIN (attributes);

COMMENT ON TABLE users IS 'User accounts within organizations';
COMMENT ON COLUMN users.attributes IS 'Custom user attributes for ABAC (Phase 2)';
COMMENT ON COLUMN users.external_id IS 'External IdP user ID for SCIM sync (Phase 2)';

-- ============================================================================
-- Role Management
-- ============================================================================

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    parent_role_id UUID REFERENCES roles(id) ON DELETE SET NULL,
    level INTEGER NOT NULL DEFAULT 0,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT roles_org_name_unique UNIQUE (organization_id, name),
    CONSTRAINT roles_level_check CHECK (level >= 0 AND level <= 10)
);

CREATE INDEX idx_roles_organization_id ON roles(organization_id);
CREATE INDEX idx_roles_parent_role_id ON roles(parent_role_id) WHERE parent_role_id IS NOT NULL;
CREATE INDEX idx_roles_is_system ON roles(is_system);

COMMENT ON TABLE roles IS 'Roles with hierarchy support for RBAC';
COMMENT ON COLUMN roles.parent_role_id IS 'Parent role for inheritance (hierarchy)';
COMMENT ON COLUMN roles.level IS 'Depth in role hierarchy (0=root, max 10)';
COMMENT ON COLUMN roles.is_system IS 'System-defined role that cannot be deleted';

-- ============================================================================
-- Permission Management
-- ============================================================================

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    resource_type VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    effect VARCHAR(50) NOT NULL DEFAULT 'allow' CHECK (effect IN ('allow', 'deny')),
    conditions JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT permissions_org_name_unique UNIQUE (organization_id, name),
    CONSTRAINT permissions_org_resource_action_unique UNIQUE (organization_id, resource_type, action)
);

CREATE INDEX idx_permissions_organization_id ON permissions(organization_id);
CREATE INDEX idx_permissions_resource_type ON permissions(resource_type);
CREATE INDEX idx_permissions_action ON permissions(action);

COMMENT ON TABLE permissions IS 'Fine-grained permissions defining allowed actions on resources';
COMMENT ON COLUMN permissions.resource_type IS 'Type of resource (e.g., document, project, api)';
COMMENT ON COLUMN permissions.action IS 'Action to perform (e.g., read, write, delete)';
COMMENT ON COLUMN permissions.conditions IS 'Additional conditions for ABAC (Phase 2)';

-- ============================================================================
-- User-Role Assignment (Join Table)
-- ============================================================================

CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    resource_type VARCHAR(255),
    resource_id VARCHAR(255),
    granted_by UUID REFERENCES users(id),
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT user_roles_unique UNIQUE (user_id, role_id, resource_type, resource_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_user_roles_resource ON user_roles(resource_type, resource_id) WHERE resource_type IS NOT NULL;
CREATE INDEX idx_user_roles_expires_at ON user_roles(expires_at) WHERE expires_at IS NOT NULL;

COMMENT ON TABLE user_roles IS 'Assignment of roles to users with optional resource scoping';
COMMENT ON COLUMN user_roles.resource_type IS 'Optional: Limit role to specific resource type';
COMMENT ON COLUMN user_roles.resource_id IS 'Optional: Limit role to specific resource instance';
COMMENT ON COLUMN user_roles.expires_at IS 'Optional: Time-limited role assignment';

-- ============================================================================
-- Role-Permission Assignment (Join Table)
-- ============================================================================

CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT role_permissions_unique UNIQUE (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

COMMENT ON TABLE role_permissions IS 'Assignment of permissions to roles';

-- ============================================================================
-- Policy Management
-- ============================================================================

CREATE TABLE policies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    policy_type VARCHAR(50) NOT NULL DEFAULT 'rego' CHECK (policy_type IN ('rego', 'cedar')),
    status VARCHAR(50) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'active', 'archived')),
    current_version INTEGER NOT NULL DEFAULT 1,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT policies_org_name_unique UNIQUE (organization_id, name)
);

CREATE INDEX idx_policies_organization_id ON policies(organization_id);
CREATE INDEX idx_policies_status ON policies(status);
CREATE INDEX idx_policies_type ON policies(policy_type);

COMMENT ON TABLE policies IS 'Policy definitions (Rego or Cedar)';
COMMENT ON COLUMN policies.policy_type IS 'Policy language: rego (OPA) or cedar (AWS Cedar)';
COMMENT ON COLUMN policies.status IS 'Lifecycle status: draft, active, or archived';

-- ============================================================================
-- Policy Version History
-- ============================================================================

CREATE TABLE policy_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    policy_id UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    content TEXT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    validation_status VARCHAR(50) NOT NULL DEFAULT 'pending' CHECK (validation_status IN ('pending', 'valid', 'invalid')),
    validation_errors JSONB,
    published_at TIMESTAMP WITH TIME ZONE,
    published_by UUID REFERENCES users(id),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT policy_versions_unique UNIQUE (policy_id, version)
);

CREATE INDEX idx_policy_versions_policy_id ON policy_versions(policy_id);
CREATE INDEX idx_policy_versions_validation_status ON policy_versions(validation_status);
CREATE INDEX idx_policy_versions_published_at ON policy_versions(published_at);

COMMENT ON TABLE policy_versions IS 'Version history for policies';
COMMENT ON COLUMN policy_versions.checksum IS 'SHA-256 hash of content for integrity verification';
COMMENT ON COLUMN policy_versions.validation_errors IS 'Syntax/semantic validation errors (JSONB)';

-- ============================================================================
-- Audit Logging (with Partitioning for Performance)
-- ============================================================================

CREATE TABLE audit_logs (
    id UUID DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    actor_id UUID REFERENCES users(id),
    actor_email VARCHAR(255),
    resource_type VARCHAR(255),
    resource_id VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    decision VARCHAR(50),
    decision_reason TEXT,
    request_data JSONB,
    response_data JSONB,
    ip_address VARCHAR(50),
    user_agent TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id, timestamp)
) PARTITION BY RANGE (timestamp);

CREATE INDEX idx_audit_logs_organization_id ON audit_logs(organization_id);
CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id) WHERE actor_id IS NOT NULL;
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id) WHERE resource_type IS NOT NULL;
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);

COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for all authorization decisions and admin actions';
COMMENT ON COLUMN audit_logs.event_type IS 'Type of event: authorization, admin_action, policy_change, etc.';
COMMENT ON COLUMN audit_logs.decision IS 'Authorization decision: allow, deny, error';

-- Create initial partitions for audit logs (full year 2025)
CREATE TABLE audit_logs_2025_01 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE audit_logs_2025_02 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE audit_logs_2025_03 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE TABLE audit_logs_2025_04 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');

CREATE TABLE audit_logs_2025_05 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');

CREATE TABLE audit_logs_2025_06 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');

CREATE TABLE audit_logs_2025_07 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');

CREATE TABLE audit_logs_2025_08 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');

CREATE TABLE audit_logs_2025_09 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');

CREATE TABLE audit_logs_2025_10 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');

CREATE TABLE audit_logs_2025_11 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

CREATE TABLE audit_logs_2025_12 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- ============================================================================
-- Functions and Triggers
-- ============================================================================

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to relevant tables
CREATE TRIGGER update_organizations_updated_at BEFORE UPDATE ON organizations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_permissions_updated_at BEFORE UPDATE ON permissions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_policies_updated_at BEFORE UPDATE ON policies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Row-Level Security (RLS) for Multi-Tenancy
-- ============================================================================

-- Enable RLS on all organization-scoped tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE policies ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- Note: RLS policies will be defined in application code using SET LOCAL
-- or via Spring Security context. This ensures organization isolation.

-- ============================================================================
-- Initial Data: System Roles
-- ============================================================================

-- Insert default organization for system roles
INSERT INTO organizations (id, name, display_name, description, status)
VALUES (
    '00000000-0000-0000-0000-000000000000',
    'system',
    'System Organization',
    'Internal system organization for platform administration',
    'active'
);

-- Insert system administrator role
INSERT INTO roles (id, organization_id, name, display_name, description, level, is_system)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000000',
    'system:admin',
    'System Administrator',
    'Full access to all platform features and organizations',
    0,
    TRUE
);

-- Insert organization administrator role template
INSERT INTO roles (id, organization_id, name, display_name, description, level, is_system)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000000',
    'org:admin',
    'Organization Administrator',
    'Full access to organization resources and user management',
    0,
    TRUE
);

-- ============================================================================
-- Performance Indexes
-- ============================================================================

-- Additional composite indexes for common query patterns
CREATE INDEX idx_users_org_status ON users(organization_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_roles_org_parent ON roles(organization_id, parent_role_id);
CREATE INDEX idx_user_roles_user_role ON user_roles(user_id, role_id);

-- GIN indexes for JSONB columns (for Phase 2 ABAC)
CREATE INDEX idx_users_attributes_gin ON users USING GIN (attributes jsonb_path_ops);
CREATE INDEX idx_permissions_conditions_gin ON permissions USING GIN (conditions jsonb_path_ops);

-- ============================================================================
-- Views for Common Queries
-- ============================================================================

-- View: Active users with their roles
CREATE VIEW v_user_roles AS
SELECT
    u.id AS user_id,
    u.email,
    u.display_name,
    u.organization_id,
    r.id AS role_id,
    r.name AS role_name,
    ur.resource_type,
    ur.resource_id,
    ur.expires_at
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.deleted_at IS NULL
  AND (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP);

COMMENT ON VIEW v_user_roles IS 'Active users with their current role assignments';

-- View: Effective permissions per role (with inheritance)
CREATE VIEW v_role_permissions AS
WITH RECURSIVE role_hierarchy AS (
    -- Base case: direct role
    SELECT id, parent_role_id, name, level, organization_id
    FROM roles
    WHERE deleted_at IS NULL

    UNION ALL

    -- Recursive case: inherited roles
    SELECT r.id, r.parent_role_id, r.name, r.level, r.organization_id
    FROM roles r
    JOIN role_hierarchy rh ON r.id = rh.parent_role_id
    WHERE r.deleted_at IS NULL
)
SELECT DISTINCT
    rh.id AS role_id,
    rh.name AS role_name,
    p.id AS permission_id,
    p.name AS permission_name,
    p.resource_type,
    p.action
FROM role_hierarchy rh
JOIN role_permissions rp ON rh.id = rp.role_id OR rh.parent_role_id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id;

COMMENT ON VIEW v_role_permissions IS 'Effective permissions including inherited permissions from parent roles';

-- ============================================================================
-- Migration Complete
-- ============================================================================
