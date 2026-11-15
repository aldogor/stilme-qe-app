# CLAUDE.md

This repository holds the Android app (Kotlin) that collected the data of MIND-TIME (internal name STILME-QE), a quasi-experimental study on screen time limits among University of Turin students, and the study's research side under `docs/`. Data collection is over: participants keep version 2.1 (tag `v2.1`), no build goes to them, and changes to the code serve whoever reuses it. The current work is the data analysis in R. This is the internal, private repository (`origin` is `aldogor/stilme-qe-app-internal`); the public `aldogor/stilme-qe-app` receives only what `publish.txt` lists.

## Where authority lives

| File or folder | What it holds, and when it changes |
|---|---|
| `README.md` | The app for developers: features, source tree, build, emulator and REDCap setup. Public. It changes with the app or its setup |
| `docs/PROJECT_CONTEXT.md` | The study briefing with its consolidated decisions, and the single source of the research questions, which are referred to by their content, never by a number |
| `docs/analysis-plan.md` | The statistical analysis plan: an analysis follows it or records its deviation there |
| `JOURNAL.md` | Dated record of verifications, intermediate decisions and what is open (the *why*; git holds *what changed*); its closing "Open" list is the project's state |
| `docs/README.md` | Entry point to the research side for colleagues |
| `docs/data/` | `raw/` holds the MedCap exports and `processed/` the tables prepared from them. Git tracks only `README.md` (each export's SHA-256 and how a new export is filed), the online survey's data dictionary and `processed/codebook.csv` |
| `docs/scripts/` | The R analysis, an renv project run from that folder, with its tests in `tests/`. `01_import_clean.R` prepares the tables and flags, without excluding anyone, the cases the group has still to decide |
| `docs/literature/` | `bibliography.json` (CSL JSON) is the record and the only tracked file; the texts stay local. Managed with `literature.py` of aldogor-research, whose `check` runs before a change to the library is committed; when the script finds no library, the plugin is updated (aldogor-core 0.17.0 or later) and no new library is created |
| `docs/drive/` | Local copy of the team's Google Drive folder, gitignored; Drive is the original |
| `archive/` | Dated documents of completed development work |
| `stilme_qe_data_dictionary.csv` | The REDCap data dictionary the app writes to; it changes with every questionnaire field |
| `publish.txt` | The include-list of the public version: a new file meant for it, or a move of a published one, updates the list in the same commit |
| `.claude/` | `settings.json` is the repository's whole Claude configuration: the marketplaces, the plugins and the hook that offers the setup in an incomplete clone. `skills/stilme-*/` hold the procedures: `stilme-setup` (a colleague's clone, internal), `stilme-questionnaire-changes`, `stilme-debug-tools` |
| `local.properties` | Git-ignored secrets and URLs (`REDCAP_API_TOKEN`, `REDCAP_API_URL`, `UPDATE_JSON_URL`), injected into `BuildConfig` at build time; the template is `local.properties.example`. `update-info.json`, also git-ignored, goes to Google Drive with the APK |

## Rules easiest to break

- **Raw data are immutable.** Nothing under `docs/data/raw/` is edited, opened and saved again, or staged: pseudonymised health data never reach GitHub, this repository included. A hash that fails means a damaged copy, and the recorded hash stays as it is.
- **Numbers for a paper come from R scripts** in `docs/scripts/`, reading `docs/data/`. Exploration may use any tool; a number found that way enters a paper only once a script reproduces it.
- **No personal data.** The app collects no name, email, phone number or device ID; study IDs are 16 random alphanumeric characters (`StudyConfig.ID_CHARS`); the network configuration blocks cleartext traffic.
- **The consent screen keeps its two Yes/No questions**, both required (DPO requirement), and they are never merged. Onboarding is linear, with no back navigation on the key steps.
- **User-facing text is Italian and lives in `strings.xml`**, never in Kotlin code.
- **The public repository changes only through publication.** Nobody works in its clone, the sibling folder `../stilme-qe-app-public`. The owner runs the publish script of the aldogor-share skill (`publish.py --check` lists what would go and writes nothing; without it the script builds the public tree from HEAD and commits it in the clone) and pushes that clone.
- **Nothing secret is committed**: `local.properties` and `.env` stay local.

## Conventions

These hold for everyone who works in this repository and for their Claude.

