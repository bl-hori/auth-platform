/**
 * Role Hierarchy Component
 *
 * @description Component for visualizing role hierarchy
 */

'use client'

import { useState, useEffect } from 'react'
import { ChevronDown, ChevronRight } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { useToast } from '@/hooks/use-toast'
import { getRoles } from '@/lib/api/roles'
import type { Role } from '@/types'

interface RoleHierarchyProps {
  currentRoleId: string
}

interface RoleNode {
  role: Role
  children: RoleNode[]
}

/**
 * Role hierarchy visualization component
 */
export function RoleHierarchy({ currentRoleId }: RoleHierarchyProps) {
  const { toast } = useToast()

  const [roles, setRoles] = useState<Role[]>([])
  const [hierarchy, setHierarchy] = useState<RoleNode[]>([])
  const [loading, setLoading] = useState(true)
  const [expandedNodes, setExpandedNodes] = useState<Set<string>>(new Set())

  /**
   * Load roles and build hierarchy
   */
  useEffect(() => {
    const loadRoles = async () => {
      setLoading(true)
      try {
        const data = await getRoles()
        setRoles(data)
        setHierarchy(buildHierarchy(data))
        // Auto-expand path to current role
        expandPathToRole(data, currentRoleId)
      } catch (error) {
        console.error('Failed to load roles:', error)
        toast({
          title: 'エラー',
          description: 'ロール情報の読み込みに失敗しました',
          variant: 'destructive',
        })
      } finally {
        setLoading(false)
      }
    }

    loadRoles()
  }, [currentRoleId, toast])

  /**
   * Build role hierarchy tree
   */
  const buildHierarchy = (roles: Role[]): RoleNode[] => {
    const roleMap = new Map<string, RoleNode>()

    // Create nodes for all roles
    roles.forEach(role => {
      roleMap.set(role.id, { role, children: [] })
    })

    // Build parent-child relationships
    const roots: RoleNode[] = []
    roles.forEach(role => {
      const node = roleMap.get(role.id)!
      if (role.parentRoleId) {
        const parent = roleMap.get(role.parentRoleId)
        if (parent) {
          parent.children.push(node)
        } else {
          // Parent not found, treat as root
          roots.push(node)
        }
      } else {
        roots.push(node)
      }
    })

    return roots
  }

  /**
   * Expand path to a specific role
   */
  const expandPathToRole = (roles: Role[], targetRoleId: string) => {
    const expanded = new Set<string>()
    let currentRole = roles.find(r => r.id === targetRoleId)

    while (currentRole?.parentRoleId) {
      expanded.add(currentRole.parentRoleId)
      currentRole = roles.find(r => r.id === currentRole?.parentRoleId)
    }

    setExpandedNodes(expanded)
  }

  /**
   * Toggle node expansion
   */
  const toggleNode = (roleId: string) => {
    const newExpanded = new Set(expandedNodes)
    if (newExpanded.has(roleId)) {
      newExpanded.delete(roleId)
    } else {
      newExpanded.add(roleId)
    }
    setExpandedNodes(newExpanded)
  }

  /**
   * Render role node
   */
  const renderNode = (node: RoleNode, level: number = 0) => {
    const isExpanded = expandedNodes.has(node.role.id)
    const hasChildren = node.children.length > 0
    const isCurrent = node.role.id === currentRoleId

    return (
      <div key={node.role.id} className="space-y-1">
        <div
          className={`flex items-center gap-2 rounded-lg p-2 ${
            isCurrent ? 'bg-primary/10 border-2 border-primary' : 'border'
          }`}
          style={{ marginLeft: `${level * 24}px` }}
        >
          {hasChildren ? (
            <button
              onClick={() => toggleNode(node.role.id)}
              className="flex-shrink-0"
            >
              {isExpanded ? (
                <ChevronDown className="h-4 w-4" />
              ) : (
                <ChevronRight className="h-4 w-4" />
              )}
            </button>
          ) : (
            <div className="w-4" />
          )}
          <div className="flex-1">
            <div className="flex items-center gap-2">
              <span className="font-medium">{node.role.displayName}</span>
              {isCurrent && <Badge variant="default">現在のロール</Badge>}
              <Badge variant="outline">
                {node.role.permissions.length} 権限
              </Badge>
            </div>
            {node.role.description && (
              <p className="text-sm text-muted-foreground">
                {node.role.description}
              </p>
            )}
          </div>
        </div>
        {hasChildren && isExpanded && (
          <div className="space-y-1">
            {node.children.map(child => renderNode(child, level + 1))}
          </div>
        )}
      </div>
    )
  }

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>ロール階層</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">読み込み中...</p>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>ロール階層</CardTitle>
        <CardDescription>
          ロール間の継承関係を可視化します
        </CardDescription>
      </CardHeader>
      <CardContent>
        {hierarchy.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            ロール階層がありません
          </p>
        ) : (
          <div className="space-y-2">
            {hierarchy.map(node => renderNode(node))}
          </div>
        )}
        <div className="mt-4 rounded-lg bg-muted p-4">
          <p className="text-sm text-muted-foreground">
            <strong>注:</strong>{' '}
            子ロールは親ロールの権限を継承します。階層が深い場合、すべての祖先ロールの権限が適用されます。
          </p>
        </div>
      </CardContent>
    </Card>
  )
}
