# DragomirStoychev_automation_test

UI test suite for **bwin** Live Betting / A-Z navigation, built with **Java + Selenium + TestNG**.  
Runs both on **desktop** and **mobile** viewports (via TestNG parameter).

## Tech stack
- Java 17+ (tested also with JDK 23)
- Maven
- Selenium WebDriver 4
- WebDriverManager
- TestNG


## How to run (IntelliJ)
- Open `testng.xml` and click **Run**.  
  It contains two runs:
    - **Desktop** (`viewport=desktop`)
    - **Mobile** (`viewport=mobile`)

You can also run any single test class directly from the editor.

## How to run (Maven)
```bash
mvn -Dsurefire.suiteXmlFiles=testng.xml test

What the tests cover

AddPickToBetslipTest: selects the first available outcome.

Desktop: verifies Bet Slip panel is visible and contains the pick.

Mobile: verifies Bet Slip is hidden behind a toggle (or collapsed).

ValidateOddsUpdateTest: observes non-halftime odds and asserts they change within a time window.

CheckSportSortingTest: opens A-Z Sports, selects a sport (e.g., Football) and verifies the respective sport page is loaded (URL and active tab).

ResponsiveDesignTest: resizes from desktop to mobile within the same test and verifies Bet Slip collapses on mobile.

Configuration

Browser language (optional): -Dsite.lang=en or -Dsite.lang=bg
(set via DriverFactory, defaults to en).

Viewport is controlled by TestNG parameter viewport (desktop/mobile) in BaseTest.

Incognito mode is enabled by default.

Notes

Cookie banner is handled robustly (explicit waits + Actions; no JS).

Odds reading is resilient (root/descendants/aria-label) and skips Halftime markets.

Run tips

If Chrome updates and you see CDP warnings, WebDriverManager will still resolve the driver version automatically.

For flaky live data, ValidateOddsUpdateTest may skip if no odds change is detected within the window (logged in the console).