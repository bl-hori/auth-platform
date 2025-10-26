# Auth Platform API - Postman Collection Guide

このガイドでは、Auth Platform APIのPostmanコレクションの使用方法を説明します。

## 📦 コレクションのインポート

### 方法1: ファイルからインポート

1. Postmanを開く
2. 左上の「Import」ボタンをクリック
3. `docs/auth-platform-api.postman_collection.json` をドラッグ&ドロップ
4. 「Import」をクリック

### 方法2: URLからインポート

```
https://raw.githubusercontent.com/[your-org]/auth-platform/main/docs/auth-platform-api.postman_collection.json
```

## 🔧 環境変数の設定

コレクションには以下の変数が含まれています:

| 変数名 | デフォルト値 | 説明 |
|--------|--------------|------|
| `base_url` | `http://localhost:8080` | API のベースURL |
| `api_key` | `dev-api-key-12345` | API認証キー |
| `org_id` | `org-001` | 組織ID |

### 環境変数の変更方法

1. Postmanの右上の環境選択ドロップダウンをクリック
2. 「Manage Environments」を選択
3. 新しい環境を作成するか、既存の環境を編集
4. 上記の変数を設定

### 本番環境用の設定例

```json
{
  "base_url": "https://api.authplatform.example.com",
  "api_key": "your-production-api-key",
  "org_id": "your-org-id"
}
```

## 📚 コレクションの構成

コレクションは以下のフォルダに分類されています:

### 1. Authorization (認可)
- **Single Authorization Request**: 単一の認可リクエスト
- **Batch Authorization Request**: 複数の認可リクエストを一括処理

### 2. Users (ユーザー管理)
- List Users: ユーザー一覧取得
- Get User by ID: ユーザー詳細取得
- Create User: ユーザー作成
- Update User: ユーザー更新
- Delete User: ユーザー削除
- Assign Role to User: ロール割り当て
- Remove Role from User: ロール削除

### 3. Roles (ロール管理)
- List Roles: ロール一覧取得
- Get Role by ID: ロール詳細取得
- Create Role: ロール作成
- Update Role: ロール更新
- Delete Role: ロール削除

### 4. Policies (ポリシー管理)
- List Policies: ポリシー一覧取得
- Get Policy by ID: ポリシー詳細取得
- Create Policy: ポリシー作成
- Update Policy: ポリシー更新
- Delete Policy: ポリシー削除
- Publish Policy: ポリシー公開
- Test Policy: ポリシーテスト

### 5. Audit Logs (監査ログ)
- List Audit Logs: 監査ログ一覧取得
- Get Audit Log by ID: 監査ログ詳細取得
- Export Audit Logs (CSV): CSV形式でエクスポート

### 6. Health & Metrics (ヘルスチェック & メトリクス)
- Health Check: APIヘルスチェック
- Prometheus Metrics: Prometheusメトリクス取得
- API Info: API情報取得

## 🚀 クイックスタート

### ステップ1: APIの起動

```bash
# バックエンドAPI起動
cd backend
./gradlew bootRun

# または Docker Composeで起動
docker-compose up -d
```

### ステップ2: ヘルスチェック

1. 「Health & Metrics」フォルダを開く
2. 「Health Check」リクエストを実行
3. レスポンスで `"status": "UP"` を確認

### ステップ3: 認可リクエストのテスト

1. 「Authorization」フォルダを開く
2. 「Single Authorization Request」を選択
3. リクエストボディを確認:
```json
{
  "subject": "user-001",
  "resource": "users",
  "action": "read",
  "context": {
    "organizationId": "org-001",
    "ipAddress": "192.168.1.100"
  }
}
```
4. 「Send」をクリック
5. レスポンスを確認:
```json
{
  "decision": "ALLOW",
  "reason": "User has required permissions",
  "evaluatedPolicies": ["user_access_policy"],
  "metadata": {}
}
```

## 🔐 認証

すべてのAPIリクエストには認証が必要です。コレクションは自動的に以下のヘッダーを追加します:

```
X-API-Key: {{api_key}}
```

### カスタムAPI キーの使用

1. 環境変数 `api_key` を更新、または
2. 個別のリクエストの「Headers」タブで `X-API-Key` を上書き

## 📝 リクエスト例

### ユーザーの作成

