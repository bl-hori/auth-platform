# Spec Delta: Example Applications

## ADDED Requirements

### Requirement: Vendor Registration Example Application
A complete Next.js 15 example application MUST demonstrate Auth Platform authorization integration through a vendor registration approval workflow.

#### Scenario: Example app demonstrates basic authorization integration
**Given** a developer wants to integrate Auth Platform into their Next.js application
**When** they review the vendor registration example
**Then** the example MUST include a working AuthPlatformClient implementation
**And** the example MUST show how to make single authorization requests
**And** the example MUST show how to make batch authorization requests
**And** the example MUST include error handling for failed authorization checks
**And** the example MUST demonstrate fail-safe defaults (deny on error)

#### Scenario: Example app demonstrates RBAC patterns
**Given** the vendor registration example application
**When** different users with different roles access the application
**Then** applicant role users MUST be able to create and edit their own vendor applications
**And** applicant role users MUST NOT be able to approve applications
**And** approver role users MUST be able to view and approve/reject applications
**And** approver role users MUST NOT be able to edit application details
**And** admin role users MUST be able to perform all operations
**And** each role's permissions MUST be enforced through Auth Platform API calls

#### Scenario: Example app demonstrates resource-level authorization
**Given** a user with applicant role has created a vendor application
**When** the user attempts to edit the application
**Then** the system MUST check authorization with the specific resource ID
**And** the system MUST allow editing only if the user owns the resource
**And** the system MUST deny editing if the application status is not 'draft'
**And** authorization decisions MUST be based on resource attributes (owner, status)

#### Scenario: Example app demonstrates UI authorization patterns
**Given** a user is viewing the vendor application UI
**When** the page renders
**Then** UI elements MUST be hidden or disabled based on authorization checks
**And** unauthorized actions MUST NOT be available in the interface
**And** authorization checks MUST use React hooks for client-side validation
**And** authorization checks MUST use Server Actions for server-side validation
**And** loading states MUST be shown during authorization checks

#### Scenario: Example app demonstrates batch authorization
**Given** a user is viewing a list of vendor applications
**When** the list page renders
**Then** the system MUST perform batch authorization for all list items
**And** each item MUST display available actions based on authorization results
**And** batch authorization MUST be more efficient than individual checks
**And** the implementation MUST show how to use AuthPlatformClient.authorizeBatch()

### Requirement: Example Application Documentation
Example applications MUST include comprehensive documentation explaining authorization integration patterns and best practices.

#### Scenario: README includes setup and authorization overview
**Given** a developer wants to run the example application
**When** they read the README.md file
**Then** the README MUST include environment setup instructions
**And** the README MUST document required environment variables (API_KEY, BACKEND_URL, ORG_ID)
**And** the README MUST explain how to configure Auth Platform backend connection
**And** the README MUST provide instructions for testing with different roles
**And** the README MUST include a high-level overview of authorization implementation

#### Scenario: Code includes inline authorization explanations
**Given** a developer is reading the example application code
**When** they encounter authorization-related code
**Then** authorization check points MUST have explanatory comments
**And** comments MUST explain WHY the check is needed
**And** comments MUST reference Auth Platform API documentation
**And** complex authorization logic MUST have detailed explanations
**And** best practices MUST be highlighted in comments

#### Scenario: Detailed authorization guide is provided
**Given** a developer wants to understand authorization patterns in depth
**When** they read docs/AUTHORIZATION_GUIDE.md
**Then** the guide MUST explain the authorization architecture
**And** the guide MUST show how to implement AuthPlatformClient
**And** the guide MUST demonstrate different authorization patterns (RBAC, resource-level)
**And** the guide MUST include troubleshooting tips
**And** the guide MUST link to Auth Platform API documentation

### Requirement: Example Application Quality Standards
Example applications MUST follow best practices and demonstrate production-ready patterns while remaining accessible for learning.

#### Scenario: Example follows Next.js and React best practices
**Given** the vendor registration example application
**When** developers review the code
**Then** the application MUST use Next.js 15 App Router
**And** the application MUST use TypeScript for type safety
**And** the application MUST use React Server Components where appropriate
**And** the application MUST use Server Actions for mutations
**And** the application MUST follow proper error handling patterns
**And** the application MUST use proper loading and error states

#### Scenario: Example demonstrates performance optimization
**Given** the vendor registration example performs authorization checks
**When** multiple authorization checks are needed
**Then** the application MUST demonstrate batch authorization for efficiency
**And** the application MUST show how to cache authorization results (with TTL)
**And** the application MUST use optimistic UI updates where appropriate
**And** authorization checks MUST not block critical rendering paths unnecessarily
**And** performance considerations MUST be documented in comments

#### Scenario: Example includes proper error handling
**Given** an authorization check fails or the Auth Platform API is unavailable
**When** the application handles the error
**Then** the application MUST default to DENY (fail-safe)
**And** the application MUST log authorization failures for debugging
**And** the application MUST display user-friendly error messages
**And** the application MUST NOT expose sensitive error details to users
**And** error handling patterns MUST be consistent across the application

#### Scenario: Example is easy to run and test
**Given** a developer wants to test the example application
**When** they follow the setup instructions
**Then** the application MUST start with a single command after setup
**And** the application MUST include mock data for easy testing
**And** the application MUST allow switching between different user roles
**And** the application MUST work with the Auth Platform backend running locally
**And** environment configuration MUST be simple and well-documented

## MODIFIED Requirements

None - this introduces a new capability.

## REMOVED Requirements

None - this introduces a new capability.

## Cross-References

This capability demonstrates the usage of:
- `authorization-core` - Authorization API integration patterns
- `user-identity` - User and role management examples
- `audit-logging` - Audit log integration (optional enhancement)

Example applications serve as living documentation for how to integrate Auth Platform capabilities into real-world applications.
