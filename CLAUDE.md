# CLAUDE.md - AI Assistant Guide for STILME-QE-APP

This document is for AI assistants working on this codebase. For project description and setup, see `README.md`.

> **IMPORTANT**: Keep documentation updated after completing significant tasks:
> - **CLAUDE.md** - Architecture, code patterns, constraints
> - **README.md** - User-facing project description, setup instructions
> - **STILME_QE_DataDictionary.csv** - Data Dictionary when questionnaire fields change

---

## Key Characteristics

| Property | Value |
|----------|-------|
| **Language** | Kotlin (100%) |
| **Min SDK** | 29 (Android 10) |
| **Target/Compile SDK** | 36 (Android 16) |
| **Java Version** | 11 |
| **Architecture** | MVVM with Repository pattern |
| **UI Language** | Italian only (all strings in `strings.xml`) |
| **Package Name** | `com.aldogor.stilme_qe_app` |

---

## Project Structure

```
app/src/main/java/com/aldogor/stilme_qe_app/
├── MainActivity.kt              # Main screen: status, questionnaire card, group instructions,
│                                # invite friends, contact, withdrawal
├── Models.kt                    # Usage data models: DailyUsage, AppUsageData, AppConfig
│                                # Contains MONITORED_APPS list (11 social media apps)
├── DataStorage.kt               # Encrypted usage data storage, CSV export, data merging
├── EncryptedPrefsFactory.kt     # Factory for EncryptedSharedPreferences with keystore
│                                # corruption recovery (catches AEADBadTagException)
├── BackgroundWork.kt            # WorkManager scheduler for weekly background data collection
├── PermissionHelper.kt          # Runtime permission checks: UsageStats, Notifications, Battery
├── PermissionRecoveryActivity.kt # Blocks app when permissions are revoked after onboarding
├── NotificationHelper.kt        # Questionnaire reminders with escalating urgency
│
├── onboarding/                  # First-run flow (linear, no back navigation on key steps)
│   ├── OnboardingActivity.kt    # Fragment container for onboarding steps
│   ├── WelcomeFragment.kt       # Study introduction with external link
│   ├── ConsentFragment.kt       # Informed consent with 2 Yes/No questions (DPO requirement)
│   │                            # Contains clickable links to privacy policy and documents
│   ├── PermissionFragment.kt    # Requests UsageStats + Notifications + Battery (all mandatory)
│   └── IneligibleFragment.kt    # Personalized ineligibility messages
│
├── questionnaire/               # In-app surveys (T0 baseline + T1-T4 monthly)
│   ├── QuestionnaireActivity.kt # Scrollable questionnaire with sticky progress bar
│   │                            # Handles early eligibility screening and data submission
│   ├── QuestionAdapter.kt       # RecyclerView adapter supporting: RADIO, YESNO, TEXT, SCALE
│   │                            # Implements branching logic (showIf conditions)
│   ├── ScoringEngine.kt         # Scale scoring (including PSS-10 reverse) and group assignment
│   └── QuestionnaireData.kt     # All question definitions for T0 and monthly questionnaires
│
├── network/                     # REDCap API layer
│   └── RedcapApiService.kt      # Retrofit interface, SecureTokenManager, RedcapRepository
│
├── sync/                        # Background sync and offline support
│   ├── SubmissionQueue.kt       # Room database for queuing failed submissions
│   └── SyncWorker.kt            # Periodic sync worker + questionnaire reminder worker
│
├── stl_guide/                   # Group-specific intervention guidance
│   ├── GroupInstructionsFragment.kt  # Instructions for all 3 groups
│   └── GroupInstructionsActivity.kt  # Standalone activity for viewing instructions
│
├── update/                      # In-app update mechanism
│   └── UpdateChecker.kt         # Checks Google Drive JSON for updates, throttled to once/day
│
└── study/                       # Study state management
    ├── StudyModels.kt           # ParticipantState, Timepoint, StudyGroup, StudyConfig,
    │                            # ConsentData, ScaleScores, QuestionnaireResponses
    ├── StudyManager.kt          # Participant state, timepoint calculations, debug time travel
    └── ThankYouActivity.kt      # Study completion screen with counseling link
```

---

## Key Concepts

### Study Groups

Group assignment logic (in `ScoringEngine.calculateGroup()`):
1. If `using_time_limits == 1` → `INELIGIBLE_USING_STL` (code -1)
2. If `is_unito_student == 0` OR `age_group == 99` → `INELIGIBLE_SCREENING` (code -2)
3. If `interested_in_limit == 0` → `CONTROL` (code 1)
4. If `limit_method == 1` → `STL_INTERVENTION` (code 2)
5. If `limit_method == 2` → `PERSONAL_COMMITMENT` (code 3)

