# CLAUDE.md - AI Assistant Guide for stilme-qe-app

This document is for AI assistants working on this codebase. For project description, setup and the annotated source tree, see `README.md`. Sources live under `app/src/main/java/com/aldogor/stilme_qe_app/` (packages `onboarding/`, `questionnaire/`, `network/`, `sync/`, `stl_guide/`, `update/`, `study/`).

> **IMPORTANT**: Keep documentation updated after completing significant tasks:
> - **CLAUDE.md**: architecture, code patterns, constraints
> - **README.md**: user-facing project description, setup instructions
> - **stilme_qe_data_dictionary.csv**: REDCap Data Dictionary, whenever questionnaire fields change

Step-by-step procedures (adding a scale or a monitored app, editing the consent text, enabling debug buttons and time travel) live in the project skills under `.claude/skills/stilme-*/`, not here.

## Conventions

These hold for everyone who works in this repository and for their Claude. Where authority lives is in *File Locations* below.

- **Git practice:** shared. Each piece of work goes on its own branch and reaches `master` through a pull request, which the repository owner merges; nobody commits to `master` directly. `public-master` is the publication branch described under *Repository layout*, not a feature branch. Nothing secret is committed: `local.properties` and `.env` stay local.
- **Language.** Documents, code comments and commit messages in English; the app's user-facing text in Italian, in `strings.xml`.
- **Journal.** `JOURNAL.md` gets one entry per working session that verifies or decides something, headed by its ISO date, with the data and sources behind each decision; the entry's last line names what it opened and what it closed (`Opened: ...; closed: ...`). The journal ends with one open list (`## Open`), edited in place, which is the state of the project. Consolidated decisions go into `docs/PROJECT_CONTEXT.md`; git is the changelog, so there is no CHANGELOG or TODO file.
- **Citations.** Inline author and year, hyperlinked to the DOI: `[Smith et al., 2023](https://doi.org/10.xxxx/yyyy)`; a source without a stable link is cited inline without one. Works in the library are identified by their key in `docs/literature/bibliography.csv`, and every citation is checked against its source before it enters a document.
- **Data.** `docs/data/raw/` is read-only; every transformation is a script, and processed data are tidy CSV with a codebook. A value without a source is written [n/d], never inferred.
- **Writing.** No dashes as punctuation (neither em nor en dashes, nor `--`): commas, colons, parentheses or separate sentences instead.

---

## Key Concepts

### Study Groups

Group assignment logic (in `ScoringEngine.calculateGroup()`):
1. If `using_time_limits == 1`, `INELIGIBLE_USING_STL` (code -1)
2. If `is_unito_student == 0` OR `age_group == 99`, `INELIGIBLE_SCREENING` (code -2)
3. If `interested_in_limit == 0`, `CONTROL` (code 1)
4. If `limit_method == 1`, `STL_INTERVENTION` (code 2)
5. If `limit_method == 2`, `PERSONAL_COMMITMENT` (code 3)

### Timepoints

`Timepoint` (in `study/StudyModels.kt`): T0 baseline, then T1 to T4 at 30, 60, 90 and 120 days from baseline. Each questionnaire window lasts 30 days. After T4 the study is complete.

### Scales and branching

Five scales (BSMAS, PHQ-9, GAD-7, FoMO, PSS-10). Only PSS-10 has reverse-scored items: 4, 5, 7 and 8 are scored as `4 - response` before summing. Conditional questions use `showIf = BranchingCondition(dependsOn, operator, value)` on `QuestionDefinition`.

---

## Code Patterns

### Encrypted Storage

All encrypted prefs go through `EncryptedPrefsFactory.create(context, fileName)`, which wraps `EncryptedSharedPreferences` with automatic recovery from Android Keystore corruption (`AEADBadTagException`).

**Instance caching**: `create()` caches one `SharedPreferences` per file name (creation is expensive and not thread-safe for concurrent first-time creation of the same file).

**Recovery chain**: first attempt, then catch `GeneralSecurityException`. Only if an `AEADBadTagException` is in the cause chain (true keystore corruption, data already unrecoverable) is the file deleted and recreated; a fallback to unencrypted `{fileName}_fallback` prevents a crash loop if recreation also fails. A **transient** `GeneralSecurityException` (e.g. a concurrent-creation race) retries **without deleting**, so valid study data is never destroyed by a non-corruption error.

Three encrypted prefs files exist:
- `stilme_qe_encrypted_prefs` (DataStorage): usage data
- `stilme_study_state` (StudyManager): participant state, timepoints, study ID (single source of truth)
- `stilme_secure_tokens` (SecureTokenManager): API token override

### Null-Safety Convention for Study State

`StudyManager` boolean accessors use `== true` (not `!= false`) so that a null `ParticipantState` returns `false`: `isEligible()`, `isQuestionnaireDue()`, `hasCompletedOnboarding()`, `isStudyComplete()` are all `false` when no state exists. This prevents showing study UI to users who haven't completed onboarding yet.

