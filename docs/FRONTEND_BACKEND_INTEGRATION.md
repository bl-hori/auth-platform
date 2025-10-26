# フロントエンド-バックエンド統合ガイド

## 概要

このドキュメントは、フロントエンド (Next.js) とバックエンド (Spring Boot) の統合方法を説明します。

## 環境設定

### 1. バックエンドの起動

```bash
cd backend
./gradlew bootRun
```

バックエンドは http://localhost:8080 で起動します。

### 2. フロントエンドの設定

`.env.local` ファイルを作成 (既に存在する場合は編集):

```bash
# Backend API Configuration
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_API_KEY=dev-key-org1-abc123

# Mock API Control
# Set to 'false' to use real backend API, 'true' or omit to use mock data
NEXT_PUBLIC_USE_MOCK_API=false
```

### 3. フロントエンドの起動

```bash
cd frontend
npm install
npm run dev
```

フロントエンドは http://localhost:3000 で起動します。

## API認証

### API Key認証

全てのAPIリクエストには `X-API-Key` ヘッダーが必要です:

```typescript
headers: {
  'X-API-Key': process.env.NEXT_PUBLIC_API_KEY
}
```

### API Key と Organization のマッピング

```yaml
# backend/src/main/resources/application.yml
auth-platform:
  security:
    api-keys:
      keys:
        dev-key-org1-abc123: 00000000-0000-0000-0000-000000000000  # System organization
```

## Organization ID の扱い

### ⚠️ 重要: organizationId パラメータ

**現在の実装では**、全てのAPIエンドポイントが `organizationId` をクエリパラメータとして要求します:

```typescript
// ❌ これは動作しません
GET /v1/policies

// ✅ これが必要です
GET /v1/policies?organizationId=00000000-0000-0000-0000-000000000000
```

### フロントエンド側の対応が必要

各 API クライアント関数に organizationId を追加する必要があります:

```typescript
// 例: policies.ts
export async function getPolicies(
  organizationId: string,  // ← 追加
  params?: PolicyListParams
): Promise<PagedResponse<Policy>> {
  if (USE_MOCK) {
    return getMockPolicies(params)
  }

  return apiClient.get<PagedResponse<Policy>>('/v1/policies', {
    params: {
      organizationId,  // ← 追加
      ...params as unknown as Record<string, string | number | boolean>,
    },
  })
}
```

### 今後の改善案

#### オプション1: バックエンド側で自動取得

SecurityContext から organizationId を自動的に取得することで、
クライアント側でパラメータを指定する必要をなくす:

```java
// カスタムアノテーション案
@GetMapping("/v1/policies")
public Page<PolicyResponse> listPolicies(
    @CurrentOrganizationId UUID organizationId,  // SecurityContextから自動取得
    Pageable pageable) {
    // ...
}
```

#### オプション2: フロントエンドで Context 使用

React Context で organizationId を管理し、全てのAPI呼び出しで自動的に追加:

```typescript
// useOrganization hook
const { organizationId } = useOrganization()
const policies = await getPolicies(organizationId)
```

## テスト済み機能

### Policy Management

1. **ポリシー作成** ✅
```bash
curl -X POST \
  -H "X-API-Key: dev-key-org1-abc123" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "00000000-0000-0000-0000-000000000000",
    "name": "test-policy-admin-access",
    "displayName": "管理者アクセスポリシー",
    "description": "管理者のGETリクエストを許可するテストポリシー",
    "regoCode": "package authz\n\ndefault allow = false\n\nallow {\n  input.method == \"GET\"\n  input.user.role == \"admin\"\n}"
  }' \
  http://localhost:8080/v1/policies
```

2. **ポリシー一覧取得** ✅
```bash
curl -H "X-API-Key: dev-key-org1-abc123" \
  "http://localhost:8080/v1/policies?organizationId=00000000-0000-0000-0000-000000000000"
```

3. **ポリシー公開** ✅
```bash
curl -X POST \
  -H "X-API-Key: dev-key-org1-abc123" \
  "http://localhost:8080/v1/policies/{policyId}/publish"
```

### Audit Log

1. **監査ログ一覧** ✅
```bash
curl -H "X-API-Key: dev-key-org1-abc123" \
  "http://localhost:8080/v1/audit-logs?organizationId=00000000-0000-0000-0000-000000000000&page=0&size=10"
```

2. **CSV エクスポート** ✅
```bash
curl -H "X-API-Key: dev-key-org1-abc123" \
  "http://localhost:8080/v1/audit-logs/export?organizationId=00000000-0000-0000-0000-000000000000"
```

## バリデーション

### Policy Name のフォーマット

ポリシー名は以下のルールに従う必要があります:

- 小文字のアルファベット (a-z)
- 数字 (0-9)
- ハイフン (-)
- 必ず文字で開始

```typescript
// ✅ 有効な名前
"user-access-policy"
"admin-write-2024"
"policy-v1"

// ❌ 無効な名前
"User_Access_Policy"  // アンダースコア不可
"001-policy"          // 数字で開始は不可
"policy/v1"           // スラッシュ不可
```

## トラブルシューティング

### CORS エラー

開発環境では CORS が設定済みです。本番環境では適切に設定してください。

### 認証エラー (401 Unauthorized)

- API Key が正しく設定されているか確認
- `.env.local` ファイルが正しい場所にあるか確認
- フロントエンドを再起動 (環境変数変更後)

### organizationId エラー (400 Bad Request)

- 全てのリクエストに organizationId パラメータを含めているか確認
- UUID形式が正しいか確認

## Next Steps

1. ✅ フロントエンドAPIクライアントに organizationId パラメータを追加
2. ✅ Organization Context/Hook の実装
3. ✅ エラーハンドリングの改善
4. ✅ ローディング状態の管理
5. ✅ Toast 通知の追加

## 参考資料

- [Backend API Documentation](../backend/README.md)
- [Frontend Documentation](../frontend/README.md)
- [Security Configuration](../backend/src/main/resources/application.yml)
