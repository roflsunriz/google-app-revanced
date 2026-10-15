# findings.md

google-app-revanced の実現手段、共通指針の適用条件と調査知見の正本。作業前に全文読む。利用者向けの導入・使用方法はREADME、更新の手順はhow-to-update.md、実施結果と未検証事項はverification.mdを参照する。移した記述の確認日・条件・留保は元のまま保持し、文書移動を再実測と解釈しない。

## 共通指針の適用条件と代替検証

- 対象の共通ルール: 設計と実装方針／検証／ReVanced
- 理由: Android APKへ適用するKotlin/GradleパッチとDEX/RVEを生成し、TypeScript/Bunコマンドでは検証できない。APKの特殊drawable名にはCLI同梱AAPT2を使用する。
- 適用範囲: Gradleビルド、パッチの互換性とManager適用
- 同等の品質を保つ手段・検証: GradleテストとbuildAndroid、RVP内容確認、複数APK世代へのCLI適用、Managerでの再構築と実機確認を用いる。Pixelの未確認事項はverification.mdで追跡し、実機の承認条件を弱めない。

新たな免除や操作許可を追加するものではない。AGENTS.mdの禁止・承認条件と共通指針は引き続き有効。

<a id="agents-1"></a>

## 外部検索・アシスタントとGmsCore

- 標準の検索・アシスタントIntentは元APKのexported activity-aliasとVoiceInteractionServiceを再利用する。検索語・extrasの転送用に別Activityを設けない。
- GmsCore用カテゴリの変更はGCMのRECEIVE/REGISTRATIONを受けるreceiverのintent-filterだけに限定する。公開検索カテゴリ（元パッケージ名のSEARCH_WIDGETなど）は外部呼び出し元とGoogleのDEXが使う契約なので改名しない。根拠は`GmsCoreTransforms.kt`と`GmsCoreTransformsTest.kt`。
- 非rootクローンは元パッケージを明示したIntentを横取りできない。Issue #8の対象はユーザー確認により標準の検索・アシスタント呼び出し。Android側の既定アシスタント選択を必要とする。
- 17.52.24とAQUOSから採取した17.54.11のDEXでは、GmsComplianceの非対応判定が`UncertifiedDeviceActivity`への転送だけでなく、services/receivers/providersの無効化も行う。拒否画面だけを隠しても復旧しない。GmsCore support内の`GmsCompliancePatch.kt`は2つのログ文字列とBoolean分岐を検出し、既存のfeature-disabled経路へ切り替えてcomponent resetを残す。Pixelでの修正確認は別途必要。
- Google APKの`$$` drawable名はSDK標準AAPT2 37.0.0で再構築に失敗する。CLI検証ではReVanced CLI既定のAAPT2を使い、`--custom-aapt2-binary`でSDK標準版へ置き換えない。17.50.19/17.52.24で確認（2026-10-11）。
- Gradle wrapper更新では、旧Gradleによる`wrapper --gradle-version`実行後、新しいGradleでも`wrapper`を再実行してJARも更新する。配布ZIPの`distributionSha256Sum`とwrapper JARの公式SHA-256を両方確認する。Gradle 9.8.1で確認（2026-10-11）。

移動元: `AGENTS.md`。

<a id="agents-2"></a>

## GitHub受付テンプレートの保守

- `.github/ISSUE_TEMPLATE/config.yml` はIssue作成画面の受付設定専用とし、設定・互換性の相談フォームは `configuration.yml` に置く。予約名へフォームの `name` や `body` を配置しない。
- 受付フォームを変更するときは、既存の `id`、選択肢、必須条件、固有の質問と案内を保全する。ワークフローが解析する見出し（`対象領域`、`OS` など）がある場合は、対応する解析処理との整合を確認する。根拠は `.github/ISSUE_TEMPLATE/` と `.github/workflows/`。

移動元: `AGENTS.md`。

<a id="readme-1"></a>

## 開発者向け

前提はJDK 21以降とAndroid SDKです。GitHub PackagesからReVanced Gradleプラグインを解決するため、`read:packages`を持つトークンをGradleプロパティへ渡します。

```powershell
./gradlew.bat test :patches:buildAndroid
```

生成された `patches/build/libs/patches-*.rvp` にはAndroid用 `classes.dex` と `extensions/googleapp.rve` が含まれます。更新手順は [how-to-update.md](how-to-update.md)、検証内容は [verification.md](verification.md) を参照してください。

移動元: `README.md`。

<a id="readme-2"></a>

## 依存更新の自動処理

Dependabot は対象の依存関係を毎週確認します。patch／minor／major 更新は、現在のPRのコミットと全チェックの成功を確認した後に自動でsquash mergeされます。CIの失敗ジョブは1回だけ再実行し、再失敗時は対象を限定したGradle lockfileの再生成と再検証を試みます。修復できないPRは手動で対応します。

移動元: `README.md`。

<a id="update-1"></a>

## Dependabot PR の更新

前提は `.github/dependabot.yml` と PR 用 CI（CI）です。更新PRのhead SHAと`gh pr checks <PR番号>`の結果を確認してください。patch／minor／majorは現在のコミットの全チェック成功後に自動取り込みされます。初回CI失敗はfailed jobsだけを1回再実行し、再失敗時は許可された3つのGradle lockfileを再生成して再検証します。修復できないPRは手動で対応します。

設定を変えたときは `actionlint .github/workflows/dependabot-automation.yml` と実際の PR の Actions 結果を確認します。問題があれば呼び出し先の共通 workflow SHA を直前の検証済み値へ戻すコミットを push します。取り込まれた依存更新に問題があれば通常の revert コミットで復旧します。

CI 完了より Dependabot の分類が遅れる場合は、`callback_workflow_file` が指す呼び出し側 workflow を `workflow_dispatch` し、同じ PR 番号・head SHA・全チェックを再確認する。呼び出し側のファイル名を変える際はこの入力も一緒に更新する。

移動元: `how-to-update.md`。

<a id="verification-1"></a>

## Dependabot 自動処理（2026-09-23）

`.github/workflows/dependabot-automation.yml` を actionlint で検査し、PR 用 workflow 名（CI）と一致することを確認する。Dependabot の patch／minor かつ全 PR チェック成功の場合だけ取り込み、major・古い SHA・再失敗は残す。

実際の Dependabot PR がまだない場合、動作経路は未検証として扱う。実 PR 発生後に自動化ジョブ、CI の再試行、マージ結果を確認する。

大量の Dependabot PR により CI 完了より分類が遅れる場合でも、分類後の `workflow_dispatch` が現在の PR 番号と head SHA を照合して再評価する。別の作成者、古い SHA、未完了の CI はマージしない。

移動元: `verification.md`。

## 関連文書

- [利用者向けREADME](README.md)
- [更新と復旧](how-to-update.md)
- [検証結果と未検証範囲](verification.md)
- [変更履歴](CHANGELOG.md)
