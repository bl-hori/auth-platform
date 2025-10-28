# Auth Platform ドキュメント

Auth Platformの包括的なドキュメントへようこそ。このディレクトリには、セットアップ、開発、デプロイメント、運用に関する全ての情報が含まれています。

## 📚 ドキュメント一覧

### はじめに

- **[Getting Started](./GETTING_STARTED.md)** - 初回セットアップガイド
  - システム要件
  - インストール手順
  - クイックスタート
  - 基本的な使い方

### 開発

- **[Development Guide](./DEVELOPMENT.md)** - 開発ガイド
  - 開発環境のセットアップ
  - プロジェクト構造
  - コーディング規約
  - 開発ワークフロー
  - デバッグ方法

- **[Testing Guide](./TESTING.md)** - テストガイド
  - テスト戦略
  - 単体テスト
  - 統合テスト
  - E2Eテスト
  - パフォーマンステスト
  - セキュリティテスト

### API

- **[API Integration Guide](./API_INTEGRATION_GUIDE.md)** - API統合ガイド
  - APIの基本的な使い方
  - 認証方法
  - エンドポイント詳細
  - レスポンス形式

- **[Authorization Guide](./AUTHORIZATION_GUIDE.md)** - 認可機能利用ガイド
  - 認可の基本概念
  - デモモード vs フル統合モード
  - クライアント側統合パターン
  - RBAC設定ガイド
  - ベストプラクティス

- **[Postman Guide](./postman-guide.md)** - Postman使用ガイド
  - コレクションのインポート
  - 環境変数の設定
  - リクエスト例

### 運用

- **[Deployment Guide](./DEPLOYMENT.md)** - デプロイメントガイド
  - デプロイメントオプション
  - Docker/Kubernetes設定
  - 環境変数設定
  - セキュリティ設定
  - モニタリング
  - バックアップ

- **[Troubleshooting Guide](./TROUBLESHOOTING.md)** - トラブルシューティング
  - よくある問題と解決方法
  - デバッグツール
  - FAQ

### インフラストラクチャ

- **[Frontend-Backend Integration](./FRONTEND_BACKEND_INTEGRATION.md)** - フロントエンド・バックエンド統合
  - アーキテクチャ概要
  - データフロー
  - 統合方法

- **[SonarQube Setup](./SONARQUBE_SETUP.md)** - SonarQubeセットアップ
  - インストール
  - 設定方法
  - コード品質分析

## 🎯 クイックリンク

### 初めての方

1. [Getting Started](./GETTING_STARTED.md) - まずはここから
2. [Authorization Guide](./AUTHORIZATION_GUIDE.md) - 認可機能の使い方
3. [API Integration Guide](./API_INTEGRATION_GUIDE.md) - APIの使い方
4. [Postman Collection](./auth-platform-api.postman_collection.json) - APIテスト

### 開発者向け

1. [Development Guide](./DEVELOPMENT.md) - 開発環境セットアップ
2. [Testing Guide](./TESTING.md) - テストの書き方
3. [Frontend E2E Testing Strategy](../frontend/e2e/TESTING_STRATEGY.md) - E2Eテスト詳細

### 運用担当者向け

1. [Deployment Guide](./DEPLOYMENT.md) - デプロイ手順
2. [Troubleshooting Guide](./TROUBLESHOOTING.md) - 問題解決
3. [Infrastructure Setup](../infrastructure/README.md) - インフラ設定

## 📖 ドキュメントの構成

```
docs/
├── README.md                           # このファイル (ドキュメントインデックス)
├── GETTING_STARTED.md                  # 初回セットアップガイド
├── DEVELOPMENT.md                      # 開発ガイド
├── TESTING.md                          # テストガイド
├── DEPLOYMENT.md                       # デプロイメントガイド
├── TROUBLESHOOTING.md                  # トラブルシューティング
├── API_INTEGRATION_GUIDE.md            # API統合ガイド
├── AUTHORIZATION_GUIDE.md              # 認可機能利用ガイド
├── FRONTEND_BACKEND_INTEGRATION.md     # フロントエンド・バックエンド統合
├── SONARQUBE_SETUP.md                  # SonarQubeセットアップ
├── postman-guide.md                    # Postman使用ガイド
└── auth-platform-api.postman_collection.json  # Postmanコレクション
```

## 🔍 トピック別ガイド

### 環境セットアップ

