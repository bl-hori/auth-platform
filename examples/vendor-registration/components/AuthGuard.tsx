'use client';

import { ReactNode } from 'react';
import { usePermission } from '@/lib/authorization';

/**
 * 認可ガードコンポーネントのプロパティ
 */
export interface AuthGuardProps {
  /** 必要なアクション（例: read, update, delete） */
  action: string;
  /** リソースタイプ（例: vendor） */
  resourceType: string;
  /** リソースID（オプション） */
  resourceId?: string;
  /** 追加コンテキスト */
  context?: Record<string, any>;
  /** 認可されている場合に表示するコンテンツ */
  children: ReactNode;
  /** 認可されていない場合に表示するフォールバック（デフォルト: null） */
  fallback?: ReactNode;
  /** ローディング中に表示するコンテンツ（デフォルト: null） */
  loading?: ReactNode;
}

/**
 * 認可ガードコンポーネント
 *
 * 指定された権限がある場合のみ子要素を表示します。
 * 宣言的に権限チェックを行うことができます。
 *
 * @example
 * ```tsx
 * <AuthGuard action="delete" resourceType="vendor" resourceId={vendorId}>
 *   <DeleteButton />
 * </AuthGuard>
 * ```
 *
 * @example フォールバックあり
 * ```tsx
 * <AuthGuard
 *   action="update"
 *   resourceType="vendor"
 *   resourceId={vendorId}
 *   fallback={<p>編集権限がありません</p>}
 * >
 *   <EditForm />
 * </AuthGuard>
 * ```
 */
export function AuthGuard({
  action,
  resourceType,
  resourceId,
  context,
  children,
  fallback = null,
  loading = null,
}: AuthGuardProps) {
  const { allowed, loading: isLoading } = usePermission(action, resourceType, resourceId, context);

  if (isLoading) {
    return <>{loading}</>;
  }

  if (!allowed) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
}
