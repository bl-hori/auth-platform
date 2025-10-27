'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useUser } from '@/lib/session-context';
import { VendorApplication } from '@/types/vendor';
import { getVendorById } from '@/lib/mock-data';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ProtectedButton } from '@/components/ProtectedButton';
import { ApprovalActions } from '@/components/ApprovalActions';
import { VendorDetailSkeleton } from '@/components/loading-skeleton';
import { ErrorDisplay } from '@/components/error-boundary';
import { formatDate } from '@/lib/utils';
import { checkAuthorization } from '@/lib/authorization';
import { deleteVendorAction, approveVendorAction, rejectVendorAction } from '../actions';

/**
 * ステータスバッジのスタイル
 */
const STATUS_STYLES = {
  draft: 'bg-gray-100 text-gray-800 border-gray-300',
  pending_approval: 'bg-yellow-100 text-yellow-800 border-yellow-300',
  approved: 'bg-green-100 text-green-800 border-green-300',
  rejected: 'bg-red-100 text-red-800 border-red-300',
} as const;

/**
 * ステータスラベル
 */
const STATUS_LABELS = {
  draft: '下書き',
  pending_approval: '承認待ち',
  approved: '承認済み',
  rejected: '却下',
} as const;

/**
 * 取引先詳細ページ
 *
 * 取引先申請の詳細を表示します。
 * - 認可チェック
 * - 全ての情報を読み取り専用で表示
 * - ステータス履歴と承認コメント
 * - 認可ベースのアクションボタン
 */
