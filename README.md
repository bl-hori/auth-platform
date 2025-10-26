# Auth Platform - 認可基盤プラットフォーム

エンタープライズグレードのAPIキーベース認証・認可システム。ユーザー管理、ロールベースアクセス制御（RBAC）、ポリシー管理、監査ログ機能を提供します。

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/bl-hori/auth-platform)
[![Test Coverage](https://img.shields.io/badge/coverage-85%25-green)](./docs/TESTING.md)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## 🚀 クイックスタート

```bash
# 1. リポジトリのクローン
git clone https://github.com/bl-hori/auth-platform.git
cd auth-platform

# 2. インフラストラクチャの起動
cd infrastructure
docker compose up -d

# 3. バックエンドの起動
cd ../backend
./gradlew bootRun

# 4. フロントエンドの起動
cd ../frontend
pnpm install
pnpm dev
```

📖 詳細は [Getting Started Guide](./docs/GETTING_STARTED.md) を参照

## 📚 ドキュメント

### はじめに
- **[Getting Started](./docs/GETTING_STARTED.md)** - 初回セットアップガイド
- **[Development Guide](./docs/DEVELOPMENT.md)** - 開発ガイド
- **[Testing Guide](./docs/TESTING.md)** - テストガイド
- **[API Integration Guide](./docs/API_INTEGRATION_GUIDE.md)** - API統合ガイド

### 運用
- **[Deployment Guide](./docs/DEPLOYMENT.md)** - デプロイメントガイド
- **[Troubleshooting Guide](./docs/TROUBLESHOOTING.md)** - トラブルシューティング

### 詳細
- **[Documentation Index](./docs/README.md)** - 全ドキュメント一覧

## 🏗️ アーキテクチャ

```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │ HTTPS
       ↓
┌─────────────┐
│  Next.js 15 │ ← Frontend (React 19 + TypeScript)
└──────┬──────┘
       │ REST API
       ↓
┌─────────────┐
│Spring Boot 3│ ← Backend (Java 21)
└──────┬──────┘
       │
       ├───────────┬──────────┐
       ↓           ↓          ↓
┌──────────┐ ┌─────────┐ ┌────────┐
│PostgreSQL│ │  Redis  │ │ Others │
└──────────┘ └─────────┘ └────────┘
```

## 🛠️ 技術スタック

### Backend
- **Framework**: Spring Boot 3.4
- **Language**: Java 21
- **Database**: PostgreSQL 17
- **Cache**: Redis 7
- **Build Tool**: Gradle 8.10

### Frontend
- **Framework**: Next.js 15
- **UI Library**: React 19
- **Language**: TypeScript 5.6
- **Styling**: Tailwind CSS + shadcn/ui
- **Package Manager**: pnpm 9

### Testing
- **Unit**: JUnit 5, Jest
- **Integration**: Spring Boot Test, React Testing Library
- **E2E**: Playwright
- **Performance**: Gatling
- **Security**: OWASP ZAP, Trivy

### Infrastructure
- **Containerization**: Docker, Docker Compose
- **CI/CD**: GitHub Actions
- **Code Quality**: SonarQube

## ✨ 主要機能

### 1. ユーザー管理
- ✅ ユーザーの作成・編集・削除
- ✅ ステータス管理（有効/無効/停止中）
- ✅ 検索・フィルタリング
- ✅ ページネーション

### 2. ロールベースアクセス制御 (RBAC)
- ✅ ロールの定義と管理
- ✅ 権限の割り当て
- ✅ 階層的なロール構造

### 3. ポリシー管理
- ✅ 柔軟なポリシー定義
- ✅ 条件ベースの認可
- ✅ リアルタイム更新

### 4. 監査ログ
- ✅ 全ての操作を記録
- ✅ 詳細なフィルタリング
- ✅ エクスポート機能

### 5. パフォーマンス
- ✅ 10,000+ req/s のスループット
- ✅ p95 レスポンスタイム <200ms
- ✅ 認可チェック p95 <10ms (Redis キャッシュ)

## 📊 プロジェクトステータス

### Phase 1: MVPリリース (完了 ✅)

| カテゴリ | 進捗 | 詳細 |
|---------|------|------|
| Backend実装 | 100% | Spring Boot基盤、全CRUD API |
| Frontend実装 | 100% | Next.js、全管理画面 |
| データベース | 100% | PostgreSQL、マイグレーション |
| キャッシュ | 100% | Redis統合 |
| 認証・認可 | 100% | APIキーベース認証 |
| テスト | 100% | 単体・統合・E2E・パフォーマンス |
| ドキュメント | 100% | 包括的なドキュメント |

### テストカバレッジ

- **Backend**: 85%+ (Line Coverage)
- **Frontend**: 80%+ (Statement Coverage)
- **E2E**: Phase 1 完了 (15/15 テスト成功)

詳細は [Testing Guide](./docs/TESTING.md) を参照

## 🚦 サービスのURL

| サービス | URL | 説明 |
|---------|-----|------|
| Frontend | http://localhost:3000 | Next.jsアプリケーション |
| Backend API | http://localhost:8080 | Spring Boot REST API |
| API Docs | http://localhost:8080/swagger-ui.html | Swagger UI |
| pgAdmin | http://localhost:5050 | PostgreSQL管理ツール |
| SonarQube | http://localhost:9000 | コード品質分析 |

## 🔧 開発

### 環境構築

```bash
# 必要なツール
- Docker 24.0+
- Docker Compose 2.20+
- Node.js 20.x
- pnpm 9.x
- Java 21
- Git 2.40+
```

### ブランチ戦略

```bash
main       # 本番環境
└── feature/*   # 新機能
└── fix/*       # バグ修正
└── docs/*      # ドキュメント
```

### コミット規約

Conventional Commits形式を使用：

```
feat(api): add GET /v1/users endpoint
fix(ui): prevent duplicate form submission
docs(readme): update installation instructions
test(e2e): add user management tests
```

詳細は [Development Guide](./docs/DEVELOPMENT.md#コミットメッセージ規約) を参照

## 🧪 テスト

### Backend

```bash
cd backend

# 全テスト実行
./gradlew test

# カバレッジレポート生成
./gradlew test jacocoTestReport
```

### Frontend

```bash
cd frontend

# 単体テスト
pnpm test

# E2Eテスト
pnpm test:e2e

# E2E UIモード（デバッグ用）
pnpm test:e2e:ui
```

### パフォーマンステスト

```bash
cd backend

# 基本負荷テスト (50 users / 30s)
./gradlew gatlingRun-authplatform.simulations.BasicLoadSimulation

# ストレステスト (10,000+ req/s)
./gradlew gatlingRun-authplatform.simulations.StressTestSimulation
```

詳細は [Testing Guide](./docs/TESTING.md) を参照

## 🚀 デプロイメント

### Docker Compose (推奨)

```bash
# 本番用設定でデプロイ
cd infrastructure
docker compose -f docker-compose.prod.yml up -d
```

### Kubernetes

```bash
# Kubernetesにデプロイ
kubectl apply -f infrastructure/k8s/
```

詳細は [Deployment Guide](./docs/DEPLOYMENT.md) を参照

## 📁 プロジェクト構造

```
auth-platform/
├── backend/                    # Spring Boot バックエンド
│   ├── src/main/java/         # Javaソースコード
│   ├── src/test/java/         # テストコード
│   ├── src/gatling/           # パフォーマンステスト
│   └── build.gradle           # Gradle設定
│
├── frontend/                   # Next.js フロントエンド
│   ├── src/app/               # App Router
│   ├── src/components/        # Reactコンポーネント
│   ├── src/services/          # APIサービス
│   ├── e2e/                   # E2Eテスト
│   └── package.json           # npm設定
│
├── infrastructure/             # インフラストラクチャ
│   ├── docker-compose.yml     # 開発環境設定
│   ├── docker-compose.prod.yml # 本番環境設定
│   └── k8s/                   # Kubernetes設定
│
├── docs/                       # ドキュメント
│   ├── GETTING_STARTED.md     # 初回セットアップ
│   ├── DEVELOPMENT.md         # 開発ガイド
│   ├── TESTING.md             # テストガイド
│   ├── DEPLOYMENT.md          # デプロイメント
│   └── TROUBLESHOOTING.md     # トラブルシューティング
│
├── specifications/             # 仕様書
└── openspec/                   # OpenSpec変更管理
```

## 🤝 コントリビューション

貢献を歓迎します！以下の手順に従ってください：

1. このリポジトリをFork
2. 機能ブランチを作成 (`git checkout -b feature/amazing-feature`)
3. 変更をコミット (`git commit -m 'feat: add amazing feature'`)
4. ブランチにプッシュ (`git push origin feature/amazing-feature`)
5. Pull Requestを作成

詳細は [Development Guide](./docs/DEVELOPMENT.md#開発ワークフロー) を参照

## 📝 ライセンス

このプロジェクトはMITライセンスの下で公開されています。

## 🆘 サポート

問題が発生した場合：

1. [Troubleshooting Guide](./docs/TROUBLESHOOTING.md)を確認
2. [GitHub Issues](https://github.com/bl-hori/auth-platform/issues)で検索
3. 新しいIssueを作成

## 🔗 リンク

- [Documentation](./docs/README.md) - 全ドキュメント
- [API Documentation](./docs/API_INTEGRATION_GUIDE.md) - API仕様
- [GitHub Issues](https://github.com/bl-hori/auth-platform/issues) - バグ報告・機能リクエスト
- [Change Log](./CHANGELOG.md) - 変更履歴

---

**開発**: Auth Platform Team
**最終更新**: 2025-10-27
**バージョン**: 1.0.0 (Phase 1 MVP)