### REDCap Submission

Uses `RedcapResult` sealed class (`Success`, `NetworkError`, `ServerError`, `ParseError`). Failed submissions are queued in Room (`SubmissionQueue`) and retried by `SyncWorker`.

**SyncWorker behavior**: resets rows orphaned in `SUBMITTING` (worker killed mid-flight) back to `PENDING` at the start of each run, then processes all pending submissions (doesn't stop on first error). Submissions exceeding `MAX_RETRIES` (5) are marked `EXPIRED`; **the payload is kept, never deleted**, so questionnaire data stays recoverable. Only `NetworkError` triggers `Result.retry()`; `ServerError` and `ParseError` don't request immediate WorkManager retry.

**Permanent vs transient failures at submit time** (`QuestionnaireActivity`): a 4xx `ServerError` or `ParseError` is treated as permanent: the timepoint is **not** marked complete and the participant sees a real error (data stays queued for retry). Only `NetworkError` and 5xx `ServerError` mark the timepoint complete with an offline note. This prevents a deterministic rejection (bad token, Data Dictionary mismatch) from being shown as success and then silently lost.

**Missed windows**: `StudyManager.getActiveTimepoint()` / `Timepoint.earliestDue()` offer the earliest *incomplete* timepoint whose window has opened, so a skipped 30-day window can still be completed rather than being permanently lost.

---

## Important Constraints

### Permissions

All three are **mandatory**; `PermissionRecoveryActivity` blocks the app if any is revoked:
- `PACKAGE_USAGE_STATS`: social media usage data
- `POST_NOTIFICATIONS`: questionnaire reminders
- Battery Optimization Exemption: reliable background work

### Onboarding and consent

Onboarding is linear, with no back navigation on the key steps. The consent screen has two Yes/No questions, both required (DPO requirement); do not merge them.

### Privacy

- **No PII**: never collect name, email, phone, device ID
- **Anonymous IDs**: 16-char random alphanumeric (`StudyConfig.ID_CHARS`)
- **Encryption**: AES-256-GCM via `EncryptedPrefsFactory`
- **HTTPS only**: network security config blocks cleartext

### Italian Language

All user-facing text in `strings.xml`. Never hardcode Italian in Kotlin code.

---

## Build Configuration

Secrets and URLs (`REDCAP_API_TOKEN`, `REDCAP_API_URL`, `UPDATE_JSON_URL`) come from the git-ignored `local.properties` (template: `local.properties.example`) and are injected into `BuildConfig` at build time. Room annotation processing uses KSP, not KAPT (faster, Windows-compatible).

### Toolchain & CLI Workflow

The dev machine has no Android Studio: everything runs from the command line. Gradle builds, the Android CLI drives emulators and deploys.

- **JDK**: Gradle needs a JDK 17+ on `JAVA_HOME`; nothing bundles one any more. If `java` is not found, install one (`winget install EclipseAdoptium.Temurin.21.JDK`) and point the user-level `JAVA_HOME` at it, otherwise every `./gradlew` call dies with an invalid `JAVA_HOME`.
- **Android SDK**: `%LOCALAPPDATA%\Android\Sdk` (`ANDROID_HOME`), with `platform-tools` and `emulator` on PATH; platforms 33 to 36 and the API 36 system images are installed.
- **Android CLI**: the `android` command on PATH (WinGet shim, `winget install Google.AndroidCLI`) downloads the real binary into `%USERPROFILE%\.android\bin\android-cli.exe` on first run and keeps it updated. `android skills add <id>` installs Google's Android skills into the current project's `.claude/skills/` (gitignored there).
- **AVD**: `medium_phone` (API 36, google_apis_playstore x86_64), created with `android emulator create medium_phone`; the first `create` downloads the system image (about 1.5 GB) and takes several minutes.
- The debug APK is custom-named: `app/build/outputs/apk/debug/MIND-TIME.apk`.
- **R** (data analysis): 4.6.1 in `C:\Program Files\R\R-4.6.1`, its `bin` on the user PATH, so scripts run with `Rscript script.R`; the folder name carries the version, so the PATH entry changes with each R upgrade. Packages live only in the renv library of `docs/scripts/` (`renv::restore()` there on a new machine), with renv's global cache switched off: R started from the Claude desktop app resolves `%LOCALAPPDATA%` to the app's private folder (`AppData\Local\Packages\Claude_…\LocalCache\Local`), so a user library or cache written from a session would be invisible to R started anywhere else.

Emulator + deploy (`run` installs and launches in one step):

```powershell
android emulator list
android emulator start medium_phone
android run --apks=app/build/outputs/apk/debug/MIND-TIME.apk --activity=com.aldogor.stilme_qe_app.MainActivity
android emulator stop medium_phone
```

`emulator start` is meant to return once the device is ready, but the first boot of a fresh image can outlast its own wait: `adb shell getprop sys.boot_completed` printing `1` is the reliable signal.

> **⚠️ Known machine issue: Gradle "Unable to establish loopback connection"**. On this Windows machine, AF_UNIX sockets fail inside `%TEMP%` (the user's `AppData\Local\Temp`), which breaks Java NIO pipes (JDK ≥16 puts pipe socket files there). Reproduces on JDK 17 and JDK 21, from any launcher that inherits the user `TEMP`.
>   - Run Gradle with `$env:TMP = "C:\WINDOWS\TEMP"; $env:TEMP = "C:\WINDOWS\TEMP"` set first (or set the user-level `TMP`/`TEMP` variables to that directory once).
>
> Diagnosed 2026-07-05; root cause is directory-specific (likely a security-filter driver on the user Temp dir), machine works normally otherwise.

---

## Repository layout: internal and public

This repository is the **internal** one and holds the whole project: the app, `docs/` and `JOURNAL.md`. Its remote `origin` is the private GitHub repository `aldogor/stilme-qe-app-internal`. The **public** repository `aldogor/stilme-qe-app` (remote `public`) receives only a cut of it: everything except the paths listed in `public_exclude.txt`.

- Work reaches `master` on `origin` through pull requests, as *Conventions* says. **Never push `master` to `public`**: it carries the internal history.
- To publish the app: `python tools/publish_public.py` builds the cut from HEAD on the local branch `public-master` (one "Publish" commit per call, verified against the manifest), then `git push public public-master:master`. The hook `tools/pre-push` refuses any other push to `public`; install it once per clone with `cp tools/pre-push .git/hooks/pre-push`.
- A new internal file outside `docs/`, or a move of one, updates `public_exclude.txt` in the same commit.

## File Locations

| Content | Location |
|---|---|
| Research project docs (ethics/DPO, communication, article drafts) and dev specs/plans/reviews (`docs/superpowers/`) | `docs/`: tracked in the internal repository only (see *Repository layout*). `docs/PROJECT_CONTEXT.md` is the project briefing; `docs/drive/` mirrors the team's Google Drive folder and is gitignored |
| Project journal: dated record of verifications, intermediate decisions and what is open (the *why*; git holds *what changed*) | `JOURNAL.md`: update it, with the date, in every session that verifies or decides something; the open list at its end is the project's state. Internal only |
| Final study data (MedCap exports) | `docs/data/raw/`: **immutable**, read-only originals; cleaned data go to `docs/data/processed/` as tidy CSV with a codebook, and every transformation is a script. Both folders are gitignored, since pseudonymised health data never goes to GitHub, not even the internal repository; only the online survey's data dictionary and `codebook.csv`, which describe fields, are tracked, and `.gitattributes` keeps git from converting raw files, which are checked byte for byte. Exports reach colleagues through the team's folder on the university Google Drive, and `docs/data/README.md` describes how one is filed |
| Data analysis | Exploration may be done directly, with any tool. **Every result meant for the paper (numbers, tables, figures) is produced by an R script** in `docs/scripts/`, reading the data from `docs/data/`; a number found during exploration enters the paper only once an R script reproduces it. `docs/scripts/` is an renv project (packages in `DESCRIPTION` and `renv.lock`, no global cache): run R from that folder. `01_import_clean.R` turns the latest raw exports into the tidy tables of `docs/data/processed/` and flags, without excluding anyone, the cases the group has still to decide; the tests in `docs/scripts/tests/` run with `Rscript -e "testthat::test_dir('tests')"`. The research questions, and whether and how the data answer each, live only in `docs/PROJECT_CONTEXT.md`, referred to by their content, never by a number |
| Literature library for writing (index by citation key, PDF and Markdown text of each work) | `docs/literature/`, one flat folder, managed as aldogor-research's `references/literature.md` describes: `bibliography.csv` is the master record and the only file git tracks (`.gitignore`: `docs/literature/*`, `!docs/literature/bibliography.csv`); `<key>.pdf` and `<key>.md` stay local. The skill's `scripts/literature.py` adds, fetches, collects, rewrites Markdown, checks and exports BibTeX, and `check` runs before a change to the library is committed. The PDFs committed while the whole library was tracked remain in this repository's history; the public cut never carried them |
| Claude configuration and project skills | `.claude/settings.json` (tracked) is the repository's whole Claude configuration: the `aldogor-claude-plugins` and official marketplaces and the plugins the project uses, installed through the trust prompt. A `SessionStart` hook in the same file tells Claude, in a clone of the internal repository, when the local setup is incomplete (no R packages, no MedCap export or no prepared tables), and Claude then offers to guide the person through it with the `stilme-setup` skill. `.claude/skills/stilme-*/` holds the project's procedures for AI assistants; `stilme-setup` is internal and left out of the public cut. `settings.local.json` keeps only the permissions Claude records there; it, `CLAUDE.local.md` and third-party skills stay local |
| REDCap Data Dictionary | `stilme_qe_data_dictionary.csv` |
| Update distribution | `update-info.json` (git-ignored, uploaded to Google Drive together with the APK) |
