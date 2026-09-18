package com.sayvabr.assistentepedagogico.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TWO manually checked samples, NOT a declaration that the 1721-entry catalogue is official.
 * Compare full remaining dataset and all metadata before production release.
 *
 * Primary sources examined 2026-09-18:
 * https://basenacionalcomum.mec.gov.br/images/BNCC_EI_EF_110518_versaofinal_site.pdf
 *   EF01CI02: PDF pp.31 and 335 (illustration and canonical first-year skills table).
 * https://basenacionalcomum.mec.gov.br/images/historico/anexo_parecer_cneceb_n_2_2022_bncc_computacao.pdf
 *   EF01CO01: PDF p.16.
 */
@RunWith(AndroidJUnit4::class)
class BnccOfficialSampleInstrumentedTest {
    private val catalog: BnccCatalog
        get() = BnccCatalog.load(ApplicationProvider.getApplicationContext<Context>())

    @Test fun ef01ci02MatchesOfficialTextAndHasValidIllustrativeLocator() {
        val skill = requireNotNull(catalog.find("EF01CI02"))
        assertEquals("EF", skill.stage)
        assertFalse(skill.supplement)
        assertEquals(listOf(1), skill.years)
        assertEquals(
            "Localizar, nomear e representar graficamente (por meio de desenhos) partes do corpo humano e explicar suas funções.",
            skill.text
        )
        // The official PDF includes the same skill in both an illustrative table and its skill table.
        assertTrue(skill.sourcePage.contains("31"))
    }

    @Test fun ef01co01MatchesComputingSupplementTextAndDocumentLocator() {
        val skill = requireNotNull(catalog.find("EF01CO01"))
        assertEquals("EF", skill.stage)
        assertTrue(skill.supplement)
        assertEquals(listOf(1), skill.years)
        assertEquals(
            "Organizar objetos físicos ou digitais considerando diferentes características para esta organização, explicitando semelhanças (padrões) e diferenças.",
            skill.text
        )
        assertTrue(skill.sourcePage.contains("16"))
        assertTrue(skill.document.contains("computacao"))
    }
}
