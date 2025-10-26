/**
 * Roles List Page
 *
 * @description Page for managing roles
 */

'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { Plus, Search, Edit, Trash2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { LoadingSpinner } from '@/components/ui/loading-spinner'
import { useToast } from '@/hooks/use-toast'
import { getRoles, deleteRole } from '@/lib/api/roles'
import type { Role } from '@/types'

/**
 * Roles list page component
 */
export default function RolesPage() {
  const router = useRouter()
  const { toast } = useToast()

  const [roles, setRoles] = useState<Role[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [roleToDelete, setRoleToDelete] = useState<Role | null>(null)

  /**
   * Load roles
   */
  useEffect(() => {
    loadRoles()
  }, [])

  /**
   * Load roles from API
   */
  const loadRoles = async () => {
    setLoading(true)
    try {
      const response = await getRoles()
      // Ensure we always have an array from the content field
      setRoles(Array.isArray(response.content) ? response.content : [])
    } catch (error) {
      console.error('Failed to load roles:', error)
      // Set empty array on error
      setRoles([])
      toast({
        title: 'エラー',
        description: 'ロール情報の読み込みに失敗しました',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  /**
   * Filter roles by search query
   */
  const filteredRoles = Array.isArray(roles)
    ? roles.filter(
        role =>
          role.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          role.displayName.toLowerCase().includes(searchQuery.toLowerCase()) ||
          role.description?.toLowerCase().includes(searchQuery.toLowerCase())
      )
    : []

  /**
   * Handle delete role
   */
  const handleDelete = async () => {
    if (!roleToDelete) return

    try {
      await deleteRole(roleToDelete.id)
      // Safely filter roles
      setRoles(Array.isArray(roles) ? roles.filter(r => r.id !== roleToDelete.id) : [])
      toast({
        title: '成功',
        description: 'ロールを削除しました',
        variant: 'success',
      })
    } catch (error) {
      console.error('Failed to delete role:', error)
      toast({
        title: 'エラー',
        description: 'ロールの削除に失敗しました',
        variant: 'destructive',
      })
    } finally {
      setDeleteDialogOpen(false)
      setRoleToDelete(null)
    }
  }

  /**
   * Open delete confirmation dialog
   */
  const confirmDelete = (role: Role) => {
    setRoleToDelete(role)
    setDeleteDialogOpen(true)
  }

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">ロール管理</h1>
          <p className="text-muted-foreground">
            システム内のロールを管理します
          </p>
        </div>
        <Button onClick={() => router.push('/roles/new')}>
          <Plus className="mr-2 h-4 w-4" />
          新規ロール
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>ロール一覧</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="mb-4">
            <div className="relative">
              <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="ロールを検索..."
                className="pl-8"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
              />
            </div>
          </div>

          {filteredRoles.length === 0 ? (
            <div className="flex h-32 items-center justify-center">
              <p className="text-muted-foreground">
                {searchQuery
                  ? '検索条件に一致するロールが見つかりません'
                  : 'ロールがまだ登録されていません'}
              </p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>名前</TableHead>
                  <TableHead>説明</TableHead>
                  <TableHead>親ロール</TableHead>
                  <TableHead>権限数</TableHead>
                  <TableHead className="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredRoles.map(role => (
                  <TableRow key={role.id}>
                    <TableCell>
                      <div>
                        <p className="font-medium">{role.displayName}</p>
                        <p className="text-sm text-muted-foreground">
                          {role.name}
                        </p>
                      </div>
                    </TableCell>
                    <TableCell>
                      <p className="text-sm text-muted-foreground">
                        {role.description || '-'}
                      </p>
                    </TableCell>
                    <TableCell>
                      {role.parentRoleId ? (
                        <Badge variant="secondary">
                          {
                            roles.find(r => r.id === role.parentRoleId)
                              ?.displayName
                          }
                        </Badge>
                      ) : (
                        '-'
                      )}
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline">
                        {role.permissions?.length ?? 0} 個
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => router.push(`/roles/${role.id}`)}
                          title="編集"
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => confirmDelete(role)}
                          title="削除"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>ロールの削除</DialogTitle>
            <DialogDescription>
              本当にこのロールを削除しますか？この操作は取り消せません。
            </DialogDescription>
          </DialogHeader>
          {roleToDelete && (
            <div className="rounded-lg border p-4">
              <p className="font-medium">{roleToDelete.displayName}</p>
              <p className="text-sm text-muted-foreground">
                {roleToDelete.description}
              </p>
            </div>
          )}
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setDeleteDialogOpen(false)}
            >
              キャンセル
            </Button>
            <Button variant="destructive" onClick={handleDelete}>
              削除
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
