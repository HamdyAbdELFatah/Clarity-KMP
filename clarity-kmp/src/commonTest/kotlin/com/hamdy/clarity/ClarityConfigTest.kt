package com.hamdy.clarity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ClarityConfigTest {
    @Test
    fun enabledConfigurationRequiresProjectId() {
        assertFailsWith<IllegalArgumentException> { ClarityConfig(projectId = " ") }
    }

    @Test
    fun disabledConfigurationAllowsMissingProjectId() {
        assertEquals("", ClarityConfig(projectId = "", enabled = false).projectId)
    }

    @Test
    fun initialMetadataIsValidated() {
        assertFailsWith<IllegalArgumentException> {
            ClarityConfig(projectId = "project", customTags = mapOf("tag" to emptySet()))
        }
    }

    @Test
    fun allLogLevelsAreStable() {
        assertEquals(
            listOf("None", "Error", "Warning", "Info", "Debug", "Verbose"),
            ClarityLogLevel.entries.map { it.name },
        )
    }
}
