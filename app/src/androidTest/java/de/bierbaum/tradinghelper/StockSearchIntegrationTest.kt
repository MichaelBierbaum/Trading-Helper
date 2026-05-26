package de.bierbaum.tradinghelper

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StockSearchIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testSearchForUbtechStock() {
        // 1. Warten bis der Splash-Screen (2 Sek) vorbei ist und die Watchlist erscheint
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithTag("add_stock_fab").fetchSemanticsNodes().isNotEmpty()
        }

        // 2. Klick auf den FAB um zur Suche zu gelangen
        composeTestRule.onNodeWithTag("add_stock_fab").performClick()

        // 3. Warten bis das Suchfeld erscheint
        val searchHint = composeTestRule.activity.getString(R.string.search_hint)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText(searchHint).fetchSemanticsNodes().isNotEmpty()
        }

        // 4. Suche nach der WKN "A4009U"
        // Dank der neuen Fallback-Logik im Repository sollte die WKN per Web-Suche in die ISIN 
        // "CNE100006CQ4" aufgelöst und Yahoo dann "Ubtech Robotics Corp Ltd" finden.
        composeTestRule.onNodeWithText(searchHint).performTextInput("A4009U")

        // 5. Warten bis das Ergebnis "Ubtech Robotics" erscheint
        composeTestRule.waitUntil(timeoutMillis = 30000) {
            composeTestRule.onAllNodesWithText("Ubtech Robotics", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // 6. Überprüfen, ob die Aktie korrekt angezeigt wird
        composeTestRule.onNodeWithText("Ubtech Robotics", substring = true, ignoreCase = true).assertIsDisplayed()
    }
}
