# backend_auth - Keycloak Authentication Server

Keycloak専用マイクロサービス。Auth PlatformにOIDC/OAuth2標準認証を提供します。

## 概要

**backend_auth**は、Auth Platformの認証を担当する独立したサービスです。Keycloak 24.0を使用し、以下の機能を提供します：

- **OIDC/OAuth2認証**: 標準プロトコルによるユーザー認証
- **JWTトークン発行**: アクセストークンとリフレッシュトークンの発行
- **Realm管理**: Organization毎の認証設定管理
- **Client管理**: Backend/Frontendアプリケーション用のClient設定

## クイックスタート

### 前提条件

- Docker 24.0+
- Docker Compose 2.20+
- PostgreSQL 15+ (docker-composeに含まれる)

### Keycloakの起動

```bash
# プロジェクトルートから
cd infrastructure
docker compose up -d

# Keycloakのログを確認
docker logs -f authplatform-keycloak

# 起動完了を確認（約1-2分かかります）
curl http://localhost:8180/health/ready
```

### 管理コンソールへのアクセス

Keycloakが起動したら、管理コンソールにアクセスできます：

**URL**: http://localhost:8180

**デフォルト管理者認証情報**:
- Username: `admin`
- Password: `admin`

⚠️ **セキュリティ警告**: 本番環境では必ず強力なパスワードに変更してください！

## ディレクトリ構造

```
backend_auth/
├── README.md           # このファイル
├── .gitignore          # Gitから除外するファイル
├── realms/             # Realm設定ファイル
│   └── authplatform-realm.json  # エクスポートされたRealm設定
└── themes/             # カスタムテーマ（今後実装）
```

## Realm設定

### デフォルトRealm: authplatform

Auth Platform用のデフォルトRealmが`authplatform`という名前で作成されます。

**Realm設定**:
- **Realm名**: `authplatform`
- **アクセストークン有効期限**: 15分
- **リフレッシュトークン有効期限**: 7日
- **セッションタイムアウト**: 1時間

### Realm設定のエクスポート/インポート

#### エクスポート

管理コンソールから手動でエクスポート：

1. 管理コンソールにログイン
2. 左側のRealm選択で`authplatform`を選択
3. `Export`メニューをクリック
4. `Export groups and roles`, `Export clients`をチェック
5. `Export`ボタンをクリック
6. ダウンロードしたJSONを`backend_auth/realms/authplatform-realm.json`に保存

#### インポート

docker-composeで自動的にインポートされます（`--import-realm`オプション使用）。

手動インポートする場合：

```bash
docker exec -it authplatform-keycloak /opt/keycloak/bin/kc.sh import \
  --file /opt/keycloak/data/import/authplatform-realm.json
```

## OIDC Endpoints

Keycloakが起動すると、以下のOIDCエンドポイントが利用可能になります：

### Discovery Document

**URL**: http://localhost:8180/realms/authplatform/.well-known/openid-configuration

```bash
curl http://localhost:8180/realms/authplatform/.well-known/openid-configuration | jq
```

### 主要エンドポイント

| エンドポイント | URL | 用途 |
|---------------|-----|------|
| Authorization | http://localhost:8180/realms/authplatform/protocol/openid-connect/auth | 認可コードフロー開始 |
| Token | http://localhost:8180/realms/authplatform/protocol/openid-connect/token | トークン取得/リフレッシュ |
| UserInfo | http://localhost:8180/realms/authplatform/protocol/openid-connect/userinfo | ユーザー情報取得 |
| JWK Set | http://localhost:8180/realms/authplatform/protocol/openid-connect/certs | 公開鍵取得 |
| Logout | http://localhost:8180/realms/authplatform/protocol/openid-connect/logout | ログアウト |

## Client設定

### Backend Client: auth-platform-backend

Backend APIサーバ用のClient設定。

**設定**:
- **Client ID**: `auth-platform-backend`
- **Access Type**: `bearer-only`
- **Valid Redirect URIs**: `http://localhost:8080/*`
- **用途**: JWTトークン検証専用

### Frontend Client: auth-platform-frontend

Next.jsフロントエンド用のClient設定。

**設定**:
- **Client ID**: `auth-platform-frontend`
- **Access Type**: `public` (Client Secretなし)
- **Standard Flow**: 有効（Authorization Code + PKCE）
- **Valid Redirect URIs**: `http://localhost:3000/*`
- **Web Origins**: `http://localhost:3000`
- **用途**: ユーザー認証フロー

## トークン取得例

### Password Grant (開発用)

⚠️ **注意**: Password Grantは開発・テスト目的のみ。本番環境ではAuthorization Code Flowを使用してください。

```bash
# テストユーザーでトークン取得
curl -X POST http://localhost:8180/realms/authplatform/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=auth-platform-frontend" \
  -d "grant_type=password" \
  -d "username=testuser@example.com" \
  -d "password=Password123!" \
  | jq
```