### Timepoints

```kotlin
enum class Timepoint(val index: Int, val daysFromBaseline: Int, val displayName: String) {
    T0(0, 0, "Baseline"),
    T1(1, 30, "Follow-up 1 mese"),
    T2(2, 60, "Follow-up 2 mesi"),
    T3(3, 90, "Follow-up 3 mesi"),
    T4(4, 120, "Follow-up finale")
}
```

Each questionnaire window is 30 days. After T4, the study is complete.

### Ineligibility Reasons

| Reason | Variable Check | String resource |
|--------|----------------|-----------------|
| Not UniTO student | `is_unito_student == 0` | `ineligible_reason_not_unito` |
| Over 30 years old | `age_group == 99` | `ineligible_reason_age` |
| Already using STL | `using_time_limits == 1` | `ineligible_reason_using_stl` |

### Psychometric Scales

| Scale | Items | Range | Reverse Items |
|-------|-------|-------|---------------|
| BSMAS | 6 | 6-30 | None |
| PHQ-9 | 9 | 0-27 | None |
| GAD-7 | 7 | 0-21 | None |
| FoMO | 10 | 10-50 | None |
| PSS-10 | 10 | 0-40 | Items 4,5,7,8 |

**PSS-10 Reverse Scoring**: Items 4, 5, 7, and 8 are reverse-scored (4 - response) before summing.

### Branching Logic

Supported operators: `EQUALS`, `NOT_EQUALS`, `IN`, `NOT_IN`, `GREATER_THAN`, `LESS_THAN`

```kotlin
QuestionDefinition(
    variableName = "followup_question",
    showIf = BranchingCondition(
        dependsOn = "parent_question",
        operator = BranchingOperator.EQUALS,
        value = 1
    )
)
```

---

## Common Development Tasks

### Adding a New Psychometric Scale

1. **Define questions** in `QuestionnaireData.kt` using `QuestionDefinition` with `QuestionType.SCALE`, `scaleGroup`, and `scaleInstructions`
2. **Add scoring function** in `ScoringEngine.kt` (handle reverse items if needed)
3. **Update `ScaleScores`** data class in `StudyModels.kt`
4. **Add to `calculateScores()`** and include in REDCap payload in `QuestionnaireActivity.kt`
5. **Update `STILME_QE_DataDictionary.csv`** with new variables

### Adding a New Monitored App

Add to `AppConfig.MONITORED_APPS` in `Models.kt`:
```kotlin
"com.newapp.package" to "New App Name"
```

### Updating Consent Text

1. Edit `consent_full_text` in `res/values/strings.xml`
2. If link positions change, verify `ConsentFragment.setupConsentLinks()` finds "seguente link" texts
3. Update URLs if needed: `consent_privacy_info_url`, `consent_download_docs_url`
4. The privacy question has a clickable link text defined by `consent_link_privacy_policy` in `strings.xml` — if changed, update both the string resource and `consent_question_privacy` to match

---

## Code Patterns

### Encrypted Storage

All encrypted prefs use `EncryptedPrefsFactory.create()` which wraps `EncryptedSharedPreferences` with automatic recovery from Android Keystore corruption (`AEADBadTagException`):

```kotlin
private val prefs = EncryptedPrefsFactory.create(context, PREFS_FILE_NAME)
```

**Recovery chain**: First attempt → catch `GeneralSecurityException` → delete corrupted file → retry → if retry also fails → fall back to unencrypted `SharedPreferences` (`{fileName}_fallback`) to prevent crash loops.

Three encrypted prefs files exist:
- `stilme_qe_encrypted_prefs` (DataStorage) — usage data, study ID
- `stilme_study_state` (StudyManager) — participant state, timepoints
- `stilme_secure_tokens` (SecureTokenManager) — API token override

### Null-Safety Convention for Study State

`StudyManager` boolean accessors use `== true` (not `!= false`) so that a null `ParticipantState` returns `false`:
- `isEligible()` → `false` when no state exists
- `isQuestionnaireDue()` → `false` when no state exists
- `hasCompletedOnboarding()` → `false` when no state exists
- `isStudyComplete()` → `false` when no state exists

This prevents showing study UI to users who haven't completed onboarding yet.

### REDCap Submission

Uses `RedcapResult` sealed class (`Success`, `NetworkError`, `ServerError`, `ParseError`). Failed submissions are queued in Room (`SubmissionQueue`) and retried by `SyncWorker`.

**SyncWorker behavior**: Processes all pending submissions in a single run (doesn't stop on first error). Submissions exceeding `MAX_RETRIES` (5) are deleted from the queue. Only `NetworkError` triggers `Result.retry()`; `ServerError` and `ParseError` are considered final failures (retried on next periodic run but don't request immediate WorkManager retry).

