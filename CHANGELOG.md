# 変更履歴

このプロジェクトの主な変更は[Keep a Changelog](https://keepachangelog.com/ja/1.1.0/)に従って記録します。

## [Unreleased]

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

[Unreleased]: https://github.com/roflsunriz/google-app-revanced/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/roflsunriz/google-app-revanced/releases/tag/v0.1.0
