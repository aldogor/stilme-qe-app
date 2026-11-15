# stilme-qe-app (Studio MIND TIME)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-29-blue.svg)](https://developer.android.com/about/versions/10)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-blue.svg)](https://developer.android.com/about/versions/16)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.2-purple.svg)](https://kotlinlang.org/)

A comprehensive Android application for a quasi-experimental research study investigating whether Screen Time Limits (STL) can help reduce social media usage and improve mental health among university students.

---

## Overview

**stilme-qe-app** (publicly known as **Studio MIND TIME**) is a research tool developed for the University of Turin's study *"Gli Screen Time Limits contribuiscono a ridurre l'uso dei social media e a migliorare la salute mentale?"*.

The application serves as a complete research platform that:
- Passively monitors social media usage across 11 platforms
- Administers validated psychometric questionnaires
- Assigns participants to study groups based on their preferences
- Submits all data securely to the university's REDCap server
- Provides personalized instructions for each study group

### Study Design

| Aspect | Details |
|--------|---------|
| **Type** | Quasi-experimental longitudinal study with 3 parallel arms |
| **Duration** | 4 months (baseline T0 + monthly follow-ups T1-T4) |
| **Population** | University of Turin students aged 18-30 |
| **Sample Size** | 311 planned (104 per group) |

### Study Groups

Participants are assigned to groups based on their interest in reducing social media use:

| Group | Name | Description | Intervention |
|-------|------|-------------|--------------|
| **1** | Control | Not interested in reducing use | None - just complete questionnaires |
| **2** | STL Intervention | Wants to reduce using tools | Set a 30-minute daily timer on each social media app (PDF guide available) |
| **3** | Personal Commitment | Wants to reduce without tools | Stay within 30 minutes a day in total, through willpower alone |

Additionally, two ineligibility categories exist:
- **Already using STL**: Participants who already use Screen Time Limits cannot join
- **Failed screening**: Not a UniTO student or over 30 years old

### Version used in the study

Data collection ended in 2026. Participants used version 2.1, tagged `v2.1`, apart from a few baseline questionnaires sent from version 2.0, whose questionnaire and scoring are the same. The commits after the tag correct defects found by a code review in July 2026 and were never distributed, so the tag is the code to read next to the study's data. The defects of version 2.1 that can show in the data:

- a monthly questionnaire whose 30-day window was skipped was never offered again;
- with the usage-access permission revoked, the background collection stored zero minutes and zero opens as real usage;
- a submission rejected by the server was shown as sent and dropped after five retries;
- the update check could not read its JSON file, so no update could be announced.

---

## Key Features

### Privacy & Security

The app was designed with privacy as a core principle:

| Feature | Implementation |
|---------|----------------|
| **Encryption** | AES-256-GCM for the stored usage data and the study state |
| **Anonymity** | 16-character random alphanumeric IDs |
| **No PII** | Never collects name, email, phone, or device ID |
| **Secure transmission** | HTTPS-only communication with REDCap |
| **Data minimization** | Only collects what's necessary for research |

### Usage Monitoring

The app tracks usage across 11 major social media platforms using Android's UsageStats API:

- Instagram, TikTok, Facebook, YouTube
- Pinterest, Twitch, BeReal, Snapchat
- X (Twitter), LinkedIn, Reddit

**Data collected for each app:**
- Daily screen time (minutes)
- Number of times opened per day

**Collection schedule:**
- Background collection every 7 days
- Fresh data collected at each questionnaire submission

**How usage is measured:** minutes come from Android's daily usage statistics (`UsageStatsManager.queryUsageStats` with the daily interval), which the system aggregates in blocks of about 24 hours that need not start at midnight; the app assigns each block to the date on which it begins. Opens are `ACTIVITY_RESUMED` events from the event log, each dated by its own time, so a change of screen inside an app counts as an open. The minutes and the opens of one date can therefore refer to different 24 hours: sums over weeks or months are reliable, single days less so. At each collection the app re-reads the last 9 days and keeps the larger value.

### In-App Questionnaires

The app delivers validated psychometric instruments:

| Scale | Full Name | Items | Purpose |
|-------|-----------|-------|---------|
| **BSMAS** | Bergen Social Media Addiction Scale | 6 | Measures problematic social media use |
| **PHQ-9** | Patient Health Questionnaire | 9 | Screens for depression severity |
| **GAD-7** | Generalized Anxiety Disorder | 7 | Screens for anxiety severity |
| **FoMO** | Fear of Missing Out Scale | 10 | Measures fear of missing out |
| **PSS-10** | Perceived Stress Scale | 10 | Measures perceived stress levels |

**Questionnaire features:**
- Scrollable interface with sticky progress bar
- Automatic scoring with reverse-scoring support (PSS-10 items 4, 5, 7, 8)
- Branching logic for conditional questions
- Scale grids for efficient Likert responses
- Offline queue for reliable submission

### Smart Notifications

