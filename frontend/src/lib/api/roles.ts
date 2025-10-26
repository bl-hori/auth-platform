/**
 * Role API Client
 *
 * @description API functions for role management
 */

import { apiClient } from '@/lib/api-client'
import type { Role, Permission } from '@/types'

/**
 * Role creation request
 */
export interface CreateRoleRequest {
  name: string
  displayName: string
  description?: string
  parentRoleId?: string
  organizationId: string
}

/**
 * Role update request
 */
export interface UpdateRoleRequest {
  displayName?: string
  description?: string
  parentRoleId?: string
}

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
 * Get role by ID
 *
 * @param id - Role ID
 * @returns Role details
 */
export async function getRole(id: string): Promise<Role> {
  // In development, return mock data
  if (process.env.NODE_ENV === 'development') {
    return getMockRole(id)
  }

  return apiClient.get<Role>(`/v1/roles/${id}`)
}

/**
 * Create new role
 *
 * @param data - Role creation data
 * @returns Created role
 */
export async function createRole(data: CreateRoleRequest): Promise<Role> {
  // In development, return mock data
  if (process.env.NODE_ENV === 'development') {
    return createMockRole(data)
  }

  return apiClient.post<Role>('/v1/roles', data)
}

/**
 * Update role
 *
 * @param id - Role ID
 * @param data - Update data
 * @returns Updated role
 */
export async function updateRole(
  id: string,
  data: UpdateRoleRequest
): Promise<Role> {
  // In development, return mock data
  if (process.env.NODE_ENV === 'development') {
    return updateMockRole(id, data)
  }

  return apiClient.put<Role>(`/v1/roles/${id}`, data)
}

/**
 * Delete role
 *
 * @param id - Role ID
 */
export async function deleteRole(id: string): Promise<void> {
  // In development, simulate deletion
  if (process.env.NODE_ENV === 'development') {
    console.log(`Mock: Deleting role ${id}`)
    return
  }

  return apiClient.delete<void>(`/v1/roles/${id}`)
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

/**
 * Get all permissions
 *
 * @returns List of permissions
 */
export async function getPermissions(): Promise<Permission[]> {
  // In development, return mock data
  if (process.env.NODE_ENV === 'development') {
    return getMockPermissions()
  }

  return apiClient.get<Permission[]>('/v1/permissions')
}

/**
 * Assign permission to role
 *
 * @param roleId - Role ID
 * @param permissionId - Permission ID
 */
export async function assignPermission(
  roleId: string,
  permissionId: string
): Promise<void> {
  // In development, simulate assignment
  if (process.env.NODE_ENV === 'development') {
    console.log(`Mock: Assigning permission ${permissionId} to role ${roleId}`)
    return
  }

  return apiClient.post<void>(`/v1/roles/${roleId}/permissions`, {
    permissionId,
  })
}

/**
 * Remove permission from role
 *
 * @param roleId - Role ID
 * @param permissionId - Permission ID
 */
export async function removePermission(
  roleId: string,
  permissionId: string
): Promise<void> {
  // In development, simulate removal
  if (process.env.NODE_ENV === 'development') {
    console.log(
      `Mock: Removing permission ${permissionId} from role ${roleId}`
    )
    return
  }

  return apiClient.delete<void>(
    `/v1/roles/${roleId}/permissions/${permissionId}`
  )
}

// Mock data
const mockPermissions: Permission[] = [
  {
    id: 'perm-user-read',
    resource: 'user',
    action: 'read',
    description: 'ユーザー情報の閲覧',
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
  {
    id: 'perm-user-write',
    resource: 'user',
    action: 'write',
    description: 'ユーザー情報の編集',
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
  {
    id: 'perm-role-read',
    resource: 'role',
    action: 'read',
    description: 'ロール情報の閲覧',
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
  {
    id: 'perm-role-write',
    resource: 'role',
    action: 'write',
    description: 'ロール情報の編集',
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
  {
    id: 'perm-policy-read',
    resource: 'policy',
    action: 'read',
    description: 'ポリシー情報の閲覧',
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
  {
    id: 'perm-policy-write',
    resource: 'policy',
    action: 'write',
    description: 'ポリシー情報の編集',
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
]

const mockRoles: Role[] = [
  {
    id: 'role-admin',
    organizationId: 'org-001',
    name: 'admin',
    displayName: '管理者',
    description: 'システム管理者ロール',
    permissions: [
      mockPermissions[0],
      mockPermissions[1],
      mockPermissions[2],
      mockPermissions[3],
      mockPermissions[4],
      mockPermissions[5],
    ],
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
  {
    id: 'role-editor',
    organizationId: 'org-001',
    name: 'editor',
    displayName: '編集者',
    description: 'コンテンツ編集者ロール',
    parentRoleId: 'role-viewer',
    permissions: [mockPermissions[0], mockPermissions[1]],
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
  {
    id: 'role-viewer',
    organizationId: 'org-001',
    name: 'viewer',
    displayName: '閲覧者',
    description: '閲覧のみのロール',
    permissions: [mockPermissions[0]],
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
]

function getMockRoles(): Role[] {
  return mockRoles
}

function getMockRole(id: string): Role {
  const role = mockRoles.find(r => r.id === id)
  if (!role) {
    throw new Error(`Role ${id} not found`)
  }
  return role
}

function createMockRole(data: CreateRoleRequest): Role {
  const newRole: Role = {
    id: `role-${Date.now()}`,
    organizationId: data.organizationId,
    name: data.name,
    displayName: data.displayName,
    description: data.description,
    parentRoleId: data.parentRoleId,
    permissions: [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }
  mockRoles.push(newRole)
  return newRole
}

function updateMockRole(id: string, data: UpdateRoleRequest): Role {
  const index = mockRoles.findIndex(r => r.id === id)
  if (index === -1) {
    throw new Error(`Role ${id} not found`)
  }

  mockRoles[index] = {
    ...mockRoles[index],
    ...data,
    updatedAt: new Date().toISOString(),
  }

  return mockRoles[index]
}

function getMockUserRoles(_userId: string): Role[] {
  // Return first role as assigned
  return [mockRoles[0]]
}

function getMockPermissions(): Permission[] {
  return mockPermissions
}
