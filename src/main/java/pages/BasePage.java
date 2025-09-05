package pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

/**
 * Minimal base for all page objects.
 * I keep only the shared WebDriver reference and a couple of simple helpers.
 * PageFactory is initialized so I can use @FindBy if needed.
 */
public class BasePage {
    protected WebDriver driver;

    /**
     * Store the driver and initialize PageFactory bindings.
     */
    public BasePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    /** Expose current URL – handy in assertions/logging. */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /** Expose current page title – occasionally useful in sanity checks. */
    public String getTitle() {
        return driver.getTitle();
    }
}