The app sends reminder notifications when questionnaires are due:

| Phase | Timing | Message Tone |
|-------|--------|--------------|
| Initial | Window opens | Friendly invitation |
| Reminder | Few days later | Gentle nudge |
| Urgent | 7+ days into window | More pressing |
| Invite | After completion | Share the study |

### REDCap Integration

All data is submitted to the University of Turin's REDCap server:

- **Direct API submission** via Retrofit/OkHttp
- **Offline queue** with automatic retry when network returns
- **Longitudinal events** support (baseline + 4 follow-ups)
- **Tombstone withdrawal** preserves audit trail while deleting study data

---

## Architecture

### Technical Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Language** | Kotlin 2.2 | Modern Android development |
| **Architecture** | MVVM | Clean separation of concerns |
| **Networking** | Retrofit 3.0 + OkHttp 5.3 | REDCap API communication |
| **Database** | Room 2.8 | Offline submission queue |
| **Background** | WorkManager 2.11 | Scheduled data collection |
| **Storage** | EncryptedSharedPreferences | Secure local storage |
| **Navigation** | AndroidX Navigation 2.9 | Fragment management |

### Project Structure

```
app/src/main/java/com/aldogor/stilme_qe_app/
├── MainActivity.kt                    # Main screen with status, questionnaire, and actions
├── Models.kt                          # Usage data models and monitored apps list
├── DataStorage.kt                     # Encrypted storage for usage data
├── EncryptedPrefsFactory.kt           # Encrypted prefs with keystore corruption recovery
├── BackgroundWork.kt                  # WorkManager for weekly collection
├── PermissionHelper.kt                # Runtime permission handling
├── PermissionRecoveryActivity.kt      # Shown when permissions are revoked
├── NotificationHelper.kt              # Questionnaire reminder notifications
│
├── onboarding/                        # First-run experience
│   ├── OnboardingActivity.kt          # Container for onboarding fragments
│   ├── WelcomeFragment.kt             # Study introduction with external link
│   ├── ConsentFragment.kt             # Full informed consent with clickable links
│   ├── PermissionFragment.kt          # Request usage stats and notifications
│   └── IneligibleFragment.kt          # Personalized ineligibility messages
│
├── questionnaire/                     # Survey administration
│   ├── QuestionnaireActivity.kt       # Scrollable questionnaire with progress
│   ├── QuestionAdapter.kt             # RecyclerView adapter with branching
│   ├── ScoringEngine.kt               # Scale calculations and group assignment
│   └── QuestionnaireData.kt           # All question definitions
│
├── network/                           # API layer
│   └── RedcapApiService.kt            # Retrofit interface and repository
│
├── sync/                              # Background operations
│   ├── SubmissionQueue.kt             # Room database for offline queue
│   └── SyncWorker.kt                  # Background sync and notifications
│
├── stl_guide/                         # Intervention guidance
│   ├── GroupInstructionsFragment.kt   # Group-specific instructions
│   └── GroupInstructionsActivity.kt   # Standalone activity for instructions
│
├── update/                            # In-app update mechanism
│   └── UpdateChecker.kt              # Checks Google Drive JSON for new versions
│
└── study/                             # Study state management
    ├── StudyModels.kt                 # Data classes for study state
    ├── StudyManager.kt                # Central state management
    └── ThankYouActivity.kt            # Study completion screen
```

---

## Quick Start

### For Research Participants

1. **Install** the MIND TIME app from the provided link (Android 10+)
2. **Complete onboarding**:
   - Read and accept the informed consent form
   - Grant usage statistics and notification permissions
   - Complete the baseline questionnaire (~8-10 minutes)
3. **Follow your group's instructions**:
   - **Group 2**: Set up Screen Time Limits (30 minutes on each app) as shown in the app
   - **Group 1 & 3**: No setup required
4. **Complete monthly questionnaires** when notified (~5 minutes each)
5. **Keep the app installed** for the full 4 months of the study

### For Developers

#### Prerequisites

The build is plain Gradle, so it runs from any shell; Android Studio works too but is not required.

