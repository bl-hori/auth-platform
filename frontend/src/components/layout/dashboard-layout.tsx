/**
 * Dashboard Layout Component
 *
 * @description Main layout wrapper for dashboard pages with sidebar and header
 */

import { Header } from './header'
import { Sidebar } from './sidebar'

/**
 * Props for DashboardLayout
 */
interface DashboardLayoutProps {
  children: React.ReactNode
}

/**
 * Dashboard layout component
 *
 * @example
 * ```tsx
 * <DashboardLayout>
 *   <YourPageContent />
 * </DashboardLayout>
 * ```
 */
export function DashboardLayout({ children }: DashboardLayoutProps) {
  return (
    <div className="min-h-screen bg-background">
      <Sidebar />
      <div className="pl-64">
        <Header />
        <main className="pt-16">
          <div className="container mx-auto p-6">{children}</div>
        </main>
      </div>
    </div>
  )
}