export default function VendorDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const router = useRouter();
  const user = useUser();
  const [vendorId, setVendorId] = useState<string | null>(null);
  const [vendor, setVendor] = useState<VendorApplication | null>(null);
  const [loading, setLoading] = useState(true);
  const [canEdit, setCanEdit] = useState(false);
  const [canDelete, setCanDelete] = useState(false);
  const [canApprove, setCanApprove] = useState(false);
  const [authorized, setAuthorized] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

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

      // 閲覧権限をチェック
      const viewResponse = await checkAuthorization(
        'read',
        'vendor',
        vendorData.id,
        { ownerId: vendorData.submittedBy }
      );

      if (viewResponse.decision !== 'ALLOW') {
        setAuthorized(false);
        setLoading(false);
        return;
      }

      setAuthorized(true);

      // 各アクションの権限をチェック
      const [editResponse, deleteResponse, approveResponse] = await Promise.all([
        checkAuthorization('update', 'vendor', vendorData.id, {
          status: vendorData.status,
          ownerId: vendorData.submittedBy,
        }),
        checkAuthorization('delete', 'vendor', vendorData.id, {
          status: vendorData.status,
          ownerId: vendorData.submittedBy,
        }),
        checkAuthorization('approve', 'vendor', vendorData.id, {
          status: vendorData.status,
        }),
      ]);

      setCanEdit(editResponse.decision === 'ALLOW');
      setCanDelete(deleteResponse.decision === 'ALLOW');
      setCanApprove(approveResponse.decision === 'ALLOW');
    } catch (error) {
      console.error('Failed to load vendor:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 削除処理
   */
  const handleDelete = async () => {
    if (!vendor || !vendorId) return;

    if (!confirm(`「${vendor.companyName}」を削除しますか？\nこの操作は取り消せません。`)) {
      return;
    }

    setDeleting(true);
    try {
      const result = await deleteVendorAction(vendorId);

      if (result.success) {
        // 一覧ページにリダイレクト
        router.push('/vendors');
      } else {
        alert(result.error || '削除に失敗しました');
      }
    } catch (error) {
      console.error('Delete error:', error);
      alert('予期しないエラーが発生しました');
    } finally {
      setDeleting(false);
    }
  };

  /**
   * 承認処理
   */
  const handleApprove = async (comment: string) => {
    if (!vendorId) return;

    setError(null);
    setSuccess(null);

    try {
      const result = await approveVendorAction(vendorId, comment);

      if (result.success && result.data) {
        setSuccess('取引先申請を承認しました');
        // データを再読み込み
        await loadVendor();
      } else {
        setError(result.error || '承認に失敗しました');
      }
    } catch (err) {
      console.error('Approve error:', err);
      setError('予期しないエラーが発生しました');
    }
  };

  /**
   * 却下処理
   */
  const handleReject = async (comment: string) => {
    if (!vendorId) return;

    setError(null);
    setSuccess(null);

    try {
      const result = await rejectVendorAction(vendorId, comment);

      if (result.success && result.data) {
        setSuccess('取引先申請を却下しました');
        // データを再読み込み
        await loadVendor();
      } else {
        setError(result.error || '却下に失敗しました');
      }
    } catch (err) {
      console.error('Reject error:', err);
      setError('予期しないエラーが発生しました');
    }
  };

  // 未ログイン
  if (!user) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-2xl text-center">
          <h1 className="text-3xl font-bold">取引先詳細</h1>
          <p className="mt-4 text-muted-foreground">
            取引先詳細を表示するにはログインが必要です。
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
    return <VendorDetailSkeleton />;
  }

  // エラー発生時
  if (error) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-4xl">
          <ErrorDisplay
            error={error}
            onRetry={() => window.location.reload()}
            title="取引先情報の読み込みエラー"
          />
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

  // 閲覧権限なし
  if (!authorized) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-2xl text-center">
          <h1 className="text-3xl font-bold">アクセス拒否</h1>
          <p className="mt-4 text-muted-foreground">
            この取引先申請を閲覧する権限がありません。
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
        <div className="mb-8 flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-3xl font-bold">{vendor.companyName}</h1>
              <Badge variant="outline" className={STATUS_STYLES[vendor.status]}>
                {STATUS_LABELS[vendor.status]}
              </Badge>
            </div>
            <p className="mt-2 text-muted-foreground">
              登録番号: {vendor.registrationNumber}
            </p>
          </div>
          <Link href="/vendors">
            <Button variant="outline">一覧に戻る</Button>
          </Link>
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

        {/* アクションボタン */}
        <div className="mb-6 flex flex-wrap gap-3">
          {canEdit && (
            <Link href={`/vendors/${vendor.id}/edit`}>
              <Button variant="default">編集</Button>
            </Link>
          )}

          {canApprove && vendor.status === 'pending_approval' && (
            <a href="#approval">
              <Button variant="default">承認/却下</Button>
            </a>
          )}

          {canDelete && (
            <ProtectedButton
              action="delete"
              resourceType="vendor"
              resourceId={vendor.id}
              context={{ status: vendor.status, ownerId: vendor.submittedBy }}
              variant="outline"
              className="text-destructive hover:text-destructive"
              onClick={handleDelete}
              disabled={deleting}
            >
              {deleting ? '削除中...' : '削除'}
            </ProtectedButton>
          )}
        </div>

        {/* 会社情報 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>会社情報</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-6">
              <div>
                <p className="text-sm text-muted-foreground">会社名</p>
                <p className="mt-1 font-medium">{vendor.companyName}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">法人番号</p>
                <p className="mt-1 font-medium">{vendor.registrationNumber}</p>
              </div>
              <div className="col-span-2">
                <p className="text-sm text-muted-foreground">住所</p>
                <p className="mt-1 font-medium">{vendor.address}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">事業カテゴリー</p>
                <p className="mt-1 font-medium">{vendor.businessCategory}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 担当者情報 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>担当者情報</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-6">
              <div>
                <p className="text-sm text-muted-foreground">担当者名</p>
                <p className="mt-1 font-medium">{vendor.contactName}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">メールアドレス</p>
                <p className="mt-1 font-medium">{vendor.contactEmail}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">電話番号</p>
                <p className="mt-1 font-medium">{vendor.contactPhone}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 申請情報 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>申請情報</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-6">
              <div>
                <p className="text-sm text-muted-foreground">申請者</p>
                <p className="mt-1 font-medium">{vendor.submittedBy}</p>
              </div>
              {vendor.submittedAt && (
                <div>
                  <p className="text-sm text-muted-foreground">申請日時</p>
                  <p className="mt-1 font-medium">{formatDate(vendor.submittedAt)}</p>
                </div>
              )}
              <div>
                <p className="text-sm text-muted-foreground">作成日時</p>
                <p className="mt-1 font-medium">{formatDate(vendor.createdAt)}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">更新日時</p>
                <p className="mt-1 font-medium">{formatDate(vendor.updatedAt)}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 承認アクション（承認待ちの場合のみ表示） */}
        {vendor.status === 'pending_approval' && (
          <div className="mb-6">
            <ApprovalActions
              vendorId={vendor.id}
              vendorName={vendor.companyName}
              status={vendor.status}
              onApprove={handleApprove}
              onReject={handleReject}
            />
          </div>
        )}

        {/* 承認情報（承認済みまたは却下の場合のみ表示） */}
        {(vendor.status === 'approved' || vendor.status === 'rejected') && (
          <Card>
            <CardHeader>
              <CardTitle>
                {vendor.status === 'approved' ? '承認情報' : '却下情報'}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-6">
                <div className="grid grid-cols-2 gap-6">
                  <div>
                    <p className="text-sm text-muted-foreground">
                      {vendor.status === 'approved' ? '承認者' : '却下者'}
                    </p>
                    <p className="mt-1 font-medium">{vendor.reviewedBy || '-'}</p>
                  </div>
                  {vendor.reviewedAt && (
                    <div>
                      <p className="text-sm text-muted-foreground">
                        {vendor.status === 'approved' ? '承認日時' : '却下日時'}
                      </p>
                      <p className="mt-1 font-medium">{formatDate(vendor.reviewedAt)}</p>
                    </div>
                  )}
                </div>
                {vendor.reviewComment && (
                  <div>
                    <p className="text-sm text-muted-foreground">コメント</p>
                    <div className="mt-2 rounded-md bg-muted p-4">
                      <p className="text-sm">{vendor.reviewComment}</p>
                    </div>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
