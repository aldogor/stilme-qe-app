package com.aldogor.stilme_qe_app.questionnaire

import com.aldogor.stilme_qe_app.study.QuestionnaireResponses
import com.aldogor.stilme_qe_app.study.StudyGroup
import com.aldogor.stilme_qe_app.study.Timepoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringEngineTest {

    private fun createResponses(): QuestionnaireResponses {
        return QuestionnaireResponses(
            studyId = "TEST1234",
            timepoint = Timepoint.T0
        )
    }

    // ========================================================================
    // BSMAS Tests (6 items, scored 1-5, range 6-30)
    // ========================================================================

    @Test
    fun `BSMAS all minimum scores equals 6`() {
        val responses = createResponses()
        for (i in 1..6) responses.setResponse("bsmas_$i", 1)
        assertEquals(6, ScoringEngine.calculateBSMAS(responses))
    }

    @Test
    fun `BSMAS all maximum scores equals 30`() {
        val responses = createResponses()
        for (i in 1..6) responses.setResponse("bsmas_$i", 5)
        assertEquals(30, ScoringEngine.calculateBSMAS(responses))
    }

    @Test
    fun `BSMAS mixed scores calculated correctly`() {
        val responses = createResponses()
        responses.setResponse("bsmas_1", 1)
        responses.setResponse("bsmas_2", 2)
        responses.setResponse("bsmas_3", 3)
        responses.setResponse("bsmas_4", 4)
        responses.setResponse("bsmas_5", 5)
        responses.setResponse("bsmas_6", 3)
        assertEquals(18, ScoringEngine.calculateBSMAS(responses))
    }

    @Test
    fun `BSMAS with monthly suffix`() {
        val responses = createResponses()
        for (i in 1..6) responses.setResponse("bsmas_${i}_m", 3)
        assertEquals(18, ScoringEngine.calculateBSMAS(responses, "_m"))
    }

    @Test
    fun `BSMAS problematic threshold at 19`() {
        assertTrue(ScoringEngine.isBSMASProblematic(19))
        assertTrue(ScoringEngine.isBSMASProblematic(30))
        assertFalse(ScoringEngine.isBSMASProblematic(18))
        assertFalse(ScoringEngine.isBSMASProblematic(6))
    }

    // ========================================================================
    // PHQ-9 Tests (9 items, scored 0-3, range 0-27)
    // ========================================================================

    @Test
    fun `PHQ9 all zeros equals 0`() {
        val responses = createResponses()
        for (i in 1..9) responses.setResponse("phq9_$i", 0)
        assertEquals(0, ScoringEngine.calculatePHQ9(responses))
    }

    @Test
    fun `PHQ9 all maximum scores equals 27`() {
        val responses = createResponses()
        for (i in 1..9) responses.setResponse("phq9_$i", 3)
        assertEquals(27, ScoringEngine.calculatePHQ9(responses))
    }

    @Test
    fun `PHQ9 mixed scores calculated correctly`() {
        val responses = createResponses()
        responses.setResponse("phq9_1", 0)
        responses.setResponse("phq9_2", 1)
        responses.setResponse("phq9_3", 2)
        responses.setResponse("phq9_4", 3)
        responses.setResponse("phq9_5", 0)
        responses.setResponse("phq9_6", 1)
        responses.setResponse("phq9_7", 2)
        responses.setResponse("phq9_8", 3)
        responses.setResponse("phq9_9", 1)
        assertEquals(13, ScoringEngine.calculatePHQ9(responses))
    }

    @Test
    fun `PHQ9 severity levels`() {
        assertEquals("minimal", ScoringEngine.getPHQ9Severity(0).level)
        assertEquals("minimal", ScoringEngine.getPHQ9Severity(4).level)
        assertEquals("mild", ScoringEngine.getPHQ9Severity(5).level)
        assertEquals("mild", ScoringEngine.getPHQ9Severity(9).level)
        assertEquals("moderate", ScoringEngine.getPHQ9Severity(10).level)
        assertEquals("moderate", ScoringEngine.getPHQ9Severity(14).level)
        assertEquals("moderately_severe", ScoringEngine.getPHQ9Severity(15).level)
        assertEquals("moderately_severe", ScoringEngine.getPHQ9Severity(19).level)
        assertEquals("severe", ScoringEngine.getPHQ9Severity(20).level)
        assertEquals("severe", ScoringEngine.getPHQ9Severity(27).level)
    }

    // ========================================================================
    // GAD-7 Tests (7 items, scored 0-3, range 0-21)
    // ========================================================================

    @Test
    fun `GAD7 all zeros equals 0`() {
        val responses = createResponses()
        for (i in 1..7) responses.setResponse("gad7_$i", 0)
        assertEquals(0, ScoringEngine.calculateGAD7(responses))
    }

    @Test
    fun `GAD7 all maximum scores equals 21`() {
        val responses = createResponses()
        for (i in 1..7) responses.setResponse("gad7_$i", 3)
        assertEquals(21, ScoringEngine.calculateGAD7(responses))
    }

    @Test
    fun `GAD7 severity levels`() {
        assertEquals("minimal", ScoringEngine.getGAD7Severity(0).level)
        assertEquals("minimal", ScoringEngine.getGAD7Severity(4).level)
        assertEquals("mild", ScoringEngine.getGAD7Severity(5).level)
        assertEquals("mild", ScoringEngine.getGAD7Severity(9).level)
        assertEquals("moderate", ScoringEngine.getGAD7Severity(10).level)
        assertEquals("moderate", ScoringEngine.getGAD7Severity(14).level)
        assertEquals("severe", ScoringEngine.getGAD7Severity(15).level)
        assertEquals("severe", ScoringEngine.getGAD7Severity(21).level)
    }

    // ========================================================================
    // FoMO Tests (10 items, scored 1-5, range 10-50)
    // ========================================================================

    @Test
    fun `FoMO all minimum scores equals 10`() {
        val responses = createResponses()
        for (i in 1..10) responses.setResponse("fomo_$i", 1)
        assertEquals(10, ScoringEngine.calculateFoMO(responses))
    }

    @Test
    fun `FoMO all maximum scores equals 50`() {
        val responses = createResponses()
        for (i in 1..10) responses.setResponse("fomo_$i", 5)
        assertEquals(50, ScoringEngine.calculateFoMO(responses))
    }

    // ========================================================================
    // PSS-10 Tests (10 items, scored 0-4, range 0-40)
    // Items 4, 5, 7, 8 are reverse-scored (4 - response)
    // ========================================================================

    @Test
    fun `PSS10 all zeros with reverse scoring`() {
        val responses = createResponses()
        for (i in 1..10) responses.setResponse("pss10_$i", 0)
        // Regular items (1,2,3,6,9,10): 6 * 0 = 0
        // Reverse items (4,5,7,8): 4 * (4-0) = 16
        assertEquals(16, ScoringEngine.calculatePSS10(responses))
    }

    @Test
    fun `PSS10 all fours with reverse scoring`() {
        val responses = createResponses()
        for (i in 1..10) responses.setResponse("pss10_$i", 4)
        // Regular items (1,2,3,6,9,10): 6 * 4 = 24
        // Reverse items (4,5,7,8): 4 * (4-4) = 0
        assertEquals(24, ScoringEngine.calculatePSS10(responses))
    }

    @Test
    fun `PSS10 reverse scoring items verified individually`() {
        // Only set reverse items (4,5,7,8) to known values, rest to 0
        val responses = createResponses()
        for (i in 1..10) responses.setResponse("pss10_$i", 0)

        // Item 4 = 3, reversed: 4-3 = 1
        responses.setResponse("pss10_4", 3)
        // Item 5 = 2, reversed: 4-2 = 2
        responses.setResponse("pss10_5", 2)
        // Item 7 = 1, reversed: 4-1 = 3
        responses.setResponse("pss10_7", 1)
        // Item 8 = 0, reversed: 4-0 = 4
        responses.setResponse("pss10_8", 0)

        // Regular items at 0: 6 * 0 = 0
        // Reverse items: 1 + 2 + 3 + 4 = 10
        assertEquals(10, ScoringEngine.calculatePSS10(responses))
    }

    @Test
    fun `PSS10 maximum stress scenario`() {
        // Maximum stress: regular items at max (4), reverse items at min (0)
        val responses = createResponses()
        // Regular items (1,2,3,6,9,10) at 4
        for (i in listOf(1, 2, 3, 6, 9, 10)) responses.setResponse("pss10_$i", 4)
        // Reverse items (4,5,7,8) at 0 -> reversed to 4
        for (i in listOf(4, 5, 7, 8)) responses.setResponse("pss10_$i", 0)
        // 6*4 + 4*4 = 24 + 16 = 40
        assertEquals(40, ScoringEngine.calculatePSS10(responses))
    }

    @Test
    fun `PSS10 minimum stress scenario`() {
        // Minimum stress: regular items at min (0), reverse items at max (4)
        val responses = createResponses()
        // Regular items at 0
        for (i in listOf(1, 2, 3, 6, 9, 10)) responses.setResponse("pss10_$i", 0)
        // Reverse items at 4 -> reversed to 0
        for (i in listOf(4, 5, 7, 8)) responses.setResponse("pss10_$i", 4)
        // 6*0 + 4*0 = 0
        assertEquals(0, ScoringEngine.calculatePSS10(responses))
    }

    // ========================================================================
    // calculateScores Tests (all scales together)
    // ========================================================================

    @Test
    fun `calculateScores baseline suffix`() {
        val responses = createResponses()
        // Set all baseline items
        for (i in 1..6) responses.setResponse("bsmas_$i", 3)
        for (i in 1..9) responses.setResponse("phq9_$i", 1)
        for (i in 1..7) responses.setResponse("gad7_$i", 2)
        for (i in 1..10) responses.setResponse("fomo_$i", 3)
        for (i in 1..10) responses.setResponse("pss10_$i", 2)

        val scores = ScoringEngine.calculateScores(responses, isMonthly = false)
        assertEquals(18, scores.bsmas) // 6 * 3
        assertEquals(9, scores.phq9)   // 9 * 1
        assertEquals(14, scores.gad7)  // 7 * 2
        assertEquals(30, scores.fomo)  // 10 * 3
        // PSS10: regular items (1,2,3,6,9,10) = 6*2 = 12
        // Reverse items (4,5,7,8) = 4*(4-2) = 8
        assertEquals(20, scores.pss10) // 12 + 8
    }

    @Test
    fun `calculateScores monthly suffix`() {
        val responses = createResponses()
        // Set all monthly items (with _m suffix)
        for (i in 1..6) responses.setResponse("bsmas_${i}_m", 2)
        for (i in 1..9) responses.setResponse("phq9_${i}_m", 0)
        for (i in 1..7) responses.setResponse("gad7_${i}_m", 0)
        for (i in 1..10) responses.setResponse("fomo_${i}_m", 1)
        for (i in 1..10) responses.setResponse("pss10_${i}_m", 0)

        val scores = ScoringEngine.calculateScores(responses, isMonthly = true)
        assertEquals(12, scores.bsmas) // 6 * 2
        assertEquals(0, scores.phq9)
        assertEquals(0, scores.gad7)
        assertEquals(10, scores.fomo)  // 10 * 1
        assertEquals(16, scores.pss10) // All 0: regular=0, reverse=4*4=16
    }

    // ========================================================================
    // Group Assignment Tests
    // ========================================================================

    @Test
    fun `group assignment - already using STL`() {
        val responses = createResponses()
        responses.setResponse("using_time_limits", 1)
        responses.setResponse("is_unito_student", 1)
        responses.setResponse("age_group", 1)
        assertEquals(StudyGroup.INELIGIBLE_USING_STL.code, ScoringEngine.calculateGroup(responses))
    }

    @Test
    fun `group assignment - not UniTO student`() {
        val responses = createResponses()
        responses.setResponse("using_time_limits", 0)
        responses.setResponse("is_unito_student", 0)
        responses.setResponse("age_group", 1)
        assertEquals(StudyGroup.INELIGIBLE_SCREENING.code, ScoringEngine.calculateGroup(responses))
    }

    @Test
    fun `group assignment - age over 30`() {
        val responses = createResponses()
        responses.setResponse("using_time_limits", 0)
        responses.setResponse("is_unito_student", 1)
        responses.setResponse("age_group", 99)
        assertEquals(StudyGroup.INELIGIBLE_SCREENING.code, ScoringEngine.calculateGroup(responses))
    }

    @Test
    fun `group assignment - control`() {
        val responses = createResponses()
        responses.setResponse("using_time_limits", 0)
        responses.setResponse("is_unito_student", 1)
        responses.setResponse("age_group", 1)
        responses.setResponse("interested_in_limit", 0)
        assertEquals(StudyGroup.CONTROL.code, ScoringEngine.calculateGroup(responses))
    }

    @Test
    fun `group assignment - STL intervention`() {
        val responses = createResponses()
        responses.setResponse("using_time_limits", 0)
        responses.setResponse("is_unito_student", 1)
        responses.setResponse("age_group", 1)
        responses.setResponse("interested_in_limit", 1)
        responses.setResponse("limit_method", 1)
        assertEquals(StudyGroup.STL_INTERVENTION.code, ScoringEngine.calculateGroup(responses))
    }

    @Test
    fun `group assignment - personal commitment`() {
        val responses = createResponses()
        responses.setResponse("using_time_limits", 0)
        responses.setResponse("is_unito_student", 1)
        responses.setResponse("age_group", 1)
        responses.setResponse("interested_in_limit", 1)
        responses.setResponse("limit_method", 2)
        assertEquals(StudyGroup.PERSONAL_COMMITMENT.code, ScoringEngine.calculateGroup(responses))
    }

    @Test
    fun `group assignment - error case returns 99`() {
        val responses = createResponses()
        responses.setResponse("using_time_limits", 0)
        responses.setResponse("is_unito_student", 1)
        responses.setResponse("age_group", 1)
        responses.setResponse("interested_in_limit", 1)
        // No limit_method set -> falls to else
        assertEquals(99, ScoringEngine.calculateGroup(responses))
    }

    @Test
    fun `group assignment - STL takes priority over screening`() {
        // If using STL AND not a student, STL check comes first
        val responses = createResponses()
        responses.setResponse("using_time_limits", 1)
        responses.setResponse("is_unito_student", 0)
        responses.setResponse("age_group", 99)
        assertEquals(StudyGroup.INELIGIBLE_USING_STL.code, ScoringEngine.calculateGroup(responses))
    }

    // ========================================================================
    // Eligibility Tests
    // ========================================================================

    @Test
    fun `eligible participant`() {
        val responses = createResponses()
        responses.setResponse("is_unito_student", 1)
        responses.setResponse("age_group", 1)
        responses.setResponse("using_time_limits", 0)
        assertTrue(ScoringEngine.isEligible(responses))
    }

    @Test
    fun `ineligible - not UniTO student`() {
        val responses = createResponses()
        responses.setResponse("is_unito_student", 0)
        responses.setResponse("age_group", 1)
        responses.setResponse("using_time_limits", 0)
        assertFalse(ScoringEngine.isEligible(responses))
    }

    @Test
    fun `ineligible - age over 30`() {
        val responses = createResponses()
        responses.setResponse("is_unito_student", 1)
        responses.setResponse("age_group", 99)
        responses.setResponse("using_time_limits", 0)
        assertFalse(ScoringEngine.isEligible(responses))
    }

    @Test
    fun `ineligible - already using STL`() {
        val responses = createResponses()
        responses.setResponse("is_unito_student", 1)
        responses.setResponse("age_group", 1)
        responses.setResponse("using_time_limits", 1)
        assertFalse(ScoringEngine.isEligible(responses))
    }

    @Test
    fun `ineligible - multiple reasons`() {
        val responses = createResponses()
        responses.setResponse("is_unito_student", 0)
        responses.setResponse("age_group", 99)
        responses.setResponse("using_time_limits", 1)
        assertFalse(ScoringEngine.isEligible(responses))
    }

    // ========================================================================
    // Missing Response Tests (null handling)
    // ========================================================================

    @Test
    fun `missing responses default to 0`() {
        val responses = createResponses()
        // No responses set at all
        assertEquals(0, ScoringEngine.calculateBSMAS(responses))
        assertEquals(0, ScoringEngine.calculatePHQ9(responses))
        assertEquals(0, ScoringEngine.calculateGAD7(responses))
        assertEquals(0, ScoringEngine.calculateFoMO(responses))
        // PSS10 with all null: regular=0, reverse items=4*4=16
        assertEquals(16, ScoringEngine.calculatePSS10(responses))
    }
}
