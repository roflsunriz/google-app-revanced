# 検証記録

## 自動検証

- Kotlin分類器: 広告ドメイン検出、通常Google URLの非誤検出
- 資源変換: 広告レイアウトの幅・高さゼロ、`GONE`、広告寸法ゼロ
- マニフェスト変換: 広告ID・AdServices権限除去、広告測定コンポーネント無効化
- API 37互換: 非公開framework直接参照、将来の音声対話属性
- Android拡張: 広告URL判定、資源名の誤検出防止
- GmsCore互換: クローンパッケージ、権限・authority、spoofメタデータ、process名変換、resource参照authorityの保持

## 2026-09-01の実測

- 入力 `17.50.19.ve.arm64` と `17.52.24.ve.arm64` の単一APKへReVanced CLI 6.0.0で適用しました。
- 17.50.19は13 DEXから19 DEX、17.52.24は14 DEXから18 DEXへ再構築し、Android拡張、資源リンク、APK整列、v3署名まで成功しました。
- 再展開したAPKで `ad_lightbox`、`duplo_ad_video`、`ads_container`、各種promoレイアウトが `0dp × 0dp` かつ `GONE` であることを確認しました。
- `AD_ID`、AdServices、昇格通知、Ad Manager宣言が除去されていることを確認しました。
- 元APKに含まれた主要広告配信URLが、両版のパッチ後DEXでは0件になったことを確認しました。
- OSV-Scanner 2.5.1でGradle lockfileを監査し、修正版へ依存を統一した後の既知脆弱性が0件であることを確認しました。
- 物理端末 SH-R80P（非root）へ `app.revanced.android.googleapp` として導入し、標準Googleアプリを無効化した状態でホーム、Googleアカウント、天気・スポーツカード、Discover、Web検索結果を表示できることを確認しました。
- ReVanced GmsCore 0.3.13.2.250932で元パッケージ名とGoogleアプリの旧署名ローテーション証明書のspoof、`googlenow` OAuth、AuthProxyサービス接続、Phenotype接続が成立することを確認しました。
- GmsCoreにNative Cronet実装がない環境ではGoogleアプリ内蔵Java Cronetへ切り替わり、`ReVanced`と`insurance`の検索結果が表示されることを確認しました。
- `insurance`の検索結果で広告・スポンサー表記と空の広告枠が0件で、通常コンテンツが上端から詰めて表示されることをスクリーンショットとUI階層で確認しました。
- Google設定一覧で「Google ReVanced」が既存項目と重ならず表示され、クリックして日本語ダークテーマの4スイッチへ遷移できることを確認しました。
- 広告SDK通信遮断は常時ONかつ変更不可、残る3スイッチは操作可能で、Web検索広告をOFFにした状態がプロセス再起動後も保存されることを確認しました。
- WebView再帰、プロセスクラッシュ、Spatula取得失敗、Native Cronet構築失敗、APIパッケージ拒否が成功した検索経路では0件であることを確認しました。
- 標準Googleアプリを再有効化すれば即座に純正版へ戻せることを確認しました。ReVanced版の削除はクローンのデータを失うため、必要な場合だけ実施します。
- 公開URLの`patches.json`をReVanced Manager 2.6.0へ登録し、「Google App ReVanced Patches」v0.2.0・2パッチとしてエラーなく読み込めることを確認しました。
- Managerで17.52.24を選択すると2パッチが既定選択され、準備2/2・パッチ適用3/3までは完了しました。ただしAPK保存が0/2のまま47分以上進まず、CPUを消費し続ける異常状態になったためキャンセルしました。Manager経由のインストール直前画面には到達していません。CLI、CI、公開RVPの同じパッチは正常に生成・適用・実機起動できるため、Manager 2.6.0の端末内保存工程に残る環境依存問題として記録します。

## 目視確認項目

非root端末では標準Googleアプリを無効化し、ReVanced GmsCoreとクローン版を使って次を確認します。

1. Discover、画像検索、Web検索、動画表示を広告が出る条件で開きます。
2. 広告カード、広告バッジ、動画広告、セルフプロモーションが表示されないことを確認します。
3. 各広告の前後で余白、空カード、スクロール停止位置が残らず、隣接コンテンツが詰まることを確認します。
4. 設定一覧末尾の「Google ReVanced」と各スイッチを確認します。
5. Google検索、Lens、音声検索、Discover、設定など広告以外の主要機能に退行がないことを確認します。

## 残る環境制約

- ReVanced GmsCoreのGoogle端末登録、Cloud Messaging、デバイス認証を有効にする必要があります。
- 初回のアカウント連携ではGmsCoreのログイン画面が開く場合があります。認証情報をログやIssueへ貼らないでください。
- Assistant、Lens、音声検索など、端末固有権限や追加モジュールを使う全機能は検索・Discover経路とは別に確認が必要です。
