package com.sayvabr.assistentepedagogico.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppointmentEditorV5Test {
    @Test fun suggestsEndRelativeToActualLegacyStartNotFixedNineAm() {
        assertEquals("09:00", suggestedAppointmentEnd("08:00"))
        assertEquals("14:00", suggestedAppointmentEnd("13:00"))
        assertEquals("22:30", suggestedAppointmentEnd("21:30"))
    }

    @Test fun lateNightSuggestionDoesNotWrapIntoPreviousTime() {
        assertEquals("23:59", suggestedAppointmentEnd("23:10"))
        assertEquals("23:59", suggestedAppointmentEnd("23:59"))
    }
}
