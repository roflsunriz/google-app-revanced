# 調査知見

- 標準検索・アシスタントは元APKのexported aliasとVoiceInteractionServiceを再利用する。非rootクローンは元package明示Intentを横取りできず、Android側の既定アシスタント選択が必要。
- GmsCore向けカテゴリ変更はGCMのRECEIVE/REGISTRATION receiverだけに限定する。公開検索カテゴリは外部呼出し元と元DEXの契約なので改名しない。
- GmsComplianceの非対応経路は拒否画面だけでなくservices/receivers/providersも無効化する。画面を隠すだけでは直らず、既存のfeature-disabled経路へ分岐させcomponent resetを残す。AQUOSでは確認済みだがPixelは未確認。
- Google APKの `$$` drawable名はSDK標準AAPT2で再構築に失敗する。ReVanced CLI同梱AAPT2を使い、`--custom-aapt2-binary` でSDK版へ差し替えない。
- Gradle wrapper更新は旧版で版指定後、新版でも `wrapper` を再実行してJARを更新する。配布ZIPとwrapper JARの公式SHA-256を両方確認する。
- `.github/ISSUE_TEMPLATE/config.yml` は受付設定専用。フォームは別名に置き、既存IDやworkflowが解析する見出しを変える場合は解析側も同時更新する。
