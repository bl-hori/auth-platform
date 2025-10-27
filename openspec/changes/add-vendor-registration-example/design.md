# Design: Add Vendor Registration Example Application

## Architecture Overview

### Application Structure
```
examples/vendor-registration/
├── README.md                 # Setup guide & authorization integration explanation
├── package.json
├── next.config.ts
├── tsconfig.json
├── tailwind.config.ts
├── .env.example
├── src/
│   ├── app/                  # Next.js 15 App Router
│   │   ├── layout.tsx
│   │   ├── page.tsx          # Landing page
│   │   ├── login/
│   │   │   └── page.tsx      # Simple login form
│   │   ├── dashboard/
│   │   │   └── page.tsx      # Dashboard with role-based views
│   │   ├── vendors/
│   │   │   ├── page.tsx      # Vendor list (authorized)
│   │   │   ├── new/
│   │   │   │   └── page.tsx  # Create vendor application
│   │   │   └── [id]/
│   │   │       ├── page.tsx  # View vendor details
│   │   │       └── edit/
│   │   │           └── page.tsx  # Edit vendor (authorized)
│   │   └── api/
│   │       └── auth/
│   │           └── [...nextauth]/route.ts  # Simple auth handler
│   ├── components/
│   │   ├── ui/               # shadcn/ui components
│   │   ├── AuthGuard.tsx     # Authorization wrapper component
│   │   ├── VendorForm.tsx
│   │   ├── VendorCard.tsx
│   │   └── ApprovalActions.tsx
│   ├── lib/
│   │   ├── auth-client.ts    # Auth Platform API client
│   │   ├── authorization.ts  # Authorization utilities
│   │   ├── mock-data.ts      # Mock vendor data
│   │   └── utils.ts
│   └── types/
│       ├── vendor.ts
│       └── auth.ts
└── docs/
    └── AUTHORIZATION_GUIDE.md  # Deep dive into authorization implementation
```

## Domain Model

### Vendor Application Entity
```typescript
interface VendorApplication {
  id: string;
  companyName: string;
  registrationNumber: string;  // 法人番号
  address: string;
  contactName: string;
  contactEmail: string;
  contactPhone: string;
  businessCategory: string;
  status: 'draft' | 'pending_approval' | 'approved' | 'rejected';
  submittedBy: string;         // User ID
  submittedAt?: Date;
  reviewedBy?: string;         // Approver User ID
  reviewedAt?: Date;
  reviewComment?: string;
  createdAt: Date;
  updatedAt: Date;
}
```

### User Roles & Permissions
```typescript
// Roles
enum Role {
  APPLICANT = 'applicant',      // 申請者
  APPROVER = 'approver',         // 承認者
  ADMIN = 'admin'                // 管理者
}

// Permissions
const PERMISSIONS = {
  // Vendor application permissions
  'vendor:create',       // Create new vendor applications
  'vendor:read:own',     // Read own applications
  'vendor:read:all',     // Read all applications
  'vendor:update:own',   // Update own draft applications
  'vendor:update:all',   // Update any application
  'vendor:delete:own',   // Delete own draft applications
  'vendor:delete:all',   // Delete any application
  'vendor:approve',      // Approve/reject applications
  'vendor:submit',       // Submit application for approval
} as const;

// Role-Permission Mapping
const ROLE_PERMISSIONS = {
  [Role.APPLICANT]: [
    'vendor:create',
    'vendor:read:own',
    'vendor:update:own',
    'vendor:delete:own',
    'vendor:submit',
  ],
  [Role.APPROVER]: [
    'vendor:read:all',
    'vendor:approve',
  ],
  [Role.ADMIN]: [
    'vendor:create',
    'vendor:read:all',
    'vendor:update:all',
    'vendor:delete:all',
    'vendor:approve',
    'vendor:submit',
  ],
};
```

## Authorization Integration Design

