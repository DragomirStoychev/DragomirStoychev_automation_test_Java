package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Betslip (right rail) helper. No JS – only short, robust waits.
 * Validates by stable EN texts ("Bet Slip", "Selections (N)") where available.
 */
public class BetslipPanel extends BasePage {

    // Header/tab "Bet Slip" (works on EN; harmless on other locales)
    private final By betSlipHeaderText = By.xpath(
            "//*[self::div or self::button or self::a or self::h1 or self::h2 or self::h3 or self::span]" +
                    "[contains(normalize-space(.),'Bet Slip') or contains(normalize-space(.),'Betslip')]"
    );

    // Row like "Selections (1)"
    private final By selectionsCounter = By.xpath(
            "//*[contains(normalize-space(.),'Selections (') and contains(normalize-space(.),')')]"
    );

    // Very broad fallback: any selection row under the aside (right rail)
    private final By anySelectionRow = By.xpath(
            "//aside//*[contains(@class,'selection') or contains(@class,'bet') or contains(@class,'row')]" +
                    "[.//*[self::span or self::div or self::b or self::strong]" +
                    "[contains(normalize-space(.),'.') or string-length(normalize-space(.))<=5] " + // often the odds
                    " or .//*[contains(@class,'remove') or contains(@class,'close') or contains(@aria-label,'Remove')]]"
    );

    //generic Bet Slip toggle/icon – detected by attributes, not visible text.
    private final By betslipToggle = By.xpath(
            "//*[" +
                    "contains(translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'betslip') or " +
                    "contains(translate(@class,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'betslip') or " +
                    "contains(translate(@data-test-id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'betslip') or " +
                    "contains(translate(@aria-label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'betslip')" +
                    "]"
    );

    public BetslipPanel(WebDriver driver) {
        super(driver);
    }

    /** Is the Bet Slip panel visible (by header)? */
    public boolean isBetslipVisible() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(betSlipHeaderText));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Are there selections inside Bet Slip? */
    public boolean hasPicks() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(selectionsCounter));
            return true;
        } catch (Exception ignored) {
            // Fallback – try to detect any "selection-like" row in the aside
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
                shortWait.until(ExpectedConditions.presenceOfElementLocated(anySelectionRow));
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    /**
     * Is there a toggle/icon for Bet Slip in the DOM?
     * We don't require it to be visible; on mobile it can be collapsed.
     */
    public boolean isTogglePresent() {
        try {
            WebDriverWait sw = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement t = sw.until(ExpectedConditions.presenceOfElementLocated(betslipToggle));
            return t != null;
        } catch (NoSuchElementException | StaleElementReferenceException | org.openqa.selenium.TimeoutException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
