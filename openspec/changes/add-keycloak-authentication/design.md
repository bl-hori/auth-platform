# Keycloak Authentication Server - Design Document

## Context

Auth Platformは現在、Spring Securityのカスタムフィルターを使用したAPI Key認証のみをサポートしています。エンタープライズ顧客からOIDC/OAuth2標準に準拠した認証機能の要望があり、既存の認可機能と統合可能な認証基盤が必要です。

### Current State
- **認証**: API Keyベース（`X-API-Key`ヘッダー）
- **認可**: RBAC + OPA統合
- **ユーザー管理**: PostgreSQLで直接管理
- **マルチテナント**: Organization IDベースで分離

### Stakeholders
- **開発チーム**: 認証・認可の責任分離を明確化したい
- **顧客**: 既存のIdP（Keycloak, Auth0等）との統合を求めている
- **運用チーム**: 標準プロトコル（OIDC）による運用負荷軽減を期待

### Constraints
- 既存のAPI Key認証を使用しているクライアントとの互換性維持
- 最小限の変更で段階的に導入可能な設計
- Docker Composeで簡単にローカル環境構築可能
- パフォーマンス劣化を最小限に抑える（JWT検証 <5ms）

## Goals / Non-Goals

### Goals
1. **標準準拠**: OIDC/OAuth2仕様に準拠した認証基盤の構築
2. **分離アーキテクチャ**: 認証（Keycloak）と認可（Backend）の責任分離
3. **段階的移行**: 既存API Key認証を残しつつJWT認証を追加
4. **開発体験**: docker-compose up -dで全環境が起動
5. **マルチテナント対応**: Organization毎にKeycloak Realmを分離

### Non-Goals
1. カスタム認証フローの実装（Keycloak標準機能を使用）
2. Keycloak自体のカスタマイズ・拡張
3. 本番環境のHA構成・スケーリング設計
4. 既存ユーザーの一括移行（Phase 2で対応）

## Architecture

### System Overview

```
┌─────────────────┐
│   Frontend      │
│   (Next.js)     │
└────────┬────────┘
         │ JWT Token (Authorization: Bearer <token>)
         ↓
┌─────────────────┐         ┌──────────────────┐
│   Backend       │←────────│  backend_auth    │
│  (Spring Boot)  │  OIDC   │   (Keycloak)     │
│                 │ Discovery│                  │
│  - JWT検証      │         │  - 認証・トークン発行│
│  - 認可判定      │         │  - User管理       │
│  - API実装      │         │  - Realm管理      │
└────────┬────────┘         └──────────────────┘
         │
         ↓
┌─────────────────┐
│   PostgreSQL    │
│  - Users        │
│  - Roles        │
│  - Policies     │
└─────────────────┘
```

### Component Design

#### 1. backend_auth (Keycloak Service)

**責任範囲**:
- ユーザー認証（ユーザー名/パスワード、MFA等）
- JWTトークンの発行・リフレッシュ
- Realmとクライアント設定の管理
- OIDC/OAuth2エンドポイントの提供

**ディレクトリ構造**:
```
backend_auth/
├── docker-compose.keycloak.yml   # Keycloak専用compose
├── realms/
│   └── authplatform-realm.json   # Realm設定エクスポート
├── themes/                       # カスタムテーマ（今後）
└── README.md                     # セットアップ手順
```

**Keycloak設定**:
- **Image**: `quay.io/keycloak/keycloak:24.0`
- **Database**: 既存PostgreSQLまたは専用DBを選択可能
- **Port**: `8180` (8080はbackendが使用中)
- **Environment**:
  - `KEYCLOAK_ADMIN=admin`
  - `KEYCLOAK_ADMIN_PASSWORD=<secure-password>`
  - `KC_DB=postgres`
  - `KC_HOSTNAME=localhost`

#### 2. Backend (Spring Boot) - JWT Integration

**新規コンポーネント**:

1. **JwtAuthenticationFilter**
   - `Authorization: Bearer <token>`ヘッダーからJWT抽出
   - Keycloak公開鍵で署名検証
   - ClaimsからユーザーID・Organization IDを抽出
   - Spring SecurityのAuthenticationオブジェクトに変換

2. **KeycloakProperties**
   ```yaml
   authplatform:
     keycloak:
       issuer-uri: http://localhost:8180/realms/authplatform
       jwk-set-uri: http://localhost:8180/realms/authplatform/protocol/openid-connect/certs
       user-info-uri: http://localhost:8180/realms/authplatform/protocol/openid-connect/userinfo
   ```

3. **SecurityConfig更新**
   - API Key認証フィルター（既存）
   - JWT認証フィルター（新規）
   - 両方をサポート（どちらかが成功すれば認証OK）

**認証フロー**:
```
1. Client → Keycloak: ユーザー名/パスワードで認証
2. Keycloak → Client: JWTアクセストークン + リフレッシュトークン
3. Client → Backend: Authorization: Bearer <JWT>
4. Backend: JWT署名検証 → ユーザー特定 → 認可チェック
5. Backend → Client: API Response
```