### 1. Authorization Client Setup
```typescript
// lib/auth-client.ts
import { AuthorizationRequest, AuthorizationResponse } from '@/types/auth';

export class AuthPlatformClient {
  private baseUrl: string;
  private apiKey: string;
  private organizationId: string;

  constructor(config: AuthClientConfig) {
    this.baseUrl = config.baseUrl;
    this.apiKey = config.apiKey;
    this.organizationId = config.organizationId;
  }

  /**
   * Single authorization check
   */
  async authorize(request: AuthorizationRequest): Promise<AuthorizationResponse> {
    const response = await fetch(`${this.baseUrl}/v1/authorize`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': this.apiKey,
        'X-Organization-Id': this.organizationId,
      },
      body: JSON.stringify({
        organizationId: this.organizationId,
        principal: {
          id: request.userId,
          type: 'user',
        },
        action: request.action,
        resource: {
          type: request.resourceType,
          id: request.resourceId,
        },
        context: request.context,
      }),
    });

    if (!response.ok) {
      throw new Error(`Authorization check failed: ${response.statusText}`);
    }

    return await response.json();
  }

  /**
   * Batch authorization check for multiple resources
   */
  async authorizeBatch(requests: AuthorizationRequest[]): Promise<AuthorizationResponse[]> {
    // Implementation for batch authorization
  }
}
```

### 2. Authorization Hooks
```typescript
// lib/authorization.ts
import { useCallback } from 'react';
import { authClient } from './auth-client';

/**
 * React hook for authorization checks
 */
export function useAuthorization() {
  const checkPermission = useCallback(async (
    action: string,
    resourceType: string,
    resourceId?: string
  ): Promise<boolean> => {
    try {
      const response = await authClient.authorize({
        userId: getCurrentUserId(), // Get from session/context
        action,
        resourceType,
        resourceId,
      });
      return response.decision === 'ALLOW';
    } catch (error) {
      console.error('Authorization check failed:', error);
      return false; // Fail-safe: deny on error
    }
  }, []);

  return { checkPermission };
}

/**
 * Authorization guard component
 */
export function AuthGuard({
  action,
  resourceType,
  resourceId,
  fallback = <AccessDenied />,
  children,
}: AuthGuardProps) {
  const { checkPermission } = useAuthorization();
  const [isAuthorized, setIsAuthorized] = useState<boolean | null>(null);

  useEffect(() => {
    checkPermission(action, resourceType, resourceId)
      .then(setIsAuthorized);
  }, [action, resourceType, resourceId, checkPermission]);

  if (isAuthorized === null) {
    return <LoadingSpinner />;
  }

  return isAuthorized ? <>{children}</> : fallback;
}
```

### 3. Server-Side Authorization (Server Actions)
```typescript
// Server Action example
'use server';

import { authClient } from '@/lib/auth-client';
import { getCurrentUser } from '@/lib/session';

export async function updateVendorApplication(
  vendorId: string,
  data: Partial<VendorApplication>
) {
  const user = await getCurrentUser();

  // Authorization check
  const authResponse = await authClient.authorize({
    userId: user.id,
    action: 'update',
    resourceType: 'vendor',
    resourceId: vendorId,
    context: {
      vendorStatus: data.status,
      isOwner: data.submittedBy === user.id,
    },
  });

  if (authResponse.decision !== 'ALLOW') {
    throw new Error('Unauthorized: ' + authResponse.reason);
  }

  // Proceed with update
  return updateVendor(vendorId, data);
}
```

## UI/UX Flow

### User Journey: Applicant (申請者)
```
1. Login → 2. Dashboard → 3. Create New Vendor Application
                ↓
4. Fill Form → 5. Save as Draft → 6. Submit for Approval
                ↓
7. View Application Status → 8. (If rejected) Edit & Resubmit
```

### User Journey: Approver (承認者)
```
1. Login → 2. Dashboard (Pending Applications List)
                ↓
3. View Application Details → 4. Review Information
                ↓
5. Approve OR Reject (with comment)
                ↓
6. Application Status Updated
```

