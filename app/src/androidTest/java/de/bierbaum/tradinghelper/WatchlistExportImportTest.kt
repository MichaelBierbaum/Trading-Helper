package de.bierbaum.tradinghelper

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WatchlistExportImportTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testExportAndImportWatchlist() {
        // 1. Warten bis Splash vorbei
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithTag("add_stock_fab").fetchSemanticsNodes().isNotEmpty()
        }

        val viewModel = composeTestRule.activity.viewModel

        // 2. Watchlist leeren für sauberen Teststart
        composeTestRule.runOnUiThread {
            val current = viewModel.watchlist.value
            current.forEach { viewModel.removeFromWatchlist(it) }
        }
        val emptyMsg = composeTestRule.activity.getString(R.string.empty_watchlist_msg)
        composeTestRule.onNodeWithText(emptyMsg).assertIsDisplayed()

        // 3. Aktie A4009U (Ubtech) hinzufügen über UI
        addStockByWkn("A4009U")
        
        // 4. Aktie A1JGSL (Nebius) hinzufügen über UI
        addStockByWkn("A1JGSL")

        // 5. Warten bis Namen geladen sind
        composeTestRule.waitUntil(timeoutMillis = 40000) {
            val watchlist = viewModel.watchlist.value
            watchlist.any { it.name.contains("Ubtech", ignoreCase = true) } &&
            watchlist.any { it.name.contains("Nebius", ignoreCase = true) }
        }

        // 6. Exportieren
        var jsonExport = ""
        composeTestRule.runOnUiThread {
            jsonExport = viewModel.exportWatchlistToJson()
        }
        assertTrue("Export JSON sollte nicht leer sein", jsonExport.length > 5)

        // 7. Watchlist leeren
        composeTestRule.runOnUiThread {
            viewModel.watchlist.value.forEach { viewModel.removeFromWatchlist(it) }
        }
        composeTestRule.onNodeWithText(emptyMsg).assertIsDisplayed()

        // 8. Importieren
        composeTestRule.runOnUiThread {
            viewModel.importWatchlistFromJson(jsonExport)
        }

        // 9. Prüfen ob Namen wieder erscheinen (nach automatischem Refresh beim Import)
        composeTestRule.waitUntil(timeoutMillis = 30000) {
            composeTestRule.onAllNodesWithText("Ubtech", substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty() &&
            composeTestRule.onAllNodesWithText("Nebius", substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Ubtech", substring = true, ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Nebius", substring = true, ignoreCase = true).assertIsDisplayed()
    }

    private fun addStockByWkn(wkn: String) {
        composeTestRule.onNodeWithTag("add_stock_fab").performClick()
        val searchHint = composeTestRule.activity.getString(R.string.search_hint)
        composeTestRule.onNodeWithText(searchHint).performTextInput(wkn)
        
        // Warten auf Suchergebnis
        composeTestRule.waitUntil(timeoutMillis = 20000) {
            composeTestRule.onAllNodesWithContentDescription("Hinzufügen").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Erstes Ergebnis hinzufügen
        composeTestRule.onAllNodesWithContentDescription("Hinzufügen").onFirst().performClick()
        
        // Zurück zur Watchlist
        composeTestRule.onNodeWithContentDescription("Zurück").performClick()
    }
}
