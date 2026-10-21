package com.ecodim.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTest {
    private fun at(h: Int, m: Int = 0) = h * 60 + m

    @Test
    fun sameDayWindow() {
        val s = Schedule(enabled = true, startMinute = at(9), endMinute = at(17))
        assertFalse(s.contains(at(8, 59)))
        assertTrue(s.contains(at(9)))
        assertTrue(s.contains(at(16, 59)))
        assertFalse(s.contains(at(17)))
    }

    @Test
    fun overnightWindow() {
        val s = Schedule(enabled = true, startMinute = at(22), endMinute = at(7))
        assertFalse(s.contains(at(21, 59)))
        assertTrue(s.contains(at(22)))
        assertTrue(s.contains(at(23, 59)))
        assertTrue(s.contains(at(0)))
        assertTrue(s.contains(at(6, 59)))
        assertFalse(s.contains(at(7)))
        assertFalse(s.contains(at(12)))
    }

    @Test
    fun emptyWindowNeverMatches() {
        val s = Schedule(enabled = true, startMinute = at(8), endMinute = at(8))
        assertFalse(s.contains(at(8)))
        assertFalse(s.contains(at(20)))
    }
}
