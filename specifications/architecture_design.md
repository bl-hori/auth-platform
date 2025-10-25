# システムアーキテクチャ設計書 - 認可基盤プラットフォーム

## 1. 概要

### 1.1 目的
本設計書は、認可基盤プラットフォームのシステムアーキテクチャを定義し、技術的な実装方針と設計判断を文書化することを目的とする。

### 1.2 スコープ
- システム全体のアーキテクチャ構成
- 技術スタックの選定と根拠
- コンポーネント間の連携方式
- データアーキテクチャと永続化戦略
- セキュリティアーキテクチャ
- 非機能要件の実現方式

### 1.3 関連文書
- 要求仕様書（requirement_spec.md）
- ユースケース一覧（use_cases.md）
- 要求トレーサビリティマトリクス（requirement_matrix.md）

## 2. アーキテクチャ概要

### 2.1 アーキテクチャパターン
- **採用パターン**: マイクロサービスアーキテクチャ + イベント駆動型
- **選定理由**:
  - 高可用性とスケーラビリティの要求に対応
  - Control PlaneとData Planeの明確な分離が可能
  - 各サービスの独立したデプロイとスケーリングが可能
  - ポリシー更新のリアルタイム反映にイベント駆動が適合

### 2.2 システム構成図

```
┌──────────────────────────────────────────────────────────────────┐
│                         External Systems                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │   IdP    │  │   Git    │  │   SIEM   │  │  Monitoring  │   │
│  │(Keycloak)│  │Repository│  │  System  │  │   System     │   │
│  └─────┬────┘  └─────┬────┘  └─────┬────┘  └──────┬───────┘   │
└────────┼─────────────┼─────────────┼──────────────┼────────────┘
         │             │             │              │
     ┌───▼─────────────▼─────────────▼──────────────▼───┐
     │              API Gateway (Kong/Spring Gateway)    │
     │                    [Load Balancer]                 │
     └────┬────────┬───────┬───────┬──────┬──────┬──────┘
          │        │       │       │      │      │
     ┌────▼────┐ ┌▼───────▼───┐ ┌▼──────▼──┐ ┌▼────────┐
     │Control  │ │Authorization│ │Identity  │ │Analytics│
     │ Plane   │ │   Service   │ │ Service  │ │Service  │
     │         │ │             │ │          │ │         │
     │┌───────┐│ │┌───────────┐│ │┌────────┐│ │┌──────┐│
     ││Policy ││ ││    PDP    ││ ││  SCIM  ││ ││Metrics││
     ││Manager││ ││  Manager  ││ ││Handler ││ ││Collect││
     │└───────┘│ │└───────────┘│ │└────────┘│ │└──────┘│
     │┌───────┐│ │┌───────────┐│ │┌────────┐│ │┌──────┐│
     ││ OPAL  ││ ││   Audit   ││ ││  User  ││ ││Report ││
     ││Server ││ ││  Logger   ││ ││  Sync  ││ ││ Gen   ││
     │└───────┘│ │└───────────┘│ │└────────┘│ │└──────┘│
     └─────────┘ └─────────────┘ └──────────┘ └─────────┘
          │              │             │            │
     ┌────▼──────────────▼─────────────▼────────────▼───┐
     │            Message Bus (Apache Kafka)             │
     └────┬──────────────┬─────────────┬────────────┬───┘
          │              │             │            │
     ┌────▼────┐    ┌───▼────┐   ┌───▼────┐  ┌───▼────┐
     │Data Plane│    │ Cache  │   │Database│  │ Object │
     │   PDP   │    │(Redis) │   │(Postgres)│ │Storage │
     │ Cluster │    │Cluster │   │ Cluster │  │  (S3)  │
     └─────────┘    └────────┘   └─────────┘  └────────┘
```

### 2.3 主要コンポーネント

