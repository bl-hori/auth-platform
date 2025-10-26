# API Integration Guide

## Table of Contents
- [Overview](#overview)
- [Authentication](#authentication)
- [Base URL](#base-url)
- [Rate Limiting](#rate-limiting)
- [Authorization API](#authorization-api)
- [User Management API](#user-management-api)
- [Role Management API](#role-management-api)
- [Error Handling](#error-handling)
- [Code Examples](#code-examples)
- [Interactive Documentation](#interactive-documentation)

## Overview

The Auth Platform provides a comprehensive RESTful API for fine-grained authorization, user management, and role-based access control (RBAC). This guide will help you integrate the Auth Platform API into your application.

### Key Features
- **Policy-Based Access Control**: Define authorization policies using Open Policy Agent (OPA) and Rego
- **Role-Based Access Control (RBAC)**: Hierarchical role management with inheritance
- **Multi-Tenancy**: Organization-scoped isolation
- **High Performance**: Multi-layer caching (L1: Caffeine, L2: Redis)
- **Audit Logging**: Comprehensive audit trail
- **Rate Limiting**: Per-organization and per-API-key rate limiting

## Authentication

All API requests must include an API key in the request header:

```
X-API-Key: your-api-key-here
```

**Important**: API keys are organization-specific and provide access to resources within that organization only.

### Development API Keys

For development and testing, the following API keys are available:

```bash
# Organization 1
X-API-Key: dev-key-org1-abc123

# Organization 2
X-API-Key: dev-key-org2-def456

# Test Organization
X-API-Key: test-key-xyz789
```

**Warning**: Do not use these keys in production. Configure production API keys using environment variables or a secrets management system.

## Base URL

### Development
```
http://localhost:8080
```

### Staging
```
https://staging.authplatform.io
```

### Production
```
https://api.authplatform.io
```

## Rate Limiting

The API implements rate limiting to ensure fair usage and system stability:

- **Default**: 100 requests per minute per organization
- **Burst capacity**: 100 requests
- **Headers**: Rate limit information is included in response headers

```
X-RateLimit-Remaining: 95
X-RateLimit-Retry-After-Seconds: 5
```

When rate limit is exceeded, the API returns HTTP 429 (Too Many Requests).

## Authorization API

### Check Authorization

Evaluate whether a principal is authorized to perform an action on a resource.

**Endpoint**: `POST /v1/authorize`

**Request Body**:
```json
{
  "organizationId": "550e8400-e29b-41d4-a716-446655440000",
  "principalId": "user-123",
  "action": "document:read",
  "resource": "document:confidential-report-2024",
  "context": {
    "ip_address": "192.168.1.1",
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

**Response**:
```json
{
  "decision": "ALLOW",
  "reason": "User has 'document:read' permission through 'Manager' role",
  "evaluationTimeMs": 2
}
```

**Decision Values**:
- `ALLOW`: Access granted
- `DENY`: Access denied
- `ERROR`: Error during evaluation

### Batch Authorization

Check multiple authorization decisions in a single request for better performance.

**Endpoint**: `POST /v1/authorize/batch`

**Request Body**:
```json
{
  "requests": [
    {
      "organizationId": "550e8400-e29b-41d4-a716-446655440000",
      "principalId": "user-123",
      "action": "document:read",
      "resource": "document:report-1"
    },
    {
      "organizationId": "550e8400-e29b-41d4-a716-446655440000",
      "principalId": "user-123",
      "action": "document:write",
      "resource": "document:report-1"
    }
  ]
}
```

**Response**:
```json
{
  "responses": [
    {
      "decision": "ALLOW",
      "reason": "User has 'document:read' permission",
      "evaluationTimeMs": 1
    },
    {
      "decision": "DENY",
      "reason": "User lacks 'document:write' permission",
      "evaluationTimeMs": 1
    }
  ]
}
```

## User Management API

### Create User

**Endpoint**: `POST /v1/users`

**Request Body**:
```json
{
  "organizationId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john.doe@example.com",
  "username": "johndoe",
  "displayName": "John Doe",
  "externalId": "ext-12345",
  "status": "ACTIVE",
  "attributes": {
    "department": "Engineering",
    "location": "San Francisco"
  }
}
```

**Response**: `201 Created`
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "organizationId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john.doe@example.com",
  "username": "johndoe",
  "displayName": "John Doe",
  "externalId": "ext-12345",
  "status": "ACTIVE",
  "attributes": {
    "department": "Engineering",
    "location": "San Francisco"
  },
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z",
  "deletedAt": null
}
```

### Get User

**Endpoint**: `GET /v1/users/{userId}`

**Response**: `200 OK`
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "organizationId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john.doe@example.com",
  "username": "johndoe",
  "displayName": "John Doe",
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### List Users

**Endpoint**: `GET /v1/users?organizationId={orgId}&search={query}&status={status}&page={page}&size={size}`

**Query Parameters**:
- `organizationId` (required): Organization UUID
- `search` (optional): Search by email, username, or display name
- `status` (optional): Filter by status (ACTIVE, INACTIVE, PENDING)
- `page` (optional): Page number (0-indexed, default: 0)
- `size` (optional): Page size (default: 20, max: 100)

**Response**: `200 OK`
```json
{
  "users": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "email": "john.doe@example.com",
      "username": "johndoe",
      "displayName": "John Doe",
      "status": "ACTIVE"
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 20
}
```

### Update User

**Endpoint**: `PUT /v1/users/{userId}`

**Request Body**:
```json
{
  "email": "john.doe@example.com",
  "displayName": "John Doe (Senior Engineer)",
  "status": "ACTIVE",
  "attributes": {
    "department": "Engineering",
    "location": "New York",
    "role_level": "Senior"
  }
}
```

**Response**: `200 OK` (same format as Create User)

### Deactivate User

**Endpoint**: `DELETE /v1/users/{userId}`

**Response**: `204 No Content`

This performs a soft delete (sets `deletedAt` timestamp). The user record is retained for audit purposes.

### Activate User

**Endpoint**: `POST /v1/users/{userId}/activate`

**Response**: `200 OK` (returns updated user)

## Role Management API

### Create Role

**Endpoint**: `POST /v1/roles`

**Request Body**:
```json
{
  "organizationId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "senior-engineer",
  "displayName": "Senior Engineer",
  "description": "Senior engineering role with elevated permissions",
  "parentRoleId": "770e8400-e29b-41d4-a716-446655440002",
  "metadata": {
    "department": "Engineering",
    "approval_required": false
  }
}
```

**Response**: `201 Created`
```json
{
  "id": "880e8400-e29b-41d4-a716-446655440003",
  "organizationId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "senior-engineer",
  "displayName": "Senior Engineer",
  "description": "Senior engineering role with elevated permissions",
  "parentRoleId": "770e8400-e29b-41d4-a716-446655440002",
  "level": 2,
  "isSystem": false,
  "metadata": {
    "department": "Engineering",
    "approval_required": false
  },
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### Get Role

**Endpoint**: `GET /v1/roles/{roleId}`

**Response**: `200 OK` (same format as Create Role response)

### List Roles

**Endpoint**: `GET /v1/roles?organizationId={orgId}&page={page}&size={size}`

**Response**: `200 OK`
```json
{
  "roles": [
    {
      "id": "880e8400-e29b-41d4-a716-446655440003",
      "name": "senior-engineer",
      "displayName": "Senior Engineer",
      "level": 2,
      "isSystem": false
    }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

### Get Role Hierarchy

**Endpoint**: `GET /v1/roles/{roleId}/hierarchy`

**Response**: `200 OK`
```json
{
  "role": {
    "id": "880e8400-e29b-41d4-a716-446655440003",
    "name": "senior-engineer",
    "level": 2
  },
  "ancestors": [
    {
      "id": "770e8400-e29b-41d4-a716-446655440002",
      "name": "engineer",
      "level": 1
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "name": "employee",
      "level": 0
    }
  ],
  "descendants": []
}
```

### Update Role

**Endpoint**: `PUT /v1/roles/{roleId}`

**Request Body**: Same as Create Role (excluding organizationId)

**Response**: `200 OK`

### Delete Role

**Endpoint**: `DELETE /v1/roles/{roleId}`

**Response**: `204 No Content`

**Note**: Cannot delete roles that have child roles or are assigned to users.

## Error Handling

The API uses standard HTTP status codes and returns detailed error messages:

### Common Status Codes

| Status Code | Description |
|-------------|-------------|
| 200 OK | Request succeeded |
| 201 Created | Resource created successfully |
| 204 No Content | Request succeeded, no response body |
| 400 Bad Request | Invalid request parameters |
| 401 Unauthorized | Missing or invalid API key |
| 403 Forbidden | Insufficient permissions |
| 404 Not Found | Resource not found |
| 409 Conflict | Resource conflict (e.g., duplicate email) |
| 429 Too Many Requests | Rate limit exceeded |
| 500 Internal Server Error | Server error |

### Error Response Format

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Email is required",
  "path": "/v1/users"
}
```

### Validation Errors

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Email must be valid"
    },
    {
      "field": "username",
      "message": "Username must contain only alphanumeric characters"
    }
  ],
  "path": "/v1/users"
}
```

## Code Examples

### Java

```java
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class AuthPlatformClient {
    private static final String API_KEY = "dev-key-org1-abc123";
    private static final String BASE_URL = "http://localhost:8080";

    public boolean checkAuthorization(String userId, String action, String resource) {
        RestTemplate restTemplate = new RestTemplate();

        // Create request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", API_KEY);

        String requestBody = String.format("""
            {
              "organizationId": "550e8400-e29b-41d4-a716-446655440000",
              "principalId": "%s",
              "action": "%s",
              "resource": "%s"
            }
            """, userId, action, resource);

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        // Make request
        ResponseEntity<AuthorizationResponse> response = restTemplate.postForEntity(
            BASE_URL + "/v1/authorize",
            request,
            AuthorizationResponse.class
        );

        return response.getBody().getDecision().equals("ALLOW");
    }
}

class AuthorizationResponse {
    private String decision;
    private String reason;
    private long evaluationTimeMs;

    // Getters and setters
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
}
```

### JavaScript/TypeScript

```typescript
// authPlatformClient.ts
const API_KEY = 'dev-key-org1-abc123';
const BASE_URL = 'http://localhost:8080';

interface AuthorizationRequest {
  organizationId: string;
  principalId: string;
  action: string;
  resource: string;
  context?: Record<string, any>;
}

interface AuthorizationResponse {
  decision: 'ALLOW' | 'DENY' | 'ERROR';
  reason: string;
  evaluationTimeMs: number;
}

export async function checkAuthorization(
  userId: string,
  action: string,
  resource: string
): Promise<boolean> {
  const request: AuthorizationRequest = {
    organizationId: '550e8400-e29b-41d4-a716-446655440000',
    principalId: userId,
    action,
    resource,
  };

  const response = await fetch(`${BASE_URL}/v1/authorize`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': API_KEY,
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error(`Authorization check failed: ${response.statusText}`);
  }

  const result: AuthorizationResponse = await response.json();
  return result.decision === 'ALLOW';
}

// Example usage
async function main() {
  try {
    const isAllowed = await checkAuthorization(
      'user-123',
      'document:read',
      'document:confidential-report-2024'
    );

    console.log(`Access ${isAllowed ? 'granted' : 'denied'}`);
  } catch (error) {
    console.error('Error:', error);
  }
}
```

### Python

```python
import requests
from typing import Dict, Any, Optional

API_KEY = "dev-key-org1-abc123"
BASE_URL = "http://localhost:8080"

class AuthPlatformClient:
    def __init__(self, api_key: str, base_url: str = BASE_URL):
        self.api_key = api_key
        self.base_url = base_url
        self.headers = {
            "Content-Type": "application/json",
            "X-API-Key": api_key
        }

    def check_authorization(
        self,
        user_id: str,
        action: str,
        resource: str,
        context: Optional[Dict[str, Any]] = None
    ) -> bool:
        """Check if a user is authorized to perform an action on a resource."""
        request_data = {
            "organizationId": "550e8400-e29b-41d4-a716-446655440000",
            "principalId": user_id,
            "action": action,
            "resource": resource,
        }

        if context:
            request_data["context"] = context

        response = requests.post(
            f"{self.base_url}/v1/authorize",
            json=request_data,
            headers=self.headers
        )

        response.raise_for_status()
        result = response.json()

        return result["decision"] == "ALLOW"

    def create_user(
        self,
        organization_id: str,
        email: str,
        username: Optional[str] = None,
        display_name: Optional[str] = None,
        attributes: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Create a new user."""
        request_data = {
            "organizationId": organization_id,
            "email": email,
            "status": "ACTIVE"
        }

        if username:
            request_data["username"] = username
        if display_name:
            request_data["displayName"] = display_name
        if attributes:
            request_data["attributes"] = attributes

        response = requests.post(
            f"{self.base_url}/v1/users",
            json=request_data,
            headers=self.headers
        )

        response.raise_for_status()
        return response.json()

# Example usage
if __name__ == "__main__":
    client = AuthPlatformClient(API_KEY)

    # Check authorization
    is_allowed = client.check_authorization(
        user_id="user-123",
        action="document:read",
        resource="document:confidential-report-2024"
    )

    print(f"Access {'granted' if is_allowed else 'denied'}")

    # Create user
    user = client.create_user(
        organization_id="550e8400-e29b-41d4-a716-446655440000",
        email="jane.doe@example.com",
        username="janedoe",
        display_name="Jane Doe",
        attributes={"department": "Engineering"}
    )

    print(f"Created user: {user['id']}")
```

### cURL

```bash
# Check authorization
curl -X POST http://localhost:8080/v1/authorize \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -d '{
    "organizationId": "550e8400-e29b-41d4-a716-446655440000",
    "principalId": "user-123",
    "action": "document:read",
    "resource": "document:confidential-report-2024"
  }'

# Create user
curl -X POST http://localhost:8080/v1/users \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -d '{
    "organizationId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john.doe@example.com",
    "username": "johndoe",
    "displayName": "John Doe",
    "status": "ACTIVE"
  }'

# List users
curl -X GET "http://localhost:8080/v1/users?organizationId=550e8400-e29b-41d4-a716-446655440000&page=0&size=20" \
  -H "X-API-Key: dev-key-org1-abc123"

# Get user
curl -X GET http://localhost:8080/v1/users/660e8400-e29b-41d4-a716-446655440001 \
  -H "X-API-Key: dev-key-org1-abc123"
```

## Interactive Documentation

The Auth Platform provides interactive API documentation using Swagger UI.

### Accessing Swagger UI

**URL**: `http://localhost:8080/swagger-ui.html`

Features:
- Browse all available endpoints
- View request/response schemas
- Try out API calls directly from the browser
- View detailed parameter descriptions
- See example requests and responses

### OpenAPI Specification

The complete OpenAPI 3.0 specification is available at:

**URL**: `http://localhost:8080/v3/api-docs`

You can use this specification to:
- Generate client libraries using OpenAPI Generator
- Import into API testing tools (Postman, Insomnia)
- Integrate with API gateways
- Generate documentation

### Example: Import to Postman

1. Open Postman
2. Click "Import" in the top left
3. Select "Link" tab
4. Enter: `http://localhost:8080/v3/api-docs`
5. Click "Continue" and "Import"

All API endpoints will be imported as a Postman collection.

## Best Practices

### 1. Cache Authorization Decisions

Cache authorization decisions in your application to minimize API calls:

```typescript
// Simple in-memory cache with TTL
const authCache = new Map<string, { decision: boolean; expiresAt: number }>();

async function checkAuthorizationCached(userId: string, action: string, resource: string): Promise<boolean> {
  const cacheKey = `${userId}:${action}:${resource}`;
  const cached = authCache.get(cacheKey);

  if (cached && cached.expiresAt > Date.now()) {
    return cached.decision;
  }

  const decision = await checkAuthorization(userId, action, resource);

  authCache.set(cacheKey, {
    decision,
    expiresAt: Date.now() + 10000 // 10 seconds
  });

  return decision;
}
```

### 2. Use Batch Endpoints

When checking multiple permissions, use the batch endpoint:

```typescript
const results = await batchCheckAuthorization([
  { userId: 'user-123', action: 'document:read', resource: 'doc-1' },
  { userId: 'user-123', action: 'document:write', resource: 'doc-1' },
  { userId: 'user-123', action: 'document:delete', resource: 'doc-1' },
]);
```

### 3. Handle Errors Gracefully

Always implement proper error handling:

```typescript
try {
  const isAllowed = await checkAuthorization(userId, action, resource);
  // Proceed based on decision
} catch (error) {
  if (error.response?.status === 401) {
    // Invalid API key
    console.error('Invalid API key');
  } else if (error.response?.status === 429) {
    // Rate limit exceeded
    console.error('Rate limit exceeded, retry after:', error.response.headers['x-ratelimit-retry-after-seconds']);
  } else {
    // Default to deny on error
    console.error('Authorization check failed:', error);
  }
  return false; // Fail closed
}
```

### 4. Implement Retry Logic

Implement exponential backoff for transient errors:

```typescript
async function checkAuthorizationWithRetry(
  userId: string,
  action: string,
  resource: string,
  maxRetries: number = 3
): Promise<boolean> {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await checkAuthorization(userId, action, resource);
    } catch (error) {
      if (i === maxRetries - 1 || error.response?.status === 401) {
        throw error; // Don't retry auth errors or final attempt
      }
      await new Promise(resolve => setTimeout(resolve, Math.pow(2, i) * 1000));
    }
  }
  throw new Error('Max retries exceeded');
}
```

## Support

For additional support:
- **Documentation**: https://docs.authplatform.io
- **Email**: support@authplatform.io
- **GitHub Issues**: https://github.com/authplatform/auth-platform/issues

## License

MIT License - See LICENSE file for details
