'use client';

import { Button, ButtonProps } from '@/components/ui/button';
import { usePermission } from '@/lib/authorization';

/**
 * 保護されたボタンコンポーネントのプロパティ
 */
export interface ProtectedButtonProps extends ButtonProps {
  /** 必要なアクション */
  action: string;
  /** リソースタイプ */
  resourceType: string;
  /** リソースID（オプション） */
  resourceId?: string;
  /** 追加コンテキスト */
  context?: Record<string, any>;
  /** 権限がない場合にボタンを非表示にするか（デフォルト: false = disabledにする） */
  hideWhenUnauthorized?: boolean;
}

/**
 * 保護されたボタンコンポーネント
 *
 * 指定された権限がある場合のみ有効化されるボタンです。
 * 権限がない場合はdisabledになるか、非表示になります。
 *
 * @example
 * ```tsx
 * <ProtectedButton
 *   action="delete"
 *   resourceType="vendor"
 *   resourceId={vendorId}
 *   variant="destructive"
 *   onClick={handleDelete}
 * >
 *   削除
 * </ProtectedButton>
 * ```
 *
 * @example 権限がない場合は非表示
 * ```tsx
 * <ProtectedButton
 *   action="approve"
 *   resourceType="vendor"
 *   resourceId={vendorId}
 *   hideWhenUnauthorized
 * >
 *   承認
 * </ProtectedButton>
 * ```
 */
export function ProtectedButton({
  action,
  resourceType,
  resourceId,
  context,
  hideWhenUnauthorized = false,
  children,
  disabled,
  ...props
}: ProtectedButtonProps) {
  const { allowed, loading } = usePermission(action, resourceType, resourceId, context);

  // ローディング中はdisabled
  if (loading) {
    return (
      <Button disabled {...props}>
        {children}
      </Button>
    );
  }

  // 権限がない場合
  if (!allowed) {
    if (hideWhenUnauthorized) {
      return null;
    }
    return (
      <Button disabled {...props}>
        {children}
      </Button>
    );
  }

  // 権限がある場合
  return (
    <Button disabled={disabled} {...props}>
      {children}
    </Button>
  );
}
