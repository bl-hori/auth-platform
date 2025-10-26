/**
 * Audit Log API Client
 *
 * @description API functions for audit log management
 */

import { apiClient } from '@/lib/api-client'
import type { AuditLog, PagedResponse } from '@/types'

/**
 * Audit log query parameters
 */
export interface AuditLogParams {
  page?: number
  size?: number
  userId?: string
  resource?: string
  action?: string
  decision?: 'ALLOW' | 'DENY'
  startDate?: string
  endDate?: string
}

/**
 * Get paginated list of audit logs
 *
 * @param params - Query parameters
 * @returns Paginated audit log list
 */
export async function getAuditLogs(
  params?: AuditLogParams
): Promise<PagedResponse<AuditLog>> {
  // In development, return mock data
  if (process.env.NODE_ENV === 'development') {
    return getMockAuditLogs(params)
  }

  return apiClient.get<PagedResponse<AuditLog>>('/v1/audit-logs', {
    params: params as unknown as Record<string, string | number | boolean>,
  })
}

/**
 * Get audit log by ID
 *
 * @param id - Audit log ID
 * @returns Audit log details
 */
export async function getAuditLog(id: string): Promise<AuditLog> {
  // In development, return mock data
  if (process.env.NODE_ENV === 'development') {
    return getMockAuditLog(id)
  }

  return apiClient.get<AuditLog>(`/v1/audit-logs/${id}`)
}

/**
 * Export audit logs to CSV
 *
 * @param params - Query parameters
 * @returns CSV content
 */
export async function exportAuditLogsCsv(
  params?: AuditLogParams
): Promise<string> {
  // In development, return mock CSV
  if (process.env.NODE_ENV === 'development') {
    return getMockCsv(params)
  }

  return apiClient.get<string>('/v1/audit-logs/export', {
    params: params as unknown as Record<string, string | number | boolean>,
  })
}

// Mock data
const mockAuditLogs: AuditLog[] = [
  {
    id: 'audit-001',
    organizationId: 'org-001',
    timestamp: '2025-10-26T10:30:00Z',
    userId: 'user-001',
    userName: 'admin@example.com',
    action: 'user:read',
    resource: 'user',
    resourceId: 'user-002',
    decision: 'ALLOW',
    metadata: {
      path: '/v1/users/user-002',
      method: 'GET',
    },
    ipAddress: '192.168.1.100',
    userAgent: 'Mozilla/5.0',
  },
  {
    id: 'audit-002',
    organizationId: 'org-001',
    timestamp: '2025-10-26T10:25:00Z',
    userId: 'user-002',
    userName: 'john.doe@example.com',
    action: 'role:write',
    resource: 'role',
    resourceId: 'role-admin',
    decision: 'DENY',
    metadata: {
      path: '/v1/roles/role-admin',
      method: 'PUT',
      reason: 'Insufficient permissions',
    },
    ipAddress: '192.168.1.101',
    userAgent: 'Mozilla/5.0',
  },
  {
    id: 'audit-003',
    organizationId: 'org-001',
    timestamp: '2025-10-26T10:20:00Z',
    userId: 'user-001',
    userName: 'admin@example.com',
    action: 'policy:write',
    resource: 'policy',
    resourceId: 'policy-001',
    decision: 'ALLOW',
    metadata: {
      path: '/v1/policies/policy-001/publish',
      method: 'POST',
      version: 2,
    },
    ipAddress: '192.168.1.100',
    userAgent: 'Mozilla/5.0',
  },
  {
    id: 'audit-004',
    organizationId: 'org-001',
    timestamp: '2025-10-26T10:15:00Z',
    userId: 'user-003',
    userName: 'jane.smith@example.com',
    action: 'user:write',
    resource: 'user',
    resourceId: 'user-003',
    decision: 'ALLOW',
    metadata: {
      path: '/v1/users/user-003',
      method: 'PUT',
      changes: ['displayName', 'email'],
    },
    ipAddress: '192.168.1.102',
    userAgent: 'Mozilla/5.0',
  },
  {
    id: 'audit-005',
    organizationId: 'org-001',
    timestamp: '2025-10-26T10:10:00Z',
    userId: 'user-001',
    userName: 'admin@example.com',
    action: 'user:write',
    resource: 'user',
    decision: 'ALLOW',
    metadata: {
      path: '/v1/users',
      method: 'POST',
      newUserId: 'user-004',
    },
    ipAddress: '192.168.1.100',
    userAgent: 'Mozilla/5.0',
  },
]

function getMockAuditLogs(
  params?: AuditLogParams
): PagedResponse<AuditLog> {
  let filtered = [...mockAuditLogs]

  // Filter by userId
  if (params?.userId) {
    filtered = filtered.filter(log => log.userId === params.userId)
  }

  // Filter by resource
  if (params?.resource) {
    filtered = filtered.filter(log => log.resource === params.resource)
  }

  // Filter by action
  if (params?.action) {
    filtered = filtered.filter(log => log.action === params.action)
  }

  // Filter by decision
  if (params?.decision) {
    filtered = filtered.filter(log => log.decision === params.decision)
  }

  // Filter by date range
  if (params?.startDate) {
    filtered = filtered.filter(
      log => new Date(log.timestamp) >= new Date(params.startDate!)
    )
  }
  if (params?.endDate) {
    filtered = filtered.filter(
      log => new Date(log.timestamp) <= new Date(params.endDate!)
    )
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

function getMockAuditLog(id: string): AuditLog {
  const log = mockAuditLogs.find(l => l.id === id)
  if (!log) {
    throw new Error(`Audit log ${id} not found`)
  }
  return log
}

function getMockCsv(params?: AuditLogParams): string {
  const logs = getMockAuditLogs(params).content
  const headers = [
    'Timestamp',
    'User',
    'Action',
    'Resource',
    'Resource ID',
    'Decision',
    'IP Address',
  ]

  const rows = logs.map(log => [
    log.timestamp,
    log.userName || log.userId,
    log.action,
    log.resource,
    log.resourceId || '',
    log.decision || '',
    log.ipAddress || '',
  ])

  return [
    headers.join(','),
    ...rows.map(row => row.map(cell => `"${cell}"`).join(',')),
  ].join('\n')
}
