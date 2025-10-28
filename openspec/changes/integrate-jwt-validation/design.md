# JWT Validation Integration - Design Document

## Context

Phase 1でKeycloak認証サーバーの基盤を構築しました。Phase 2では、BackendにJWT検証機能を実装し、Keycloakが発行したJWTトークンを使用してAPIにアクセスできるようにします。

### Current State (Phase 1完了時点)
- **認証サーバー**: Keycloak 24.0が稼働中（ポート8180）
- **Realm**: authplatform realm設定済み
- **Clients**: auth-platform-backend (bearer-only), auth-platform-frontend (public)
- **JWT発行**: Keycloakが正常にJWTトークンを発行
- **Backend認証**: API Keyのみサポート（JWT検証機能なし）

### Stakeholders
- **開発チーム**: 標準的なOAuth2/OIDC認証を実装したい
- **既存クライアント**: API Key認証を継続使用したい
- **新規クライアント**: JWT認証を使用したい
- **運用チーム**: シームレスな移行と監視を求めている

### Constraints
- 既存API Keyクライアントとの100%後方互換性
- JWT検証レイテンシ <5ms (p95)
- 既存Usersテーブルスキーマへの最小限の変更
- ゼロダウンタイムでのデプロイ

## Goals / Non-Goals

### Goals
1. **JWT検証機能の実装**: Keycloak発行のJWTを検証
2. **ハイブリッド認証**: API KeyとJWT両方をサポート
3. **ユーザー情報統合**: JWT ClaimsとUsersテーブルを連携
4. **パフォーマンス維持**: JWT検証でも<5ms (p95)を達成
5. **包括的テスト**: ユニット・統合・E2E・パフォーマンステスト

### Non-Goals
1. Frontend OAuth2フロー実装（Phase 3で対応）
2. トークンリボケーション機能（将来的に検討）
3. API Key認証の廃止（Phase 4で対応）
4. Gateway層でのJWT検証（現時点でGateway未導入）

## Architecture

### High-Level Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    Client Request                        │
│                                                          │
│  Option 1: X-API-Key: <api-key>                         │
│  Option 2: Authorization: Bearer <jwt-token>            │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ↓
┌──────────────────────────────────────────────────────────┐
│              Spring Security Filter Chain                │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │ 1. RateLimitFilter                             │    │
│  │    - IP-based rate limiting                    │    │
│  └────────────────────────────────────────────────┘    │
│                         ↓                               │
│  ┌────────────────────────────────────────────────┐    │
│  │ 2. JwtAuthenticationFilter (NEW)               │    │
│  │    - Extract JWT from Authorization header     │    │
│  │    - Validate signature with Keycloak JWK Set │    │
│  │    - Extract claims (sub, email, org_id)      │    │
│  │    - Load/Create user in Users table          │    │
│  │    - Set SecurityContext                       │    │
│  └────────────────────────────────────────────────┘    │
│                         ↓ (if JWT not present)         │
│  ┌────────────────────────────────────────────────┐    │
│  │ 3. ApiKeyAuthenticationFilter (Existing)       │    │
│  │    - Extract API Key from X-API-Key header    │    │
│  │    - Validate against configured keys         │    │
│  │    - Set SecurityContext with organization    │    │
│  └────────────────────────────────────────────────┘    │
│                         ↓                               │
│  ┌────────────────────────────────────────────────┐    │
│  │ Authentication Success                         │    │
│  │   SecurityContext contains:                    │    │
│  │   - User ID (from JWT sub or API Key)        │    │
│  │   - Organization ID                           │    │
│  │   - Authorities (roles)                       │    │
│  └────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────┘
                         │
                         ↓
┌──────────────────────────────────────────────────────────┐
│              Authorization Check (Existing)              │
│                                                          │
│  - RBAC (Role-Based Access Control)                     │
│  - OPA Policy Evaluation                                │
│  - Organization Isolation                               │
└──────────────────────────────────────────────────────────┘
                         │
                         ↓
