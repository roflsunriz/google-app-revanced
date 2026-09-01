# 変更履歴

このプロジェクトの主な変更は[Keep a Changelog](https://keepachangelog.com/ja/1.1.0/)に従って記録します。

## [Unreleased]

## [0.2.3] - 2026-09-02

### Fixed

- クローン版がReVanced GmsCoreのCloud Messagingへ登録されずDiscoverを取得できない問題を防ぐため、GCM受信カテゴリを実パッケージ名へ変換し、`:googleapp`固有の初期化ProviderからGmsCore互換登録を自動要求して、返されたトークンをGoogleアプリ内蔵Firebaseの保存形式へ同期するようにしました。

## [0.2.2] - 2026-09-02

### Fixed

- クローン版の音声検索、曲検索、Assistant系APIが`X-Android-Package`へクローン名を送りGoogle APIから拒否される問題を防ぐため、API認証ヘッダーの直接キーと静的キーを横断検出し、入力APK内の公式パッケージ名・公式API証明書へ限定的に置換するようにしました。

## [0.2.1] - 2026-09-01

### Fixed

- ReVanced Managerの通常プロセスでは巨大なGoogle アプリのDEX再構築が`classes17.dex`付近で数時間停滞するため、640MiB未満のヒープを早期検出して別プロセス実行を案内し、実用的でない待機を防ぐようにしました。
- 多DEX走査中の一時メモリと不要な再変換を減らすため、GmsCore互換処理を対象メソッド検出後にだけmutable化し、全メソッドの巨大な中間リストを遅延走査へ変更しました。
- ReVanced GmsCore環境でPixel Launcherの信頼確認がDynamite版GoogleCertificatesを初期化できず起動停止するため、systemアプリとQSB権限を確認済みのLauncher連携箇所だけを限定的に信頼するよう修正しました。
- DependabotのJUnit 6.1.3更新後も依存ロック競合なくテストできるよう、マージ後の全構成からGradle lockfileを再生成しました。

## [0.2.0] - 2026-09-01

### Added

- 非root端末でも検索とGoogleアカウント連携を利用できるよう、ReVanced GmsCoreへ元パッケージ名とOAuth登録証明書を通知し、Play Servicesの権限・authority・サービス接続を転送する「GmsCore support」パッチを追加しました。
- 標準Googleアプリを残したまま導入できるよう、既定のパッチ後パッケージを `app.revanced.android.googleapp`、表示名を「Google ReVanced」にしました。

### Fixed

- 別パッケージ時に実プロセス名とGoogleアプリ内のハッシュ定数が不一致になり、誤ったDagger/Hiltコンポーネントで起動停止する問題を、Daggerのプロセス選択箇所だけに限定した互換フックで修正しました。
- GmsCore環境で構成更新やシステム連携を再試行した際、Googleアプリ内の`killProcess`／`System.exit`がクローンを自己終了させる経路を無効化しました。
- 元パッケージ名の過剰置換でGoogle APIヘッダーまでクローン名になり検索が拒否される問題を修正し、`googlenow` OAuthに使われるGoogleアプリの旧署名ローテーション証明書SHA-1をGmsCoreへ通知するようにしました。
- GmsCoreが端末証明キーを取得できない場合のSpatulaフォールバック失敗を許容し、GmsCoreにNative Cronet実装がない場合はGoogleアプリ内蔵Java Cronetへ切り替えることで検索結果を表示できるようにしました。
- WebViewの`loadUrl`をラッパーへ置換した際にサブクラスから自己再帰し検索が停止する問題を、URL検査と広告除去スクリプトの事前挿入方式へ変更して修正しました。
- Google設定一覧の「Google ReVanced」行が既存項目と重ならず全文を読めるよう、設定リスト領域と分離したテーマ準拠のフッターとして統合しました。

### Changed

- 非root利用者の導入手順を、クローンのインストール後・初回起動前に標準Googleアプリを無効化し、問題時は再有効化して復旧できる手順へ更新しました。

## [0.1.0] - 2026-08-31

### Added

- Google アプリ内の広告通信を遮断するため、DoubleClick、Google Ads、Google Ad Services、Google Syndication、IMA SDKの通信境界を書き換えるReVancedパッチを追加しました。
- 広告表示後の空白を残さないため、広告・プロモーション用レイアウトと寸法を幅・高さゼロかつ非表示へ変換する資源パッチを追加しました。
- 画像検索のCompose広告枠を描画前に除去する、文字列フィンガープリント方式のバージョン非依存処理を追加しました。
- 検索結果の広告DOM、ネイティブ広告カード、アプリ内セルフプロモーションを実行時にも除去するAndroid拡張を追加しました。
- 利用者が動的な除去項目を管理できるよう、Google アプリの設定一覧へ「Google ReVanced」を統合しました。
- 日本語と主要10言語、アラビア語・ウルドゥー語のRTL表示に対応した設定画面を追加しました。
- Android用DEXを含むRVP、ReVanced API形式の`patches.json`、リリース・CI・更新・検証文書を追加しました。

### Security

- 広告ID、AdServices attribution、昇格通知権限と広告測定コンポーネントを無効化し、広告SDKへ識別子や測定イベントが渡る経路を削減しました。
- ビルド・検証経路の既知脆弱性を除くため、Netty、Protobuf、Commons Lang、Apache HttpClient、Bouncy Castleの推移依存をOSV修正版へ固定しました。

[Unreleased]: https://github.com/roflsunriz/google-app-revanced/compare/v0.2.3...HEAD
[0.2.3]: https://github.com/roflsunriz/google-app-revanced/compare/v0.2.2...v0.2.3
[0.2.2]: https://github.com/roflsunriz/google-app-revanced/compare/v0.2.1...v0.2.2
[0.2.1]: https://github.com/roflsunriz/google-app-revanced/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/roflsunriz/google-app-revanced/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/roflsunriz/google-app-revanced/releases/tag/v0.1.0
