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

`.env.example` を参照してください。

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
