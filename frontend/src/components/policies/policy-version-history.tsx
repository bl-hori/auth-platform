/**
 * Policy Version History Component
 *
 * @description Component for viewing policy version history
 */

'use client'

import { useState, useEffect } from 'react'
import { History, Eye } from 'lucide-react'

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
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { useToast } from '@/hooks/use-toast'
import { PolicyEditor } from '@/components/policies/policy-editor'
import { getPolicyVersions } from '@/lib/api/policies'
import type { PolicyVersion } from '@/types'

interface PolicyVersionHistoryProps {
  policyId: string
}

/**
 * Policy version history component
 */
export function PolicyVersionHistory({
  policyId,
}: PolicyVersionHistoryProps) {
  const { toast } = useToast()

  const [versions, setVersions] = useState<PolicyVersion[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedVersion, setSelectedVersion] = useState<PolicyVersion | null>(
    null
  )
  const [dialogOpen, setDialogOpen] = useState(false)

  /**
   * Load policy versions
   */
  useEffect(() => {
    const loadVersions = async () => {
      setLoading(true)
      try {
        const data = await getPolicyVersions(policyId)
        setVersions(data)
      } catch (error) {
        console.error('Failed to load policy versions:', error)
        toast({
          title: 'エラー',
          description: 'バージョン履歴の読み込みに失敗しました',
          variant: 'destructive',
        })
      } finally {
        setLoading(false)
      }
    }

    loadVersions()
  }, [policyId, toast])

  /**
   * Handle version view
   */
  const handleViewVersion = (version: PolicyVersion) => {
    setSelectedVersion(version)
    setDialogOpen(true)
  }

  /**
   * Format date
   */
  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return new Intl.DateTimeFormat('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date)
  }

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>バージョン履歴</CardTitle>
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
        <div className="flex items-center gap-2">
          <History className="h-5 w-5" />
          <div>
            <CardTitle>バージョン履歴</CardTitle>
            <CardDescription>
              このポリシーの過去のバージョンを表示します
            </CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {versions.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            バージョン履歴がありません
          </p>
        ) : (
          <div className="space-y-4">
            {versions.map((version, index) => (
              <div
                key={version.id}
                className="flex items-center justify-between rounded-lg border p-4"
              >
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <Badge variant={index === 0 ? 'default' : 'outline'}>
                      バージョン {version.version}
                    </Badge>
                    {index === 0 && (
                      <Badge variant="success">最新</Badge>
                    )}
                  </div>
                  <p className="text-sm text-muted-foreground">
                    公開日時: {formatDate(version.publishedAt)}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    公開者: {version.publishedBy}
                  </p>
                  {version.comment && (
                    <p className="text-sm mt-2">
                      <strong>コメント:</strong> {version.comment}
                    </p>
                  )}
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => handleViewVersion(version)}
                  title="コードを表示"
                >
                  <Eye className="h-4 w-4" />
                </Button>
              </div>
            ))}
          </div>
        )}

        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogContent className="max-w-4xl max-h-[80vh]">
            <DialogHeader>
              <DialogTitle>
                バージョン {selectedVersion?.version} - コード表示
              </DialogTitle>
              <DialogDescription>
                公開日時: {selectedVersion && formatDate(selectedVersion.publishedAt)}
                {' | '}
                公開者: {selectedVersion?.publishedBy}
              </DialogDescription>
            </DialogHeader>
            {selectedVersion && (
              <div className="space-y-4">
                {selectedVersion.comment && (
                  <div className="rounded-lg bg-muted p-3">
                    <p className="text-sm">
                      <strong>コメント:</strong> {selectedVersion.comment}
                    </p>
                  </div>
                )}
                <PolicyEditor
                  value={selectedVersion.regoCode}
                  onChange={() => {}}
                  readOnly={true}
                  height="500px"
                />
              </div>
            )}
          </DialogContent>
        </Dialog>
      </CardContent>
    </Card>
  )
}
