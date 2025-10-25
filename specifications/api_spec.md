# API仕様書 - 認可基盤プラットフォーム

## 1. 概要

### 1.1 目的
本仕様書は、認可基盤プラットフォームが提供するAPIの詳細仕様を定義する。RESTful APIとgRPC APIの両方について、エンドポイント、リクエスト/レスポンス形式、エラーハンドリングを明確化する。

### 1.2 API設計原則
- **RESTful原則**: リソース指向、統一インターフェース、ステートレス
- **一貫性**: 命名規則、レスポンス形式の統一
- **バージョニング**: URLパスベースのバージョニング（/v1/）
- **セキュリティ**: Bearer Token認証、API Key認証、mTLS
- **レート制限**: 段階的なレート制限とクォータ管理
- **国際化**: UTF-8エンコーディング、ISO 8601日時形式

## 2. 共通仕様

### 2.1 ベースURL
```
Production: https://api.authplatform.io
Staging: https://api-staging.authplatform.io
Development: https://api-dev.authplatform.io
```

### 2.2 認証方式

#### Bearer Token（OAuth 2.0）
```http
Authorization: Bearer {access_token}
```

#### API Key
```http
X-API-Key: {api_key}
```

#### mTLS（サービス間通信）
クライアント証明書による相互認証

### 2.3 共通ヘッダー

| ヘッダー名 | 必須 | 説明 |
|-----------|------|------|
| X-Request-ID | 推奨 | リクエストトレース用UUID |
| X-Organization-ID | 条件付き | マルチテナント時の組織ID |
| Accept-Language | 任意 | エラーメッセージの言語（ja, en） |
| X-Idempotency-Key | 条件付き | 冪等性保証用キー（POST/PUT時） |

### 2.4 共通レスポンス形式

#### 成功レスポンス
```json
{
  "data": {
    // レスポンスデータ
  },
  "metadata": {
    "request_id": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2025-10-25T12:00:00Z",
    "version": "1.0.0"
  }
}
```

#### エラーレスポンス
```json
{
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "The requested resource was not found",
    "details": {
      "resource_type": "policy",
      "resource_id": "policy-123"
    }
  },
  "metadata": {
    "request_id": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2025-10-25T12:00:00Z"
  }
}
```

### 2.5 HTTPステータスコード

| コード | 意味 | 使用場面 |
|--------|------|----------|
| 200 | OK | GET/PUT成功 |
| 201 | Created | POST成功（リソース作成） |
| 204 | No Content | DELETE成功 |
| 400 | Bad Request | リクエスト不正 |
| 401 | Unauthorized | 認証エラー |
| 403 | Forbidden | 権限不足 |
| 404 | Not Found | リソース不在 |
| 409 | Conflict | 競合（重複等） |
| 429 | Too Many Requests | レート制限 |
| 500 | Internal Server Error | サーバーエラー |
| 503 | Service Unavailable | メンテナンス中 |

## 3. 認可API

### 3.1 認可チェック

#### POST /v1/authorize
認可判定を行う最重要API

**リクエスト**
```json
{
  "subject": {
    "type": "user",
    "id": "user-123",
    "attributes": {
      "department": "engineering",
      "location": "tokyo"
    }
  },
  "action": "read",
  "resource": {
    "type": "document",
    "id": "doc-456",
    "attributes": {
      "classification": "confidential",
      "owner": "user-789"
    }
  },
  "context": {
    "ip_address": "192.168.1.1",
    "time": "2025-10-25T12:00:00Z",
    "request_source": "web_app"
  }
}
```

**レスポンス（200 OK）**
```json
{
  "data": {
    "decision": "allow",
    "reasons": [
      "User has role 'admin' which grants 'read' on 'document'",
      "Attribute 'department' matches required value 'engineering'"
    ],
    "evaluated_policies": [
      {
        "id": "policy-123",
        "name": "AdminAccessPolicy",
        "type": "RBAC"
      },
      {
        "id": "policy-456",
        "name": "DepartmentPolicy",
        "type": "ABAC"
      }
    ],
    "obligations": [
      {
        "type": "audit_log",
        "parameters": {
          "level": "high",
          "retention_days": 90
        }
      }
    ]
  },
  "metadata": {
    "request_id": "550e8400-e29b-41d4-a716-446655440000",
    "processing_time_ms": 8,
    "timestamp": "2025-10-25T12:00:00Z"
  }
}
```

