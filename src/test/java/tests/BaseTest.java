package tests;

import org.openqa.selenium.Dimension;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.ITestResult;

import java.lang.reflect.Method;

import utils.DriverFactory;

/**
 * Common test bootstrap/teardown.
 * - Initializes/quits the WebDriver once per test method.
 * - Accepts a TestNG parameter "viewport" (desktop/mobile) to run the same tests
 *   against different window sizes.
 * - Navigates to the Live Betting URL before each test.
 *
 * I keep the window sizing here (instead of inside each test) so all tests
 * start from a consistent viewport and page state.
 */
public class BaseTest {

    /** Exposed to child tests so they can branch assertions by viewport. */
    protected String viewport = "desktop";

    /** Entry point URL used across the suite. */
    protected static final String LIVE_URL = "https://sports.bwin.com/en/sports/live/betting";

    @BeforeMethod
    @Parameters({"viewport"})
    public void setUp(Method m, @Optional("desktop") String viewport) {
        this.viewport = viewport;
        Reporter.log("=== START: " + m.getName() + " [viewport=" + viewport + "] ===", true);

        // Start a fresh driver (incognito etc. is configured in DriverFactory)
        DriverFactory.initDriver();

        // Deterministic viewport per run.
        if ("mobile".equalsIgnoreCase(viewport)) {
            // ~iPhone 12-ish portrait size; enough to trigger mobile layout
            DriverFactory.getDriver().manage().window().setSize(new Dimension(390, 844));
        } else {
            // Common desktop viewport that triggers the desktop layout
            DriverFactory.getDriver().manage().window().setSize(new Dimension(1366, 900));
        }

        // Navigate to Live Betting entry page.
        DriverFactory.getDriver().get(LIVE_URL);
    }

    @AfterMethod
    public void tearDown(ITestResult result, Method m) {
        // Compact status logging in the report
        String status = switch (result.getStatus()) {
            case ITestResult.SUCCESS -> "PASS";
            case ITestResult.FAILURE -> "FAIL";
            case ITestResult.SKIP -> "SKIP";
            default -> "UNKNOWN";
        };
        Reporter.log("=== END: " + m.getName() + " => " + status + " ===", true);

        // Always quit to avoid driver/browser leaks between tests
        DriverFactory.quitDriver();
    }
}
