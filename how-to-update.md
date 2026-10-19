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
10. 利用者のインストール承認を得て非root実機へ別パッケージとして導入し、標準Googleアプリを一時無効化してから、ReVanced GmsCoreのCloud Messagingアプリ一覧へクローン版が自動登録されること、アカウント、Discover、検索、設定統合、スイッチ保存、広告枠が消えて空間が詰まることを目視確認します。更新時は既存のManager署名鍵を使い、データを保持します。
11. 標準Googleアプリを再有効化し、ReVanced版を削除して復旧できることも確認します。
12. ReVanced Managerへ`patches.json`をURL登録し、「高度な設定」で別プロセス実行と700MiB以上のメモリ上限を設定してから、APK選択、パッチ完了、インストール直前まで確認します。通常プロセスでは640MiB未満を検出して早期停止し、数時間のDEX書き込み待ちにならないことも確認します。
13. `git diff --check`、Git状態、秘密情報、生成物混入を確認してコミット・プッシュ・タグ作成します。
14. リリースのRVPと`patches.json`を取得し、ローカル生成物と同じ構造であることを再確認します。

## リリース公開

`gradle.properties`と`CHANGELOG.md`の版・日付を揃え、`scripts/generate-patches-json.ps1 -Version <version> -OutputPath patches.json`でリポジトリの配布メタデータも更新します。ローカルの検証と監査後にmainへpushし、CIの成功を確認してから同じコミットへ`v<version>`タグを付けてpushします。

タグのpushで`.github/workflows/release.yml`がテスト・ビルド・RVP構造検査を行い、該当版の変更履歴を本文にしたリリースへ`google-app-patches.rvp`と`patches.json`を公開します。公開後に両ファイルを取得し、JSONの版と配布URL、Android用DEXと拡張、ビルド証明を確認します。Managerの既存URLソースにも新しい版が表示されることを確認します。

リリース済みタグの付け替えや過去リリースの上書きは行わず、問題があれば修正コミットと次の版で配布します。

## 外部呼び出しと起動制限の回帰確認

- アプリ内の「Google ReVanced」から既定アシスタントの選択画面を開き、キャンセル時に既定値が変わらないこと、Google ReVanced選択後に標準アシスタント呼び出しが届くことを確認します。
- 外部の`WEB_SEARCH`へ検索語を渡し、Google ReVancedに同じ検索語が表示されることを確認します。`GLOBAL_SEARCH`、`ASSIST`、`VOICE_ASSIST`と、元の`SEARCH_WIDGET`カテゴリを付けた呼び出しも確認します。パッケージを明示する呼び出しは対象外です。
- GmsComplianceの起動拒否が再現する端末では、同じ署名の更新後に起動、検索、Discover、Cloud Messagingを確認します。`Feature is disabled!`経路に進み、拒否画面への転送とコンポーネント無効化が再発しないことを確認します。API認証やPlay Protectの設定を変更して回避しません。
- 複数チャットで実機を共有する場合は、UI操作・設定変更・インストール前に使用順を合意し、検証終了後に解放を連絡します。

## 依存監査と資源再構築

Gradle依存を更新した際は、対象モジュールの`dependencies --update-locks '<group>:*'`で未実行構成も解決し、lockfileの差分とOSV監査を確認します。`settings-gradle.lockfile`を監査ツールが識別できない場合は、内容を変えずに一時フォルダの`gradle.lockfile`へコピーして入力します。

Google APKは`$$`で始まるdrawable名を含みます。SDK標準AAPT2が`invalid entry name`で失敗する場合は、ReVanced CLI既定のAAPT2を使ってください。資源の削除や手動改名で失敗を隠さないでください。

## API 37以降のAPK

公開Android SDKより新しい属性や非公開framework資源は一般のAAPT2で再リンクできない場合があります。このプロジェクトでは、現行端末が無視する将来属性を限定的に除去し、非公開の直接資源参照だけを `@*android:` 記法へ戻します。新しい失敗が出た場合は、エラー行の意味と対象APIを確認し、機能影響をテストへ固定してから対応します。

## ロールバック

- リポジトリは直前タグへ戻し、同じ入力APKでRVPを再生成します。
- 端末は事前に退避したGoogle署名のAPK一式、またはシステムイメージ／Play Store更新へ戻します。
- 非rootでは標準Googleアプリを再有効化し、ReVanced版クローンをアンインストールします。rootマウントを使った場合は、Managerの復旧手順で元のシステムAPKへ戻します。

## Dependabot PR の更新



Dependabot version update は毎月28日00:00、検証済み PR の自動マージは同日18:00（いずれも `Asia/Tokyo`）にまとめます。28日より前に自動マージせず、現在の head SHA と全 PR チェックを月末処理で再確認します。
詳細は [findings.md](findings.md#update-1) に移しました。
