'use client';

import Link from 'next/link';
import { usePermission } from '@/lib/authorization';
import { ReactNode } from 'react';

/**
 * 保護されたリンクコンポーネントのプロパティ
 */
export interface ProtectedLinkProps {
  /** リンク先URL */
  href: string;
  /** 必要なアクション */
  action: string;
  /** リソースタイプ */
  resourceType: string;
  /** リソースID（オプション） */
  resourceId?: string;
  /** 追加コンテキスト */
  context?: Record<string, any>;
  /** 権限がない場合にリンクを非表示にするか（デフォルト: false = disabledスタイルにする） */
  hideWhenUnauthorized?: boolean;
  /** CSSクラス */
  className?: string;
  /** 子要素 */
  children: ReactNode;
}

/**
 * 保護されたリンクコンポーネント
 *
 * 指定された権限がある場合のみ有効化されるリンクです。
 * 権限がない場合はdisabledスタイルになるか、非表示になります。
 *
 * @example
 * ```tsx
 * <ProtectedLink
 *   href={`/vendors/${vendorId}/edit`}
 *   action="update"
 *   resourceType="vendor"
 *   resourceId={vendorId}
 * >
 *   編集
 * </ProtectedLink>
 * ```
 */
export function ProtectedLink({
  href,
  action,
  resourceType,
  resourceId,
  context,
  hideWhenUnauthorized = false,
  className = '',
  children,
}: ProtectedLinkProps) {
  const { allowed, loading } = usePermission(action, resourceType, resourceId, context);

  // ローディング中
  if (loading) {
    return (
      <span className={`cursor-wait opacity-50 ${className}`}>
        {children}
      </span>
    );
  }

  // 権限がない場合
  if (!allowed) {
    if (hideWhenUnauthorized) {
      return null;
    }
    return (
      <span className={`cursor-not-allowed opacity-50 ${className}`}>
        {children}
      </span>
    );
  }

  // 権限がある場合
  return (
    <Link href={href} className={className}>
      {children}
    </Link>
  );
}
