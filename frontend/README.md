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
