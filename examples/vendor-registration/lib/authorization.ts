'use client';

import { useCallback, useEffect, useState } from 'react';
import { authClient } from './auth-client';
import { getCurrentUser } from './session';
import { AuthorizationRequest, AuthorizationResponse } from '@/types/auth';

/**
 * 認可結果のキャッシュエントリ
 */
interface CacheEntry {
  response: AuthorizationResponse;
  expiresAt: number;
}

/**
 * 認可結果キャッシュ（TTL付き）
 * 同じ認可チェックを短期間に繰り返す場合のパフォーマンス最適化
 */
class AuthorizationCache {
  private cache = new Map<string, CacheEntry>();
  private defaultTTL = 60000; // 60秒

  /**
   * キャッシュキーを生成
   */
  private getCacheKey(request: AuthorizationRequest): string {
    return JSON.stringify({
      userId: request.userId,
      action: request.action,
      resourceType: request.resourceType,
      resourceId: request.resourceId,
    });
  }

  /**
   * キャッシュから取得
   */
  get(request: AuthorizationRequest): AuthorizationResponse | null {
    const key = this.getCacheKey(request);
    const entry = this.cache.get(key);

    if (!entry) {
      return null;
    }

    // 期限切れチェック
    if (Date.now() > entry.expiresAt) {
      this.cache.delete(key);
      return null;
    }

    return entry.response;
  }

  /**
   * キャッシュに保存
   */
  set(request: AuthorizationRequest, response: AuthorizationResponse, ttl?: number): void {
    const key = this.getCacheKey(request);
    const expiresAt = Date.now() + (ttl || this.defaultTTL);

    this.cache.set(key, { response, expiresAt });
  }

  /**
   * キャッシュをクリア
   */
  clear(): void {
    this.cache.clear();
  }

  /**
   * 特定のユーザーのキャッシュをクリア
   */
  clearForUser(userId: string): void {
    for (const [key] of this.cache) {
      if (key.includes(`"userId":"${userId}"`)) {
        this.cache.delete(key);
      }
    }
  }
}

/**
 * グローバル認可キャッシュ
 */
const authCache = new AuthorizationCache();

/**
 * 認可チェックを実行（キャッシュ付き）
 */
export async function checkAuthorization(
  action: string,
  resourceType: string,
  resourceId?: string,
  context?: Record<string, any>
): Promise<AuthorizationResponse> {
  const user = getCurrentUser();

  if (!user) {
    return {
      decision: 'DENY',
      reason: 'User not authenticated',
    };
  }

  const request: AuthorizationRequest = {
    userId: user.id,
    action,
    resourceType,
    resourceId,
    context,
  };

  // キャッシュチェック
  const cached = authCache.get(request);
  if (cached) {
    return cached;
  }

  // Auth Platform APIに問い合わせ
  const response = await authClient.authorize(request);

  // キャッシュに保存（ALLOWの場合のみ）
  if (response.decision === 'ALLOW') {
    authCache.set(request, response);
  }

  return response;
}

/**
 * サーバー側認可チェック（Server Actions用）
 */
export async function checkServerAuthorization(
  userId: string,
  action: string,
  resourceType: string,
  resourceId?: string,
  context?: Record<string, any>
): Promise<AuthorizationResponse> {
  const request: AuthorizationRequest = {
    userId,
    action,
    resourceType,
    resourceId,
    context,
  };

  return await authClient.authorize(request);
}

/**
 * 認可チェックReact Hook
 *
 * @example
 * ```tsx
 * const { checkPermission, isLoading } = useAuthorization();
 *
 * const canDelete = await checkPermission('delete', 'vendor', vendorId);
 * if (canDelete) {
 *   // 削除可能
 * }
 * ```
 */
export function useAuthorization() {
  const [isLoading, setIsLoading] = useState(false);

  const checkPermission = useCallback(
    async (
      action: string,
      resourceType: string,
      resourceId?: string,
      context?: Record<string, any>
    ): Promise<boolean> => {
      setIsLoading(true);
      try {
        const response = await checkAuthorization(action, resourceType, resourceId, context);
        return response.decision === 'ALLOW';
      } catch (error) {
        console.error('Authorization check failed:', error);
        return false; // フェイルセーフ
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  const clearCache = useCallback(() => {
    authCache.clear();
  }, []);

  return {
    checkPermission,
    isLoading,
    clearCache,
  };
}

/**
 * 特定のアクションに対する認可状態を管理するHook
 *
 * @example
 * ```tsx
 * const { allowed, loading } = usePermission('delete', 'vendor', vendorId);
 *
 * if (loading) return <Spinner />;
 * if (!allowed) return null;
 *
 * return <DeleteButton />;
 * ```
 */
export function usePermission(
  action: string,
  resourceType: string,
  resourceId?: string,
  context?: Record<string, any>
) {
  const [allowed, setAllowed] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let mounted = true;

    const check = async () => {
      try {
        setLoading(true);
        const response = await checkAuthorization(action, resourceType, resourceId, context);
        if (mounted) {
          setAllowed(response.decision === 'ALLOW');
          setError(null);
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err : new Error('Unknown error'));
          setAllowed(false); // フェイルセーフ
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    check();

    return () => {
      mounted = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [action, resourceType, resourceId]);

  return { allowed, loading, error };
}

/**
 * 認可キャッシュのエクスポート（テスト用）
 */
export { authCache };
