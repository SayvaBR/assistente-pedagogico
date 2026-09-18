package com.sayvabr.assistentepedagogico.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real bundled asset, never a remote endpoint or teacher data. */
@RunWith(AndroidJUnit4::class)
class BnccCatalogInstrumentedTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun fullSnapshotHasAllStagesComputingAndDocumentProvenance() {
        val catalog = BnccCatalog.load(context)
        assertEquals(1721, catalog.entries.size)
        assertEquals(141, catalog.entries.count { it.supplement })
        assertTrue(catalog.entries.any { it.stage == "EI" && !it.supplement })
        assertTrue(catalog.entries.any { it.stage == "EF" && !it.supplement })
        assertTrue(catalog.entries.any { it.stage == "EM" && !it.supplement })
        assertTrue(catalog.entries.all { it.sourceFile.isNotEmpty() && it.text.isNotEmpty() })
        assertTrue(catalog.attribution.contains("bncc.dev"))
        assertEquals(141, catalog.search(supplementOnly = true, limit = 500).size)
    }

    @Test fun lookupIsExactAndSearchAcceptsAccentsAndGradeFilters() {
        val catalog = BnccCatalog.load(context)
        val learning = requireNotNull(catalog.find(" ef05ma07 "))
        assertEquals("EF05MA07", learning.code)
        assertEquals("EF", learning.stage)
        assertTrue(learning.text.isNotBlank())
        assertTrue(catalog.search("EF05MA07", stage = "EF", year = 5).any { it.code == learning.code })
        assertFalse(catalog.search("EF05MA07", stage = "EF", year = 6).any { it.code == learning.code })
        assertTrue(catalog.search("adicao", stage = "EF", limit = 500).any { it.code == learning.code })
        assertNull(catalog.find("EF99ZZ99"))
    }

    @Test fun selectionParsingAndUnknownCodesAreDeterministic() {
        val catalog = BnccCatalog.load(context)
        assertEquals(listOf("EF05MA07", "EF67LP08"), BnccCatalog.parseCodes("ef05ma07, EF67LP08; ef05ma07"))
        assertEquals(listOf("CODIGO_INEXISTENTE"), catalog.unknownCodes("EF05MA07, CODIGO_INEXISTENTE"))
        assertTrue(catalog.unknownCodes("EF05MA07").isEmpty())
    }
}
