package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Live Betting page object.
 * I avoid JS and stick to explicit waits + Actions for stable clicks.
 */
public class LiveBettingPage extends BasePage {

    // OneTrust cookie banner pieces — I handle it once per session/page.
    private final By cookieBanner = By.id("onetrust-banner-sdk");
    private final By cookieAcceptButton = By.cssSelector("button#onetrust-accept-btn-handler");

    // Broad selector for outcome "buttons" (Angular custom element).
    private final By outcomeButtons = By.cssSelector("ms-event-pick");

    public LiveBettingPage(WebDriver driver) { super(driver); }

    /**
     * Dismiss the cookie banner safely if present.
     * Strategy: wait visible → wait clickable → Actions.click → wait to disappear.
     * I keep fallbacks for intercepted/stale cases without resorting to JS.
     */
    public void acceptCookiesIfPresent() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(cookieBanner));
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(cookieAcceptButton));
            new Actions(driver).moveToElement(btn).pause(Duration.ofMillis(120)).click().perform();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(cookieBanner));
        } catch (TimeoutException ignore) {
            // No banner -> nothing to do.
        } catch (ElementClickInterceptedException e) {
            // Fallback: ENTER on the button (keeps it JS-free).
            try {
                WebElement btn = driver.findElement(cookieAcceptButton);
                btn.sendKeys(Keys.ENTER);
                wait.until(ExpectedConditions.invisibilityOfElementLocated(cookieBanner));
            } catch (Exception ignored) { }
        } catch (StaleElementReferenceException e) {
            // Fallback: re-fetch and click again.
            try {
                WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(cookieAcceptButton));
                new Actions(driver).moveToElement(btn).click().perform();
                wait.until(ExpectedConditions.invisibilityOfElementLocated(cookieBanner));
            } catch (Exception ignored) { }
        }
    }

    /**
     * Returns the first *visible* outcome.
     * I re-find the list to avoid stale references caused by live updates.
     */
    private WebElement findFirstVisibleOutcome(long timeoutSec) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSec));
        List<WebElement> all = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(outcomeButtons));
        for (WebElement el : all) {
            try {
                if (el.isDisplayed()) return el;
            } catch (StaleElementReferenceException ignored) { }
        }
        // As a last resort I return the first; caller will retry on errors.
        return all.get(0);
    }

    /**
     * Click the first available outcome in a robust way.
     * Steps: cookie → find → wait clickable → Actions.click.
     * I retry a few times on stale/intercepted since the DOM is live.
     */
    public void selectFirstOutcome() {
        acceptCookiesIfPresent();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement pick = findFirstVisibleOutcome(15);
                wait.until(ExpectedConditions.elementToBeClickable(pick));

                new Actions(driver).moveToElement(pick)
                        .pause(Duration.ofMillis(150))
                        .click()
                        .perform();

                // Give the DOM a moment — either it re-renders or keeps the node.
                try {
                    wait.until(ExpectedConditions.or(
                            ExpectedConditions.stalenessOf(pick),
                            ExpectedConditions.visibilityOf(pick)
                    ));
                } catch (StaleElementReferenceException ignored) { }
                return;
            } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                // Re-try with a fresh reference.
            }
        }
        throw new RuntimeException("Failed to click an outcome after retries.");
    }

    /**
     * Extract the first decimal-looking number from a text.
     * I normalize commas to dots to keep comparisons consistent.
     */
    private String extractOddsNumber(String text) {
        if (text == null) return null;
        String t = text.replace('\n', ' ').trim();
        StringBuilder num = new StringBuilder();
        boolean seenDigit = false;

        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isDigit(c)) {
                num.append(c);
                seenDigit = true;
            } else if ((c == '.' || c == ',') && seenDigit) {
                num.append('.'); // normalize comma to dot
            } else if (seenDigit) {
                break;
            }
        }
        String out = num.toString();
        return out.isEmpty() ? null : out;
    }

    /**
     * Read odds text from an outcome root and its common descendants/attributes.
     * This is intentionally tolerant to markup changes.
     */
    private String readOddsFromPick(WebElement root) {
        try {
            String v = extractOddsNumber(root.getAttribute("textContent"));
            if (v != null) return v;

            v = extractOddsNumber(root.getAttribute("aria-label"));
            if (v != null) return v;

            List<WebElement> descendants = root.findElements(
                    By.cssSelector("span,div,strong,em,b,i,small,p,button")
            );
            for (WebElement d : descendants) {
                try {
                    v = extractOddsNumber(d.getText());
                    if (v != null) return v;

                    v = extractOddsNumber(d.getAttribute("textContent"));
                    if (v != null) return v;

                    v = extractOddsNumber(d.getAttribute("aria-label"));
                    if (v != null) return v;
                } catch (StaleElementReferenceException ignored) { }
            }
        } catch (StaleElementReferenceException ignored) { }
        return null;
    }

    /** Quick read (no heavy waits): first visible outcome’s odds or null right now. */
    public String getFirstOutcomeOddsTextQuick() {
        List<WebElement> picks = driver.findElements(outcomeButtons);
        for (WebElement el : picks) {
            try {
                if (!el.isDisplayed()) continue;
                String v = readOddsFromPick(el);
                if (v != null && !v.isEmpty()) return v;
            } catch (StaleElementReferenceException ignored) { }
        }
        return null;
    }

    /** Wait up to `timeout` to obtain first visible outcome odds (light polling). */
    public String waitAndGetFirstOutcomeOddsText(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                String v = getFirstOutcomeOddsTextQuick();
                if (v != null && !v.isEmpty()) return v;
            } catch (StaleElementReferenceException ignored) { }
            try { Thread.sleep(500); } catch (InterruptedException ignored) { }
        }
        return null;
    }

    /** Block until the first outcome’s odds change, using light polling. */
    public boolean waitForFirstOutcomeOddsChange(Duration timeout) {
        final String baseline = waitAndGetFirstOutcomeOddsText(Duration.ofSeconds(10));
        if (baseline == null) return false;

        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                String current = getFirstOutcomeOddsTextQuick();
                if (current != null && !current.equals(baseline)) return true;
            } catch (StaleElementReferenceException ignored) { }
            try { Thread.sleep(500); } catch (InterruptedException ignored) { }
        }
        return false;
    }

    /**
     * Heuristic: returns true if the surrounding context suggests a half-time market.
     * I scan nearest container/parent text for common EN/BG tokens.
     */
    private boolean isHalftimeContextFor(WebElement pick) {
        final String[] tokens = new String[] {
                "halftime", "half time", "1st half", "2nd half", "first half", "second half",
                "half-time", "ht",
                "първо полувреме", "второ полувреме", "полувреме"
        };

        String ctx = null;

        // Try a nearest “market” container.
        try {
            WebElement container = pick.findElement(By.xpath(
                    "ancestor::*[self::ms-option-group or self::ms-market or self::section or self::div][1]"
            ));
            ctx = container.getText();
            if (ctx == null || ctx.trim().isEmpty()) {
                ctx = container.getAttribute("textContent");
            }
        } catch (NoSuchElementException ignored) { }

        // Fallback: parent node.
        if (ctx == null || ctx.trim().isEmpty()) {
            try {
                WebElement parent = pick.findElement(By.xpath(".."));
                ctx = parent.getText();
                if (ctx == null || ctx.trim().isEmpty()) {
                    ctx = parent.getAttribute("textContent");
                }
            } catch (Exception ignored) { }
        }

        if (ctx == null) return false;
        String lc = ctx.toLowerCase();
        for (String t : tokens) {
            if (lc.contains(t)) return true;
        }
        return false;
    }

    /** Quick: first visible NON-halftime outcome odds, or null if none right now. */
    public String getFirstNonHalftimeOddsTextQuick() {
        List<WebElement> picks = driver.findElements(outcomeButtons);
        for (WebElement el : picks) {
            try {
                if (!el.isDisplayed()) continue;
                if (isHalftimeContextFor(el)) continue; // skip half-time markets
                String v = readOddsFromPick(el);
                if (v != null && !v.isEmpty()) return v;
            } catch (StaleElementReferenceException ignored) { }
        }
        return null;
    }

    /** Wait up to `timeout` for a NON-halftime odds value (light polling). */
    public String waitAndGetFirstNonHalftimeOddsText(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                String v = getFirstNonHalftimeOddsTextQuick();
                if (v != null && !v.isEmpty()) return v;
            } catch (StaleElementReferenceException ignored) { }
            try { Thread.sleep(500); } catch (InterruptedException ignored) { }
        }
        return null;
    }

    /** Block until a NON-halftime odds value changes (light polling). */
    public boolean waitForNonHalftimeOddsChange(Duration timeout) {
        final String baseline = waitAndGetFirstNonHalftimeOddsText(Duration.ofSeconds(12));
        if (baseline == null) return false;

        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                String current = getFirstNonHalftimeOddsTextQuick();
                if (current != null && !current.equals(baseline)) return true;
            } catch (StaleElementReferenceException ignored) { }
            try { Thread.sleep(500); } catch (InterruptedException ignored) { }
        }
        return false;
    }
}
