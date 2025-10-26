/**
 * Policy Creation Page
 *
 * @description Page for creating new authorization policies
 */

'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { useToast } from '@/hooks/use-toast'
import { useAuth } from '@/contexts/auth-context'
import { PolicyEditor } from '@/components/policies/policy-editor'
import { createPolicy, type CreatePolicyRequest } from '@/lib/api/policies'

const DEFAULT_REGO_CODE = `package authz

# デフォルトでアクセスを拒否
default allow = false

# ルールをここに記述
# allow {
#   input.method == "GET"
#   input.user.role == "admin"
# }
`

/**
 * Policy creation page component
 */
export default function NewPolicyPage() {
  const router = useRouter()
  const { toast } = useToast()
  const { organization } = useAuth()

  const [formData, setFormData] = useState({
    name: '',
    displayName: '',
    description: '',
    regoCode: DEFAULT_REGO_CODE,
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)

  /**
   * Handle form field changes
   */
  const handleChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }))
    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }))
    }
  }

  /**
   * Validate form data
   */
  const validate = (): boolean => {
    const newErrors: Record<string, string> = {}

    if (!formData.name.trim()) {
      newErrors.name = '名前は必須です'
    } else if (!/^[a-zA-Z0-9_-]+$/.test(formData.name)) {
      newErrors.name =
        '名前は英数字、ハイフン、アンダースコアのみ使用できます'
    }

    if (!formData.displayName.trim()) {
      newErrors.displayName = '表示名は必須です'
    }

    if (!formData.regoCode.trim()) {
      newErrors.regoCode = 'Regoコードは必須です'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  /**
   * Handle form submission
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validate()) return
    if (!organization) {
      toast({
        title: 'エラー',
        description: '組織情報が見つかりません',
        variant: 'destructive',
      })
      return
    }

    setSubmitting(true)
    try {
      const request: CreatePolicyRequest = {
        name: formData.name,
        displayName: formData.displayName,
        description: formData.description || undefined,
        regoCode: formData.regoCode,
        organizationId: organization.id,
      }

      const policy = await createPolicy(request)

      toast({
        title: '成功',
        description: 'ポリシーを作成しました',
        variant: 'success',
      })

      router.push(`/policies/${policy.id}`)
    } catch (error) {
      console.error('Failed to create policy:', error)
      toast({
        title: 'エラー',
        description: 'ポリシーの作成に失敗しました',
        variant: 'destructive',
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => router.push('/policies')}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-3xl font-bold">新規ポリシー作成</h1>
          <p className="text-muted-foreground">
            新しい認可ポリシーを作成します
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>基本情報</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">
                名前 <span className="text-red-500">*</span>
              </Label>
              <Input
                id="name"
                value={formData.name}
                onChange={e => handleChange('name', e.target.value)}
                placeholder="user_access_policy"
                disabled={submitting}
              />
              {errors.name && (
                <Alert variant="destructive">
                  <AlertDescription>{errors.name}</AlertDescription>
                </Alert>
              )}
              <p className="text-sm text-muted-foreground">
                英数字、ハイフン、アンダースコアのみ使用できます
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
                placeholder="ユーザーアクセスポリシー"
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
                placeholder="ポリシーの説明を入力"
                disabled={submitting}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>ポリシーコード (Rego)</CardTitle>
          </CardHeader>
          <CardContent>
            <PolicyEditor
              value={formData.regoCode}
              onChange={value => handleChange('regoCode', value)}
              readOnly={submitting}
            />
            {errors.regoCode && (
              <Alert variant="destructive" className="mt-4">
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
          <Button type="submit" disabled={submitting}>
            {submitting ? '作成中...' : '作成'}
          </Button>
        </div>
      </form>
    </div>
  )
}