┌──────────────────────────────────────────────────────────┐
│                   API Controller                         │
└──────────────────────────────────────────────────────────┘
```

### Component Design

#### 1. JwtAuthenticationFilter (新規実装)

**責任範囲**:
- `Authorization: Bearer <token>`ヘッダーからJWT抽出
- JWT署名検証（Keycloak公開鍵使用）
- JWT Claims解析（sub, email, organization_id, roles）
- Usersテーブルからユーザー情報取得または作成
- Spring SecurityのAuthenticationオブジェクト設定

**実装クラス**:
```java
package io.authplatform.platform.security;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final UserService userService;
    private final KeycloakProperties keycloakProperties;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. JWT抽出
        String jwt = extractJwtFromHeader(request);
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return; // API Keyフィルターにフォールバック
        }

        try {
            // 2. JWT検証
            Jwt decodedJwt = jwtDecoder.decode(jwt);

            // 3. Claims抽出
            String subject = decodedJwt.getSubject();
            String email = decodedJwt.getClaimAsString("email");
            String organizationId = decodedJwt.getClaimAsString("organization_id");
            List<String> roles = decodedJwt.getClaimAsStringList("roles");

            // 4. ユーザー情報取得または作成（Just-In-Time Provisioning）
            User user = userService.findOrCreateFromJwt(subject, email, organizationId);

            // 5. Authentication設定
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                user.getId(),
                organizationId,
                roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList()),
                decodedJwt
            );
            authentication.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (JwtException e) {
            logger.error("JWT validation failed", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
        }
    }

    private String extractJwtFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

#### 2. JwtDecoder Configuration (新規実装)

**責任範囲**:
- Keycloak JWK Set URIから公開鍵を取得
- JWT署名検証（RS256）
- Claims検証（iss, aud, exp）

**実装クラス**:
```java
package io.authplatform.platform.config;

@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(KeycloakProperties keycloakProperties) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
            .withJwkSetUri(keycloakProperties.getJwkSetUri())
            .build();

        // Issuer検証
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            new JwtIssuerValidator(keycloakProperties.getIssuerUri()),
            new JwtClaimValidator<String>("aud", aud ->
                aud != null && aud.contains("auth-platform-backend")
            )
        ));

        return jwtDecoder;
    }
}
```

#### 3. KeycloakProperties (新規実装)

**責任範囲**:
- Keycloak接続情報の管理
- application.ymlからの設定読み込み

**実装クラス**:
```java
package io.authplatform.platform.config;

@Configuration
@ConfigurationProperties(prefix = "authplatform.keycloak")
public class KeycloakProperties {

    private boolean enabled = false;
    private String baseUrl;
    private String realm;
    private String issuerUri;
    private String jwkSetUri;
    private JwtValidationSettings jwt = new JwtValidationSettings();

    public static class JwtValidationSettings {
        private int publicKeyCacheTtl = 3600; // 1 hour
        private int clockSkewSeconds = 30;
        private String expectedAudience = "auth-platform-backend";

        // getters/setters
    }

    // getters/setters
}
```

#### 4. UserService拡張 (既存クラスの更新)

**新規メソッド**:
```java
package io.authplatform.platform.service;

@Service
public class UserService {

    // 既存メソッド...

    /**
     * JWTからユーザーを検索または作成（Just-In-Time Provisioning）
     */
    @Transactional
    public User findOrCreateFromJwt(String keycloakSub, String email, String organizationId) {
        // 1. keycloak_subで検索
        Optional<User> userBySub = userRepository.findByKeycloakSub(keycloakSub);
        if (userBySub.isPresent()) {
            return userBySub.get();
        }

        // 2. emailで検索（既存ユーザーとの紐付け）
        Optional<User> userByEmail = userRepository.findByEmail(email);
        if (userByEmail.isPresent()) {
            User user = userByEmail.get();
            // keycloak_subを更新
            user.setKeycloakSub(keycloakSub);
            return userRepository.save(user);
        }

        // 3. 新規ユーザー作成
        User newUser = new User();
        newUser.setKeycloakSub(keycloakSub);
        newUser.setEmail(email);
        newUser.setOrganizationId(UUID.fromString(organizationId));
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setCreatedAt(Instant.now());

        return userRepository.save(newUser);
    }
}
```

#### 5. SecurityConfig更新 (既存クラスの更新)

**変更点**:
- JwtAuthenticationFilterを認証チェーンに追加
- フィルター順序: RateLimit → JWT → API Key

