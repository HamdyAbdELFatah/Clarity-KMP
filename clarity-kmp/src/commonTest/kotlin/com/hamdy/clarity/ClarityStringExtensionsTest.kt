package com.hamdy.clarity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClarityStringExtensionsTest {

    @Test
    fun isValidClarityValueAcceptsNonBlankWithinLimit() {
        assertTrue("abc".isValidClarityValue())
        assertTrue("x".repeat(255).isValidClarityValue())
    }

    @Test
    fun isValidClarityValueRejectsBlankOrOverLimit() {
        assertFalse("".isValidClarityValue())
        assertFalse("   ".isValidClarityValue())
        assertFalse("x".repeat(256).isValidClarityValue())
    }

    @Test
    fun isValidClarityEventNameAcceptsNonBlankWithinLimit() {
        assertTrue("abc".isValidClarityEventName())
        assertTrue("x".repeat(254).isValidClarityEventName())
    }

    @Test
    fun isValidClarityEventNameRejectsBlankOrOverLimit() {
        assertFalse("".isValidClarityEventName())
        assertFalse("   ".isValidClarityEventName())
        assertFalse("x".repeat(255).isValidClarityEventName())
    }

    @Test
    fun truncatedToClarityValueCapsAt255() {
        assertEquals("x".repeat(255), "x".repeat(500).truncatedToClarityValue())
        assertEquals("short", "short".truncatedToClarityValue())
    }

    @Test
    fun truncatedToClarityEventNameCapsAt254() {
        assertEquals("x".repeat(254), "x".repeat(500).truncatedToClarityEventName())
        assertEquals("short", "short".truncatedToClarityEventName())
    }
}