---

## Important Constraints

### Permissions

All three are **mandatory** — `PermissionRecoveryActivity` blocks the app if any is revoked:
- `PACKAGE_USAGE_STATS` — social media usage data
- `POST_NOTIFICATIONS` — questionnaire reminders
- Battery Optimization Exemption — reliable background work

### Privacy

- **No PII**: Never collect name, email, phone, device ID
- **Anonymous IDs**: 16-char random alphanumeric (`StudyConfig.ID_CHARS`)
- **Encryption**: AES-256-GCM via `EncryptedPrefsFactory`
- **HTTPS only**: Network security config blocks cleartext

### Italian Language

All user-facing text in `strings.xml`. Never hardcode Italian in Kotlin code.

### REDCap Integration

| Setting | Value |
|---------|-------|
| API endpoint | `BuildConfig.REDCAP_API_URL` (from `local.properties`) |
| API token | `BuildConfig.REDCAP_API_TOKEN` (from `local.properties`) |
| Format | JSON |
| Offline support | Room-based queue with auto-retry |
| Data Dictionary | `STILME_QE_DataDictionary.csv` |

---

## Debugging

### Debug Day Offset (Time Travel)

Debug buttons are **hidden by default** (`debug_buttons_container` has `visibility="gone"` in `activity_main.xml`). To enable:
- Set `android:visibility="visible"` in XML, or
- Uncomment the `BuildConfig.DEBUG` block in `MainActivity.setupUI()`

**What it affects**: `isQuestionnaireDue()`, `getCurrentTimepoint()`, `getDaysSinceWindowOpened()`, `getDaysUntilWindowCloses()`, UI status messages

**What it does NOT affect**: Background data collection, UsageStats dates, REDCap timestamps, `lastCompletionDateString`, WorkManager scheduling

**Testing notifications after time jump**: WorkManager uses real time, so press "Test notifica" button to trigger the notification check with the simulated date.

### Clear All App State

- Uninstall and reinstall, or
- Settings > Apps > MIND TIME > Clear Data, or
- `studyManager.clearAllData()`

---

## Build Configuration

### Local Properties

Copy `local.properties.example` to `local.properties`:

```properties
sdk.dir=/path/to/your/android/sdk
REDCAP_API_TOKEN=YOUR_TOKEN_HERE
REDCAP_API_URL=https://www.medcap.unito.it/redcap/api/
UPDATE_JSON_URL=https://drive.google.com/uc?export=download&id=YOUR_JSON_FILE_ID
```

These are injected into `BuildConfig` at build time. `local.properties` is git-ignored.

### Build Tools

| Tool | Version |
|------|---------|
| Android Gradle Plugin (AGP) | 8.13.2 |
| Kotlin | 2.2.21 |
| KSP | 2.2.21-RC2-2.0.4 |

KSP is used instead of KAPT for Room annotation processing (faster, Windows-compatible).

### R8/ProGuard

Release builds use R8 with rules in `app/proguard-rules.pro`. Key keep rules: Retrofit interfaces, Gson `@SerializedName` fields, Room entities, Tink/AndroidX Security Crypto, WorkManager workers, Kotlin enums.

### In-App Updates

Checks a JSON file hosted on Google Drive (throttled to once/day). The JSON contains `version` and `apk_url`. Both the JSON file and the APK are hosted on Google Drive. Configure `UPDATE_JSON_URL` in `local.properties`.

### Unit Tests

Run with `./gradlew test`:
- `ScoringEngineTest.kt` — All 5 scales, PSS-10 reverse scoring, group assignment (5 paths + error)
- `TimepointTest.kt` — Boundary conditions, REDCap event names

---

## File Locations

| Content | Location |
|---------|----------|
| All strings (Italian) | `res/values/strings.xml` |
| Layouts | `res/layout/*.xml` |
| Themes | `res/values/themes.xml`, `res/values-night/themes.xml` |
| Colors | `res/values/colors.xml` |
| Network security | `res/xml/network_security_config.xml` |
| Build config | `app/build.gradle.kts` |
| Dependencies | `gradle/libs.versions.toml` |
| ProGuard rules | `app/proguard-rules.pro` |
| Local config template | `local.properties.example` |
| Update distribution | `update-info.json` (git-ignored, uploaded to Google Drive) |
| REDCap Data Dictionary | `STILME_QE_DataDictionary.csv` |
| Unit tests | `app/src/test/java/com/aldogor/stilme_qe_app/` |

---

## Contact

| Type | Contact |
|------|---------|
| Research Email | progettoscreentime.dsspp@unito.it |
| Developer | Aldo Gorga MD |
| Institution | University of Turin, Department of Public Health Sciences and Pediatrics |
