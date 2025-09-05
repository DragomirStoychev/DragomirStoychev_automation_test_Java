package utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Single place to create/cleanup WebDriver instances.
 * I keep it ThreadLocal so parallel runs can be enabled later without refactor.
 */
public class DriverFactory {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    /** Get the current thread's driver instance. */
    public static WebDriver getDriver() {
        return DRIVER.get();
    }

    /**
     * Create a fresh ChromeDriver with sane defaults:
     * - incognito (per requirement)
     * - maximized (unless headless)
     * - optional language and headless via system properties
     */
    public static void initDriver() {
        // Optional knobs (safe defaults):
        // -Dsite.lang=en|bg
        final String browserLang = System.getProperty("site.lang", "en");
        // -Dheadless=true to run in CI
        final boolean headless = Boolean.parseBoolean(System.getProperty("headless", "false"));

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.addArguments("--incognito");          // requirement
        options.addArguments("--lang=" + browserLang);

        // Hint Chrome to prefer our language (helps some UIs)
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("intl.accept_languages", browserLang + "," + browserLang + "_" + browserLang.toUpperCase() + ",en");
        options.setExperimentalOption("prefs", prefs);

        if (headless) {
            // Use new headless mode; set a window size so responsive checks are deterministic
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1366,900");
        } else {
            // In headed mode we'll just maximize after driver creation
            options.addArguments("--start-maximized");
        }

        // A couple of harmless quality-of-life flags (don’t impact the tests)
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-infobars");

        DRIVER.set(new ChromeDriver(options));

        // Maximize only when not headless (headless ignores it anyway)
        if (!headless) {
            getDriver().manage().window().maximize();
        }
    }

    /** Quit and clean up. Always removes the ThreadLocal reference. */
    public static void quitDriver() {
        WebDriver d = DRIVER.get();
        if (d != null) {
            try {
                d.quit();
            } finally {
                DRIVER.remove();
            }
        }
    }
}