**実装**:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ApiKeyProperties apiKeyProperties;
    private final KeycloakProperties keycloakProperties;
    private final RateLimitFilter rateLimitFilter;
    private final JwtDecoder jwtDecoder;
    private final UserService userService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // Filter順序: RateLimit → JWT → API Key
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtAuthenticationFilter(), RateLimitFilter.class)
            .addFilterAfter(apiKeyAuthenticationFilter(), JwtAuthenticationFilter.class)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtDecoder, userService, keycloakProperties);
    }

    @Bean
    public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter() {
        return new ApiKeyAuthenticationFilter(apiKeyProperties);
    }
}
```

### Database Schema Changes

#### Users テーブル拡張

**新規カラム**:
```sql
ALTER TABLE users
ADD COLUMN keycloak_sub VARCHAR(255) UNIQUE,
ADD COLUMN keycloak_synced_at TIMESTAMP;

CREATE INDEX idx_users_keycloak_sub ON users(keycloak_sub);

COMMENT ON COLUMN users.keycloak_sub IS 'Keycloak user ID (sub claim from JWT)';
COMMENT ON COLUMN users.keycloak_synced_at IS 'Last sync timestamp with Keycloak';
```

**マイグレーション戦略**:
- 既存ユーザーの`keycloak_sub`はNULL許可
- JWT認証時に初めて`keycloak_sub`が設定される
- emailをキーに既存ユーザーと新規JWTユーザーを紐付け

## Decisions

### Decision 1: JWT検証方法

**Options Considered**:
1. **Local Validation (公開鍵キャッシュ)**: JWK Set URIから公開鍵取得、ローカルで検証
2. **Token Introspection**: 毎回Keycloakに問い合わせ
3. **Hybrid**: 初回はIntrospection、以降はローカル

**Decision**: Local Validation (公開鍵キャッシュ)

**Rationale**:
- p95レイテンシ<5msを達成可能（Introspectionは>50ms）
- Keycloakの公開鍵は頻繁に変わらない（キャッシュ有効）
- Spring SecurityのNimbusJwtDecoderがデフォルトサポート
- トークンリボケーションはPhase 3で検討

### Decision 2: 認証フィルター順序

**Options Considered**:
1. **JWT優先**: JWT → API Key
2. **API Key優先**: API Key → JWT
3. **並列処理**: 両方同時にチェック

**Decision**: JWT優先（JWT → API Key）

**Rationale**:
- 新しい認証方式（JWT）を優先
- JWT検証失敗時のみAPI Keyにフォールバック
- 段階的移行を促進（JWTを推奨）
- パフォーマンス: JWTの方が高速（<5ms vs API Key DB lookup）

### Decision 3: ユーザー情報マッピング

**Options Considered**:
1. **JWT Claims Only**: DBを参照せず、Claims情報のみ使用
2. **DB連携 (JIT Provisioning)**: JWT初回認証時にユーザー自動作成
3. **Manual Registration**: 事前にユーザー登録必須

**Decision**: DB連携（Just-In-Time Provisioning）

**Rationale**:
- 既存のUsersテーブルと整合性を保つ
- RBAC/ABACで既存の認可ロジックを再利用
- ユーザー登録の手間を削減（自動化）
- Keycloak ↔ Backend間でユーザー情報を同期

### Decision 4: Organization検証

**Options Considered**:
1. **自動作成**: JWT内のorganization_idが未登録なら自動作成
2. **エラー**: 未登録organization_idはエラー
3. **デフォルト組織**: 特定のデフォルト組織に割り当て

**Decision**: エラー（未登録organization_idは認証失敗）

**Rationale**:
- セキュリティ: 不正な組織IDでのアクセスを防止
- データ整合性: 組織は事前に管理画面から作成
- Phase 3で組織自動作成機能を検討

## Data Model

### JWT Token Structure (Keycloak発行)

```json
{
  "exp": 1698765432,
  "iat": 1698761832,
  "jti": "abc-123-xyz",
  "iss": "http://localhost:8180/realms/authplatform",
  "aud": "auth-platform-backend",
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "typ": "Bearer",
  "azp": "auth-platform-frontend",
  "preferred_username": "user@example.com",
  "email": "user@example.com",
  "email_verified": true,
  "organization_id": "org-uuid-12345",
  "roles": ["user", "admin"]
}
```

### JwtAuthenticationToken (Spring Security)

```java
public class JwtAuthenticationToken extends AbstractAuthenticationToken {
    private final UUID userId;
    private final String organizationId;
    private final Jwt jwt;