### 3.2 バッチ認可チェック

#### POST /v1/authorize/batch
複数の認可判定を一括実行

**リクエスト**
```json
{
  "requests": [
    {
      "id": "req-1",
      "subject": {...},
      "action": "read",
      "resource": {...}
    },
    {
      "id": "req-2",
      "subject": {...},
      "action": "write",
      "resource": {...}
    }
  ],
  "options": {
    "parallel": true,
    "fail_fast": false
  }
}
```

**レスポンス（200 OK）**
```json
{
  "data": {
    "results": [
      {
        "request_id": "req-1",
        "decision": "allow",
        "reasons": [...]
      },
      {
        "request_id": "req-2",
        "decision": "deny",
        "reasons": [...]
      }
    ],
    "summary": {
      "total": 2,
      "allowed": 1,
      "denied": 1,
      "errors": 0
    }
  }
}
```

## 4. ポリシー管理API

### 4.1 ポリシー作成

#### POST /v1/policies
新規ポリシーを作成

**リクエスト**
```json
{
  "name": "DocumentAccessPolicy",
  "description": "Controls access to documents",
  "type": "RBAC",
  "rules": {
    "language": "rego",
    "content": "package authz\n\nallow {\n  input.user.role == \"admin\"\n}"
  },
  "metadata": {
    "tags": ["document", "rbac"],
    "owner": "security-team"
  }
}
```

**レスポンス（201 Created）**
```json
{
  "data": {
    "id": "policy-789",
    "name": "DocumentAccessPolicy",
    "type": "RBAC",
    "status": "draft",
    "version": 1,
    "created_at": "2025-10-25T12:00:00Z",
    "created_by": "user-123"
  }
}
```

### 4.2 ポリシー一覧取得

#### GET /v1/policies
ポリシー一覧を取得

**パラメータ**
- `type`: ポリシータイプ（RBAC, ABAC, ReBAC）
- `status`: ステータス（draft, active, archived）
- `tag`: タグによるフィルタ
- `page`: ページ番号（デフォルト: 1）
- `limit`: 取得件数（デフォルト: 20, 最大: 100）
- `sort`: ソート順（created_at:desc, name:asc等）

**レスポンス（200 OK）**
```json
{
  "data": {
    "policies": [
      {
        "id": "policy-123",
        "name": "AdminPolicy",
        "type": "RBAC",
        "status": "active",
        "version": 3,
        "updated_at": "2025-10-25T10:00:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 45,
      "has_next": true
    }
  }
}
```

### 4.3 ポリシー詳細取得

#### GET /v1/policies/{policy_id}
特定のポリシーの詳細を取得

### 4.4 ポリシー更新

#### PUT /v1/policies/{policy_id}
ポリシーを更新（新バージョン作成）

### 4.5 ポリシー削除

#### DELETE /v1/policies/{policy_id}
ポリシーを論理削除

### 4.6 ポリシー公開

#### POST /v1/policies/{policy_id}/publish
ドラフトポリシーを公開

### 4.7 ポリシーテスト

#### POST /v1/policies/{policy_id}/test
ポリシーをテスト実行

## 5. ユーザー管理API

### 5.1 ユーザー同期

#### POST /v1/users/sync
IdPからユーザー情報を同期

**リクエスト**
```json
{
  "provider": "keycloak",
  "users": [
    {
      "external_id": "idp-user-123",
      "email": "user@example.com",
      "username": "johndoe",
      "full_name": "John Doe",
      "attributes": {
        "department": "engineering",
        "employee_id": "EMP001"
      }
    }
  ]
}
```

### 5.2 ユーザー取得

#### GET /v1/users/{user_id}
ユーザー情報を取得

### 5.3 ユーザー属性更新

#### PATCH /v1/users/{user_id}/attributes
ユーザー属性を部分更新

## 6. ロール管理API

