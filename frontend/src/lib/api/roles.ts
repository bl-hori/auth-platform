/**
 * Role API Client
 *
 * @description API functions for role management
 */

import { apiClient } from '@/lib/api-client'
import type { Role } from '@/types'

/**
 * Get all roles
 *
 * @returns List of roles
 */
export async function getRoles(): Promise<Role[]> {
  // In development, return mock data
  if (process.env.NODE_ENV === 'development') {
    return getMockRoles()
  }

  return apiClient.get<Role[]>('/v1/roles')
}

/**
 * Get user's assigned roles
 *
 * @param userId - User ID
 * @returns List of assigned roles
 */
export async function getUserRoles(userId: string): Promise<Role[]> {
  // In development, return mock data
  if (process.env.NODE_ENV === 'development') {
    return getMockUserRoles(userId)
  }

  return apiClient.get<Role[]>(`/v1/users/${userId}/roles`)
}

// Mock data
const mockRoles: Role[] = [
  {
    id: 'role-admin',
    organizationId: 'org-001',
    name: 'admin',
    displayName: '管理者',
    description: 'システム管理者ロール',
    permissions: [],
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
  {
    id: 'role-editor',
    organizationId: 'org-001',
    name: 'editor',
    displayName: '編集者',
    description: 'コンテンツ編集者ロール',
    permissions: [],
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
  {
    id: 'role-viewer',
    organizationId: 'org-001',
    name: 'viewer',
    displayName: '閲覧者',
    description: '閲覧のみのロール',
    permissions: [],
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
]

function getMockRoles(): Role[] {
  return mockRoles
}

function getMockUserRoles(_userId: string): Role[] {
  // Return first role as assigned
  return [mockRoles[0]]
}