| コンポーネント | 役割 | 責務 |
|----------------|------|------|
| API Gateway | エントリーポイント | ルーティング、認証、レート制限、負荷分散 |
| Policy Manager | ポリシー管理 | ポリシーのCRUD、バージョン管理、検証 |
| Authorization Service | 認可処理 | 認可判定、決定理由生成、キャッシング |
| PDP (Policy Decision Point) | ポリシー評価 | OPA/Cedarエンジンでのポリシー実行 |
| Identity Service | アイデンティティ管理 | ユーザー/グループ管理、SCIM連携 |
| OPAL Server | ポリシー配信 | リアルタイムポリシー更新、PDP同期 |
| Audit Service | 監査ログ | 認可決定の記録、検索、レポート生成 |
| Analytics Service | 分析 | メトリクス収集、ダッシュボード、トレンド分析 |
| Message Bus | イベント処理 | 非同期通信、イベント配信、ストリーミング |
| Cache Cluster | キャッシュ | 高速データアクセス、セッション管理 |

## 3. 技術スタック

### 3.1 プログラミング言語

| 用途 | 言語 | バージョン | 選定理由 |
|------|------|------------|----------|
| バックエンド | Java | 21 (LTS) | Spring Boot生態系の充実、エンタープライズ実績 |
| フロントエンド | TypeScript | 5.3+ | 型安全性、Next.js 15との親和性 |
| ポリシー記述 | Rego/Cedar | 最新 | OPA/Cedar標準言語 |
| スクリプト | Python | 3.11+ | 運用自動化、データ処理 |

### 3.2 フレームワーク・ライブラリ

| 名称 | 用途 | バージョン | 選定理由 |
|------|------|------------|----------|
| Spring Boot | バックエンドフレームワーク | 3.2+ | マイクロサービス対応、豊富な機能 |
| Spring Cloud | マイクロサービス基盤 | 2023.0+ | サービスディスカバリ、設定管理 |
| Next.js | フロントエンドフレームワーク | 15+ | App Router、RSC対応 |
| React | UIライブラリ | 18+ | コンポーネントベース開発 |
| Tailwind CSS | スタイリング | 3.4+ | ユーティリティファースト |
| OPA | ポリシーエンジン | 0.60+ | 業界標準、高性能 |
| Cedar | ポリシーエンジン（代替） | 3.0+ | AWS開発、形式検証可能 |

### 3.3 ミドルウェア

| 種別 | 製品名 | バージョン | 用途 |
|------|--------|------------|------|
| API Gateway | Spring Cloud Gateway | 4.1+ | ルーティング、認証統合 |
| Message Broker | Apache Kafka | 3.6+ | イベントストリーミング |
| Cache | Redis | 7.2+ | 分散キャッシュ、セッション |
| Service Mesh | Istio | 1.20+ | サービス間通信制御（オプション） |
| Container Runtime | Docker | 24+ | コンテナ化 |
| Orchestration | Kubernetes | 1.28+ | コンテナオーケストレーション |

### 3.4 データベース

| 種別 | 製品名 | バージョン | 用途 |
|------|--------|------------|------|
| RDBMS | PostgreSQL | 15+ | ポリシーデータ、ユーザーデータ |
| Time Series DB | TimescaleDB | 2.13+ | 監査ログ、メトリクス |
| Graph DB | Neo4j | 5.15+ | ReBAC関係性データ（オプション） |
| Object Storage | MinIO/S3 | 最新 | バックアップ、大容量ログ |

### 3.5 インフラ基盤
- **環境**: ハイブリッドクラウド対応（クラウド優先）
- **クラウドプロバイダー**: AWS / Azure / GCP（マルチクラウド対応）
- **リージョン**: 東京リージョン（Primary）、大阪リージョン（DR）
- **コンテナレジストリ**: Amazon ECR / Docker Hub
- **CI/CD**: GitHub Actions + ArgoCD

## 4. データアーキテクチャ

### 4.1 データモデル概要
データは機能ごとに分離され、各サービスが独自のデータストアを持つ（Database per Service パターン）。
共有が必要なデータはイベント駆動で同期される。

### 4.2 主要エンティティ