- **Git practice:** shared. Each piece of work goes on its own branch and reaches `main` through a pull request, which the repository owner merges; nobody commits to `main` directly.
- **Tests** run before a pull request: `./gradlew test` for the app, and `Rscript -e "testthat::test_dir('tests')"` in `docs/scripts/` for the analysis. Tests ship with the code they cover.
- **Screens.** A change to a screen starts from a list of what the screen does today and ends with the owner's walk through the running app against that list.
- **Language.** Documents, code comments and commit messages in English; the app's user-facing text in Italian.
- **Journal.** `JOURNAL.md` gets one entry per working session that verifies or decides something, headed by its ISO date, with the data and sources behind each decision; the entry's last line names what it opened and what it closed (`Opened: ...; closed: ...`). The journal ends with one open list (`## Open`), edited in place. Consolidated decisions go into `docs/PROJECT_CONTEXT.md`; git is the changelog, so there is no CHANGELOG or TODO file.
- **Citations.** Inline author and year, hyperlinked to the DOI: `[Smith et al., 2023](https://doi.org/10.xxxx/yyyy)`; a source without a stable link is cited inline without one. Works in the library are identified by their key in `docs/literature/bibliography.json`, and every citation is checked against its source before it enters a document.
- **Data.** Every transformation is a script, and processed data are tidy CSV with a codebook. A value without a source is written [n/d], never inferred.
- **Writing.** No dashes as punctuation (neither em nor en dashes, nor `--`): commas, colons, parentheses or separate sentences instead.

## The app

### Study logic

Group assignment, in `ScoringEngine.calculateGroup()`:

1. `using_time_limits == 1`: `INELIGIBLE_USING_STL` (code -1)
2. `is_unito_student == 0` or `age_group == 99`: `INELIGIBLE_SCREENING` (code -2)
3. `interested_in_limit == 0`: `CONTROL` (code 1)
4. `limit_method == 1`: `STL_INTERVENTION` (code 2)
5. `limit_method == 2`: `PERSONAL_COMMITMENT` (code 3)

`Timepoint` (in `study/StudyModels.kt`): T0 baseline, then T1 to T4 at 30, 60, 90 and 120 days from baseline, each with a window of 30 days; after T4 the study is complete. `StudyManager.getActiveTimepoint()` and `Timepoint.earliestDue()` offer the earliest incomplete timepoint whose window has opened, so a skipped window can still be completed.

Five scales (BSMAS, PHQ-9, GAD-7, FoMO, PSS-10). Only the PSS-10 has reverse-scored items: 4, 5, 7 and 8 are scored as `4 - response` before summing. Conditional questions use `showIf = BranchingCondition(dependsOn, operator, value)` on `QuestionDefinition`.

### Code patterns

**Encrypted storage.** All encrypted prefs go through `EncryptedPrefsFactory.create(context, fileName)`, which wraps `EncryptedSharedPreferences` (AES-256-GCM) and caches one instance per file name. Three files exist: `stilme_qe_encrypted_prefs` (`DataStorage`, usage data), `stilme_study_state` (`StudyManager`, the single source of the participant's state and study ID) and `stilme_secure_tokens` (`SecureTokenManager`, API token override). The factory deletes and recreates a file only when an `AEADBadTagException` is in the cause chain (keystore corruption, data already unrecoverable), with an unencrypted `{fileName}_fallback` if recreation also fails; any other security error retries without deleting, so valid study data survive a transient failure.

**Null safety of the study state.** The boolean accessors of `StudyManager` (`isEligible()`, `isQuestionnaireDue()`, `hasCompletedOnboarding()`, `isStudyComplete()`) compare with `== true`, so a null `ParticipantState` gives `false` and no study screen appears before onboarding.

**REDCap submission.** Results are a `RedcapResult` sealed class (`Success`, `NetworkError`, `ServerError`, `ParseError`). A failed submission is queued in Room (`SubmissionQueue`) and retried by `SyncWorker`, which first resets the rows left in `SUBMITTING` by a killed worker, then processes every pending row without stopping at the first error. After `MAX_RETRIES` (5) a row becomes `EXPIRED` and keeps its payload, so the questionnaire stays recoverable. Only a `NetworkError` asks WorkManager for a retry. At submit time (`QuestionnaireActivity`) a 4xx `ServerError` or a `ParseError` is permanent: the timepoint is not marked complete and the participant sees an error. A `NetworkError` or a 5xx marks it complete with an offline note.

**Permissions.** `PACKAGE_USAGE_STATS`, `POST_NOTIFICATIONS` and the battery optimization exemption are all mandatory, and `PermissionRecoveryActivity` blocks the app while one is revoked.

### Build

Plain Gradle from the command line; the prerequisites and the emulator commands are in `README.md`. Room's annotation processing uses KSP. The debug APK is `app/build/outputs/apk/debug/MIND-TIME.apk`.
