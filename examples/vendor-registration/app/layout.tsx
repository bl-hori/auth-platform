import type { Metadata } from 'next';
import Link from 'next/link';
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
        <div className="relative flex min-h-screen flex-col">
          <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
            <div className="container flex h-14 items-center">
              <div className="mr-4 flex">
                <Link className="mr-6 flex items-center space-x-2" href="/">
                  <span className="font-bold">取引先登録システム</span>
                </Link>
              </div>
              <div className="flex flex-1 items-center justify-end space-x-2">
                <nav className="flex items-center space-x-2">
                  <span className="text-sm text-muted-foreground">Auth Platform Example</span>
                </nav>
              </div>
            </div>
          </header>
          <main className="flex-1">{children}</main>
          <footer className="border-t py-6 md:py-0">
            <div className="container flex h-14 items-center justify-between">
              <p className="text-sm text-muted-foreground">
                Auth Platform 認可機構の実装例
              </p>
            </div>
          </footer>
        </div>
      </body>
    </html>
  );
}