### User Journey: Admin (管理者)
```
1. Login → 2. Dashboard (All Applications)
                ↓
3. Manage Applications (View/Edit/Delete/Approve)
                ↓
4. User Management (Assign Roles)
```

## Implementation Strategy

### Phase 1: Basic Structure & Mock Data (2-3 hours)
- Setup Next.js 15 project with TypeScript
- Create basic UI components (shadcn/ui)
- Implement mock vendor data and user sessions
- Create basic routing and navigation

### Phase 2: Auth Platform Integration (3-4 hours)
- Implement AuthPlatformClient
- Create authorization hooks and utilities
- Add AuthGuard component
- Integrate authorization checks in UI

### Phase 3: Business Logic & Workflows (2-3 hours)
- Implement vendor application CRUD operations
- Add approval workflow
- Role-based dashboard views
- Status transitions and validations

### Phase 4: Documentation & Polish (2-3 hours)
- Write comprehensive README.md
- Add inline code comments explaining authorization
- Create AUTHORIZATION_GUIDE.md
- Add error handling and loading states

## Security Considerations

### Defense in Depth
1. **Client-Side**: UI elements hidden/disabled based on permissions
2. **Server-Side**: All mutations protected by authorization checks
3. **API-Level**: Backend enforces authorization (Auth Platform)

### Fail-Safe Defaults
- Default to DENY if authorization check fails
- Log authorization failures for audit
- Display generic error messages to users

### Performance Optimizations
1. **Batch Authorization**: Check multiple permissions at once
2. **Client-Side Caching**: Cache authorization results (with TTL)
3. **Optimistic UI**: Show UI first, verify permissions in background
4. **Server Components**: Leverage React Server Components for server-side auth checks

## Testing Strategy

### Authorization Test Cases
```typescript
describe('Vendor Authorization', () => {
  it('should allow applicant to create vendor', async () => {
    // Test RBAC permission: vendor:create
  });

  it('should allow applicant to edit own draft vendor', async () => {
    // Test resource-level permission: vendor:update:own
  });

  it('should deny applicant from editing submitted vendor', async () => {
    // Test status-based authorization
  });

  it('should allow approver to approve vendor', async () => {
    // Test RBAC permission: vendor:approve
  });

  it('should deny approver from editing vendor details', async () => {
    // Test permission boundary
  });

  it('should allow admin to perform all operations', async () => {
    // Test admin privileges
  });
});
```

## Documentation Structure

### README.md
- Quick start guide
- Environment setup
- Role-based testing instructions
- Authorization implementation overview

### AUTHORIZATION_GUIDE.md
- Deep dive into Auth Platform integration
- Authorization patterns and best practices
- Code examples with explanations
- Troubleshooting common issues

## Success Metrics

### Functional Requirements
- ✅ All CRUD operations protected by authorization
- ✅ Role-based access control working correctly
- ✅ Approval workflow functioning as expected
- ✅ Auth Platform API integration successful

### Non-Functional Requirements
- ✅ Authorization checks complete in < 100ms (p95)
- ✅ UI responsive with optimistic updates
- ✅ Clear error messages for unauthorized actions
- ✅ Comprehensive documentation

## Trade-offs and Decisions

### Decision 1: Mock Data vs. Database
**Choice**: Use in-memory mock data
**Rationale**: Focus is on authorization integration, not data persistence
**Trade-off**: Not production-ready, but simpler to setup and understand

### Decision 2: Simple Auth vs. Full OAuth
**Choice**: Simple email/password with role selection
**Rationale**: Demonstration purposes only
**Trade-off**: Not secure for production, but easier to test different roles

### Decision 3: Inline Authorization vs. Middleware
**Choice**: Use both - inline for clarity, middleware for DRY
**Rationale**: Educational value in showing explicit checks, efficiency in middleware
**Trade-off**: Some code duplication, but better learning experience
