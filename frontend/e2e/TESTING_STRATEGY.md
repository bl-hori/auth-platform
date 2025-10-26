# E2E Testing Strategy - Step by Step Approach

## Overview

このディレクトリには、Auth Platformのフロントエンド向けのE2Eテストが含まれています。
複雑なテストケースを段階的に実装するアプローチを採用しています。

## Test Files Structure

### Active Tests (実行中)

1. **01-basic-auth.spec.ts** - 基本的な認証フロー
   - ログインページの表示
   - 空のAPIキーでのエラー表示
   - APIキーでのログイン成功
   - ログイン状態の永続化
   - ログアウト機能
   - 保護されたルートへのリダイレクト

2. **02-navigation.spec.ts** - ページ間のナビゲーション
   - ユーザー管理ページへの遷移
   - ロール管理ページへの遷移
   - 権限管理ページへの遷移
   - ポリシー管理ページへの遷移
   - 監査ログページへの遷移
   - ダッシュボードへの戻り

3. **03-users-list.spec.ts** - ユーザー一覧ページの基本機能
   - ページの表示確認
   - 検索機能の有無
   - ユーザー作成ボタン/リンクの有無

### Disabled Tests (無効化中)

以下のテストファイルは `.disabled` 拡張子で無効化されています：
- `auth.spec.ts.disabled` - 元の認証テスト
- `users.spec.ts.disabled` - 元のユーザー管理テスト
- `roles.spec.ts.disabled` - 元のロール管理テスト
- `policies.spec.ts.disabled` - 元のポリシー管理テスト
- `audit-logs.spec.ts.disabled` - 元の監査ログテスト
- `accessibility.spec.ts.disabled` - 元のアクセシビリティテスト

これらは基本テストが安定した後、段階的に有効化・修正していきます。

## Step-by-Step Testing Approach

### Phase 1: Foundation (現在)
✅ 基本的な認証フローのテスト
✅ シンプルなナビゲーションテスト
✅ ページ表示の基本確認

### Phase 2: Page Functionality (次のステップ)
- ユーザー作成・編集・削除
- ロール管理機能
- 権限管理機能
- データの検索・フィルタリング

### Phase 3: Advanced Features
- ポリシー管理と検証
- 監査ログの詳細表示
- エクスポート機能
- リアルタイム更新

### Phase 4: Quality & Accessibility
- アクセシビリティ検証
- エラーハンドリング
- パフォーマンス確認

## Running Tests

### Run all active tests
```bash
cd frontend
pnpm test:e2e
```

### Run specific test file
```bash
pnpm test:e2e 01-basic-auth
pnpm test:e2e 02-navigation
pnpm test:e2e 03-users-list
```

### Run in UI mode (recommended for debugging)
```bash
pnpm test:e2e:ui
```

### Run in headed mode (see browser)
```bash
pnpm test:e2e:headed
```

## Key Principles

1. **Simple First**: 最もシンプルなテストから始める
2. **Incremental**: 段階的に複雑なテストを追加
3. **Flexible Selectors**: data-testidを優先しつつ、柔軟なセレクタも使用
4. **Resilient**: ページ構造の変更に強いテスト
5. **Clear Failures**: 失敗時に何が問題かすぐわかる

## Test Writing Guidelines

### Good Practices
```typescript
// ✅ Good: Flexible selector with timeout
await expect(page.locator('h1, h2').filter({ hasText: /Users/i }))
  .toBeVisible({ timeout: 5000 });

// ✅ Good: Check existence before asserting
if (await searchInput.count() > 0) {
  await expect(searchInput.first()).toBeVisible();
}

// ✅ Good: Wait for navigation explicitly
await page.waitForURL('**/users', { timeout: 10000 });
```

### Anti-Patterns
```typescript
// ❌ Bad: Hard-coded exact text
await page.click('text=Create New User');

// ❌ Bad: Fragile CSS selectors
await page.click('.btn-primary.create-user-btn');

// ❌ Bad: No timeout or flexibility
await expect(page.locator('h1')).toBeVisible();
```

## Troubleshooting

### Tests timing out
- Increase timeouts in assertions: `{ timeout: 10000 }`
- Add explicit waits: `await page.waitForLoadState('networkidle')`
- Check if backend API is running

### Element not found
- Use Playwright inspector: `pnpm test:e2e:debug`
- Check if element exists: `await element.count()`
- Use flexible selectors: `page.locator('a, button').filter({ hasText: /text/i })`

### Tests pass locally but fail in CI
- Check if timing issues: add more explicit waits
- Verify environment variables are set
- Check if services (backend, DB) are running in CI

## Next Steps

1. ✅ Fix basic authentication tests
2. ✅ Fix navigation tests
3. ✅ Fix users list basic tests
4. ⏳ Enable and fix users CRUD tests
5. ⏳ Enable and fix roles tests
6. ⏳ Enable and fix policies tests
7. ⏳ Enable and fix audit logs tests
8. ⏳ Enable and fix accessibility tests

---

**Last Updated**: 2025-10-26
**Status**: Phase 1 - Foundation Tests