### 6.1 ロール作成

#### POST /v1/roles
新規ロールを作成

**リクエスト**
```json
{
  "name": "DocumentEditor",
  "description": "Can edit documents",
  "permissions": [
    {
      "action": "read",
      "resource_type": "document"
    },
    {
      "action": "write",
      "resource_type": "document"
    }
  ],
  "parent_role_id": "role-viewer"
}
```

### 6.2 ロール割り当て

#### POST /v1/users/{user_id}/roles
ユーザーにロールを割り当て

**リクエスト**
```json
{
  "role_id": "role-123",
  "resource_id": "resource-456",
  "expires_at": "2025-12-31T23:59:59Z"
}
```

### 6.3 ロール取り消し

#### DELETE /v1/users/{user_id}/roles/{role_id}
ロール割り当てを取り消し

## 7. リレーションシップAPI（ReBAC）

### 7.1 リレーションシップ作成

#### POST /v1/relationships
エンティティ間の関係を定義

**リクエスト**
```json
{
  "subject": {
    "type": "user",
    "id": "user-123"
  },
  "relation": "owner",
  "object": {
    "type": "folder",
    "id": "folder-456"
  }
}
```

### 7.2 リレーションシップ検索

#### GET /v1/relationships
関係性を検索

**パラメータ**
- `subject_type`: 主体タイプ
- `subject_id`: 主体ID
- `relation`: 関係タイプ
- `object_type`: 客体タイプ
- `object_id`: 客体ID

### 7.3 リレーションシップ削除

#### DELETE /v1/relationships/{relationship_id}
関係性を削除

## 8. 監査ログAPI

### 8.1 監査ログ検索

#### GET /v1/audit-logs
監査ログを検索

**パラメータ**
- `user_id`: ユーザーID
- `action`: アクション
- `resource_type`: リソースタイプ
- `decision`: 判定結果（allow/deny）
- `from`: 開始日時
- `to`: 終了日時
- `page`: ページ番号
- `limit`: 取得件数

**レスポンス（200 OK）**
```json
{
  "data": {
    "logs": [
      {
        "id": "log-123",
        "timestamp": "2025-10-25T12:00:00Z",
        "user_id": "user-123",
        "action": "read",
        "resource": {
          "type": "document",
          "id": "doc-456"
        },
        "decision": "allow",
        "decision_reason": "User has admin role",
        "ip_address": "192.168.1.1",
        "user_agent": "Mozilla/5.0..."
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 50,
      "total": 1250
    }
  }
}
```

### 8.2 監査レポート生成

#### POST /v1/audit-logs/reports
監査レポートを生成

**リクエスト**
```json
{
  "type": "compliance",
  "period": {
    "from": "2025-10-01T00:00:00Z",
    "to": "2025-10-31T23:59:59Z"
  },
  "filters": {
    "resource_types": ["document", "database"],
    "include_denied": true
  },
  "format": "pdf"
}
```

## 9. メトリクスAPI

### 9.1 メトリクス取得

#### GET /v1/metrics
システムメトリクスを取得

**レスポンス（200 OK）**
```json
{
  "data": {
    "authorization_requests": {
      "total": 1000000,
      "allowed": 950000,
      "denied": 50000,
      "error_rate": 0.001
    },
    "latency": {
      "p50": 5,
      "p95": 10,
      "p99": 25
    },
    "active_policies": 45,
    "active_users": 1250
  }
}
```

## 10. 管理API

### 10.1 ヘルスチェック

#### GET /v1/health
サービスの健全性を確認

**レスポンス（200 OK）**
```json
{
  "status": "healthy",
  "checks": {
    "database": "healthy",
    "cache": "healthy",
    "message_queue": "healthy"
  },
  "version": "1.2.3",
  "uptime": 864000
}
```

### 10.2 設定取得

#### GET /v1/config
公開設定を取得

### 10.3 APIキー管理

#### POST /v1/api-keys
新規APIキーを生成

**リクエスト**
```json
{
  "name": "Production App Key",
  "expires_at": "2026-10-25T00:00:00Z",
  "scopes": ["read:policies", "write:policies", "authorize"],
  "rate_limit": {
    "requests_per_second": 100,
    "burst": 200
  }
}
```

