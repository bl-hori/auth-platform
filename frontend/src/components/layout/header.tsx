'use client'

/**
 * Header Component
 *
 * @description Top navigation bar with user menu and organization context
 */

import { LogOut, User, Building2 } from 'lucide-react'
import { useRouter } from 'next/navigation'

import { Button } from '@/components/ui/button'
import { useAuth } from '@/contexts/auth-context'

/**
 * Header component
 */
export function Header() {
  const { user, organization, logout } = useAuth()
  const router = useRouter()

  /**
   * Handle logout
   */
  const handleLogout = () => {
    logout()
    router.push('/login')
  }

  return (
    <header className="fixed left-64 right-0 top-0 z-30 h-16 border-b bg-card">
      <div className="flex h-full items-center justify-between px-6">
        <div className="flex items-center space-x-4">
          {/* Organization Info */}
          {organization && (
            <div className="flex items-center space-x-2 rounded-lg bg-muted px-3 py-1.5">
              <Building2 className="h-4 w-4 text-muted-foreground" />
              <span className="text-sm font-medium">
                {organization.displayName}
              </span>
            </div>
          )}
        </div>

        <div className="flex items-center space-x-4">
          {/* User Info */}
          {user && (
            <div className="flex items-center space-x-3">
              <div className="text-right">
                <p className="text-sm font-medium">{user.displayName}</p>
                <p className="text-xs text-muted-foreground">{user.email}</p>
              </div>
              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-primary-foreground">
                <User className="h-5 w-5" />
              </div>
            </div>
          )}

          {/* Logout Button */}
          <Button variant="ghost" size="sm" onClick={handleLogout}>
            <LogOut className="mr-2 h-4 w-4" />
            ログアウト
          </Button>
        </div>
      </div>
    </header>
  )
}
