# 認可基盤プラットフォーム仕様書プロジェクト

## プロジェクト概要
Permit.ioのような認可基盤プラットフォームを構築するための仕様書群です。

### 目的
- 企業向けの統合認可基盤の構築
- RBAC、ABAC、ReBACをサポートする柔軟な認可システム
- ゼロレイテンシーでスケーラブルなアーキテクチャの実現
- ノーコード/ローコードでのポリシー管理

### 技術スタック
- **フロントエンド**: Next.js 15 (App Router)
- **バックエンド**: Spring Boot 3.x
- **ポリシーエンジン**: Open Policy Agent (OPA) / Cedar
- **データベース**: PostgreSQL
- **キャッシュ**: Redis
- **メッセージング**: Apache Kafka
- **認証**: Keycloak

### アーキテクチャ概要
- Control Plane（管理）とData Plane（実行）の分離
- マイクロサービスアーキテクチャ
- イベント駆動型の動的ポリシー更新
- GitOps対応のPolicy-as-Code

### 主要機能
1. **認可モデルサポート**
   - RBAC (Role-Based Access Control)
   - ABAC (Attribute-Based Access Control)
   - ReBAC (Relationship-Based Access Control)

2. **管理機能**
   - ノーコードポリシーエディタ
   - API/SDK提供
   - GitOps統合
   - 監査ログ

3. **性能要件**
   - 認可決定: <10ms (p95)
   - 高可用性: 99.99%
   - 水平スケーラビリティ

## ディレクトリ構成
```
auth-platform/
├── openspec/                    # OpenSpec仕様管理（NEW）
│   ├── project.md              # プロジェクトコンテキスト
│   ├── AGENTS.md               # AI開発ガイド
│   ├── specs/                  # 現在の仕様（実装済み機能）
│   │   ├── policy-management/
│   │   ├── authorization-core/
│   │   ├── user-identity/
│   │   └── audit-logging/
│   └── changes/                # 変更提案（実装予定機能）
│       └── add-phase1-mvp/     # Phase 1 MVP実装提案
│           ├── proposal.md     # 提案概要
│           ├── tasks.md        # 実装タスク（170+項目）
│           ├── design.md       # 設計判断
│           └── specs/          # 仕様デルタ（差分）
├── specifications/             # レガシー仕様書（参照用）
│   ├── requirement_spec.md     # 要求仕様書
│   ├── architecture_design.md  # アーキテクチャ設計書
│   ├── data_model.md           # データモデル設計書
│   ├── api_spec.md             # API仕様書
│   ├── test_spec.md            # テスト仕様書
│   └── use_cases.md            # ユースケース一覧
├── progress.md                 # 進捗管理
├── todo.md                     # TODOリスト
└── README.md                   # プロジェクト概要（本ファイル）
```

## プロジェクトステータス

### 現在のフェーズ
**Phase 1 MVP - 提案段階** (2025-10-25)

### OpenSpec導入済み ✅
- [x] プロジェクトコンテキスト定義完了
- [x] 4つのコア仕様作成完了
  - Policy Management (ポリシー管理)
  - Authorization Core (認可コア)
  - User Identity (ユーザー管理)
  - Audit Logging (監査ログ)
- [x] Phase 1 MVP実装提案作成
  - 12週間の実装計画
  - 170+個の詳細タスク
  - 10個の主要設計判断

### レガシー仕様書ステータス
- [x] 要求仕様書 (specifications/requirement_spec.md)
- [x] システムアーキテクチャ設計書 (specifications/architecture_design.md)
- [x] API仕様書 (specifications/api_spec.md)
- [x] データモデル設計書 (specifications/data_model.md)
- [x] 実装仕様書 (テスト仕様含む)
- [x] テスト仕様書 (specifications/test_spec.md)

**注**: レガシー仕様書はOpenSpecに移行済み。参照用として保持。

### 次のステップ
1. Phase 1 MVP提案のレビューと承認
2. 開発環境のセットアップ（Week 1）
3. バックエンド基盤の実装開始（Week 1-2）

## OpenSpec使用方法

### 仕様確認
```bash
# 全仕様一覧
openspec list --specs

# 特定仕様の詳細表示
openspec show policy-management --type spec

# 変更提案一覧
openspec list

# Phase 1 MVP提案の詳細
openspec show add-phase1-mvp
```

### 実装開始時
```bash
# Phase 1 MVP実装開始
cd openspec/changes/add-phase1-mvp
cat tasks.md  # 実装タスク確認

# 進捗追跡（tasks.mdのチェックボックスを更新）
# タスク完了後、OpenSpecに反映
openspec archive add-phase1-mvp  # 実装完了後
```

## 参考資料
- [Permit.io 公式サイト](https://www.permit.io)
- [Open Policy Agent](https://www.openpolicyagent.org)
- [Google Zanzibar Paper](https://research.google/pubs/pub48190/)
