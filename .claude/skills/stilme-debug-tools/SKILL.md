---
name: stilme-debug-tools
description: Use when testing the study timeline of stilme-qe-app on a device or emulator, such as enabling the hidden debug buttons, simulating the passing of days (time travel), triggering the questionnaire notification check, or resetting the participant state.
---

# Debug tools in stilme-qe-app

The debug controls are hidden in every build; nothing in the shipped app changes unless one of the switches below is flipped, and neither switch should be committed enabled.

## Enabling the debug buttons

Either of:

- set `android:visibility="visible"` on `debug_buttons_container` in `app/src/main/res/layout/activity_main.xml`, or
- uncomment the `if (BuildConfig.DEBUG)` block in `MainActivity.setupUI()` (`app/src/main/java/com/aldogor/stilme_qe_app/MainActivity.kt`).

The buttons give a usage-data CSV export to the clipboard and the time-jump dialog (`res/layout/dialog_debug_time_jump.xml`).

## Time travel (debug day offset)

The offset is stored by `StudyManager` (`study/StudyManager.kt`) and added to the real date only inside the study-state calculations.

| Affected | Not affected |
|---|---|
| `isQuestionnaireDue()`, `getCurrentTimepoint()`, `getActiveTimepoint()` | Background usage collection and UsageStats dates |
| `getDaysSinceWindowOpened()`, `getDaysUntilWindowCloses()` | REDCap timestamps and `lastCompletionDateString` |
| Main-screen status messages | WorkManager scheduling (real clock) |

Because WorkManager runs on the real clock, jumping days does not fire a reminder by itself: press "Test notifica" in the dialog to run the notification check against the simulated date. Reset the offset from the dialog before testing anything date-sensitive for real.

## Clearing all app state

Uninstall and reinstall, or Settings > Apps > MIND TIME > Clear data: both wipe every encrypted prefs file, so the participant restarts onboarding with a new study ID. `studyManager.clearAllData()` clears only the study state (`stilme_study_state`), leaving usage data and the submission queue in place.

## Emulator

Machine setup (Android CLI, AVD `stilme_test`, TEMP workaround for Gradle) is in `CLAUDE.md`, section Toolchain.
