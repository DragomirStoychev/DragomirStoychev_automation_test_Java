package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.text.Normalizer;
import java.time.Duration;
import java.util.List;

/**
 * Lightweight helper around the A–Z Sports navigation.
 * I keep the locators generic so small DOM tweaks won't break the flow.
 */
public class AZSportsPage extends BasePage {

    // Clickable control that opens the A–Z panel.
    // I match by visible text (not a brittle id/class) and allow different tag types.
    private final By azTab = By.xpath(
            "//*[self::a or self::button or self::div or self::span]" +
                    "[contains(normalize-space(.),'A-Z Sports')]"
    );

    // Once the A–Z panel is open, this header is present.
    // I use it as an anchor to know the panel is actually rendered.
    private final By azHeader = By.xpath(
            "//*[self::h1 or self::h2 or self::h3 or self::div or self::span]" +
                    "[normalize-space(.)='A-Z Sports' or contains(normalize-space(.),'A-Z Sports')]"
    );

    public AZSportsPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Open the A–Z panel only if it's not already visible.
     * - If the header is present, I do nothing.
     * - Otherwise I click the tab and wait for the header.
     */
    public void openAZIfNeeded() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(azHeader));
        } catch (TimeoutException e) {
            WebElement tab = new WebDriverWait(driver, Duration.ofSeconds(6))
                    .until(ExpectedConditions.elementToBeClickable(azTab));
            tab.click();

            // Wait until the A–Z content is actually loaded.
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.presenceOfElementLocated(azHeader));
        }
    }

    /**
     * Convert a sport name to the slug used in bwin URLs.
     * Example: "Ice Hockey" -> "ice-hockey".
     * - lowercase
     * - strip diacritics
     * - replace non-alphanumerics with a single dash
     */
    private String slugify(String name) {
        String s = name == null ? "" : name.toLowerCase().trim();
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+|-+$", "");
        return s;
    }

    /**
     * Select a sport inside the A–Z panel by visible text.
     * I first try an exact text match; if that fails, I fall back to an <a href="/en/sports/<slug>">.
     * If neither path works, I fail fast with a clear message.
     */
    public void selectSportByName(String sportName) {
        openAZIfNeeded();
        String target = sportName.trim();

        // 1) Try matching the visible text (fast path).
        List<WebElement> byText = driver.findElements(
                By.xpath("(.//*[normalize-space(.)='" + target + "'])[1]")
        );
        for (WebElement el : byText) {
            if (el.isDisplayed()) {
                try {
                    el.click();
                    return;
                } catch (Exception ignored) {
                    // If the node is overlayed or stale, try the fallback below.
                }
            }
        }

        // 2) Fallback: click by URL slug (more tolerant to nested markup).
        String slug = slugify(target);
        List<WebElement> byHref = driver.findElements(
                By.xpath("//a[contains(@href,'/en/sports/" + slug + "')]")
        );
        for (WebElement el : byHref) {
            if (el.isDisplayed()) {
                el.click();
                return;
            }
        }

        throw new NoSuchElementException("Sport not found in A-Z: " + sportName);
    }

    /**
     * Check that the respective sport page is loaded.
     * I don't rely on a single signal; I accept:
     *  - URL contains /en/sports/<slug>  OR
     *  - a tab/header for the sport is present and looks active/selected.
     * This keeps the assertion robust across minor UI changes.
     */
    public boolean isSportPageLoaded(String sportName) {
        String slug = slugify(sportName);
        boolean urlOk = driver.getCurrentUrl()
                .toLowerCase()
                .contains("/en/sports/" + slug);

        // Active tab/header patterns (class "active/selected" or aria-selected).
        By activeTab = By.xpath(
                "//*[normalize-space(.)='" + sportName + "']" +
                        "[contains(@class,'active') or contains(@class,'selected') or @aria-selected='true' or self::h1 or self::h2]"
        );

        boolean tabVisible = false;
        try {
            WebElement el = new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.presenceOfElementLocated(activeTab));
            tabVisible = el.isDisplayed();
        } catch (TimeoutException ignored) {
            // If the tab isn't found, we fall back to the URL check only.
        }

        // I consider the page loaded if either URL or visual indicator confirms it.
        return urlOk || tabVisible;
    }
}
