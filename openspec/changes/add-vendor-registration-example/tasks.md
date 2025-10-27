# Tasks: Add Vendor Registration Example Application

## Phase 1: Project Setup & Infrastructure

### Task 1.1: Initialize Next.js 15 Project
- [x] Create `examples/vendor-registration/` directory
- [x] Initialize Next.js 15 project with TypeScript and App Router
- [x] Configure Tailwind CSS and PostCSS
- [x] Add required dependencies (lucide-react, clsx, tailwind-merge)
- [x] Setup ESLint and Prettier configuration
- [x] Create `.env.example` with Auth Platform configuration variables
- **Validation**: `pnpm build` completes successfully ✅
- **Dependencies**: None
- **Estimated Time**: 30 minutes
- **Actual Time**: 30 minutes

### Task 1.2: Setup Project Structure
- [ ] Create directory structure (app/, components/, lib/, types/, docs/)
- [ ] Setup shadcn/ui CLI and install base components (button, card, form, input, select)
- [ ] Create base layout.tsx with navigation
- [ ] Create global styles and theme configuration
- [ ] Add TypeScript types for Vendor and Auth entities
- **Validation**: Project structure matches design.md specifications
- **Dependencies**: Task 1.1
- **Estimated Time**: 45 minutes

### Task 1.3: Create Mock Data and Utilities
- [ ] Implement mock vendor data in `lib/mock-data.ts`
- [ ] Create mock user sessions with different roles (applicant, approver, admin)
- [ ] Implement utility functions in `lib/utils.ts`
- [ ] Create type definitions in `types/vendor.ts` and `types/auth.ts`
- [ ] Add data validation utilities
- **Validation**: Mock data is properly typed and accessible
- **Dependencies**: Task 1.2
- **Estimated Time**: 45 minutes

## Phase 2: Auth Platform Integration

### Task 2.1: Implement Auth Platform Client
- [ ] Create `lib/auth-client.ts` with AuthPlatformClient class
- [ ] Implement `authorize()` method for single authorization checks
- [ ] Implement `authorizeBatch()` method for batch authorization checks
- [ ] Add proper error handling and retry logic
- [ ] Add request/response logging for debugging
- [ ] Configure client with environment variables (API_KEY, BACKEND_URL, ORG_ID)
- **Validation**: Client can successfully call Auth Platform /v1/authorize endpoint
- **Dependencies**: Task 1.3
- **Estimated Time**: 1.5 hours

### Task 2.2: Create Authorization Utilities and Hooks
- [ ] Implement `lib/authorization.ts` with authorization helper functions
- [ ] Create `useAuthorization()` React hook for client-side checks
- [ ] Create `checkServerAuthorization()` function for Server Actions
- [ ] Implement authorization result caching (with TTL)
- [ ] Add authorization error types and handling
- **Validation**: Hooks and utilities work with mocked Auth Platform responses
- **Dependencies**: Task 2.1
- **Estimated Time**: 1.5 hours

### Task 2.3: Create Authorization Guard Components
- [ ] Implement `AuthGuard` component for declarative authorization
- [ ] Create `ProtectedButton` component with authorization check
- [ ] Create `ProtectedLink` component with authorization check
- [ ] Add loading and error states for authorization checks
- [ ] Implement fallback UI for unauthorized access
- **Validation**: Components correctly show/hide based on authorization
- **Dependencies**: Task 2.2
- **Estimated Time**: 1 hour

## Phase 3: Authentication & Session Management

### Task 3.1: Implement Simple Authentication
- [ ] Create `/app/login/page.tsx` with simple login form
- [ ] Implement role selection (applicant, approver, admin)
- [ ] Create mock authentication logic (no real security needed)
- [ ] Store user session in cookies or localStorage
- [ ] Create `lib/session.ts` for session management
- **Validation**: Users can "login" and switch between roles
- **Dependencies**: Task 1.3
- **Estimated Time**: 1 hour

### Task 3.2: Create Session Context Provider
- [ ] Implement React Context for user session
- [ ] Create `useUser()` hook to access current user
- [ ] Add session loading and initialization logic
- [ ] Implement logout functionality
- [ ] Add session persistence across page refreshes
- **Validation**: User session is accessible throughout the application
- **Dependencies**: Task 3.1
- **Estimated Time**: 45 minutes

## Phase 4: Core Features - Vendor Application CRUD

