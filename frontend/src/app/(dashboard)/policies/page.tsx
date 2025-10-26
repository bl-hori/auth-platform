/**
 * Policies List Page
 *
 * @description Page for managing authorization policies
 */

'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { Plus, Search, Edit, Trash2, Play, FileCode } from 'lucide-react'

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
import {
  getPolicies,
  deletePolicy,
  publishPolicy,
  type PolicyListParams,
} from '@/lib/api/policies'
import { PolicyStatus, type Policy } from '@/types'

/**
 * Policies list page component
 */
export default function PoliciesPage() {
  const router = useRouter()
  const { toast } = useToast()

  const [policies, setPolicies] = useState<Policy[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<PolicyStatus | ''>('')
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [policyToDelete, setPolicyToDelete] = useState<Policy | null>(null)
  const [publishing, setPublishing] = useState<string | null>(null)

  /**
   * Load policies
   */
  useEffect(() => {
    loadPolicies()
  }, [statusFilter])

  /**
   * Load policies from API
   */
  const loadPolicies = async () => {
    setLoading(true)
    try {
      const params: PolicyListParams = {
        status: statusFilter || undefined,
      }
      const response = await getPolicies(params)
      setPolicies(response.content)
    } catch (error) {
      console.error('Failed to load policies:', error)
      toast({
        title: 'エラー',
        description: 'ポリシー情報の読み込みに失敗しました',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  /**
   * Filter policies by search query
   */
  const filteredPolicies = policies.filter(
    policy =>
      policy.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      policy.displayName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      policy.description?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  /**
   * Handle delete policy
   */
  const handleDelete = async () => {
    if (!policyToDelete) return

    try {
      await deletePolicy(policyToDelete.id)
      setPolicies(policies.filter(p => p.id !== policyToDelete.id))
      toast({
        title: '成功',
        description: 'ポリシーを削除しました',
        variant: 'success',
      })
    } catch (error) {
      console.error('Failed to delete policy:', error)
      toast({
        title: 'エラー',
        description: 'ポリシーの削除に失敗しました',
        variant: 'destructive',
      })
    } finally {
      setDeleteDialogOpen(false)
      setPolicyToDelete(null)
    }
  }

  /**
   * Handle publish policy
   */
  const handlePublish = async (policy: Policy) => {
    setPublishing(policy.id)
    try {
      const updated = await publishPolicy(policy.id)
      setPolicies(policies.map(p => (p.id === policy.id ? updated : p)))
      toast({
        title: '成功',
        description: 'ポリシーを公開しました',
        variant: 'success',
      })
    } catch (error) {
      console.error('Failed to publish policy:', error)
      toast({
        title: 'エラー',
        description: 'ポリシーの公開に失敗しました',
        variant: 'destructive',
      })
    } finally {
      setPublishing(null)
    }
  }

  /**
   * Open delete confirmation dialog
   */
  const confirmDelete = (policy: Policy) => {
    setPolicyToDelete(policy)
    setDeleteDialogOpen(true)
  }

  /**
   * Get badge variant for policy status
   */
  const getStatusBadgeVariant = (
    status: PolicyStatus
  ): 'default' | 'secondary' | 'destructive' | 'outline' | 'success' => {
    switch (status) {
      case PolicyStatus.PUBLISHED:
        return 'success'
      case PolicyStatus.DRAFT:
        return 'secondary'
      case PolicyStatus.ARCHIVED:
        return 'outline'
      default:
        return 'default'
    }
  }

  /**
   * Get status display text
   */
  const getStatusText = (status: PolicyStatus): string => {
    switch (status) {
      case PolicyStatus.PUBLISHED:
        return '公開済み'
      case PolicyStatus.DRAFT:
        return '下書き'
      case PolicyStatus.ARCHIVED:
        return 'アーカイブ'
      default:
        return status
    }
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
          <h1 className="text-3xl font-bold">ポリシー管理</h1>
          <p className="text-muted-foreground">
            認可ポリシー(Rego)を管理します
          </p>
        </div>
        <Button onClick={() => router.push('/policies/new')}>
          <Plus className="mr-2 h-4 w-4" />
          新規ポリシー
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>ポリシー一覧</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="mb-4 flex gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="ポリシーを検索..."
                className="pl-8"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
              />
            </div>
            <select
              value={statusFilter}
              onChange={e =>
                setStatusFilter((e.target.value as PolicyStatus) || '')
              }
              className="flex h-10 w-[200px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              <option value="">すべてのステータス</option>
              <option value={PolicyStatus.PUBLISHED}>公開済み</option>
              <option value={PolicyStatus.DRAFT}>下書き</option>
              <option value={PolicyStatus.ARCHIVED}>アーカイブ</option>
            </select>
          </div>

          {filteredPolicies.length === 0 ? (
            <div className="flex h-32 items-center justify-center">
              <p className="text-muted-foreground">
                {searchQuery || statusFilter
                  ? '検索条件に一致するポリシーが見つかりません'
                  : 'ポリシーがまだ登録されていません'}
              </p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>名前</TableHead>
                  <TableHead>説明</TableHead>
                  <TableHead>バージョン</TableHead>
                  <TableHead>ステータス</TableHead>
                  <TableHead className="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredPolicies.map(policy => (
                  <TableRow key={policy.id}>
                    <TableCell>
                      <div>
                        <p className="font-medium">{policy.displayName}</p>
                        <p className="text-sm text-muted-foreground">
                          {policy.name}
                        </p>
                      </div>
                    </TableCell>
                    <TableCell>
                      <p className="text-sm text-muted-foreground">
                        {policy.description || '-'}
                      </p>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline">v{policy.version}</Badge>
                    </TableCell>
                    <TableCell>
                      <Badge variant={getStatusBadgeVariant(policy.status)}>
                        {getStatusText(policy.status)}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() =>
                            router.push(`/policies/${policy.id}/test`)
                          }
                          title="テスト実行"
                        >
                          <Play className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => router.push(`/policies/${policy.id}`)}
                          title="編集"
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        {policy.status === PolicyStatus.DRAFT && (
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => handlePublish(policy)}
                            disabled={publishing === policy.id}
                            title="公開"
                          >
                            <FileCode className="h-4 w-4" />
                          </Button>
                        )}
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => confirmDelete(policy)}
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
            <DialogTitle>ポリシーの削除</DialogTitle>
            <DialogDescription>
              本当にこのポリシーを削除しますか？この操作は取り消せません。
            </DialogDescription>
          </DialogHeader>
          {policyToDelete && (
            <div className="rounded-lg border p-4">
              <p className="font-medium">{policyToDelete.displayName}</p>
              <p className="text-sm text-muted-foreground">
                {policyToDelete.description}
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
