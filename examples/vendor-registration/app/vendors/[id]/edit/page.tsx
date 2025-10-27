'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useUser } from '@/lib/session-context';
import { VendorApplication } from '@/types/vendor';
import { getVendorById } from '@/lib/mock-data';
import { VendorForm } from '@/components/vendor-form';
import { Button } from '@/components/ui/button';
import { updateVendorAction, submitVendorAction } from '../../actions';
import { CreateVendorRequest } from '@/types/vendor';
import { checkAuthorization } from '@/lib/authorization';

/**
 * 取引先編集ページ
 *
 * 既存の取引先申請を編集します。
 * - 編集権限のチェック
 * - ステータスに基づく編集制限
 * - フォームの初期値設定
 */
export default function EditVendorPage({ params }: { params: Promise<{ id: string }> }) {
  const router = useRouter();
  const user = useUser();
  const [vendorId, setVendorId] = useState<string | null>(null);
  const [vendor, setVendor] = useState<VendorApplication | null>(null);
  const [loading, setLoading] = useState(true);
  const [authorized, setAuthorized] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    params.then((p) => setVendorId(p.id));
  }, [params]);

  useEffect(() => {
    if (!user || !vendorId) {
      setLoading(false);
      return;
    }

    loadVendor();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, vendorId]);

  /**
   * 取引先を読み込む
   */
  const loadVendor = async () => {
    if (!user || !vendorId) return;

    setLoading(true);
    try {
      // モックデータから取引先を取得
      const vendorData = getVendorById(vendorId);

      if (!vendorData) {
        setLoading(false);
        return;
      }

      setVendor(vendorData);

      // 編集権限をチェック
      const authResponse = await checkAuthorization(
        'update',
        'vendor',
        vendorData.id,
        {
          status: vendorData.status,
          ownerId: vendorData.submittedBy,
        }
      );

      setAuthorized(authResponse.decision === 'ALLOW');
    } catch (error) {
      console.error('Failed to load vendor:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * フォーム送信処理
   */
  const handleSubmit = async (data: CreateVendorRequest) => {
    if (!vendorId) return;

    setError(null);
    setSuccess(null);

    try {
      const result = await updateVendorAction(vendorId, data);

      if (result.success && result.data) {
        setSuccess('取引先申請を更新しました');
        // 1秒後に詳細ページにリダイレクト
        setTimeout(() => {
          router.push(`/vendors/${vendorId}`);
        }, 1000);
      } else {
        setError(result.error || '取引先の更新に失敗しました');
      }
    } catch (err) {
      console.error('Submit error:', err);
      setError('予期しないエラーが発生しました');
    }
  };

  /**
   * 申請処理
   */
  const handleSubmitForApproval = async () => {
    if (!vendorId || !vendor) return;

    if (!confirm(`「${vendor.companyName}」を承認申請しますか？`)) {
      return;
    }

    setSubmitting(true);
    setError(null);
    setSuccess(null);

    try {
      const result = await submitVendorAction(vendorId);

      if (result.success) {
        setSuccess('承認申請を提出しました');
        setTimeout(() => {
          router.push(`/vendors/${vendorId}`);
        }, 1000);
      } else {
        setError(result.error || '申請に失敗しました');
      }
    } catch (err) {
      console.error('Submit error:', err);
      setError('予期しないエラーが発生しました');
    } finally {
      setSubmitting(false);
    }
  };

  /**
   * キャンセル処理
   */
  const handleCancel = () => {
    if (vendorId) {
      router.push(`/vendors/${vendorId}`);
    }
  };

  // 未ログイン
  if (!user) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-2xl text-center">
          <h1 className="text-3xl font-bold">取引先申請の編集</h1>
          <p className="mt-4 text-muted-foreground">
            取引先申請を編集するにはログインが必要です。
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

  // ローディング中
  if (loading) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-4xl">
          <h1 className="text-3xl font-bold">取引先申請の編集</h1>
          <div className="mt-8 text-center text-muted-foreground">
            読み込み中...
          </div>
        </div>
      </div>
    );
  }

  // 取引先が見つからない
  if (!vendor) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-2xl text-center">
          <h1 className="text-3xl font-bold">取引先が見つかりません</h1>
          <p className="mt-4 text-muted-foreground">
            指定された取引先申請は存在しません。
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

  // 編集権限なし
  if (!authorized) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-2xl text-center">
          <h1 className="text-3xl font-bold">アクセス拒否</h1>
          <p className="mt-4 text-muted-foreground">
            この取引先申請を編集する権限がありません。
          </p>
          <p className="mt-2 text-sm text-muted-foreground">
            申請のオーナーのみが編集できます。
            また、承認済みまたは却下済みの申請は編集できません。
          </p>
          <div className="mt-8">
            <Link href={`/vendors/${vendorId}`}>
              <Button variant="outline">詳細に戻る</Button>
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
          <h1 className="text-3xl font-bold">取引先申請の編集</h1>
          <p className="mt-2 text-muted-foreground">
            {vendor.companyName} の情報を編集します
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

        {/* ステータス制限の警告 */}
        {vendor.status !== 'draft' && (
          <div className="mb-6 rounded-md border border-yellow-300 bg-yellow-50 p-4 text-sm text-yellow-800">
            <p className="font-medium">注意:</p>
            <p className="mt-1">
              この申請は「{vendor.status === 'pending_approval' ? '承認待ち' :
                         vendor.status === 'approved' ? '承認済み' : '却下'}」状態です。
              編集には制限がある場合があります。
            </p>
          </div>
        )}

        {/* フォーム */}
        <VendorForm
          initialValues={{
            companyName: vendor.companyName,
            registrationNumber: vendor.registrationNumber,
            address: vendor.address,
            contactName: vendor.contactName,
            contactEmail: vendor.contactEmail,
            contactPhone: vendor.contactPhone,
            businessCategory: vendor.businessCategory,
          }}
          onSubmit={handleSubmit}
          submitLabel="更新"
          onCancel={handleCancel}
        />

        {/* 承認申請ボタン（下書きの場合のみ） */}
        {vendor.status === 'draft' && (
          <div className="mt-6">
            <div className="rounded-md border border-blue-300 bg-blue-50 p-4">
              <p className="text-sm font-medium text-blue-900">承認申請</p>
              <p className="mt-1 text-sm text-blue-800">
                全ての情報を入力したら、承認申請を提出できます。
                申請後は限定的な編集のみ可能になります。
              </p>
              <Button
                onClick={handleSubmitForApproval}
                disabled={submitting}
                className="mt-3"
              >
                {submitting ? '申請中...' : '承認申請を提出'}
              </Button>
            </div>
          </div>
        )}

        {/* ヘルプテキスト */}
        <div className="mt-6 rounded-md border p-4 text-sm text-muted-foreground">
          <p className="font-medium">編集に関する注意事項:</p>
          <ul className="mt-2 list-inside list-disc space-y-1">
            <li>下書き状態の申請はいつでも編集できます</li>
            <li>承認待ちの申請は限定的な編集が可能です</li>
            <li>承認済みまたは却下済みの申請は編集できません</li>
            <li>他のユーザーの申請は編集できません</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
