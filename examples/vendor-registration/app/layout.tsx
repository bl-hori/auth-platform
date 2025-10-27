import type { Metadata } from 'next';
import { SessionProvider } from '@/lib/session-context';
import { Header } from '@/components/header';
import './globals.css';

export const metadata: Metadata = {
  title: 'Vendor Registration - Auth Platform Example',
  description: 'Example application demonstrating Auth Platform authorization integration',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ja">
      <body className="min-h-screen bg-background font-sans antialiased">
        <SessionProvider>
          <div className="relative flex min-h-screen flex-col">
            <Header />
            <main className="flex-1">{children}</main>
            <footer className="border-t py-6 md:py-0">
              <div className="container flex h-14 items-center justify-between">
                <p className="text-sm text-muted-foreground">
                  Auth Platform 認可機構の実装例
                </p>
              </div>
            </footer>
          </div>
        </SessionProvider>
      </body>
    </html>
  );
}
