package tests;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;
import pages.BetslipPanel;
import pages.LiveBettingPage;
import utils.DriverFactory;

/**
 * Adds a live pick and validates Bet Slip behavior per viewport.
 * Desktop  -> Bet Slip panel is visible and contains the selection.
 * Mobile   -> Bet Slip is hidden behind a toggle (or collapsed).
 *
 * Note: `viewport` comes from BaseTest via TestNG parameter (desktop/mobile).
 */
public class AddPickToBetslipTest extends BaseTest {

    @Test
    public void testAddPickToBetslip() {
        // Arrange: page objects on the current driver instance
        LiveBettingPage live = new LiveBettingPage(DriverFactory.getDriver());
        BetslipPanel betslip = new BetslipPanel(DriverFactory.getDriver());

        // Act: click the first available live outcome (cookie banner is handled inside)
        Reporter.log("[Betslip] Clicking first available outcome...", true);
        live.selectFirstOutcome();

        // Assert: adapt expectations to viewport behavior
        if ("mobile".equalsIgnoreCase(viewport)) {
            // On mobile the right panel is collapsed, so I only expect a visible toggle
            boolean toggle = betslip.isTogglePresent();
            Assert.assertTrue(toggle, "On mobile expect Bet Slip toggle after selecting a pick.");
            Reporter.log("[Betslip][Mobile] PASS: toggle present.", true);
        } else {
            // On desktop the right panel must be visible and contain at least one selection
            boolean visible = betslip.isBetslipVisible();
            boolean hasPicks = visible && betslip.hasPicks();

            Reporter.log("[Betslip][Desktop] Visible=" + visible + ", HasSelections=" + hasPicks, true);
            Assert.assertTrue(visible, "Bet Slip panel not visible (desktop).");
            Assert.assertTrue(hasPicks, "No selections in Bet Slip (desktop).");
            Reporter.log("[Betslip][Desktop] PASS: visible + has picks.", true);
        }
    }
}
