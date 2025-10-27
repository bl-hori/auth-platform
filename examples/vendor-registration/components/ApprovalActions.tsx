'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { usePermission } from '@/lib/authorization';

/**
 * ApprovalActionsコンポーネントのプロパティ
 */
export interface ApprovalActionsProps {
  /** 取引先ID */
  vendorId: string;
  /** 取引先名 */
  vendorName: string;
  /** 取引先ステータス */
  status: string;
  /** 承認処理のコールバック */
  onApprove: (comment: string) => Promise<void>;
  /** 却下処理のコールバック */
  onReject: (comment: string) => Promise<void>;
}

/**
 * 承認アクションコンポーネント
 *
 * 取引先申請の承認/却下を行うコンポーネントです。
 * - 承認権限のチェック
 * - コメント入力フィールド
 * - 承認/却下ボタン
 */
export function ApprovalActions({
  vendorId,
  vendorName,
  status,
  onApprove,
  onReject,
}: ApprovalActionsProps) {
  const [comment, setComment] = useState('');
  const [approving, setApproving] = useState(false);
  const [rejecting, setRejecting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 承認権限をチェック
  const { allowed: canApprove, loading: authLoading } = usePermission(
    'approve',
    'vendor',
    vendorId,
    { status }
  );

  /**
   * 承認処理
   */
  const handleApprove = async () => {
    if (!confirm(`「${vendorName}」を承認しますか？`)) {
      return;
    }

    setError(null);
    setApproving(true);
    try {
      await onApprove(comment);
      // 成功時は親コンポーネントがリダイレクトまたはリロードを行う
    } catch (err) {
      console.error('Approve error:', err);
      setError('承認処理に失敗しました');
    } finally {
      setApproving(false);
    }
  };

  /**
   * 却下処理
   */
  const handleReject = async () => {
    // 却下時はコメント必須
    if (!comment.trim()) {
      setError('却下理由を入力してください');
      return;
    }

    if (!confirm(`「${vendorName}」を却下しますか？\nこの操作は取り消せません。`)) {
      return;
    }

    setError(null);
    setRejecting(true);
    try {
      await onReject(comment);
      // 成功時は親コンポーネントがリダイレクトまたはリロードを行う
    } catch (err) {
      console.error('Reject error:', err);
      setError('却下処理に失敗しました');
    } finally {
      setRejecting(false);
    }
  };

  // 承認待ち状態でない場合は表示しない
  if (status !== 'pending_approval') {
    return null;
  }

  // 権限チェック中
  if (authLoading) {
    return (
      <Card id="approval">
        <CardHeader>
          <CardTitle>承認/却下</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center text-muted-foreground">読み込み中...</div>
        </CardContent>
      </Card>
    );
  }

  // 承認権限なし
  if (!canApprove) {
    return (
      <Card id="approval">
        <CardHeader>
          <CardTitle>承認/却下</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center text-muted-foreground">
            この申請を承認/却下する権限がありません
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card id="approval">
      <CardHeader>
        <CardTitle>承認/却下</CardTitle>
        <CardDescription>
          この申請を承認または却下してください。却下する場合は理由を必ず入力してください。
        </CardDescription>
      </CardHeader>
      <CardContent>
        {/* エラーメッセージ */}
        {error && (
          <div className="mb-4 rounded-md bg-destructive/10 p-4 text-sm text-destructive">
            {error}
          </div>
        )}

        {/* コメント入力 */}
        <div className="mb-6 space-y-2">
          <Label htmlFor="reviewComment">
            コメント
            <span className="ml-2 text-xs text-muted-foreground">
              (却下時は必須)
            </span>
          </Label>
          <textarea
            id="reviewComment"
            className="min-h-[120px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="承認または却下の理由、コメントを入力してください"
            disabled={approving || rejecting}
          />
        </div>

        {/* アクションボタン */}
        <div className="flex gap-4">
          <Button
            onClick={handleApprove}
            disabled={approving || rejecting}
            className="flex-1 bg-green-600 hover:bg-green-700"
          >
            {approving ? '承認中...' : '承認'}
          </Button>
          <Button
            onClick={handleReject}
            disabled={approving || rejecting}
            variant="outline"
            className="flex-1 border-destructive text-destructive hover:bg-destructive hover:text-destructive-foreground"
          >
            {rejecting ? '却下中...' : '却下'}
          </Button>
        </div>

        {/* ヘルプテキスト */}
        <div className="mt-6 rounded-md border p-4 text-sm text-muted-foreground">
          <p className="font-medium">承認/却下に関する注意事項:</p>
          <ul className="mt-2 list-inside list-disc space-y-1">
            <li>承認すると申請者は取引先として登録されます</li>
            <li>却下する場合は必ず理由を入力してください</li>
            <li>承認/却下後は操作を取り消せません</li>
            <li>申請者には結果が通知されます（本実装では未実装）</li>
          </ul>
        </div>
      </CardContent>
    </Card>
  );
}
