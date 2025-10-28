# Proposal: integrate-jwt-validation

## Overview

Phase 1で構築したKeycloak認証サーバーと統合し、BackendでJWTトークンの検証機能を実装します。Spring Security OAuth2 Resource Serverを使用してKeycloakが発行したJWTを検証し、既存のAPI Key認証と併用可能なハイブリッド認証システムを構築します。

## Problem

現在、Auth PlatformはAPI Key認証のみをサポートしています。Phase 1でKeycloak認証サーバーの基盤は構築されましたが、BackendはまだJWTトークンを検証できません。

### Current Limitations
1. **JWT検証機能の欠如**: Keycloakが発行したJWTを検証する仕組みがない
2. **認証方式の選択肢が限定的**: API Keyのみで、標準的なOAuth2/OIDCフローが使えない
3. **段階的移行の困難**: 既存クライアントの移行パスが不明確
4. **ユーザー情報の分断**: KeycloakのユーザーとBackendのUsersテーブルが連携していない

### Impact
- エンタープライズ顧客が要求するOIDC/OAuth2標準に対応できない
- SaaS型のフロントエンドアプリケーションとの統合が困難
- 既存のAPI Keyクライアントとの互換性を保ちながらJWTに移行できない

## Solution

Spring Security OAuth2 Resource Serverを使用して、BackendにJWT検証機能を実装します。

### Key Features
1. **JWT検証フィルター**: `Authorization: Bearer <token>`ヘッダーからJWTを抽出・検証
2. **ハイブリッド認証**: API KeyとJWT、どちらでも認証可能
3. **Keycloak連携**: OIDC DiscoveryとJWK Set URIで公開鍵を自動取得
4. **ユーザーマッピング**: JWT Claims（email, sub）から既存Usersテーブルと連携
5. **組織分離**: JWT内の`organization_id` claimでマルチテナント対応

### Architecture

```
┌─────────────────┐
│  API Request    │
└────────┬────────┘
         │
         ↓
┌─────────────────────────────────────┐
│  Authentication Filter Chain        │
│                                     │
│  1. RateLimitFilter                 │
│  2. JWT Authentication Filter (NEW) │ ← Phase 2で追加
│  3. API Key Authentication Filter   │ ← 既存
│                                     │
└────────┬────────────────────────────┘
         │
         ↓ Authentication Success
┌─────────────────┐
│  Authorization  │
│  (RBAC + OPA)   │
└─────────────────┘
```

### Implementation Approach

1. **Dependency追加**: Spring Security OAuth2 Resource Server
2. **JwtAuthenticationFilter実装**: JWT抽出・検証・Claims解析
3. **SecurityConfig更新**: JWTフィルターを認証チェーンに追加
4. **UserService拡張**: JWT Claimsから既存ユーザーを検索・作成
5. **包括的テスト**: ユニット・統合・E2E・パフォーマンステスト

## Capabilities Impacted

### New Capabilities
- **jwt-authentication**: JWT based authentication capability (NEW)

### Modified Capabilities
- **user-identity**: ユーザー情報にKeycloak連携を追加（keycloak_sub カラム追加）
- **authorization-core**: JWT認証を考慮した認可判定の更新

### Related Capabilities
- Keycloak認証サーバー（Phase 1で実装済み）

## Phases

### Phase 2 (This Proposal): Backend JWT検証統合
- Spring Security OAuth2 Resource Server統合
- JWT検証フィルター実装
- ハイブリッド認証（API Key + JWT）
- Usersテーブルとの連携
- 包括的テスト（ユニット・統合・E2E・パフォーマンス）

**Timeline**: 2-3週間

### Phase 3 (Future): Frontend OAuth2フロー統合
- Next.js / NextAuth.js統合
- Authorization Code Flow + PKCE実装
- セッション管理
- トークンリフレッシュ

**Timeline**: 2-3週間（Phase 2完了後）

### Phase 4 (Future): API Key段階的廃止
- JWT移行完了の監視
- API Key非推奨化アナウンス
- 段階的なAPI Key認証削除

