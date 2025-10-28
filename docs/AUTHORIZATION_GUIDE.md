# Auth Platform 認可機能 利用ガイド

Auth Platformの認可機能を使用してアプリケーションにきめ細かなアクセス制御を実装する方法を説明します。

## 目次

- [概要](#概要)
- [認可の基本概念](#認可の基本概念)
- [デモモード vs フル統合モード](#デモモード-vs-フル統合モード)
- [クイックスタート](#クイックスタート)
- [クライアント側統合](#クライアント側統合)
- [RBAC設定ガイド](#rbac設定ガイド)
- [ベストプラクティス](#ベストプラクティス)
- [トラブルシューティング](#トラブルシューティング)

---

## 概要

Auth Platformは、Role-Based Access Control (RBAC)とAttribute-Based Access Control (ABAC)を組み合わせた強力な認可システムを提供します。

### 主要機能

- **ロールベースアクセス制御 (RBAC)**: ユーザーにロールを割り当て、ロールに権限を付与
- **属性ベースアクセス制御 (ABAC)**: ユーザー属性やリソース属性に基づく動的な認可
- **リアルタイム認可**: APIを通じてリアルタイムで認可判定
- **キャッシュによる高速化**: L1 (Caffeine) とL2 (Redis) の2層キャッシュ
- **バッチ認可**: 複数リソースの認可を一度にチェック
- **OPA統合**: Open Policy Agent (OPA)による高度なポリシー評価

---

## 認可の基本概念

### 認可リクエストの構成要素

```typescript
{
  organizationId: "組織ID",
  principal: {
    id: "user-001",           // ユーザーID (externalId)
    type: "user"              // プリンシパルタイプ
  },
  action: "read",             // アクション (read, update, delete, approve など)
  resource: {
    type: "vendor",           // リソースタイプ
    id: "vendor-001"          // リソースID
  },
  context: {                  // オプション: コンテキスト情報
    status: "pending_approval",
    ownerId: "user-001"
  }
}
```

### 認可レスポンス

```typescript
{
  decision: "ALLOW" | "DENY" | "ERROR",
  reason: "承認理由または却下理由",
  timestamp: "2025-10-28T12:00:00Z",
  evaluationTimeMs: 5,
  context: {
    matchedRoles: ["approver", "admin"],
    matchedPermissions: ["vendor:read"]
  }
}
```

---

## デモモード vs フル統合モード

Auth Platform統合には2つのモードがあります。

### デモモード（推奨: クイックスタート用）

**用途**: プロトタイプ、UI確認、デモ

**特徴**:
- Auth Platform APIを呼ばない
- すべての認可チェックが`ALLOW`を返す
- ユーザー・ロール・パーミッションのセットアップ不要
- 即座に動作確認可能

**設定方法**:
```bash
# .env.local
NEXT_PUBLIC_DEMO_MODE=true
```

**メリット**:
- ✅ 即座に動作確認
- ✅ バックエンド設定不要
- ✅ UIフローの確認に最適

**デメリット**:
- ❌ 実際の認可チェックは行われない
- ❌ 本番環境では使用不可

### フル統合モード

**用途**: 本番環境、実際の認可が必要な環境

**特徴**:
- Auth Platform APIにリアルタイムで問い合わせ
- RBAC/ABACに基づく実際の認可判定
- ユーザー・ロール・パーミッションの事前設定が必要

**設定方法**:
```bash
# .env.local
NEXT_PUBLIC_DEMO_MODE=false
NEXT_PUBLIC_AUTH_BACKEND_URL=http://localhost:8080
NEXT_PUBLIC_AUTH_API_KEY=dev-key-org1-abc123
NEXT_PUBLIC_AUTH_ORGANIZATION_ID=00000000-0000-0000-0000-000000000000
```

**メリット**:
- ✅ 実際の認可チェック
- ✅ 本番環境対応
- ✅ 監査ログ記録

**デメリット**:
- ❌ RBAC設定が必要（ユーザー、ロール、パーミッション）
- ❌ セットアップに時間がかかる

### 推奨フロー

```
開発初期段階
  ↓ デモモードでUI確認
本番準備段階
  ↓ フル統合モードでRBAC設定
本番環境
  ↓ フル統合モードで運用
```

---

## クイックスタート

### ステップ 1: クライアントライブラリのセットアップ

#### TypeScript/React プロジェクト

参考実装: `examples/vendor-registration/lib/auth-client.ts`

```typescript
// lib/auth-client.ts
import { AuthorizationRequest, AuthorizationResponse } from '@/types/auth';

export class AuthPlatformClient {
  private baseUrl: string;
  private apiKey: string;
  private organizationId: string;
  private demoMode: boolean;

  constructor(config: {
    baseUrl: string;
    apiKey: string;
    organizationId: string;
  }) {
    this.baseUrl = config.baseUrl;
    this.apiKey = config.apiKey;
    this.organizationId = config.organizationId;
    this.demoMode = process.env.NEXT_PUBLIC_DEMO_MODE === 'true';
  }

  async authorize(request: AuthorizationRequest): Promise<AuthorizationResponse> {
    // デモモード: 常にALLOWを返す
    if (this.demoMode) {
      return {
        decision: 'ALLOW',
        reason: 'Demo mode - all requests allowed',
      };
    }

    // 実際のAPI呼び出し
    const response = await fetch(`${this.baseUrl}/v1/authorize`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': this.apiKey,
        'X-Organization-Id': this.organizationId,
      },
      body: JSON.stringify({
        organizationId: this.organizationId,
        principal: {
          id: request.userId,
          type: 'user',
        },
        action: request.action,
        resource: {
          type: request.resourceType,
          id: request.resourceId,
        },
        context: request.context,
      }),
    });

    if (!response.ok) {
      throw new Error(`Authorization API error: ${response.status}`);
    }

    return await response.json();
  }
}

// シングルトンインスタンス
export const authClient = new AuthPlatformClient({
  baseUrl: process.env.NEXT_PUBLIC_AUTH_BACKEND_URL!,
  apiKey: process.env.NEXT_PUBLIC_AUTH_API_KEY!,
  organizationId: process.env.NEXT_PUBLIC_AUTH_ORGANIZATION_ID!,
});
```

### ステップ 2: 環境変数の設定

```bash
# .env.local
NEXT_PUBLIC_AUTH_BACKEND_URL=http://localhost:8080
NEXT_PUBLIC_AUTH_API_KEY=dev-key-org1-abc123
NEXT_PUBLIC_AUTH_ORGANIZATION_ID=00000000-0000-0000-0000-000000000000

# デモモード (開発初期段階)
NEXT_PUBLIC_DEMO_MODE=true

# フル統合モード (本番準備段階)
# NEXT_PUBLIC_DEMO_MODE=false
```

### ステップ 3: 認可チェックの実装

#### パターン 1: シンプルな認可チェック

```typescript
import { authClient } from '@/lib/auth-client';

async function checkPermission(userId: string, action: string, resourceId: string) {
  const response = await authClient.authorize({
    userId,
    action,
    resourceType: 'vendor',
    resourceId,
  });

  return response.decision === 'ALLOW';
}

// 使用例
const canEdit = await checkPermission('user-001', 'update', 'vendor-001');
if (canEdit) {
  // 編集可能
} else {
  // 編集不可
}
```

#### パターン 2: React Hookを使用

参考実装: `examples/vendor-registration/lib/authorization.ts`

```typescript
// lib/authorization.ts
import { useCallback, useState } from 'react';
import { authClient } from './auth-client';

export function useAuthorization() {
  const [isLoading, setIsLoading] = useState(false);

  const checkPermission = useCallback(
    async (
      action: string,
      resourceType: string,
      resourceId?: string,
      context?: Record<string, any>
    ): Promise<boolean> => {
      setIsLoading(true);
      try {
        const response = await authClient.authorize({
          userId: getCurrentUser().id,
          action,
          resourceType,
          resourceId,
          context,
        });
        return response.decision === 'ALLOW';
      } catch (error) {
        console.error('Authorization check failed:', error);
        return false; // フェイルセーフ
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  return { checkPermission, isLoading };
}

// コンポーネントでの使用例
function VendorEditButton({ vendorId }: { vendorId: string }) {
  const { checkPermission, isLoading } = useAuthorization();
  const [canEdit, setCanEdit] = useState(false);

  useEffect(() => {
    checkPermission('update', 'vendor', vendorId).then(setCanEdit);
  }, [vendorId, checkPermission]);

  if (isLoading) return <Spinner />;
  if (!canEdit) return null;

  return <Button>編集</Button>;
}
```

#### パターン 3: ページレベルの認可

参考実装: `examples/vendor-registration/app/vendors/[id]/page.tsx`

```typescript
export default function VendorDetailPage({ params }: { params: { id: string } }) {
  const [authorized, setAuthorized] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkAuthorization();
  }, [params.id]);

  async function checkAuthorization() {
    const vendor = await getVendorById(params.id);

    // 閲覧権限をチェック
    const response = await authClient.authorize({
      userId: getCurrentUser().id,
      action: 'read',
      resourceType: 'vendor',
      resourceId: vendor.id,
      context: { ownerId: vendor.submittedBy },
    });

    setAuthorized(response.decision === 'ALLOW');
    setLoading(false);
  }

  if (loading) return <LoadingSpinner />;

  if (!authorized) {
    return (
      <div>
        <h1>アクセス拒否</h1>
        <p>この取引先申請を閲覧する権限がありません。</p>
      </div>
    );
  }

  return <div>{/* 取引先詳細 */}</div>;
}
```

---

## クライアント側統合

### 認可チェックのベストプラクティス

#### 1. フェイルセーフ設計

```typescript
try {
  const response = await authClient.authorize(request);
  return response.decision === 'ALLOW';
} catch (error) {
  console.error('Authorization check failed:', error);
  // エラー時はアクセス拒否（安全側に倒す）
  return false;
}
```

#### 2. キャッシュの活用

同じ認可チェックを短期間に繰り返す場合、キャッシュを使用：

```typescript
class AuthorizationCache {
  private cache = new Map<string, CacheEntry>();
  private defaultTTL = 60000; // 60秒

  get(request: AuthorizationRequest): AuthorizationResponse | null {
    const key = this.getCacheKey(request);
    const entry = this.cache.get(key);

    if (!entry || Date.now() > entry.expiresAt) {
      this.cache.delete(key);
      return null;
    }

    return entry.response;
  }

  set(request: AuthorizationRequest, response: AuthorizationResponse): void {
    const key = this.getCacheKey(request);
    this.cache.set(key, {
      response,
      expiresAt: Date.now() + this.defaultTTL,
    });
  }
}
```

#### 3. バッチ認可

複数リソースの権限を一度にチェック：

```typescript
const results = await authClient.authorizeBatch([
  { userId: 'user-001', action: 'read', resourceType: 'vendor', resourceId: 'vendor-001' },
  { userId: 'user-001', action: 'update', resourceType: 'vendor', resourceId: 'vendor-002' },
  { userId: 'user-001', action: 'delete', resourceType: 'vendor', resourceId: 'vendor-003' },
]);

results.forEach((response, index) => {
  console.log(`Resource ${index + 1}: ${response.decision}`);
});
```

### React コンポーネントパターン

#### 条件付きレンダリング

```typescript
function DeleteButton({ vendorId }: { vendorId: string }) {
  const { allowed, loading } = usePermission('delete', 'vendor', vendorId);

  if (loading) return <Spinner />;
  if (!allowed) return null;

  return <Button variant="destructive">削除</Button>;
}
```

#### 保護されたボタン

```typescript
<ProtectedButton
  action="delete"
  resourceType="vendor"
  resourceId={vendorId}
  variant="destructive"
  onClick={handleDelete}
>
  削除
</ProtectedButton>
```

---

## RBAC設定ガイド

フル統合モードで使用する場合、事前にRBAC設定が必要です。

### ステップ 1: 組織の作成

すでに存在する場合はスキップ。開発環境ではシステム組織を使用可能：

```bash
# システム組織ID（開発用）
00000000-0000-0000-0000-000000000000
```

### ステップ 2: ユーザーの作成

**重要**: `externalId`フィールドを設定してください。これが認可APIでのユーザー識別子になります。

```bash
curl -X POST http://localhost:8080/v1/users \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "organizationId": "00000000-0000-0000-0000-000000000000",
    "email": "tanaka@example.com",
    "username": "tanaka",
    "displayName": "田中太郎",
    "externalId": "user-001",
    "attributes": {
      "department": "Sales",
      "title": "Account Manager"
    }
  }'
```

**返却値**:
```json
{
  "id": "c0a10831-02f9-46ea-abae-db94b9310c5b",
  "organizationId": "00000000-0000-0000-0000-000000000000",
  "email": "tanaka@example.com",
  "username": "tanaka",
  "displayName": "田中太郎",
  "externalId": "user-001",
  "status": "active"
}
```

### ステップ 3: ロールの作成

```bash
# 申請者ロール
curl -X POST http://localhost:8080/v1/roles \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "organizationId": "00000000-0000-0000-0000-000000000000",
    "name": "applicant",
    "displayName": "申請者",
    "description": "取引先申請を作成・編集できるロール"
  }'

# 承認者ロール
curl -X POST http://localhost:8080/v1/roles \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "organizationId": "00000000-0000-0000-0000-000000000000",
    "name": "approver",
    "displayName": "承認者",
    "description": "取引先申請を承認・却下できるロール"
  }'

# 管理者ロール
curl -X POST http://localhost:8080/v1/roles \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "organizationId": "00000000-0000-0000-0000-000000000000",
    "name": "admin",
    "displayName": "管理者",
    "description": "すべての操作が可能な管理者ロール"
  }'
```

### ステップ 4: パーミッションの作成

```bash
# 読み取り権限
curl -X POST http://localhost:8080/v1/permissions \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "organizationId": "00000000-0000-0000-0000-000000000000",
    "name": "vendor:read",
    "displayName": "取引先閲覧",
    "description": "取引先情報を閲覧する権限",
    "resourceType": "vendor",
    "action": "read",
    "effect": "ALLOW"
  }'

# 更新権限
curl -X POST http://localhost:8080/v1/permissions \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "organizationId": "00000000-0000-0000-0000-000000000000",
    "name": "vendor:update",
    "displayName": "取引先編集",
    "description": "取引先情報を編集する権限",
    "resourceType": "vendor",
    "action": "update",
    "effect": "ALLOW"
  }'

# 承認権限
curl -X POST http://localhost:8080/v1/permissions \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "organizationId": "00000000-0000-0000-0000-000000000000",
    "name": "vendor:approve",
    "displayName": "取引先承認",
    "description": "取引先申請を承認・却下する権限",
    "resourceType": "vendor",
    "action": "approve",
    "effect": "ALLOW"
  }'

# 削除権限
curl -X POST http://localhost:8080/v1/permissions \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "organizationId": "00000000-0000-0000-0000-000000000000",
    "name": "vendor:delete",
    "displayName": "取引先削除",
    "description": "取引先情報を削除する権限",
    "resourceType": "vendor",
    "action": "delete",
    "effect": "ALLOW"
  }'
```

### ステップ 5: ロールにパーミッションを割り当て

```bash
# 申請者ロール: 読み取り、更新
curl -X POST http://localhost:8080/v1/roles/{APPLICANT_ROLE_ID}/permissions \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "permissionId": "{VENDOR_READ_PERMISSION_ID}"
  }'

curl -X POST http://localhost:8080/v1/roles/{APPLICANT_ROLE_ID}/permissions \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "permissionId": "{VENDOR_UPDATE_PERMISSION_ID}"
  }'

# 承認者ロール: 読み取り、承認
curl -X POST http://localhost:8080/v1/roles/{APPROVER_ROLE_ID}/permissions \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "permissionId": "{VENDOR_READ_PERMISSION_ID}"
  }'

curl -X POST http://localhost:8080/v1/roles/{APPROVER_ROLE_ID}/permissions \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "permissionId": "{VENDOR_APPROVE_PERMISSION_ID}"
  }'

# 管理者ロール: すべての権限
# ... (同様に全パーミッションを割り当て)
```

### ステップ 6: ユーザーにロールを割り当て

```bash
curl -X POST http://localhost:8080/v1/users/{USER_ID}/roles \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "roleId": "{APPROVER_ROLE_ID}"
  }'
```

### ステップ 7: 認可テスト

```bash
curl -X POST http://localhost:8080/v1/authorize \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "organizationId": "00000000-0000-0000-0000-000000000000",
    "principal": {
      "id": "user-001",
      "type": "user"
    },
    "action": "read",
    "resource": {
      "type": "vendor",
      "id": "vendor-001"
    }
  }'
```

**期待される結果**:
```json
{
  "decision": "ALLOW",
  "reason": "User has 'vendor:read' permission via roles: approver",
  "timestamp": "2025-10-28T12:00:00Z",
  "evaluationTimeMs": 5,
  "context": {
    "matchedRoles": ["approver"],
    "matchedPermissions": ["vendor:read"]
  }
}
```

---

## ベストプラクティス

### 1. セキュリティ

#### APIキーの保護

```bash
# ❌ NG: APIキーをコードにハードコード
const apiKey = "dev-key-org1-abc123";

# ✅ OK: 環境変数から読み込み
const apiKey = process.env.NEXT_PUBLIC_AUTH_API_KEY!;
```

#### フェイルセーフ

```typescript
// ✅ エラー時はアクセス拒否
try {
  const response = await authClient.authorize(request);
  return response.decision === 'ALLOW';
} catch (error) {
  console.error('Authorization failed:', error);
  return false; // 安全側に倒す
}
```

### 2. パフォーマンス

#### キャッシュの活用

```typescript
// ✅ キャッシュを使用して同じ認可チェックを高速化
const cached = authCache.get(request);
if (cached) {
  return cached;
}

const response = await authClient.authorize(request);
authCache.set(request, response);
return response;
```

#### バッチ処理

```typescript
// ❌ NG: ループ内で個別に認可チェック
for (const vendor of vendors) {
  const canEdit = await checkAuthorization('update', 'vendor', vendor.id);
  // ...
}

// ✅ OK: バッチ認可で一度にチェック
const requests = vendors.map(v => ({
  userId: currentUser.id,
  action: 'update',
  resourceType: 'vendor',
  resourceId: v.id,
}));
const results = await authClient.authorizeBatch(requests);
```

### 3. ユーザビリティ

#### ローディング状態の表示

```typescript
function EditButton({ vendorId }: { vendorId: string }) {
  const { allowed, loading } = usePermission('update', 'vendor', vendorId);

  if (loading) return <Spinner />;
  if (!allowed) return null;

  return <Button>編集</Button>;
}
```

#### わかりやすいエラーメッセージ

```typescript
if (!authorized) {
  return (
    <div>
      <h1>アクセス拒否</h1>
      <p>この取引先申請を閲覧する権限がありません。</p>
      <p>管理者に権限付与を依頼してください。</p>
    </div>
  );
}
```

### 4. デバッグ

#### デバッグログの活用

```typescript
if (process.env.NODE_ENV === 'development') {
  console.log('Authorization request:', request);
  console.log('Authorization response:', response);
}
```

#### 開発者ツールでの確認

```typescript
// Chrome DevTools の Network タブで認可APIコールを確認
// Headers: X-API-Key, X-Organization-Id
// Request Payload: principal, action, resource
// Response: decision, reason, context
```

---

## トラブルシューティング

### よくある問題と解決方法

#### 1. "Failed to fetch" エラー

**原因**: バックエンドが起動していない、またはCORS設定の問題

**解決方法**:
```bash
# バックエンドの起動確認
curl http://localhost:8080/actuator/health

# CORS設定確認（WebConfig.java）
# localhost:3000, localhost:3001 が許可されているか確認
```

**クイックフィックス**: デモモードを有効にする
```bash
NEXT_PUBLIC_DEMO_MODE=true
```

#### 2. "User not found" エラー

**原因**: `externalId`が設定されていない、またはユーザーが存在しない

**解決方法**:
```bash
# ユーザー一覧を確認
curl -X GET "http://localhost:8080/v1/users?page=0&size=20" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000"

# externalId が null の場合は更新
curl -X PUT "http://localhost:8080/v1/users/{USER_ID}" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "email": "tanaka@example.com",
    "username": "tanaka",
    "displayName": "田中太郎",
    "externalId": "user-001"
  }'
```

#### 3. "User has no roles assigned" エラー

**原因**: ユーザーにロールが割り当てられていない

**解決方法**:
```bash
# ロール一覧を取得
curl -X GET "http://localhost:8080/v1/roles?page=0&size=20" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000"

# ユーザーにロールを割り当て
curl -X POST "http://localhost:8080/v1/users/{USER_ID}/roles" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "X-Organization-Id: 00000000-0000-0000-0000-000000000000" \
  -d '{
    "roleId": "{ROLE_ID}"
  }'
```

#### 4. "Invalid API key" (403エラー)

**原因**: APIキーが間違っている、または組織IDが一致しない

**解決方法**:
```bash
# backend/src/main/resources/application.yml を確認
# api-keys セクションで有効なキーを確認

# 環境変数を確認
echo $NEXT_PUBLIC_AUTH_API_KEY
echo $NEXT_PUBLIC_AUTH_ORGANIZATION_ID
```

#### 5. 環境変数が反映されない

**原因**: 開発サーバーを再起動していない

**解決方法**:
```bash
# .env.local を変更したら必ず再起動
# Ctrl+C でサーバーを停止
pnpm dev  # または npm run dev
```

### デバッグチェックリスト

- [ ] バックエンドが起動している (`http://localhost:8080/actuator/health`)
- [ ] `.env.local` が存在し、正しい値が設定されている
- [ ] 環境変数の変更後、開発サーバーを再起動した
- [ ] CORS設定にフロントエンドのポートが含まれている
- [ ] ユーザーの`externalId`が設定されている
- [ ] ユーザーにロールが割り当てられている
- [ ] ロールにパーミッションが割り当てられている
- [ ] パーミッションのresourceTypeとactionが一致している

---

## 参考資料

- [API Integration Guide](./API_INTEGRATION_GUIDE.md) - 認可API詳細
- [Getting Started](./GETTING_STARTED.md) - 初期セットアップ
- [Vendor Registration Example](../examples/vendor-registration/README.md) - 実装例
- [Troubleshooting Guide](./TROUBLESHOOTING.md) - その他の問題解決

---

## まとめ

Auth Platformの認可機能を使用することで、アプリケーションにきめ細かなアクセス制御を実装できます。

**開発フロー**:
1. デモモードでUI/UXを確認
2. RBAC設定（ユーザー、ロール、パーミッション）
3. フル統合モードでテスト
4. 本番環境にデプロイ

**開発初期段階**: デモモードを使用して素早くプロトタイプ
**本番準備段階**: フル統合モードで実際の認可チェック
**本番環境**: セキュアな運用とログ記録

質問や問題がある場合は、[Troubleshooting Guide](./TROUBLESHOOTING.md)を参照するか、開発チームにお問い合わせください。
