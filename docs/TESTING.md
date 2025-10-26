# テストガイド

Auth Platformでは、包括的なテスト戦略により高品質なコードを維持しています。このガイドでは、各種テストの実行方法と作成方法について説明します。

## 目次

- [テスト戦略](#テスト戦略)
- [Backend テスト](#backend-テスト)
- [Frontend テスト](#frontend-テスト)
- [E2E テスト](#e2e-テスト)
- [パフォーマンステスト](#パフォーマンステスト)
- [セキュリティテスト](#セキュリティテスト)
- [テストカバレッジ](#テストカバレッジ)

## テスト戦略

### テストピラミッド

```
        /\
       /E2\      E2E Tests (少数・遅い・高コスト)
      /----\
     / Intg \    Integration Tests (中程度)
    /--------\
   /   Unit   \  Unit Tests (多数・高速・低コスト)
  /------------\
```

### テスト種別

| テスト種別 | 目的 | 実行頻度 | ツール |
|-----------|------|---------|--------|
| Unit Test | 単一機能の検証 | 毎回のコミット | JUnit 5, Jest |
| Integration Test | コンポーネント間連携 | PR作成時 | Spring Boot Test, React Testing Library |
| E2E Test | ユーザーシナリオ | PR作成時 | Playwright |
| Performance Test | 負荷・パフォーマンス | リリース前 | Gatling |
| Security Test | 脆弱性スキャン | 週次 | OWASP ZAP, Trivy |

## Backend テスト

### 単体テスト (Unit Tests)

#### テストの実行

```bash
cd backend

# 全テスト実行
./gradlew test

# 特定のテストクラスを実行
./gradlew test --tests UserServiceTest

# 特定のテストメソッドを実行
./gradlew test --tests UserServiceTest.testCreateUser

# カバレッジレポート生成
./gradlew test jacocoTestReport
```

#### テストの書き方

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should create user successfully")
    void testCreateUser() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");

        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.createUser(request);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user already exists")
    void testCreateUser_AlreadyExists() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existing");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.createUser(request);
        });
    }
}
```

### 統合テスト (Integration Tests)

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should create user via API")
    void testCreateUserAPI() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("new@example.com"));

        // Verify database
        assertTrue(userRepository.existsByUsername("newuser"));
    }

    @Test
    @DisplayName("Should return 400 for invalid request")
    void testCreateUser_InvalidRequest() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("ab"); // Too short

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
```

### リポジトリテスト

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should find user by username")
    void testFindByUsername() {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        userRepository.save(user);

        // Act
        Optional<User> found = userRepository.findByUsername("testuser");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
    }
}
```

## Frontend テスト

### 単体テスト (Component Tests)

#### テストの実行

```bash
cd frontend

# 全テスト実行
pnpm test

# Watch mode
pnpm test:watch

# カバレッジ
pnpm test:coverage
```

#### コンポーネントテスト

```typescript
import { render, screen, fireEvent } from '@testing-library/react'
import { UserCard } from '@/components/UserCard'

describe('UserCard', () => {
  const mockUser = {
    id: '1',
    username: 'testuser',
    email: 'test@example.com',
    displayName: 'Test User',
    status: 'ACTIVE',
  }

  const mockOnEdit = jest.fn()
  const mockOnDelete = jest.fn()

  it('should render user information', () => {
    render(
      <UserCard user={mockUser} onEdit={mockOnEdit} onDelete={mockOnDelete} />
    )

    expect(screen.getByText('Test User')).toBeInTheDocument()
    expect(screen.getByText('testuser')).toBeInTheDocument()
    expect(screen.getByText('test@example.com')).toBeInTheDocument()
  })

  it('should call onEdit when edit button is clicked', () => {
    render(
      <UserCard user={mockUser} onEdit={mockOnEdit} onDelete={mockOnDelete} />
    )

    const editButton = screen.getByRole('button', { name: /edit/i })
    fireEvent.click(editButton)

    expect(mockOnEdit).toHaveBeenCalledWith(mockUser)
  })

  it('should call onDelete when delete button is clicked', async () => {
    render(
      <UserCard user={mockUser} onEdit={mockOnEdit} onDelete={mockOnDelete} />
    )

    const deleteButton = screen.getByRole('button', { name: /delete/i })
    fireEvent.click(deleteButton)

    expect(mockOnDelete).toHaveBeenCalledWith('1')
  })
})
```

#### カスタムフックのテスト

```typescript
import { renderHook, waitFor } from '@testing-library/react'
import { useUsers } from '@/hooks/useUsers'
import { userService } from '@/services/user-service'

jest.mock('@/services/user-service')

describe('useUsers', () => {
  it('should fetch users successfully', async () => {
    const mockUsers = [
      { id: '1', username: 'user1', email: 'user1@example.com' },
      { id: '2', username: 'user2', email: 'user2@example.com' },
    ]

    ;(userService.listUsers as jest.Mock).mockResolvedValue({
      content: mockUsers,
      totalElements: 2,
    })

    const { result } = renderHook(() => useUsers())

    expect(result.current.loading).toBe(true)

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(result.current.users).toEqual(mockUsers)
    expect(result.current.error).toBeNull()
  })

  it('should handle error', async () => {
    const error = new Error('Failed to fetch')
    ;(userService.listUsers as jest.Mock).mockRejectedValue(error)

    const { result } = renderHook(() => useUsers())

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(result.current.users).toEqual([])
    expect(result.current.error).toEqual(error)
  })
})
```

## E2E テスト

### Playwright テスト

#### テストの実行

```bash
cd frontend

# 全E2Eテスト実行
pnpm test:e2e

# 特定のテストファイル実行
pnpm test:e2e 01-basic-auth

# UI Mode（デバッグ用）
pnpm test:e2e:ui

# Headed Mode（ブラウザ表示）
pnpm test:e2e:headed

# デバッグモード
pnpm test:e2e:debug
```

#### テストの書き方

```typescript
import { test, expect } from '@playwright/test'

test.describe('User Management', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/login')
    await page.getByTestId('api-key-input').fill('test-api-key-12345')
    await page.getByTestId('login-button').click()
    await page.waitForURL('**/dashboard')

    // Navigate to users page
    await page.getByRole('link', { name: 'ユーザー管理' }).click()
    await page.waitForURL('**/users')
  })

  test('should create new user', async ({ page }) => {
    // Click create button
    await page.getByRole('button', { name: '新規ユーザー' }).click()

    // Fill form
    await page.getByLabel('ユーザー名').fill('newuser')
    await page.getByLabel('メールアドレス').fill('new@example.com')
    await page.getByLabel('表示名').fill('New User')

    // Submit
    await page.getByRole('button', { name: '作成' }).click()

    // Verify success
    await expect(page.getByText('ユーザーが作成されました')).toBeVisible()
    await expect(page.getByText('newuser')).toBeVisible()
  })

  test('should show validation errors', async ({ page }) => {
    await page.getByRole('button', { name: '新規ユーザー' }).click()

    // Submit without filling
    await page.getByRole('button', { name: '作成' }).click()

    // Check validation errors
    await expect(page.getByText('ユーザー名は必須です')).toBeVisible()
    await expect(page.getByText('メールアドレスは必須です')).toBeVisible()
  })
})
```

### E2Eテスト戦略

詳細は [frontend/e2e/TESTING_STRATEGY.md](../frontend/e2e/TESTING_STRATEGY.md) を参照。

**Phase 1: Foundation** (✅ 完了)
- 基本認証フロー
- ページナビゲーション
- 基本的なページ表示

**Phase 2: Page Functionality** (次のステップ)
- CRUD操作
- フォームバリデーション
- データの永続化

**Phase 3: Advanced Features**
- ポリシー評価
- 複雑なワークフロー
- エラーハンドリング

**Phase 4: Quality & Accessibility**
- アクセシビリティ検証
- パフォーマンス測定
- ビジュアルリグレッション

## パフォーマンステスト

### Gatling テスト

#### テストの実行

```bash
cd backend

