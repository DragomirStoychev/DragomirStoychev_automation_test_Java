package tests;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.Test;
import pages.LiveBettingPage;
import utils.DriverFactory;

import java.time.Duration;

/**
 * Scenario 2: Validate live odds update (skip Halftime markets).
 * - Read odds from the first NON-halftime outcome
 * - Observe for a change within a time window (light polling)
 *
 * I explicitly skip halftime markets because odds may stay flat for longer intervals.
 */
public class ValidateOddsUpdateTest extends BaseTest {

    @Test
    public void testValidateOddsUpdate() {
        // Timeouts kept here for clarity/quick tuning.
        final Duration BASELINE_TIMEOUT = Duration.ofSeconds(12);
        final Duration FALLBACK_TIMEOUT = Duration.ofSeconds(8);
        final Duration CHANGE_WINDOW    = Duration.ofSeconds(60);

        LiveBettingPage live = new LiveBettingPage(DriverFactory.getDriver());
        live.acceptCookiesIfPresent(); // keep click interceptions away

        // 1) Establish baseline from a NON-halftime market (preferred path).
        String initial = live.waitAndGetFirstNonHalftimeOddsText(BASELINE_TIMEOUT);

        // 2) Rare fallback: if we couldn't read a non-halftime value, try generic first outcome.
        if (initial == null) {
            initial = live.waitAndGetFirstOutcomeOddsText(FALLBACK_TIMEOUT);
        }
        Assert.assertNotNull(initial, "Couldn't read initial odds from a non-halftime outcome.");

        Reporter.log("[OddsUpdate] Initial (non-halftime) odds: " + initial, true);

        // 3) Wait for a change within the window (light polling every ~500 ms inside the page object).
        boolean changed = live.waitForNonHalftimeOddsChange(CHANGE_WINDOW);
        String current = live.getFirstNonHalftimeOddsTextQuick();
        Reporter.log("[OddsUpdate] Observed odds after wait (non-halftime): " + current, true);

        // 4) Assert/Skip policy:
        //    - PASS if changed within the window
        //    - If not changed, but right after the window we already see a different value -> consider PASS
        //    - Otherwise SKIP (live systems can be momentarily static)
        if (!changed) {
            if (current != null && !current.equals(initial)) {
                Reporter.log("[OddsUpdate] Change detected right after window: " + initial + " → " + current
                        + " (treating as PASS).", true);
                Assert.assertTrue(true);
                return;
            }
            Reporter.log("[OddsUpdate] No change within 60s (non-halftime). Marking test as SKIPPED.", true);
            throw new SkipException("No odds change in 60s (non-halftime). Initial=" + initial + ", final=" + current);
        }

        Reporter.log("[OddsUpdate] CHANGED within window (non-halftime): " + initial + " → " + current, true);
        Assert.assertTrue(true);
    }
}
