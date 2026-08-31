# コントリビューション

## 不具合報告

Issueテンプレートを使い、Google アプリ版、APK形式、CPU、ReVanced Manager/CLI版、適用ログ、再現手順、期待結果、実際の結果を記載してください。APK本体、認証情報、アカウント情報は添付しないでください。

## 変更手順

1. `COMMON-AGENTS.md` と `how-to-update.md` を確認します。
2. 既存差分を保護し、広告経路を単一版の偶然ではなく一般規則で検出します。
3. 不具合を先に再現するテストを追加します。
4. `./gradlew.bat test build :patches:buildAndroid` を成功させます。
5. 複数世代APK、RVP内DEX、再展開資源、実機を検証します。
6. `CHANGELOG.md` と必要な利用者文書を更新します。

コミットは日本語Conventional Commits形式にしてください。第三者APKや逆コンパイル済みGoogleコードをコミットしないでください。
