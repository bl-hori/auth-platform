/**
 * 取引先申請のステータス
 */
export type VendorStatus = 'draft' | 'pending_approval' | 'approved' | 'rejected';

/**
 * 取引先申請エンティティ
 */
export interface VendorApplication {
  /** 一意の識別子 */
  id: string;
  /** 会社名 */
  companyName: string;
  /** 法人番号 */
  registrationNumber: string;
  /** 住所 */
  address: string;
  /** 担当者名 */
  contactName: string;
  /** 担当者メールアドレス */
  contactEmail: string;
  /** 担当者電話番号 */
  contactPhone: string;
  /** 事業カテゴリー */
  businessCategory: string;
  /** 申請ステータス */
  status: VendorStatus;
  /** 申請者のユーザーID */
  submittedBy: string;
  /** 申請日時 */
  submittedAt?: Date;
  /** 承認者のユーザーID */
  reviewedBy?: string;
  /** 承認/却下日時 */
  reviewedAt?: Date;
  /** 承認/却下コメント */
  reviewComment?: string;
  /** 作成日時 */
  createdAt: Date;
  /** 更新日時 */
  updatedAt: Date;
}

/**
 * 取引先申請の作成リクエスト
 */
export interface CreateVendorRequest {
  companyName: string;
  registrationNumber: string;
  address: string;
  contactName: string;
  contactEmail: string;
  contactPhone: string;
  businessCategory: string;
}

/**
 * 取引先申請の更新リクエスト
 */
export interface UpdateVendorRequest extends Partial<CreateVendorRequest> {
  status?: VendorStatus;
  reviewComment?: string;
}

/**
 * ステータスバッジの色マッピング
 */
export const STATUS_BADGE_VARIANT: Record<
  VendorStatus,
  'default' | 'warning' | 'success' | 'destructive'
> = {
  draft: 'default',
  pending_approval: 'warning',
  approved: 'success',
  rejected: 'destructive',
};

/**
 * ステータスの日本語表示
 */
export const STATUS_LABELS: Record<VendorStatus, string> = {
  draft: '下書き',
  pending_approval: '承認待ち',
  approved: '承認済み',
  rejected: '却下',
};
