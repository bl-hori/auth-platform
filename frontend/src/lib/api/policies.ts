/**
 * Policy API Client
 *
 * @description API functions for policy management
 */

import { apiClient } from '@/lib/api-client'
import type { Policy, PolicyVersion, PagedResponse } from '@/types'
import { PolicyStatus } from '@/types'

/**
 * Check if we should use mock data
 * Set NEXT_PUBLIC_USE_MOCK_API=false to use real backend API
 */
const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK_API !== 'false'

/**
 * Policy creation request
 */
export interface CreatePolicyRequest {
  name: string
  displayName: string
  description?: string
  regoCode: string
  organizationId: string
}

/**
 * Policy update request
 */
export interface UpdatePolicyRequest {
  displayName?: string
  description?: string
  regoCode?: string
}

/**
 * Policy list query parameters
 */
export interface PolicyListParams {
  page?: number
  size?: number
  search?: string
  status?: PolicyStatus
}

/**
 * Policy test request
 */
export interface PolicyTestRequest {
  input: Record<string, unknown>
}

/**
 * Policy test response
 */
export interface PolicyTestResponse {
  result: boolean
  output?: Record<string, unknown>
  errors?: string[]
  duration?: number
}

/**
 * Get paginated list of policies
 *
 * @param params - Query parameters
 * @returns Paginated policy list
 */
export async function getPolicies(
  params?: PolicyListParams
): Promise<PagedResponse<Policy>> {
  if (USE_MOCK) {
    return getMockPolicies(params)
  }

  return apiClient.get<PagedResponse<Policy>>('/v1/policies', {
    params: params as unknown as Record<string, string | number | boolean>,
  })
}

/**
 * Get policy by ID
 *
 * @param id - Policy ID
 * @returns Policy details
 */
export async function getPolicy(id: string): Promise<Policy> {
  // In development, return mock data
  if (USE_MOCK) {
    return getMockPolicy(id)
  }

  return apiClient.get<Policy>(`/v1/policies/${id}`)
}

/**
 * Create new policy
 *
 * @param data - Policy creation data
 * @returns Created policy
 */
export async function createPolicy(data: CreatePolicyRequest): Promise<Policy> {
  // In development, return mock data
  if (USE_MOCK) {
    return createMockPolicy(data)
  }

  return apiClient.post<Policy>('/v1/policies', data)
}

/**
 * Update policy
 *
 * @param id - Policy ID
 * @param data - Update data
 * @returns Updated policy
 */
export async function updatePolicy(
  id: string,
  data: UpdatePolicyRequest
): Promise<Policy> {
  // In development, return mock data
  if (USE_MOCK) {
    return updateMockPolicy(id, data)
  }

  return apiClient.put<Policy>(`/v1/policies/${id}`, data)
}

/**
 * Delete policy
 *
 * @param id - Policy ID
 */
export async function deletePolicy(id: string): Promise<void> {
  // In development, simulate deletion
  if (USE_MOCK) {
    console.log(`Mock: Deleting policy ${id}`)
    return
  }

  return apiClient.delete<void>(`/v1/policies/${id}`)
}

/**
 * Publish policy
 *
 * @param id - Policy ID
 * @returns Published policy
 */
export async function publishPolicy(id: string): Promise<Policy> {
  // In development, return mock data
  if (USE_MOCK) {
    return publishMockPolicy(id)
  }

  return apiClient.post<Policy>(`/v1/policies/${id}/publish`)
}

/**
 * Test policy execution
 *
 * @param id - Policy ID
 * @param data - Test input data
 * @returns Test result
 */
export async function testPolicy(
  id: string,
  data: PolicyTestRequest
): Promise<PolicyTestResponse> {
  // In development, return mock data
  if (USE_MOCK) {
    return testMockPolicy(id, data)
  }

  return apiClient.post<PolicyTestResponse>(`/v1/policies/${id}/test`, data)
}

/**
 * Get policy versions
 *
 * @param policyId - Policy ID
 * @returns List of policy versions
 */
export async function getPolicyVersions(
  policyId: string
): Promise<PolicyVersion[]> {
  // In development, return mock data
  if (USE_MOCK) {
    return getMockPolicyVersions(policyId)
  }

  return apiClient.get<PolicyVersion[]>(`/v1/policies/${policyId}/versions`)
}

