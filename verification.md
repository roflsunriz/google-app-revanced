# 検証記録

## 自動検証

- Kotlin分類器: 広告ドメイン検出、通常Google URLの非誤検出
- 資源変換: 広告レイアウトの幅・高さゼロ、`GONE`、広告寸法ゼロ
- マニフェスト変換: 広告ID・AdServices権限除去、広告測定コンポーネント無効化
- API 37互換: 非公開framework直接参照、将来の音声対話属性
- Android拡張: 広告URL判定、資源名の誤検出防止

## 2026-08-31の実測

- 入力 `17.50.19.ve.arm64` と `17.52.24.ve.arm64` の単一APKへReVanced CLI 6.0.0で適用しました。
- 17.50.19は13 DEXから19 DEX、17.52.24は14 DEXから19 DEXへ再構築し、Android拡張、資源リンク、APK整列、v3署名まで成功しました。
- 再展開したAPKで `ad_lightbox`、`duplo_ad_video`、`ads_container`、各種promoレイアウトが `0dp × 0dp` かつ `GONE` であることを確認しました。
- `AD_ID`、AdServices、昇格通知、Ad Manager宣言が除去されていることを確認しました。
- 元APKに含まれた主要広告配信URLが、両版のパッチ後DEXでは0件になったことを確認しました。
- OSV-Scanner 2.5.1でGradle lockfileを監査し、修正版へ依存を統一した後の既知脆弱性が0件であることを確認しました。
- 物理端末 SH-R80P 上で設定Activityを表示し、日本語ダークテーマ、4スイッチ、OFF/ON操作、プロセス再起動後の保存を確認しました。
- 元Google アプリへの上書きはGoogle証明書とReVanced証明書が異なるため、Androidが `INSTALL_FAILED_UPDATE_INCOMPATIBLE` で拒否しました。元アプリとデータは変更されていません。

## 目視確認項目

rootで同一パッケージを安全にマウントできる端末では、次を確認します。

1. Discover、画像検索、Web検索、動画表示を広告が出る条件で開きます。
2. 広告カード、広告バッジ、動画広告、セルフプロモーションが表示されないことを確認します。
3. 各広告の前後で余白、空カード、スクロール停止位置が残らず、隣接コンテンツが詰まることを確認します。
4. 設定一覧末尾の「Google ReVanced」と各スイッチを確認します。
5. Google検索、Lens、音声検索、Discover、設定など広告以外の主要機能に退行がないことを確認します。

## 残る環境制約

Google アプリは署名・UID・privileged permissionへ強く依存します。非root端末上の別パッケージ化ではGoogle本体が署名検証で起動しないため、本番Google画面の目視検証にはGoogle署名を維持できるrootマウント環境が必要です。
