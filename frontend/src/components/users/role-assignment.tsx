/**
 * Role Assignment Component
 *
 * @description Component for managing user role assignments
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
import { getRoles, getUserRoles } from '@/lib/api/roles'
import { assignRole, removeRole } from '@/lib/api/users'
import type { Role } from '@/types'

interface RoleAssignmentProps {
  userId: string
}

/**
 * Role assignment component
 */
export function RoleAssignment({ userId }: RoleAssignmentProps) {
  const { toast } = useToast()

  const [availableRoles, setAvailableRoles] = useState<Role[]>([])
  const [assignedRoles, setAssignedRoles] = useState<Role[]>([])
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)

  /**
   * Load roles
   */
  useEffect(() => {
    const loadRoles = async () => {
      setLoading(true)
      try {
        const [all, assigned] = await Promise.all([
          getRoles(),
          getUserRoles(userId),
        ])
        setAvailableRoles(all)
        setAssignedRoles(assigned)
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
  }, [userId, toast])

  /**
   * Handle role assignment
   */
  const handleAssign = async (roleId: string) => {
    try {
      await assignRole(userId, roleId)
      const role = availableRoles.find(r => r.id === roleId)
      if (role) {
        setAssignedRoles([...assignedRoles, role])
      }
      toast({
        title: '成功',
        description: 'ロールを割り当てました',
        variant: 'success',
      })
      setDialogOpen(false)
    } catch (error) {
      console.error('Failed to assign role:', error)
      toast({
        title: 'エラー',
        description: 'ロールの割り当てに失敗しました',
        variant: 'destructive',
      })
    }
  }

  /**
   * Handle role removal
   */
  const handleRemove = async (roleId: string) => {
    try {
      await removeRole(userId, roleId)
      setAssignedRoles(assignedRoles.filter(r => r.id !== roleId))
      toast({
        title: '成功',
        description: 'ロールを削除しました',
        variant: 'success',
      })
    } catch (error) {
      console.error('Failed to remove role:', error)
      toast({
        title: 'エラー',
        description: 'ロールの削除に失敗しました',
        variant: 'destructive',
      })
    }
  }

  /**
   * Get unassigned roles
   */
  const getUnassignedRoles = () => {
    const assignedIds = new Set(assignedRoles.map(r => r.id))
    return availableRoles.filter(r => !assignedIds.has(r.id))
  }

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>ロール割り当て</CardTitle>
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
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>ロール割り当て</CardTitle>
            <CardDescription>ユーザーに割り当てられたロール</CardDescription>
          </div>
          <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
            <DialogTrigger asChild>
              <Button size="sm">
                <Plus className="mr-2 h-4 w-4" />
                ロール追加
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>ロールを追加</DialogTitle>
                <DialogDescription>
                  ユーザーに割り当てるロールを選択してください
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-2">
                {getUnassignedRoles().length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    割り当て可能なロールがありません
                  </p>
                ) : (
                  getUnassignedRoles().map(role => (
                    <button
                      key={role.id}
                      onClick={() => handleAssign(role.id)}
                      className="flex w-full items-center justify-between rounded-lg border p-3 hover:bg-accent"
                    >
                      <div className="text-left">
                        <p className="font-medium">{role.displayName}</p>
                        {role.description && (
                          <p className="text-sm text-muted-foreground">
                            {role.description}
                          </p>
                        )}
                      </div>
                      <Plus className="h-4 w-4 text-muted-foreground" />
                    </button>
                  ))
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
        {assignedRoles.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            ロールが割り当てられていません
          </p>
        ) : (
          <div className="space-y-2">
            {assignedRoles.map(role => (
              <div
                key={role.id}
                className="flex items-center justify-between rounded-lg border p-3"
              >
                <div className="flex items-center gap-3">
                  <Badge variant="secondary">{role.displayName}</Badge>
                  {role.description && (
                    <p className="text-sm text-muted-foreground">
                      {role.description}
                    </p>
                  )}
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => handleRemove(role.id)}
                  title="削除"
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
