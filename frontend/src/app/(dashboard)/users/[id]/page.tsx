'use client'

/**
 * User Detail/Edit Page
 *
 * @description Page for viewing and editing user details
 */

import { useState, useEffect } from 'react'
import { useRouter, useParams } from 'next/navigation'
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
import { RoleAssignment } from '@/components/users/role-assignment'
import { useToast } from '@/hooks/use-toast'
import { getUser, updateUser } from '@/lib/api/users'
import type { User } from '@/types'
import { UserStatus } from '@/types'

/**
 * User edit page component
 */
export default function UserEditPage() {
  const router = useRouter()
  const params = useParams()
  const { toast } = useToast()
  const userId = params.id as string

  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [formData, setFormData] = useState({
    email: '',
    displayName: '',
    status: UserStatus.ACTIVE,
  })

  /**
   * Load user data
   */
  useEffect(() => {
    const loadUser = async () => {
      try {
        const userData = await getUser(userId)
        setUser(userData)
        setFormData({
          email: userData.email,
          displayName: userData.displayName,
          status: userData.status,
        })
      } catch (error) {
        console.error('Failed to load user:', error)
        toast({
          title: 'エラー',
          description: 'ユーザー情報の読み込みに失敗しました',
          variant: 'destructive',
        })
      } finally {
        setLoading(false)
      }
    }

    loadUser()
  }, [userId, toast])

  /**
   * Handle form submission
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)

    try {
      await updateUser(userId, formData)
      toast({
        title: '成功',
        description: 'ユーザー情報を更新しました',
        variant: 'success',
      })
      router.push('/users')
    } catch (error) {
      console.error('Failed to update user:', error)
      toast({
        title: 'エラー',
        description: 'ユーザー情報の更新に失敗しました',
        variant: 'destructive',
      })
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <ProtectedRoute>
        <DashboardLayout>
          <div className="flex items-center justify-center p-8">
            <p>読み込み中...</p>
          </div>
        </DashboardLayout>
      </ProtectedRoute>
    )
  }

  if (!user) {
    return (
      <ProtectedRoute>
        <DashboardLayout>
          <div className="flex flex-col items-center justify-center p-8">
            <p className="text-muted-foreground">ユーザーが見つかりません</p>
            <Button
              variant="outline"
              onClick={() => router.push('/users')}
              className="mt-4"
            >
              ユーザー一覧に戻る
            </Button>
          </div>
        </DashboardLayout>
      </ProtectedRoute>
    )
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
                ユーザー編集
              </h1>
              <p className="text-muted-foreground">ユーザー情報を編集します</p>
            </div>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit}>
            <div className="grid gap-6 md:grid-cols-2">
              {/* Basic Information */}
              <Card className="md:col-span-2">
                <CardHeader>
                  <CardTitle>基本情報</CardTitle>
                  <CardDescription>
                    ユーザーの基本情報を設定します
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid gap-4 md:grid-cols-2">
                    <div className="space-y-2">
                      <Label htmlFor="username">ユーザー名</Label>
                      <Input id="username" value={user.username} disabled />
                      <p className="text-xs text-muted-foreground">
                        ユーザー名は変更できません
                      </p>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="email">メールアドレス</Label>
                      <Input
                        id="email"
                        type="email"
                        value={formData.email}
                        onChange={e =>
                          setFormData({ ...formData, email: e.target.value })
                        }
                        required
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="displayName">表示名</Label>
                      <Input
                        id="displayName"
                        value={formData.displayName}
                        onChange={e =>
                          setFormData({
                            ...formData,
                            displayName: e.target.value,
                          })
                        }
                        required
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="status">ステータス</Label>
                      <select
                        id="status"
                        value={formData.status}
                        onChange={e =>
                          setFormData({
                            ...formData,
                            status: e.target
                              .value as (typeof UserStatus)[keyof typeof UserStatus],
                          })
                        }
                        className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                      >
                        <option value="ACTIVE">有効</option>
                        <option value="INACTIVE">無効</option>
                        <option value="SUSPENDED">停止中</option>
                      </select>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Role Assignment */}
              <div className="md:col-span-2">
                <RoleAssignment userId={user.id} />
              </div>

              {/* Metadata */}
              <Card className="md:col-span-2">
                <CardHeader>
                  <CardTitle>メタデータ</CardTitle>
                  <CardDescription>システムが管理する情報</CardDescription>
                </CardHeader>
                <CardContent className="space-y-2">
                  <div className="grid gap-4 md:grid-cols-3">
                    <div>
                      <p className="text-sm font-medium">ユーザーID</p>
                      <p className="text-sm text-muted-foreground">{user.id}</p>
                    </div>
                    <div>
                      <p className="text-sm font-medium">作成日時</p>
                      <p className="text-sm text-muted-foreground">
                        {new Date(user.createdAt).toLocaleString('ja-JP')}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm font-medium">更新日時</p>
                      <p className="text-sm text-muted-foreground">
                        {new Date(user.updatedAt).toLocaleString('ja-JP')}
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Actions */}
            <div className="mt-6 flex gap-4">
              <Button type="submit" disabled={saving}>
                <Save className="mr-2 h-4 w-4" />
                {saving ? '保存中...' : '保存'}
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
