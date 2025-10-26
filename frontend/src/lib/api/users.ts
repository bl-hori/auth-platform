/**
 * User API Client
 *
 * @description API functions for user management
 */

import { apiClient } from '@/lib/api-client'
import type { User, PagedResponse, UserStatus } from '@/types'

/**
 * Check if we should use mock data
 * Set NEXT_PUBLIC_USE_MOCK_API=false to use real backend API
 */
const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK_API !== 'false'

/**
 * User creation request
 */
export interface CreateUserRequest {
  email: string
  username: string
  displayName: string
  organizationId: string
  attributes?: Record<string, unknown>
}

/**
 * User update request
 */
export interface UpdateUserRequest {
  email?: string
  displayName?: string
  status?: UserStatus
  attributes?: Record<string, unknown>
}

/**
 * User list query parameters
 */
export interface UserListParams {
  page?: number
  size?: number
  search?: string
  status?: UserStatus
}

/**
 * Get paginated list of users
 *
 * @param params - Query parameters
 * @returns Paginated user list
 */
export async function getUsers(
  params?: UserListParams
): Promise<PagedResponse<User>> {
  // In development, return mock data
  if (USE_MOCK) {
    return getMockUsers(params)
  }

  return apiClient.get<PagedResponse<User>>('/v1/users', {
    params: params as unknown as Record<string, string | number | boolean>,
  })
}

/**
 * Get user by ID
 *
 * @param id - User ID
 * @returns User details
 */
export async function getUser(id: string): Promise<User> {
  // In development, return mock data
  if (USE_MOCK) {
    return getMockUser(id)
  }

  return apiClient.get<User>(`/v1/users/${id}`)
}

/**
 * Create new user
 *
 * @param data - User creation data
 * @returns Created user
 */
export async function createUser(data: CreateUserRequest): Promise<User> {
  // In development, return mock data
  if (USE_MOCK) {
    return createMockUser(data)
  }

  return apiClient.post<User>('/v1/users', data)
}

/**
 * Update user
 *
 * @param id - User ID
 * @param data - Update data
 * @returns Updated user
 */
export async function updateUser(
  id: string,
  data: UpdateUserRequest
): Promise<User> {
  // In development, return mock data
  if (USE_MOCK) {
    return updateMockUser(id, data)
  }

  return apiClient.put<User>(`/v1/users/${id}`, data)
}

/**
 * Delete (deactivate) user
 *
 * @param id - User ID
 */
export async function deleteUser(id: string): Promise<void> {
  // In development, simulate deletion
  if (USE_MOCK) {
    console.log(`Mock: Deleting user ${id}`)
    return
  }

  return apiClient.delete<void>(`/v1/users/${id}`)
}

/**
 * Assign role to user
 *
 * @param userId - User ID
 * @param roleId - Role ID
 */
export async function assignRole(
  userId: string,
  roleId: string
): Promise<void> {
  // In development, simulate role assignment
  if (USE_MOCK) {
    console.log(`Mock: Assigning role ${roleId} to user ${userId}`)
    return
  }

  return apiClient.post<void>(`/v1/users/${userId}/roles`, { roleId })
}

/**
 * Remove role from user
 *
 * @param userId - User ID
 * @param roleId - Role ID
 */
export async function removeRole(
  userId: string,
  roleId: string
): Promise<void> {
  // In development, simulate role removal
  if (USE_MOCK) {
    console.log(`Mock: Removing role ${roleId} from user ${userId}`)
    return
  }

  return apiClient.delete<void>(`/v1/users/${userId}/roles/${roleId}`)
}

// Mock data functions for development
const mockUsers: User[] = [
  {
    id: 'user-001',
    organizationId: 'org-001',
    email: 'admin@example.com',
    username: 'admin',
    displayName: 'Admin User',
    status: 'ACTIVE' as UserStatus,
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
  {
    id: 'user-002',
    organizationId: 'org-001',
    email: 'john.doe@example.com',
    username: 'john.doe',
    displayName: 'John Doe',
    status: 'ACTIVE' as UserStatus,
    createdAt: '2025-01-02T00:00:00Z',
    updatedAt: '2025-01-02T00:00:00Z',
  },
  {
    id: 'user-003',
    organizationId: 'org-001',
    email: 'jane.smith@example.com',
    username: 'jane.smith',
    displayName: 'Jane Smith',
    status: 'INACTIVE' as UserStatus,
    createdAt: '2025-01-03T00:00:00Z',
    updatedAt: '2025-01-03T00:00:00Z',
  },
]

function getMockUsers(params?: UserListParams): PagedResponse<User> {
  let filtered = [...mockUsers]

  if (params?.search) {
    const search = params.search.toLowerCase()
    filtered = filtered.filter(
      u =>
        u.email.toLowerCase().includes(search) ||
        u.username.toLowerCase().includes(search) ||
        u.displayName.toLowerCase().includes(search)
    )
  }

  if (params?.status) {
    filtered = filtered.filter(u => u.status === params.status)
  }

  const page = params?.page ?? 0
  const size = params?.size ?? 10
  const start = page * size
  const end = start + size

  return {
    content: filtered.slice(start, end),
    page,
    size,
    totalElements: filtered.length,
    totalPages: Math.ceil(filtered.length / size),
    last: end >= filtered.length,
  }
}

function getMockUser(id: string): User {
  const user = mockUsers.find(u => u.id === id)
  if (!user) {
    throw new Error(`User ${id} not found`)
  }
  return user
}

function createMockUser(data: CreateUserRequest): User {
  const newUser: User = {
    id: `user-${Date.now()}`,
    organizationId: data.organizationId,
    email: data.email,
    username: data.username,
    displayName: data.displayName,
    status: 'ACTIVE' as UserStatus,
    attributes: data.attributes,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }
  mockUsers.push(newUser)
  return newUser
}

function updateMockUser(id: string, data: UpdateUserRequest): User {
  const index = mockUsers.findIndex(u => u.id === id)
  if (index === -1) {
    throw new Error(`User ${id} not found`)
  }

  mockUsers[index] = {
    ...mockUsers[index],
    ...data,
    updatedAt: new Date().toISOString(),
  }

  return mockUsers[index]
}
