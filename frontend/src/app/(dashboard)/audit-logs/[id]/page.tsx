/**
 * Audit Log Detail Page
 *
 * @description Page for viewing audit log details
 */

'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { ArrowLeft, User, Globe, Clock, FileCode } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { LoadingSpinner } from '@/components/ui/loading-spinner'
import { useToast } from '@/hooks/use-toast'
import { getAuditLog } from '@/lib/api/audit-logs'
import type { AuditLog } from '@/types'

/**
 * Audit log detail page component
 */
export default function AuditLogDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const router = useRouter()
  const { toast } = useToast()

  const [log, setLog] = useState<AuditLog | null>(null)
  const [loading, setLoading] = useState(true)

  /**
   * Load audit log
   */
  useEffect(() => {
    const loadLog = async () => {
      setLoading(true)
      try {
        const { id } = await params
        const logData = await getAuditLog(id)
        setLog(logData)
      } catch (error) {
        console.error('Failed to load audit log:', error)
        toast({
          title: 'エラー',
          description: '監査ログの読み込みに失敗しました',
          variant: 'destructive',
        })
      } finally {
        setLoading(false)
      }
    }

    loadLog()
  }, [params, toast])

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
      timeZoneName: 'short',
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

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (!log) {
    return (
      <div className="space-y-6">
        <Alert variant="destructive">
          <AlertDescription>監査ログが見つかりません</AlertDescription>
        </Alert>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => router.push('/audit-logs')}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-3xl font-bold">監査ログ詳細</h1>
          <p className="text-muted-foreground font-mono text-sm">{log.id}</p>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <User className="h-5 w-5" />
              <CardTitle>ユーザー情報</CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-3">
            <div>
              <p className="text-sm font-medium text-muted-foreground">
                ユーザー名
              </p>
              <p className="text-lg">{log.userName || '-'}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-muted-foreground">
                ユーザーID
              </p>
              <p className="font-mono text-sm">{log.userId}</p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Clock className="h-5 w-5" />
              <CardTitle>タイムスタンプ</CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-lg">{formatTimestamp(log.timestamp)}</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <FileCode className="h-5 w-5" />
              <CardTitle>アクション情報</CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-3">
            <div>
              <p className="text-sm font-medium text-muted-foreground">
                アクション
              </p>
              <Badge variant="outline" className="mt-1">
                {log.action}
              </Badge>
            </div>
            <div>
              <p className="text-sm font-medium text-muted-foreground">
                リソース
              </p>
              <p className="text-lg">{log.resource}</p>
            </div>
            {log.resourceId && (
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  リソースID
                </p>
                <p className="font-mono text-sm">{log.resourceId}</p>
              </div>
            )}
            {log.decision && (
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  判定
                </p>
                <Badge
                  variant={getDecisionBadgeVariant(log.decision)}
                  className="mt-1"
                >
                  {log.decision === 'ALLOW' ? '許可' : '拒否'}
                </Badge>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Globe className="h-5 w-5" />
              <CardTitle>ネットワーク情報</CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-3">
            <div>
              <p className="text-sm font-medium text-muted-foreground">
                IPアドレス
              </p>
              <p className="font-mono text-sm">{log.ipAddress || '-'}</p>
            </div>
            {log.userAgent && (
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  User Agent
                </p>
                <p className="text-sm text-muted-foreground break-all">
                  {log.userAgent}
                </p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {log.metadata && Object.keys(log.metadata).length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>メタデータ</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="rounded-lg bg-muted p-4">
              <pre className="text-sm overflow-x-auto">
                {JSON.stringify(log.metadata, null, 2)}
              </pre>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