    public JwtAuthenticationToken(
        UUID userId,
        String organizationId,
        Collection<? extends GrantedAuthority> authorities,
        Jwt jwt
    ) {
        super(authorities);
        this.userId = userId;
        this.organizationId = organizationId;
        this.jwt = jwt;
    }

    @Override
    public Object getCredentials() {
        return jwt;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    public String getOrganizationId() {
        return organizationId;
    }
}
```

## Performance Considerations

### JWT Validation Latency

| 処理ステップ | 目標レイテンシ | 実装方法 |
|------------|--------------|---------|
| JWT抽出 | <1ms | ヘッダー文字列操作のみ |
| 署名検証 | <3ms | 公開鍵キャッシュ（Nimbus JOSE+JWT） |
| Claims解析 | <1ms | JSONパース（Jackson） |
| DB検索（ユーザー） | <2ms | keycloak_subインデックス使用 |
| **合計** | **<5ms (p95)** | |

### Caching Strategy

1. **公開鍵キャッシュ**:
   - NimbusJwtDecoderのデフォルトキャッシュ（5分）
   - 設定で1時間に延長可能
   - Kid（Key ID）ごとにキャッシュ

2. **ユーザー情報キャッシュ** (オプション):
   - Spring Cache（Caffeine）で検索結果キャッシュ
   - TTL: 5分
   - キャッシュキー: `keycloak_sub`

3. **組織情報キャッシュ** (既存):
   - 既存のL1/L2キャッシュを継続使用

### Load Testing Targets

| メトリクス | 目標値 |
|----------|-------|
| スループット | >10,000 req/s |
| JWT認証レイテンシ (p50) | <3ms |
| JWT認証レイテンシ (p95) | <5ms |
| JWT認証レイテンシ (p99) | <10ms |
| エラー率 | <0.1% |

## Security Considerations

### JWT Validation

**必須検証項目**:
1. **署名検証**: RS256公開鍵で署名検証
2. **Issuer (iss)**: `http://localhost:8180/realms/authplatform`を検証
3. **Audience (aud)**: `auth-platform-backend`を含むか検証
4. **Expiration (exp)**: トークン有効期限を検証
5. **Not Before (nbf)**: トークンの使用開始時刻を検証

**オプション検証**:
- Clock Skew: 30秒の許容誤差
- Custom Claims: `organization_id`, `roles`の存在チェック

### Threat Model

| 脅威 | 対策 | 実装 |
|-----|------|-----|
| トークン盗聴 | HTTPS必須 | 本番環境でTLS 1.3+ |
| トークン改ざん | 署名検証 | RS256検証 |
| トークン再利用 | 短期有効期限 | Access: 15分, Refresh: 7日 |
| リプレイ攻撃 | jti（JWT ID）チェック | Phase 3で検討 |
| CSRF | Stateless JWT | 影響なし |
| XSS | HttpOnly Cookie | Phase 3 (Frontend)で対応 |

### Secret Management

- **Keycloak公開鍵**: JWK Set URIから自動取得（シークレット不要）
- **API Key**: 環境変数（既存）
- **Database Password**: 環境変数（既存）

## Testing Strategy

### Unit Tests

**対象**:
1. `JwtAuthenticationFilter`
   - JWT抽出ロジック
   - 署名検証失敗時のエラーハンドリング
   - Claims解析ロジック

2. `UserService.findOrCreateFromJwt()`
   - 新規ユーザー作成
   - 既存ユーザー（keycloak_sub）の検索
   - 既存ユーザー（email）との紐付け

3. `JwtAuthenticationToken`
   - Authentication設定
   - Authorities（roles）変換

**カバレッジ目標**: 90%以上

### Integration Tests

**対象**:
1. **Keycloak統合テスト**:
   - Testcontainersでkeycloakコンテナ起動
   - 実際のJWT発行・検証フロー
   - OIDC Discovery動作確認

2. **認証フィルターチェーン**:
   - JWT認証成功パス
   - JWT認証失敗 → API Keyフォールバック
   - 両方失敗時の401エラー

3. **ユーザープロビジョニング**:
   - JWT初回認証でユーザー自動作成
   - 2回目以降はDB検索のみ

**テストケース例**:
```java
@SpringBootTest
@Testcontainers
class JwtAuthenticationIntegrationTest {

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:24.0")
        .withRealmImportFile("test-realm.json");

    @Test
    void shouldAuthenticateWithValidJwt() {
        // 1. Keycloakからトークン取得
        String jwt = getTokenFromKeycloak("testuser@example.com", "password");

        // 2. JWTでAPI呼び出し
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/users",
            HttpMethod.GET,
            new HttpEntity<>(createAuthHeaders(jwt)),
            String.class
        );

        // 3. 検証
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldFallbackToApiKeyWhenJwtInvalid() {
        // 1. 無効なJWT + 有効なAPI Key
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid-jwt");
        headers.set("X-API-Key", validApiKey);

        // 2. API呼び出し
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/users",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        // 3. API Key認証で成功
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
```

### E2E Tests

**対象**:
1. **JWT認証フロー**:
   - Keycloak→Token取得→Backend API呼び出し
   - Organization分離の検証
   - Role-based認可の検証

2. **エラーハンドリング**:
   - 期限切れトークン
   - 無効な署名
   - 存在しない組織ID

**ツール**: Playwright（既存のE2Eフレームワーク）

### Performance Tests

**対象**:
1. **JWT認証スループット**:
   - 10,000 req/s以上の負荷テスト
   - JWT検証レイテンシ測定

2. **比較テスト**:
   - API Key認証 vs JWT認証
   - ハイブリッド（両方混在）シナリオ

**ツール**: Gatling（既存）

**シナリオ例**:
```scala
class JwtAuthenticationSimulation extends Simulation {

  val jwtToken = getJwtFromKeycloak()

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .header("Authorization", s"Bearer $jwtToken")

  val scn = scenario("JWT Authentication Load Test")
    .exec(http("Get Users")
      .get("/api/v1/users")
      .check(status.is(200))
    )

  setUp(
    scn.inject(
      rampUsersPerSec(100) to 10000 during (60 seconds),
      constantUsersPerSec(10000) during (300 seconds)
    )
  ).protocols(httpProtocol)
}
```

## Migration Plan

### Step 1: 実装・テスト (週1-2)
1. Spring Security依存関係追加
2. JwtAuthenticationFilter実装
3. SecurityConfig更新
4. UserService拡張（JIT Provisioning）
5. ユニット・統合テスト実装

### Step 2: 統合テスト・E2E (週2)
1. Testcontainers統合テスト
2. E2Eテスト実装
3. パフォーマンステスト実施
4. ドキュメント作成

### Step 3: デプロイ・検証 (週3)
1. 開発環境デプロイ
2. JWT認証の動作確認
3. API Key認証の後方互換性確認
4. 監視・ログ確認

### Rollback Plan

**Rollback Trigger**:
- JWT検証のパフォーマンス劣化（p95 >10ms）
- API Key認証への影響（エラー率増加）
- 重大なセキュリティ脆弱性の発見

**Rollback Steps**:
1. `KeycloakProperties.enabled=false`に設定
2. SecurityConfigから`JwtAuthenticationFilter`を削除
3. アプリケーション再起動
4. API Key認証のみで運用継続

## Open Questions

1. **Token Refresh戦略**: Refresh Token処理をどこで行うか？
   - **Recommendation**: Phase 3でFrontend側で実装

2. **ユーザーロールの同期**: JWT RolesとDB Rolesが異なる場合、どちらを優先？
   - **Recommendation**: JWT Rolesを優先（Single Source of Truth）

3. **組織管理画面**: 組織の登録・管理UIを追加するか？
   - **Recommendation**: Phase 3で管理画面追加を検討

4. **Audit Log**: JWT認証ログを記録するか？
   - **Recommendation**: Phase 2で認証イベントのログ記録を実装

## References

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Nimbus JOSE+JWT](https://connect2id.com/products/nimbus-jose-jwt)
- [RFC 7519 - JWT](https://datatracker.ietf.org/doc/html/rfc7519)
- [RFC 7517 - JWK](https://datatracker.ietf.org/doc/html/rfc7517)
- Phase 1 Design: `openspec/changes/add-keycloak-authentication/design.md`
- Keycloak Integration Guide: `docs/KEYCLOAK_INTEGRATION.md`

---

**Author**: Auth Platform Team
**Created**: 2025-10-28
**Status**: Draft