### Task 4.1: Implement Vendor List Page
- [ ] Create `/app/vendors/page.tsx` to display vendor applications
- [ ] Implement role-based filtering (applicants see only their own)
- [ ] Add status badges and visual indicators
- [ ] Implement batch authorization for list items
- [ ] Add action buttons based on authorization (view, edit, delete, approve)
- [ ] Create `VendorCard` component for list items
- **Validation**: List page shows vendors with correct authorization-based actions
- **Dependencies**: Task 2.3, Task 3.2
- **Estimated Time**: 1.5 hours

### Task 4.2: Implement Vendor Creation
- [ ] Create `/app/vendors/new/page.tsx` with vendor creation form
- [ ] Implement `VendorForm` component with validation
- [ ] Add authorization check for `vendor:create` permission
- [ ] Implement form submission with Server Action
- [ ] Add success/error toast notifications
- [ ] Handle form validation errors
- **Validation**: Applicants and admins can create vendors; approvers cannot
- **Dependencies**: Task 2.3, Task 3.2
- **Estimated Time**: 1.5 hours

### Task 4.3: Implement Vendor Detail View
- [ ] Create `/app/vendors/[id]/page.tsx` to display vendor details
- [ ] Add authorization check for viewing vendor
- [ ] Display all vendor information in readonly format
- [ ] Show status history and approval comments
- [ ] Add action buttons based on authorization (edit, delete, approve)
- **Validation**: Users can view vendors they have permission to access
- **Dependencies**: Task 2.3, Task 3.2
- **Estimated Time**: 1 hour

### Task 4.4: Implement Vendor Edit
- [ ] Create `/app/vendors/[id]/edit/page.tsx` with edit form
- [ ] Add authorization check for `vendor:update:own` or `vendor:update:all`
- [ ] Pre-populate form with existing vendor data
- [ ] Implement update Server Action with authorization check
- [ ] Restrict editing based on vendor status (draft vs submitted)
- [ ] Add optimistic UI updates
- **Validation**: Users can edit only authorized vendors based on ownership and status
- **Dependencies**: Task 4.3
- **Estimated Time**: 1.5 hours

### Task 4.5: Implement Vendor Deletion
- [ ] Add delete functionality in vendor detail page
- [ ] Implement authorization check for `vendor:delete:own` or `vendor:delete:all`
- [ ] Add confirmation dialog before deletion
- [ ] Implement delete Server Action with authorization check
- [ ] Redirect to list page after successful deletion
- **Validation**: Users can delete only authorized vendors
- **Dependencies**: Task 4.3
- **Estimated Time**: 45 minutes

## Phase 5: Approval Workflow

### Task 5.1: Implement Application Submission
- [ ] Add "Submit for Approval" button in vendor edit page
- [ ] Check authorization for `vendor:submit` permission
- [ ] Validate all required fields before submission
- [ ] Update vendor status from 'draft' to 'pending_approval'
- [ ] Show confirmation and redirect to detail page
- **Validation**: Applicants can submit their draft applications
- **Dependencies**: Task 4.4
- **Estimated Time**: 45 minutes

### Task 5.2: Implement Approval Actions Component
- [ ] Create `ApprovalActions` component with approve/reject buttons
- [ ] Add authorization check for `vendor:approve` permission
- [ ] Implement approve Server Action with comment field
- [ ] Implement reject Server Action with required comment
- [ ] Update vendor status and add reviewer information
- [ ] Send notification (mocked) to applicant
- **Validation**: Approvers and admins can approve/reject applications
- **Dependencies**: Task 4.3
- **Estimated Time**: 1.5 hours

### Task 5.3: Create Approval Dashboard View
- [ ] Create dashboard page at `/app/dashboard/page.tsx`
- [ ] Show pending approvals for approvers
- [ ] Show all applications for admins
- [ ] Show user's applications for applicants
- [ ] Add quick action buttons
- [ ] Implement role-based dashboard layout
- **Validation**: Dashboard shows appropriate information based on user role
- **Dependencies**: Task 3.2, Task 4.1, Task 5.2
- **Estimated Time**: 1.5 hours

## Phase 6: UI/UX Polish

### Task 6.1: Add Loading and Error States
- [ ] Implement loading skeletons for async operations
- [ ] Add error boundaries for component errors
- [ ] Create error toast notifications
- [ ] Add retry mechanisms for failed operations
- [ ] Implement optimistic UI updates where appropriate
- **Validation**: Application handles loading and errors gracefully
- **Dependencies**: All previous tasks
- **Estimated Time**: 1 hour