**Timeline**: 3-6ヶ月（Phase 3完了後）

## Success Criteria

### Functional
- ✅ Keycloakが発行したJWTトークンでBackend APIにアクセス可能
- ✅ API Key認証も引き続き動作（後方互換性）
- ✅ JWT Claims内の`organization_id`でマルチテナント分離
- ✅ JWT検証失敗時は適切なエラーレスポンス（401 Unauthorized）

### Non-Functional
- ✅ JWT検証レイテンシ <5ms (p95)
- ✅ 既存API Keyクライアントに影響なし（0ダウンタイム）
- ✅ テストカバレッジ 85%以上
- ✅ パフォーマンステストで10,000+ req/s達成

### Documentation
- ✅ JWT認証の使用方法ドキュメント
- ✅ curl/Postmanサンプル
- ✅ トラブルシューティングガイド
- ✅ API Key→JWT移行ガイド

## Risks and Mitigations

### Risk 1: JWT検証のパフォーマンス劣化
**Mitigation**: 公開鍵キャッシュによるローカル検証で<5ms達成。必要に応じて検証済みトークンもキャッシュ。

### Risk 2: 既存API Keyクライアントへの影響
**Mitigation**: 認証フィルターの順序を慎重に設定。統合テストで両方の認証方式を検証。

### Risk 3: ユーザー情報の同期問題
**Mitigation**: JWT Claims優先、DB検索はフォールバック。ユーザー作成は自動化し、同期ずれを防止。

### Risk 4: Keycloak公開鍵のローテーション
**Mitigation**: Spring SecurityのJWK Set URIキャッシュが自動更新。キャッシュTTLを1時間に設定。

## Open Questions

1. **JWT Claims優先 vs DB情報優先**: ユーザーロールをJWT Claimsで扱うか、DBで管理するか？
   - **Recommendation**: JWT Claimsを優先し、DBはフォールバックとして使用

2. **ユーザー自動作成**: JWT検証成功時に未登録ユーザーを自動作成するか？
   - **Recommendation**: 初回JWT認証時に自動作成（Just-In-Time Provisioning）

3. **トークンリボケーション**: JWTの失効をどう扱うか？
   - **Recommendation**: Phase 2ではスコープ外。Phase 3でToken Introspection検討

4. **組織の自動作成**: JWT内のorganization_idが未登録の場合、自動作成するか？
   - **Recommendation**: Phase 2ではエラーとする。Phase 3で管理画面から作成

## Dependencies

### Internal
- Phase 1: Keycloak認証サーバー（完了 ✅）
- 既存のSecurityConfig, ApiKeyAuthenticationFilter

### External
- Keycloak 24.0（稼働中）
- Spring Security OAuth2 Resource Server 6.2.x
- Nimbus JOSE+JWT 9.37.3

## Alternatives Considered

### Alternative 1: Opaque Token Introspection
**Pros**: 常にKeycloakの最新状態を参照、トークンリボケーション対応
**Cons**: 全リクエストでKeycloakへの通信が発生し、レイテンシ増加（p95 >50ms）
**Decision**: 却下（パフォーマンス要件を満たせない）

### Alternative 2: Gateway層でのJWT検証
**Pros**: Backend実装がシンプル、認証をGatewayに集約
**Cons**: 現時点でGatewayが存在しない、Phase 2の範囲を超える
**Decision**: 将来的に検討（Phase 3以降）

### Alternative 3: JWT検証のみ（API Key廃止）
**Pros**: 認証方式が一本化され、シンプル
**Cons**: 既存クライアントとの互換性が失われる、移行リスク大
**Decision**: 却下（段階的移行を優先）

## References

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [RFC 7519 - JWT](https://datatracker.ietf.org/doc/html/rfc7519)
- [OIDC Core Specification](https://openid.net/specs/openid-connect-core-1_0.html)
- Phase 1 Design: `openspec/changes/add-keycloak-authentication/design.md`
- Keycloak Integration Guide: `docs/KEYCLOAK_INTEGRATION.md`

---

**Author**: Auth Platform Team
**Created**: 2025-10-28
**Status**: Draft
