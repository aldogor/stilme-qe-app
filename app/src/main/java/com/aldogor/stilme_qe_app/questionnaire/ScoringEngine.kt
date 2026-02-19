package com.aldogor.stilme_qe_app.questionnaire

import com.aldogor.stilme_qe_app.study.QuestionnaireResponses
import com.aldogor.stilme_qe_app.study.ScaleScores
import com.aldogor.stilme_qe_app.study.StudyGroup

/**
 * Engine for calculating psychometric scale scores and group assignment.
 */
object ScoringEngine {

    // ========================================================================
    // GROUP ASSIGNMENT
    // ========================================================================

    /**
     * Calculate group assignment based on eligibility and interest responses.
     * Returns StudyGroup code (1, 2, 3, -1, or -2)
     */
    fun calculateGroup(responses: QuestionnaireResponses): Int {
        val usingTimeLimits = responses.getIntResponse("using_time_limits")
        val isUnitoStudent = responses.getIntResponse("is_unito_student")
        val ageGroup = responses.getIntResponse("age_group")
        val interestedInLimit = responses.getIntResponse("interested_in_limit")
        val limitMethod = responses.getIntResponse("limit_method")

        return when {
            // Already using STL tools - ineligible but collect T0 data
            usingTimeLimits == 1 -> StudyGroup.INELIGIBLE_USING_STL.code

            // Failed screening criteria
            isUnitoStudent == 0 || ageGroup == 99 -> StudyGroup.INELIGIBLE_SCREENING.code

            // Not interested in reducing use - Control group
            interestedInLimit == 0 -> StudyGroup.CONTROL.code

            // Interested and will use STL tools - STL Intervention group
            limitMethod == 1 -> StudyGroup.STL_INTERVENTION.code

            // Interested but without tools - Personal Commitment group
            limitMethod == 2 -> StudyGroup.PERSONAL_COMMITMENT.code

            // Error case
            else -> 99
        }
    }

    /**
     * Check if participant is eligible based on screening responses.
     */
    fun isEligible(responses: QuestionnaireResponses): Boolean {
        val isUnitoStudent = responses.getIntResponse("is_unito_student")
        val ageGroup = responses.getIntResponse("age_group")
        val usingTimeLimits = responses.getIntResponse("using_time_limits")

        // Ineligible if not UniTO student, wrong age, or already using STL
        if (isUnitoStudent == 0 || ageGroup == 99 || usingTimeLimits == 1) {
            return false
        }

        return true
    }

    // ========================================================================
    // SCALE SCORING
    // ========================================================================

    /**
     * Calculate all scale scores from responses.
     * @param isMonthly If true, uses _m suffix for variable names
     */
    fun calculateScores(responses: QuestionnaireResponses, isMonthly: Boolean = false): ScaleScores {
        val suffix = if (isMonthly) "_m" else ""

        return ScaleScores(
            bsmas = calculateBSMAS(responses, suffix),
            phq9 = calculatePHQ9(responses, suffix),
            gad7 = calculateGAD7(responses, suffix),
            fomo = calculateFoMO(responses, suffix),
            pss10 = calculatePSS10(responses, suffix)
        )
    }

    /**
     * BSMAS - Bergen Social Media Addiction Scale
     * 6 items, each scored 1-5
     * Range: 6-30
     * Cut-off ≥19 suggests problematic use
     */
    fun calculateBSMAS(responses: QuestionnaireResponses, suffix: String = ""): Int {
        return (1..6).sumOf { i ->
            responses.getIntResponse("bsmas_$i$suffix") ?: 0
        }
    }

    /**
     * PHQ-9 - Patient Health Questionnaire
     * 9 items, each scored 0-3
     * Range: 0-27
     * Severity: 0-4 minimal, 5-9 mild, 10-14 moderate, 15-19 moderately severe, 20-27 severe
     */
    fun calculatePHQ9(responses: QuestionnaireResponses, suffix: String = ""): Int {
        return (1..9).sumOf { i ->
            responses.getIntResponse("phq9_$i$suffix") ?: 0
        }
    }

    /**
     * GAD-7 - Generalized Anxiety Disorder
     * 7 items, each scored 0-3
     * Range: 0-21
     * Severity: 0-4 minimal, 5-9 mild, 10-14 moderate, 15-21 severe
     */
    fun calculateGAD7(responses: QuestionnaireResponses, suffix: String = ""): Int {
        return (1..7).sumOf { i ->
            responses.getIntResponse("gad7_$i$suffix") ?: 0
        }
    }

    /**
     * FoMO Scale - Fear of Missing Out
     * 10 items, each scored 1-5
     * Range: 10-50
     */
    fun calculateFoMO(responses: QuestionnaireResponses, suffix: String = ""): Int {
        return (1..10).sumOf { i ->
            responses.getIntResponse("fomo_$i$suffix") ?: 0
        }
    }

    /**
     * PSS-10 - Perceived Stress Scale
     * 10 items, each scored 0-4
     * Items 4, 5, 7, 8 are reverse-scored (4 - response)
     * Range: 0-40
     */
    fun calculatePSS10(responses: QuestionnaireResponses, suffix: String = ""): Int {
        val reverseItems = setOf(4, 5, 7, 8)

        return (1..10).sumOf { i ->
            val value = responses.getIntResponse("pss10_$i$suffix") ?: 0
            if (i in reverseItems) 4 - value else value
        }
    }

    // ========================================================================
    // SEVERITY INTERPRETATIONS
    // ========================================================================

    data class PHQ9Severity(val score: Int, val level: String, val description: String)

    fun getPHQ9Severity(score: Int): PHQ9Severity {
        return when {
            score < 5 -> PHQ9Severity(score, "minimal", "Depressione minima")
            score < 10 -> PHQ9Severity(score, "mild", "Depressione lieve")
            score < 15 -> PHQ9Severity(score, "moderate", "Depressione moderata")
            score < 20 -> PHQ9Severity(score, "moderately_severe", "Depressione moderatamente severa")
            else -> PHQ9Severity(score, "severe", "Depressione severa")
        }
    }

    data class GAD7Severity(val score: Int, val level: String, val description: String)

    fun getGAD7Severity(score: Int): GAD7Severity {
        return when {
            score < 5 -> GAD7Severity(score, "minimal", "Ansia minima")
            score < 10 -> GAD7Severity(score, "mild", "Ansia lieve")
            score < 15 -> GAD7Severity(score, "moderate", "Ansia moderata")
            else -> GAD7Severity(score, "severe", "Ansia severa")
        }
    }

    fun isBSMASProblematic(score: Int): Boolean = score >= 19
}
