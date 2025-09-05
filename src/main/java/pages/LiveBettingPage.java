package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object for the Live Betting page (no JS; waits + Actions).
 */
public class LiveBettingPage extends BasePage {

    // OneTrust cookie banner
    private final By cookieBanner = By.id("onetrust-banner-sdk");
    private final By cookieAcceptButton = By.cssSelector("button#onetrust-accept-btn-handler");

    // Broad selector for outcome buttons
    private final By outcomeButtons = By.cssSelector("ms-event-pick");

    public LiveBettingPage(WebDriver driver) { super(driver); }

    /** Dismiss cookie banner safely if present. */
    public void acceptCookiesIfPresent() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            // 1) visible banner?
            wait.until(ExpectedConditions.visibilityOfElementLocated(cookieBanner));
            // 2) clickable button
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(cookieAcceptButton));
            // 3) Actions click (auto scroll)
            new Actions(driver).moveToElement(btn).pause(Duration.ofMillis(120)).click().perform();
            // 4) ensure it vanished
            wait.until(ExpectedConditions.invisibilityOfElementLocated(cookieBanner));
        } catch (TimeoutException ignore) {
            // no banner -> noop
        } catch (ElementClickInterceptedException e) {
            // fallback: ENTER on focused button
            try {
                WebElement btn = driver.findElement(cookieAcceptButton);
                btn.sendKeys(Keys.ENTER);
                wait.until(ExpectedConditions.invisibilityOfElementLocated(cookieBanner));
            } catch (Exception ignored) {}
        } catch (StaleElementReferenceException e) {
            // fallback: re-fetch and click
            try {
                WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(cookieAcceptButton));
                new Actions(driver).moveToElement(btn).click().perform();
                wait.until(ExpectedConditions.invisibilityOfElementLocated(cookieBanner));
            } catch (Exception ignored) {}
        }
    }

    /** Find first *visible* outcome; re-find to avoid stale references. */
    private WebElement findFirstVisibleOutcome(long timeoutSec) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSec));
        List<WebElement> all = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(outcomeButtons));
        for (WebElement el : all) {
            try { if (el.isDisplayed()) return el; } catch (StaleElementReferenceException ignored) {}
        }
        // fallback: return first; caller retries if needed
        return all.get(0);
    }

    /**
     * Robust click on first available outcome.
     * Steps: cookie -> find -> wait clickable -> Actions.click(). Retries on stale/intercepted.
     */
    public void selectFirstOutcome() {
        acceptCookiesIfPresent();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement pick = findFirstVisibleOutcome(15);
                wait.until(ExpectedConditions.elementToBeClickable(pick));

                new Actions(driver).moveToElement(pick).pause(Duration.ofMillis(150)).click().perform();

                // settle: either DOM re-renders or stays same
                try {
                    wait.until(ExpectedConditions.or(
                            ExpectedConditions.stalenessOf(pick),
                            ExpectedConditions.visibilityOf(pick)
                    ));
                } catch (StaleElementReferenceException ignored) {}
                return;
            } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                // retry with fresh reference
            }
        }
        throw new RuntimeException("Failed to click an outcome after retries.");
    }

    /** Extract first decimal from text (e.g. "2.35"/"2,35"). Returns null if not found. */
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

    /** Read odds from a pick: root + common descendants + attributes. */
    private String readOddsFromPick(WebElement root) {
        try {
            // root textContent
            String v = extractOddsNumber(root.getAttribute("textContent"));
            if (v != null) return v;

            // root aria-label
            v = extractOddsNumber(root.getAttribute("aria-label"));
            if (v != null) return v;

            // common descendants
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
                } catch (StaleElementReferenceException ignored) {}
            }
        } catch (StaleElementReferenceException ignored) {}
        return null;
    }

    /** QUICK read. First visible outcome, current odds or null now. */
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

    /** Wait up to timeout to get first visible outcome odds (light polling). */
    public String waitAndGetFirstOutcomeOddsText(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                String v = getFirstOutcomeOddsTextQuick();
                if (v != null && !v.isEmpty()) return v;
            } catch (StaleElementReferenceException ignored) {}
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        return null;
    }

    /** Wait until first outcome odds change (light polling). */
    public boolean waitForFirstOutcomeOddsChange(Duration timeout) {
        final String baseline = waitAndGetFirstOutcomeOddsText(Duration.ofSeconds(10));
        if (baseline == null) return false;

        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                String current = getFirstOutcomeOddsTextQuick();
                if (current != null && !current.equals(baseline)) return true;
            } catch (StaleElementReferenceException ignored) {}
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    /** Heuristic: true if the surrounding market/context suggests a Half-time market. */
    private boolean isHalftimeContextFor(WebElement pick) {
        final String[] tokens = new String[] {
                "halftime", "half time", "1st half", "2nd half", "first half", "second half",
                "half-time", "ht",
                "първо полувреме", "второ полувреме", "полувреме"
        };

        String ctx = null;

        // nearest market/container ancestor
        try {
            WebElement container = pick.findElement(By.xpath(
                    "ancestor::*[self::ms-option-group or self::ms-market or self::section or self::div][1]"
            ));
            ctx = container.getText();
            if (ctx == null || ctx.trim().isEmpty()) {
                ctx = container.getAttribute("textContent");
            }
        } catch (NoSuchElementException ignored) {}

        // fallback: parent node
        if (ctx == null || ctx.trim().isEmpty()) {
            try {
                WebElement parent = pick.findElement(By.xpath(".."));
                ctx = parent.getText();
                if (ctx == null || ctx.trim().isEmpty()) {
                    ctx = parent.getAttribute("textContent");
                }
            } catch (Exception ignored) {}
        }

        if (ctx == null) return false;
        String lc = ctx.toLowerCase();
        for (String t : tokens) {
            if (lc.contains(t)) return true;
        }
        return false;
    }

    /** First visible NON-halftime outcome odds (quick) or null now. */
    public String getFirstNonHalftimeOddsTextQuick() {
        List<WebElement> picks = driver.findElements(outcomeButtons);
        for (WebElement el : picks) {
            try {
                if (!el.isDisplayed()) continue;
                if (isHalftimeContextFor(el)) continue; // skip halftime markets
                String v = readOddsFromPick(el);
                if (v != null && !v.isEmpty()) return v;
            } catch (StaleElementReferenceException ignored) { }
        }
        return null;
    }

    /** Wait up to timeout for NON-halftime odds (light polling). */
    public String waitAndGetFirstNonHalftimeOddsText(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                String v = getFirstNonHalftimeOddsTextQuick();
                if (v != null && !v.isEmpty()) return v;
            } catch (StaleElementReferenceException ignored) {}
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        return null;
    }

    /** Wait until a NON-halftime odds value changes (light polling). */
    public boolean waitForNonHalftimeOddsChange(Duration timeout) {
        final String baseline = waitAndGetFirstNonHalftimeOddsText(Duration.ofSeconds(12));
        if (baseline == null) return false;

        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                String current = getFirstNonHalftimeOddsTextQuick();
                if (current != null && !current.equals(baseline)) return true;
            } catch (StaleElementReferenceException ignored) {}
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    // ===== NEW helper for mobile flow =====

    /** Quick check: is there at least one selected/active outcome in the grid? */
    public boolean isAnyPickSelectedQuick() {
        List<WebElement> picks = driver.findElements(outcomeButtons);
        for (WebElement el : picks) {
            try {
                String cls = (el.getAttribute("class") + "").toLowerCase();
                String aria = (el.getAttribute("aria-pressed") + "").toLowerCase();

                if (cls.contains("selected") || cls.contains("active") || "true".equals(aria)) {
                    return true;
                }
                // sometimes the status is on descendants
                if (!el.findElements(By.cssSelector("[aria-pressed='true'], .selected, .active")).isEmpty()) {
                    return true;
                }
            } catch (StaleElementReferenceException ignored) { }
        }
        return false;
    }
}
