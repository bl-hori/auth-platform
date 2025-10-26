'use client'

/**
 * User Creation Page
 *
 * @description Page for creating new users
 */

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { ArrowLeft, Save } from 'lucide-react'

import { ProtectedRoute } from '@/components/auth/protected-route'
import { DashboardLayout } from '@/components/layout/dashboard-layout'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { useToast } from '@/hooks/use-toast'
import { useAuth } from '@/contexts/auth-context'
import { createUser } from '@/lib/api/users'

/**
 * User creation page component
 */
export default function UserNewPage() {
  const router = useRouter()
  const { toast } = useToast()
  const { organization } = useAuth()

  const [saving, setSaving] = useState(false)
  const [formData, setFormData] = useState({
    email: '',
    username: '',
    displayName: '',
  })

  const [errors, setErrors] = useState<Record<string, string>>({})

  /**
   * Validate form data
   */
  const validate = (): boolean => {
    const newErrors: Record<string, string> = {}

    if (!formData.email) {
      newErrors.email = 'メールアドレスは必須です'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = '有効なメールアドレスを入力してください'
    }

    if (!formData.username) {
      newErrors.username = 'ユーザー名は必須です'
    } else if (!/^[a-zA-Z0-9._-]+$/.test(formData.username)) {
      newErrors.username =
        'ユーザー名は英数字とピリオド、ハイフン、アンダースコアのみ使用できます'
    }

    if (!formData.displayName) {
      newErrors.displayName = '表示名は必須です'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  /**
   * Handle form submission
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validate()) {
      return
    }

    if (!organization) {
      toast({
        title: 'エラー',
        description: '組織情報が取得できません',
        variant: 'destructive',
      })
      return
    }

    setSaving(true)

    try {
      await createUser({
        ...formData,
        organizationId: organization.id,
      })

      toast({
        title: '成功',
        description: 'ユーザーを作成しました',
        variant: 'success',
      })

      router.push('/users')
    } catch (error) {
      console.error('Failed to create user:', error)
      toast({
        title: 'エラー',
        description: 'ユーザーの作成に失敗しました',
        variant: 'destructive',
      })
    } finally {
      setSaving(false)
    }
  }

  return (
    <ProtectedRoute>
      <DashboardLayout>
        <div className="space-y-6">
          {/* Header */}
          <div className="flex items-center gap-4">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => router.push('/users')}
            >
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <h1 className="text-3xl font-bold tracking-tight">
                新規ユーザー作成
              </h1>
              <p className="text-muted-foreground">
                新しいユーザーを登録します
              </p>
            </div>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit}>
            <Card>
              <CardHeader>
                <CardTitle>ユーザー情報</CardTitle>
                <CardDescription>
                  新しいユーザーの基本情報を入力してください
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="username">
                      ユーザー名 <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="username"
                      value={formData.username}
                      onChange={e => {
                        setFormData({ ...formData, username: e.target.value })
                        setErrors({ ...errors, username: '' })
                      }}
                      placeholder="john.doe"
                      required
                    />
                    {errors.username && (
                      <p className="text-xs text-destructive">
                        {errors.username}
                      </p>
                    )}
                    <p className="text-xs text-muted-foreground">
                      英数字、ピリオド、ハイフン、アンダースコアが使用できます
                    </p>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="email">
                      メールアドレス <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="email"
                      type="email"
                      value={formData.email}
                      onChange={e => {
                        setFormData({ ...formData, email: e.target.value })
                        setErrors({ ...errors, email: '' })
                      }}
                      placeholder="john.doe@example.com"
                      required
                    />
                    {errors.email && (
                      <p className="text-xs text-destructive">{errors.email}</p>
                    )}
                  </div>

                  <div className="space-y-2 md:col-span-2">
                    <Label htmlFor="displayName">
                      表示名 <span className="text-destructive">*</span>
                    </Label>
                    <Input
                      id="displayName"
                      value={formData.displayName}
                      onChange={e => {
                        setFormData({
                          ...formData,
                          displayName: e.target.value,
                        })
                        setErrors({ ...errors, displayName: '' })
                      }}
                      placeholder="John Doe"
                      required
                    />
                    {errors.displayName && (
                      <p className="text-xs text-destructive">
                        {errors.displayName}
                      </p>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Actions */}
            <div className="mt-6 flex gap-4">
              <Button type="submit" disabled={saving}>
                <Save className="mr-2 h-4 w-4" />
                {saving ? '作成中...' : '作成'}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => router.push('/users')}
              >
                キャンセル
              </Button>
            </div>
          </form>
        </div>
      </DashboardLayout>
    </ProtectedRoute>
  )
}
