package com.aldogor.stilme_qe_app.study

import android.content.Context
import android.util.Log
import com.aldogor.stilme_qe_app.EncryptedPrefsFactory
import com.google.gson.Gson
import java.security.SecureRandom
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

        // Process-wide lock serializing read-copy-write on the participant state. StudyManager is
        // instantiated in several places (activities + WorkManager workers) that all share the same
        // backing prefs file, so mutations must serialize or a worker can clobber a concurrent
        // write (e.g. overwrite a just-completed timepoint). Being in the companion makes the lock
        // shared across every StudyManager instance in the process.
        private val STATE_LOCK = Any()
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
        Log.d(TAG, "Saved participant state")
    }

    /**
     * Atomically read-modify-write the participant state under [STATE_LOCK]. No-op if no state
     * exists yet. All state mutations should go through this so concurrent writers (activities and
     * background workers) cannot clobber each other's updates.
     */
    private fun updateState(transform: (ParticipantState) -> ParticipantState) {
        synchronized(STATE_LOCK) {
            val current = getParticipantState() ?: return
            saveParticipantState(transform(current))
        }
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
        // Serialized so two concurrent callers can't each generate an ID and race to persist it.
        synchronized(STATE_LOCK) {
            getParticipantState()?.let { return it.studyId }

            val newId = generateStudyId()
            saveParticipantState(ParticipantState.createNew(newId))
            return newId
        }
    }

    private fun generateStudyId(): String {
        val chars = StudyConfig.ID_CHARS
        val random = SecureRandom()
        return (1..StudyConfig.ID_LENGTH)
            .map { chars[random.nextInt(chars.length)] }
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

        // Due if ANY timepoint whose window has already opened is still incomplete.
        // This handles skipped windows: a participant who missed T1's 30-day window can
        // still complete it later instead of the timepoint being lost forever.
        val currentIndex = getCurrentTimepoint().index
        return (0..currentIndex).any { it !in state.timepointsCompleted }
    }

    /**
     * The timepoint the participant should complete now: the earliest whose window has
     * opened but which hasn't been completed yet.
     *
     * This differs from [getCurrentTimepoint], which is purely date-derived. If a participant
     * skips a 30-day window (vacation, app not opened, etc.), [getCurrentTimepoint] would jump
     * ahead and the missed timepoint's data would be permanently lost. [getActiveTimepoint]
     * instead offers the earliest incomplete timepoint so no follow-up is silently skipped.
     */
    fun getActiveTimepoint(): Timepoint {
        val state = getParticipantState() ?: return Timepoint.T0
        return Timepoint.earliestDue(getCurrentTimepoint().index, state.timepointsCompleted)
    }

    fun getDaysUntilWindowCloses(): Int {
        val state = getParticipantState() ?: return 30
        val baselineDate = state.baselineDate ?: return 30

        val currentTimepoint = getActiveTimepoint()
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

        val currentTimepoint = getActiveTimepoint()
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

    fun markConsentGiven(informedConsent: Boolean, privacyConsent: Boolean) = updateState { state ->
        state.copy(
            consentGiven = true,
            consentInformed = informedConsent,
            consentPrivacy = privacyConsent,
            consentTimestampString = LocalDateTime.now().toString()
        )
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

    fun markOnboardingComplete() = updateState { it.copy(onboardingComplete = true) }

    fun setGroup(group: Int) = updateState { it.copy(group = group) }

    fun setEligibility(eligible: Boolean) = updateState { it.copy(isEligible = eligible) }

    fun markTimepointComplete(timepoint: Timepoint) {
        val isComplete = timepoint == Timepoint.T4
        // Use the simulated date so debug time-travel writes a consistent baseline/completion date
        // (production offset is 0, so this is LocalDate.now() in the field).
        val today = getSimulatedCurrentDate().toString()
        updateState { state ->
            state.copy(
                timepointsCompleted = state.timepointsCompleted + timepoint.index,
                currentTimepoint = timepoint.index,
                isStudyComplete = isComplete,
                pendingQuestionnaire = false,
                baselineDateString = if (timepoint == Timepoint.T0 && state.baselineDateString == null) {
                    today
                } else {
                    state.baselineDateString
                },
                lastCompletionDateString = today
            )
        }
        Log.d(TAG, "Marked timepoint ${timepoint.displayName} complete. Study complete: $isComplete")
    }

    fun markQuestionnairePending(pending: Boolean) = updateState { it.copy(pendingQuestionnaire = pending) }

    fun updateLastUsageCollection() = updateState {
        it.copy(lastUsageCollectionString = LocalDateTime.now().toString())
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
        updateState { it.copy(isStudyComplete = true, pendingQuestionnaire = false) }
        Log.d(TAG, "Study marked as complete")
    }

    // ========================================================================
    // WITHDRAWAL
    // ========================================================================

    /**
     * Records that the participant requested withdrawal but the server-side confirmation
     * (record deletion + tombstone) has not yet succeeded. Local data is intentionally KEPT
     * so the withdrawal can be retried; it is wiped only once the server confirms.
     */
    fun markWithdrawalPending() {
        updateState { it.copy(pendingWithdrawal = true) }
    }

    fun isWithdrawalPending(): Boolean {
        return getParticipantState()?.pendingWithdrawal == true
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
