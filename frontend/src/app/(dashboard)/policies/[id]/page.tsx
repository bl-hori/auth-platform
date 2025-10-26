/**
 * Policy Edit Page
 *
 * @description Page for editing policy details with validation feedback
 */

'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { ArrowLeft, CheckCircle, XCircle, AlertCircle } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { LoadingSpinner } from '@/components/ui/loading-spinner'
import { useToast } from '@/hooks/use-toast'
import { PolicyEditor } from '@/components/policies/policy-editor'
import { PolicyVersionHistory } from '@/components/policies/policy-version-history'
import {
  getPolicy,
  updatePolicy,
  publishPolicy,
  type UpdatePolicyRequest,
} from '@/lib/api/policies'
import { PolicyStatus, type Policy } from '@/types'

/**
 * Policy edit page component
 */
export default function PolicyEditPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const router = useRouter()
  const { toast } = useToast()

  const [policy, setPolicy] = useState<Policy | null>(null)
  const [loading, setLoading] = useState(true)
  const [formData, setFormData] = useState({
    displayName: '',
    description: '',
    regoCode: '',
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [validationErrors, setValidationErrors] = useState<string[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [publishing, setPublishing] = useState(false)

  /**
   * Load policy data
   */
  useEffect(() => {
    const loadData = async () => {
      setLoading(true)
      try {
        const { id } = await params
        const policyData = await getPolicy(id)

        setPolicy(policyData)
        setFormData({
          displayName: policyData.displayName,
          description: policyData.description || '',
          regoCode: policyData.regoCode,
        })
      } catch (error) {
        console.error('Failed to load policy:', error)
        toast({
          title: 'エラー',
          description: 'ポリシー情報の読み込みに失敗しました',
          variant: 'destructive',
        })
      } finally {
        setLoading(false)
      }
    }

    loadData()
  }, [params, toast])

  /**
   * Handle form field changes
   */
  const handleChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }))
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }))
    }

    // Validate Rego code on change
    if (field === 'regoCode') {
      validateRegoCode(value)
    }
  }

  /**
   * Validate Rego code syntax
   */
  const validateRegoCode = (code: string) => {
    const errors: string[] = []

    // Basic validation checks
    if (!code.includes('package ')) {
      errors.push('ポリシーには package 宣言が必要です')
    }

    // Check for forbidden imports (security check)
    const forbiddenImports = ['http', 'net', 'file', 'exec']
    forbiddenImports.forEach(forbidden => {
      if (code.includes(`import ${forbidden}`)) {
        errors.push(`セキュリティ上の理由により、'${forbidden}' モジュールのインポートは禁止されています`)
      }
    })

    // Check for balanced braces
    const openBraces = (code.match(/{/g) || []).length
    const closeBraces = (code.match(/}/g) || []).length
    if (openBraces !== closeBraces) {
      errors.push('中括弧 {} のバランスが取れていません')
    }

    // Check for balanced brackets
    const openBrackets = (code.match(/\[/g) || []).length
    const closeBrackets = (code.match(/\]/g) || []).length
    if (openBrackets !== closeBrackets) {
      errors.push('角括弧 [] のバランスが取れていません')
    }

    setValidationErrors(errors)
  }

  /**
   * Validate form data
   */
  const validate = (): boolean => {
    const newErrors: Record<string, string> = {}

    if (!formData.displayName.trim()) {
      newErrors.displayName = '表示名は必須です'
    }

    if (!formData.regoCode.trim()) {
      newErrors.regoCode = 'Regoコードは必須です'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0 && validationErrors.length === 0
  }

  /**
   * Handle form submission
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validate()) return
    if (!policy) return

    setSubmitting(true)
    try {
      const request: UpdatePolicyRequest = {
        displayName: formData.displayName,
        description: formData.description || undefined,
        regoCode: formData.regoCode,
      }

      const updatedPolicy = await updatePolicy(policy.id, request)
      setPolicy(updatedPolicy)

      toast({
        title: '成功',
        description: 'ポリシーを更新しました',
        variant: 'success',
      })
    } catch (error) {
      console.error('Failed to update policy:', error)
      toast({
        title: 'エラー',
        description: 'ポリシーの更新に失敗しました',
        variant: 'destructive',
      })
    } finally {
      setSubmitting(false)
    }
  }

  /**
   * Handle policy publish
   */
  const handlePublish = async () => {
    if (!policy) return
    if (!validate()) {
      toast({
        title: 'エラー',
        description: 'バリデーションエラーを修正してから公開してください',
        variant: 'destructive',
      })
      return
    }

    setPublishing(true)
    try {
      const publishedPolicy = await publishPolicy(policy.id)
      setPolicy(publishedPolicy)

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
      setPublishing(false)
    }
  }

  /**
   * Get status badge variant
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

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (!policy) {
    return (
      <div className="space-y-6">
        <Alert variant="destructive">
          <AlertDescription>ポリシーが見つかりません</AlertDescription>
        </Alert>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => router.push('/policies')}
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-3xl font-bold">ポリシー編集</h1>
            <p className="text-muted-foreground">{policy.name}</p>
          </div>
          <Badge variant={getStatusBadgeVariant(policy.status)}>
            {policy.status === PolicyStatus.PUBLISHED ? '公開済み' : '下書き'}
          </Badge>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={() => router.push(`/policies/${policy.id}/test`)}
          >
            テスト実行
          </Button>
          {policy.status === PolicyStatus.DRAFT && (
            <Button onClick={handlePublish} disabled={publishing || validationErrors.length > 0}>
              {publishing ? '公開中...' : '公開'}
            </Button>
          )}
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>基本情報</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">名前</Label>
              <Input id="name" value={policy.name} disabled />
              <p className="text-sm text-muted-foreground">
                名前は変更できません
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="displayName">
                表示名 <span className="text-red-500">*</span>
              </Label>
              <Input
                id="displayName"
                value={formData.displayName}
                onChange={e => handleChange('displayName', e.target.value)}
                disabled={submitting}
              />
              {errors.displayName && (
                <Alert variant="destructive">
                  <AlertDescription>{errors.displayName}</AlertDescription>
                </Alert>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">説明</Label>
              <Input
                id="description"
                value={formData.description}
                onChange={e => handleChange('description', e.target.value)}
                disabled={submitting}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>ポリシーコード (Rego)</CardTitle>
              <div className="flex items-center gap-2">
                {validationErrors.length === 0 ? (
                  <div className="flex items-center gap-1 text-sm text-green-600">
                    <CheckCircle className="h-4 w-4" />
                    検証OK
                  </div>
                ) : (
                  <div className="flex items-center gap-1 text-sm text-destructive">
                    <XCircle className="h-4 w-4" />
                    {validationErrors.length} 個のエラー
                  </div>
                )}
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {validationErrors.length > 0 && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  <ul className="list-disc pl-4 space-y-1">
                    {validationErrors.map((error, index) => (
                      <li key={index}>{error}</li>
                    ))}
                  </ul>
                </AlertDescription>
              </Alert>
            )}
            <PolicyEditor
              value={formData.regoCode}
              onChange={value => handleChange('regoCode', value)}
              readOnly={submitting}
            />
            {errors.regoCode && (
              <Alert variant="destructive">
                <AlertDescription>{errors.regoCode}</AlertDescription>
              </Alert>
            )}
          </CardContent>
        </Card>

        <div className="flex justify-end gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => router.push('/policies')}
            disabled={submitting}
          >
            キャンセル
          </Button>
          <Button type="submit" disabled={submitting || validationErrors.length > 0}>
            {submitting ? '更新中...' : '更新'}
          </Button>
        </div>
      </form>

      <PolicyVersionHistory policyId={policy.id} />
    </div>
  )
}
