package com.aldogor.stilme_qe_app.study

import android.content.Context
import android.util.Log
import com.aldogor.stilme_qe_app.EncryptedPrefsFactory
import com.google.gson.Gson
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Manages study state, timepoint tracking, and participant data.
 */
class StudyManager(context: Context) {

    companion object {
        private const val TAG = "StudyManager"
        private const val PREFS_NAME = "stilme_study_state"
        private const val KEY_PARTICIPANT_STATE = "participant_state"
        private const val KEY_DEBUG_DAY_OFFSET = "debug_day_offset"
        private const val KEY_INVITE_SHOWN_FOR_TIMEPOINT = "invite_shown_for_timepoint"
    }

    private val prefs = EncryptedPrefsFactory.create(context, PREFS_NAME)

    private val gson = Gson()

    // ========================================================================
    // STATE MANAGEMENT
    // ========================================================================

    fun getParticipantState(): ParticipantState? {
        val json = prefs.getString(KEY_PARTICIPANT_STATE, null) ?: return null
        return try {
            gson.fromJson(json, ParticipantState::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing participant state", e)
            null
        }
    }

    fun saveParticipantState(state: ParticipantState) {
        val json = gson.toJson(state)
        prefs.edit().putString(KEY_PARTICIPANT_STATE, json).apply()
        Log.d(TAG, "Saved participant state: $json")
    }

    fun hasCompletedOnboarding(): Boolean {
        return getParticipantState()?.onboardingComplete == true
    }

    fun isStudyComplete(): Boolean {
        return getParticipantState()?.isStudyComplete == true
    }

    fun isEligible(): Boolean {
        return getParticipantState()?.isEligible == true
    }

    // ========================================================================
    // STUDY ID MANAGEMENT
    // ========================================================================

    fun getOrCreateStudyId(): String {
        val state = getParticipantState()
        if (state != null) {
            return state.studyId
        }

        // Generate new study ID
        val newId = generateStudyId()
        val newState = ParticipantState.createNew(newId)
        saveParticipantState(newState)
        return newId
    }

    private fun generateStudyId(): String {
        val chars = StudyConfig.ID_CHARS
        return (1..StudyConfig.ID_LENGTH)
            .map { chars.random() }
            .joinToString("")
    }

    // ========================================================================
    // DEBUG DAY OFFSET (for testing study flow)
    // ========================================================================

    /**
     * Gets the simulated "current date" for study timeline calculations.
     * In production (offset = 0), returns LocalDate.now().
     * With a debug offset, returns a future date.
     */
    fun getSimulatedCurrentDate(): LocalDate {
        val offset = getDebugDayOffset()
        return LocalDate.now().plusDays(offset.toLong())
    }

    /**
     * Gets the current debug day offset.
     */
    fun getDebugDayOffset(): Int {
        return prefs.getInt(KEY_DEBUG_DAY_OFFSET, 0)
    }

    /**
     * Sets the debug day offset (for testing).
     * @param days Number of days to add to the current date for study calculations
     */
    fun setDebugDayOffset(days: Int) {
        prefs.edit().putInt(KEY_DEBUG_DAY_OFFSET, days).apply()
        Log.d(TAG, "Debug day offset set to $days days")
    }

    /**
     * Adds days to the current debug offset.
     * @param days Number of days to add
     */
    fun addDebugDays(days: Int) {
        val currentOffset = getDebugDayOffset()
        setDebugDayOffset(currentOffset + days)
    }

    /**
     * Resets the debug day offset to 0.
     */
    fun resetDebugDayOffset() {
        setDebugDayOffset(0)
    }

    // ========================================================================
    // TIMEPOINT MANAGEMENT
    // ========================================================================

    fun getCurrentTimepoint(): Timepoint {
        val state = getParticipantState() ?: return Timepoint.T0
        val baselineDate = state.baselineDate ?: return Timepoint.T0

        val daysSinceBaseline = ChronoUnit.DAYS.between(baselineDate, getSimulatedCurrentDate())
        return Timepoint.fromDaysSinceBaseline(daysSinceBaseline)
    }

    fun isQuestionnaireDue(): Boolean {
        val state = getParticipantState() ?: return false // No state = not yet onboarded

        if (state.isStudyComplete) return false
        if (!state.isEligible) return false

        val currentTimepoint = getCurrentTimepoint()
        return currentTimepoint.index !in state.timepointsCompleted
    }

    fun getDaysUntilWindowCloses(): Int {
        val state = getParticipantState() ?: return 30
        val baselineDate = state.baselineDate ?: return 30

        val currentTimepoint = getCurrentTimepoint()
        val windowEndDay = (currentTimepoint.index + 1) * 30 // Each window is 30 days

        val daysSinceBaseline = ChronoUnit.DAYS.between(baselineDate, getSimulatedCurrentDate()).toInt()
        return maxOf(0, windowEndDay - daysSinceBaseline)
    }

    fun getNextQuestionnaireDueDate(): LocalDate? {
        val state = getParticipantState() ?: return getSimulatedCurrentDate()

        if (state.isStudyComplete) return null

        val baselineDate = state.baselineDate ?: return getSimulatedCurrentDate()

        // Find next uncompleted timepoint
        val nextTimepoint = (0..4).firstOrNull { it !in state.timepointsCompleted }
            ?: return null

        return when (nextTimepoint) {
            0 -> getSimulatedCurrentDate()
            1 -> baselineDate.plusDays(30)
            2 -> baselineDate.plusDays(60)
            3 -> baselineDate.plusDays(90)
            4 -> baselineDate.plusDays(120)
            else -> null
        }
    }

    fun getDaysSinceBaseline(): Long {
        val state = getParticipantState() ?: return 0
        val baselineDate = state.baselineDate ?: return 0
        return ChronoUnit.DAYS.between(baselineDate, getSimulatedCurrentDate())
    }

    /**
     * Returns the number of days since the current questionnaire window opened.
     * Used to determine which notification type to show.
     */
    fun getDaysSinceWindowOpened(): Int {
        val state = getParticipantState() ?: return 0
        val baselineDate = state.baselineDate ?: return 0

        val currentTimepoint = getCurrentTimepoint()
        val windowStartDay = currentTimepoint.daysFromBaseline
        val daysSinceBaseline = ChronoUnit.DAYS.between(baselineDate, getSimulatedCurrentDate()).toInt()

        return maxOf(0, daysSinceBaseline - windowStartDay)
    }

    /**
     * Returns the number of days since the last questionnaire was completed.
     * Used to determine when to show the invite friend notification.
     */
    fun getDaysSinceLastCompletion(): Int {
        val state = getParticipantState() ?: return -1
        val lastCompletionDateString = state.lastCompletionDateString ?: return -1

        return try {
            val lastCompletion = LocalDate.parse(lastCompletionDateString)
            ChronoUnit.DAYS.between(lastCompletion, getSimulatedCurrentDate()).toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing last completion date", e)
            -1
        }
    }

    // ========================================================================
    // STATE UPDATES
    // ========================================================================

    fun markConsentGiven(informedConsent: Boolean, privacyConsent: Boolean) {
        val state = getParticipantState() ?: return
        saveParticipantState(state.copy(
            consentGiven = true,
            consentInformed = informedConsent,
            consentPrivacy = privacyConsent,
            consentTimestampString = LocalDateTime.now().toString()
        ))
    }

    fun getConsentData(): ConsentData? {
        val state = getParticipantState() ?: return null
        if (!state.consentGiven) return null
        return ConsentData(
            informedConsent = state.consentInformed,
            privacyConsent = state.consentPrivacy,
            timestamp = state.consentTimestampString
        )
    }

    fun markOnboardingComplete() {
        val state = getParticipantState() ?: return
        saveParticipantState(state.copy(onboardingComplete = true))
    }

    fun setGroup(group: Int) {
        val state = getParticipantState() ?: return
        saveParticipantState(state.copy(group = group))
    }

    fun setEligibility(eligible: Boolean) {
        val state = getParticipantState() ?: return
        saveParticipantState(state.copy(isEligible = eligible))
    }

    fun markTimepointComplete(timepoint: Timepoint) {
        val state = getParticipantState() ?: return

        val newTimepointsCompleted = state.timepointsCompleted + timepoint.index

        val isComplete = timepoint == Timepoint.T4

        val newState = state.copy(
            timepointsCompleted = newTimepointsCompleted,
            currentTimepoint = timepoint.index,
            isStudyComplete = isComplete,
            pendingQuestionnaire = false,
            baselineDateString = if (timepoint == Timepoint.T0 && state.baselineDateString == null) {
                LocalDate.now().toString()
            } else {
                state.baselineDateString
            },
            lastCompletionDateString = LocalDate.now().toString()
        )

        saveParticipantState(newState)

        Log.d(TAG, "Marked timepoint ${timepoint.displayName} complete. Study complete: $isComplete")
    }

    fun markQuestionnairePending(pending: Boolean) {
        val state = getParticipantState() ?: return
        saveParticipantState(state.copy(pendingQuestionnaire = pending))
    }

    fun updateLastUsageCollection() {
        val state = getParticipantState() ?: return
        saveParticipantState(state.copy(
            lastUsageCollectionString = LocalDateTime.now().toString()
        ))
    }

    /**
     * Checks if the invite notification has already been shown for the given timepoint.
     */
    fun hasInviteBeenShownForTimepoint(timepointIndex: Int): Boolean {
        return prefs.getInt(KEY_INVITE_SHOWN_FOR_TIMEPOINT, -1) == timepointIndex
    }

    /**
     * Marks the invite notification as shown for the given timepoint.
     */
    fun markInviteShownForTimepoint(timepointIndex: Int) {
        prefs.edit().putInt(KEY_INVITE_SHOWN_FOR_TIMEPOINT, timepointIndex).apply()
    }

    // ========================================================================
    // STUDY COMPLETION
    // ========================================================================

    fun completeStudy() {
        val state = getParticipantState() ?: return
        saveParticipantState(state.copy(
            isStudyComplete = true,
            pendingQuestionnaire = false
        ))
        Log.d(TAG, "Study marked as complete")
    }

    // ========================================================================
    // DATA MANAGEMENT
    // ========================================================================

    fun clearAllData() {
        prefs.edit().clear().apply()
        Log.d(TAG, "All study data cleared")
    }

    fun getGroup(): Int {
        return getParticipantState()?.group ?: 0
    }

    fun getStudyId(): String? {
        return getParticipantState()?.studyId
    }

    // ========================================================================
    // STATUS DISPLAY
    // ========================================================================

    fun getStatusSummary(): String {
        val state = getParticipantState() ?: return "Stato: Non iniziato"

        if (state.isStudyComplete) {
            return "Studio completato"
        }

        if (!state.isEligible) {
            return "Non idoneo alla partecipazione"
        }

        if (!state.onboardingComplete) {
            return "Configurazione in corso..."
        }

        val currentTimepoint = getCurrentTimepoint()
        val dueDate = getNextQuestionnaireDueDate()

        return if (isQuestionnaireDue()) {
            "Questionario ${currentTimepoint.displayName} da completare"
        } else {
            val daysUntilNext = if (dueDate != null) {
                ChronoUnit.DAYS.between(getSimulatedCurrentDate(), dueDate)
            } else {
                0
            }

            if (daysUntilNext > 0) {
                "Prossimo questionario tra $daysUntilNext giorni"
            } else {
                "In attesa del prossimo questionario"
            }
        }
    }

    fun getGroupName(): String {
        val group = getGroup()
        return StudyGroup.fromCode(group)?.displayName ?: "Non assegnato"
    }
}
