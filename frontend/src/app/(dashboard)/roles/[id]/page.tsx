/**
 * Role Edit Page
 *
 * @description Page for editing role details and managing permissions
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
import { LoadingSpinner } from '@/components/ui/loading-spinner'
import { useToast } from '@/hooks/use-toast'
import { PermissionAssignment } from '@/components/roles/permission-assignment'
import { RoleHierarchy } from '@/components/roles/role-hierarchy'
import {
  getRole,
  getRoles,
  updateRole,
  type UpdateRoleRequest,
} from '@/lib/api/roles'
import type { Role } from '@/types'

/**
 * Role edit page component
 */
export default function RoleEditPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const router = useRouter()
  const { toast } = useToast()

  const [role, setRole] = useState<Role | null>(null)
  const [roles, setRoles] = useState<Role[]>([])
  const [loading, setLoading] = useState(true)
  const [formData, setFormData] = useState({
    displayName: '',
    description: '',
    parentRoleId: '',
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)

  /**
   * Load role data
   */
  useEffect(() => {
    const loadData = async () => {
      setLoading(true)
      try {
        const { id } = await params
        const [roleData, rolesResponse] = await Promise.all([
          getRole(id),
          getRoles(),
        ])

        setRole(roleData)
        // Ensure we always have an array from the content field
        setRoles(Array.isArray(rolesResponse.content) ? rolesResponse.content : [])
        setFormData({
          displayName: roleData.displayName,
          description: roleData.description || '',
          parentRoleId: roleData.parentRoleId || '',
        })
      } catch (error) {
        console.error('Failed to load role:', error)
        // Set empty array on error
        setRoles([])
        toast({
          title: 'エラー',
          description: 'ロール情報の読み込みに失敗しました',
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
  }

  /**
   * Validate form data
   */
  const validate = (): boolean => {
    const newErrors: Record<string, string> = {}

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
    if (!role) return

    setSubmitting(true)
    try {
      const request: UpdateRoleRequest = {
        displayName: formData.displayName,
        description: formData.description || undefined,
        parentRoleId: formData.parentRoleId || undefined,
      }

      const updatedRole = await updateRole(role.id, request)
      setRole(updatedRole)

      toast({
        title: '成功',
        description: 'ロールを更新しました',
        variant: 'success',
      })
    } catch (error) {
      console.error('Failed to update role:', error)
      toast({
        title: 'エラー',
        description: 'ロールの更新に失敗しました',
        variant: 'destructive',
      })
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (!role) {
    return (
      <div className="space-y-6">
        <Alert variant="destructive">
          <AlertDescription>ロールが見つかりません</AlertDescription>
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
          onClick={() => router.push('/roles')}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-3xl font-bold">ロール編集</h1>
          <p className="text-muted-foreground">{role.name}</p>
        </div>
      </div>

      <form onSubmit={handleSubmit}>
        <Card>
          <CardHeader>
            <CardTitle>基本情報</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">名前</Label>
              <Input id="name" value={role.name} disabled />
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
                  roles
                    .filter(r => r.id !== role?.id)
                    .map(r => (
                      <option key={r.id} value={r.id}>
                        {r.displayName}
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
                {submitting ? '更新中...' : '更新'}
              </Button>
            </div>
          </CardContent>
        </Card>
      </form>

      <PermissionAssignment roleId={role.id} />

      <RoleHierarchy currentRoleId={role.id} />
    </div>
  )
}
