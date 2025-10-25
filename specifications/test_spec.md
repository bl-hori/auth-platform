# テスト設計書 - 認可基盤プラットフォーム

## 1. 概要

### 1.1 目的
本設計書は、認可基盤プラットフォームの品質保証を実現するための包括的なテスト戦略、テストケース、テスト環境、およびテスト実行計画を定義する。

### 1.2 テスト方針
- **シフトレフト**: 開発初期段階からのテスト実施
- **自動化優先**: CI/CDパイプラインでの自動テスト実行
- **リスクベース**: 重要度と影響度に基づくテスト優先順位
- **継続的改善**: テストカバレッジとテスト品質の継続的向上

### 1.3 品質目標
- コードカバレッジ: 80%以上
- 重要パスのカバレッジ: 95%以上
- バグ密度: 1.0件/KLOC以下
- 性能目標達成率: 100%
- セキュリティ脆弱性: Critical/High 0件

## 2. テストレベルと種類

### 2.1 テストピラミッド

```
         /\
        /  \  E2Eテスト（5%）
       /----\
      /      \ 統合テスト（20%）
     /--------\
    /          \ コンポーネントテスト（25%）
   /------------\
  /              \ 単体テスト（50%）
 /________________\
```

### 2.2 テスト種別マトリクス

| テストレベル | 自動/手動 | 実行頻度 | 所要時間 | 責任者 |
|------------|----------|---------|---------|--------|
| 単体テスト | 自動 | コミット毎 | 1-2分 | 開発者 |
| コンポーネントテスト | 自動 | プルリクエスト | 5分 | 開発者 |
| 統合テスト | 自動 | デイリー | 30分 | QAチーム |
| E2Eテスト | 自動 | デイリー | 1時間 | QAチーム |
| 性能テスト | 自動 | ウィークリー | 2時間 | 性能チーム |
| セキュリティテスト | 半自動 | リリース前 | 4時間 | セキュリティチーム |
| 探索的テスト | 手動 | スプリント毎 | 8時間 | QAチーム |

## 3. 単体テスト設計

### 3.1 対象と範囲
- すべてのビジネスロジック
- ユーティリティ関数
- データ変換処理
- バリデーション処理

### 3.2 テストケース設計手法
- **境界値分析**: 入力値の境界条件
- **同値分割**: 有効/無効な入力グループ
- **判定条件網羅**: すべての条件分岐
- **例外処理**: エラーケースの網羅

### 3.3 Java/Spring Boot単体テスト例

```java
@SpringBootTest
class PolicyServiceTest {
    
    @MockBean
    private PolicyRepository policyRepository;
    
    @Autowired
    private PolicyService policyService;
    
    @Test
    @DisplayName("正常系: ポリシー作成成功")
    void testCreatePolicy_Success() {
        // Given
        CreatePolicyRequest request = CreatePolicyRequest.builder()
            .name("TestPolicy")
            .type(PolicyType.RBAC)
            .rules("package authz\nallow = true")
            .build();
        
        Policy expectedPolicy = Policy.builder()
            .id(UUID.randomUUID())
            .name("TestPolicy")
            .build();
        
        when(policyRepository.save(any())).thenReturn(expectedPolicy);
        
        // When
        Policy result = policyService.createPolicy(request);
        
        // Then
        assertNotNull(result);
        assertEquals("TestPolicy", result.getName());
        verify(policyRepository).save(any());
    }
    
    @Test
    @DisplayName("異常系: 重複ポリシー名でエラー")
    void testCreatePolicy_DuplicateName() {
        // Given
        when(policyRepository.existsByName(anyString())).thenReturn(true);
        
        // When & Then
        assertThrows(DuplicatePolicyException.class, () -> {
            policyService.createPolicy(request);
        });
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "a".repeat(256)})
    @DisplayName("境界値: 無効なポリシー名")
    void testCreatePolicy_InvalidName(String name) {
        CreatePolicyRequest request = CreatePolicyRequest.builder()
            .name(name)
            .build();
        
        assertThrows(ValidationException.class, () -> {
            policyService.createPolicy(request);
        });
    }
}
```

