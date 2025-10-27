'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useUser } from '@/lib/session-context';
import { VendorApplication } from '@/types/vendor';
import { getMockVendors } from '@/lib/mock-data';
import { VendorCard } from '@/components/vendor-card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { authClient } from '@/lib/auth-client';
import { AuthorizationRequest } from '@/types/auth';

/**
 * 取引先一覧ページ
 *
 * ロールベースで取引先申請の一覧を表示します。
 * - 申請者: 自分が申請したもののみ表示
 * - 承認者: 全ての申請を表示
 * - 管理者: 全ての申請を表示
 *
 * バッチ認可を使用して、各取引先に対する権限を効率的にチェックします。
 */
export default function VendorsPage() {
  const user = useUser();
  const [vendors, setVendors] = useState<VendorApplication[]>([]);
  const [permissions, setPermissions] = useState<Map<string, { canEdit: boolean; canDelete: boolean; canApprove: boolean }>>(new Map());
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'all' | 'draft' | 'pending_approval' | 'approved' | 'rejected'>('all');

  useEffect(() => {
    if (!user) {
      setLoading(false);
      return;
    }

    loadVendors();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, filter]);

  /**
   * 取引先一覧を読み込む
   */
  const loadVendors = async () => {
    if (!user) return;

    setLoading(true);
    try {
      // モックデータから取引先を取得
      let allVendors = getMockVendors();

      // ロールベースのフィルタリング
      if (user.role === 'applicant') {
        // 申請者は自分のもののみ表示
        allVendors = allVendors.filter(v => v.submittedBy === user.name);
      }

      // ステータスフィルタリング
      if (filter !== 'all') {
        allVendors = allVendors.filter(v => v.status === filter);
      }

      setVendors(allVendors);

      // バッチ認可でアクション権限をチェック
      await checkBatchPermissions(allVendors);
    } catch (error) {
      console.error('Failed to load vendors:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * バッチ認可で各取引先に対する権限をチェック
   */
  const checkBatchPermissions = async (vendorList: VendorApplication[]) => {
    if (!user || vendorList.length === 0) return;

    try {
      // 各取引先に対する編集、削除、承認の権限をチェック
      const requests: AuthorizationRequest[] = [];

      vendorList.forEach(vendor => {
        // 編集権限
        requests.push({
          userId: user.id,
          action: 'update',
          resourceType: 'vendor',
          resourceId: vendor.id,
          context: {
            status: vendor.status,
            ownerId: vendor.submittedBy,
          },
        });

        // 削除権限
        requests.push({
          userId: user.id,
          action: 'delete',
          resourceType: 'vendor',
          resourceId: vendor.id,
          context: {
            status: vendor.status,
            ownerId: vendor.submittedBy,
          },
        });

        // 承認権限
        requests.push({
          userId: user.id,
          action: 'approve',
          resourceType: 'vendor',
          resourceId: vendor.id,
          context: {
            status: vendor.status,
          },
        });
      });

      // バッチ認可を実行
      const responses = await authClient.authorizeBatch(requests);

      // 結果をMapに格納
      const permissionsMap = new Map();
      vendorList.forEach((vendor, index) => {
        const baseIndex = index * 3;
        permissionsMap.set(vendor.id, {
          canEdit: responses[baseIndex]?.decision === 'ALLOW',
          canDelete: responses[baseIndex + 1]?.decision === 'ALLOW',
          canApprove: responses[baseIndex + 2]?.decision === 'ALLOW',
        });
      });

      setPermissions(permissionsMap);
    } catch (error) {
      console.error('Failed to check batch permissions:', error);
    }
  };

  // 未ログインの場合
  if (!user) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-2xl text-center">
          <h1 className="text-3xl font-bold">取引先一覧</h1>
          <p className="mt-4 text-muted-foreground">
            取引先一覧を表示するにはログインが必要です。
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
        <div className="mx-auto max-w-6xl">
          <div className="flex items-center justify-between">
            <h1 className="text-3xl font-bold">取引先一覧</h1>
          </div>
          <div className="mt-8 text-center text-muted-foreground">
            読み込み中...
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container py-12">
      <div className="mx-auto max-w-6xl">
        {/* ヘッダー */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">取引先一覧</h1>
            <p className="mt-2 text-muted-foreground">
              {user.role === 'applicant'
                ? 'あなたが申請した取引先の一覧です'
                : '全ての取引先申請の一覧です'}
            </p>
          </div>
          {(user.role === 'applicant' || user.role === 'admin') && (
            <Link href="/vendors/new">
              <Button>新規申請</Button>
            </Link>
          )}
        </div>

        {/* フィルター */}
        <div className="mt-8 flex flex-wrap gap-2">
          <Badge
            variant={filter === 'all' ? 'default' : 'outline'}
            className="cursor-pointer"
            onClick={() => setFilter('all')}
          >
            全て ({getMockVendors().filter(v => user.role === 'applicant' ? v.submittedBy === user.name : true).length})
          </Badge>
          <Badge
            variant={filter === 'draft' ? 'default' : 'outline'}
            className="cursor-pointer"
            onClick={() => setFilter('draft')}
          >
            下書き
          </Badge>
          <Badge
            variant={filter === 'pending_approval' ? 'default' : 'outline'}
            className="cursor-pointer"
            onClick={() => setFilter('pending_approval')}
          >
            承認待ち
          </Badge>
          <Badge
            variant={filter === 'approved' ? 'default' : 'outline'}
            className="cursor-pointer"
            onClick={() => setFilter('approved')}
          >
            承認済み
          </Badge>
          <Badge
            variant={filter === 'rejected' ? 'default' : 'outline'}
            className="cursor-pointer"
            onClick={() => setFilter('rejected')}
          >
            却下
          </Badge>
        </div>

        {/* 取引先リスト */}
        {vendors.length === 0 ? (
          <div className="mt-8 text-center">
            <p className="text-muted-foreground">
              {filter === 'all'
                ? '取引先申請がまだありません。'
                : 'このステータスの取引先申請はありません。'}
            </p>
            {user.role === 'applicant' && filter === 'all' && (
              <div className="mt-4">
                <Link href="/vendors/new">
                  <Button>最初の申請を作成</Button>
                </Link>
              </div>
            )}
          </div>
        ) : (
          <div className="mt-8 space-y-6">
            {vendors.map(vendor => {
              const vendorPermissions = permissions.get(vendor.id) || {
                canEdit: false,
                canDelete: false,
                canApprove: false,
              };

              return (
                <VendorCard
                  key={vendor.id}
                  vendor={vendor}
                  {...vendorPermissions}
                />
              );
            })}
          </div>
        )}

        {/* 統計情報 */}
        {vendors.length > 0 && (
          <div className="mt-8 text-sm text-muted-foreground">
            {vendors.length}件の取引先申請を表示中
          </div>
        )}
      </div>
    </div>
  );
}
