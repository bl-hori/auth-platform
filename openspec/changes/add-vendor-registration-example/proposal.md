# Proposal: Add Vendor Registration Example Application

## Why
現在、Auth Platformのバックエンド認可機構の使い方を示す実践的なサンプルアプリケーションが存在しません。開発者が以下の課題に直面しています：

1. **実装例の不足**: 認可APIの統合方法を示す具体的なコード例がない
2. **ユースケースの不明確さ**: RBAC/ABACをどのように実際のビジネスロジックに適用するか分からない
3. **ベストプラクティスの欠如**: マルチテナント対応、エラーハンドリング、キャッシング戦略などの実装パターンが不明

これにより、Auth Platformの導入障壁が高くなっており、開発者がAPIの実際の活用方法を理解するまでに時間がかかっています。

## What Changes
Next.js 15を使用した「取引先登録申請アプリケーション」のサンプルを`examples/vendor-registration/`配下に作成します。このアプリケーションは以下を実現します：

### Core Features
1. **取引先登録申請ワークフロー**
   - 申請者: 取引先情報を入力して申請を作成
   - 承認者: 申請内容を確認して承認/却下
   - 管理者: 全ての申請を管理

2. **認可機構の実装例**
   - RBAC: ロールベースのアクセス制御（申請者、承認者、管理者）
   - リソースレベルの権限チェック（自分の申請のみ編集可能など）
   - バッチ認可による効率的な権限チェック

3. **Auth Platform統合のベストプラクティス**
   - APIキー認証の実装
   - 認可チェックのミドルウェア化
   - エラーハンドリングとフォールバック
   - パフォーマンス最適化（キャッシング）

### Technical Scope
- **Frontend**: Next.js 15 (App Router) + TypeScript + Tailwind CSS
- **Authentication**: 簡易的なログインフォーム（実装のデモ目的）
- **Authorization**: Auth Platform Backend APIとの統合
- **Data Storage**: ローカルストレージまたはモックAPI（フォーカスは認可機構）

## Success Criteria
- [ ] 取引先登録申請の作成・編集・削除が権限に応じて制御される
- [ ] 承認者ロールが申請の承認/却下を実行できる
- [ ] 管理者ロールが全ての申請を管理できる
- [ ] Auth Platform認可APIとの統合が正しく動作する
- [ ] README.mdにセットアップ手順と認可実装の解説が含まれる
- [ ] コードコメントで認可チェックのポイントが説明されている

## Impact Assessment
- **Breaking Changes**: なし（新規追加のみ）
- **Dependencies**: 既存のAuth Platform Backend APIに依存
- **Performance**: サンプルアプリのため本番環境への影響なし
- **Documentation**: examples/vendor-registration/README.mdを追加

## Timeline
- **実装**: 6-8時間
- **テスト**: 2-3時間
- **ドキュメント作成**: 2-3時間
- **Total**: 10-14時間
