---
name: stilme-questionnaire-changes
description: Use when changing questionnaire content in stilme-qe-app, such as adding a psychometric scale, adding a social media app to the monitored list, or editing the informed consent text and its clickable links.
---

# Questionnaire changes in stilme-qe-app

Every questionnaire variable that reaches REDCap must exist in `stilme_qe_data_dictionary.csv` with the same name; a mismatch is a deterministic 4xx rejection at submit time. Baseline (T0) items carry no suffix, monthly items carry `_m`; score fields carry `_t0` or `_m`.

## Adding a psychometric scale

1. Define the items in `app/src/main/java/com/aldogor/stilme_qe_app/questionnaire/QuestionnaireData.kt` with `QuestionType.SCALE`, a shared `scaleGroup` and `scaleInstructions`, for both the T0 and the monthly questionnaire.
2. Add a `calculateXxx(responses, suffix)` function in `questionnaire/ScoringEngine.kt` (reverse items follow the PSS-10 pattern, `4 - response`) and call it from `calculateScores()`.
3. Add the field to `ScaleScores` in `study/StudyModels.kt`.
4. Add the score to the REDCap payload in `questionnaire/QuestionnaireActivity.kt`, next to `bsmas_score$scoreSuffix`.
5. Add the items and the score field to `stilme_qe_data_dictionary.csv` for the `start_q` and `month_q` instruments, then re-import the dictionary into REDCap.
6. Extend `app/src/test/java/com/aldogor/stilme_qe_app/questionnaire/ScoringEngineTest.kt`.

## Adding a monitored app

Add a `"package.name" to "Display Name"` entry to `AppConfig.MONITORED_APPS` in `app/src/main/java/com/aldogor/stilme_qe_app/Models.kt`. Usage collection, CSV export and the REDCap usage payload all iterate that map; nothing else changes.

## Updating the consent text

1. Edit `consent_full_text` in `app/src/main/res/values/strings.xml`.
2. `ConsentFragment.setupConsentLinks()` (in `onboarding/`) makes the two occurrences of "seguente link" clickable, pointing to `consent_privacy_info_url` and `consent_download_docs_url`; keep exactly two occurrences or update the fragment.
3. The privacy question `consent_question_privacy` embeds the link text `consent_link_privacy_policy`; the two strings must keep matching text.
4. Consent has two Yes/No questions, both required (DPO requirement); do not merge them.
