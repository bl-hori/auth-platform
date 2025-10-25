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
auth-platform-specs/
├── specifications/       # 仕様書格納
│   ├── requirement/     # 要求仕様書
│   ├── architecture/    # アーキテクチャ設計書
│   ├── implementation/  # 実装仕様書
│   ├── api/            # API仕様書
│   └── test/           # テスト仕様書
├── review/             # レビュー文書
├── templates/          # テンプレート
├── progress.md         # 進捗管理
├── todo.md            # TODOリスト
└── README.md          # プロジェクト概要（本ファイル）
```

## 仕様書作成ステータス
- [ ] 要求仕様書
- [ ] システムアーキテクチャ設計書
- [ ] API仕様書
- [ ] データモデル設計書
- [ ] 実装仕様書
- [ ] テスト仕様書

## 参考資料
- [Permit.io 公式サイト](https://www.permit.io)
- [Open Policy Agent](https://www.openpolicyagent.org)
- [Google Zanzibar Paper](https://research.google/pubs/pub48190/)
