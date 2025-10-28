# Keycloak Realm設定ガイド

このガイドでは、`authplatform` realmの設定手順を説明します。

## 自動インポート

`authplatform-realm.json`は、Keycloak起動時に自動的にインポートされます。

```bash
cd infrastructure
docker compose up -d
```

Keycloakが起動すると、以下が自動設定されます：

- ✅ Realm: `authplatform`
- ✅ Client: `auth-platform-backend` (bearer-only)
- ✅ Client: `auth-platform-frontend` (public)
- ✅ Client Scope: `organization` (organization_id claim)
- ✅ Roles: `user`, `admin`
- ✅ Test Users: `testuser`, `admin`

## 管理コンソールでの確認

### 1. 管理コンソールにアクセス

**URL**: http://localhost:8180

**認証情報**:
- Username: `admin`
- Password: `admin`

### 2. Realmの選択

左上のドロップダウンから`authplatform` realmを選択します。

### 3. Client設定の確認

#### Backend Client: auth-platform-backend

**左メニュー → Clients → auth-platform-backend**

**設定確認**:
- **Client ID**: `auth-platform-backend`
- **Enabled**: ON
- **Client authentication**: ON
- **Authorization**: OFF
- **Access Type**: bearer-only ✅

**Valid redirect URIs**:
```
http://localhost:8080/*
```

**Web Origins**:
```
http://localhost:8080
```

#### Frontend Client: auth-platform-frontend

**左メニュー → Clients → auth-platform-frontend**

**設定確認**:
- **Client ID**: `auth-platform-frontend`
- **Enabled**: ON
- **Client authentication**: OFF (Public client)
- **Standard Flow Enabled**: ON
- **Direct Access Grants Enabled**: ON (開発用、本番では無効化推奨)
- **Implicit Flow Enabled**: OFF

**Valid redirect URIs**:
```
http://localhost:3000/*
http://localhost:3000/api/auth/callback/*
```

**Web Origins**:
```
http://localhost:3000
```

**Advanced Settings**:
- **Proof Key for Code Exchange Code Challenge Method**: S256 ✅

### 4. Client Scope設定の確認

**左メニュー → Client scopes → organization**

**Mappers → organization_id**:

| 設定項目 | 値 |
|---------|---|
| Name | `organization_id` |
| Mapper Type | User Attribute |
| User Attribute | `organization_id` |
| Token Claim Name | `organization_id` |
| Claim JSON Type | String |
| Add to ID token | ON |
| Add to access token | ON |
| Add to userinfo | ON |

### 5. Token設定の確認

**左メニュー → Realm settings → Tokens**

| 設定項目 | 値 | 説明 |
|---------|---|------|
| Access Token Lifespan | 15 minutes (900秒) | アクセストークン有効期限 |
| Refresh Token Max Reuse | 0 | リフレッシュトークン再利用回数 |
| SSO Session Idle | 1 hour (3600秒) | アイドルセッションタイムアウト |
| SSO Session Max | 24 hours (86400秒) | 最大セッション時間 |
| Offline Session Idle | 30 days | オフラインセッション有効期限 |

### 6. テストユーザーの確認

**左メニュー → Users**

#### testuser

- **Username**: `testuser`
- **Email**: `testuser@example.com`
- **Email Verified**: YES
- **Enabled**: YES

**Attributes**:
```
organization_id: org-001
```

**Credentials**:
- Password: `Password123!`

**Role Mappings**:
- `user` (Realm Role)

#### admin

- **Username**: `admin`
- **Email**: `admin@example.com`
- **Email Verified**: YES
- **Enabled**: YES

**Attributes**:
```
organization_id: org-001
```

**Credentials**:
- Password: `Admin123!`

**Role Mappings**:
- `user` (Realm Role)
- `admin` (Realm Role)

## トークン取得テスト

### テストユーザーでトークン取得

```bash
curl -X POST http://localhost:8180/realms/authplatform/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=auth-platform-frontend" \
  -d "grant_type=password" \
  -d "username=testuser@example.com" \
  -d "password=Password123!" \
  | jq
```

**期待されるレスポンス**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 900,
  "refresh_expires_in": 604800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "not-before-policy": 0,
  "session_state": "...",
  "scope": "email profile"
}
```

### トークンのデコード

```bash
# Access tokenをデコード
echo "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." | \
  cut -d'.' -f2 | base64 -d 2>/dev/null | jq