#### 3. Infrastructure (Docker Compose)

**docker-compose.yml更新**:
```yaml
services:
  # 既存サービス（postgres, redis, opa）

  keycloak:
    image: quay.io/keycloak/keycloak:24.0
    container_name: authplatform-keycloak
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: authplatform
      KC_DB_PASSWORD: authplatform
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    command: start-dev
    ports:
      - "8180:8080"
    depends_on:
      - postgres
    networks:
      - authplatform
```

## Decisions

### Decision 1: Keycloakを選択した理由

**Options Considered**:
1. **Keycloak**: OSSの成熟したIdP、OIDC/SAML対応、高い拡張性
2. **Auth0**: マネージドサービス、簡単だがコスト高、ベンダーロックイン
3. **自作認証サーバ**: フルコントロール可だが開発・運用コスト大

**Decision**: Keycloakを採用

**Rationale**:
- OSS（Apache License 2.0）でベンダーロックインなし
- OIDC/OAuth2/SAMLの完全サポート
- User Federation（LDAP/Active Directory統合）
- Spring Securityとの統合が容易
- プロジェクトの「既存IdPとの統合」要件を満たせる

### Decision 2: backend_authの位置づけ

**Options Considered**:
1. **Infrastructureディレクトリに配置**: 純粋なインフラとして扱う
2. **独立したマイクロサービス**: 将来的な拡張性を考慮
3. **Backendに統合**: 単一アプリケーションとして管理

**Decision**: 独立したマイクロサービス（backend_auth）

**Rationale**:
- 認証と認可の責任分離（Single Responsibility Principle）
- Keycloak特有の設定・カスタマイズを分離管理
- 将来的なスケーリング（Keycloak Clusteringなど）に対応しやすい
- backend_authディレクトリで完結するため、開発者がわかりやすい

### Decision 3: JWT検証方法

**Options Considered**:
1. **毎回Keycloakに問い合わせ**: 最新状態を保証、レイテンシ増
2. **公開鍵キャッシュ + ローカル検証**: 高速、鍵更新時のみ通信
3. **Opaque Token Introspection**: セキュアだが全リクエストで通信発生

**Decision**: 公開鍵キャッシュ + ローカル検証

**Rationale**:
- p95レイテンシ目標 <5ms を達成可能
- Keycloakの公開鍵は頻繁に変わらない（キャッシュ有効）
- Spring Securityのデフォルト動作（JwkSetUri）
- セキュリティ: JWT署名検証は暗号学的に安全

### Decision 4: Realmの分離戦略

**Options Considered**:
1. **Single Realm + Organization属性**: 全組織を1つのRealmで管理
2. **Realm per Organization**: 組織毎に独立したRealm
3. **Hybrid**: 小規模組織は共有、大規模は専用Realm

**Decision**: Phase 1では Single Realm（将来的に Realm per Organization対応）

**Rationale**:
- MVP（Phase 1）では複雑さを避け、1 Realmで十分
- JWT Claims内で`organization_id`を含めて区別
- 将来的にRealm分離も技術的に可能（後方互換性あり）
- 顧客要求に応じて柔軟に変更可能

## Data Model

### JWT Token Structure (Keycloak発行)

```json
{
  "exp": 1698765432,
  "iat": 1698761832,
  "jti": "abc-123-xyz",
  "iss": "http://localhost:8180/realms/authplatform",
  "aud": "auth-platform-backend",
  "sub": "user-uuid-from-keycloak",
  "typ": "Bearer",
  "azp": "auth-platform-frontend",
  "preferred_username": "user@example.com",
  "email": "user@example.com",
  "organization_id": "org-uuid",
  "roles": ["user", "admin"]
}
```

### Backend User Mapping

既存のUsersテーブルに`keycloak_sub`カラムを追加（今後のPhaseで対応）:
```sql
ALTER TABLE users ADD COLUMN keycloak_sub VARCHAR(255) UNIQUE;
```

Phase 1では、JWT Claims内の`email`をキーにユーザーを特定します。

## Migration Plan

### Phase 1: 最小限の統合（このProposal）

**Steps**:
1. ✅ Keycloakコンテナの追加とdocker-compose設定
2. ✅ Realmとクライアント設定の作成
3. ✅ OIDC Discovery動作確認
4. ✅ ドキュメント作成（setup手順、トークン取得方法）

**Rollback**: docker-compose.ymlからkeycloakサービスを削除

### Phase 2: Backend JWT統合（今後）

**Steps**:
1. Spring Security OAuth2 Resource Server依存関係追加
2. JwtAuthenticationFilter実装
3. SecurityConfig更新（API Key + JWT併用）
4. 統合テスト実装
5. Frontend OAuth2フロー実装

**Rollback**: SecurityConfigから JWT認証フィルターを無効化

### Phase 3: レガシー廃止（今後）

**Steps**:
1. 全クライアントのJWT移行完了確認
2. API Key認証を非推奨化（Deprecation Notice）
3. 段階的にAPI Key認証を削除
4. 完全なJWTベース認証へ移行