# 基本負荷テスト
./gradlew gatlingRun-authplatform.simulations.BasicLoadSimulation

# 認可パフォーマンステスト
./gradlew gatlingRun-authplatform.simulations.AuthorizationLoadSimulation

# キャッシュパフォーマンステスト
./gradlew gatlingRun-authplatform.simulations.CachePerformanceSimulation

# ストレステスト
./gradlew gatlingRun-authplatform.simulations.StressTestSimulation

# 全シミュレーション実行
./gradlew gatlingRun
```

#### シミュレーション例

```scala
class BasicLoadSimulation extends Simulation {
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")

  val scn = scenario("Basic Load Test")
    .exec(
      http("List Users")
        .get("/api/v1/users")
        .header("X-API-Key", "test-key")
        .check(status.is(200))
    )
    .pause(1)

  setUp(
    scn.inject(
      rampUsers(50).during(30.seconds)
    )
  ).protocols(httpProtocol)
}
```

### パフォーマンス目標

| メトリクス | 目標値 | 測定方法 |
|----------|--------|---------|
| レスポンスタイム (p95) | <200ms | Gatling |
| スループット | >10,000 req/s | Gatling |
| 認可チェック (p95) | <10ms | Cache Performance Sim |
| エラー率 | <0.1% | 全シミュレーション |

## セキュリティテスト

### OWASP ZAP

```bash
# ZAPを起動（Docker）
docker run -p 8090:8090 zaproxy/zap-stable zap-webswing.sh

