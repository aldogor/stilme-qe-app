package com.aldogor.stilme_qe_app.study

import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Study-related data models for STILME-QE.
 */

// ============================================================================
// STUDY CONSTANTS
// ============================================================================

object StudyConfig {
    const val ID_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    const val ID_LENGTH = 16

    // Collection constants
    const val COLLECTION_DAYS = 9
    const val COLLECTION_INTERVAL = 7
    const val MAX_STORAGE_DAYS = 140

    // Timepoint intervals (days from baseline)
    const val T1_DAYS = 30
    const val T2_DAYS = 60
    const val T3_DAYS = 90
    const val T4_DAYS = 120

    // Window duration for completing questionnaires
    const val QUESTIONNAIRE_WINDOW_DAYS = 30
}

// ============================================================================
// PARTICIPANT STATE
// ============================================================================

/**
 * Complete state of a study participant.
 * Stored encrypted in SharedPreferences.
 */
data class ParticipantState(
    @SerializedName("study_id")
    val studyId: String,

    @SerializedName("group")
    val group: Int, // 1=Control, 2=STL, 3=Personal Commitment, -1=Ineligible (using STL), -2=Ineligible (screening)

    @SerializedName("baseline_date")
    val baselineDateString: String?, // ISO-8601 format

    @SerializedName("current_timepoint")
    val currentTimepoint: Int = 0, // 0-4

    @SerializedName("timepoints_completed")
    val timepointsCompleted: Set<Int> = emptySet(),

    @SerializedName("is_study_complete")
    val isStudyComplete: Boolean = false,

    @SerializedName("is_eligible")
    val isEligible: Boolean = true,

    @SerializedName("last_usage_collection")
    val lastUsageCollectionString: String?, // ISO-8601 format

    @SerializedName("pending_questionnaire")
    val pendingQuestionnaire: Boolean = false,

    @SerializedName("consent_given")
    val consentGiven: Boolean = false,

    @SerializedName("consent_informed")
    val consentInformed: Boolean = false, // "Ho letto e compreso... e acconsento a partecipare"

    @SerializedName("consent_privacy")
    val consentPrivacy: Boolean = false, // "Acconsento al trattamento dei dati"

    @SerializedName("consent_timestamp")
    val consentTimestampString: String? = null, // ISO-8601 timestamp

    @SerializedName("onboarding_complete")
    val onboardingComplete: Boolean = false,

    @SerializedName("last_completion_date")
    val lastCompletionDateString: String? = null // ISO-8601 date of last questionnaire completion
) {
    val baselineDate: LocalDate?
        get() = baselineDateString?.let { LocalDate.parse(it) }

    val lastUsageCollection: LocalDateTime?
        get() = lastUsageCollectionString?.let { LocalDateTime.parse(it) }

    val consentTimestamp: LocalDateTime?
        get() = consentTimestampString?.let { LocalDateTime.parse(it) }

    val lastCompletionDate: LocalDate?
        get() = lastCompletionDateString?.let { LocalDate.parse(it) }

    companion object {
        fun createNew(studyId: String): ParticipantState {
            return ParticipantState(
                studyId = studyId,
                group = 0, // Not yet assigned
                baselineDateString = null,
                lastUsageCollectionString = null
            )
        }
    }
}

// ============================================================================
// GROUP DEFINITIONS
// ============================================================================

enum class StudyGroup(val code: Int, val displayName: String) {
    CONTROL(1, "Gruppo di Controllo"),
    STL_INTERVENTION(2, "Gruppo Screen Time Limits"),
    PERSONAL_COMMITMENT(3, "Gruppo Impegno Personale"),
    INELIGIBLE_USING_STL(-1, "Non idoneo - Già usa STL"),
    INELIGIBLE_SCREENING(-2, "Non idoneo - Criteri");

    companion object {
        fun fromCode(code: Int): StudyGroup? = entries.find { it.code == code }
    }
}

// ============================================================================
// TIMEPOINT DEFINITIONS
// ============================================================================

enum class Timepoint(val index: Int, val daysFromBaseline: Int, val displayName: String) {
    T0(0, 0, "Baseline"),
    T1(1, 30, "Follow-up 1 mese"),
    T2(2, 60, "Follow-up 2 mesi"),
    T3(3, 90, "Follow-up 3 mesi"),
    T4(4, 120, "Follow-up finale");