### 3.4 TypeScript/Next.js単体テスト例

```typescript
import { render, screen, fireEvent } from '@testing-library/react';
import { PolicyEditor } from '@/components/PolicyEditor';
import { mockPolicy } from '@/test/fixtures';

describe('PolicyEditor Component', () => {
  it('should render policy editor with initial values', () => {
    render(<PolicyEditor policy={mockPolicy} />);
    
    expect(screen.getByLabelText('Policy Name')).toHaveValue('TestPolicy');
    expect(screen.getByLabelText('Policy Type')).toHaveValue('RBAC');
  });
  
  it('should validate required fields', async () => {
    render(<PolicyEditor />);
    
    const submitButton = screen.getByRole('button', { name: 'Save' });
    fireEvent.click(submitButton);
    
    expect(await screen.findByText('Policy name is required')).toBeInTheDocument();
  });
  
  it('should call onSave with valid data', async () => {
    const onSave = jest.fn();
    render(<PolicyEditor onSave={onSave} />);
    
    fireEvent.change(screen.getByLabelText('Policy Name'), {
      target: { value: 'NewPolicy' }
    });
    
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    
    await waitFor(() => {
      expect(onSave).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'NewPolicy'
        })
      );
    });
  });
});
```

## 4. 統合テスト設計

### 4.1 統合テストシナリオ

#### TC-INT-001: 認可フロー統合テスト
**目的**: API Gateway → Auth Service → PDP → Cache の統合確認

```gherkin
Feature: 認可判定フロー
  
  Scenario: 正常な認可判定
    Given ユーザー "user-123" が認証済み
    And ポリシー "AdminPolicy" がアクティブ
    When ユーザーが "/documents/doc-456" への "read" アクセスを要求
    Then 認可判定は "allow" を返す
    And レスポンスタイムは 10ms 以内
    And 監査ログが記録される
```

#### TC-INT-002: ポリシー更新伝播テスト
**目的**: ポリシー更新がすべてのPDPに伝播することを確認

```java
@Test
@DirtiesContext
public void testPolicyUpdatePropagation() throws Exception {
    // 1. ポリシー作成
    Policy policy = createTestPolicy();
    
    // 2. 複数のPDPインスタンスで確認
    List<CompletableFuture<Policy>> futures = pdpInstances.stream()
        .map(pdp -> CompletableFuture.supplyAsync(() -> 
            pdp.getPolicy(policy.getId())
        ))
        .collect(Collectors.toList());
    
    // 3. 1秒以内に全PDPに伝播することを確認
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .get(1, TimeUnit.SECONDS);
    
    futures.forEach(future -> {
        Policy retrievedPolicy = future.join();
        assertEquals(policy.getVersion(), retrievedPolicy.getVersion());
    });
}
```

### 4.2 データベース統合テスト

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
class PolicyRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Test
    @Sql("/test-data/policies.sql")
    void testComplexPolicyQuery() {
        // Given
        Specification<Policy> spec = PolicySpecifications
            .withOrganization("org-123")
            .and(PolicySpecifications.withStatus(PolicyStatus.ACTIVE))
            .and(PolicySpecifications.withType(PolicyType.RBAC));
        
        // When
        Page<Policy> policies = policyRepository.findAll(spec, 
            PageRequest.of(0, 10, Sort.by("createdAt").descending()));
        
        // Then
        assertThat(policies).hasSize(5);
        assertThat(policies.getContent())
            .extracting(Policy::getStatus)
            .containsOnly(PolicyStatus.ACTIVE);
    }
}
```

## 5. E2Eテスト設計

### 5.1 E2Eテストシナリオ

#### シナリオ1: 完全な認可ライフサイクル

```typescript
// Playwright E2E Test
import { test, expect } from '@playwright/test';

