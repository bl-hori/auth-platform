# 開発ガイド

このガイドでは、Auth Platformの開発環境のセットアップと開発ワークフローについて説明します。

## 目次

- [開発環境のセットアップ](#開発環境のセットアップ)
- [プロジェクト構造](#プロジェクト構造)
- [開発ワークフロー](#開発ワークフロー)
- [コーディング規約](#コーディング規約)
- [デバッグ](#デバッグ)
- [パフォーマンス最適化](#パフォーマンス最適化)

## 開発環境のセットアップ

### 必要なツール

```bash
# Node.js 20.x
nvm install 20
nvm use 20

# pnpm
npm install -g pnpm

# Java 21 (SDKMANを推奨)
curl -s "https://get.sdkman.io" | bash
sdk install java 21-tem

# Docker Desktop
# https://www.docker.com/products/docker-desktop からダウンロード
```

### IDEの推奨設定

#### VS Code

推奨拡張機能:

```json
{
  "recommendations": [
    "dbaeumer.vscode-eslint",
    "esbenp.prettier-vscode",
    "bradlc.vscode-tailwindcss",
    "ms-playwright.playwright",
    "vscjava.vscode-java-pack",
    "vscjava.vscode-spring-boot-dashboard",
    "redhat.java",
    "vmware.vscode-boot-dev-pack"
  ]
}
```

設定 (`.vscode/settings.json`):

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "[java]": {
    "editor.defaultFormatter": "redhat.java"
  },
  "typescript.tsdk": "frontend/node_modules/typescript/lib",
  "tailwindCSS.experimental.classRegex": [
    ["cn\\(([^)]*)\\)", "(?:'|\"|`)([^'\"`]*)(?:'|\"|`)"]
  ]
}
```

#### IntelliJ IDEA

1. Spring Boot プラグインを有効化
2. Lombok プラグインをインストール
3. Code Style を Google Java Style に設定
4. Enable annotation processing を有効化

### 環境変数の設定

#### Backend (.env)

```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/authplatform
SPRING_DATASOURCE_USERNAME=authuser
SPRING_DATASOURCE_PASSWORD=authpass

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# JWT
JWT_SECRET=your-secret-key-change-in-production
JWT_EXPIRATION=3600000

# Application
SPRING_PROFILES_ACTIVE=dev
```

#### Frontend (.env.local)

```env
# API URL
NEXT_PUBLIC_API_URL=http://localhost:8080

# Development API Key
NEXT_PUBLIC_API_KEY=dev-api-key-12345

# Environment
NODE_ENV=development
```

## プロジェクト構造

```
auth-platform/
├── backend/                    # Spring Boot バックエンド
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/authplatform/
│   │   │   │       ├── config/          # 設定クラス
│   │   │   │       ├── controller/      # REST コントローラー
│   │   │   │       ├── dto/             # Data Transfer Objects
│   │   │   │       ├── entity/          # JPA エンティティ
│   │   │   │       ├── repository/      # データアクセス層
│   │   │   │       ├── service/         # ビジネスロジック
│   │   │   │       ├── security/        # セキュリティ設定
│   │   │   │       ├── exception/       # カスタム例外
│   │   │   │       └── util/            # ユーティリティ
│   │   │   └── resources/
│   │   │       ├── application.yml      # メイン設定
│   │   │       ├── application-dev.yml  # 開発環境設定
│   │   │       └── application-prod.yml # 本番環境設定
│   │   ├── test/                        # テストコード
│   │   └── gatling/                     # パフォーマンステスト
│   └── build.gradle                     # Gradle ビルド設定
│
├── frontend/                   # Next.js フロントエンド
│   ├── src/
│   │   ├── app/                         # Next.js App Router
│   │   │   ├── (auth)/                  # 認証ルート
│   │   │   ├── (dashboard)/             # ダッシュボードルート
│   │   │   ├── layout.tsx               # ルートレイアウト
│   │   │   └── page.tsx                 # ホームページ
│   │   ├── components/                  # Reactコンポーネント
│   │   │   ├── ui/                      # 再利用可能なUIコンポーネント
│   │   │   ├── layout/                  # レイアウトコンポーネント
│   │   │   └── features/                # 機能別コンポーネント
│   │   ├── contexts/                    # React Context
│   │   ├── hooks/                       # カスタムフック
│   │   ├── lib/                         # ユーティリティライブラリ
│   │   ├── services/                    # API サービス
│   │   └── types/                       # TypeScript型定義
│   ├── e2e/                             # E2Eテスト
│   ├── public/                          # 静的ファイル
│   └── package.json                     # npm パッケージ設定
│
├── infrastructure/             # インフラストラクチャ
│   └── docker-compose.yml               # Docker Compose 設定
│
├── docs/                       # ドキュメント
├── specifications/             # 仕様書
└── openspec/                   # OpenSpec変更管理

```

## 開発ワークフロー

### 1. 新しい機能の開発

```bash
# 1. mainブランチから最新を取得
git checkout main
git pull origin main

# 2. 機能ブランチを作成
git checkout -b feature/user-authentication

# 3. 開発を行う
# ... コードを書く ...

# 4. コミット
git add .
git commit -m "feat(auth): implement JWT token refresh"

# 5. プッシュ
git push origin feature/user-authentication

# 6. Pull Requestを作成
gh pr create --title "feat(auth): Implement JWT token refresh" \
  --body "## Summary\n..."
```

### 2. コミットメッセージ規約

Conventional Commits形式を使用：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type:**
- `feat`: 新機能
- `fix`: バグ修正
- `docs`: ドキュメントのみの変更
- `style`: コードの意味に影響しない変更（空白、フォーマット等）
- `refactor`: バグ修正や機能追加を伴わないコード変更
- `perf`: パフォーマンス改善
- `test`: テストの追加・修正
- `chore`: ビルドプロセスやツールの変更

**例:**

```bash
feat(api): add GET /v1/users endpoint

Implement user listing endpoint with pagination and filtering.

- Add UserController.listUsers method
- Implement pagination with page and size parameters
- Add filtering by status and role
- Add comprehensive tests

Closes #123
```

### 3. コードレビュー

Pull Request作成時のチェックリスト：

- [ ] テストが全て通過している
- [ ] Lintエラーがない
- [ ] 新機能にはテストが追加されている
- [ ] ドキュメントが更新されている
- [ ] Breaking Changesが文書化されている
- [ ] コミットメッセージが規約に従っている

## コーディング規約

### Backend (Java)

#### スタイル

Google Java Style Guideに従う：

```java
// Good
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(CreateUserRequest request) {
        // Implementation
    }
}

// Bad
public class UserService{
  private UserRepository userRepository;

  public User createUser(CreateUserRequest request){
    // Implementation
  }
}
```

#### アノテーション

```java
// Controller
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management API")
public class UserController {

    @GetMapping
    @Operation(summary = "List users", description = "Get paginated list of users")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        // Implementation
    }
}

// Service
@Service
@Transactional(readOnly = true)
public class UserService {

    @Transactional
    public User createUser(CreateUserRequest request) {
        // Implementation
    }
}
```

#### エラーハンドリング

```java
// Custom Exception
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("User not found: " + userId);
    }
}

// Global Exception Handler
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            "USER_NOT_FOUND",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

### Frontend (TypeScript/React)

#### コンポーネント

```typescript
// Good - Function Component with TypeScript
interface UserCardProps {
  user: User
  onEdit: (user: User) => void
  onDelete: (userId: string) => void
}

export function UserCard({ user, onEdit, onDelete }: UserCardProps) {
  const handleEdit = () => {
    onEdit(user)
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{user.displayName}</CardTitle>
      </CardHeader>
      <CardContent>
        {/* Content */}
      </CardContent>
    </Card>
  )
}

// Bad
export default function UserCard(props: any) {
  return <div>{props.user.name}</div>
}
```

#### Hooks

```typescript
// Custom Hook
export function useUsers() {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  useEffect(() => {
    const fetchUsers = async () => {
      try {
        setLoading(true)
        const data = await userService.listUsers()
        setUsers(data)
      } catch (err) {
        setError(err as Error)
      } finally {
        setLoading(false)
      }
    }

    fetchUsers()
  }, [])

  return { users, loading, error }
}
```

#### API Service

```typescript
// API Service
class UserService {
  private readonly baseUrl = '/api/v1/users'

  async listUsers(params?: ListUsersParams): Promise<PageResponse<User>> {
    const response = await apiClient.get<PageResponse<User>>(this.baseUrl, {
      params,
    })
    return response.data
  }

  async getUser(userId: string): Promise<User> {
    const response = await apiClient.get<User>(`${this.baseUrl}/${userId}`)
    return response.data
  }

  async createUser(request: CreateUserRequest): Promise<User> {
    const response = await apiClient.post<User>(this.baseUrl, request)
    return response.data
  }
}

export const userService = new UserService()
```

## デバッグ

### Backend

#### Spring Boot DevTools

`application-dev.yml`に設定:

```yaml
spring:
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
```

#### ログレベル

```yaml
logging:
  level:
    com.authplatform: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

#### IntelliJ IDEA デバッグ

1. ブレークポイントを設定
2. Debug 'AuthPlatformApplication' を実行
3. ステップ実行で変数を確認

### Frontend

#### React DevTools

```bash
# Chrome Extension
https://chrome.google.com/webstore/detail/react-developer-tools/
```

#### Console Logging

```typescript
// Development only logging
if (process.env.NODE_ENV === 'development') {
  console.log('User data:', user)
}
```

#### Next.js Debug Mode

```bash
NODE_OPTIONS='--inspect' pnpm dev
```

Chrome DevToolsで `chrome://inspect` を開く

## パフォーマンス最適化

### Backend

#### データベースクエリ最適化

```java
// N+1 問題を避ける
@EntityGraph(attributePaths = {"roles", "permissions"})
@Query("SELECT u FROM User u WHERE u.id = :id")
Optional<User> findByIdWithRolesAndPermissions(@Param("id") Long id);

// Projection を使用
public interface UserSummary {
    Long getId();
    String getUsername();
    String getEmail();
}

List<UserSummary> findAllProjectedBy();
```

#### キャッシング

```java
@Cacheable(value = "users", key = "#userId")
public User getUser(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
}

@CacheEvict(value = "users", key = "#user.id")
public User updateUser(User user) {
    return userRepository.save(user);
}
```

### Frontend

#### Code Splitting

```typescript
// Dynamic Import
const UserManagement = dynamic(() => import('@/components/UserManagement'), {
  loading: () => <Skeleton />,
  ssr: false,
})
```

#### Memo化

```typescript
// useMemo
const filteredUsers = useMemo(() => {
  return users.filter(user => user.status === 'ACTIVE')
}, [users])

// useCallback
const handleUserClick = useCallback(
  (userId: string) => {
    router.push(`/users/${userId}`)
  },
  [router]
)
```

#### Image Optimization

```tsx
import Image from 'next/image'

<Image
  src="/avatar.png"
  alt="User avatar"
  width={40}
  height={40}
  priority={false}
  loading="lazy"
/>
```

## 次のステップ

- [テストガイド](./TESTING.md) - テストの書き方と実行方法
- [API Integration Guide](./API_INTEGRATION_GUIDE.md) - APIの使用方法
- [Deployment Guide](./DEPLOYMENT.md) - デプロイメント手順
