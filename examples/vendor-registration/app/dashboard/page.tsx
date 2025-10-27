'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useSession } from '@/lib/session-context';
import { getMockVendors } from '@/lib/mock-data';
import { type VendorApplication } from '@/types/vendor';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { DashboardSkeleton } from '@/components/loading-skeleton';
import { ErrorDisplay } from '@/components/error-boundary';
import { formatDate } from '@/lib/utils';

/**
 * ステータスラベル
 */
const STATUS_LABELS: Record<string, string> = {
  draft: '下書き',
  pending_approval: '承認待ち',
  approved: '承認済み',
  rejected: '却下',
};

/**
 * ステータスバッジのスタイル
 */
const STATUS_STYLES: Record<string, string> = {
  draft: 'bg-gray-100 text-gray-800',
  pending_approval: 'bg-yellow-100 text-yellow-800',
  approved: 'bg-green-100 text-green-800',
  rejected: 'bg-red-100 text-red-800',
};

/**
 * ダッシュボード統計情報
 */
interface DashboardStats {
  total: number;
  draft: number;
  pending: number;
  approved: number;
  rejected: number;
}

/**
 * ダッシュボードページ
 *
 * ユーザーの役割に応じて異なる情報を表示します:
 * - 申請者（applicant）: 自分の申請一覧と統計
 * - 承認者（approver）: 承認待ちの申請一覧
 * - 管理者（admin）: 全ての申請一覧と統計
 */
