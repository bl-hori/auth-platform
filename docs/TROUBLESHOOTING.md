# トラブルシューティングガイド

Auth Platformでよくある問題とその解決方法をまとめたガイドです。

## 目次

- [一般的な問題](#一般的な問題)
- [Backend の問題](#backend-の問題)
- [Frontend の問題](#frontend-の問題)
- [Database の問題](#database-の問題)
- [Docker の問題](#docker-の問題)
- [パフォーマンスの問題](#パフォーマンスの問題)
- [デバッグツール](#デバッグツール)

## 一般的な問題

### アプリケーションが起動しない

#### 症状
```
Error: Cannot start application
```

#### 確認手順

1. **全サービスの状態を確認**
```bash
# Docker Compose
docker compose ps

# Kubernetes
kubectl get pods -n auth-platform
```

2. **ログを確認**
```bash
# Docker
docker compose logs backend
docker compose logs frontend

# Kubernetes
kubectl logs -f deployment/auth-platform-backend -n auth-platform
```

3. **ポートの競合を確認**
```bash
# ポート8080が使用されているか確認
sudo lsof -i :8080

# ポート3000が使用されているか確認
sudo lsof -i :3000

# プロセスを停止
kill -9 <PID>
```

### 接続エラー

#### 症状
```
Connection refused
Failed to connect to database
```

#### 解決方法

**PostgreSQLへの接続**
```bash
# データベースが起動しているか確認
docker compose ps postgres

# データベースに直接接続してテスト
docker compose exec postgres psql -U authuser -d authplatform

# 接続文字列を確認
# jdbc:postgresql://localhost:5432/authplatform
```

**Redisへの接続**
```bash
# Redisが起動しているか確認
docker compose ps redis

# Redis CLIでテスト
docker compose exec redis redis-cli
> PING
PONG

# パスワード認証が必要な場合
docker compose exec redis redis-cli -a <password>
```

## Backend の問題

### JWT認証エラー

#### 症状: 401 Unauthorized - Invalid JWT token

```json
{
  "timestamp": "2024-01-28T10:15:30.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid JWT token: Signature verification failed"
}
```

#### 解決方法

1. **JWT署名を確認**
```bash
# JWTをデコードして内容確認 (https://jwt.io)
echo "YOUR_JWT" | cut -d. -f2 | base64 -d | jq
```

2. **公開鍵の確認**
```bash
# Keycloak公開鍵エンドポイントにアクセス
curl http://localhost:8180/realms/authplatform/protocol/openid-connect/certs | jq
```

3. **Issuer URIの確認**
```yaml
# application.ymlの設定確認
authplatform:
  keycloak:
    issuer-uri: http://localhost:8180/realms/authplatform  # JWTのissと一致する必要あり
```

#### 症状: JWT expired

```json
{
  "message": "Invalid JWT token: JWT expired"
}
```

#### 解決方法

1. **トークンの有効期限を確認**
```bash
# expクレームを確認
echo "YOUR_JWT" | cut -d. -f2 | base64 -d | jq '.exp'
date -d @<EXP_VALUE>
```

2. **新しいトークンを取得**
```bash
curl -X POST "http://localhost:8180/realms/authplatform/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=auth-platform-backend" \
  -d "client_secret=YOUR_SECRET" \
  -d "grant_type=client_credentials"
```

3. **Clock Skewの調整** (システム時刻がずれている場合)
```yaml
authplatform:
  keycloak:
    jwt:
      clock-skew-seconds: 60  # 許容時間を60秒に増やす
```

#### 症状: Missing organization_id claim

```json
{
  "message": "JWT token missing required claim: organization_id"
}
```

#### 解決方法

1. **Keycloakでorganization_id mapperを追加**
   - Keycloak Admin Console → Clients → auth-platform-backend → Mappers
   - Create → Protocol: openid-connect
   - Mapper Type: User Attribute
   - User Attribute: `organization_id`
   - Token Claim Name: `organization_id`
   - Add to ID token: ON
   - Add to access token: ON

2. **ユーザーにorganization_id属性を設定**
   - Users → Select user → Attributes
   - Key: `organization_id`
   - Value: `your-org-uuid`

3. **JWTの内容を確認**
```bash
echo "YOUR_JWT" | cut -d. -f2 | base64 -d | jq '.organization_id'
```

#### 症状: Organization not found

```json
{
  "message": "Authentication failed: Organization not found: org-uuid-123"
}
```

#### 解決方法

1. **組織がデータベースに存在するか確認**
```sql
SELECT * FROM organizations WHERE id = 'org-uuid-123';
```

2. **組織を作成**
```bash
curl -X POST "http://localhost:8080/v1/organizations" \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-org",
    "displayName": "Test Organization"
  }'
```

3. **JWT claimのorganization_idを更新**
   - Keycloak User Attributes を修正

#### 症状: JIT Provisioning failed

```
ERROR [JwtAuthenticationFilter] User provisioning failed: ...
```

#### 解決方法

1. **ログを確認**
```bash
tail -f logs/application.log | grep "JIT provisioning"
```

2. **データベース権限を確認**
```sql
-- usersテーブルへのINSERT権限確認
SELECT grantee, privilege_type
FROM information_schema.role_table_grants
WHERE table_name='users';
```

3. **トランザクションタイムアウトを増やす**
```yaml
spring:
  jpa:
    properties:
      javax.persistence.query.timeout: 5000  # 5秒
```

### Spring Boot アプリケーションが起動しない

#### 症状
```
Error creating bean with name 'dataSource'
Failed to configure a DataSource
```

#### 解決方法

1. **application.ymlの設定を確認**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/authplatform
    username: authuser
    password: authpass
```

2. **環境変数を確認**
```bash
echo $SPRING_DATASOURCE_URL
echo $SPRING_DATASOURCE_USERNAME
```

3. **データベースマイグレーションのエラー**
```bash
# Flyway/Liquibaseのマイグレーション履歴を確認
docker compose exec postgres psql -U authuser -d authplatform -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;"

# 失敗したマイグレーションを修復
./gradlew flywayRepair
```

### Java OutOfMemory エラー

#### 症状
```
java.lang.OutOfMemoryError: Java heap space
```

#### 解決方法

1. **ヒープサイズを増やす**
```bash
# Gradle
export JAVA_OPTS="-Xmx2g -Xms512m"
./gradlew bootRun

# Docker
docker run -e JAVA_OPTS="-Xmx2g" auth-platform-backend
```

2. **メモリリークを調査**
```bash
# Heap Dumpを取得
jmap -dump:live,format=b,file=heap.bin <PID>

# VisualVMで分析
visualvm
```

### API レスポンスが遅い

#### 確認手順

1. **ログレベルを上げる**
```yaml
logging:
  level:
    com.authplatform: DEBUG
    org.hibernate.SQL: DEBUG
```

2. **N+1クエリの確認**
```
# ログに複数のSELECTクエリが表示されている場合
Hibernate: select user0_.id...
Hibernate: select role0_.id...
Hibernate: select role1_.id...
```

**解決**: `@EntityGraph`を使用
```java
@EntityGraph(attributePaths = {"roles", "permissions"})
@Query("SELECT u FROM User u WHERE u.id = :id")
Optional<User> findByIdWithRolesAndPermissions(@Param("id") Long id);
```

3. **キャッシュの確認**
```bash
# Redis接続確認
docker compose exec redis redis-cli
> INFO stats
> KEYS *
```

## Frontend の問題

### Next.js ビルドエラー

#### 症状
```
Error: Cannot find module 'next'
Module not found: Can't resolve '@/components/...'
```

#### 解決方法

1. **依存関係を再インストール**
```bash
cd frontend
rm -rf node_modules pnpm-lock.yaml
pnpm install
```

2. **キャッシュをクリア**
```bash
rm -rf .next
pnpm dev
```

3. **TypeScript エラー**
```bash
# 型チェック
pnpm tsc --noEmit

# 型定義の再生成
pnpm types:generate
```

### APIリクエストが失敗する

#### 症状
```
Failed to fetch
Network error
CORS error
```

#### 解決方法

1. **APIのURLを確認**
```bash
# .env.local
NEXT_PUBLIC_API_URL=http://localhost:8080

# 環境変数が読み込まれているか確認
console.log(process.env.NEXT_PUBLIC_API_URL)
```

2. **CORSエラー**

Backendの設定を確認:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true);
    }
}
```

3. **ネットワークタブでリクエストを確認**
- ブラウザの開発者ツール → Network タブ
- リクエストURL、ステータスコード、レスポンスを確認

### Hydration エラー

#### 症状
```
Error: Hydration failed
Text content does not match server-rendered HTML
```

#### 解決方法

1. **SSRとクライアントで異なるコンテンツを返している**
```typescript
// Bad
export default function Page() {
  return <div>{new Date().toString()}</div>
}

// Good - useEffectでクライアントサイドのみ実行
export default function Page() {
  const [date, setDate] = useState('')

  useEffect(() => {
    setDate(new Date().toString())
  }, [])

  return <div>{date}</div>
}
```

2. **suppressHydrationWarningを使用** (一時的な対処)
```typescript
<div suppressHydrationWarning>{dynamicContent}</div>
```

## Database の問題

### データベース接続が切れる

#### 症状
```
Connection is closed
Could not open JPA EntityManager for transaction
```

#### 解決方法

1. **コネクションプール設定**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

2. **タイムアウト設定**
```yaml
spring:
  datasource:
    hikari:
      connection-test-query: SELECT 1
      validation-timeout: 5000
```

### マイグレーションエラー

#### 症状
```
Migration checksum mismatch
Migration failed
```

#### 解決方法

1. **Flywayの修復**
```bash
./gradlew flywayRepair
```

2. **マイグレーション履歴をリセット** (開発環境のみ)
```sql
-- マイグレーション履歴をクリア
DELETE FROM flyway_schema_history;

-- 再実行
./gradlew flywayMigrate
```

3. **本番環境での対応**
```bash
# バックアップを取得
pg_dump -U authuser authplatform > backup.sql

# マイグレーション履歴を確認
./gradlew flywayInfo

# 手動で修正
./gradlew flywayValidate
```

### データベースパフォーマンスが悪い

#### 確認方法

1. **スロークエリログを有効化**
```sql
-- PostgreSQL
ALTER DATABASE authplatform SET log_min_duration_statement = 1000;
```

2. **実行計画を確認**
```sql
EXPLAIN ANALYZE
SELECT * FROM users WHERE username = 'test';
```

3. **インデックスを追加**
```sql
-- 頻繁に検索されるカラムにインデックス
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

## Docker の問題

### コンテナが起動しない

#### 症状
```
Error response from daemon
Container exited with code 1
```

#### 解決方法

1. **ログを確認**
```bash
docker compose logs <service-name>
docker logs <container-id>
```

2. **コンテナの状態を確認**
```bash
docker compose ps
docker inspect <container-id>
```

3. **コンテナを再起動**
```bash
# 特定のサービスを再起動
docker compose restart backend

# 全サービスを再起動
docker compose down
docker compose up -d
```

### ディスク容量不足

#### 症状
```
no space left on device
Cannot create container
```

#### 解決方法

1. **ディスク使用状況を確認**
```bash
docker system df
```

2. **未使用のリソースを削除**
```bash
# 停止中のコンテナを削除
docker container prune

# 未使用のイメージを削除
docker image prune -a

# 未使用のボリュームを削除
docker volume prune

# 全て削除
docker system prune -a --volumes
```

### ネットワークエラー

#### 症状
```
network <name> not found
could not find an available IP address
```

#### 解決方法

1. **ネットワークを再作成**
```bash
docker network ls
docker network rm <network-name>
docker compose up -d
```

2. **IPアドレスの競合を解決**
```bash
# すべてのネットワークを削除
docker network prune

# Docker Composeを再起動
docker compose down
docker compose up -d
```

## パフォーマンスの問題

### 高負荷時のパフォーマンス低下

#### 確認手順

1. **CPU使用率を確認**
```bash
# Docker
docker stats

# システム
top
htop
```

2. **メモリ使用量を確認**
```bash
free -h
docker stats --no-stream
```

3. **スレッドダンプを取得**
```bash
# Java
jstack <PID> > thread-dump.txt

# ビジュアル分析
jvisualvm
```

#### 解決方法

1. **コネクションプールのチューニング**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50  # 増やす
      minimum-idle: 10
```

2. **キャッシュの最適化**
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10分
```

3. **非同期処理の活用**
```java
@Async
public CompletableFuture<List<User>> findUsersAsync() {
    return CompletableFuture.completedFuture(
        userRepository.findAll()
    );
}
```

### データベースのボトルネック

#### 確認方法

1. **クエリパフォーマンスを監視**
```sql
-- 実行中のクエリ
SELECT pid, query, state, query_start
FROM pg_stat_activity
WHERE state = 'active';

-- スロークエリ
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
ORDER BY total_time DESC
LIMIT 10;
```

2. **インデックスの使用状況**
```sql
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC;
```

#### 解決方法

1. **Read Replicaの導入**
```yaml
spring:
  datasource:
    hikari:
      # Primary (Write)
      jdbc-url: jdbc:postgresql://primary:5432/authplatform

    replica:
      hikari:
        # Replica (Read)
        jdbc-url: jdbc:postgresql://replica:5432/authplatform
        read-only: true
```

2. **クエリの最適化**
```java
// Bad: N+1 problem
List<User> users = userRepository.findAll();
for (User user : users) {
    user.getRoles().size();  // 追加クエリ
}

// Good: Eager loading
@EntityGraph(attributePaths = {"roles"})
List<User> users = userRepository.findAll();
```

## デバッグツール

### Backend デバッグ

1. **Spring Boot Actuator**
```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Thread dump
curl http://localhost:8080/actuator/threaddump

# Heap dump
curl http://localhost:8080/actuator/heapdump -o heap.hprof
```

2. **リモートデバッグ**
```bash
# JVM起動オプション
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar app.jar

# IntelliJ IDEA
Run → Edit Configurations → Add New Configuration → Remote JVM Debug
```

### Frontend デバッグ

1. **React DevTools**
- Chrome Extension: React Developer Tools
- コンポーネントツリーの確認
- Props/State の検査

2. **Next.js Debug Mode**
```bash
# Node.js デバッガー
NODE_OPTIONS='--inspect' pnpm dev

# Chrome DevTools
chrome://inspect
```

3. **Network 監視**
```javascript
// Axiosリクエストロギング
axios.interceptors.request.use(request => {
  console.log('Request:', request)
  return request
})

axios.interceptors.response.use(response => {
  console.log('Response:', response)
  return response
})
```

### Database デバッグ

1. **pgAdmin**
```bash
# Docker Compose経由でアクセス
http://localhost:5050

# ログイン
Email: admin@example.com
Password: admin
```

2. **psql CLI**
```bash
# データベースに接続
docker compose exec postgres psql -U authuser -d authplatform

# テーブル一覧
\dt

# テーブル構造
\d users

# クエリ実行
SELECT * FROM users LIMIT 10;
```

## よくある質問 (FAQ)

### Q: ログインできません

A: 以下を確認してください：
1. バックエンドが起動しているか (`http://localhost:8080/actuator/health`)
2. データベースに接続できているか
3. 開発環境では任意のAPIキーで動作します
4. ブラウザのコンソールでエラーを確認

### Q: E2Eテストが失敗します

A: [テストガイド](./TESTING.md)を参照し、以下を確認：
1. フロントエンド・バックエンドが起動しているか
2. データベースがクリーンな状態か
3. `pnpm test:e2e:ui`でUIモードでデバッグ

### Q: デプロイ後にアプリケーションが動作しません

A: [デプロイメントガイド](./DEPLOYMENT.md)のチェックリストを確認：
1. 環境変数が正しく設定されているか
2. データベースマイグレーションが完了しているか
3. ログにエラーがないか
4. ヘルスチェックが成功しているか

## サポート

問題が解決しない場合：

1. [GitHub Issues](https://github.com/bl-hori/auth-platform/issues)で検索
2. 新しいIssueを作成（以下の情報を含める）:
   - 問題の詳細な説明
   - 再現手順
   - エラーログ
   - 環境情報（OS、Dockerバージョン等）
   - 期待される動作

## 関連ドキュメント

- [Getting Started](./GETTING_STARTED.md)
- [Development Guide](./DEVELOPMENT.md)
- [Testing Guide](./TESTING.md)
- [Deployment Guide](./DEPLOYMENT.md)
