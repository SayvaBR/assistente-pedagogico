package com.sayvabr.assistentepedagogico.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StudentRosterImportTest {
    @Test fun trimsNamesSkipsBlankLinesAndPreservesHomonyms() {
        assertEquals(
            listOf("Ana Silva", "Bruno Souza", "Ana Silva"),
            StudentRosterImport.parse("  Ana Silva  \r\n\r\nBruno Souza\nAna Silva\n"),
        )
    }

    @Test fun rejectsShortNameWithoutReturningAPartialList() {
        val error = reject { StudentRosterImport.parse("Ana Silva\nX\nBruno Souza") }
        assertTrue(error.contains("linha 2"))
        assertTrue(error.contains("Nenhum aluno foi adicionado"))
    }

    @Test fun rejectsMultipleColumnsInsteadOfSilentlyIgnoringStudentData() {
        val error = reject { StudentRosterImport.parse("Ana Silva\t5º A") }
        assertTrue(error.contains("somente os nomes"))
    }

    private fun reject(block: () -> Unit): String = try {
        block()
        fail("Expected invalid roster to be rejected")
        ""
    } catch (error: IllegalArgumentException) {
        error.message.orEmpty()
    }
}
