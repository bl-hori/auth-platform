/**
 * Type definitions for Auth Platform
 */

/**
 * User entity
 */
export interface User {
  id: string
  organizationId: string
  email: string
  username: string
  displayName: string
  status: UserStatus
  attributes?: Record<string, unknown>
  createdAt: string
  updatedAt: string
  lastLoginAt?: string
}

/**
 * User status enum
 */
export enum UserStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  SUSPENDED = 'SUSPENDED',
}

/**
 * Organization entity
 */
export interface Organization {
  id: string
  name: string
  displayName: string
  status: OrganizationStatus
  settings?: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

/**
 * Organization status enum
 */
export enum OrganizationStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
}

/**
 * Role entity
 */
export interface Role {
  id: string
  organizationId: string
  name: string
  displayName: string
  description?: string
  parentRoleId?: string
  permissions: Permission[]
  createdAt: string
  updatedAt: string
}

/**
 * Permission entity
 */
export interface Permission {
  id: string
  resource: string
  action: string
  description?: string
  createdAt: string
  updatedAt: string
}

/**
 * Authentication context
 */
export interface AuthContext {
  user: User | null
  organization: Organization | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (apiKey: string) => Promise<void>
  logout: () => void
}

/**
 * API error response
 */
export interface ApiErrorResponse {
  message: string
  status: number
  timestamp: string
  path?: string
  errors?: Array<{
    field: string
    message: string
  }>
}

/**
 * Paginated response
 */
export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

/**
 * Authorization request
 */
export interface AuthorizationRequest {
  subject: string
  resource: string
  action: string
  context?: Record<string, unknown>
}

/**
 * Authorization response
 */
export interface AuthorizationResponse {
  decision: 'ALLOW' | 'DENY'
  reason?: string
  evaluatedPolicies: string[]
  metadata?: Record<string, unknown>
}

/**
 * Policy entity
 */
export interface Policy {
  id: string
  organizationId: string
  name: string
  displayName: string
  description?: string
  regoCode: string
  version: number
  status: PolicyStatus
  createdAt: string
  updatedAt: string
  publishedAt?: string
  publishedBy?: string
}

/**
 * Policy status enum
 */
export enum PolicyStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  ARCHIVED = 'ARCHIVED',
}

/**
 * Policy version entity
 */
export interface PolicyVersion {
  id: string
  policyId: string
  version: number
  regoCode: string
  publishedAt: string
  publishedBy: string
  comment?: string
}

/**
 * Audit log entity
 */
export interface AuditLog {
  id: string
  organizationId: string
  timestamp: string
  userId: string
  userName?: string
  action: string
  resource: string
  resourceId?: string
  decision?: 'ALLOW' | 'DENY'
  metadata?: Record<string, unknown>
  ipAddress?: string
  userAgent?: string
}
