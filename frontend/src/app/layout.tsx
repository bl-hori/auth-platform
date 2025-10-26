import type { Metadata } from 'next'

import './globals.css'
import { ErrorBoundary } from '@/components/error-boundary'
import { AuthProvider } from '@/contexts/auth-context'
import { ToastProvider } from '@/hooks/use-toast'

export const metadata: Metadata = {
  title: 'Auth Platform',
  description: 'Enterprise Authorization Platform',
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="ja">
      <body className="antialiased">
        <ErrorBoundary>
          <AuthProvider>
            <ToastProvider>{children}</ToastProvider>
          </AuthProvider>
        </ErrorBoundary>
      </body>
    </html>
  )
}