### Task 6.2: Improve Navigation and Layout
- [ ] Create navigation menu with role-based links
- [ ] Add user profile dropdown with logout
- [ ] Implement breadcrumb navigation
- [ ] Add page titles and meta tags
- [ ] Style active navigation items
- **Validation**: Navigation is intuitive and works correctly
- **Dependencies**: Task 3.2
- **Estimated Time**: 1 hour

### Task 6.3: Add Status Indicators and Visual Feedback
- [ ] Create status badge component with color coding
- [ ] Add icons for different actions and states
- [ ] Implement success/error animations
- [ ] Add hover states and tooltips
- [ ] Improve form validation visual feedback
- **Validation**: UI provides clear visual feedback for all interactions
- **Dependencies**: Task 6.1
- **Estimated Time**: 1 hour

## Phase 7: Documentation

### Task 7.1: Write README.md
- [ ] Add project overview and purpose
- [ ] Document environment setup and prerequisites
- [ ] Provide step-by-step setup instructions
- [ ] Explain how to test with different user roles
- [ ] Add troubleshooting section
- [ ] Include architecture overview diagram
- [ ] Document Auth Platform integration overview
- **Validation**: Developers can setup and run the example by following README
- **Dependencies**: All implementation tasks completed
- **Estimated Time**: 1.5 hours

### Task 7.2: Write Authorization Guide
- [ ] Create `docs/AUTHORIZATION_GUIDE.md`
- [ ] Explain Auth Platform authorization architecture
- [ ] Document AuthPlatformClient implementation in detail
- [ ] Show examples of different authorization patterns (RBAC, resource-level)
- [ ] Document batch authorization optimization
- [ ] Add troubleshooting section for common authorization issues
- [ ] Link to Auth Platform API documentation
- **Validation**: Guide provides comprehensive authorization integration knowledge
- **Dependencies**: All implementation tasks completed
- **Estimated Time**: 2 hours

### Task 7.3: Add Inline Code Documentation
- [ ] Add JSDoc comments to all exported functions and components
- [ ] Add explanatory comments at all authorization check points
- [ ] Document WHY decisions were made in complex logic
- [ ] Add examples in comments where helpful
- [ ] Ensure TypeScript types are well-documented
- **Validation**: Code is self-documenting with clear comments
- **Dependencies**: All implementation tasks completed
- **Estimated Time**: 1.5 hours

## Phase 8: Testing & Validation

### Task 8.1: Manual Testing - Role-Based Access
- [ ] Test all features as applicant role
- [ ] Test all features as approver role
- [ ] Test all features as admin role
- [ ] Verify authorization denials work correctly
- [ ] Test edge cases (expired sessions, network errors)
- **Validation**: All authorization rules work as specified
- **Dependencies**: All implementation tasks completed
- **Estimated Time**: 1 hour

### Task 8.2: Integration Testing with Backend
- [ ] Test with actual Auth Platform backend running locally
- [ ] Verify authorization requests are correctly formatted
- [ ] Validate authorization responses are correctly handled
- [ ] Test error scenarios (backend down, invalid API key)
- [ ] Verify audit logs are created (if implemented)
- **Validation**: Integration with Auth Platform backend works correctly
- **Dependencies**: Task 8.1
- **Estimated Time**: 1 hour

### Task 8.3: Documentation Review
- [ ] Review README.md for completeness and accuracy
- [ ] Review AUTHORIZATION_GUIDE.md for technical accuracy
- [ ] Check all code comments for clarity
- [ ] Verify .env.example is complete
- [ ] Test setup instructions from scratch
- **Validation**: Documentation is accurate and complete
- **Dependencies**: Task 7.1, Task 7.2, Task 7.3
- **Estimated Time**: 45 minutes

## Summary

**Total Tasks**: 31 tasks across 8 phases
**Estimated Total Time**: 26-28 hours

**Key Milestones**:
- Phase 1-2: Basic setup and Auth Platform integration (5 hours)
- Phase 3-4: Authentication and core CRUD features (7 hours)
- Phase 5: Approval workflow (4 hours)
- Phase 6: UI/UX polish (3 hours)
- Phase 7: Documentation (5 hours)
- Phase 8: Testing & validation (3 hours)

**Dependencies**:
- Auth Platform backend must be running for integration testing
- All authorization checks depend on AuthPlatformClient implementation
- Documentation tasks depend on implementation completion

**Risk Factors**:
- Auth Platform API integration complexity
- Time spent on UI polish may vary
- Documentation quality requires iteration
