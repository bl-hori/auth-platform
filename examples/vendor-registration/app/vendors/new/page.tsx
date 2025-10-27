'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useUser } from '@/lib/session-context';
import { usePermission } from '@/lib/authorization';
import { VendorForm } from '@/components/vendor-form';
import { Button } from '@/components/ui/button';
import { createVendorAction } from '../actions';
import { CreateVendorRequest } from '@/types/vendor';

/**
 * 取引先新規作成ページ
 *
 * 新しい取引先申請を作成します。
 * - 申請者と管理者のみアクセス可能
 * - 承認者はアクセス不可
 * - 認可チェック付き
 */
export default function NewVendorPage() {
  const router = useRouter();
  const user = useUser();
  const { allowed, loading: permissionLoading } = usePermission('create', 'vendor');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  /**
   * フォーム送信処理
   */
  const handleSubmit = async (data: CreateVendorRequest) => {
    setError(null);
    setSuccess(null);

    try {
      const result = await createVendorAction(data);

      if (result.success && result.data) {
        setSuccess('取引先申請を作成しました');
        // 1秒後に一覧ページにリダイレクト
        setTimeout(() => {
          router.push('/vendors');
        }, 1000);
      } else {
        setError(result.error || '取引先の作成に失敗しました');
      }
    } catch (err) {
      console.error('Submit error:', err);
      setError('予期しないエラーが発生しました');
    }
  };

  /**
   * キャンセル処理
   */
  const handleCancel = () => {
    router.push('/vendors');
  };

  // 未ログイン
  if (!user) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-2xl text-center">
          <h1 className="text-3xl font-bold">取引先申請の作成</h1>
          <p className="mt-4 text-muted-foreground">
            取引先申請を作成するにはログインが必要です。
          </p>
          <div className="mt-8">
            <Link href="/login">
              <Button>ログイン</Button>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // 権限チェック中
  if (permissionLoading) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-4xl">
          <h1 className="text-3xl font-bold">取引先申請の作成</h1>
          <div className="mt-8 text-center text-muted-foreground">
            読み込み中...
          </div>
        </div>
      </div>
    );
  }

  // 権限なし
  if (!allowed) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-2xl text-center">
          <h1 className="text-3xl font-bold">アクセス拒否</h1>
          <p className="mt-4 text-muted-foreground">
            取引先申請を作成する権限がありません。
          </p>
          <p className="mt-2 text-sm text-muted-foreground">
            申請者または管理者のみが取引先を作成できます。
          </p>
          <div className="mt-8">
            <Link href="/vendors">
              <Button variant="outline">一覧に戻る</Button>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container py-12">
      <div className="mx-auto max-w-4xl">
        {/* ヘッダー */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold">取引先申請の作成</h1>
          <p className="mt-2 text-muted-foreground">
            新しい取引先の情報を入力してください
          </p>
        </div>

        {/* 成功メッセージ */}
        {success && (
          <div className="mb-6 rounded-md bg-green-50 p-4 text-sm text-green-800">
            {success}
          </div>
        )}

        {/* エラーメッセージ */}
        {error && (
          <div className="mb-6 rounded-md bg-destructive/10 p-4 text-sm text-destructive">
            {error}
          </div>
        )}

        {/* フォーム */}
        <VendorForm
          onSubmit={handleSubmit}
          submitLabel="作成"
          onCancel={handleCancel}
        />

        {/* ヘルプテキスト */}
        <div className="mt-6 rounded-md border p-4 text-sm text-muted-foreground">
          <p className="font-medium">注意事項:</p>
          <ul className="mt-2 list-inside list-disc space-y-1">
            <li>作成直後は「下書き」ステータスになります</li>
            <li>全ての必須項目を入力する必要があります</li>
            <li>作成後に編集が可能です</li>
            <li>承認を依頼する場合は、編集画面から「申請」を行ってください</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
