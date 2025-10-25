# User Identity Specification

## Overview
The User Identity capability manages user information, roles, and synchronization with external Identity Providers through SCIM protocol.

## Requirements

### Requirement: User Synchronization via SCIM
The system SHALL synchronize user and group data from external Identity Providers using SCIM 2.0 protocol.

#### Scenario: Create user via SCIM
- **WHEN** an IdP sends a SCIM user creation request
- **THEN** the system validates the SCIM payload
- **AND** creates a new user with mapped attributes
- **AND** returns HTTP 201 Created with user resource
- **AND** triggers user.created event to Kafka

#### Scenario: Update user via SCIM
- **WHEN** an IdP sends a SCIM user update request
- **THEN** the system locates the user by external_id
- **AND** updates the specified attributes
- **AND** returns HTTP 200 OK with updated resource
- **AND** invalidates authorization cache for the user

#### Scenario: Delete user via SCIM
- **WHEN** an IdP sends a SCIM user deletion request
- **THEN** the system performs soft delete (sets deleted_at)
- **AND** revokes all active sessions
- **AND** removes all role assignments
- **AND** returns HTTP 204 No Content

#### Scenario: Bulk user synchronization
- **WHEN** an IdP sends a SCIM bulk operation request
- **THEN** the system processes all operations atomically
- **AND** rolls back all changes if any operation fails
- **AND** returns detailed status for each operation
- **AND** completes within 10 seconds for 1000 users

### Requirement: Role Assignment
The system SHALL allow administrators to assign and revoke roles for users.

#### Scenario: Assign role to user
- **WHEN** an administrator assigns a role to a user
- **THEN** the system validates the role exists
- **AND** creates a user_role record
- **AND** invalidates authorization cache for the user
- **AND** logs the assignment with grantor information

#### Scenario: Assign resource-specific role
- **WHEN** an administrator assigns a role scoped to a specific resource
- **THEN** the system creates role assignment with resource_id
- **AND** the user has the role only for that resource
- **AND** authorization decisions respect the resource scope

#### Scenario: Time-limited role assignment
- **WHEN** an administrator assigns a role with expiration date
- **THEN** the system stores the expires_at timestamp
- **AND** the role is automatically revoked after expiration
- **AND** a background job cleans up expired assignments daily

#### Scenario: Revoke role from user
- **WHEN** an administrator revokes a role from a user
- **THEN** the system deletes the user_role record
- **AND** invalidates all cached permissions for the user
- **AND** logs the revocation with reason and revoker

### Requirement: User Attributes Management
The system SHALL manage custom user attributes for ABAC authorization.

#### Scenario: Set user attributes
- **WHEN** an administrator sets custom attributes for a user
- **THEN** the system stores attributes in JSONB field
- **AND** validates attribute values against defined schema (if exists)
- **AND** makes attributes available for policy evaluation

#### Scenario: Query users by attributes
- **WHEN** an administrator searches users by attribute criteria
- **THEN** the system uses GIN index for efficient JSONB search
- **AND** returns matching users within 500ms for 100,000 users
- **AND** supports complex queries (AND, OR, range operators)

### Requirement: User Groups
The system SHALL support user groups for simplified role management.

#### Scenario: Create user group
- **WHEN** an administrator creates a new group
- **THEN** the system creates the group with unique ID
- **AND** allows adding users to the group
- **AND** supports nested groups (group hierarchy)

#### Scenario: Assign role to group
- **WHEN** an administrator assigns a role to a group
- **THEN** all current group members inherit the role
- **AND** future members automatically receive the role
- **AND** removing from group revokes inherited roles

#### Scenario: Evaluate group-based permissions
- **WHEN** authorization is checked for a group member
- **THEN** the system evaluates both direct and group-inherited roles
- **AND** applies priority rules if conflicts exist
- **AND** decision reasoning includes group membership path

### Requirement: External ID Mapping
The system SHALL maintain mapping between internal user IDs and external IdP identifiers.

#### Scenario: Map external identity
- **WHEN** a user authenticates via external IdP for the first time
- **THEN** the system creates internal user record
- **AND** stores IdP identifier in external_id field
- **AND** maintains unique constraint per (organization, external_id)

#### Scenario: Handle IdP identifier change
- **WHEN** an IdP changes a user's identifier
- **THEN** the system updates external_id via SCIM update
- **AND** preserves user's internal ID and all assignments
- **AND** logs the identifier change for audit

### Requirement: User Status Management
The system SHALL track and enforce user status (active, inactive, suspended).

#### Scenario: Deactivate user
- **WHEN** an administrator deactivates a user
- **THEN** the user status changes to 'inactive'
- **AND** all authorization requests for the user are denied
- **AND** active sessions are terminated immediately
- **AND** the user can be reactivated later

#### Scenario: Suspend user
- **WHEN** a user is suspended due to security policy
- **THEN** the user status changes to 'suspended'
- **AND** authorization denies with specific suspension message
- **AND** the suspension includes reason and expiry date
- **AND** automatic reactivation occurs after suspension period

### Requirement: User Search and Filtering
The system SHALL provide efficient user search capabilities.

#### Scenario: Search by email
- **WHEN** an administrator searches users by email pattern
- **THEN** the system performs case-insensitive partial match
- **AND** returns results within 200ms for 1M users
- **AND** includes pagination (default 20, max 100 per page)

#### Scenario: Filter by multiple criteria
- **WHEN** an administrator filters by role, status, and attributes
- **THEN** the system applies all filters with AND logic
- **AND** uses appropriate indexes for performance
- **AND** returns count of total matches for pagination

### Requirement: Multi-Tenancy Isolation
The system SHALL ensure complete isolation of user data between organizations.

#### Scenario: User data isolation
- **WHEN** querying users within an organization
- **THEN** only users belonging to that organization are returned
- **AND** cross-organization queries are prevented at database level
- **AND** organization_id is always included in WHERE clauses

#### Scenario: Prevent cross-tenant role assignments
- **WHEN** attempting to assign a role from different organization
- **THEN** the system rejects with FORBIDDEN error
- **AND** logs the unauthorized attempt
- **AND** enforces organization match via database constraints

### Requirement: Audit Trail for User Operations
The system SHALL log all user management operations for compliance.

#### Scenario: Log user creation
- **WHEN** a user is created via any method (SCIM, API, UI)
- **THEN** the system records user.created event
- **AND** includes who created, when, and initial attributes
- **AND** stores in tamper-proof audit log

#### Scenario: Log role assignment changes
- **WHEN** roles are assigned or revoked
- **THEN** the system logs the complete before/after state
- **AND** includes grantor, timestamp, and reason (if provided)
- **AND** enables compliance reporting and investigation

## Non-Functional Requirements

### Performance
- SCIM operations SHALL complete within 500ms (p95)
- User search SHALL return results within 200ms for 1M users
- Bulk operations SHALL process 1000 users within 10 seconds

### Reliability
- SCIM endpoint SHALL have 99.9% availability
- Failed synchronizations SHALL be retried with exponential backoff
- Data consistency SHALL be maintained across all operations

### Security
- SCIM endpoint SHALL require OAuth 2.0 authentication
- User passwords SHALL NEVER be stored (authentication is external)
- Sensitive attributes SHALL be encrypted at rest

### Scalability
- The system SHALL support 1,000,000+ users per organization
- User attribute storage SHALL handle JSONB documents up to 10KB
- Group hierarchy SHALL support nesting up to 10 levels
