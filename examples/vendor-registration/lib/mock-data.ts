import { User, UserRole } from '@/types/auth';
import { VendorApplication, VendorStatus } from '@/types/vendor';

/**
 * モックユーザーデータ
 * デモ用の3つの異なるロールのユーザー
 */
export const MOCK_USERS: User[] = [
  {
    id: 'user-001',
    name: '田中太郎',
    email: 'tanaka@example.com',
    role: 'applicant' as UserRole,
  },
  {
    id: 'user-002',
    name: '佐藤花子',
    email: 'sato@example.com',
    role: 'approver' as UserRole,
  },
  {
    id: 'user-003',
    name: '鈴木一郎',
    email: 'suzuki@example.com',
    role: 'admin' as UserRole,
  },
];

/**
 * ロール別にユーザーを取得
 */
export function getUserByRole(role: UserRole): User {
  return MOCK_USERS.find((user) => user.role === role) || MOCK_USERS[0];
}

/**
 * ユーザーIDでユーザーを取得
 */
export function getUserById(userId: string): User | undefined {
  return MOCK_USERS.find((user) => user.id === userId);
}

/**
 * モック取引先申請データ
 */
export const MOCK_VENDORS: VendorApplication[] = [
  {
    id: 'vendor-001',
    companyName: '株式会社サンプル商事',
    registrationNumber: '1234567890123',
    address: '東京都千代田区丸の内1-1-1',
    contactName: '山田太郎',
    contactEmail: 'yamada@sample-corp.co.jp',
    contactPhone: '03-1234-5678',
    businessCategory: '卸売業',
    status: 'approved' as VendorStatus,
    submittedBy: 'user-001',
    submittedAt: new Date('2024-01-15T10:00:00Z'),
    reviewedBy: 'user-002',
    reviewedAt: new Date('2024-01-16T14:30:00Z'),
    reviewComment: '問題なく承認いたします。',
    createdAt: new Date('2024-01-15T09:30:00Z'),
    updatedAt: new Date('2024-01-16T14:30:00Z'),
  },
  {
    id: 'vendor-002',
    companyName: '有限会社テスト物産',
    registrationNumber: '9876543210987',
    address: '大阪府大阪市北区梅田2-2-2',
    contactName: '佐藤次郎',
    contactEmail: 'sato@test-bussan.co.jp',
    contactPhone: '06-9876-5432',
    businessCategory: '小売業',
    status: 'pending_approval' as VendorStatus,
    submittedBy: 'user-001',
    submittedAt: new Date('2024-01-20T11:00:00Z'),
    createdAt: new Date('2024-01-20T10:45:00Z'),
    updatedAt: new Date('2024-01-20T11:00:00Z'),
  },
  {
    id: 'vendor-003',
    companyName: '合同会社デモ技研',
    registrationNumber: '5555555555555',
    address: '愛知県名古屋市中区栄3-3-3',
    contactName: '鈴木三郎',
    contactEmail: 'suzuki@demo-tech.co.jp',
    contactPhone: '052-1111-2222',
    businessCategory: '製造業',
    status: 'draft' as VendorStatus,
    submittedBy: 'user-001',
    createdAt: new Date('2024-01-22T15:00:00Z'),
    updatedAt: new Date('2024-01-22T15:30:00Z'),
  },
  {
    id: 'vendor-004',
    companyName: '株式会社却下サンプル',
    registrationNumber: '1111111111111',
    address: '福岡県福岡市博多区博多駅前4-4-4',
    contactName: '高橋四郎',
    contactEmail: 'takahashi@rejected-sample.co.jp',
    contactPhone: '092-3333-4444',
    businessCategory: 'サービス業',
    status: 'rejected' as VendorStatus,
    submittedBy: 'user-001',
    submittedAt: new Date('2024-01-18T09:00:00Z'),
    reviewedBy: 'user-002',
    reviewedAt: new Date('2024-01-19T16:00:00Z'),
    reviewComment: '登記情報に不備があるため、修正後に再申請をお願いします。',
    createdAt: new Date('2024-01-18T08:45:00Z'),
    updatedAt: new Date('2024-01-19T16:00:00Z'),
  },
];

/**
 * 取引先申請を取得（ID指定）
 */
export function getVendorById(id: string): VendorApplication | undefined {
  return MOCK_VENDORS.find((vendor) => vendor.id === id);
}

/**
 * ユーザーの取引先申請一覧を取得
 * - 申請者: 自分が作成した申請のみ
 * - 承認者・管理者: 全ての申請
 */
export function getVendorsByUser(userId: string, userRole: UserRole): VendorApplication[] {
  if (userRole === 'applicant') {
    return MOCK_VENDORS.filter((vendor) => vendor.submittedBy === userId);
  }
  // 承認者と管理者は全ての申請を閲覧可能
  return MOCK_VENDORS;
}

/**
 * ステータス別に取引先申請を取得
 */
export function getVendorsByStatus(status: VendorStatus): VendorApplication[] {
  return MOCK_VENDORS.filter((vendor) => vendor.status === status);
}

/**
 * 承認待ちの取引先申請を取得
 */
export function getPendingVendors(): VendorApplication[] {
  return getVendorsByStatus('pending_approval');
}

/**
 * 次の取引先申請IDを生成
 */
let vendorIdCounter = MOCK_VENDORS.length + 1;
export function generateVendorId(): string {
  return `vendor-${String(vendorIdCounter++).padStart(3, '0')}`;
}

/**
 * モックデータストア（実際のアプリでは状態管理ライブラリやAPIを使用）
 */
class MockDataStore {
  private vendors: VendorApplication[] = [...MOCK_VENDORS];

  getAll(): VendorApplication[] {
    return [...this.vendors];
  }

  getById(id: string): VendorApplication | undefined {
    return this.vendors.find((v) => v.id === id);
  }

  create(vendor: VendorApplication): VendorApplication {
    this.vendors.push(vendor);
    return vendor;
  }

  update(id: string, updates: Partial<VendorApplication>): VendorApplication | undefined {
    const index = this.vendors.findIndex((v) => v.id === id);
    if (index === -1) return undefined;

    this.vendors[index] = {
      ...this.vendors[index],
      ...updates,
      updatedAt: new Date(),
    };
    return this.vendors[index];
  }

  delete(id: string): boolean {
    const index = this.vendors.findIndex((v) => v.id === id);
    if (index === -1) return false;

    this.vendors.splice(index, 1);
    return true;
  }

  reset(): void {
    this.vendors = [...MOCK_VENDORS];
    vendorIdCounter = MOCK_VENDORS.length + 1;
  }
}

/**
 * グローバルモックデータストアインスタンス
 */
export const mockDataStore = new MockDataStore();