| タスク | ドキュメント | セクション |
|-------|------------|-----------|
| 初めてのセットアップ | [Getting Started](./GETTING_STARTED.md) | クイックスタート |
| 開発環境構築 | [Development Guide](./DEVELOPMENT.md) | 開発環境のセットアップ |
| IDE設定 | [Development Guide](./DEVELOPMENT.md#ide) | 推奨設定 |
| Docker環境 | [Getting Started](./GETTING_STARTED.md) | インフラストラクチャの起動 |

### テスト

| タスク | ドキュメント | セクション |
|-------|------------|-----------|
| 単体テストの実行 | [Testing Guide](./TESTING.md) | Backend テスト / Frontend テスト |
| E2Eテストの実行 | [Testing Guide](./TESTING.md) | E2E テスト |
| E2Eテスト戦略 | [E2E Testing Strategy](../frontend/e2e/TESTING_STRATEGY.md) | 全体 |
| パフォーマンステスト | [Testing Guide](./TESTING.md) | パフォーマンステスト |
| カバレッジ確認 | [Testing Guide](./TESTING.md) | テストカバレッジ |

### API使用

| タスク | ドキュメント | セクション |
|-------|------------|-----------|
| API認証 | [API Integration Guide](./API_INTEGRATION_GUIDE.md) | 認証 |
| 認可機能の実装 | [Authorization Guide](./AUTHORIZATION_GUIDE.md) | クライアント側統合 |
| RBAC設定 | [Authorization Guide](./AUTHORIZATION_GUIDE.md) | RBAC設定ガイド |
| デモモード設定 | [Authorization Guide](./AUTHORIZATION_GUIDE.md) | デモモード vs フル統合モード |
| エンドポイント一覧 | [API Integration Guide](./API_INTEGRATION_GUIDE.md) | APIエンドポイント |
| Postman使用 | [Postman Guide](./postman-guide.md) | 全体 |
| OpenAPI仕様 | [specifications/openapi.yaml](../specifications/openapi.yaml) | 全体 |

### デプロイメント

| タスク | ドキュメント | セクション |
|-------|------------|-----------|
| Docker デプロイ | [Deployment Guide](./DEPLOYMENT.md) | Docker デプロイ |
| Kubernetes デプロイ | [Deployment Guide](./DEPLOYMENT.md) | Kubernetes デプロイ |
| 環境変数設定 | [Deployment Guide](./DEPLOYMENT.md) | 環境変数設定 |
| SSL/TLS設定 | [Deployment Guide](./DEPLOYMENT.md) | セキュリティ設定 |
| モニタリング設定 | [Deployment Guide](./DEPLOYMENT.md) | モニタリング |

### トラブルシューティング

| 問題 | ドキュメント | セクション |
|------|------------|-----------|
| 起動しない | [Troubleshooting Guide](./TROUBLESHOOTING.md) | 一般的な問題 |
| 接続エラー | [Troubleshooting Guide](./TROUBLESHOOTING.md) | 接続エラー |
| 認可エラー | [Authorization Guide](./AUTHORIZATION_GUIDE.md) | トラブルシューティング |
| User not found | [Authorization Guide](./AUTHORIZATION_GUIDE.md) | トラブルシューティング |
| パフォーマンス問題 | [Troubleshooting Guide](./TROUBLESHOOTING.md) | パフォーマンスの問題 |
| データベース問題 | [Troubleshooting Guide](./TROUBLESHOOTING.md) | Database の問題 |
| Docker問題 | [Troubleshooting Guide](./TROUBLESHOOTING.md) | Docker の問題 |

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
- **Styling**: Tailwind CSS
- **Package Manager**: pnpm 9

### Testing
- **Unit**: JUnit 5, Jest
- **Integration**: Spring Boot Test, React Testing Library
- **E2E**: Playwright
- **Performance**: Gatling
- **Security**: OWASP ZAP, Trivy

### Infrastructure
- **Containerization**: Docker, Docker Compose
- **Orchestration**: Kubernetes
- **CI/CD**: GitHub Actions
- **Monitoring**: Prometheus, Grafana
- **Code Quality**: SonarQube

## 📝 ドキュメントの更新

ドキュメントを更新する場合：

1. 適切なマークダウンファイルを編集
2. 構造と形式を維持
3. コード例は実際に動作することを確認
4. リンクが正しいことを確認
5. PRを作成してレビューを受ける

## 🤝 コントリビューション

ドキュメントの改善案や誤りを見つけた場合：

1. [GitHub Issues](https://github.com/bl-hori/auth-platform/issues)で報告
2. Pull Requestを作成
3. ドキュメントの改善提案を歓迎します

## 📄 ライセンス

このプロジェクトはMITライセンスの下で公開されています。

## 🆘 サポート

質問や問題がある場合：

1. このドキュメントを検索
2. [Troubleshooting Guide](./TROUBLESHOOTING.md)を確認
3. [GitHub Issues](https://github.com/bl-hori/auth-platform/issues)で検索
4. 新しいIssueを作成

---

**最終更新**: 2025-10-27
**バージョン**: 1.0.0
**メンテナー**: Auth Platform Team
