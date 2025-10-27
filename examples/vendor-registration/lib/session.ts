import { User, UserRole } from '@/types/auth';
import { getUserByRole, MOCK_USERS } from './mock-data';

/**
 * セッションストレージのキー
 */
const SESSION_KEY = 'vendor-app-session';

/**
 * セッションデータ
 */
export interface SessionData {
  user: User;
  loginAt: string;
}

/**
 * 現在のセッションを取得
 * ブラウザのlocalStorageから取得（デモ用）
 */
export function getSession(): SessionData | null {
  if (typeof window === 'undefined') {
    return null;
  }

  try {
    const sessionJson = localStorage.getItem(SESSION_KEY);
    if (!sessionJson) {
      return null;
    }
    return JSON.parse(sessionJson) as SessionData;
  } catch (error) {
    console.error('Failed to parse session:', error);
    return null;
  }
}

/**
 * 現在のユーザーを取得
 */
export function getCurrentUser(): User | null {
  const session = getSession();
  return session?.user || null;
}

/**
 * セッションを保存
 */
export function setSession(user: User): void {
  if (typeof window === 'undefined') {
    return;
  }

  const sessionData: SessionData = {
    user,
    loginAt: new Date().toISOString(),
  };

  localStorage.setItem(SESSION_KEY, JSON.stringify(sessionData));
}

/**
 * ログイン処理（モック）
 * 実際のアプリではバックエンドAPIを呼び出す
 */
export function login(email: string, role?: UserRole): User | null {
  // メールアドレスでユーザーを検索
  let user = MOCK_USERS.find((u) => u.email === email);

  // 見つからない場合はロールで検索
  if (!user && role) {
    user = getUserByRole(role);
  }

  if (!user) {
    return null;
  }

  setSession(user);
  return user;
}

/**
 * ログアウト処理
 */
export function logout(): void {
  if (typeof window === 'undefined') {
    return;
  }

  localStorage.removeItem(SESSION_KEY);
}

/**
 * ログイン状態の確認
 */
export function isAuthenticated(): boolean {
  return getSession() !== null;
}

/**
 * ユーザーロールの確認
 */
export function hasRole(role: UserRole): boolean {
  const user = getCurrentUser();
  return user?.role === role;
}

/**
 * 複数のロールのいずれかを持っているか確認
 */
export function hasAnyRole(roles: UserRole[]): boolean {
  const user = getCurrentUser();
  if (!user) return false;
  return roles.includes(user.role);
}
