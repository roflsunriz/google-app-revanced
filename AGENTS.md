# AGENTS.md

## 作業開始前の必須手順（最優先・例外なし）

1. エージェントは、調査、計画、コマンド実行、スキル利用、ファイル編集、コミット、プッシュを始める前に、必ずリポジトリ直下の `.\COMMON-AGENTS.md` を開き、先頭から末尾まで全文を読む。
2. `COMMON-AGENTS.md` はGit管理外のシンボリックリンクである。`git`や既定のignore設定が有効な`rg --files`の検索結果だけで、ファイルが存在しないと判断してはならない。PowerShellでは最初に次を実行する。

```powershell
Get-Content -Raw -LiteralPath .\COMMON-AGENTS.md
```

3. 読み取りに失敗した場合、出力が省略された場合、または末尾まで読めたことを確認できない場合は、一切の作業を開始せず、パスとシンボリックリンク先を確認して全文を再取得する。必要なら分割して末尾まで読む。
4. 全文を読了するまで、ローカル `AGENTS.md` だけを根拠に作業を続けてはならない。読了後は `COMMON-AGENTS.md` を最優先の指針とし、読了直後の最初の進捗報告で全文を読了したことを明示する。
   このファイルでは `google-app-revanced` 固有の補足だけを記載する。

## 目的
- Google(ID:com.google.android.googlequicksearchbox)アプリ用のReVanced patchを作る
- 広告を除去する

## 外部検索・アシスタントとGmsCore

- 標準の検索・アシスタントIntentは元APKのexported activity-aliasとVoiceInteractionServiceを再利用する。検索語・extrasの転送用に別Activityを設けない。
- GmsCore用カテゴリの変更はGCMのRECEIVE/REGISTRATIONを受けるreceiverのintent-filterだけに限定する。公開検索カテゴリ（元パッケージ名のSEARCH_WIDGETなど）は外部呼び出し元とGoogleのDEXが使う契約なので改名しない。根拠は`GmsCoreTransforms.kt`と`GmsCoreTransformsTest.kt`。
- 非rootクローンは元パッケージを明示したIntentを横取りできない。Issue #8の対象はユーザー確認により標準の検索・アシスタント呼び出し。Android側の既定アシスタント選択を必要とする。
- 17.52.24とAQUOSから採取した17.54.11のDEXでは、GmsComplianceの非対応判定が`UncertifiedDeviceActivity`への転送だけでなく、services/receivers/providersの無効化も行う。拒否画面だけを隠しても復旧しない。GmsCore support内の`GmsCompliancePatch.kt`は2つのログ文字列とBoolean分岐を検出し、既存のfeature-disabled経路へ切り替えてcomponent resetを残す。Pixelでの修正確認は別途必要。
- Google APKの`$$` drawable名はSDK標準AAPT2 37.0.0で再構築に失敗する。CLI検証ではReVanced CLI既定のAAPT2を使い、`--custom-aapt2-binary`でSDK標準版へ置き換えない。17.50.19/17.52.24で確認（2026-10-11）。

## GitHub受付テンプレートの保守

- `.github/ISSUE_TEMPLATE/config.yml` はIssue作成画面の受付設定専用とし、設定・互換性の相談フォームは `configuration.yml` に置く。予約名へフォームの `name` や `body` を配置しない。
- 受付フォームを変更するときは、既存の `id`、選択肢、必須条件、固有の質問と案内を保全する。ワークフローが解析する見出し（`対象領域`、`OS` など）がある場合は、対応する解析処理との整合を確認する。根拠は `.github/ISSUE_TEMPLATE/` と `.github/workflows/`。
