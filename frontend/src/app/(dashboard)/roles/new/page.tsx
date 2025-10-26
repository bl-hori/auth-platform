/**
 * Role Creation Page
 *
 * @description Page for creating new roles
 */

'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { useToast } from '@/hooks/use-toast'
import { useAuth } from '@/contexts/auth-context'
import { createRole, getRoles, type CreateRoleRequest } from '@/lib/api/roles'
import type { Role } from '@/types'

/**
 * Role creation page component
 */
export default function NewRolePage() {
  const router = useRouter()
  const { toast } = useToast()
  const { organization } = useAuth()

  const [roles, setRoles] = useState<Role[]>([])
  const [formData, setFormData] = useState({
    name: '',
    displayName: '',
    description: '',
    parentRoleId: '',
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)

  /**
   * Load roles for parent role selection
   */
  useEffect(() => {
    const loadRoles = async () => {
      try {
        const data = await getRoles()
        // Ensure we always have an array
        setRoles(Array.isArray(data) ? data : [])
      } catch (error) {
        console.error('Failed to load roles:', error)
        // Set empty array on error
        setRoles([])
      }
    }
    loadRoles()
  }, [])

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
      const request: CreateRoleRequest = {
        name: formData.name,
        displayName: formData.displayName,
        description: formData.description || undefined,
        parentRoleId: formData.parentRoleId || undefined,
        organizationId: organization.id,
      }

      const role = await createRole(request)

      toast({
        title: '成功',
        description: 'ロールを作成しました',
        variant: 'success',
      })

      router.push(`/roles/${role.id}`)
    } catch (error) {
      console.error('Failed to create role:', error)
      toast({
        title: 'エラー',
        description: 'ロールの作成に失敗しました',
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
          onClick={() => router.push('/roles')}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-3xl font-bold">新規ロール作成</h1>
          <p className="text-muted-foreground">新しいロールを作成します</p>
        </div>
      </div>

      <form onSubmit={handleSubmit}>
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
                placeholder="admin, editor, viewer"
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
                placeholder="管理者、編集者、閲覧者"
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
                placeholder="ロールの説明を入力"
                disabled={submitting}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="parentRoleId">親ロール (オプション)</Label>
              <select
                id="parentRoleId"
                value={formData.parentRoleId}
                onChange={e => handleChange('parentRoleId', e.target.value)}
                disabled={submitting}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <option value="">親ロールを選択...</option>
                {Array.isArray(roles) &&
                  roles.map(role => (
                    <option key={role.id} value={role.id}>
                      {role.displayName}
                    </option>
                  ))}
              </select>
              <p className="text-sm text-muted-foreground">
                親ロールの権限を継承します
              </p>
            </div>

            <div className="flex justify-end gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => router.push('/roles')}
                disabled={submitting}
              >
                キャンセル
              </Button>
              <Button type="submit" disabled={submitting}>
                {submitting ? '作成中...' : '作成'}
              </Button>
            </div>
          </CardContent>
        </Card>
      </form>
    </div>
  )
}
