# 更新手順

## 前提

- JDK 21以降
- Android SDKとAPI 36以上のbuild-tools
- `read:packages`権限を持つGitHubトークン
- ReVanced CLI
- APKEditor
- ADBで接続できる検証端末
- 複数世代のGoogle アプリ単一APK

## 手順

1. `git status --short --branch` で既存差分を確認します。
2. `google-app-apks/` に新旧のAPK/XAPKを追加し、版、入手元、SHA-256を記録します。
3. XAPK/APKM/APKSはAPKEditorで単一APKへ統合します。
4. APKを展開し、広告SDK、配信先、広告・プロモーション資源、Composeタグの変更を比較します。
5. 新しい広告経路があれば、安定した一般規則として分類器、通信境界、資源変換、テストへ追加します。
6. `gradle.properties` の版を更新し、`CHANGELOG.md`へ意図と利用者影響を記載します。
7. 次を実行します。

```powershell
./gradlew.bat clean test build :patches:buildAndroid
./scripts/verify-android-rvp.ps1 -Path ./patches/build/libs/patches-<version>.rvp
```

8. ReVanced CLIで複数世代の単一APKへRVPを適用し、資源リンク、DEX生成、整列、署名まで確認します。
9. パッチ後APKを再展開し、広告権限・広告配信URL・広告枠寸法・拡張DEXを確認します。
10. 非root実機へ別パッケージとしてインストールし、標準Googleアプリを一時無効化してから、ReVanced GmsCoreのアカウント、起動、検索、設定統合、スイッチ保存、広告枠が消えて空間が詰まることを目視確認します。
11. 標準Googleアプリを再有効化し、ReVanced版を削除して復旧できることも確認します。
12. ReVanced Managerへ`patches.json`をURL登録し、「高度な設定」で別プロセス実行と700MiB以上のメモリ上限を設定してから、APK選択、パッチ完了、インストール直前まで確認します。通常プロセスでは640MiB未満を検出して早期停止し、数時間のDEX書き込み待ちにならないことも確認します。
13. `git diff --check`、Git状態、秘密情報、生成物混入を確認してコミット・プッシュ・タグ作成します。
14. リリースのRVPと`patches.json`を取得し、ローカル生成物と同じ構造であることを再確認します。

## API 37以降のAPK

公開Android SDKより新しい属性や非公開framework資源は一般のAAPT2で再リンクできない場合があります。このプロジェクトでは、現行端末が無視する将来属性を限定的に除去し、非公開の直接資源参照だけを `@*android:` 記法へ戻します。新しい失敗が出た場合は、エラー行の意味と対象APIを確認し、機能影響をテストへ固定してから対応します。

## ロールバック

- リポジトリは直前タグへ戻し、同じ入力APKでRVPを再生成します。
- 端末は事前に退避したGoogle署名のAPK一式、またはシステムイメージ／Play Store更新へ戻します。
- 非rootでは標準Googleアプリを再有効化し、ReVanced版クローンをアンインストールします。rootマウントを使った場合は、Managerの復旧手順で元のシステムAPKへ戻します。