test.describe('Authorization Lifecycle', () => {
  test('Complete authorization flow from UI to decision', async ({ page }) => {
    // 1. ログイン
    await page.goto('/login');
    await page.fill('[name="email"]', 'admin@example.com');
    await page.fill('[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    
    // 2. ポリシー作成画面へ移動
    await page.goto('/policies/new');
    
    // 3. RBACポリシーを作成
    await page.fill('[name="policyName"]', 'E2E Test Policy');
    await page.selectOption('[name="policyType"]', 'RBAC');
    await page.fill('[name="rules"]', `
      package authz
      allow {
        input.user.role == "tester"
      }
    `);
    await page.click('button:text("Create Policy")');
    
    // 4. ポリシーが作成されたことを確認
    await expect(page.locator('.success-message'))
      .toContainText('Policy created successfully');
    
    // 5. APIで認可チェック
    const response = await page.request.post('/api/v1/authorize', {
      data: {
        subject: { type: 'user', id: 'test-user' },
        action: 'read',
        resource: { type: 'document', id: 'doc-123' }
      }
    });
    
    const result = await response.json();
    expect(result.data.decision).toBe('allow');
  });
});
```

### 5.2 クロスブラウザテスト

| ブラウザ | バージョン | テスト範囲 |
|---------|-----------|----------|
| Chrome | 最新2バージョン | 全機能 |
| Firefox | 最新2バージョン | 全機能 |
| Safari | 最新2バージョン | 主要機能 |
| Edge | 最新2バージョン | 主要機能 |

## 6. 性能テスト設計

### 6.1 性能要件と指標

| 指標 | 要求値 | 測定方法 |
|------|--------|----------|
| 認可API レイテンシー(p95) | < 10ms | JMeter/Gatling |
| スループット | > 100,000 req/s | 負荷試験 |
| 同時接続数 | > 10,000 | 接続試験 |
| CPU使用率 | < 70% | モニタリング |
| メモリ使用率 | < 80% | モニタリング |

### 6.2 負荷テストシナリオ（Gatling）

```scala
class AuthorizationLoadTest extends Simulation {
  
  val httpProtocol = http
    .baseUrl("https://api.authplatform.io")
    .acceptHeader("application/json")
    .header("Authorization", "Bearer ${token}")
  
  val authorizationScenario = scenario("Authorization Load Test")
    .exec(
      http("Authorize Request")
        .post("/v1/authorize")
        .body(ElFileBody("authorization_request.json"))
        .check(status.is(200))
        .check(jsonPath("$.data.decision").exists)
        .check(responseTimeInMillis.lt(10))
    )
  
  // ランプアップシナリオ
  setUp(
    authorizationScenario.inject(
      rampUsers(100) during (30 seconds),
      constantUsersPerSec(1000) during (5 minutes),
      rampUsersPerSec(1000) to 5000 during (2 minutes),
      constantUsersPerSec(5000) during (10 minutes),
      rampUsersPerSec(5000) to 0 during (1 minute)
    ).protocols(httpProtocol)
  ).assertions(
    global.responseTime.percentile(95).lt(10),
    global.successfulRequests.percent.gt(99.9),
    global.requestsPerSec.gt(100000)
  )
}
```

### 6.3 ストレステスト

```yaml
# K6 ストレステスト設定
stages:
  - duration: 5m
    target: 1000   # 通常負荷
  - duration: 10m
    target: 5000   # 高負荷
  - duration: 5m
    target: 10000  # 限界負荷
  - duration: 10m
    target: 10000  # 持続負荷
  - duration: 5m
    target: 0      # クールダウン

thresholds:
  http_req_duration: ['p(95)<10']
  http_req_failed: ['rate<0.001']
  iteration_duration: ['p(95)<100']
```

## 7. セキュリティテスト設計

### 7.1 セキュリティテスト項目

| カテゴリ | テスト項目 | ツール | 頻度 |
|---------|-----------|--------|------|
| SAST | 静的コード解析 | SonarQube, Checkmarx | コミット毎 |
| DAST | 動的アプリケーション解析 | OWASP ZAP, Burp Suite | デイリー |
| SCA | 依存関係チェック | Snyk, Dependabot | デイリー |
| コンテナスキャン | イメージ脆弱性 | Trivy, Clair | ビルド毎 |
| ペネトレーションテスト | 侵入テスト | 手動/外部委託 | 四半期 |

### 7.2 OWASP Top 10対策テスト

#### A01: Broken Access Control
```python
# pytest セキュリティテスト
def test_unauthorized_access():
    """権限のないリソースへのアクセスが拒否されることを確認"""
    client = TestClient()
    
    # 別ユーザーのリソースへアクセス試行
    response = client.get('/api/v1/users/other-user-id/data',
                          headers={'Authorization': 'Bearer user-token'})
    
    assert response.status_code == 403
    assert response.json()['error']['code'] == 'FORBIDDEN'

def test_privilege_escalation():
    """権限昇格攻撃が防げることを確認"""
    client = TestClient()
    
    # 一般ユーザーが管理者APIを呼び出し
    response = client.post('/api/v1/admin/policies',
                          headers={'Authorization': 'Bearer user-token'},
                          json={'name': 'MaliciousPolicy'})
    
    assert response.status_code == 403
```

#### A03: Injection
```java
@Test
public void testSQLInjectionPrevention() {
    String maliciousInput = "'; DROP TABLE users; --";
    
    assertThrows(ValidationException.class, () -> {
        userService.findByName(maliciousInput);
    });
    
    // データベースの整合性確認
    assertTrue(userRepository.count() > 0);
}

@Test  
public void testRegoInjectionPrevention() {
    String maliciousPolicy = "package authz\n" +
        "import data.users\n" +
        "allow { users[_].password }";  // パスワード露出試行
    
    ValidationResult result = policyValidator.validate(maliciousPolicy);
    assertFalse(result.isValid());
    assertTrue(result.getErrors().contains("Forbidden import"));
}
```

### 7.3 認証・認可テスト

```typescript
describe('Authentication & Authorization Tests', () => {
  it('should reject expired tokens', async () => {
    const expiredToken = generateExpiredToken();
    
    const response = await request(app)
      .get('/api/v1/policies')
      .set('Authorization', `Bearer ${expiredToken}`);
    
    expect(response.status).toBe(401);
    expect(response.body.error.code).toBe('TOKEN_EXPIRED');
  });
  
  it('should enforce rate limiting', async () => {
    const requests = Array(150).fill(null).map(() => 
      request(app)
        .post('/api/v1/authorize')
        .set('X-API-Key', 'test-key')
        .send(validAuthRequest)
    );
    
    const responses = await Promise.all(requests);
    const rateLimited = responses.filter(r => r.status === 429);
    
    expect(rateLimited.length).toBeGreaterThan(0);
  });
});
```

## 8. カオステスト設計

### 8.1 障害注入シナリオ

```yaml
# Chaos Mesh設定
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: pod-failure-test
spec:
  action: pod-kill
  mode: random-one
  selector:
    namespaces:
      - auth-platform
    labelSelectors:
      app: authorization-service
  scheduler:
    cron: "@every 10m"
```

### 8.2 ネットワーク遅延テスト

```yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: network-delay-test
spec:
  action: delay
  mode: all
  selector:
    namespaces:
      - auth-platform
  delay:
    latency: "100ms"
    correlation: "25"
    jitter: "10ms"
  duration: "30s"
```

## 9. 回帰テスト戦略

### 9.1 回帰テストスイート

| レベル | テスト数 | 実行時間 | 実行タイミング |
|--------|---------|---------|---------------|
| スモークテスト | 50 | 2分 | デプロイ直後 |
| コア機能 | 200 | 10分 | PR マージ前 |
| 完全回帰 | 1000+ | 1時間 | ナイトリービルド |

### 9.2 テスト自動化フレームワーク

```javascript
// テストスイート管理
const regressionSuite = {
  smoke: [
    'auth.basic.login',
    'auth.basic.authorize',
    'policy.basic.create'
  ],
  core: [
    ...smokeTests,
    'auth.advanced.*',
    'policy.advanced.*',
    'audit.basic.*'
  ],
  full: [
    '**/*.test.js'
  ]
};

// 実行
async function runRegressionTests(level = 'smoke') {
  const tests = regressionSuite[level];
  const results = await testRunner.run(tests);
  
  await reportGenerator.generate(results);
  
  if (results.failed > 0) {
    await notifier.alert('Regression test failed', results);
  }
}
```

## 10. テスト環境

### 10.1 環境構成

| 環境 | 用途 | 構成 | データ |
|------|------|------|--------|
| Local | 開発者テスト | Docker Compose | モックデータ |
| CI | 自動テスト | GitHub Actions | テストデータ |
| QA | 統合テスト | K8s (3ノード) | 本番相当データ |
| Staging | 受け入れテスト | K8s (5ノード) | 本番データコピー |
| Performance | 性能テスト | K8s (10ノード) | 大量テストデータ |

### 10.2 テストデータ管理

```sql
-- テストデータ生成
CREATE OR REPLACE FUNCTION generate_test_data(
    num_orgs INTEGER,
    users_per_org INTEGER,
    policies_per_org INTEGER
) RETURNS void AS $$
DECLARE
    org_id UUID;
    user_id UUID;
BEGIN
    FOR i IN 1..num_orgs LOOP
        org_id := gen_random_uuid();
        INSERT INTO organizations (id, name, domain)
        VALUES (org_id, 'TestOrg' || i, 'test' || i || '.com');
        
        -- ユーザー生成
        FOR j IN 1..users_per_org LOOP
            INSERT INTO users (id, organization_id, email, username)
            VALUES (
                gen_random_uuid(),
                org_id,
                'user' || j || '@test' || i || '.com',
                'user' || j
            );
        END LOOP;
        
        -- ポリシー生成
        FOR k IN 1..policies_per_org LOOP
            INSERT INTO policies (id, organization_id, name, type)
            VALUES (
                gen_random_uuid(),
                org_id,
                'Policy' || k,
                CASE k % 3
                    WHEN 0 THEN 'RBAC'
                    WHEN 1 THEN 'ABAC'
                    ELSE 'ReBAC'
                END
            );
        END LOOP;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- 実行
SELECT generate_test_data(10, 100, 20);
```

## 11. テスト実行とレポート

### 11.1 CI/CDパイプライン統合

```yaml
# GitHub Actions Workflow
name: Test Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Run Unit Tests
        run: |
          mvn test
          npm test
        
      - name: Generate Coverage Report
        run: |
          mvn jacoco:report
          npm run coverage
        
      - name: Upload Coverage to SonarQube
        run: |
          mvn sonar:sonar \
            -Dsonar.projectKey=auth-platform \
            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
  
  integration-tests:
    needs: unit-tests
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      
      redis:
        image: redis:7
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
      - name: Run Integration Tests
        run: mvn verify -P integration-tests
  
  security-scan:
    runs-on: ubuntu-latest
    steps:
      - name: Run SAST
        uses: checkmarx/ast-github-action@v1
        
      - name: Run Dependency Check
        uses: snyk/actions/maven@master
        
      - name: Container Scan
        run: trivy image auth-platform:latest
```

### 11.2 テストレポート形式

```html
<!DOCTYPE html>
<html>
<head>
    <title>Test Report - Auth Platform</title>
</head>
<body>
    <h1>Test Execution Summary</h1>
    
    <div class="summary">
        <table>
            <tr>
                <th>Test Type</th>
                <th>Total</th>
                <th>Passed</th>
                <th>Failed</th>
                <th>Skipped</th>
                <th>Pass Rate</th>
            </tr>
            <tr>
                <td>Unit Tests</td>
                <td>1,250</td>
                <td>1,245</td>
                <td>3</td>
                <td>2</td>
                <td>99.6%</td>
            </tr>
            <tr>
                <td>Integration Tests</td>
                <td>350</td>
                <td>348</td>
                <td>2</td>
                <td>0</td>
                <td>99.4%</td>
            </tr>
            <tr>
                <td>E2E Tests</td>
                <td>50</td>
                <td>50</td>
                <td>0</td>
                <td>0</td>
                <td>100%</td>
            </tr>
        </table>
    </div>
    
    <div class="coverage">
        <h2>Code Coverage</h2>
        <ul>
            <li>Line Coverage: 85.3%</li>
            <li>Branch Coverage: 78.9%</li>
            <li>Function Coverage: 92.1%</li>
        </ul>
    </div>
    
    <div class="failures">
        <h2>Failed Tests</h2>
        <!-- 失敗したテストの詳細 -->
    </div>
</body>
</html>
```

## 12. テストメトリクスとKPI

### 12.1 品質メトリクス

| メトリクス | 目標値 | 現在値 | 評価 |
|-----------|--------|--------|------|
| コードカバレッジ | > 80% | 85.3% | ✅ |
| バグ密度 | < 1.0/KLOC | 0.8/KLOC | ✅ |
| テスト成功率 | > 99% | 99.5% | ✅ |
| 平均修正時間 | < 4時間 | 3.2時間 | ✅ |
| 回帰バグ率 | < 5% | 3.8% | ✅ |

### 12.2 テスト効率メトリクス

- テスト自動化率: 92%
- テスト実行時間: 平均45分（フルスイート）
- 欠陥検出率: 85%（リリース前）
- テストケース再利用率: 70%

## 13. リスクベーステスト

### 13.1 リスク評価マトリクス

| 機能 | 影響度 | 発生確率 | リスクレベル | テスト優先度 |
|------|--------|----------|-------------|-------------|
| 認可API | 極高 | 中 | 高 | P1 |
| ポリシー評価 | 極高 | 低 | 高 | P1 |
| ユーザー認証 | 高 | 中 | 高 | P1 |
| 監査ログ | 中 | 低 | 中 | P2 |
| レポート生成 | 低 | 低 | 低 | P3 |

### 13.2 リスク緩和策

- **高リスク機能**: 包括的テスト + 継続的監視
- **中リスク機能**: 標準テスト + 定期レビュー
- **低リスク機能**: 基本テスト + スモークテスト

## 14. テストツールと技術スタック

### 14.1 テストツール一覧

| カテゴリ | ツール | 用途 |
|---------|--------|------|
| 単体テスト | JUnit 5, Jest, pytest | 単体テスト実行 |
| モック | Mockito, Sinon.js | モック/スタブ |
| E2E | Playwright, Selenium | ブラウザテスト |
| API | REST Assured, Postman | APIテスト |
| 性能 | JMeter, Gatling, K6 | 負荷テスト |
| セキュリティ | OWASP ZAP, Burp Suite | セキュリティテスト |
| カオス | Chaos Mesh, Litmus | 障害注入 |
| カバレッジ | JaCoCo, Istanbul | カバレッジ測定 |

## 15. 継続的改善

### 15.1 改善プロセス

1. **メトリクス収集**: テスト実行毎にメトリクス記録
2. **分析**: 週次でトレンド分析
3. **改善案策定**: 月次で改善案検討
4. **実施**: スプリント単位で改善実施
5. **効果測定**: 四半期毎に効果測定

### 15.2 レトロスペクティブ項目

- テストの有効性（バグ検出率）
- テスト実行時間の最適化
- テストの保守性向上
- 新しいテスト手法の導入
- チーム間の知識共有

---

**文書情報**
- バージョン: 1.0.0
- 作成日: 2025-10-25
- 作成者: Implementation Spec Agent
- レビュー状態: ドラフト
- 関連文書: api_spec.md, architecture_design.md, requirement_spec.md
