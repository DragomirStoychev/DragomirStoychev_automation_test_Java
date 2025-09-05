package tests;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;
import pages.AZSportsPage;
import pages.LiveBettingPage;
import utils.DriverFactory;

/**
 * Scenario: A–Z Sports navigation.
 * I validate that I can open the A–Z panel, pick a sport, and land on the correct sport page.
 * I accept the page as "loaded" if either the URL matches the expected slug or the tab/header is active.
 */
public class CheckSportSortingTest extends BaseTest {

    @Test
    public void testAZNavigateToFootball() {
        // I start from the generic Sports landing (not the live page),
        // because the A–Z entry point sits there clearly in the header.
        DriverFactory.getDriver().get("https://sports.bwin.com/en/sports");

        // Handle the cookie banner once to avoid click interception later.
        new LiveBettingPage(DriverFactory.getDriver()).acceptCookiesIfPresent();

        AZSportsPage az = new AZSportsPage(DriverFactory.getDriver());

        // Open the A–Z panel only if it's not already visible.
        az.openAZIfNeeded();
        Reporter.log("[A-Z] Opened A-Z Sports.", true);

        // Select a specific sport by its visible name.
        // If the exact text match isn't clickable, the page object falls back to the href slug.
        String sport = "Football";
        az.selectSportByName(sport);
        Reporter.log("[A-Z] Selected sport: " + sport, true);

        // Assert that the respective sport page actually loaded.
        // The page object accepts URL match OR active tab/header to stay resilient to small UI changes.
        boolean loaded = az.isSportPageLoaded(sport);
        Assert.assertTrue(loaded, "Sport page not recognized as loaded for: " + sport);
        Reporter.log("[A-Z] PASS: Sport page loaded for " + sport, true);
    }
}
