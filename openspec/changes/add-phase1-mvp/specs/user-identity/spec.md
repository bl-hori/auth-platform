# User Identity Spec Delta (Phase 1 MVP)

## ADDED Requirements

### Requirement: Manual User Management
The system SHALL support creating and managing users via REST API and Web UI.

#### Scenario: Create user manually
- **WHEN** an administrator creates a user via API or UI
- **THEN** the system validates email format and uniqueness
- **AND** creates user with status 'active'
- **AND** generates unique user ID
- **AND** associates user with organization

#### Scenario: Update user information
- **WHEN** an administrator updates user details
- **THEN** the system validates changes
- **AND** updates user record in database
- **AND** invalidates authorization cache for the user
- **AND** logs the change in audit trail

#### Scenario: Deactivate user
- **WHEN** an administrator deactivates a user
- **THEN** user status changes to 'inactive'
- **AND** all future authorization requests are denied
- **AND** existing sessions remain valid (grace period)
- **AND** user can be reactivated later

### Requirement: Direct Role Assignment
The system SHALL support assigning roles directly to users.

#### Scenario: Assign global role to user
- **WHEN** an administrator assigns a role to a user
- **THEN** the system creates user_role record
- **AND** role applies to all resources
- **AND** authorization cache for user is invalidated
- **AND** assignment is logged with grantor information

#### Scenario: Assign resource-scoped role
- **WHEN** assigning role for specific resource
- **THEN** the system stores resource_id in user_role
- **AND** role only applies to that resource
- **AND** authorization respects the scope

### Requirement: Basic User Search
The system SHALL provide simple user search and listing.

#### Scenario: List users with pagination
- **WHEN** administrator lists users
- **THEN** system returns paginated results (default 20 per page)
- **AND** includes user ID, email, name, status
- **AND** sorted by created date descending

#### Scenario: Search users by email
- **WHEN** searching by email pattern
- **THEN** system performs case-insensitive partial match
- **AND** returns matching users within 500ms
- **AND** respects organization isolation

## MODIFIED Requirements

### Requirement: User Data Source
The system SHALL manage users directly in Phase 1 (MODIFIED from SCIM sync).

**Original**: Primary user source is external IdP via SCIM synchronization
**Modified**: Users created and managed directly in auth platform; SCIM deferred to Phase 2

#### Scenario: User lifecycle managed internally
- **WHEN** managing users
- **THEN** all CRUD operations happen in auth platform database
- **AND** no external IdP synchronization occurs
- **AND** users authenticate with auth platform (not external IdP in MVP)

### Requirement: User Attributes (Simplified Storage)
The system SHALL store custom attributes but not use them in policies for MVP.

**Original**: User attributes used for ABAC policy evaluation
**Modified**: Attributes stored in JSONB field but not evaluated in policies (Phase 1 focuses on RBAC)

#### Scenario: Store custom user attributes
- **WHEN** setting user attributes via API
- **THEN** system stores in attributes JSONB field
- **AND** validates JSON format
- **AND** does NOT use attributes in authorization decisions yet

## REMOVED Requirements

### Requirement: SCIM Synchronization
**Removed from Phase 1 MVP** - Deferred to Phase 2

**Reason**: SCIM integration adds significant complexity (protocol implementation, error handling, conflict resolution). Manual user management is sufficient for pilot applications.

**Migration**: When SCIM is added in Phase 2:
- Existing users will be matched to external IDP users via email
- external_id field will be populated during first sync
- Duplicate detection and merge strategy will be implemented

### Requirement: User Groups
**Removed from Phase 1 MVP** - Deferred to Phase 2

**Reason**: Role hierarchy provides similar benefits for MVP. Groups add complexity in UI and permission resolution logic.

**Migration**: When Groups are added:
- Group-role assignments will work alongside direct user-role assignments
- Permission resolution will evaluate both direct and group-inherited roles

### Requirement: Advanced User Search
**Removed from Phase 1 MVP** - Simplified to basic search only

**Reason**: Complex queries (attribute filtering, multi-criteria) require sophisticated query builder. Simple email search sufficient for small pilot user base.

**Migration**: Phase 2 will add advanced filter UI and optimized query logic.

## ADDED Non-Functional Requirements

### Performance (MVP-Specific)
- User CRUD operations SHALL complete within 500ms (p95)
- User search SHALL handle up to 10,000 users per organization
- User listing SHALL return within 200ms with pagination

### Scalability (MVP-Specific)
- Support up to 10,000 users per organization
- Support up to 50 organizations in MVP deployment
- User attribute JSONB limited to 5KB per user

### Security (MVP-Specific)
- User email addresses SHALL be unique within organization
- User passwords SHALL NOT be stored (authentication via API key for now)
- All user operations SHALL be authenticated and authorized
