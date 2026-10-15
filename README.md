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
6. 「Google ReVanced」を起動します。起動後にCloud Messagingの登録が自動要求されます。ReVanced GmsCoreの「Cloud Messaging」→「Cloud Messagingを使用するアプリ」に「Google ReVanced」が追加されたことを確認します。Discover、検索結果、Google設定内の「Google ReVanced」が開くことも確認します。初回だけGmsCoreのアカウント選択や登録許可が表示される場合があります。

問題があれば標準Googleアプリを「有効にする」へ戻し、`app.revanced.android.googleapp`をアンインストールすれば復旧できます。純正版とクローンを同時に有効にするとGoogle側の構成更新でクローンが終了するため、クローンの初回起動前に純正版を無効化してください。

rootマウントで元パッケージを維持する場合は、Managerの高度な設定でパッチ選択を許可し、「GmsCore support」を無効にしてから適用してください。

## 外部アプリから検索・アシスタントを使う

標準のWeb検索、グローバル検索、アシスタント、音声アシスタントの呼び出しは、Google ReVancedが元APKの受付を引き継ぎます。検索語や追加情報も元のGoogleアプリの処理へ渡ります。

1. 標準Googleアプリを無効にし、Google ReVancedを起動します。
2. Googleアプリの「設定」→「Google ReVanced」→「標準アシスタントとして使う」を開きます。
3. Androidの選択画面でGoogle ReVancedを選びます。既に選択済みの場合や端末が選択要求に対応しない場合は、Androidのアシスタント設定を開きます。
4. 外部アプリの標準検索を実行します。選択画面が出る場合はGoogle ReVancedを選び、継続して使う場合は「常時」を選びます。

選択をキャンセルすると既定アシスタントは変わりません。復旧時はAndroidの既定アシスタントを元に戻し、標準Googleアプリを有効にしてください。

元パッケージ名`com.google.android.googlequicksearchbox`やそのComponentを明示する呼び出しは、非rootの別パッケージへ転送できません。呼び出し元が対応していれば、標準検索またはGoogle ReVancedを呼び出す設定へ変更してください。[AndroidのIntent仕様](https://developer.android.com/guide/components/intents-filters)も参照してください。

## 「Playプロテクトの認定を受けていません」で起動できない場合

「GmsCore support」は、Googleアプリ内のGmsComplianceによる起動制限を、その機能を使わない既存の経路へ切り替えます。以前の制限で無効化されたアプリ内コンポーネントは、Googleアプリ自身の復旧処理へ渡します。AndroidやPlayストアの端末認定、Play Protectの設定は変更しません。

この対応はv0.2.4以降に含まれます。Pixel 10aでの改善と修正版インストール後の動作は未確認です。検証した範囲は[verification.md](verification.md)を参照してください。

修正版RVPで元APKを再パッチし、同じManager署名鍵で更新してから起動してください。データを消去したりPlay Protectを無効にしたりする必要はありません。改善しない場合は、端末・Googleアプリ・GmsCoreの版、拒否画面、秘密情報を除いた起動ログをIssueへ添えてください。

## ReVanced Managerへパッチを追加する

ReVanced Managerの「Patches」タブで編集ボタン、追加ボタン、「Enter URL」の順に開き、次のURLを登録します。

```text
https://github.com/roflsunriz/google-app-revanced/releases/latest/download/patches.json
```

その後「Apps」タブから単一APKを選び、既定の2パッチを適用します。APK bundle、XAPK、APKM、APKSは先に単一APKへ統合してください。公式Managerの現在の操作は[Managing patches](https://github.com/ReVanced/revanced-manager/blob/main/docs/2_3_managing_patches.md)と[Patching apps](https://github.com/ReVanced/revanced-manager/blob/main/docs/2_1_patching.md)も参照してください。

Google アプリは約230MB・14 DEX以上あるため、通常のManagerプロセスに割り当てられる512MiBでは、パッチ後DEXの書き込みが`classes17.dex`付近で極端に遅くなります。パッチ前にManagerの「設定」→「高度な設定」で「Patcherを別のプロセスで実行」を有効にし、メモリ上限を700MiB以上にしてください。端末の空きメモリに余裕があれば1024MiBを推奨します。

メモリ上限が640MiB未満の場合、このパッチは、数時間かかる処理を続けずに早期停止し、エラーで設定変更を案内します。700MiB制限で複数世代のGoogle アプリを最後まで再構築できることを確認しています。実測の詳細は[verification.md](verification.md)に記録しています。

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

詳細は [findings.md](findings.md#readme-1) に移しました。

## ライセンス

[MIT License](LICENSE)

## 依存更新の自動処理

詳細は [findings.md](findings.md#readme-2) に移しました。