export default function DashboardPage() {
  const { user, loading: sessionLoading } = useSession();
  const [vendors, setVendors] = useState<VendorApplication[]>([]);
  const [stats, setStats] = useState<DashboardStats>({
    total: 0,
    draft: 0,
    pending: 0,
    approved: 0,
    rejected: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadDashboard = async () => {
      if (!user) {
        setLoading(false);
        return;
      }

      try {
        setError(null);
        const allVendors = getMockVendors();
        let filteredVendors: VendorApplication[] = [];

        // 役割に応じてフィルタリング
        if (user.role === 'applicant') {
          // 申請者: 自分の申請のみ
          filteredVendors = allVendors.filter(v => v.submittedBy === user.id);
        } else if (user.role === 'approver') {
          // 承認者: 承認待ちの申請のみ
          filteredVendors = allVendors.filter(v => v.status === 'pending_approval');
        } else if (user.role === 'admin') {
          // 管理者: 全ての申請
          filteredVendors = allVendors;
        }

        // 統計情報を計算
        const newStats: DashboardStats = {
          total: filteredVendors.length,
          draft: filteredVendors.filter(v => v.status === 'draft').length,
          pending: filteredVendors.filter(v => v.status === 'pending_approval').length,
          approved: filteredVendors.filter(v => v.status === 'approved').length,
          rejected: filteredVendors.filter(v => v.status === 'rejected').length,
        };

        setVendors(filteredVendors);
        setStats(newStats);
      } catch (err) {
        console.error('Failed to load dashboard:', err);
        setError(err instanceof Error ? err.message : 'ダッシュボードの読み込みに失敗しました');
      } finally {
        setLoading(false);
      }
    };

    if (!sessionLoading) {
      loadDashboard();
    }
  }, [user, sessionLoading]);

  // 未ログイン
  if (!sessionLoading && !user) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-2xl text-center">
          <h1 className="text-3xl font-bold">ダッシュボード</h1>
          <p className="mt-4 text-muted-foreground">
            ダッシュボードを表示するにはログインしてください
          </p>
          <div className="mt-6">
            <Link href="/login">
              <Button>ログイン</Button>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // ローディング中
  if (loading || sessionLoading) {
    return <DashboardSkeleton />;
  }

  // エラー発生時
  if (error) {
    return (
      <div className="container py-12">
        <div className="mx-auto max-w-6xl">
          <ErrorDisplay
            error={error}
            onRetry={() => window.location.reload()}
            title="ダッシュボードの読み込みエラー"
          />
        </div>
      </div>
    );
  }

  return (
    <div className="container py-12">
      <div className="mx-auto max-w-6xl">
        {/* ヘッダー */}
        <div className="mb-8 flex items-start justify-between">
          <div>
            <h1 className="text-3xl font-bold">ダッシュボード</h1>
            <p className="mt-2 text-muted-foreground">
              {user?.role === 'applicant' && 'あなたの申請状況を確認できます'}
              {user?.role === 'approver' && '承認待ちの申請を確認できます'}
              {user?.role === 'admin' && '全ての申請を管理できます'}
            </p>
          </div>
          {user?.role === 'applicant' && (
            <Link href="/vendors/new">
              <Button>新規申請</Button>
            </Link>
          )}
        </div>

        {/* 統計カード */}
        <div className="mb-8 grid gap-4 md:grid-cols-2 lg:grid-cols-5">
          <Card>
            <CardHeader className="pb-2">
              <CardDescription>合計</CardDescription>
              <CardTitle className="text-3xl">{stats.total}</CardTitle>
            </CardHeader>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardDescription>下書き</CardDescription>
              <CardTitle className="text-3xl">{stats.draft}</CardTitle>
            </CardHeader>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardDescription>承認待ち</CardDescription>
              <CardTitle className="text-3xl">{stats.pending}</CardTitle>
            </CardHeader>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardDescription>承認済み</CardDescription>
              <CardTitle className="text-3xl">{stats.approved}</CardTitle>
            </CardHeader>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardDescription>却下</CardDescription>
              <CardTitle className="text-3xl">{stats.rejected}</CardTitle>
            </CardHeader>
          </Card>
        </div>

        {/* 申請一覧 */}
        <Card>
          <CardHeader>
            <CardTitle>
              {user?.role === 'applicant' && 'あなたの申請'}
              {user?.role === 'approver' && '承認待ちの申請'}
              {user?.role === 'admin' && '全ての申請'}
            </CardTitle>
            <CardDescription>
              {vendors.length}件の申請があります
            </CardDescription>
          </CardHeader>
          <CardContent>
            {vendors.length === 0 ? (
              <div className="text-center text-muted-foreground">
                申請がありません
              </div>
            ) : (
              <div className="space-y-4">
                {vendors.map((vendor) => (
                  <div
                    key={vendor.id}
                    className="flex items-center justify-between rounded-lg border p-4 hover:bg-muted/50"
                  >
                    <div className="flex-1">
                      <div className="flex items-center gap-3">
                        <h3 className="font-semibold">{vendor.companyName}</h3>
                        <Badge
                          variant="outline"
                          className={STATUS_STYLES[vendor.status]}
                        >
                          {STATUS_LABELS[vendor.status]}
                        </Badge>
                      </div>
                      <p className="mt-1 text-sm text-muted-foreground">
                        登録番号: {vendor.registrationNumber}
                      </p>
                      {vendor.submittedAt && (
                        <p className="mt-1 text-sm text-muted-foreground">
                          申請日: {formatDate(vendor.submittedAt)}
                        </p>
                      )}
                    </div>
                    <div className="flex gap-2">
                      <Link href={`/vendors/${vendor.id}`}>
                        <Button variant="outline" size="sm">
                          詳細
                        </Button>
                      </Link>
                      {user?.role === 'applicant' &&
                        vendor.status === 'draft' && (
                          <Link href={`/vendors/${vendor.id}/edit`}>
                            <Button variant="default" size="sm">
                              編集
                            </Button>
                          </Link>
                        )}
                      {(user?.role === 'approver' || user?.role === 'admin') &&
                        vendor.status === 'pending_approval' && (
                          <Link href={`/vendors/${vendor.id}#approval`}>
                            <Button variant="default" size="sm">
                              承認/却下
                            </Button>
                          </Link>
                        )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* クイックリンク */}
        <div className="mt-8">
          <Card>
            <CardHeader>
              <CardTitle>クイックリンク</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid gap-2 md:grid-cols-3">
                <Link href="/vendors">
                  <Button variant="outline" className="w-full">
                    全ての取引先を見る
                  </Button>
                </Link>
                {user?.role === 'applicant' && (
                  <Link href="/vendors/new">
                    <Button variant="outline" className="w-full">
                      新規申請
                    </Button>
                  </Link>
                )}
                {(user?.role === 'approver' || user?.role === 'admin') && (
                  <Link href="/vendors?status=pending_approval">
                    <Button variant="outline" className="w-full">
                      承認待ち一覧
                    </Button>
                  </Link>
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
