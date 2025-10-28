# JWT Authentication Guide

This guide explains how to use JWT authentication with the Auth Platform Backend API.

## Table of Contents

- [Overview](#overview)
- [Getting Started](#getting-started)
- [Obtaining a JWT Token](#obtaining-a-jwt-token)
- [Using JWT with Backend API](#using-jwt-with-backend-api)
- [JWT Claims Structure](#jwt-claims-structure)
- [Authentication Flow](#authentication-flow)
- [Just-In-Time (JIT) Provisioning](#just-in-time-jit-provisioning)
- [Error Handling](#error-handling)
- [Migration from API Keys](#migration-from-api-keys)
- [Troubleshooting](#troubleshooting)

## Overview

Auth Platform Backend supports **hybrid authentication** with two methods:

1. **JWT Authentication** (Recommended): Token-based authentication using Keycloak
2. **API Key Authentication**: Legacy method for backward compatibility

JWT authentication takes precedence when both methods are provided.

### Benefits of JWT Authentication

- ✅ **Secure**: Cryptographically signed tokens (RS256)
- ✅ **Stateless**: No server-side session management
- ✅ **Standards-based**: OpenID Connect (OIDC) compliant
- ✅ **User Identity**: Automatic user provisioning and linking
- ✅ **Multi-tenant**: Organization isolation via claims

## Getting Started

### Prerequisites

- Keycloak server running (see [KEYCLOAK_SETUP.md](./KEYCLOAK_SETUP.md))
- User account in Keycloak
- Organization configured in Auth Platform

### Configuration

JWT authentication is enabled by default. Configuration in `application.yml`:

```yaml
authplatform:
  keycloak:
    enabled: true
    base-url: http://localhost:8180
    realm: authplatform
    issuer-uri: http://localhost:8180/realms/authplatform
    jwk-set-uri: http://localhost:8180/realms/authplatform/protocol/openid-connect/certs
    jwt:
      public-key-cache-ttl: 3600
      clock-skew-seconds: 30
      expected-audience: auth-platform-backend
```

## Obtaining a JWT Token

### Method 1: Keycloak Admin Console

1. Open Keycloak Admin Console: `http://localhost:8180`
2. Select realm: `authplatform`
3. Go to **Clients** → Select your client
4. Go to **Credentials** tab
5. Copy the client secret
6. Use Token Endpoint to get JWT

### Method 2: Direct Token Request

#### Using curl

```bash
# Get access token
curl -X POST "http://localhost:8180/realms/authplatform/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=auth-platform-backend" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "grant_type=client_credentials"
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

#### Using Postman

1. Create new request
2. Authorization tab → Type: OAuth 2.0
3. Configure OAuth:
   - **Grant Type**: Client Credentials
   - **Access Token URL**: `http://localhost:8180/realms/authplatform/protocol/openid-connect/token`
   - **Client ID**: `auth-platform-backend`
   - **Client Secret**: Your secret
4. Click **Get New Access Token**

### Method 3: Resource Owner Password Flow (Development Only)

```bash
curl -X POST "http://localhost:8180/realms/authplatform/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=auth-platform-backend" \
  -d "username=user@example.com" \
  -d "password=password123" \
  -d "grant_type=password"
```

⚠️ **Warning**: Only use password grant in development. Use Authorization Code Flow in production.

## Using JWT with Backend API

### Making Authenticated Requests

Include the JWT in the `Authorization` header with `Bearer` prefix:

```bash
curl -X GET "http://localhost:8080/v1/users" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Example: List Users

```bash
# Get JWT token
TOKEN=$(curl -s -X POST "http://localhost:8180/realms/authplatform/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=auth-platform-backend" \
  -d "client_secret=YOUR_SECRET" \
  -d "grant_type=client_credentials" | jq -r '.access_token')

# Use token to list users
curl -X GET "http://localhost:8080/v1/users" \
  -H "Authorization: Bearer $TOKEN"
```

### Example: Create User

```bash
curl -X POST "http://localhost:8080/v1/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "displayName": "New User",
    "organizationId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

## JWT Claims Structure

### Required Claims

| Claim | Type | Description | Example |
|-------|------|-------------|---------|
| `sub` | String | Keycloak user ID | `550e8400-e29b-41d4-a716-446655440000` |
| `organization_id` | String | Organization UUID | `org-uuid-123` |
| `iss` | String | Token issuer | `http://localhost:8180/realms/authplatform` |
| `aud` | String | Expected audience | `auth-platform-backend` |
| `exp` | Number | Expiration timestamp | `1698765432` |
| `iat` | Number | Issued at timestamp | `1698765132` |

### Optional Claims

| Claim | Type | Description | Example |
|-------|------|-------------|---------|
| `email` | String | User email address | `user@example.com` |
| `preferred_username` | String | Username | `john.doe` |
| `name` | String | Full name | `John Doe` |
| `roles` | Array | User roles | `["admin", "user"]` |

### Example JWT Payload

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "organization_id": "org-uuid-456",
  "preferred_username": "user@example.com",
  "name": "John Doe",
  "roles": ["user"],
  "iss": "http://localhost:8180/realms/authplatform",
  "aud": "auth-platform-backend",
  "exp": 1698765432,
  "iat": 1698765132
}
```

## Authentication Flow

### High-Level Flow

```
┌─────────┐         ┌──────────┐         ┌─────────────┐         ┌──────────┐
│ Client  │         │ Keycloak │         │ Auth        │         │ Database │
│         │         │          │         │ Platform    │         │          │
└────┬────┘         └────┬─────┘         └──────┬──────┘         └────┬─────┘
     │                   │                      │                     │
     │ 1. Request Token  │                      │                     │
     ├──────────────────>│                      │                     │
     │                   │                      │                     │
     │ 2. JWT Token      │                      │                     │
     │<──────────────────┤                      │                     │
     │                   │                      │                     │
     │ 3. API Request with JWT                  │                     │
     ├──────────────────────────────────────────>│                     │
     │                   │                      │                     │
     │                   │ 4. Validate JWT      │                     │
     │                   │<─────────────────────┤                     │
     │                   │                      │                     │
     │                   │ 5. JWT Valid         │                     │
     │                   ├─────────────────────>│                     │
     │                   │                      │                     │
     │                   │                      │ 6. Find/Create User │
     │                   │                      ├────────────────────>│
     │                   │                      │                     │
     │                   │                      │ 7. User Record      │
     │                   │                      │<────────────────────┤
     │                   │                      │                     │
     │ 8. API Response                          │                     │
     │<─────────────────────────────────────────┤                     │
```

### Detailed Filter Chain

1. **RateLimitFilter**: Check rate limits by IP
2. **JwtAuthenticationFilter**: Validate JWT and provision user
   - Extract JWT from `Authorization: Bearer <token>` header
   - Decode and validate JWT signature using Keycloak public key
   - Validate claims: issuer, audience, expiration
   - Extract user identity: `sub`, `email`, `organization_id`
   - Find or create user (JIT Provisioning)
   - Set `SecurityContext` with authenticated user
3. **ApiKeyAuthenticationFilter**: Fallback authentication (if JWT not present)

## Just-In-Time (JIT) Provisioning

Auth Platform automatically creates or links users based on JWT claims.

### JIT Provisioning Flow

```
┌─────────────────────────────────────────┐
│ JWT Authentication Request              │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ Search by keycloak_sub                  │
│ (Fastest - indexed lookup)              │
└────────┬────────────────────────────────┘
         │
         ├─ Found? ──> Update keycloak_synced_at ──> Return User
         │
         ▼ Not Found
┌─────────────────────────────────────────┐
│ Search by email                         │
│ (Link existing user to Keycloak)       │
└────────┬────────────────────────────────┘
         │
         ├─ Found? ──> Set keycloak_sub ──> Update keycloak_synced_at ──> Return User
         │
         ▼ Not Found
┌─────────────────────────────────────────┐
│ Create New User                         │
│ - Set keycloak_sub                      │
│ - Set email (if provided)               │
│ - Set organization from JWT claim       │
│ - Set status = ACTIVE                   │
│ - Set keycloak_synced_at                │
└────────┬────────────────────────────────┘
         │
         ▼
     Return User
```

### Scenarios

#### Scenario 1: First-time JWT Login (New User)

```
User: user@example.com (Keycloak ID: kc-123)
Database: No matching user

Result: New user created
- keycloak_sub: kc-123
- email: user@example.com
- status: ACTIVE
```

#### Scenario 2: Existing User (Email Match)

```
User: user@example.com (Keycloak ID: kc-456)
Database: User exists with email "user@example.com" but no keycloak_sub

Result: Existing user linked to Keycloak
- keycloak_sub: kc-456 (updated)
- email: user@example.com (unchanged)
- keycloak_synced_at: now
```

#### Scenario 3: Returning User

```
User: user@example.com (Keycloak ID: kc-123)
Database: User exists with keycloak_sub "kc-123"

Result: User authenticated
- keycloak_synced_at: updated to current time
```

## Error Handling

### Common Error Responses

#### 401 Unauthorized - Invalid JWT

```json
{
  "timestamp": "2024-01-28T10:15:30.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid JWT token: Signature verification failed",
  "path": "/v1/users"
}
```

**Causes**:
- JWT signature verification failed
- Token not signed by Keycloak
- Wrong public key

#### 401 Unauthorized - Expired JWT

```json
{
  "timestamp": "2024-01-28T10:15:30.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid JWT token: JWT expired",
  "path": "/v1/users"
}
```

**Causes**:
- Token `exp` claim is in the past
- Clock skew too large

#### 401 Unauthorized - Missing Required Claim

```json
{
  "timestamp": "2024-01-28T10:15:30.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token missing required claim: organization_id",
  "path": "/v1/users"
}
```

**Causes**:
- JWT missing `organization_id` claim
- Incorrect Keycloak client configuration

#### 401 Unauthorized - Invalid Organization

```json
{
  "timestamp": "2024-01-28T10:15:30.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication failed: Organization not found: org-uuid-123",
  "path": "/v1/users"
}
```

**Causes**:
- Organization in JWT claim doesn't exist in database
- Incorrect `organization_id` claim value

### Error Response Structure

All authentication errors return HTTP 401 with:

```json
{
  "timestamp": "ISO-8601 timestamp",
  "status": 401,
  "error": "Unauthorized",
  "message": "Detailed error message",
  "path": "Request path"
}
```

## Migration from API Keys

### Hybrid Authentication Period

During migration, both JWT and API Key authentication are supported:

```bash
# Option 1: JWT Authentication (Recommended)
curl -H "Authorization: Bearer <JWT>" http://localhost:8080/v1/users

# Option 2: API Key Authentication (Legacy)
curl -H "X-API-Key: <API-KEY>" http://localhost:8080/v1/users

# Option 3: Both (JWT takes precedence)
curl -H "Authorization: Bearer <JWT>" \
     -H "X-API-Key: <API-KEY>" \
     http://localhost:8080/v1/users
```

### Migration Strategy

#### Phase 1: Add JWT Support (Current)

- ✅ JWT authentication enabled alongside API Keys
- ✅ Existing API Key users continue working
- ✅ New users can use JWT authentication
- ✅ Gradual rollout to clients

#### Phase 2: Migrate Clients

1. **Update client applications** to use JWT
2. **Test thoroughly** with both authentication methods
3. **Monitor logs** for API Key usage
4. **Deprecation notice** for API Key authentication

#### Phase 3: Deprecate API Keys (Future)

1. Set `authplatform.security.api-keys.enabled: false`
2. Remove API Key authentication filter
3. API Key requests return 401 Unauthorized

### Migration Checklist

- [ ] Update client application to obtain JWT from Keycloak
- [ ] Update client application to use `Authorization: Bearer <JWT>` header
- [ ] Test JWT authentication in staging environment
- [ ] Deploy JWT-enabled client to production
- [ ] Monitor authentication logs
- [ ] Remove API Key code after successful migration

## Troubleshooting

### JWT Token Not Working

**Symptom**: 401 Unauthorized with "Invalid JWT token"

**Solutions**:

1. **Verify JWT format**:
   ```bash
   # Decode JWT to check structure (https://jwt.io)
   echo "YOUR_JWT" | cut -d. -f2 | base64 -d | jq
   ```

2. **Check token expiration**:
   ```bash
   # Check exp claim
   echo "YOUR_JWT" | cut -d. -f2 | base64 -d | jq '.exp'
   date -d @<EXP_VALUE>
   ```

3. **Verify issuer**:
   - JWT `iss` must match `authplatform.keycloak.issuer-uri`

4. **Verify audience**:
   - JWT `aud` must match `authplatform.keycloak.jwt.expected-audience`

### Missing organization_id Claim

**Symptom**: 401 Unauthorized with "JWT token missing required claim: organization_id"

**Solutions**:

1. **Add organization_id mapper in Keycloak**:
   - Go to Client → Mappers → Create
   - Mapper Type: User Attribute
   - User Attribute: organization_id
   - Token Claim Name: organization_id

2. **Set user attribute in Keycloak**:
   - Go to Users → Select user → Attributes
   - Add attribute: `organization_id` = `your-org-uuid`

### User Not Created After JWT Login

**Symptom**: JWT authentication succeeds but user not found in database

**Solutions**:

1. **Check logs** for JIT provisioning errors:
   ```bash
   grep "JIT provisioning" logs/application.log
   ```

2. **Verify organization exists**:
   ```sql
   SELECT * FROM organizations WHERE id = 'organization-uuid';
   ```

3. **Check database permissions**:
   - Ensure application has INSERT permission on `users` table

### Clock Skew Issues

**Symptom**: Valid JWT rejected with "JWT expired" immediately after generation

**Solutions**:

1. **Synchronize clocks**:
   ```bash
   # Check system time
   date

   # Sync with NTP (Linux)
   sudo ntpdate time.nist.gov
   ```

2. **Increase clock skew tolerance**:
   ```yaml
   authplatform:
     keycloak:
       jwt:
         clock-skew-seconds: 60  # Allow 60 seconds skew
   ```

## Best Practices

### Security

- ✅ **Use HTTPS** in production
- ✅ **Keep tokens short-lived** (5-15 minutes)
- ✅ **Implement token refresh** for long-running clients
- ✅ **Never log JWT tokens** (contains sensitive data)
- ✅ **Validate audience claim** to prevent token reuse across services
- ✅ **Monitor authentication failures** for suspicious activity

### Performance

- ✅ **Cache public keys** (configured via `public-key-cache-ttl`)
- ✅ **Reuse JWT tokens** until expiration
- ✅ **Use connection pooling** for Keycloak requests
- ✅ **Monitor JIT provisioning latency**

### Development

- ✅ **Use different realms** for dev/staging/production
- ✅ **Test with expired tokens** to verify error handling
- ✅ **Document custom JWT claims** used by your application
- ✅ **Version your Keycloak configuration** (export/import)

## Additional Resources

- [Keycloak Setup Guide](./KEYCLOAK_SETUP.md)
- [Keycloak Integration Guide](./KEYCLOAK_INTEGRATION.md)
- [API Documentation](./API.md)
- [Security Best Practices](./SECURITY.md)
- [Keycloak Official Documentation](https://www.keycloak.org/documentation)
- [OpenID Connect Specification](https://openid.net/connect/)

## Support

For issues or questions:

- GitHub Issues: https://github.com/your-org/auth-platform/issues
- Documentation: https://docs.auth-platform.example.com
- Email: support@example.com
