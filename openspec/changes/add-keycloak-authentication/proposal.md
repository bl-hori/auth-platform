# Keycloak Authentication Server Implementation

## Why

現在のAuth Platformは認可（Authorization）に特化しているが、認証（Authentication）機能が不足しています。エンタープライズ環境では、OIDC/OAuth2標準に準拠した統一的な認証基盤が必要とされています。Keycloakを認証サーバとして導入することで、既存のAPI Key認証から業界標準のJWTトークンベース認証への段階的な移行を実現し、マルチテナント対応の本格的な認証・認可プラットフォームを構築します。

## What Changes

### 新規実装
- **backend_auth**: Keycloak専用マイクロサービスの構築
  - Keycloak 24.0+コンテナの配備と設定
  - Realm設定（Organization毎にRealm作成）
  - OIDC/OAuth2エンドポイント設定
  - Client設定（Frontend, Backend用）

- **backend**: OIDC統合とJWT検証機能の追加
  - Spring Security OAuth2 Resource Server統合
  - JWT検証フィルターの実装
  - API Key認証との併用サポート
  - Keycloak公開鍵取得・キャッシング機構

- **infrastructure**: Docker Compose設定の拡張
  - Keycloakサービス追加
  - PostgreSQL共有またはKeycloak専用DB設定
  - ネットワーク設定の更新

### 段階的な移行戦略
1. **Phase 1 (このProposal)**: 最小限のKeycloak統合
   - Keycloakコンテナの起動と基本設定
   - OIDC Discovery設定
   - 基本的なRealm/Client設定

2. **Phase 2 (今後)**: 完全な認証フロー統合
   - JWT検証のbackend統合
   - Frontend OAuth2フロー実装
   - User Federation（既存ユーザーとの統合）

3. **Phase 3 (今後)**: レガシー移行
   - API Key認証の段階的廃止
   - 完全なJWTベース認証への移行

### 影響を受けるコンポーネント
- **backend/**: SecurityConfig、新規JwtAuthenticationFilter
- **backend_auth/**: 新規ディレクトリ（Keycloak設定・管理）
- **infrastructure/**: docker-compose.yml更新
- **docs/**: 認証フロー・設定ガイドの追加

### Breaking Changes
- なし（既存API Key認証は維持、JWT認証を追加オプションとして提供）

## Impact

### Affected Specs
- **user-authentication** (新規capability): Keycloak統合、OIDC認証、JWT検証
- **user-identity** (既存): SCIM同期機能との統合検討（今後のPhaseで対応）

### Affected Code
- `backend/src/main/java/io/authplatform/platform/config/SecurityConfig.java`
- `backend/src/main/resources/application.yml`
- `infrastructure/docker-compose.yml`
- `backend_auth/` (新規ディレクトリ全体)

### Performance Impact
- 認証レイテンシ: JWT検証は署名検証のみで <5ms (p95)
- Keycloakへの通信: 公開鍵キャッシュにより初回のみ発生

### Security Considerations
- JWT署名検証による堅牢な認証
- トークン有効期限管理（短期アクセストークン + リフレッシュトークン）
- 既存のマルチテナント分離は維持

### Testing Requirements
- Keycloakコンテナの起動・接続テスト
- OIDC Discovery動作確認
- JWT署名検証ユニットテスト
- 統合テスト（Keycloak + Backend）は今後のPhaseで実装

## Success Criteria

1. ✅ Keycloakコンテナが起動し、管理コンソールにアクセス可能
2. ✅ OIDC Discovery エンドポイントが正常に応答
3. ✅ 最低1つのRealmとClientが設定済み
4. ✅ docker-compose up -dで全サービスが正常起動
5. ✅ ドキュメントにKeycloak設定手順が記載

## Non-Goals (Out of Scope)

- Frontend OAuth2フロー実装（Phase 2で対応）
- 既存User IdentityとのFederation統合（Phase 2で対応）
- SCIM同期との統合（Phase 2で対応）
- API Key認証の廃止（Phase 3で検討）
- 本番環境用のKeycloak HA構成（今後の運用フェーズ）