| エンティティ名 | 説明 | 主要属性 |
|----------------|------|----------|
| Organization | テナント組織 | id, name, domain, settings, created_at |
| User | ユーザー | id, email, name, attributes, organization_id |
| Role | ロール | id, name, permissions, organization_id |
| Policy | ポリシー | id, name, type(RBAC/ABAC/ReBAC), rules, version |
| Resource | リソース | id, type, name, attributes, owner_id |
| Permission | 権限 | id, action, resource_type, conditions |
| AuditLog | 監査ログ | id, timestamp, user_id, action, resource, decision, reason |
| Relationship | 関係性（ReBAC用） | id, subject_id, relation, object_id |

### 4.3 データフロー

```
[User Request] → [API Gateway] → [Auth Service]
                                       ↓
                              [Check Cache (Redis)]
                                   ↓ (miss)
                              [Query Policy DB]
                                       ↓
                              [Evaluate with PDP]
                                       ↓
                              [Update Cache]
                                       ↓
                              [Log to Audit DB]
                                       ↓
                              [Return Decision]
```

### 4.4 データ永続化戦略
- **ポリシーデータ**: PostgreSQL（マスター） + Redis（キャッシュ）
- **監査ログ**: TimescaleDB（時系列最適化）+ S3（長期保存）
- **セッション**: Redis（揮発性）
- **バックアップ**: 日次フルバックアップ + 継続的な差分バックアップ

## 5. インターフェース設計

### 5.1 外部インターフェース

| IF-ID | 接続先 | プロトコル | データ形式 | 説明 |
|-------|--------|------------|------------|------|
| IF001 | IdP (Keycloak) | HTTPS/OIDC | JSON | 認証連携、トークン検証 |
| IF002 | IdP (SCIM) | HTTPS/SCIM 2.0 | JSON | ユーザー/グループ同期 |
| IF003 | Git Repository | HTTPS/Git | YAML/Rego | ポリシーコード同期 |
| IF004 | SIEM | Syslog/HTTPS | JSON/CEF | セキュリティイベント転送 |
| IF005 | Monitoring | HTTPS/Prometheus | Metrics | メトリクスエクスポート |
| IF006 | Client Apps | HTTPS/REST | JSON | 認可API |
| IF007 | Client Apps | WebSocket | JSON | リアルタイム更新通知 |

### 5.2 内部インターフェース

| IF-ID | From | To | プロトコル | データ形式 |
|-------|------|-----|------------|------------|
| IF101 | API Gateway | Auth Service | HTTP/gRPC | JSON/Protobuf |
| IF102 | Auth Service | PDP | gRPC | Protobuf |
| IF103 | Policy Manager | OPAL Server | HTTP | JSON |
| IF104 | OPAL Server | PDP | WebSocket | JSON |
| IF105 | All Services | Message Bus | Kafka Protocol | Avro/JSON |
| IF106 | Services | Cache | Redis Protocol | Binary |
| IF107 | Services | Database | PostgreSQL Wire | SQL |

## 6. セキュリティアーキテクチャ

### 6.1 認証・認可
- **認証方式**: 
  - 外部IdP連携（OIDC/SAML）
  - APIキー認証（M2M）
  - mTLS（サービス間通信）
- **認可モデル**: 
  - 自システムでドッグフーディング（自己利用）
  - 管理機能はRBAC
  - API アクセスはAPIキー + レート制限

### 6.2 暗号化
- **通信暗号化**: 
  - 外部通信: TLS 1.3必須
  - 内部通信: mTLS（Istio Service Mesh）
- **データ暗号化**: 
  - 保存時: AES-256-GCM
  - キー管理: AWS KMS / HashiCorp Vault

### 6.3 セキュリティ対策

| 脅威 | 対策 |
|------|------|
| DDoS攻撃 | CloudFlare/AWS Shield、レート制限 |
| SQLインジェクション | プリペアドステートメント、ORMの使用 |
| 認証バイパス | 多層防御、ゼロトラストモデル |
| データ漏洩 | 暗号化、アクセス制御、DLP |
| 内部脅威 | 最小権限原則、監査ログ、異常検知 |
| サプライチェーン攻撃 | 依存関係スキャン、SBOM管理 |