# スキャン実行
docker run --rm -v $(pwd):/zap/wrk/:rw \
  zaproxy/zap-stable \
  zap-baseline.py \
  -t http://host.docker.internal:3000 \
  -r zap-report.html
```

### Trivy

```bash
# イメージスキャン
trivy image auth-platform-backend:latest
trivy image auth-platform-frontend:latest

# ファイルシステムスキャン
trivy fs --severity HIGH,CRITICAL .

# 設定ファイルスキャン
trivy config .
```

### 依存関係の脆弱性スキャン

```bash
# Backend (Gradle)
cd backend
./gradlew dependencyCheckAnalyze

# Frontend (pnpm)
cd frontend
pnpm audit
pnpm audit --fix
```

## テストカバレッジ

### Backend カバレッジ

```bash
cd backend
./gradlew test jacocoTestReport

# レポート確認
open build/reports/jacoco/test/html/index.html
```

**目標カバレッジ:**
- Line Coverage: >80%
- Branch Coverage: >75%
- クリティカルパス: 100%

### Frontend カバレッジ

```bash
cd frontend
pnpm test:coverage

# レポート確認
open coverage/lcov-report/index.html
```

**目標カバレッジ:**
- Statements: >80%
- Branches: >75%
- Functions: >80%
- Lines: >80%

## CI/CD でのテスト

GitHub Actions ワークフローで自動実行：

```yaml
name: Test

on: [push, pull_request]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Run tests
        run: |
          cd backend
          ./gradlew test jacocoTestReport

  frontend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '20'
      - uses: pnpm/action-setup@v2
      - name: Run tests
        run: |
          cd frontend
          pnpm install
          pnpm test:coverage

  e2e-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Start services
        run: docker compose -f infrastructure/docker-compose.yml up -d
      - name: Run E2E tests
        run: |
          cd frontend
          pnpm install
          pnpm test:e2e
```

## ベストプラクティス

### テストの独立性

```java
// Good - テストごとにデータをセットアップ
@BeforeEach
void setUp() {
    testUser = userRepository.save(createTestUser());
}

@AfterEach
void tearDown() {
    userRepository.deleteAll();
}

// Bad - グローバル状態に依存
static User globalUser; // テスト間で共有しない
```

### テストの可読性

```typescript
// Good - Arrange-Act-Assert パターン
test('should calculate total price', () => {
  // Arrange
  const items = [
    { price: 100, quantity: 2 },
    { price: 50, quantity: 3 },
  ]

  // Act
  const total = calculateTotal(items)

  // Assert
  expect(total).toBe(350)
})

// Bad - 何をテストしているか不明確
test('test1', () => {
  expect(func([{ p: 100, q: 2 }])).toBe(200)
})
```

### モックの使用

```typescript
// Good - 必要な部分だけモック
jest.mock('@/services/user-service', () => ({
  userService: {
    listUsers: jest.fn(),
  },
}))

// Bad - 全てモックして実装が検証されない
jest.mock('@/components/UserCard') // コンポーネント自体をモックしない
```

## トラブルシューティング

### テストが不安定（Flaky）

1. 非同期処理を適切に待つ
2. タイムアウトを延長
3. テストの独立性を確保
4. データベースの状態をクリーンアップ

### カバレッジが低い

1. エッジケースをテスト
2. エラーハンドリングをテスト
3. 統合テストを追加
4. テストしにくいコードをリファクタリング

## 次のステップ

- [Development Guide](./DEVELOPMENT.md) - 開発ワークフロー
- [Deployment Guide](./DEPLOYMENT.md) - デプロイメント手順
- [Frontend E2E Testing Strategy](../frontend/e2e/TESTING_STRATEGY.md) - E2Eテスト詳細
