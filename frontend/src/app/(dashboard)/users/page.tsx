'use client'

/**
 * Users List Page
 *
 * @description User management page with search, filters, and CRUD operations
 */

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { Search, Plus, Edit, Trash2, UserCheck, UserX } from 'lucide-react'

import { ProtectedRoute } from '@/components/auth/protected-route'
import { DashboardLayout } from '@/components/layout/dashboard-layout'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
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
import { useToast } from '@/hooks/use-toast'
import { getUsers, deleteUser, updateUser } from '@/lib/api/users'
import type { User } from '@/types'
import { UserStatus } from '@/types'
import { formatDateTime } from '@/lib/utils'

/**
 * Users list page component
 */
export default function UsersPage() {
  const router = useRouter()
  const { toast } = useToast()

  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<
    (typeof UserStatus)[keyof typeof UserStatus] | ''
  >('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [userToDelete, setUserToDelete] = useState<User | null>(null)

  /**
   * Load users from API
   */
  const loadUsers = async () => {
    setLoading(true)
    try {
      const response = await getUsers({
        page,
        size: 10,
        search: search || undefined,
        status: statusFilter || undefined,
      })
      setUsers(response.content || [])
      setTotalPages(response.totalPages || 0)
    } catch (error) {
      console.error('Failed to load users:', error)
      setUsers([]) // エラー時も空配列をセット
      setTotalPages(0)
      toast({
        title: 'エラー',
        description: 'ユーザーの読み込みに失敗しました',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  /**
   * Load users on mount and when filters change
   */
  useEffect(() => {
    loadUsers()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, search, statusFilter])

  /**
   * Handle user deletion
   */
  const handleDelete = async () => {
    if (!userToDelete) return

    try {
      await deleteUser(userToDelete.id)
      toast({
        title: '成功',
        description: 'ユーザーを削除しました',
        variant: 'success',
      })
      setDeleteDialogOpen(false)
      setUserToDelete(null)
      loadUsers()
    } catch (error) {
      console.error('Failed to delete user:', error)
      toast({
        title: 'エラー',
        description: 'ユーザーの削除に失敗しました',
        variant: 'destructive',
      })
    }
  }

  /**
   * Toggle user status
   */
  const handleToggleStatus = async (user: User) => {
    const newStatus =
      user.status === UserStatus.ACTIVE
        ? UserStatus.INACTIVE
        : UserStatus.ACTIVE

    try {
      await updateUser(user.id, { status: newStatus })
      toast({
        title: '成功',
        description: 'ユーザーステータスを更新しました',
        variant: 'success',
      })
      loadUsers()
    } catch (error) {
      console.error('Failed to update user status:', error)
      toast({
        title: 'エラー',
        description: 'ステータスの更新に失敗しました',
        variant: 'destructive',
      })
    }
  }

  /**
   * Get status badge variant
   */
  const getStatusBadge = (
    status: (typeof UserStatus)[keyof typeof UserStatus]
  ) => {
    switch (status) {
      case UserStatus.ACTIVE:
        return <Badge variant="success">有効</Badge>
      case UserStatus.INACTIVE:
        return <Badge variant="warning">無効</Badge>
      case UserStatus.SUSPENDED:
        return <Badge variant="destructive">停止中</Badge>
      default:
        return <Badge>{status}</Badge>
    }
  }

  return (
    <ProtectedRoute>
      <DashboardLayout>
        <div className="space-y-6">
          {/* Header */}
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold tracking-tight">
                ユーザー管理
              </h1>
              <p className="text-muted-foreground">
                組織内のユーザーを管理します
              </p>
            </div>
            <Button onClick={() => router.push('/users/new')}>
              <Plus className="mr-2 h-4 w-4" />
              新規ユーザー
            </Button>
          </div>

          {/* Filters */}
          <div className="flex gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="メール、ユーザー名、表示名で検索..."
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="pl-9"
              />
            </div>
            <select
              value={statusFilter}
              onChange={e =>
                setStatusFilter(
                  e.target.value as
                    | (typeof UserStatus)[keyof typeof UserStatus]
                    | ''
                )
              }
              className="rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">全てのステータス</option>
              <option value={UserStatus.ACTIVE}>有効</option>
              <option value={UserStatus.INACTIVE}>無効</option>
              <option value={UserStatus.SUSPENDED}>停止中</option>
            </select>
          </div>

          {/* Table */}
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>表示名</TableHead>
                  <TableHead>ユーザー名</TableHead>
                  <TableHead>メールアドレス</TableHead>
                  <TableHead>ステータス</TableHead>
                  <TableHead>作成日時</TableHead>
                  <TableHead className="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center">
                      読み込み中...
                    </TableCell>
                  </TableRow>
                ) : !users || users.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center">
                      ユーザーが見つかりません
                    </TableCell>
                  </TableRow>
                ) : (
                  users.map(user => (
                    <TableRow key={user.id}>
                      <TableCell className="font-medium">
                        {user.displayName}
                      </TableCell>
                      <TableCell>{user.username}</TableCell>
                      <TableCell>{user.email}</TableCell>
                      <TableCell>{getStatusBadge(user.status)}</TableCell>
                      <TableCell>{formatDateTime(user.createdAt)}</TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => router.push(`/users/${user.id}`)}
                            title="編集"
                          >
                            <Edit className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => handleToggleStatus(user)}
                            title={
                              user.status === UserStatus.ACTIVE
                                ? '無効化'
                                : '有効化'
                            }
                          >
                            {user.status === UserStatus.ACTIVE ? (
                              <UserX className="h-4 w-4" />
                            ) : (
                              <UserCheck className="h-4 w-4" />
                            )}
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => {
                              setUserToDelete(user)
                              setDeleteDialogOpen(true)
                            }}
                            title="削除"
                          >
                            <Trash2 className="h-4 w-4 text-destructive" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between">
              <p className="text-sm text-muted-foreground">
                ページ {page + 1} / {totalPages}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(page - 1)}
                  disabled={page === 0}
                >
                  前へ
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(page + 1)}
                  disabled={page >= totalPages - 1}
                >
                  次へ
                </Button>
              </div>
            </div>
          )}
        </div>

        {/* Delete Confirmation Dialog */}
        <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>ユーザーの削除</DialogTitle>
              <DialogDescription>
                本当に {userToDelete?.displayName} を削除しますか？
                <br />
                この操作は取り消せません。
              </DialogDescription>
            </DialogHeader>
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
      </DashboardLayout>
    </ProtectedRoute>
  )
}
