# Auth Platform Frontend

Enterprise Authorization Platform のフロントエンドアプリケーションです。

## 技術スタック

- **Framework**: Next.js 15 (App Router)
- **Language**: TypeScript 5.7
- **Styling**: Tailwind CSS 3.4
- **Package Manager**: pnpm
- **Linting**: ESLint 9 with Next.js config

## 開発環境のセットアップ

### 必要要件

- Node.js 18.17以上
- pnpm 10.17以上

### インストール

```bash
# 依存関係のインストール
pnpm install

# 環境変数の設定
cp .env.example .env.local
```

### 開発サーバーの起動

```bash
# 開発サーバーを起動（デフォルト: http://localhost:3000）
pnpm dev

# ポートを指定して起動
pnpm dev -- -p 3001
```

## スクリプト

```bash
# 開発サーバー起動
pnpm dev

# 本番ビルド
pnpm build

# 本番サーバー起動
pnpm start

# リンティング
pnpm lint

# 型チェック
pnpm type-check

# E2Eテスト実行
pnpm test:e2e

# E2EテストUI実行（デバッグ用）
pnpm test:e2e:ui
```

## プロジェクト構造

```
frontend/
├── src/
│   ├── app/           # Next.js App Router
│   │   ├── globals.css
│   │   ├── layout.tsx
│   │   └── page.tsx
│   ├── components/    # Reactコンポーネント
│   ├── lib/          # ユーティリティ関数
│   └── types/        # TypeScript型定義
├── public/           # 静的ファイル
├── next.config.ts    # Next.js設定
├── tailwind.config.ts # Tailwind CSS設定
├── tsconfig.json     # TypeScript設定
└── package.json      # プロジェクト設定
```

## 環境変数

### バックエンドAPI連携

フロントエンドは **モックデータ** と **実際のバックエンドAPI** の両方をサポートしています。

#### 1. .env.local ファイルの作成

```bash
# .env.local.example をコピー
cp .env.local.example .env.local
```

#### 2. バックエンドAPIを使用する場合

`.env.local` を以下のように設定:

```bash
# Backend API Configuration
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_API_KEY=dev-key-org1-abc123

# Mock API Control
# Set to 'false' to use real backend API
NEXT_PUBLIC_USE_MOCK_API=false
```

**重要**:
- `NEXT_PUBLIC_USE_MOCK_API=false` を設定すると実際のバックエンドAPIに接続します
- この設定を省略または `true` にすると、モックデータが使用されます
- 環境変数を変更した後は、開発サーバーを再起動してください

#### 3. モックデータのみで開発する場合

```bash
# Mock API Control
NEXT_PUBLIC_USE_MOCK_API=true  # または省略
```

### トラブルシューティング

#### 問題: フロントエンドがバックエンドに接続できない

**症状**: フロントエンドを開いてもモックデータのみが表示される

**原因と解決方法**:

1. **環境変数が未設定**
   ```bash
   # .env.local の確認
   cat .env.local

   # NEXT_PUBLIC_USE_MOCK_API=false が設定されているか確認
   # コメントアウト(#)されている場合は外す
   ```

2. **開発サーバーの再起動が必要**
   ```bash
   # Ctrl+C で停止してから再起動
   pnpm dev
   ```

3. **バックエンドが起動していない**
   ```bash
   # バックエンドが起動しているか確認
   curl -H "X-API-Key: dev-key-org1-abc123" http://localhost:8080/v1/policies

   # エラーが返る場合はバックエンドを起動
   cd ../backend
   ./gradlew bootRun
   ```

4. **API Keyが間違っている**
   ```bash
   # .env.local の API Key を確認
   # 正しい値: dev-key-org1-abc123
   NEXT_PUBLIC_API_KEY=dev-key-org1-abc123
   ```

#### 問題: CORS エラーが発生する

**症状**: ブラウザコンソールに CORS エラーが表示される

**解決方法**: バックエンドの CORS 設定を確認
```yaml
# backend/src/main/resources/application.yml
auth-platform:
  security:
    cors:
      allowed-origins: "http://localhost:3000"
```

詳細は [`/docs/FRONTEND_BACKEND_INTEGRATION.md`](../docs/FRONTEND_BACKEND_INTEGRATION.md) を参照してください。

## E2Eテスト

フロントエンドは **Playwright** を使用したEnd-to-End (E2E) テストを提供しています。

### 必要要件

#### システム依存関係

Playwright Chromium を実行するには、以下のシステムライブラリが必要です:

- `libnspr4` - Netscape Portable Runtime
- `libnss3` - Network Security Services (SSL/TLS サポート)
- `libasound2t64` - ALSA サウンドライブラリ

### セットアップ

#### 1. Playwright ブラウザのインストール

```bash
cd frontend
pnpm exec playwright install chromium
```

#### 2. システム依存関係のインストール

**方法A: Playwright CLI（推奨）**

```bash
# すべての必要な依存関係を自動インストール
sudo pnpm exec playwright install-deps chromium
```

**方法B: 手動 apt-get インストール**

sudo 権限が制限されている環境の場合:

```bash
# 最小限の依存関係をインストール
sudo apt-get update
sudo apt-get install -y libnspr4 libnss3 libasound2t64
```

#### 3. インストールの確認

```bash
# システム依存関係が正しくインストールされているか確認
dpkg -l | grep -E 'libnspr4|libnss3|libasound2'

# すべてのパッケージが "ii" ステータスであることを確認
```

