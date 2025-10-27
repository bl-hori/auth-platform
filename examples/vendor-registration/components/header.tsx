'use client';

import Link from 'next/link';
import { useSession } from '@/lib/session-context';
import { Button } from '@/components/ui/button';
import { ROLE_LABELS } from '@/types/auth';

/**
 * アプリケーションヘッダーコンポーネント
 *
 * ナビゲーションとユーザー情報を表示します。
 * - ログイン状態に応じて表示を切り替え
 * - ユーザー名とロールを表示
 * - ログアウトボタンを提供
 */
export function Header() {
  const { user, loading, logout } = useSession();

  return (
    <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-14 items-center">
        <div className="mr-4 flex">
          <Link className="mr-6 flex items-center space-x-2" href="/">
            <span className="font-bold">取引先登録システム</span>
          </Link>
        </div>
        <div className="flex flex-1 items-center justify-end space-x-4">
          {loading ? (
            <span className="text-sm text-muted-foreground">読み込み中...</span>
          ) : user ? (
            <>
              <div className="flex items-center space-x-2">
                <span className="text-sm font-medium">{user.name}</span>
                <span className="text-xs text-muted-foreground">
                  ({ROLE_LABELS[user.role]})
                </span>
              </div>
              <Button variant="outline" size="sm" onClick={logout}>
                ログアウト
              </Button>
            </>
          ) : (
            <Link href="/login">
              <Button variant="outline" size="sm">
                ログイン
              </Button>
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
