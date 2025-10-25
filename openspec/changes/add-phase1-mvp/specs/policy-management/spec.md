# Policy Management Spec Delta (Phase 1 MVP)

## ADDED Requirements

### Requirement: RBAC Policy Creation
The system SHALL support creating RBAC-only policies in Phase 1 MVP.

#### Scenario: Create basic RBAC policy
- **WHEN** an administrator creates an RBAC policy using Rego language
- **THEN** the system validates Rego syntax
- **AND** stores the policy with version 1
- **AND** sets policy status to 'draft'
- **AND** returns policy ID and metadata

#### Scenario: Manual policy activation
- **WHEN** an administrator manually activates a draft policy
- **THEN** the policy is compiled and loaded into OPA
- **AND** policy status changes to 'active'
- **AND** policy is distributed to all application instances within 30 seconds

### Requirement: Simplified Policy Versioning
The system SHALL maintain basic version history for rollback capability.

#### Scenario: Create new policy version
- **WHEN** updating an existing policy
- **THEN** the system increments version number
- **AND** preserves previous version in database
- **AND** new version starts as 'draft'

#### Scenario: Activate specific version
- **WHEN** activating a specific policy version
- **THEN** the selected version becomes active
- **AND** previously active version is deactivated
- **AND** change is logged in audit trail

## MODIFIED Requirements

### Requirement: Policy Types Support
The system SHALL support RBAC only in Phase 1 (MODIFIED from original spec).

**Original**: System SHALL support RBAC, ABAC, and ReBAC
**Modified**: System SHALL support RBAC only; ABAC and ReBAC deferred to Phase 2

#### Scenario: RBAC policy evaluation
- **WHEN** a policy is evaluated
- **THEN** the system uses role-based logic only
- **AND** supports role hierarchy (inheritance)
- **AND** allows resource-scoped role assignments
- **AND** evaluates permissions based on user's roles

### Requirement: Policy Validation
The system SHALL perform simplified validation for MVP.

**Original**: Comprehensive validation including circular dependencies, forbidden imports, and complex policy analysis
**Modified**: Basic syntax validation and forbidden import checking only

#### Scenario: Basic Rego syntax validation
- **WHEN** a policy is submitted
- **THEN** the system validates Rego syntax using OPA
- **AND** checks for forbidden imports (passwords, secrets)
- **AND** rejects invalid policies with error details

## REMOVED Requirements

### Requirement: GitOps Integration
**Removed from Phase 1 MVP** - Deferred to Phase 2

**Reason**: Adds complexity not needed for initial pilot deployments. Manual policy management via UI is sufficient for MVP.

**Migration**: When added in Phase 2, existing manually-created policies will be exportable to Git repository format.

### Requirement: No-Code Policy Editor
**Removed from Phase 1 MVP** - Deferred to Phase 3

**Reason**: Requires significant frontend development effort. Code-based policy editor (Monaco) provides sufficient functionality for technical users in pilot phase.

**Migration**: When added in Phase 3, visual editor will generate Rego code that's stored in the same format as manually-written policies.

### Requirement: Cedar Policy Language Support
**Removed from Phase 1 MVP** - Deferred to Phase 2

**Reason**: Supporting multiple policy languages adds complexity. OPA/Rego is mature and well-documented, sufficient for MVP.

**Migration**: When added in Phase 2, Cedar policies will be stored alongside Rego policies with a language indicator field.

## ADDED Non-Functional Requirements

### Performance (MVP-Specific)
- Policy validation SHALL complete within 1 second for policies up to 5KB
- Policy activation SHALL complete within 30 seconds (relaxed from 1 second in full spec)
- Policy query API SHALL return results within 200ms (relaxed from 100ms)

### Scope Limitations
- Maximum 1,000 policies per organization in MVP
- Maximum 10 policy versions retained per policy
- Policies limited to 10KB in size