レスポンス例:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 900,
  "refresh_expires_in": 604800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer"
}
```

### トークンのデコード

JWTトークンをデコードして内容を確認：

```bash
# jwt-cliツールを使用（https://github.com/mike-engel/jwt-cli）
jwt decode <access_token>

# またはオンラインツール: https://jwt.io
```

期待されるClaims:
```json
{
  "exp": 1698765432,
  "iat": 1698761832,
  "jti": "abc-123-xyz",
  "iss": "http://localhost:8180/realms/authplatform",
  "aud": "auth-platform-backend",
  "sub": "user-uuid",
  "typ": "Bearer",
  "preferred_username": "testuser@example.com",
  "email": "testuser@example.com",
  "organization_id": "org-123"
}
```

### トークンのリフレッシュ

```bash
curl -X POST http://localhost:8180/realms/authplatform/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=auth-platform-frontend" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=<refresh_token>" \
  | jq
```

## トラブルシューティング

### Keycloakが起動しない

**症状**: `docker logs authplatform-keycloak`でエラーが表示される

**解決策**:
1. PostgreSQLが起動していることを確認
   ```bash
   docker ps | grep postgres
   ```

2. データベース接続設定を確認
   ```bash
   docker exec -it authplatform-postgres psql -U authplatform -c "\l"
   ```

3. Keycloakコンテナを再起動
   ```bash
   docker compose restart keycloak
   ```

### 管理コンソールにアクセスできない

**症状**: http://localhost:8180 にアクセスできない

**解決策**:
1. Keycloakが完全に起動するまで待つ（初回起動は1-2分かかる）
   ```bash
   curl http://localhost:8180/health/ready
   ```

2. ポート8180が他のプロセスで使用されていないか確認
   ```bash
   lsof -i :8180  # macOS/Linux
   netstat -ano | findstr :8180  # Windows
   ```

### トークン取得に失敗する

**症状**: `invalid_grant` エラーが返される

**解決策**:
1. ユーザーが存在することを確認（管理コンソールで確認）
2. Client IDが正しいことを確認
3. ユーザーのパスワードが正しいことを確認
4. Realmが`authplatform`であることを確認

### JWTトークンの署名検証に失敗する

**症状**: Backend側でトークン検証エラー

**解決策**:
1. JWK Set URIが正しいことを確認
   ```bash
   curl http://localhost:8180/realms/authplatform/protocol/openid-connect/certs
   ```

2. トークンの`iss` claimがKeycloakのissuer URIと一致することを確認

## セキュリティベストプラクティス

### 開発環境

✅ **実施済み**:
- TLS通信（開発環境ではHTTP）
- パスワードポリシー設定
- アカウントロックアウト設定

⚠️ **注意事項**:
- デフォルトの管理者パスワード（`admin`）を変更
- 強力なパスワードポリシーの適用

### 本番環境

🔒 **必須設定**:

1. **HTTPS/TLS 1.3の有効化**
   ```yaml
   KC_HTTPS_CERTIFICATE_FILE: /path/to/cert.pem
   KC_HTTPS_CERTIFICATE_KEY_FILE: /path/to/key.pem
   ```

2. **強力な管理者パスワード**
   - 最低16文字
   - 大文字・小文字・数字・記号を含む
   - パスワードマネージャーで管理

3. **データベース暗号化**
   - PostgreSQLのTLS接続
   - データベースバックアップの暗号化

4. **シークレット管理**
   - 環境変数ではなくSecret管理ツール使用
   - Kubernetes Secrets / AWS Secrets Manager / HashiCorp Vault

5. **ネットワーク分離**
   - Keycloakは内部ネットワークのみ
   - リバースプロキシ（Nginx/Traefik）経由でアクセス

## 次のステップ

### Phase 2: Backend JWT統合

backend_auth（Keycloak）が稼働したら、次はBackend側のJWT検証を実装します：

1. Spring Security OAuth2 Resource Server依存関係追加
2. JwtAuthenticationFilter実装
3. SecurityConfig更新
4. 統合テスト

詳細は `openspec/changes/add-keycloak-authentication/design.md` を参照。

### Phase 3: Frontend OAuth2統合

Frontend（Next.js）からOAuth2フローを実装：

1. NextAuth.js統合
2. Authorization Code Flow + PKCE実装
3. トークン管理
4. セッション管理

## 関連ドキュメント

- [Design Document](../openspec/changes/add-keycloak-authentication/design.md)
- [Implementation Tasks](../openspec/changes/add-keycloak-authentication/tasks.md)
- [Keycloak Official Documentation](https://www.keycloak.org/documentation)
- [OIDC Specification](https://openid.net/specs/openid-connect-core-1_0.html)

## サポート

問題が発生した場合：

1. [Troubleshooting Guide](../docs/TROUBLESHOOTING.md)を確認
2. Keycloakのログを確認: `docker logs authplatform-keycloak`
3. GitHub Issuesで報告

---

**最終更新**: 2025-10-28
**バージョン**: Phase 1 MVP
