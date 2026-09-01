# Google App ReVanced

Google アプリ（`com.google.android.googlequicksearchbox`）から広告通信、広告表示枠、アプリ内セルフプロモーションを除去する ReVanced パッチです。

## できること

- Google Mobile Ads、IMA、AdServices の既知広告配信先への通信を遮断します。
- Web 検索結果内の広告 DOM を削除し、残った空白も詰めます。
- ネイティブ広告カード、動画広告枠、画像検索の Compose 広告枠を非表示にします。
- Google アプリ自身のプロモーション用レイアウトを幅・高さ `0dp`、`GONE` にします。
- Google アプリの設定一覧へ「Google ReVanced」を追加し、動的な除去項目を設定できます。
- 対応バージョンを固定せず、安定したAPI・資源名・構造を検出して適用します。

## 非root端末への導入

Google署名の標準アプリは別証明書で上書きできないため、既定の「GmsCore support」パッチはReVanced版を `app.revanced.android.googleapp` として別にインストールします。Play Services連携には[ReVanced GmsCore](https://github.com/ReVanced/GmsCore/releases/latest)（`app.revanced.android.gms`）が必要です。

1. ReVanced GmsCoreをインストールし、Googleデバイス登録、Cloud Messaging、デバイス認証を有効にして、必要なGoogleアカウントを追加します。
2. 下記手順でパッチソースをManagerへ追加します。
3. Google アプリの単一APKを選び、既定の「GmsCore support」と「Google ReVanced」を両方適用します。
4. 「Google ReVanced」をインストールします。
5. Androidの「設定」→「アプリ」→標準の「Google」を開き、「無効にする」を選びます。
6. 「Google ReVanced」を起動します。起動後にCloud Messaging登録が自動要求されるため、ReVanced GmsCoreの「Cloud Messaging」→「Cloud Messagingを使用するアプリ」に「Google ReVanced」が追加され、Discover、検索結果、Google設定内の「Google ReVanced」が開くことを確認します。初回だけGmsCoreのアカウント選択や登録許可が表示される場合があります。

問題があれば標準Googleアプリを「有効にする」へ戻し、`app.revanced.android.googleapp`をアンインストールすれば復旧できます。純正版とクローンを同時に有効にするとGoogle側の構成更新でクローンが終了するため、クローンの初回起動前に純正版を無効化してください。

rootマウントで元パッケージを維持する場合は、Managerの高度な設定でパッチ選択を許可し、「GmsCore support」を無効にしてから適用してください。

## ReVanced Managerへパッチを追加する

ReVanced Managerの「Patches」タブで編集ボタン、追加ボタン、「Enter URL」の順に開き、次のURLを登録します。

```text
https://github.com/roflsunriz/google-app-revanced/releases/latest/download/patches.json
```

その後「Apps」タブから単一APKを選び、既定の2パッチを適用します。APK bundle、XAPK、APKM、APKSは先に単一APKへ統合してください。公式Managerの現在の操作は[Managing patches](https://github.com/ReVanced/revanced-manager/blob/main/docs/2_3_managing_patches.md)と[Patching apps](https://github.com/ReVanced/revanced-manager/blob/main/docs/2_1_patching.md)も参照してください。

Google アプリは約230MB・14 DEX以上あるため、通常のManagerプロセスに割り当てられる512MiBでは、パッチ後DEXの書き込みが`classes17.dex`付近で極端に遅くなります。パッチ前にManagerの「設定」→「高度な設定」で「Patcherを別のプロセスで実行」を有効にし、メモリ上限を700MiB以上にしてください。端末の空きメモリに余裕があれば1024MiBを推奨します。

メモリ上限が640MiB未満の場合、このパッチは数時間待たせる代わりに、設定変更を案内するエラーで早期停止します。700MiB制限で複数世代のGoogle アプリを最後まで再構築できることを確認しています。実測の詳細は[verification.md](verification.md)に記録しています。

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
