'use client'

/**
 * Sidebar Navigation Component
 *
 * @description Main navigation sidebar for the dashboard
 */

import {
  LayoutDashboard,
  Users,
  Shield,
  FileText,
  History,
  Settings,
  type LucideIcon,
} from 'lucide-react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'

import { cn } from '@/lib/utils'

/**
 * Navigation item interface
 */
interface NavItem {
  href: string
  label: string
  icon: LucideIcon
}

/**
 * Navigation items configuration
 */
const navItems: NavItem[] = [
  {
    href: '/dashboard',
    label: 'ダッシュボード',
    icon: LayoutDashboard,
  },
  {
    href: '/users',
    label: 'ユーザー管理',
    icon: Users,
  },
  {
    href: '/roles',
    label: 'ロール管理',
    icon: Shield,
  },
  {
    href: '/policies',
    label: 'ポリシー管理',
    icon: FileText,
  },
  {
    href: '/audit-logs',
    label: '監査ログ',
    icon: History,
  },
  {
    href: '/settings',
    label: '設定',
    icon: Settings,
  },
]

/**
 * Sidebar component
 */
export function Sidebar() {
  const pathname = usePathname()

  return (
    <aside className="fixed left-0 top-0 z-40 h-screen w-64 border-r bg-card">
      <div className="flex h-full flex-col">
        {/* Logo/Brand */}
        <div className="flex h-16 items-center border-b px-6">
          <Link href="/dashboard" className="flex items-center space-x-2">
            <Shield className="h-6 w-6 text-primary" />
            <span className="text-lg font-bold">Auth Platform</span>
          </Link>
        </div>

        {/* Navigation */}
        <nav className="flex-1 space-y-1 p-4">
          {navItems.map(item => {
            const isActive = pathname === item.href
            const Icon = item.icon

            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'flex items-center space-x-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
                )}
              >
                <Icon className="h-5 w-5" />
                <span>{item.label}</span>
              </Link>
            )
          })}
        </nav>

        {/* Footer */}
        <div className="border-t p-4">
          <div className="rounded-lg bg-muted p-3">
            <p className="text-xs text-muted-foreground">Version 1.0.0</p>
            <p className="text-xs text-muted-foreground">Phase 1 MVP</p>
          </div>
        </div>
      </div>
    </aside>
  )
}
