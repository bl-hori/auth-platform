/**
 * Audit Logs Page
 *
 * @description Page for viewing and filtering audit logs
 */

'use client'

import { useState, useEffect, useCallback } from 'react'
import { Search, Download, RefreshCw, Eye } from 'lucide-react'
import { useRouter } from 'next/navigation'

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
import { LoadingSpinner } from '@/components/ui/loading-spinner'
import { DateRangePicker } from '@/components/ui/date-range-picker'
import { useToast } from '@/hooks/use-toast'
import {
  getAuditLogs,
  exportAuditLogsCsv,
  type AuditLogParams,
} from '@/lib/api/audit-logs'
import type { AuditLog } from '@/types'

// ポーリング間隔 (30秒)
const POLLING_INTERVAL = 30000

/**
 * Audit logs page component
 */
export default function AuditLogsPage() {
  const router = useRouter()
  const { toast } = useToast()

  const [logs, setLogs] = useState<AuditLog[]>([])
  const [loading, setLoading] = useState(true)
  const [exporting, setExporting] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [filters, setFilters] = useState({
    userId: '',
    resource: '',
    action: '',
    decision: '' as '' | 'ALLOW' | 'DENY',
    startDate: '',
    endDate: '',
  })
  const [totalElements, setTotalElements] = useState(0)
  const [page, setPage] = useState(0)
  const [autoRefresh, setAutoRefresh] = useState(false)

  /**
   * Load audit logs
   */
  const loadLogs = useCallback(async () => {
    setLoading(true)
    try {
      const params: AuditLogParams = {
        page,
        size: 20,
        userId: filters.userId ? filters.userId : undefined,
        resource: filters.resource ? filters.resource : undefined,
        action: filters.action ? filters.action : undefined,
        decision: filters.decision ? filters.decision : undefined,
        startDate: filters.startDate ? filters.startDate : undefined,
        endDate: filters.endDate ? filters.endDate : undefined,
      }

      const response = await getAuditLogs(params)
      setLogs(response.content)
      setTotalElements(response.totalElements)
    } catch (error) {
      console.error('Failed to load audit logs:', error)
      toast({
        title: 'エラー',
        description: '監査ログの読み込みに失敗しました',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }, [page, filters, toast])

  /**
   * Load logs on mount and when dependencies change
   */
  useEffect(() => {
    loadLogs()
  }, [loadLogs])

  /**
   * Auto-refresh polling
   */
  useEffect(() => {
    if (!autoRefresh) return

    const intervalId = setInterval(() => {
      loadLogs()
    }, POLLING_INTERVAL)

    return () => clearInterval(intervalId)
  }, [autoRefresh, loadLogs])

  /**
   * Filter logs by search query
   */
  const filteredLogs = logs.filter(
    log =>
      log.userName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      log.action.toLowerCase().includes(searchQuery.toLowerCase()) ||
      log.resource.toLowerCase().includes(searchQuery.toLowerCase()) ||
      log.resourceId?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  /**
   * Handle export to CSV
   */
  const handleExport = async () => {
    setExporting(true)
    try {
      const params: AuditLogParams = {
        userId: filters.userId ? filters.userId : undefined,
        resource: filters.resource ? filters.resource : undefined,
        action: filters.action ? filters.action : undefined,
        decision: filters.decision ? filters.decision : undefined,
        startDate: filters.startDate ? filters.startDate : undefined,
        endDate: filters.endDate ? filters.endDate : undefined,
      }

      const csv = await exportAuditLogsCsv(params)

      // Download CSV file
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      const url = URL.createObjectURL(blob)
      link.setAttribute('href', url)
      link.setAttribute(
        'download',
        `audit-logs-${new Date().toISOString()}.csv`
      )
      link.style.visibility = 'hidden'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)

      toast({
        title: '成功',
        description: '監査ログをエクスポートしました',
        variant: 'success',
      })
    } catch (error) {
      console.error('Failed to export audit logs:', error)
      toast({
        title: 'エラー',
        description: '監査ログのエクスポートに失敗しました',
        variant: 'destructive',
      })
    } finally {
      setExporting(false)
    }
  }

  /**
   * Handle filter change
   */
  const handleFilterChange = (field: string, value: string) => {
    setFilters(prev => ({ ...prev, [field]: value }))
    setPage(0) // Reset to first page
  }

  /**
   * Clear all filters
   */
  const clearFilters = () => {
    setFilters({
      userId: '',
      resource: '',
      action: '',
      decision: '',
      startDate: '',
      endDate: '',
    })
    setSearchQuery('')
    setPage(0)
  }

  /**
   * Format timestamp
   */
  const formatTimestamp = (timestamp: string) => {
    const date = new Date(timestamp)
    return new Intl.DateTimeFormat('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }).format(date)
  }

  /**
   * Get decision badge variant
   */
  const getDecisionBadgeVariant = (
    decision?: 'ALLOW' | 'DENY'
  ): 'default' | 'secondary' | 'destructive' | 'outline' | 'success' => {
    if (!decision) return 'outline'
    return decision === 'ALLOW' ? 'success' : 'destructive'
  }

  const hasFilters =
    filters.userId ||
    filters.resource ||
    filters.action ||
    filters.decision ||
    filters.startDate ||
    filters.endDate

  if (loading && logs.length === 0) {
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
          <h1 className="text-3xl font-bold">監査ログ</h1>
          <p className="text-muted-foreground">
            システム内のアクティビティを監視します
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={() => setAutoRefresh(!autoRefresh)}
          >
            <RefreshCw
              className={`mr-2 h-4 w-4 ${autoRefresh ? 'animate-spin' : ''}`}
            />
            {autoRefresh ? '自動更新中' : '自動更新'}
          </Button>
          <Button
            variant="outline"
            onClick={handleExport}
            disabled={exporting}
          >
            <Download className="mr-2 h-4 w-4" />
            {exporting ? 'エクスポート中...' : 'CSV出力'}
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>フィルター</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">ユーザー</label>
              <Input
                placeholder="ユーザーID"
                value={filters.userId}
                onChange={e => handleFilterChange('userId', e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">リソース</label>
              <select
                value={filters.resource}
                onChange={e => handleFilterChange('resource', e.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              >
                <option value="">すべて</option>
                <option value="user">ユーザー</option>
                <option value="role">ロール</option>
                <option value="policy">ポリシー</option>
                <option value="permission">権限</option>
              </select>
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">アクション</label>
              <Input
                placeholder="例: user:read"
                value={filters.action}
                onChange={e => handleFilterChange('action', e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">判定</label>
              <select
                value={filters.decision}
                onChange={e =>
                  handleFilterChange('decision', e.target.value)
                }
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              >
                <option value="">すべて</option>
                <option value="ALLOW">許可</option>
                <option value="DENY">拒否</option>
              </select>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <DateRangePicker
              startDate={filters.startDate}
              endDate={filters.endDate}
              onStartDateChange={date => handleFilterChange('startDate', date)}
              onEndDateChange={date => handleFilterChange('endDate', date)}
            />
            {hasFilters && (
              <Button variant="outline" onClick={clearFilters}>
                フィルタークリア
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>監査ログ一覧</CardTitle>
            <div className="relative w-[300px]">
              <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="検索..."
                className="pl-8"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
              />
            </div>
          </div>
          <p className="text-sm text-muted-foreground">
            {totalElements} 件の記録
          </p>
        </CardHeader>
        <CardContent>
          {filteredLogs.length === 0 ? (
            <div className="flex h-32 items-center justify-center">
              <p className="text-muted-foreground">
                {searchQuery || hasFilters
                  ? '検索条件に一致するログが見つかりません'
                  : '監査ログがありません'}
              </p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>日時</TableHead>
                  <TableHead>ユーザー</TableHead>
                  <TableHead>アクション</TableHead>
                  <TableHead>リソース</TableHead>
                  <TableHead>判定</TableHead>
                  <TableHead>IPアドレス</TableHead>
                  <TableHead className="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredLogs.map(log => (
                  <TableRow key={log.id}>
                    <TableCell className="font-mono text-xs">
                      {formatTimestamp(log.timestamp)}
                    </TableCell>
                    <TableCell>
                      <div>
                        <p className="font-medium">
                          {log.userName || log.userId}
                        </p>
                        {log.userName && (
                          <p className="text-xs text-muted-foreground">
                            {log.userId}
                          </p>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline">{log.action}</Badge>
                    </TableCell>
                    <TableCell>
                      <div>
                        <p className="font-medium">{log.resource}</p>
                        {log.resourceId && (
                          <p className="text-xs text-muted-foreground">
                            {log.resourceId}
                          </p>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      {log.decision && (
                        <Badge variant={getDecisionBadgeVariant(log.decision)}>
                          {log.decision === 'ALLOW' ? '許可' : '拒否'}
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell className="font-mono text-xs">
                      {log.ipAddress || '-'}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => router.push(`/audit-logs/${log.id}`)}
                        title="詳細表示"
                      >
                        <Eye className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}

          {totalElements > 20 && (
            <div className="mt-4 flex items-center justify-between">
              <p className="text-sm text-muted-foreground">
                {page * 20 + 1} - {Math.min((page + 1) * 20, totalElements)} /{' '}
                {totalElements}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                >
                  前へ
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(p => p + 1)}
                  disabled={(page + 1) * 20 >= totalElements}
                >
                  次へ
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
