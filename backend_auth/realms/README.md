# Keycloak Realm Configurations

このディレクトリには、Keycloakのrealm設定ファイルを格納します。

## ファイル

- `authplatform-realm.json`: Auth Platform用のデフォルトrealm設定

## Realm設定のエクスポート

Keycloak管理コンソールからrealm設定をエクスポートする手順：

1. http://localhost:8180 にアクセス
2. `admin` / `admin` でログイン
3. 左上のRealm選択で`authplatform`を選択
4. 左メニューから`Realm settings`を選択
5. `Action` > `Partial export`をクリック
6. 以下をチェック:
   - Export groups and roles
   - Export clients
7. `Export`ボタンをクリック
8. ダウンロードしたJSONを`authplatform-realm.json`として保存

## Realm設定のインポート

Docker Composeで自動的にインポートされます（`--import-realm`オプション）。

手動でインポートする場合：

```bash
docker exec -it authplatform-keycloak /opt/keycloak/bin/kc.sh import \
  --file /opt/keycloak/data/import/authplatform-realm.json
```

## 注意事項

- **機密情報**: client secretsやパスワードはエクスポートに含めないでください
- **バージョン管理**: このファイルはGitで管理され、チーム全体で共有されます
- **本番環境**: 本番用の設定は別途管理し、このファイルには含めないでください

## 次のステップ

PR #2でrealm設定を作成し、このディレクトリに`authplatform-realm.json`を追加します。
