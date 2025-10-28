# Capability: user-identity (MODIFIED)

## Overview

Modifications to the user-identity capability to support Keycloak integration and JWT-based authentication.

## MODIFIED Requirements

### Requirement: USER-IDENTITY-101 - Keycloak User Linking

The system MUST support linking existing users with Keycloak identities through the keycloak_sub field.

#### Scenario: User record includes Keycloak identifier
**Given** a user record in the database
**When** the user is created or updated
**Then** the record can store keycloak_sub (Keycloak user ID)
**And** keycloak_sub is unique across all users
**And** keycloak_synced_at timestamp tracks last synchronization

#### Scenario: User lookup by keycloak_sub is optimized
**Given** a JWT contains sub="keycloak-user-123"
**When** the system searches for the user
**Then** the query uses the keycloak_sub index
**And** the lookup completes in <2ms

#### Scenario: User can be found by either email or keycloak_sub
**Given** a user with email="user@example.com" and keycloak_sub="keycloak-456"
**When** searching for the user
**Then** the system can find the user by email OR keycloak_sub
**And** keycloak_sub takes precedence if both are provided

---

### Requirement: USER-IDENTITY-102 - Keycloak Synchronization Timestamp

The system MUST track when user information was last synchronized with Keycloak.

#### Scenario: Synchronization timestamp is updated on JWT authentication
**Given** a user authenticates with JWT
**When** the user record is updated or created
**Then** keycloak_synced_at is set to current timestamp
**And** the timestamp can be used to identify stale user data

#### Scenario: Synchronization timestamp helps identify inactive Keycloak users
**Given** a user has not authenticated via JWT for >90 days
**When** querying user activity
**Then** the system can identify users with old keycloak_synced_at timestamps
**And** these users may need re-verification or cleanup
