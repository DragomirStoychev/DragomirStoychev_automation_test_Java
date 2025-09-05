package tests;

import org.openqa.selenium.Dimension;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;
import pages.BetslipPanel;
import pages.LiveBettingPage;
import utils.DriverFactory;

/**
 * Scenario 4: Responsive behavior.
 * Desktop: betslip visible after adding a selection.
 * Mobile viewport: betslip collapses (toggle present or panel not visible).
 */
public class ResponsiveDesignTest extends BaseTest {

    @Test
    public void testResponsiveBetslipBehavior() {
        LiveBettingPage live = new LiveBettingPage(DriverFactory.getDriver());
        BetslipPanel betslip = new BetslipPanel(DriverFactory.getDriver());

        // Desktop size
        DriverFactory.getDriver().manage().window().setSize(new Dimension(1366, 900));

        live.acceptCookiesIfPresent();
        live.selectFirstOutcome(); // ensure betslip has something to show

        boolean desktopVisible = betslip.isBetslipVisible();
        Reporter.log("[Responsive] Desktop visible=" + desktopVisible, true);
        Assert.assertTrue(desktopVisible, "Betslip should be visible on desktop after a selection.");

        // Mobile size (e.g., iPhone 12-ish)
        DriverFactory.getDriver().manage().window().setSize(new Dimension(390, 844));

        // Short settle
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        boolean mobileVisible = betslip.isBetslipVisible();
        boolean mobileToggle = betslip.isTogglePresent();

        Reporter.log("[Responsive] Mobile visible=" + mobileVisible + ", toggle=" + mobileToggle, true);

        // Expectation: on mobile, panel is hidden OR behind a toggle
        Assert.assertTrue(!mobileVisible || mobileToggle,
                "On mobile, betslip should be hidden or toggled. Got visible=" + mobileVisible + ", toggle=" + mobileToggle);

        Reporter.log("[Responsive] PASS: Desktop shows betslip; Mobile hides or toggles it.", true);
    }
}