```

**期待されるClaims**:
```json
{
  "exp": 1698765432,
  "iat": 1698761832,
  "jti": "abc-123-xyz",
  "iss": "http://localhost:8180/realms/authplatform",
  "aud": "auth-platform-backend",
  "sub": "user-uuid",
  "typ": "Bearer",
  "azp": "auth-platform-frontend",
  "preferred_username": "testuser@example.com",
  "email": "testuser@example.com",
  "email_verified": true,
  "organization_id": "org-001",
  "realm_access": {
    "roles": ["user"]
  }
}
```

✅ **重要**: `organization_id` claimが含まれていることを確認してください。

## カスタム設定（オプション）

### 追加のorganization_idを持つユーザー作成

1. **左メニュー → Users → Add user**
2. Username, Email, First name, Last nameを入力
3. **Email verified**: ON
4. **Save**
5. **Credentials** タブでパスワードを設定
6. **Attributes** タブで以下を追加:
   - Key: `organization_id`
   - Value: `org-002` (任意の組織ID)
7. **Role mappings** タブでRoleを割り当て

### パスワードポリシーの強化

**左メニュー → Authentication → Policies → Password Policy**

推奨設定:
- **Minimum Length**: 12
- **Uppercase Characters**: 1
- **Lowercase Characters**: 1
- **Digits**: 1
- **Special Characters**: 1
- **Not Recently Used**: 5
- **Password Blacklist**: ON

### Brute Force Detection

**左メニュー → Realm settings → Security defenses → Brute force detection**

設定:
- **Enabled**: ON
- **Permanent Lockout**: OFF (開発環境)
- **Max Login Failures**: 5
- **Wait Increment Seconds**: 60
- **Quick Login Check Milliseconds**: 1000
- **Minimum Quick Login Wait Seconds**: 60
- **Max Wait**: 900 seconds

## Realm設定のエクスポート

設定を変更した場合、realmをエクスポートして`authplatform-realm.json`を更新します。

### 管理コンソールからエクスポート

1. **左メニュー → Realm settings**
2. **Action → Partial export**
3. 以下をチェック:
   - ✅ Export groups and roles
   - ✅ Export clients
4. **Export** ボタンをクリック
5. ダウンロードしたJSONを`backend_auth/realms/authplatform-realm.json`に上書き

⚠️ **注意**: エクスポートされたJSONからclient secretsやパスワードは削除されます。

### コマンドラインからエクスポート（オプション）

```bash
docker exec -it authplatform-keycloak /opt/keycloak/bin/kc.sh export \
  --realm authplatform \
  --file /opt/keycloak/data/export/authplatform-realm.json \
  --users realm_file
```

## トラブルシューティング

### Realm設定がインポートされない

**症状**: Keycloak起動後、authplatform realmが存在しない

**原因**:
- `backend_auth/realms/authplatform-realm.json`が存在しない
- JSONファイルのフォーマットエラー

**解決策**:
1. JSONファイルが存在することを確認:
   ```bash
   ls -la backend_auth/realms/
   ```

2. JSONが有効か確認:
   ```bash
   cat backend_auth/realms/authplatform-realm.json | jq
   ```

3. Keycloakを再起動:
   ```bash
   docker compose restart keycloak
   docker logs -f authplatform-keycloak
   ```

### organization_id claimが含まれない

**症状**: JWT tokenに`organization_id` claimがない

**原因**: Client scopeが正しく設定されていない

**解決策**:
1. **Client scopes → organization** が存在するか確認
2. **Clients → auth-platform-frontend → Client scopes** タブ
3. `organization` scopeが **Default** に追加されているか確認
4. 追加されていない場合、**Add client scope → organization → Add → Default**

### トークン取得時に401エラー

**症状**: `curl`でトークン取得時に401 Unauthorized

**原因**:
- ユーザーが存在しない
- パスワードが間違っている
- Client IDが間違っている

**解決策**:
1. ユーザーが存在するか確認:
   ```bash
   # 管理コンソール → Users → View all users
   ```

2. パスワードをリセット:
   ```bash
   # ユーザー選択 → Credentials → Reset password
   ```

3. Client IDを確認:
   ```bash
   # Clients → 一覧から確認
   ```

## 次のステップ

Realm設定が完了したら、次のPRでOIDC DiscoveryとToken Configurationを検証します。

- **PR #3**: OIDC Discovery & Token Configuration
- **PR #4**: Backend Configuration Prep

---

**最終更新**: 2025-10-28
**バージョン**: Phase 1 MVP
