/**
 * Permissions Page
 *
 * @description Page for managing permissions
 */

'use client'

import { useState, useEffect } from 'react'
import { Plus, Pencil, Trash2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useToast } from '@/hooks/use-toast'
import {
  getPermissions,
  createPermission,
  updatePermission,
  deletePermission,
  type CreatePermissionRequest,
  type UpdatePermissionRequest,
} from '@/lib/api/roles'
import type { Permission } from '@/types'
import { useAuth } from '@/contexts/auth-context'

/**
 * Permissions page component
 */
export default function PermissionsPage() {
  const { toast } = useToast()
  const { organization } = useAuth()

  const [permissions, setPermissions] = useState<Permission[]>([])
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingPermission, setEditingPermission] = useState<Permission | null>(
    null
  )
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [permissionToDelete, setPermissionToDelete] =
    useState<Permission | null>(null)

  // Form state
  const [formData, setFormData] = useState({
    resourceType: '',
    action: '',
    displayName: '',
    description: '',
    effect: 'allow' as 'allow' | 'deny',
  })

  /**
   * Load permissions
   */
  const loadPermissions = async () => {
    setLoading(true)
    try {
      const response = await getPermissions()
      setPermissions(
        Array.isArray(response.content) ? response.content : []
      )
    } catch (error) {
      console.error('Failed to load permissions:', error)
      setPermissions([])
      toast({
        title: 'エラー',
        description: '権限情報の読み込みに失敗しました',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadPermissions()
  }, [])

  /**
   * Handle create/update permission
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!organization) {
      toast({
        title: 'エラー',
        description: '組織情報が見つかりません',
        variant: 'destructive',
      })
      return
    }

    try {
      if (editingPermission) {
        // Update
        const request: UpdatePermissionRequest = {
          displayName: formData.displayName || undefined,
          description: formData.description || undefined,
          effect: formData.effect,
        }
        await updatePermission(editingPermission.id, request)
        toast({
          title: '成功',
          description: '権限を更新しました',
        })
      } else {
        // Create
        const request: CreatePermissionRequest = {
          organizationId: organization.id,
          name: `${formData.resourceType}:${formData.action}`,
          displayName: formData.displayName || undefined,
          description: formData.description || undefined,
          resourceType: formData.resourceType,
          action: formData.action,
          effect: formData.effect,
        }
        await createPermission(request)
        toast({
          title: '成功',
          description: '権限を作成しました',
        })
      }

      setDialogOpen(false)
      setEditingPermission(null)
      resetForm()
      loadPermissions()
    } catch (error) {
      console.error('Failed to save permission:', error)
      toast({
        title: 'エラー',
        description: `権限の${editingPermission ? '更新' : '作成'}に失敗しました`,
        variant: 'destructive',
      })
    }
  }

  /**
   * Handle delete permission
   */
  const handleDelete = async () => {
    if (!permissionToDelete) return

    try {
      await deletePermission(permissionToDelete.id)
      toast({
        title: '成功',
        description: '権限を削除しました',
      })
      setDeleteDialogOpen(false)
      setPermissionToDelete(null)
      loadPermissions()
    } catch (error) {
      console.error('Failed to delete permission:', error)
      toast({
        title: 'エラー',
        description: '権限の削除に失敗しました',
        variant: 'destructive',
      })
    }
  }

  /**
   * Open edit dialog
   */
  const openEditDialog = (permission: Permission) => {
    setEditingPermission(permission)
    setFormData({
      resourceType: permission.resource,
      action: permission.action,
      displayName: '',
      description: permission.description || '',
      effect: 'allow',
    })
    setDialogOpen(true)
  }

  /**
   * Open create dialog
   */
  const openCreateDialog = () => {
    setEditingPermission(null)
    resetForm()
    setDialogOpen(true)
  }

  /**
   * Reset form
   */
  const resetForm = () => {
    setFormData({
      resourceType: '',
      action: '',
      displayName: '',
      description: '',
      effect: 'allow',
    })
  }

  if (loading) {
    return <div className="p-8">読み込み中...</div>
  }

  return (
    <div className="container mx-auto py-8">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold">権限管理</h1>
          <p className="text-muted-foreground mt-2">
            システムの権限を管理します
          </p>
        </div>
        <Button onClick={openCreateDialog}>
          <Plus className="mr-2 h-4 w-4" />
          新規作成
        </Button>
      </div>

      <div className="border rounded-lg">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>リソース</TableHead>
              <TableHead>アクション</TableHead>
              <TableHead>説明</TableHead>
              <TableHead className="text-right">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {permissions.length === 0 ? (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-muted-foreground">
                  権限がありません
                </TableCell>
              </TableRow>
            ) : (
              permissions.map((permission) => (
                <TableRow key={permission.id}>
                  <TableCell>
                    <Badge variant="outline">{permission.resource}</Badge>
                  </TableCell>
                  <TableCell>
                    <Badge>{permission.action}</Badge>
                  </TableCell>
                  <TableCell>{permission.description || '-'}</TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => openEditDialog(permission)}
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                          setPermissionToDelete(permission)
                          setDeleteDialogOpen(true)
                        }}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Create/Edit Dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <form onSubmit={handleSubmit}>
            <DialogHeader>
              <DialogTitle>
                {editingPermission ? '権限を編集' : '新しい権限を作成'}
              </DialogTitle>
              <DialogDescription>
                {editingPermission
                  ? '権限の情報を更新します（リソースとアクションは変更できません）'
                  : 'リソースとアクションを指定して権限を作成します'}
              </DialogDescription>
            </DialogHeader>

            <div className="grid gap-4 py-4">
              <div className="grid gap-2">
                <Label htmlFor="resourceType">リソースタイプ *</Label>
                <Input
                  id="resourceType"
                  value={formData.resourceType}
                  onChange={(e) =>
                    setFormData({ ...formData, resourceType: e.target.value })
                  }
                  placeholder="document, user, api など"
                  required
                  disabled={!!editingPermission}
                />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="action">アクション *</Label>
                <Input
                  id="action"
                  value={formData.action}
                  onChange={(e) =>
                    setFormData({ ...formData, action: e.target.value })
                  }
                  placeholder="read, write, delete など"
                  required
                  disabled={!!editingPermission}
                />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="displayName">表示名</Label>
                <Input
                  id="displayName"
                  value={formData.displayName}
                  onChange={(e) =>
                    setFormData({ ...formData, displayName: e.target.value })
                  }
                  placeholder="例: ドキュメントを読む"
                />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="description">説明</Label>
                <Textarea
                  id="description"
                  value={formData.description}
                  onChange={(e) =>
                    setFormData({ ...formData, description: e.target.value })
                  }
                  placeholder="権限の説明を入力"
                  rows={3}
                />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="effect">効果</Label>
                <Select
                  value={formData.effect}
                  onValueChange={(value: 'allow' | 'deny') =>
                    setFormData({ ...formData, effect: value })
                  }
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="allow">許可 (Allow)</SelectItem>
                    <SelectItem value="deny">拒否 (Deny)</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                キャンセル
              </Button>
              <Button type="submit">
                {editingPermission ? '更新' : '作成'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>権限を削除</DialogTitle>
            <DialogDescription>
              本当にこの権限を削除しますか？この操作は取り消せません。
            </DialogDescription>
          </DialogHeader>
          {permissionToDelete && (
            <div className="py-4">
              <p className="font-medium">
                {permissionToDelete.resource}:{permissionToDelete.action}
              </p>
              {permissionToDelete.description && (
                <p className="text-sm text-muted-foreground mt-1">
                  {permissionToDelete.description}
                </p>
              )}
            </div>
          )}
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setDeleteDialogOpen(false)}
            >
              キャンセル
            </Button>
            <Button type="button" variant="destructive" onClick={handleDelete}>
              削除
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