    val redcapEventName: String
        get() = when (this) {
            T0 -> "baseline_arm_1"
            T1 -> "followup_1_arm_1"
            T2 -> "followup_2_arm_1"
            T3 -> "followup_3_arm_1"
            T4 -> "followup_4_arm_1"
        }

    companion object {
        fun fromIndex(index: Int): Timepoint? = entries.find { it.index == index }

        fun fromDaysSinceBaseline(days: Long): Timepoint {
            return when {
                days < 30 -> T0
                days < 60 -> T1
                days < 90 -> T2
                days < 120 -> T3
                else -> T4
            }
        }
    }
}

// ============================================================================
// QUESTIONNAIRE MODELS
// ============================================================================

/**
 * Type of questionnaire form.
 */
enum class QuestionnaireType {
    BASELINE,      // T0 - Full questionnaire with demographics
    MONTHLY,       // T1-T3 - Monthly follow-up
    FINAL          // T4 - Monthly + exit question for Group 2
}

/**
 * Type of question input.
 */
enum class QuestionType {
    RADIO,         // Single choice from options
    YESNO,         // Yes/No binary choice
    TEXT,          // Free text input
    SCALE          // Likert scale (special display)
}

/**
 * Definition of a single question.
 */
data class QuestionDefinition(
    val variableName: String,
    val label: String,
    val type: QuestionType,
    val options: List<QuestionOption> = emptyList(),
    val showIf: BranchingCondition? = null,
    val required: Boolean = true,
    val helperText: String? = null,
    val scaleGroup: String? = null, // For grouping scale items
    val scaleInstructions: String? = null // Instructions shown before scale
)

/**
 * Option for radio/select questions.
 */
data class QuestionOption(
    val value: Int,
    val label: String
)

/**
 * Branching condition for showing/hiding questions.
 */
data class BranchingCondition(
    val dependsOn: String,        // Variable name to check
    val operator: BranchingOperator,
    val value: Any                // Value to compare against
)

enum class BranchingOperator {
    EQUALS,
    NOT_EQUALS,
    IN,          // Value is in a set
    NOT_IN,
    GREATER_THAN,
    LESS_THAN
}

/**
 * Collected questionnaire responses.
 */
data class QuestionnaireResponses(
    val studyId: String,
    val timepoint: Timepoint,
    val responses: MutableMap<String, Any> = mutableMapOf(),
    val startedAt: LocalDateTime = LocalDateTime.now(),
    var completedAt: LocalDateTime? = null
) {
    fun setResponse(variableName: String, value: Any) {
        responses[variableName] = value
    }

    fun getResponse(variableName: String): Any? = responses[variableName]

    fun getIntResponse(variableName: String): Int? = responses[variableName] as? Int

    fun getStringResponse(variableName: String): String? = responses[variableName] as? String
}

// ============================================================================
// SCALE SCORES
// ============================================================================

/**
 * Calculated scores for all psychometric scales.
 */
data class ScaleScores(
    val bsmas: Int = 0,      // Bergen Social Media Addiction Scale (6-30)
    val phq9: Int = 0,       // Patient Health Questionnaire (0-27)
    val gad7: Int = 0,       // Generalized Anxiety Disorder (0-21)
    val fomo: Int = 0,       // Fear of Missing Out (10-50)
    val pss10: Int = 0       // Perceived Stress Scale (0-40)
)

// ============================================================================
// CONSENT DATA
// ============================================================================

/**
 * Data about informed consent for REDCap submission.
 */
data class ConsentData(
    val informedConsent: Boolean, // Checkbox: read + agree to participate
    val privacyConsent: Boolean,  // Checkbox: consent to data processing (privacy)
    val timestamp: String?        // ISO-8601 timestamp when consent was given
)

// ============================================================================
// SUBMISSION MODELS
// ============================================================================

/**
 * Status of a queued submission.
 */
enum class SubmissionStatus {
    PENDING,
    SUBMITTING,
    FAILED,
    SUCCESS
}

/**
 * Result of a REDCap API call.
 */
sealed class RedcapResult {
    data class Success(val recordId: String) : RedcapResult()
    data class NetworkError(val message: String) : RedcapResult()
    data class ServerError(val code: Int, val message: String) : RedcapResult()
    data class ParseError(val message: String) : RedcapResult()
}
