'use client';

import Link from 'next/link';
import { VendorApplication } from '@/types/vendor';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ProtectedButton } from '@/components/ProtectedButton';
import { formatDate, truncate } from '@/lib/utils';

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
 * VendorCardコンポーネントのプロパティ
 */
export interface VendorCardProps {
  vendor: VendorApplication;
  /** 編集権限があるか */
  canEdit: boolean;
  /** 削除権限があるか */
  canDelete: boolean;
  /** 承認権限があるか */
  canApprove: boolean;
}

/**
 * 取引先申請カードコンポーネント
 *
 * 取引先申請の概要を表示するカードです。
 * - ステータスバッジ
 * - 会社情報の表示
 * - 認可ベースのアクションボタン
 *
 * @example
 * ```tsx
 * <VendorCard
 *   vendor={vendorData}
 *   canEdit={true}
 *   canDelete={false}
 *   canApprove={false}
 * />
 * ```
 */
export function VendorCard({ vendor, canEdit, canDelete, canApprove }: VendorCardProps) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <CardTitle className="text-xl">
              <Link
                href={`/vendors/${vendor.id}`}
                className="hover:underline"
              >
                {vendor.companyName}
              </Link>
            </CardTitle>
            <CardDescription>
              登録番号: {vendor.registrationNumber}
            </CardDescription>
          </div>
          <Badge
            variant="outline"
            className={STATUS_STYLES[vendor.status]}
          >
            {STATUS_LABELS[vendor.status]}
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {/* 基本情報 */}
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-muted-foreground">担当者</p>
              <p className="font-medium">{vendor.contactName}</p>
            </div>
            <div>
              <p className="text-muted-foreground">電話番号</p>
              <p className="font-medium">{vendor.contactPhone}</p>
            </div>
            <div>
              <p className="text-muted-foreground">メールアドレス</p>
              <p className="font-medium">{vendor.contactEmail}</p>
            </div>
            <div>
              <p className="text-muted-foreground">住所</p>
              <p className="font-medium">{truncate(vendor.address, 30)}</p>
            </div>
          </div>

          {/* 申請情報 */}
          <div className="border-t pt-4">
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-muted-foreground">申請者</p>
                <p className="font-medium">{vendor.submittedBy}</p>
              </div>
              <div>
                <p className="text-muted-foreground">申請日</p>
                <p className="font-medium">{formatDate(vendor.submittedAt)}</p>
              </div>
              {vendor.reviewedBy && (
                <>
                  <div>
                    <p className="text-muted-foreground">承認者</p>
                    <p className="font-medium">{vendor.reviewedBy}</p>
                  </div>
                  <div>
                    <p className="text-muted-foreground">承認日</p>
                    <p className="font-medium">{formatDate(vendor.reviewedAt)}</p>
                  </div>
                </>
              )}
            </div>
          </div>

          {/* アクションボタン */}
          <div className="flex flex-wrap gap-2 border-t pt-4">
            <Link href={`/vendors/${vendor.id}`}>
              <Button variant="outline" size="sm">
                詳細を見る
              </Button>
            </Link>

            {canEdit && (
              <Link href={`/vendors/${vendor.id}/edit`}>
                <Button variant="outline" size="sm">
                  編集
                </Button>
              </Link>
            )}

            {canApprove && vendor.status === 'pending_approval' && (
              <Link href={`/vendors/${vendor.id}#approval`}>
                <Button variant="default" size="sm">
                  承認/却下
                </Button>
              </Link>
            )}

            {canDelete && (
              <ProtectedButton
                action="delete"
                resourceType="vendor"
                resourceId={vendor.id}
                context={{ status: vendor.status, ownerId: vendor.submittedBy }}
                variant="outline"
                size="sm"
                className="text-destructive hover:text-destructive"
                onClick={() => {
                  if (confirm(`「${vendor.companyName}」を削除しますか？`)) {
                    // 削除処理は後のタスクで実装
                    alert('削除機能は後のタスクで実装されます');
                  }
                }}
              >
                削除
              </ProtectedButton>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
