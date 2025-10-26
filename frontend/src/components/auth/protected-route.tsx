'use client'

/**
 * Protected Route Component
 *
 * @description Wrapper component that ensures user is authenticated before
 * rendering children. Redirects to login if not authenticated.
 */

import { useRouter } from 'next/navigation'
import { useEffect, type ReactNode } from 'react'

import { useAuth } from '@/contexts/auth-context'

/**
 * Props for ProtectedRoute component
 */
interface ProtectedRouteProps {
  children: ReactNode
  /**
   * Custom loading component to show while checking authentication
   */
  fallback?: ReactNode
}

/**
 * Protected route wrapper component
 *
 * @description Checks authentication state and redirects to login if not authenticated
 *
 * @example
 * ```tsx
 * <ProtectedRoute>
 *   <DashboardPage />
 * </ProtectedRoute>
 * ```
 */
export function ProtectedRoute({ children, fallback }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading } = useAuth()
  const router = useRouter()

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      // Redirect to login if not authenticated
      router.push('/login')
    }
  }, [isAuthenticated, isLoading, router])

  // Show loading state while checking authentication
  if (isLoading) {
    return (
      <>
        {fallback || (
          <div className="flex min-h-screen items-center justify-center">
            <div className="space-y-4 text-center">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
              <p className="text-sm text-muted-foreground">Loading...</p>
            </div>
          </div>
        )}
      </>
    )
  }

  // Don't render children if not authenticated
  if (!isAuthenticated) {
    return null
  }

  return <>{children}</>
}
