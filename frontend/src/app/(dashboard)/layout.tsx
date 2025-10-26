/**
 * Dashboard Route Group Layout
 *
 * @description Shared layout for all dashboard pages with sidebar and authentication
 */

import { ProtectedRoute } from '@/components/auth/protected-route'
import { DashboardLayout } from '@/components/layout/dashboard-layout'

/**
 * Props for DashboardRootLayout
 */
interface DashboardRootLayoutProps {
  children: React.ReactNode
}

/**
 * Dashboard root layout component
 *
 * @description Wraps all pages in the (dashboard) route group with ProtectedRoute and DashboardLayout
 * This ensures consistent layout with sidebar across all dashboard pages
 */
export default function DashboardRootLayout({
  children,
}: DashboardRootLayoutProps) {
  return (
    <ProtectedRoute>
      <DashboardLayout>{children}</DashboardLayout>
    </ProtectedRoute>
  )
}