## Risks / Trade-offs

### Risk 1: Keycloakのバージョンアップ

**Risk**: Keycloak更新時にAPIやデフォルト設定が変わる可能性

**Mitigation**:
- Realm設定をJSON形式でエクスポート・バージョン管理
- Docker Imageのバージョンを固定（`keycloak:24.0`）
- アップグレード時は事前にテスト環境で検証

### Risk 2: JWT検証のパフォーマンス

**Risk**: 全リクエストでJWT検証が発生し、レイテンシ増加の恐れ

**Mitigation**:
- 公開鍵キャッシュにより署名検証を高速化（<5ms）
- 必要に応じてSpring Cacheで検証済みトークンをキャッシュ
- モニタリングで継続的にレイテンシ監視

### Risk 3: API Key認証との併用複雑化

**Risk**: 2つの認証方式が混在し、セキュリティポリシーが複雑化

**Mitigation**:
- 認証方式の優先順位を明確化（JWT優先、フォールバックでAPI Key）
- ドキュメントで推奨方式を明記（新規実装はJWT使用）
- 段階的廃止計画の策定

### Risk 4: Keycloakの運用負荷

**Risk**: Keycloakの運用（バックアップ、HA構成、監査）が複雑

**Mitigation**:
- Phase 1では開発環境のみ、本番運用は今後検討
- Keycloakのデータベースは既存PostgreSQLと統合可能
- マネージドKeycloak（Red Hat SSO）への移行オプション保持

## Performance Considerations

### Expected Latency

| 処理 | 目標レイテンシ | 実装方法 |
|------|---------------|----------|
| JWT署名検証 | <5ms (p95) | 公開鍵キャッシュ + ローカル検証 |
| OIDC Discovery | <100ms | 初回のみ、結果キャッシュ |
| Token発行 (Keycloak) | <200ms | Keycloak標準性能 |
| 認可判定 | <10ms (p95) | 既存のキャッシュ機構を継続使用 |

### Caching Strategy

1. **公開鍵キャッシュ**: Spring SecurityのデフォルトJWK Set取得・キャッシュ
2. **OIDC Discoveryキャッシュ**: アプリケーション起動時に1回取得
3. **既存の認可キャッシュ**: JWT検証後も既存のL1/L2キャッシュを使用

## Security Considerations

### JWT Security

- **署名検証**: RS256（RSA公開鍵暗号）による署名検証
- **有効期限**: アクセストークン 15分、リフレッシュトークン 7日
- **Token Binding**: なし（Phase 2でDPoP検討）
- **クレーム検証**: `iss`, `aud`, `exp`を必須検証

### Secret Management

- **Keycloak管理者パスワード**: 環境変数または外部Secret管理（本番環境）
- **Client Secret**: Publicクライアント（Frontend）はSecretなし
- **Database Password**: 環境変数、Docker Secretsを使用

### Threat Model

| 脅威 | 対策 |
|------|------|
| トークン盗聴 | HTTPS必須（本番環境） |
| トークン改ざん | 署名検証で防止 |
| トークン再利用 | 短期有効期限 + リフレッシュトークンローテーション |
| CSRF | Stateless JWTのため影響なし |

## Testing Strategy

### Phase 1 Testing

1. **手動テスト**:
   - Keycloak管理コンソールへのアクセス
   - OIDC Discovery（`/.well-known/openid-configuration`）
   - Realm設定の動作確認

2. **統合テスト**:
   - Docker Composeでの起動テスト
   - Keycloak Health Check

### Phase 2 Testing (今後)

1. **ユニットテスト**:
   - JWT署名検証ロジック
   - Claims抽出ロジック

2. **統合テスト**:
   - TestcontainersでKeycloakを起動
   - End-to-End認証フロー

3. **パフォーマンステスト**:
   - JWT検証レイテンシ測定
   - スループット測定

## Open Questions

1. **Keycloakのデータベース**: 既存PostgreSQLと共有 or 専用DB？
   - **Recommendation**: Phase 1では共有、本番では専用DBを検討

2. **Custom User Attributes**: Keycloak内でカスタム属性をどう管理？
   - **Recommendation**: User Attributes機能を使用、JWT Claimsに含める

3. **Email検証**: ユーザー登録時のメール検証は必要？
   - **Recommendation**: Phase 1ではスキップ、Phase 2で実装

4. **MFA (Multi-Factor Authentication)**: 対応する？
   - **Recommendation**: Keycloakの標準MFA機能を有効化（Phase 2以降）

5. **User Migration**: 既存ユーザーの移行方法は？
   - **Recommendation**: Phase 2でUser Federation or 一括インポートスクリプト

## References

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [OIDC Core Specification](https://openid.net/specs/openid-connect-core-1_0.html)
- [RFC 7519 - JWT](https://datatracker.ietf.org/doc/html/rfc7519)
- [RFC 6749 - OAuth 2.0](https://datatracker.ietf.org/doc/html/rfc6749)
