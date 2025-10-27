'use client';

import { createContext, useContext, useEffect, useState, useCallback, ReactNode } from 'react';
import { User } from '@/types/auth';
import { getCurrentUser, clearSession } from '@/lib/session';

/**
 * セッションコンテキストの型定義
 */
interface SessionContextType {
  /** 現在のユーザー（未ログインの場合はnull） */
  user: User | null;
  /** セッションの読み込み状態 */
  loading: boolean;
  /** ログアウト関数 */
  logout: () => void;
  /** セッションを再読み込みする関数 */
  refreshSession: () => void;
}

/**
 * セッションコンテキスト
 */
const SessionContext = createContext<SessionContextType | undefined>(undefined);

/**
 * セッションコンテキストプロバイダーのプロパティ
 */
interface SessionProviderProps {
  children: ReactNode;
}

/**
 * セッションコンテキストプロバイダー
 *
 * アプリケーション全体でユーザーセッションを管理します。
 * - localStorageからセッションを自動読み込み
 * - ページリフレッシュ時もセッションを維持
 * - ログアウト機能を提供
 *
 * @example
 * ```tsx
 * // app/layout.tsxでアプリ全体をラップ
 * export default function RootLayout({ children }) {
 *   return (
 *     <SessionProvider>
 *       {children}
 *     </SessionProvider>
 *   );
 * }
 * ```
 */
export function SessionProvider({ children }: SessionProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  /**
   * セッションを初期化する
   * localStorageからユーザー情報を読み込む
   */
  const initializeSession = useCallback(() => {
    setLoading(true);
    try {
      const currentUser = getCurrentUser();
      setUser(currentUser);
    } catch (error) {
      console.error('Failed to initialize session:', error);
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * ログアウトする
   * localStorageをクリアして、ログインページにリダイレクト
   */
  const logout = useCallback(() => {
    clearSession();
    setUser(null);
    // ログインページにリダイレクト
    window.location.href = '/login';
  }, []);

  /**
   * セッションを再読み込みする
   * ログイン後などに呼び出す
   */
  const refreshSession = useCallback(() => {
    initializeSession();
  }, [initializeSession]);

  // コンポーネントマウント時にセッションを初期化
  useEffect(() => {
    initializeSession();
  }, [initializeSession]);

  return (
    <SessionContext.Provider value={{ user, loading, logout, refreshSession }}>
      {children}
    </SessionContext.Provider>
  );
}

/**
 * セッションコンテキストを使用するフック
 *
 * 現在のユーザー情報とセッション管理機能にアクセスします。
 *
 * @throws {Error} SessionProvider外で使用された場合
 *
 * @example
 * ```tsx
 * function MyComponent() {
 *   const { user, loading, logout } = useSession();
 *
 *   if (loading) return <div>読み込み中...</div>;
 *   if (!user) return <div>ログインしてください</div>;
 *
 *   return (
 *     <div>
 *       <p>ようこそ、{user.name}さん</p>
 *       <button onClick={logout}>ログアウト</button>
 *     </div>
 *   );
 * }
 * ```
 */
export function useSession(): SessionContextType {
  const context = useContext(SessionContext);
  if (context === undefined) {
    throw new Error('useSession must be used within a SessionProvider');
  }
  return context;
}

/**
 * 現在のユーザーを取得するフック
 *
 * useSessionの簡易版。ユーザー情報のみが必要な場合に使用。
 *
 * @returns 現在のユーザー（未ログインの場合はnull）
 *
 * @example
 * ```tsx
 * function UserProfile() {
 *   const user = useUser();
 *   if (!user) return null;
 *
 *   return (
 *     <div>
 *       <h2>{user.name}</h2>
 *       <p>{user.email}</p>
 *       <p>ロール: {user.role}</p>
 *     </div>
 *   );
 * }
 * ```
 */
export function useUser(): User | null {
  const { user } = useSession();
  return user;
}