## 7. 非機能要件への対応

### 7.1 パフォーマンス
- **対応方針**: エッジでの分散処理とインメモリキャッシング
- **具体的対策**:
  - ローカルPDP配置による遅延削減
  - Redisによる多層キャッシング
  - 非同期処理とイベント駆動
  - データベースのインデックス最適化
  - コネクションプーリング

### 7.2 可用性
- **対応方針**: 冗長化とフェイルオーバー
- **具体的対策**:
  - マルチリージョン展開
  - データベースレプリケーション
  - サーキットブレーカーパターン
  - ヘルスチェックと自動復旧
  - グレースフルシャットダウン

### 7.3 スケーラビリティ
- **対応方針**: 水平スケーリングとElastic対応
- **具体的対策**:
  - Kubernetes HPA/VPA
  - データベースシャーディング
  - イベント駆動による疎結合
  - ステートレス設計
  - 負荷分散とオートスケーリング

### 7.4 保守性
- **対応方針**: 観測可能性とGitOps
- **具体的対策**:
  - 構造化ログ（JSON形式）
  - 分散トレーシング（OpenTelemetry）
  - メトリクス監視（Prometheus + Grafana）
  - Infrastructure as Code（Terraform）
  - GitOpsによる宣言的デプロイ

## 8. 開発・運用環境

### 8.1 環境構成

| 環境 | 用途 | 構成 |
|------|------|------|
| 開発環境 | 個人開発 | Docker Compose、ローカルK8s (kind/minikube) |
| テスト環境 | 結合テスト | Kubernetes (3ノード)、縮小版構成 |
| ステージング環境 | 本番相当テスト | Kubernetes (5ノード)、本番同等構成 |
| 本番環境 | サービス提供 | Kubernetes (10+ノード)、マルチAZ |

### 8.2 デプロイメント
- **デプロイ方式**: Blue-Green / カナリアデプロイメント
- **CI/CDツール**: 
  - CI: GitHub Actions
  - CD: ArgoCD (GitOps)
  - イメージビルド: Buildkit
  - セキュリティスキャン: Trivy, Snyk

## 9. 移行計画

### 9.1 移行方針
新規構築のため、段階的な機能リリースとなる。既存システムがある場合は、Strangler Figパターンで段階移行。

### 9.2 移行ステップ
1. **Phase 1**: コア機能（RBAC、基本API）のリリース
2. **Phase 2**: 高度な機能（ABAC、ノーコードエディタ）追加
3. **Phase 3**: ReBAC、GitOps統合
4. **Phase 4**: 分析機能、高度な運用機能

## 10. リスクと対策

| リスク | 影響度 | 発生確率 | 対策 |
|--------|--------|----------|------|
| パフォーマンス目標未達 | 高 | 中 | 早期の性能テスト、キャッシュ戦略の最適化 |
| OPA/Cedarの学習コスト | 中 | 高 | 充実したドキュメント、サンプル提供 |
| マイクロサービスの複雑性 | 高 | 中 | 段階的導入、サービスメッシュ活用 |
| データ整合性問題 | 高 | 低 | Sagaパターン、イベントソーシング |
| ベンダーロックイン | 中 | 中 | 抽象化レイヤー、標準技術の採用 |

## 11. 決定事項と検討事項

### 11.1 決定事項
- マイクロサービスアーキテクチャを採用
- Spring Boot + Next.js 15の技術スタック
- PostgreSQLをメインデータストアとして使用
- Kubernetesベースのコンテナ基盤
- OPAをプライマリポリシーエンジンとして採用

### 11.2 検討事項（TBD）
- Service Meshの採用範囲（Istio全面採用 or 部分採用）
- Graph DB (Neo4j) の必要性（ReBACの複雑性次第）
- マルチクラウド対応の優先度
- WebAssemblyベースのポリシー実行の検討
- 長期的なデータアーカイブ戦略

---

**文書情報**
- バージョン: 1.0.0
- 作成日: 2025-10-25
- 作成者: System Architect Agent
- レビュー状態: ドラフト
- 次回レビュー: 未定
