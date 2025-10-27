import type { Metadata } from 'next';
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
      <body className="antialiased">{children}</body>
    </html>
  );
}
