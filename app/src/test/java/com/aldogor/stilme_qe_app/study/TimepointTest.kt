package com.aldogor.stilme_qe_app.study

import org.junit.Assert.assertEquals
import org.junit.Test

class TimepointTest {

    @Test
    fun `day 0 is T0`() {
        assertEquals(Timepoint.T0, Timepoint.fromDaysSinceBaseline(0))
    }

    @Test
    fun `day 29 is still T0`() {
        assertEquals(Timepoint.T0, Timepoint.fromDaysSinceBaseline(29))
    }

    @Test
    fun `day 30 is T1`() {
        assertEquals(Timepoint.T1, Timepoint.fromDaysSinceBaseline(30))
    }

    @Test
    fun `day 59 is still T1`() {
        assertEquals(Timepoint.T1, Timepoint.fromDaysSinceBaseline(59))
    }

    @Test
    fun `day 60 is T2`() {
        assertEquals(Timepoint.T2, Timepoint.fromDaysSinceBaseline(60))
    }

    @Test
    fun `day 89 is still T2`() {
        assertEquals(Timepoint.T2, Timepoint.fromDaysSinceBaseline(89))
    }

    @Test
    fun `day 90 is T3`() {
        assertEquals(Timepoint.T3, Timepoint.fromDaysSinceBaseline(90))
    }

    @Test
    fun `day 119 is still T3`() {
        assertEquals(Timepoint.T3, Timepoint.fromDaysSinceBaseline(119))
    }

    @Test
    fun `day 120 is T4`() {
        assertEquals(Timepoint.T4, Timepoint.fromDaysSinceBaseline(120))
    }

    @Test
    fun `day 200 is still T4`() {
        assertEquals(Timepoint.T4, Timepoint.fromDaysSinceBaseline(200))
    }

    @Test
    fun `fromIndex returns correct timepoints`() {
        assertEquals(Timepoint.T0, Timepoint.fromIndex(0))
        assertEquals(Timepoint.T1, Timepoint.fromIndex(1))
        assertEquals(Timepoint.T2, Timepoint.fromIndex(2))
        assertEquals(Timepoint.T3, Timepoint.fromIndex(3))
        assertEquals(Timepoint.T4, Timepoint.fromIndex(4))
        assertEquals(null, Timepoint.fromIndex(5))
        assertEquals(null, Timepoint.fromIndex(-1))
    }

    @Test
    fun `redcapEventName correct for each timepoint`() {
        assertEquals("baseline_arm_1", Timepoint.T0.redcapEventName)
        assertEquals("followup_1_arm_1", Timepoint.T1.redcapEventName)
        assertEquals("followup_2_arm_1", Timepoint.T2.redcapEventName)
        assertEquals("followup_3_arm_1", Timepoint.T3.redcapEventName)
        assertEquals("followup_4_arm_1", Timepoint.T4.redcapEventName)
    }

    @Test
    fun `daysFromBaseline correct for each timepoint`() {
        assertEquals(0, Timepoint.T0.daysFromBaseline)
        assertEquals(30, Timepoint.T1.daysFromBaseline)
        assertEquals(60, Timepoint.T2.daysFromBaseline)
        assertEquals(90, Timepoint.T3.daysFromBaseline)
        assertEquals(120, Timepoint.T4.daysFromBaseline)
    }

    // ------------------------------------------------------------------------
    // earliestDue — the missed-window fix: a skipped 30-day window must still be
    // offerable, not permanently skipped.
    // ------------------------------------------------------------------------

    @Test
    fun `earliestDue is T0 when nothing completed, regardless of current window`() {
        assertEquals(Timepoint.T0, Timepoint.earliestDue(currentIndex = 0, completed = emptySet()))
        // Even if the date says we're in T2's window, an incomplete T0 is offered first.
        assertEquals(Timepoint.T0, Timepoint.earliestDue(currentIndex = 2, completed = emptySet()))
    }

    @Test
    fun `earliestDue advances as earlier timepoints are completed`() {
        assertEquals(Timepoint.T1, Timepoint.earliestDue(currentIndex = 1, completed = setOf(0)))
        assertEquals(Timepoint.T2, Timepoint.earliestDue(currentIndex = 2, completed = setOf(0, 1)))
    }

    @Test
    fun `earliestDue offers a skipped window instead of jumping ahead`() {
        // Participant completed T0, missed T1's window, opens the app during T2's window.
        // The date-derived current timepoint is T2, but T1 was never completed — so T1 must
        // be offered (previously this returned T2 and T1 was lost forever).
        assertEquals(Timepoint.T1, Timepoint.earliestDue(currentIndex = 2, completed = setOf(0)))
    }

    @Test
    fun `earliestDue offers earliest of multiple skipped windows`() {
        // Completed only T0; missed T1, T2, T3; now in T4's window. Offer T1 first.
        assertEquals(Timepoint.T1, Timepoint.earliestDue(currentIndex = 4, completed = setOf(0)))
    }

    @Test
    fun `earliestDue falls back to current index when all opened windows complete`() {
        // All windows up to current are done — nothing earlier is due, stay at current.
        assertEquals(Timepoint.T3, Timepoint.earliestDue(currentIndex = 3, completed = setOf(0, 1, 2, 3)))
    }
}
