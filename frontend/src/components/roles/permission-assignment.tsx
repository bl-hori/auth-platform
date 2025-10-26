/**
 * Permission Assignment Component
 *
 * @description Component for managing role permission assignments
 */

'use client'

import { useState, useEffect } from 'react'
import { Plus, X } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { useToast } from '@/hooks/use-toast'
import {
  getRole,
  getPermissions,
  assignPermission,
  removePermission,
} from '@/lib/api/roles'
import type { Permission } from '@/types'

interface PermissionAssignmentProps {
  roleId: string
}

/**
 * Permission assignment component
 */
export function PermissionAssignment({ roleId }: PermissionAssignmentProps) {
  const { toast } = useToast()

  const [allPermissions, setAllPermissions] = useState<Permission[]>([])
  const [assignedPermissions, setAssignedPermissions] = useState<Permission[]>(
    []
  )
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)

  /**
   * Load permissions
   */
  useEffect(() => {
    const loadPermissions = async () => {
      setLoading(true)
      try {
        const [permissionsResponse, role] = await Promise.all([
          getPermissions(),
          getRole(roleId),
        ])
        // Ensure we always have an array from the content field
        setAllPermissions(
          Array.isArray(permissionsResponse.content)
            ? permissionsResponse.content
            : []
        )
        setAssignedPermissions(
          Array.isArray(role.permissions) ? role.permissions : []
        )
      } catch (error) {
        console.error('Failed to load permissions:', error)
        setAllPermissions([])
        setAssignedPermissions([])
        toast({
          title: 'エラー',
          description: '権限情報の読み込みに失敗しました',
          variant: 'destructive',
        })
      } finally {
        setLoading(false)
      }
    }

    loadPermissions()
  }, [roleId, toast])

  /**
   * Handle permission assignment
   */
  const handleAssign = async (permissionId: string) => {
    try {
      await assignPermission(roleId, permissionId)
      const permission = allPermissions.find(p => p.id === permissionId)
      if (permission) {
        setAssignedPermissions([...assignedPermissions, permission])
      }
      toast({
        title: '成功',
        description: '権限を割り当てました',
        variant: 'success',
      })
      setDialogOpen(false)
    } catch (error) {
      console.error('Failed to assign permission:', error)
      toast({
        title: 'エラー',
        description: '権限の割り当てに失敗しました',
        variant: 'destructive',
      })
    }
  }

  /**
   * Handle permission removal
   */
  const handleRemove = async (permissionId: string) => {
    try {
      await removePermission(roleId, permissionId)
      setAssignedPermissions(
        assignedPermissions.filter(p => p.id !== permissionId)
      )
      toast({
        title: '成功',
        description: '権限を削除しました',
        variant: 'success',
      })
    } catch (error) {
      console.error('Failed to remove permission:', error)
      toast({
        title: 'エラー',
        description: '権限の削除に失敗しました',
        variant: 'destructive',
      })
    }
  }

  /**
   * Get unassigned permissions
   */
  const getUnassignedPermissions = () => {
    const assignedIds = new Set(assignedPermissions.map(p => p.id))
    return allPermissions.filter(p => !assignedIds.has(p.id))
  }

  /**
   * Group permissions by resource
   */
  const groupByResource = (permissions: Permission[]) => {
    const grouped: Record<string, Permission[]> = {}
    permissions.forEach(p => {
      if (!grouped[p.resource]) {
        grouped[p.resource] = []
      }
      grouped[p.resource].push(p)
    })
    return grouped
  }

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>権限割り当て</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">読み込み中...</p>
        </CardContent>
      </Card>
    )
  }

  const assignedGrouped = groupByResource(assignedPermissions)

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>権限割り当て</CardTitle>
            <CardDescription>
              このロールに割り当てられた権限
            </CardDescription>
          </div>
          <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
            <DialogTrigger asChild>
              <Button size="sm">
                <Plus className="mr-2 h-4 w-4" />
                権限追加
              </Button>
            </DialogTrigger>
            <DialogContent className="max-h-[80vh] overflow-y-auto">
              <DialogHeader>
                <DialogTitle>権限を追加</DialogTitle>
                <DialogDescription>
                  ロールに割り当てる権限を選択してください
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4">
                {getUnassignedPermissions().length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    割り当て可能な権限がありません
                  </p>
                ) : (
                  Object.entries(groupByResource(getUnassignedPermissions())).map(
                    ([resource, permissions]) => (
                      <div key={resource} className="space-y-2">
                        <h3 className="font-semibold capitalize">
                          {resource}
                        </h3>
                        <div className="space-y-2">
                          {permissions.map(permission => (
                            <button
                              key={permission.id}
                              onClick={() => handleAssign(permission.id)}
                              className="flex w-full items-center justify-between rounded-lg border p-3 hover:bg-accent"
                            >
                              <div className="text-left">
                                <p className="font-medium">
                                  {permission.action}
                                </p>
                                {permission.description && (
                                  <p className="text-sm text-muted-foreground">
                                    {permission.description}
                                  </p>
                                )}
                              </div>
                              <Plus className="h-4 w-4 text-muted-foreground" />
                            </button>
                          ))}
                        </div>
                      </div>
                    )
                  )
                )}
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setDialogOpen(false)}>
                  閉じる
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
      </CardHeader>
      <CardContent>
        {assignedPermissions.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            権限が割り当てられていません
          </p>
        ) : (
          <div className="space-y-4">
            {Object.entries(assignedGrouped).map(([resource, permissions]) => (
              <div key={resource} className="space-y-2">
                <h3 className="text-sm font-semibold capitalize text-muted-foreground">
                  {resource}
                </h3>
                <div className="space-y-2">
                  {permissions.map(permission => (
                    <div
                      key={permission.id}
                      className="flex items-center justify-between rounded-lg border p-3"
                    >
                      <div className="flex items-center gap-3">
                        <Badge variant="secondary">{permission.action}</Badge>
                        {permission.description && (
                          <p className="text-sm text-muted-foreground">
                            {permission.description}
                          </p>
                        )}
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleRemove(permission.id)}
                        title="削除"
                      >
                        <X className="h-4 w-4" />
                      </Button>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
