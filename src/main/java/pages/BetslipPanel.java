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
 * Betslip panel (right rail).
 * No JS; only short explicit waits. I validate against stable EN labels
 * like "Bet Slip" and "Selections (N)" so the checks survive minor DOM tweaks.
 */
public class BetslipPanel extends BasePage {

    // Header/tab "Bet Slip" – broad selector so it matches across small markup changes.
    private final By betSlipHeaderText = By.xpath(
            "//*[self::div or self::button or self::a or self::h1 or self::h2 or self::h3 or self::span]" +
                    "[contains(normalize-space(.),'Bet Slip') or contains(normalize-space(.),'Betslip')]"
    );

    // Row like "Selections (1)" inside the betslip.
    private final By selectionsCounter = By.xpath(
            "//*[contains(normalize-space(.),'Selections (') and contains(normalize-space(.),')')]"
    );

    // Wide fallback: try to spot “some” selection row under the right rail.
    // I look for a compact text (often odds) or a remove/close control.
    private final By anySelectionRow = By.xpath(
            "//aside//*[contains(@class,'selection') or contains(@class,'bet') or contains(@class,'row')]" +
                    "[.//*[self::span or self::div or self::b or self::strong]" +
                    "[contains(normalize-space(.),'.') or string-length(normalize-space(.))<=5] " + // odds-like value
                    " or .//*[contains(@class,'remove') or contains(@class,'close') or contains(@aria-label,'Remove')]]"
    );

    // Toggle/tab used on mobile to show the betslip (used in the responsive test).
    private final By betslipToggle = By.xpath(
            "//*[self::button or self::a or self::div or self::span]" +
                    "[contains(normalize-space(.),'Bet Slip') or contains(normalize-space(.),'Betslip')]"
    );

    public BetslipPanel(WebDriver driver) {
        super(driver);
    }

    /**
     * Returns true if the Bet Slip header is present (panel visible on desktop).
     * I only wait for presence, not visibility, to keep it fast and resilient.
     */
    public boolean isBetslipVisible() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(betSlipHeaderText));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if at least one selection is detected.
     * Primary signal: "Selections (N)". Fallback: any selection-like row.
     */
    public boolean hasPicks() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(selectionsCounter));
            return true;
        } catch (Exception ignored) {
            // Fallback: try a broader pattern for a selection row.
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
     * On mobile the betslip is hidden behind a toggle tab.
     * This checks whether such a toggle is present and displayed.
     */
    public boolean isTogglePresent() {
        try {
            WebDriverWait sw = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement t = sw.until(ExpectedConditions.presenceOfElementLocated(betslipToggle));
            return t != null && t.isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException | org.openqa.selenium.TimeoutException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
