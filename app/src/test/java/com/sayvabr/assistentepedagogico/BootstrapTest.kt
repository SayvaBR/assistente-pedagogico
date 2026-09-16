package com.sayvabr.assistentepedagogico

import com.sayvabr.assistentepedagogico.domain.EducationStage
import com.sayvabr.assistentepedagogico.domain.FreePlanPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootstrapTest {
    @Test fun onlyThreeVisibleEducationStagesExist() {
        assertEquals(3, EducationStage.entries.size)
    }

    @Test fun freePlanAllowsFirstAndSecondClassOnly() {
        assertTrue(FreePlanPolicy.mayCreateActiveClass(0))
        assertTrue(FreePlanPolicy.mayCreateActiveClass(1))
        assertFalse(FreePlanPolicy.mayCreateActiveClass(2))
        assertFalse(FreePlanPolicy.mayCreateActiveClass(3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun freePlanRejectsInvalidCount() {
        FreePlanPolicy.mayCreateActiveClass(-1)
    }
}