## 11. WebSocket API

### 11.1 リアルタイム更新購読

#### WebSocket /v1/ws/subscribe
ポリシー更新のリアルタイム通知

**接続**
```javascript
const ws = new WebSocket('wss://api.authplatform.io/v1/ws/subscribe');

// 認証
ws.send(JSON.stringify({
  type: 'auth',
  token: 'Bearer {access_token}'
}));

// 購読
ws.send(JSON.stringify({
  type: 'subscribe',
  channels: ['policies', 'roles']
}));

// メッセージ受信
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  if (message.type === 'policy_updated') {
    // ポリシー更新処理
  }
};
```

## 12. gRPC API

### 12.1 Protocol Buffers定義

```protobuf
syntax = "proto3";

package authplatform.v1;

service Authorization {
  rpc Authorize(AuthorizeRequest) returns (AuthorizeResponse);
  rpc AuthorizeBatch(AuthorizeBatchRequest) returns (AuthorizeBatchResponse);
  rpc StreamAuthorize(stream AuthorizeRequest) returns (stream AuthorizeResponse);
}

message AuthorizeRequest {
  Subject subject = 1;
  string action = 2;
  Resource resource = 3;
  Context context = 4;
}

message AuthorizeResponse {
  Decision decision = 1;
  repeated string reasons = 2;
  int64 processing_time_ms = 3;
}

enum Decision {
  UNKNOWN = 0;
  ALLOW = 1;
  DENY = 2;
}
```

### 12.2 gRPCエンドポイント
```
grpc://api.authplatform.io:443
```

## 13. SDK使用例

### 13.1 JavaScript/TypeScript

```typescript
import { AuthClient } from '@authplatform/sdk';

const client = new AuthClient({
  apiKey: 'your-api-key',
  baseUrl: 'https://api.authplatform.io'
});

// 認可チェック
const result = await client.authorize({
  subject: { type: 'user', id: 'user-123' },
  action: 'read',
  resource: { type: 'document', id: 'doc-456' }
});

if (result.decision === 'allow') {
  // アクセス許可
}
```

### 13.2 Java/Spring Boot

```java
@Service
public class AuthService {
    private final AuthClient authClient;
    
    @Autowired
    public AuthService(AuthClient authClient) {
        this.authClient = authClient;
    }
    
    public boolean checkAccess(String userId, String action, String resourceId) {
        AuthorizeRequest request = AuthorizeRequest.builder()
            .subject(Subject.of("user", userId))
            .action(action)
            .resource(Resource.of("document", resourceId))
            .build();
            
        AuthorizeResponse response = authClient.authorize(request);
        return response.getDecision() == Decision.ALLOW;
    }
}
```

## 14. エラーコード一覧

| エラーコード | HTTPステータス | 説明 |
|-------------|---------------|------|
| INVALID_REQUEST | 400 | リクエスト形式不正 |
| UNAUTHORIZED | 401 | 認証失敗 |
| FORBIDDEN | 403 | アクセス権限なし |
| RESOURCE_NOT_FOUND | 404 | リソース不存在 |
| CONFLICT | 409 | リソース競合 |
| RATE_LIMITED | 429 | レート制限超過 |
| INTERNAL_ERROR | 500 | 内部エラー |
| SERVICE_UNAVAILABLE | 503 | サービス利用不可 |

## 15. レート制限

### 15.1 制限値

| プラン | 認可API | 管理API | バースト |
|--------|---------|---------|----------|
| Free | 100 req/s | 10 req/s | 200 |
| Pro | 1,000 req/s | 100 req/s | 2,000 |
| Enterprise | 10,000 req/s | 1,000 req/s | 20,000 |

### 15.2 レート制限ヘッダー
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 950
X-RateLimit-Reset: 1635158400
```

---

**文書情報**
- バージョン: 1.0.0
- 作成日: 2025-10-25
- 作成者: Implementation Spec Agent
- レビュー状態: ドラフト
- 関連文書: architecture_design.md, requirement_spec.md
