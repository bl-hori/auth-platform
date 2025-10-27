/**
 * ユーザーロール
 */
export type UserRole = 'applicant' | 'approver' | 'admin';

/**
 * ユーザーエンティティ
 */
export interface User {
  /** ユーザーID */
  id: string;
  /** ユーザー名 */
  name: string;
  /** メールアドレス */
  email: string;
  /** ユーザーロール */
  role: UserRole;
}

/**
 * 認可リクエスト
 */
export interface AuthorizationRequest {
  /** ユーザーID */
  userId: string;
  /** アクション（例: read, update, delete, approve） */
  action: string;
  /** リソースタイプ（例: vendor） */
  resourceType: string;
  /** リソースID（オプション） */
  resourceId?: string;
  /** 追加コンテキスト情報 */
  context?: Record<string, any>;
}

/**
 * 認可レスポンス
 */
export interface AuthorizationResponse {
  /** 認可決定（ALLOW/DENY/ERROR） */
  decision: 'ALLOW' | 'DENY' | 'ERROR';
  /** 決定理由 */
  reason?: string;
  /** 追加メタデータ */
  metadata?: Record<string, any>;
}

/**
 * バッチ認可リクエスト
 */
export interface BatchAuthorizationRequest {
  /** 認可リクエストの配列 */
  requests: AuthorizationRequest[];
}

/**
 * バッチ認可レスポンス
 */
export interface BatchAuthorizationResponse {
  /** 認可レスポンスの配列 */
  responses: AuthorizationResponse[];
}

/**
 * Auth Platformクライアント設定
 */
export interface AuthClientConfig {
  /** バックエンドURL */
  baseUrl: string;
  /** APIキー */
  apiKey: string;
  /** 組織ID */
  organizationId: string;
}

/**
 * ロールの日本語表示
 */
export const ROLE_LABELS: Record<UserRole, string> = {
  applicant: '申請者',
  approver: '承認者',
  admin: '管理者',
};

/**
 * ロール別の権限
 */
export const ROLE_PERMISSIONS: Record<UserRole, string[]> = {
  applicant: [
    'vendor:create',
    'vendor:read:own',
    'vendor:update:own',
    'vendor:delete:own',
    'vendor:submit',
  ],
  approver: ['vendor:read:all', 'vendor:approve'],
  admin: [
    'vendor:create',
    'vendor:read:all',
    'vendor:update:all',
    'vendor:delete:all',
    'vendor:approve',
    'vendor:submit',
  ],
};
