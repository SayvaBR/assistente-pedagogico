package com.sayvabr.assistentepedagogico.ui

import android.content.Context
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sayvabr.assistentepedagogico.data.BnccCatalog
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the shipped catalogue and synthetic selections only, with no screenshots or teacher records. */
@RunWith(AndroidJUnit4::class)
class BnccPickerRestorationInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unsavedSelectionAndSearchSurviveStateRestoreUntilExplicitConfirmation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assumeTrue(Build.HARDWARE.contains("ranchu", true) || Build.HARDWARE.contains("goldfish", true) ||
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator", true))
        val catalog = BnccCatalog.load(context)
        var confirmed: List<String>? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ApTheme {
                BnccPicker(catalog, emptyList(), onConfirm = { confirmed = it }, onDismiss = {})
            }
        }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("EF05MA07")
        // The first match is the search input; the last is the unique result row.
        compose.onAllNodesWithText("EF05MA07", substring = true).onLast().performClick()
        compose.onNodeWithText("1 selecionada(s)", substring = true).assertExists()
        assertEquals(null, confirmed)

        restoration.emulateSavedInstanceStateRestore()
        compose.onAllNodesWithText("EF05MA07", substring = true).onLast().assertExists()
        compose.onNodeWithText("1 selecionada(s)", substring = true).assertExists()
        assertEquals(null, confirmed)
        compose.onNodeWithText("Usar selecionadas").performClick()
        assertEquals(listOf("EF05MA07"), confirmed)
    }

    @Test fun switchingFromFundamentalClearsGradeConstraintForComputing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val catalog = BnccCatalog.load(context)
        compose.setContent { ApTheme { BnccPicker(catalog, emptyList(), {}, {}) } }
        compose.onNodeWithText("Fundamental").performClick()
        // Query is input #0; the grade field appears as input #1 only in Fundamental.
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("5")
        // The final filter is off-screen in the horizontal LazyRow on small phones.
        // A real user has to scroll that row first; directly searching for an uncomposed
        // LazyRow item caused a false failure even though the search implementation worked.
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(4)
        compose.onNodeWithText("Computação").performClick()
        compose.onNodeWithText("0 selecionada(s) · 100 resultado(s) exibidos").assertExists()
    }
}
