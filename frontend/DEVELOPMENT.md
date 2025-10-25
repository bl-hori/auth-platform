# フロントエンド開発ガイド

Auth Platform フロントエンドの開発環境セットアップと開発ワークフローのガイドです。

## 目次

- [必要要件](#必要要件)
- [環境構築](#環境構築)
- [開発サーバー](#開発サーバー)
- [コード品質](#コード品質)
- [VSCode設定](#vscode設定)
- [デバッグ](#デバッグ)
- [トラブルシューティング](#トラブルシューティング)

## 必要要件

- **Node.js**: 18.17以上
- **pnpm**: 10.17以上
- **VSCode**: 推奨（拡張機能自動インストール対応）

## 環境構築

### 1. 依存関係のインストール

```bash
cd frontend
pnpm install
```

### 2. 環境変数の設定

```bash
cp .env.local.example .env.local
```

`.env.local`を編集して、バックエンドAPIの設定を行います：

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_API_KEY=your-dev-api-key
```

## 開発サーバー

### 標準モード（Fast Refresh有効）

```bash
pnpm dev
```

デフォルトで`http://localhost:3000`で起動します。

**Fast Refreshの機能**:
- ファイル保存時に自動リロード
- Reactコンポーネントの状態を保持したままリロード
- エラー時の詳細なオーバーレイ表示

### Turbopackモード（実験的）

Next.js 15の高速ビルドツールを使用：

```bash
pnpm dev:turbo
```

**メリット**:
- 初回起動が高速
- ホットリロードがさらに高速
- 大規模プロジェクトでのパフォーマンス向上

### デバッグモード

Chrome DevToolsでNode.jsプロセスをデバッグ：

```bash
pnpm dev:debug
```

1. 開発サーバー起動後、Chrome で `chrome://inspect` を開く
2. "Open dedicated DevTools for Node" をクリック
3. ブレークポイントを設定してデバッグ

### カスタムポート

```bash
PORT=3001 pnpm dev
```

## コード品質

### TypeScript型チェック

```bash
pnpm type-check
```

すべての型エラーを検出します。

### ESLint

```bash
# リンティング実行
pnpm lint

# 自動修正
pnpm lint:fix
```

Next.js推奨ルール + TypeScriptルールを適用。

### Prettier（コードフォーマット）

```bash
# フォーマット実行
pnpm format

# フォーマットチェック（CI用）
pnpm format:check
```

**設定** (`.prettierrc.json`):
- セミコロンなし
- シングルクォート
- Tailwind CSSクラス自動ソート

### プリコミットチェック

コミット前に以下を実行することを推奨：

```bash
pnpm type-check && pnpm lint && pnpm format:check
```

## ビルド

### 本番ビルド

```bash
pnpm build
```

最適化されたビルドを`.next`ディレクトリに生成。

### バンドルサイズ分析

```bash
pnpm build:analyze
```

バンドルサイズを分析し、最適化のヒントを表示。

### 本番サーバー起動

```bash
pnpm start
```

ビルド済みアプリケーションを起動（`http://localhost:3000`）。

## VSCode設定

### 推奨拡張機能

プロジェクトを開くと自動的に推奨拡張機能がインストールされます：

- **ESLint**: リアルタイムリンティング
- **Prettier**: 保存時自動フォーマット
- **Tailwind CSS IntelliSense**: クラス名補完
- **TypeScript**: 高度な型サポート

### 自動設定

`.vscode/settings.json`により以下が自動設定されます：

- 保存時にPrettier自動実行
- 保存時にESLintエラー自動修正
- Tailwind CSSのIntelliSense有効化
- TypeScriptワークスペース使用

## デバッグ

### ブラウザデバッグ

1. Chrome DevToolsを開く（F12）
2. Sourcesタブでブレークポイント設定
3. コンソールでエラーやログ確認

### React Developer Tools

Chrome拡張機能をインストール：
- [React Developer Tools](https://chrome.google.com/webstore/detail/react-developer-tools/fmkadmapgofadopljbjfkapdkoienihi)

### Next.js DevTools

開発サーバー起動中、以下のURLでNext.js情報を確認：

- `http://localhost:3000/__nextjs_original-stack-frame` - エラースタック
- Network タブでRSC（React Server Components）ペイロード確認

## ホットリロード

### Fast Refresh

Next.js 15のFast Refreshは以下の変更を即座に反映：

- Reactコンポーネント
- CSSファイル
- 環境変数（`.env.local`）

**注意事項**:
- エクスポートされていないコンポーネントの状態は保持されます
- クラスコンポーネントは完全リロード
- グローバル変数の変更は完全リロード

### リロードされない場合

1. `.next`ディレクトリを削除：
   ```bash
   rm -rf .next
   pnpm dev
   ```

2. `node_modules`を再インストール：
   ```bash
   rm -rf node_modules pnpm-lock.yaml
   pnpm install
   ```

## トラブルシューティング

### ポートが使用中

```bash
# ポート3000を使用しているプロセスを確認
lsof -i :3000

# プロセスを終了
kill -9 <PID>

# または別のポートを使用
PORT=3001 pnpm dev
```

### TypeScriptエラー

```bash
# TypeScriptサーバーを再起動（VSCode）
Cmd/Ctrl + Shift + P -> "TypeScript: Restart TS Server"

# tsconfigを再生成
rm tsconfig.tsbuildinfo
pnpm type-check
```

### ESLintエラー

```bash
# ESLintキャッシュをクリア
rm -rf .next/cache/eslint
pnpm lint
```

### Tailwind CSSが効かない

1. PostCSSが正しく設定されているか確認：
   ```bash
   cat postcss.config.mjs
   ```

2. Tailwindディレクティブが含まれているか確認：
   ```bash
   cat src/app/globals.css | head -3
   ```

3. 開発サーバーを再起動：
   ```bash
   rm -rf .next
   pnpm dev
   ```

### 依存関係の問題

```bash
# pnpmキャッシュをクリア
pnpm store prune

# 依存関係を再インストール
rm -rf node_modules pnpm-lock.yaml
pnpm install
```

## パフォーマンス最適化

### 開発時のヒント

1. **Turbopack使用**: `pnpm dev:turbo`で高速化
2. **不要なインポート削除**: バンドルサイズ削減
3. **動的インポート**: 大きなコンポーネントは遅延読み込み
4. **メモ化**: `useMemo`、`useCallback`の適切な使用

### 本番最適化

- 画像最適化: Next.js Image コンポーネント使用
- フォント最適化: `next/font`使用
- コード分割: 動的インポート活用
- キャッシング: `stale-while-revalidate`戦略

## 参考リンク

- [Next.js Documentation](https://nextjs.org/docs)
- [React Documentation](https://react.dev)
- [Tailwind CSS Documentation](https://tailwindcss.com/docs)
- [TypeScript Documentation](https://www.typescriptlang.org/docs)
- [shadcn/ui Documentation](https://ui.shadcn.com)
