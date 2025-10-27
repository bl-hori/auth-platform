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
 * 取引先を更新するServer Action
 *
 * @param id - 取引先ID
 * @param request - 取引先更新リクエスト
 * @returns 更新された取引先
 */
export async function updateVendorAction(
  id: string,
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

    // 既存の取引先を取得
    const existingVendor = mockDataStore.getById(id);
    if (!existingVendor) {
      return {
        success: false,
        error: '取引先が見つかりません',
      };
    }

    // 認可チェック
    const authResponse = await authClient.authorize({
      userId: user.id,
      action: 'update',
      resourceType: 'vendor',
      resourceId: id,
      context: {
        status: existingVendor.status,
        ownerId: existingVendor.submittedBy,
      },
    });

    if (authResponse.decision !== 'ALLOW') {
      return {
        success: false,
        error: '取引先を更新する権限がありません',
      };
    }

    // 取引先を更新
    const updatedVendor = mockDataStore.update(id, {
      ...request,
      updatedAt: new Date(),
    });

    if (!updatedVendor) {
      return {
        success: false,
        error: '取引先の更新に失敗しました',
      };
    }

    // キャッシュを無効化
    revalidatePath('/vendors');
    revalidatePath(`/vendors/${id}`);

    return {
      success: true,
      data: updatedVendor,
    };
  } catch (error) {
    console.error('Failed to update vendor:', error);
    return {
      success: false,
      error: '取引先の更新に失敗しました',
    };
  }
}

/**
 * 取引先を削除するServer Action
 *
 * @param id - 取引先ID
 * @returns 削除結果
 */
export async function deleteVendorAction(
  id: string
): Promise<ActionResult<void>> {
  try {
    // ユーザーセッションを取得
    const user = getCurrentUser();
    if (!user) {
      return {
        success: false,
        error: 'ログインが必要です',
      };
    }

    // 既存の取引先を取得
    const existingVendor = mockDataStore.getById(id);
    if (!existingVendor) {
      return {
        success: false,
        error: '取引先が見つかりません',
      };
    }

    // 認可チェック
    const authResponse = await authClient.authorize({
      userId: user.id,
      action: 'delete',
      resourceType: 'vendor',
      resourceId: id,
      context: {
        status: existingVendor.status,
        ownerId: existingVendor.submittedBy,
      },
    });

    if (authResponse.decision !== 'ALLOW') {
      return {
        success: false,
        error: '取引先を削除する権限がありません',
      };
    }

    // 取引先を削除
    const deleted = mockDataStore.delete(id);

    if (!deleted) {
      return {
        success: false,
        error: '取引先の削除に失敗しました',
      };
    }

    // キャッシュを無効化
    revalidatePath('/vendors');

    return {
      success: true,
    };
  } catch (error) {
    console.error('Failed to delete vendor:', error);
    return {
      success: false,
      error: '取引先の削除に失敗しました',
    };
  }
}

/**
 * 取引先を申請するServer Action
 *
 * @param id - 取引先ID
 * @returns 申請結果
 */
export async function submitVendorAction(
  id: string
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

    // 既存の取引先を取得
    const existingVendor = mockDataStore.getById(id);
    if (!existingVendor) {
      return {
        success: false,
        error: '取引先が見つかりません',
      };
    }

    // ステータスチェック
    if (existingVendor.status !== 'draft') {
      return {
        success: false,
        error: '下書き状態の申請のみ提出できます',
      };
    }

    // 認可チェック
    const authResponse = await authClient.authorize({
      userId: user.id,
      action: 'submit',
      resourceType: 'vendor',
      resourceId: id,
      context: {
        status: existingVendor.status,
        ownerId: existingVendor.submittedBy,
      },
    });

    if (authResponse.decision !== 'ALLOW') {
      return {
        success: false,
        error: '取引先を申請する権限がありません',
      };
    }

    // ステータスを更新
    const submittedVendor = mockDataStore.update(id, {
      status: 'pending_approval',
      submittedAt: new Date(),
      updatedAt: new Date(),
    });

    if (!submittedVendor) {
      return {
        success: false,
        error: '取引先の申請に失敗しました',
      };
    }

    // キャッシュを無効化
    revalidatePath('/vendors');
    revalidatePath(`/vendors/${id}`);

    return {
      success: true,
      data: submittedVendor,
    };
  } catch (error) {
    console.error('Failed to submit vendor:', error);
    return {
      success: false,
      error: '取引先の申請に失敗しました',
    };
  }
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