```http
POST /v1/users
Content-Type: application/json
X-API-Key: {{api_key}}

{
  "email": "newuser@example.com",
  "username": "newuser",
  "displayName": "New User",
  "organizationId": "org-001",
  "attributes": {
    "department": "Engineering",
    "level": "Senior"
  }
}
```

### ロールの作成（階層構造）

```http
POST /v1/roles
Content-Type: application/json
X-API-Key: {{api_key}}

{
  "name": "developer",
  "displayName": "Developer",
  "description": "Software developer role",
  "parentRoleId": "role-viewer",
  "organizationId": "org-001"
}
```

### ポリシーの作成

```http
POST /v1/policies
Content-Type: application/json
X-API-Key: {{api_key}}

{
  "name": "example_policy",
  "displayName": "Example Policy",
  "description": "Example authorization policy",
  "regoCode": "package authz\n\ndefault allow = false\n\nallow {\n  input.method == \"GET\"\n  input.user.role == \"admin\"\n}",
  "organizationId": "org-001"
}
```

### 監査ログのフィルタリング

```http
GET /v1/audit-logs?page=0&size=20&userId=user-001&resource=user&action=read&decision=ALLOW&startDate=2025-01-01&endDate=2025-12-31
X-API-Key: {{api_key}}
```

## 🧪 テストシナリオ

### シナリオ1: ユーザーとロールの管理

1. **新しいユーザーを作成** (`POST /v1/users`)
2. **ユーザー一覧を取得** (`GET /v1/users`)
3. **ロールを作成** (`POST /v1/roles`)
4. **ユーザーにロールを割り当て** (`POST /v1/users/:userId/roles`)
5. **ユーザー詳細を確認** (`GET /v1/users/:userId`)

### シナリオ2: ポリシーの作成とテスト

1. **新しいポリシーを作成** (`POST /v1/policies`)
2. **ポリシーをテスト** (`POST /v1/policies/:policyId/test`)
3. **ポリシーを公開** (`POST /v1/policies/:policyId/publish`)
4. **認可リクエストを実行** (`POST /v1/authorize`)

### シナリオ3: 監査ログの確認

1. **複数のAPIリクエストを実行**
2. **監査ログを取得** (`GET /v1/audit-logs`)
3. **特定のログの詳細を確認** (`GET /v1/audit-logs/:logId`)
4. **CSVでエクスポート** (`GET /v1/audit-logs/export`)

## 🔄 バッチリクエストの使用

複数の認可判定を一度に実行する場合:

```http
POST /v1/authorize/batch
Content-Type: application/json
X-API-Key: {{api_key}}

{
  "requests": [
    {
      "subject": "user-001",
      "resource": "users",
      "action": "read",
      "context": { "organizationId": "org-001" }
    },
    {
      "subject": "user-001",
      "resource": "roles",
      "action": "write",
      "context": { "organizationId": "org-001" }
    },
    {
      "subject": "user-001",
      "resource": "policies",
      "action": "read",
      "context": { "organizationId": "org-001" }
    }
  ]
}
```

レスポンス:
```json
{
  "responses": [
    { "decision": "ALLOW", "reason": "...", "evaluatedPolicies": [...] },
    { "decision": "DENY", "reason": "...", "evaluatedPolicies": [...] },
    { "decision": "ALLOW", "reason": "...", "evaluatedPolicies": [...] }
  ]
}
```

## 📊 メトリクスの確認

### Prometheusメトリクス

```http
GET /actuator/prometheus
X-API-Key: {{api_key}}
```

主要なメトリクス:
- `authz_requests_total`: 認可リクエスト総数
- `authz_cache_hit_ratio`: キャッシュヒット率
- `http_server_requests_seconds`: HTTPリクエストレイテンシ

## ❗ トラブルシューティング

### 401 Unauthorized

- `X-API-Key` ヘッダーが正しく設定されているか確認
- API キーが有効か確認

### 404 Not Found

- `base_url` が正しいか確認
- APIが起動しているか確認

### 500 Internal Server Error

- バックエンドのログを確認
- データベース接続を確認
- Redisが起動しているか確認

## 📖 追加リソース

- [API Documentation (Swagger UI)](http://localhost:8080/swagger-ui.html)
- [OpenAPI Specification](http://localhost:8080/v3/api-docs)
- [GitHub Repository](https://github.com/[your-org]/auth-platform)

## 🤝 サポート

質問や問題がある場合は、GitHubのIssuesページで報告してください。