### テストの実行

#### すべてのE2Eテストを実行

```bash
cd frontend
pnpm test:e2e
```

テストは自動的に開発サーバー (`http://localhost:3000`) を起動し、すべてのテストを実行します。

**期待される結果:**
- ✅ 15個すべてのテストが合格
- ⏱️ 実行時間: 2分未満
- 📊 テストレポート: `playwright-report/index.html`

#### 特定のテストファイルを実行

```bash
# 認証テストのみ実行
pnpm test:e2e 01-basic-auth

# ナビゲーションテストのみ実行
pnpm test:e2e 02-navigation

# ユーザー一覧テストのみ実行
pnpm test:e2e 03-users-list
```

#### UIモードでテストを実行（デバッグ用）

```bash
pnpm test:e2e:ui
```

Playwright UI が起動し、テストをステップごとに確認できます。

### テスト構造

```
frontend/e2e/
├── 01-basic-auth.spec.ts        # 6テスト - 認証フロー
│   ├── ログインページ表示
│   ├── 空のAPIキーバリデーション
│   ├── ログイン成功
│   ├── リロード時の認証永続化
│   ├── ログアウト機能
│   └── 保護されたルートへのリダイレクト
│
├── 02-navigation.spec.ts         # 6テスト - ナビゲーション
│   ├── ユーザーページへの遷移
│   ├── ロールページへの遷移
│   ├── パーミッションページへの遷移
│   ├── ポリシーページへの遷移
│   ├── 監査ログページへの遷移
│   └── ダッシュボードへの戻る
│
└── 03-users-list.spec.ts         # 3テスト - ユーザー一覧機能
    ├── ページ表示
    ├── 検索機能
    └── ユーザー作成ボタン
```

**合計**: 15 E2Eテスト

### テストレポート

テスト実行後、以下の成果物が生成されます:

```bash
# テストレポートをブラウザで開く
open playwright-report/index.html

# または自動的に開く
pnpm exec playwright show-report
```

**テスト失敗時に自動生成される成果物:**
- 📸 **スクリーンショット**: 失敗時点の画面キャプチャ
- 🎥 **ビデオ**: テスト実行全体の録画
- 🔍 **トレース**: ステップごとの詳細分析

### トラブルシューティング

#### 問題: ブラウザ依存関係が見つからない

**症状**:
```
Error: browserType.launch:
Host system is missing dependencies to run browsers.
Missing dependencies:
  - libnspr4
  - libnss3
  - libasound2t64
```

**解決方法**:
```bash
# システム依存関係をインストール
sudo pnpm exec playwright install-deps chromium

# または手動インストール
sudo apt-get install -y libnspr4 libnss3 libasound2t64
```

#### 問題: 開発サーバーのタイムアウト

**症状**:
```
Error: Timed out waiting for http://localhost:3000 to be available
```

**解決方法**:
```bash
# ポート3000が既に使用されているか確認
lsof -i :3000

# プロセスを停止
kill -9 <PID>

# または別のポートで開発サーバーを起動
pnpm dev -- -p 3001

# playwright.config.ts の baseURL も変更する必要があります
```

#### 問題: テストがフレーキー（不安定）

**症状**: テストが時々失敗する

**解決方法**:
- テストには自動リトライが設定されています（ローカル: 1回、CI: 2回）
- `waitForLoadState('networkidle')` を使用して安定性を確保
- トレースファイル (`playwright-report/`) で詳細を確認

#### 問題: Docker環境でテストが失敗

**解決方法**:

公式の Playwright Docker イメージを使用:

```bash
# Docker コンテナでテストを実行
docker run -it --rm \
  -v $(pwd):/work \
  -w /work/frontend \
  mcr.microsoft.com/playwright:v1.56.1-jammy \
  pnpm test:e2e
```

### CI/CD での実行

GitHub Actions での E2E テスト実行例:

```yaml
name: E2E Tests

on: [push, pull_request]

jobs:
  e2e-tests:
    runs-on: ubuntu-latest
    container:
      image: mcr.microsoft.com/playwright:v1.56.1-jammy
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
      - name: Install dependencies
        run: pnpm install
        working-directory: frontend
      - name: Run E2E tests
        run: pnpm test:e2e
        working-directory: frontend
      - uses: actions/upload-artifact@v4
        if: failure()
        with:
          name: playwright-report
          path: frontend/playwright-report/
          retention-days: 30
```

### 設定

E2Eテストの設定は `playwright.config.ts` で管理されています:

```typescript
{
  testDir: './e2e',
  timeout: 30000,              // テストタイムアウト: 30秒
  fullyParallel: true,         // 並列実行
  retries: process.env.CI ? 2 : 1,  // リトライ回数

  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',   // 最初のリトライ時にトレース
    screenshot: 'only-on-failure',  // 失敗時のみスクリーンショット
    video: 'retain-on-failure'      // 失敗時のみビデオ保存
  },

  webServer: {
    command: 'pnpm dev',
    url: 'http://localhost:3000',
    timeout: 120000,           // サーバー起動タイムアウト: 2分
    reuseExistingServer: !process.env.CI
  }
}
```

## ビルド

```bash
# 本番用ビルド
pnpm build

# ビルド結果の確認
pnpm start
```

## デプロイ

本番環境へのデプロイ方法は、プロジェクトルートの `docs/` ディレクトリを参照してください。

## License

Proprietary
