package family.safe

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Run on a fresh family.safe.debug installation; never clears a family's app data. */
class FamilyFlowTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    @Test fun setupApproveTopicSearchRequestAndRecreationLock() {
        ui.waitUntil(10000) { ui.activity.settings != null }
        check(ui.activity.settings?.pinHash == null) { "Use a fresh debug install for this setup test." }
        ui.onNodeWithText("Parent PIN").performTextInput("732918")
        ui.onNodeWithText("Confirm PIN").performTextInput("732918")
        ui.onNodeWithText("Create PIN").performClick()
        ui.waitUntil(30000) { ui.activity.parent && !ui.activity.busy }
        ui.runOnIdle { ui.activity.update { state -> state.copy(categories=state.categories.map{ if(it.id=="science") it.copy(enabled=true) else it }) } }
        ui.waitUntil(10000) { !ui.activity.busy }
        ui.onNodeWithText("Lock and return home").performClick()
        ui.onNodeWithText("What would you like to learn?").performTextInput("plant cells")
        ui.onNodeWithText("Explore",useUnmergedTree=true).performClick()
        ui.onNodeWithText("Video discovery is not set up. Explore an approved website below.").assertIsDisplayed()
        ui.onNodeWithText("What would you like to learn?").performTextReplacement("unknown entertainment")
        ui.onNodeWithText("Explore",useUnmergedTree=true).performClick()
        ui.onNodeWithText("Ask my parent").performClick()
        ui.waitUntil(10000) { ui.activity.settings!!.requests.isNotEmpty() && !ui.activity.busy }
        ui.activityRule.scenario.recreate()
        ui.waitUntil(10000) { ui.activity.settings != null }
        ui.runOnIdle {
            assertFalse(ui.activity.parent)
            assertTrue(ui.activity.settings!!.categories.single{it.id=="science"}.enabled)
            assertEquals("unknown entertainment",ui.activity.settings!!.requests.single().text)
        }
        ui.onNodeWithText("Parent settings").performClick()
        ui.onNodeWithText("Parent PIN").assertExists()
    }
}
