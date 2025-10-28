# Vendor Registration Example - Auth Platform Integration

This is a sample Next.js application demonstrating how to integrate with Auth Platform for authorization and access control in a vendor registration system.

## Features

- **Vendor Management**: Create, view, update, and delete vendor information
- **Authorization**: Role-based access control using Auth Platform
- **Real-time Authorization**: Dynamic permission checks for every action
- **Approval Workflow**: Manager approval system for vendor applications
- **Loading States**: UX-optimized loading and error handling

## Prerequisites

Before running this example, make sure you have:

1. **Auth Platform Backend** running on `http://localhost:8080`
   ```bash
   cd backend
   ./gradlew bootRun
   ```

2. **PostgreSQL** and **Redis** running (via Docker Compose)
   ```bash
   cd infrastructure
   docker compose up -d
   ```

## Setup

### 1. Install Dependencies

```bash
pnpm install
```

### 2. Configure Environment Variables

Create a `.env.local` file from the example:

```bash
cp .env.example .env.local
```

The default configuration uses:
- **API Key**: `dev-key-org1-abc123` (defined in `backend/src/main/resources/application.yml`)
- **Organization ID**: `00000000-0000-0000-0000-000000000000` (system organization)
- **Backend URL**: `http://localhost:8080`
- **Demo Mode**: `true` (bypasses Auth Platform API calls)

**Important**: These credentials are for development only. Never use them in production!

#### Demo Mode vs. Full Integration Mode

This example supports two modes:

**Demo Mode (Default - Recommended for Quick Start)**:
- Set `NEXT_PUBLIC_DEMO_MODE=true` in `.env.local`
- All authorization checks return `ALLOW` without calling Auth Platform API
- Perfect for exploring the UI and workflow without backend setup
- No user registration required in Auth Platform

**Full Integration Mode**:
- Set `NEXT_PUBLIC_DEMO_MODE=false` in `.env.local`
- Real authorization checks against Auth Platform Backend
- Requires users to be registered in Auth Platform database
- See "Full Integration Setup" section below for details

### 3. Verify Backend Connection

Test that the backend is accessible:

```bash
curl -X POST http://localhost:8080/v1/authorize \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "organizationId": "00000000-0000-0000-0000-000000000000",
    "principal": {
      "id": "test-user",
      "type": "user"
    },
    "action": "read",
    "resource": {
      "type": "vendor",
      "id": "test-vendor"
    }
  }'
```

You should receive a JSON response with authorization decision.

### 4. Run Development Server

```bash
pnpm dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

## Project Structure

```
vendor-registration/
├── app/                    # Next.js 15 App Router pages
│   ├── dashboard/          # Dashboard overview
│   ├── vendors/            # Vendor list and detail pages
│   │   ├── [id]/           # Vendor detail page
│   │   └── new/            # New vendor form
│   └── layout.tsx          # Root layout
├── components/             # React components
│   ├── vendors/            # Vendor-specific components
│   ├── ui/                 # shadcn/ui components
│   └── dashboard/          # Dashboard components
├── lib/                    # Utility libraries
│   ├── auth-client.ts      # Auth Platform API client
│   ├── authorization.ts    # Authorization helpers
│   └── utils.ts            # Common utilities
├── types/                  # TypeScript type definitions
│   ├── auth.ts             # Auth-related types
│   └── vendor.ts           # Vendor types
└── .env.local              # Environment configuration (not committed)
```

## Authorization Integration

This example demonstrates several authorization patterns:

### 1. Client-Side Authorization Check

```typescript
import { authClient, checkAuthorization } from '@/lib/authorization';

// Check if user can perform an action
const canEdit = await checkAuthorization({
  userId: currentUser.id,
  action: 'update',
  resourceType: 'vendor',
  resourceId: vendor.id,
});

if (!canEdit) {
  // Hide edit button or show access denied message
}
```

### 2. Using Auth Platform Client Directly

```typescript
import { AuthPlatformClient } from '@/lib/auth-client';

const client = new AuthPlatformClient({
  baseUrl: process.env.NEXT_PUBLIC_AUTH_BACKEND_URL!,
  apiKey: process.env.NEXT_PUBLIC_AUTH_API_KEY!,
  organizationId: process.env.NEXT_PUBLIC_AUTH_ORGANIZATION_ID!,
});

const response = await client.authorize({
  userId: 'user-123',
  action: 'read',
  resourceType: 'vendor',
  resourceId: 'vendor-456',
});

if (response.decision === 'ALLOW') {
  // Grant access
} else {
  // Deny access
  console.log('Access denied:', response.reason);
}
```

### 3. Batch Authorization (Multiple Resources)

```typescript
const results = await client.authorizeBatch({
  requests: [
    {
      userId: 'user-123',
      action: 'read',
      resourceType: 'vendor',
      resourceId: 'vendor-001',
    },
    {
      userId: 'user-123',
      action: 'update',
      resourceType: 'vendor',
      resourceId: 'vendor-002',
    },
  ],
});

// Check results for each request
results.responses.forEach((response, index) => {
  console.log(`Request ${index + 1}: ${response.decision}`);
});
```

## Common Issues

### "Failed to fetch" Error

**Cause**: Backend is not running or environment variables are incorrect.

**Quick Solution**: Enable Demo Mode
```bash
# In .env.local
NEXT_PUBLIC_DEMO_MODE=true
```

**Full Integration Solution**:
1. Verify backend is running: `curl http://localhost:8080/actuator/health`
2. Check `.env.local` exists and has correct values
3. Restart the dev server to reload environment variables: `pnpm dev`

### "Invalid API key" (403 Error)

**Cause**: API key in `.env.local` doesn't match backend configuration.

**Solution**:
1. Check `backend/src/main/resources/application.yml` for valid API keys
2. Update `NEXT_PUBLIC_AUTH_API_KEY` in `.env.local`
3. Restart dev server

### "User not found" Authorization Response

**Cause**: The mock user IDs (user-001, user-002, user-003) don't exist in Auth Platform database.

**Quick Solution**: Enable Demo Mode (Recommended)
```bash
# In .env.local
NEXT_PUBLIC_DEMO_MODE=true
```

This allows you to explore the application without setting up users in Auth Platform.

**Full Integration Solution**: Create users in Auth Platform
```bash
# Create user via API (example)
curl -X POST http://localhost:8080/v1/users \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user-001",
    "email": "tanaka@example.com",
    "displayName": "田中太郎",
    "status": "ACTIVE"
  }'
```

Repeat for user-002 and user-003 to enable full authorization.

## Development

### Build

```bash
pnpm build
```

### Lint

```bash
pnpm lint
```

### Type Check

```bash
pnpm type-check
```

## Learn More

- [Auth Platform Documentation](../../docs/)
- [Next.js 15 Documentation](https://nextjs.org/docs)
- [shadcn/ui Components](https://ui.shadcn.com/)

## License

This example is part of the Auth Platform project.