- A **JDK 17+** on `JAVA_HOME` (for example [Eclipse Temurin](https://adoptium.net/) 21)
- **Android SDK** with `platform-tools` and platform 36 (`ANDROID_HOME` set)
- **Android CLI** (`winget install Google.AndroidCLI`, command `android`) for creating and starting emulators and deploying APKs from the terminal; optional if you use Studio or raw `adb`
- Kotlin 2.2+ (provided by the Gradle build, no separate install)

#### Setup

```bash
# Clone the repository
git clone https://github.com/aldogor/stilme-qe-app.git
cd stilme-qe-app

# Build the project
./gradlew build

# Run unit tests
./gradlew test

# Install on a connected device or running emulator
./gradlew installDebug
```

With the Android CLI, an emulator is created once (`android emulator create medium_phone`, which downloads the system image) and then started, and `run` installs and launches the debug APK in one step:

```bash
android emulator start medium_phone
android run --apks=app/build/outputs/apk/debug/MIND-TIME.apk --activity=com.aldogor.stilme_qe_app.MainActivity
android emulator stop medium_phone
```

The first boot of a fresh image can outlast the wait of `emulator start`: `adb shell getprop sys.boot_completed` printing `1` is the reliable signal.

> **Note:** this public repository holds the app. The research-project documents (ethics approvals, communication material, article drafts, literature) are kept in the project's private repository and left out of this one.

#### REDCap Configuration

1. **Create a longitudinal REDCap project** with 5 events:
   - `baseline_arm_1` (T0)
   - `followup_1_arm_1` through `followup_4_arm_1` (T1-T4)

2. **Import the Data Dictionary**:
   - Navigate to **Project Setup** > **Data Dictionary** > **Upload Data Dictionary**
   - Upload `stilme_qe_data_dictionary.csv` from the repository root

3. **Designate instruments to events**:
   - `start_q` → `baseline_arm_1` only
   - `month_q` → all four `followup_X_arm_1` events

4. **Generate API token** with Import/Export/Delete permissions

5. **Configure the app** by adding your token to `local.properties`:
   ```properties
   REDCAP_API_TOKEN=YOUR_32_CHARACTER_TOKEN
   REDCAP_API_URL=https://www.medcap.unito.it/redcap/api/
   ```
   These values are injected into `BuildConfig` at build time. See `local.properties.example` for all available settings.

### Working in this repository with Claude

The repository carries its Claude Code configuration in `.claude/settings.json` and its conventions in `CLAUDE.md`.

1. Install git and the GitHub CLI (on Windows `winget install Git.Git` and `winget install GitHub.cli`; on macOS `brew install git gh`) and sign in with `gh auth login`.
2. Clone the repository, open Claude Code in it and accept the trust prompt: it registers the `aldogor-claude-plugins` marketplace and installs the plugins the project uses (aldogor-core, security-guidance).
3. The conventions (git practice, language, journal, citations, data rules) are in `CLAUDE.md`, which Claude reads at the start of every session. In a fresh clone of the project's internal repository, Claude notices that the local data and packages are missing and offers to guide the setup step by step.

Each person's Claude creates a branch for each piece of work and opens a pull request, and the repository owner merges it. GitHub Free does not enforce reviews on a private repository, so the rule rests on `CLAUDE.md`.

---

## Data Collection

### Usage Data

| Aspect | Details |
|--------|---------|
| **Collection** | Weekly background sync + fresh data at questionnaire |
| **Metrics** | Screen time (minutes) + app opens per day |
| **Retention** | Up to 140 days (full study duration) |
| **Format** | JSON blob submitted to REDCap |

### Questionnaire Schedule

| Timepoint | Form | Content | Est. Duration |
|-----------|------|---------|---------------|
| **T0** | Baseline | Demographics, screening, all 5 scales | 8-10 min |
| **T1-T3** | Monthly | Academic status + all 5 scales | 5-8 min |
| **T4** | Final | Monthly content + exit questions | 5-8 min |

---

## User Interface

### Main Screen Components

After completing onboarding, the main screen displays:

- **Status Card**: Current study status and next steps
- **Progress Indicator**: Visual timeline showing T0 through M4
- **Questionnaire Card**: Shown when a questionnaire is due
- **Group Instructions Card**: Quick access to your group's instructions
- **Invite Friends Card**: Share the study with other students
- **More Info Link**: External link to study website
- **Contact Section**: Research team email for questions
- **Withdrawal Option**: Ability to leave the study (with confirmation)

---

## Security & Privacy

### Data Protection

| Data Type | Storage Method | Encryption |
|-----------|----------------|------------|
| Study ID | EncryptedSharedPreferences | AES-256-GCM |
| Usage data | EncryptedSharedPreferences | AES-256-GCM |
| Study state | EncryptedSharedPreferences | AES-256-GCM |
| Questionnaire responses | Memory only (until submit) | N/A |
| Offline queue | Room database in the app's private storage, until sent | Not encrypted by the app |

### Anonymity Guarantees

- **No personally identifiable information** is ever collected
- Study ID is a **random 16-character alphanumeric** string
- **No device identifiers** (Android ID, IMEI, etc.)
- **No location data** is accessed or stored
- **No message content** is ever accessed

---

## Testing

`./gradlew test` runs the unit tests (scoring, timepoints, update check). The hidden debug tools (usage export, simulated passing of days, notification check) and how to enable them are described in `.claude/skills/stilme-debug-tools/SKILL.md`.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Contact

| Purpose | Contact |
|---------|---------|
| Research questions | progettoscreentime.dsspp@unito.it |
| Bug reports | [GitHub Issues](https://github.com/aldogor/stilme-qe-app/issues) |
| Institution | University of Turin, Department of Public Health Sciences and Pediatrics |
