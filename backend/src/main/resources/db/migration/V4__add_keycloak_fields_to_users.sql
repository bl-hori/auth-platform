-- ============================================================================
-- Migration: V4__add_keycloak_fields_to_users.sql
-- Description: Add Keycloak integration fields to users table
-- Author: Auth Platform Team
-- Date: 2025-10-28
-- Phase: Phase 2 - JWT Validation Integration
-- ============================================================================

-- Add Keycloak integration fields to users table
ALTER TABLE users
ADD COLUMN keycloak_sub VARCHAR(255),
ADD COLUMN keycloak_synced_at TIMESTAMP;

-- Add unique constraint on keycloak_sub to prevent duplicate Keycloak users
ALTER TABLE users
ADD CONSTRAINT uk_users_keycloak_sub UNIQUE (keycloak_sub);

-- Create index on keycloak_sub for fast lookups during JWT authentication
CREATE INDEX idx_users_keycloak_sub ON users(keycloak_sub)
WHERE keycloak_sub IS NOT NULL;

-- Add column comments for documentation
COMMENT ON COLUMN users.keycloak_sub IS 'Keycloak user ID (sub claim from JWT). Used to link Auth Platform users with Keycloak identities.';
COMMENT ON COLUMN users.keycloak_synced_at IS 'Last synchronization timestamp with Keycloak. Updated on each JWT authentication.';

-- ============================================================================
-- Notes:
-- - keycloak_sub is nullable to support existing users who haven't authenticated via JWT yet
-- - Existing users will be linked to Keycloak on first JWT authentication (JIT provisioning)
-- - keycloak_sub takes precedence over email for user lookup when both are available
-- - Partial index on keycloak_sub excludes NULL values for better performance
-- ============================================================================
