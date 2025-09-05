package tests;

import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;
import pages.BetslipPanel;
import pages.LiveBettingPage;
import utils.DriverFactory;

import java.time.Duration;

public class AddPickToBetslipTest extends BaseTest {

    @Test
    public void testAddPickToBetslip() {
        LiveBettingPage live = new LiveBettingPage(DriverFactory.getDriver());
        BetslipPanel betslip = new BetslipPanel(DriverFactory.getDriver());

        Reporter.log("[Betslip] Clicking first available outcome...", true);
        live.selectFirstOutcome();

        if ("mobile".equalsIgnoreCase(viewport)) {
            // Wait shortly for ANY success signal:
            //  - Bet Slip toggle/icon exists OR
            //  - Bet Slip panel visible OR
            //  - Pick itself is marked selected/pressed
            boolean accessible = new WebDriverWait(DriverFactory.getDriver(), Duration.ofSeconds(6))
                    .until(d -> {
                        BetslipPanel b = new BetslipPanel(DriverFactory.getDriver());
                        LiveBettingPage l = new LiveBettingPage(DriverFactory.getDriver());
                        return b.isTogglePresent() || b.isBetslipVisible() || l.isAnyPickSelectedQuick();
                    });

            // Diagnostics
            boolean toggle = betslip.isTogglePresent();
            boolean visible = betslip.isBetslipVisible();
            boolean selected = live.isAnyPickSelectedQuick();
            Reporter.log("[Betslip][Mobile] hasPicks=" + betslip.hasPicks() +
                    ", visible=" + visible + ", toggle=" + toggle + ", pickSelected=" + selected, true);

            Assert.assertTrue(
                    accessible,
                    "On mobile expect Bet Slip to be accessible (toggle or visible) OR the pick to be selected."
            );
            Reporter.log("[Betslip][Mobile] PASS: accessible/selected.", true);

        } else {
            // Desktop flow
            boolean visible = betslip.isBetslipVisible();
            boolean hasPicks = visible && betslip.hasPicks();
            Reporter.log("[Betslip][Desktop] Visible=" + visible + ", HasSelections=" + hasPicks, true);
            Assert.assertTrue(visible, "Bet Slip panel not visible (desktop).");
            Assert.assertTrue(hasPicks, "No selections in Bet Slip (desktop).");
            Reporter.log("[Betslip][Desktop] PASS: visible + has picks.", true);
        }
    }
}
