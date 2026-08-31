# Google App ReVanced

Google アプリ（`com.google.android.googlequicksearchbox`）から広告通信、広告表示枠、アプリ内セルフプロモーションを除去する ReVanced パッチです。

## できること

- Google Mobile Ads、IMA、AdServices の既知広告配信先への通信を遮断します。
- Web 検索結果内の広告 DOM を削除し、残った空白も詰めます。
- ネイティブ広告カード、動画広告枠、画像検索の Compose 広告枠を非表示にします。
- Google アプリ自身のプロモーション用レイアウトを幅・高さ `0dp`、`GONE` にします。
- Google アプリの設定一覧へ「Google ReVanced」を追加し、動的な除去項目を設定できます。
- 対応バージョンを固定せず、安定したAPI・資源名・構造を検出して適用します。

## 利用前の重要事項

Google アプリは多くの端末でGoogle署名のシステムアプリです。ReVancedで署名したAPKは証明書が異なるため、rootなしの端末では同じパッケージ名へ上書きインストールできません。`INSTALL_FAILED_UPDATE_INCOMPATIBLE` はパッチ失敗ではなくAndroidの署名保護です。

実際の導入には、元へ戻せるバックアップを用意したうえで、root対応のReVanced Managerによるマウント方式など、端末側でシステムアプリを安全に置き換えられる環境が必要です。Googleアカウント、Assistant、システム連携を使うため、別パッケージ名への変更はサポートしていません。

## ReVanced Managerへパッチを追加する

ReVanced Managerの「Patches」タブで編集ボタン、追加ボタン、「Enter URL」の順に開き、次のURLを登録します。

```text
https://github.com/roflsunriz/google-app-revanced/releases/latest/download/patches.json
```

その後「Apps」タブから単一APKを選び、「Google ReVanced」を適用します。APK bundle、XAPK、APKM、APKSは先に単一APKへ統合してください。公式Managerの現在の操作は[Managing patches](https://github.com/ReVanced/revanced-manager/blob/main/docs/2_3_managing_patches.md)と[Patching apps](https://github.com/ReVanced/revanced-manager/blob/main/docs/2_1_patching.md)も参照してください。

## APKの準備

このリポジトリはAPKを配布しません。APKMirror、APKPure、Uptodownなどから対象APKを取得し、入手元、版、CPUアーキテクチャ、SHA-256を記録してください。

- 単一APKはそのまま使用できます。
- XAPK/APKM/APKSはPCではAPKEditor、AndroidではAnti Split Mなどで単一APKへ統合します。
- 端末のCPUアーキテクチャとAPKのアーキテクチャを一致させます。
- 元APKとアプリデータを退避し、復旧方法を先に確認します。

## パッチ後の設定

Google アプリの「設定」一覧末尾に「Google ReVanced」が追加されます。

- 広告SDK通信を遮断: 常時有効です。
- Web検索広告を非表示: 既定で有効です。
- セルフプロモーションを非表示: 既定で有効です。
- ネイティブ広告枠を非表示: 既定で有効です。

## 開発者向け

前提はJDK 21以降とAndroid SDKです。GitHub PackagesからReVanced Gradleプラグインを解決するため、`read:packages`を持つトークンをGradleプロパティへ渡します。

```powershell
./gradlew.bat test :patches:buildAndroid
```

生成された `patches/build/libs/patches-*.rvp` にはAndroid用 `classes.dex` と `extensions/googleapp.rve` が含まれます。更新手順は [how-to-update.md](how-to-update.md)、検証内容は [verification.md](verification.md) を参照してください。

## ライセンス

[MIT License](LICENSE)
