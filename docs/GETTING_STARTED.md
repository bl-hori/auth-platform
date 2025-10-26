# Getting Started with Auth Platform

## 概要

Auth Platformは、エンタープライズグレードのAPIキーベース認証・認可システムです。ユーザー管理、ロールベースアクセス制御（RBAC）、ポリシー管理、監査ログ機能を提供します。

## アーキテクチャ

- **Backend**: Spring Boot 3.4 + Java 21
- **Frontend**: Next.js 15 + React 19 + TypeScript
- **Database**: PostgreSQL 17
- **Cache**: Redis 7
- **Infrastructure**: Docker Compose

## 前提条件

以下のソフトウェアがインストールされていることを確認してください：

- **Docker** 24.0+ および **Docker Compose** 2.20+
- **Node.js** 20.x (フロントエンド開発用)
- **pnpm** 9.x (フロントエンドパッケージマネージャー)
- **Java** 21 (バックエンド開発用)
- **Git** 2.40+

### インストール確認

```bash
# Docker
docker --version
docker compose version

# Node.js & pnpm
node --version
pnpm --version

# Java
java --version

# Git
git --version
```

## クイックスタート

### 1. リポジトリのクローン

```bash
git clone https://github.com/bl-hori/auth-platform.git
cd auth-platform
```

### 2. インフラストラクチャの起動

```bash
cd infrastructure
docker compose up -d
```

これにより以下のサービスが起動します：
- PostgreSQL (ポート: 5432)
- Redis (ポート: 6379)
- pgAdmin (ポート: 5050)

### 3. バックエンドの起動

```bash
cd ../backend
./gradlew bootRun
```

バックエンドは http://localhost:8080 で起動します。

### 4. フロントエンドの起動

新しいターミナルで：

```bash
cd frontend
pnpm install
pnpm dev
```

フロントエンドは http://localhost:3000 で起動します。

### 5. アプリケーションへのアクセス

1. ブラウザで http://localhost:3000 にアクセス
2. ログインページで任意のAPIキーを入力（開発モードではすべてのキーが受け入れられます）
3. ダッシュボードが表示されます

## 開発用APIキー

開発環境では、`.env.local`ファイルに設定されたAPIキーを使用できます：

```env
NEXT_PUBLIC_API_KEY=dev-api-key-12345
```

ログインページの「開発用APIキーを使用」ボタンをクリックすると、自動的に入力されます。

## サービスのURL

| サービス | URL | 説明 |
|---------|-----|------|
| Frontend | http://localhost:3000 | Next.jsアプリケーション |
| Backend API | http://localhost:8080 | Spring Boot REST API |
| API Docs | http://localhost:8080/swagger-ui.html | Swagger UI |
| pgAdmin | http://localhost:5050 | PostgreSQL管理ツール |
| SonarQube | http://localhost:9000 | コード品質分析 |

### pgAdmin接続情報

- **Email**: admin@example.com
- **Password**: admin
- **PostgreSQL Host**: postgres (Docker内) または localhost (ホストから)
- **Port**: 5432
- **Database**: authplatform
- **Username**: authuser
- **Password**: authpass

## 次のステップ

- [開発ガイド](./DEVELOPMENT.md) - 開発環境のセットアップと開発ワークフロー
- [テストガイド](./TESTING.md) - テストの実行方法
- [API統合ガイド](./API_INTEGRATION_GUIDE.md) - APIの使用方法
- [デプロイメントガイド](./DEPLOYMENT.md) - 本番環境へのデプロイ

## トラブルシューティング

### ポートが既に使用されている

```bash
# 使用中のポートを確認
sudo lsof -i :3000  # フロントエンド
sudo lsof -i :8080  # バックエンド
sudo lsof -i :5432  # PostgreSQL

# プロセスを停止
kill -9 <PID>
```

### Dockerコンテナが起動しない

```bash
# コンテナの状態を確認
docker compose ps

# ログを確認
docker compose logs postgres
docker compose logs redis

# コンテナを再起動
docker compose down
docker compose up -d
```

### データベース接続エラー

```bash
# PostgreSQLが起動しているか確認
docker compose ps postgres

# データベースに接続できるか確認
docker compose exec postgres psql -U authuser -d authplatform

# バックエンドの接続設定を確認
cat backend/src/main/resources/application-dev.yml
```

### フロントエンドのビルドエラー

```bash
# node_modulesを削除して再インストール
cd frontend
rm -rf node_modules pnpm-lock.yaml
pnpm install

# キャッシュをクリア
rm -rf .next
pnpm dev
```

## サポート

問題が発生した場合：

1. [トラブルシューティングガイド](./TROUBLESHOOTING.md)を確認
2. [GitHub Issues](https://github.com/bl-hori/auth-platform/issues)で既存の問題を検索
3. 新しいIssueを作成（問題が見つからない場合）

## ライセンス

このプロジェクトはMITライセンスの下で公開されています。
