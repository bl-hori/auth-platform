'use server';

import { revalidatePath } from 'next/cache';
import { CreateVendorRequest, VendorApplication } from '@/types/vendor';
import { mockDataStore, generateVendorId } from '@/lib/mock-data';
import { getCurrentUser } from '@/lib/session';
import { authClient } from '@/lib/auth-client';

/**
 * Server Actionの結果
 */
export interface ActionResult<T = void> {
  success: boolean;
  data?: T;
  error?: string;
}

/**
 * 取引先を作成するServer Action
 *
 * @param request - 取引先作成リクエスト
 * @returns 作成された取引先
 */
export async function createVendorAction(
  request: CreateVendorRequest
): Promise<ActionResult<VendorApplication>> {
  try {
    // ユーザーセッションを取得
    const user = getCurrentUser();
    if (!user) {
      return {
        success: false,
        error: 'ログインが必要です',
      };
    }

    // 認可チェック
    const authResponse = await authClient.authorize({
      userId: user.id,
      action: 'create',
      resourceType: 'vendor',
      context: {},
    });

    if (authResponse.decision !== 'ALLOW') {
      return {
        success: false,
        error: '取引先を作成する権限がありません',
      };
    }

    // 取引先を作成
    const now = new Date();
    const vendor: VendorApplication = {
      id: generateVendorId(),
      ...request,
      status: 'draft',
      submittedBy: user.id,
      createdAt: now,
      updatedAt: now,
    };

    // モックデータストアに保存
    const createdVendor = mockDataStore.create(vendor);

    // キャッシュを無効化
    revalidatePath('/vendors');

    return {
      success: true,
      data: createdVendor,
    };
  } catch (error) {
    console.error('Failed to create vendor:', error);
    return {
      success: false,
      error: '取引先の作成に失敗しました',
    };
  }
}