// Mock data
const mockPolicies: Policy[] = [
  {
    id: 'policy-001',
    organizationId: 'org-001',
    name: 'user_access_policy',
    displayName: 'ユーザーアクセスポリシー',
    description: 'ユーザーアクセス制御のための基本ポリシー',
    regoCode: `package authz

default allow = false

allow {
  input.method == "GET"
  input.path == ["users"]
  input.user.role == "admin"
}

allow {
  input.method == "GET"
  input.path == ["users", user_id]
  input.user.id == user_id
}`,
    version: 2,
    status: PolicyStatus.PUBLISHED,
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-15T00:00:00Z',
    publishedAt: '2025-01-15T00:00:00Z',
    publishedBy: 'admin@example.com',
  },
  {
    id: 'policy-002',
    organizationId: 'org-001',
    name: 'role_management_policy',
    displayName: 'ロール管理ポリシー',
    description: 'ロール管理操作の認可ポリシー',
    regoCode: `package authz

default allow = false

allow {
  input.method == "POST"
  input.path == ["roles"]
  input.user.role == "admin"
}

allow {
  input.method == "DELETE"
  input.path == ["roles", _]
  input.user.role == "admin"
}`,
    version: 1,
    status: PolicyStatus.PUBLISHED,
    createdAt: '2025-01-05T00:00:00Z',
    updatedAt: '2025-01-05T00:00:00Z',
    publishedAt: '2025-01-05T00:00:00Z',
    publishedBy: 'admin@example.com',
  },
  {
    id: 'policy-003',
    organizationId: 'org-001',
    name: 'draft_policy',
    displayName: '下書きポリシー',
    description: 'テスト中のポリシー',
    regoCode: `package authz

default allow = false

# Work in progress
`,
    version: 1,
    status: PolicyStatus.DRAFT,
    createdAt: '2025-01-20T00:00:00Z',
    updatedAt: '2025-01-20T00:00:00Z',
  },
]

const mockPolicyVersions: PolicyVersion[] = [
  {
    id: 'version-001',
    policyId: 'policy-001',
    version: 1,
    regoCode: `package authz

default allow = false

allow {
  input.method == "GET"
  input.user.role == "admin"
}`,
    publishedAt: '2025-01-01T00:00:00Z',
    publishedBy: 'admin@example.com',
    comment: '初回バージョン',
  },
  {
    id: 'version-002',
    policyId: 'policy-001',
    version: 2,
    regoCode: mockPolicies[0].regoCode,
    publishedAt: '2025-01-15T00:00:00Z',
    publishedBy: 'admin@example.com',
    comment: 'ユーザー自身のデータへのアクセスを許可',
  },
]

function getMockPolicies(params?: PolicyListParams): PagedResponse<Policy> {
  let filtered = [...mockPolicies]

  if (params?.search) {
    const search = params.search.toLowerCase()
    filtered = filtered.filter(
      p =>
        p.name.toLowerCase().includes(search) ||
        p.displayName.toLowerCase().includes(search) ||
        p.description?.toLowerCase().includes(search)
    )
  }

  if (params?.status) {
    filtered = filtered.filter(p => p.status === params.status)
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

function getMockPolicy(id: string): Policy {
  const policy = mockPolicies.find(p => p.id === id)
  if (!policy) {
    throw new Error(`Policy ${id} not found`)
  }
  return policy
}

function createMockPolicy(data: CreatePolicyRequest): Policy {
  const newPolicy: Policy = {
    id: `policy-${Date.now()}`,
    organizationId: data.organizationId,
    name: data.name,
    displayName: data.displayName,
    description: data.description,
    regoCode: data.regoCode,
    version: 1,
    status: PolicyStatus.DRAFT,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }
  mockPolicies.push(newPolicy)
  return newPolicy
}

function updateMockPolicy(id: string, data: UpdatePolicyRequest): Policy {
  const index = mockPolicies.findIndex(p => p.id === id)
  if (index === -1) {
    throw new Error(`Policy ${id} not found`)
  }

  mockPolicies[index] = {
    ...mockPolicies[index],
    ...data,
    updatedAt: new Date().toISOString(),
  }

  return mockPolicies[index]
}

function publishMockPolicy(id: string): Policy {
  const index = mockPolicies.findIndex(p => p.id === id)
  if (index === -1) {
    throw new Error(`Policy ${id} not found`)
  }

  mockPolicies[index] = {
    ...mockPolicies[index],
    status: PolicyStatus.PUBLISHED,
    version: mockPolicies[index].version + 1,
    publishedAt: new Date().toISOString(),
    publishedBy: 'admin@example.com',
    updatedAt: new Date().toISOString(),
  }

  return mockPolicies[index]
}

function testMockPolicy(
  _id: string,
  data: PolicyTestRequest
): PolicyTestResponse {
  // Simulate policy evaluation
  return {
    result: true,
    output: {
      allow: true,
      user: data.input.user,
      resource: data.input.resource,
    },
    duration: 15,
  }
}

function getMockPolicyVersions(policyId: string): PolicyVersion[] {
  return mockPolicyVersions.filter(v => v.policyId === policyId)
}
