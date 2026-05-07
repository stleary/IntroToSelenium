# Intro to Selenium

# Session 3: Waits and Dynamic Content

---

## 1\. Overview

In Sessions 1 and 2, your tests worked reliably because saucedemo.com loads pages almost instantly. Every element is present in the DOM by the time your test looks for it. Real web applications are not that predictable. Pages load at different speeds depending on network conditions, server load, and client-side rendering. Content may be injected into the page by JavaScript after an AJAX call completes. Sections of a page may be hidden until a user action reveals them.

If your test tries to interact with an element before it exists in the DOM or before it becomes visible, the test fails; not because the application is broken, but because the test did not wait long enough.

This session introduces the problem of timing in test automation and teaches you the correct way to handle it. We will use a dedicated practice site to see the problem firsthand, then apply what we learn to make our saucedemo tests more robust.

### 1.1 What You Will Learn

- Why tests fail when pages load content dynamically  
- Why `Thread.sleep()` is the wrong solution  
- Why implicit waits are unreliable  
- How to use `WebDriverWait` and `ExpectedConditions` (explicit waits)  
- How to handle elements that are hidden, not yet rendered, or loading dynamically  
- How to write tests that are robust against variable page load times

### 1.2 Prerequisites

You should have completed Sessions 1 and 2\. Your project should have a working test class with `@BeforeEach` and `@AfterEach` lifecycle methods, and you should be comfortable using locator strategies to find elements on a page.

---

## 2\. The Problem: Timing

### 2.1 Dynamic Content in the Real World

Modern web applications rarely deliver a fully rendered page in a single response. Instead, the initial HTML loads quickly, and then JavaScript fetches additional data, renders components, and updates the DOM. This means elements your test needs to interact with may not exist yet; or may exist but be hidden; when your test code runs.

Common patterns that cause timing problems include: AJAX calls that fetch data and inject it into the page after loading, single-page application frameworks that render components asynchronously, loading spinners that overlay content until a backend call completes, accordions, tabs, and modals that hide content until a user action reveals them, and form fields that become enabled only after validation completes.

Saucedemo.com is fast enough that you have not encountered these problems yet. To see them clearly, we need a site that deliberately introduces dynamic behavior.

### 2.2 The Practice Site: the-internet.herokuapp.com

Dave Haeffner (one of the early Selenium contributors) created **the-internet.herokuapp.com**, a practice site with isolated pages for specific interaction patterns. We will use its dynamic loading examples to see timing failures firsthand.

Go to [the-internet.herokuapp.com/dynamic\_loading/2](https://the-internet.herokuapp.com/dynamic_loading/2) in your browser. Click the "Start" button. Observe that a loading bar appears for several seconds, and then the text "Hello World\!" appears. Now imagine your test clicks "Start" and immediately calls `driver.findElement(By.cssSelector("#finish h4"))`. The element does not exist in the DOM yet. Selenium throws a `NoSuchElementException` and the test fails.

The application is working correctly. The test is the problem.

---

## 3\. Bad Solutions

### 3.1 Thread.sleep(); The Hard-Coded Wait

The first instinct most people have is to add a `Thread.sleep()` call before the element lookup:

// DON'T DO THIS

Thread.sleep(5000); // wait 5 seconds

WebElement title \= driver.findElement(By.cssSelector("\#finish h4"));

This works, in the sense that the test passes. But it is a bad solution for several reasons:

- **It always waits the full duration.** If the page loads in 1 second, you still wait 5\. Multiply by dozens or hundreds of tests, and your test suite takes far longer than necessary.  
- **The duration is a guess.** You pick 5 seconds because it seems like enough. But on a slow CI server, it might need 8\. On a developer's fast machine, 2 seconds would suffice. There is no correct value.  
- **It hides real performance problems.** If the application starts taking 10 seconds to load a page, your 5-second sleep fails. You bump it to 15\. Now you have a test that passes but masks a serious regression.  
- **It does not check anything.** The sleep expires and your code proceeds whether the element is ready or not. If 5 seconds is not enough, you still get the same `NoSuchElementException`.

In professional test automation, `Thread.sleep()` is considered a code smell. If you see it in a code review, flag it.

### 3.2 Implicit Waits; The Global Timeout

Selenium provides an implicit wait mechanism that tells the driver to poll the DOM for a specified duration when an element is not found immediately:

// Also not recommended

driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

Once set, this applies to **every** `findElement()` call for the life of the driver. If an element is not found, Selenium retries internally for up to 10 seconds before throwing the exception.

Implicit waits are better than `Thread.sleep` because they return as soon as the element appears. But they have significant drawbacks:

- **They only check presence, not state.** An element can be present in the DOM but hidden, disabled, or obscured by a loading overlay. The implicit wait finds it, your code tries to click it, and the interaction fails.  
- **They apply globally.** Every `findElement` call waits up to the timeout, even when you expect an element to be absent (for example, verifying an error message does not appear). This makes "should not exist" assertions extremely slow.  
- **They interact poorly with explicit waits.** The Selenium documentation warns against mixing implicit and explicit waits. The two mechanisms can interfere with each other in unpredictable ways, leading to waits that are longer than you expect or conditions that are never properly evaluated.  
- **They hide the intent of your test.** Reading the test, you cannot see what condition the test is waiting for. The wait is invisible, buried in a one-time configuration call at setup.

For these reasons, the Selenium project's official documentation recommends explicit waits over implicit waits.

---

## 4\. The Right Solution: Explicit Waits

### 4.1 WebDriverWait and ExpectedConditions

Selenium provides two classes that work together to implement explicit waits: `WebDriverWait` and `ExpectedConditions`. You create a wait object with a timeout, then tell it what condition to wait for:

import org.openqa.selenium.support.ui.WebDriverWait;

import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

WebDriverWait wait \= new WebDriverWait(driver, Duration.ofSeconds(10));

WebElement element \= wait.until(

    ExpectedConditions.visibilityOfElementLocated(By.cssSelector("\#finish h4"))

);

This code says: poll the DOM every 500 milliseconds (the default interval) for up to 10 seconds, checking whether the element with the given locator is both present in the DOM and visible on the page. If the condition is satisfied, return the element immediately. If 10 seconds elapse without the condition being met, throw a `TimeoutException`.

This is better than all the previous approaches:

- **It returns as soon as possible.** If the element appears in 200 milliseconds, the test proceeds in 200 milliseconds, not 5 seconds or 10 seconds.  
- **It checks a specific condition.** You are not just checking whether the element exists in the DOM. You are checking whether it is visible, clickable, has certain text, or whatever condition your test requires.  
- **It is scoped to a single action.** Other `findElement` calls in the same test are not affected. You wait exactly where you need to and nowhere else.  
- **It documents intent.** Anyone reading the test can see: "we wait for this element to become visible because it loads dynamically."

### 4.2 Common ExpectedConditions

The `ExpectedConditions` class provides a set of pre-built conditions. Here are the ones you will use most often:

| Condition | What It Checks |
| :---- | :---- |
| `visibilityOfElementLocated(locator)` | Element is present in the DOM AND visible on the page (has non-zero size, is not hidden by CSS) |
| `presenceOfElementLocated(locator)` | Element exists in the DOM, regardless of visibility. Useful for elements you need to inspect but not interact with |
| `elementToBeClickable(locator)` | Element is visible AND enabled (not disabled). Use this before clicking buttons or links |
| `invisibilityOfElementLocated(locator)` | Element is either not present or not visible. Use this to wait for loading spinners or overlays to disappear |
| `textToBePresentInElementLocated(locator, text)` | Element is present and contains the specified text. Useful for waiting until dynamic content finishes rendering |
| `titleIs(title)` | The page title matches the expected string exactly |
| `urlContains(substring)` | The current URL contains the given substring. Useful for waiting after navigation or redirects |

### 4.3 Handling TimeoutException

When the condition is not met within the specified timeout, `WebDriverWait` throws a `TimeoutException`. In most test code, you let this propagate; a timeout means the test failed because the application did not reach the expected state. JUnit will catch the exception and report the test as failed.

Do **not** wrap every wait in a try-catch just to print a custom message. The `TimeoutException` already includes the condition that was not met and the timeout duration. Reserve try-catch for cases where a timeout is an expected alternative outcome (for example, checking whether an optional element appeared).

### 4.4 Where to Create the Wait Object

A `WebDriverWait` object is reusable. You can create one in your `@BeforeEach` method alongside the driver and reuse it in every test:

private WebDriver driver;

private WebDriverWait wait;

@BeforeEach

void setUp() {

    WebDriverManager.chromedriver().setup();

    driver \= new ChromeDriver();

    wait \= new WebDriverWait(driver, Duration.ofSeconds(10));

}

The timeout value (10 seconds in this example) is a maximum. Choosing a good default requires judgment; long enough that slow-but-correct pages pass, short enough that genuinely broken pages fail promptly. 10 seconds is a reasonable starting point. You can always create additional `WebDriverWait` instances with different timeouts for specific cases.

---

## 5\. Dynamic Loading Exercises

### 5.1 Example 1: Hidden Element Revealed

URL: [the-internet.herokuapp.com/dynamic\_loading/1](https://the-internet.herokuapp.com/dynamic_loading/1)

This page has a "Hello World\!" heading that is already present in the DOM when the page loads, but it is hidden via CSS. When you click the "Start" button, a loading indicator appears for several seconds, then the hidden element becomes visible.

This pattern simulates applications where all the HTML is rendered server-side but certain sections are hidden until a user action reveals them. Think of accordion panels, tab content, or modal dialogs.

#### 5.1.1 Requirements: Hidden Element Test

Write a test class called `DynamicLoadingTest` with a test method for this scenario.

**Test:** `testHiddenElementBecomesVisible`

- Navigate to the dynamic loading Example 1 page  
- Click the `Start` button (locate it by its CSS selector within the `#start` div)  
- Wait until the element in the `#finish` div becomes visible  
- Assert that the text is `"Hello World!"` (use `getText()` and verify the exact text for yourself in the browser)

**Key concept:** The element is in the DOM the entire time. `presenceOfElementLocated` would return immediately and your subsequent visibility assertion might fail. You need `visibilityOfElementLocated` to wait for the element to actually be shown.

#### 5.1.2 Experiment: See It Break

Before writing the wait-aware version, try it without any wait; click "Start" and immediately call `driver.findElement(By.cssSelector("#finish h4")).getText()`. The element exists in the DOM, so `findElement` succeeds, but the text may come back empty because the element is still hidden. This is a subtle bug: the test does not crash, but the assertion fails with an unexpected empty string.

This is why `visibilityOfElementLocated` is important; it does not just check that the element exists, it checks that the element is actually displayed.

### 5.2 Example 2: Element Rendered After the Fact

URL: [the-internet.herokuapp.com/dynamic\_loading/2](https://the-internet.herokuapp.com/dynamic_loading/2)

This page is similar, but the "Hello World\!" element does not exist in the DOM at all when the page loads. It is injected into the DOM dynamically after you click "Start" and the loading completes.

This pattern simulates AJAX-driven applications where content is fetched from a server and inserted into the page after an action; the most common pattern in modern single-page applications.

#### 5.2.1 Requirements: Rendered Element Test

**Test:** `testElementRenderedAfterLoading`

- Navigate to the dynamic loading Example 2 page  
- Click the Start button  
- Wait until the element in the `#finish` div becomes visible  
- Assert that the text is `"Hello World!"`

**Key concept:** Here, `presenceOfElementLocated` would also work as a wait condition since the element is not in the DOM at all until the loading completes. However, `visibilityOfElementLocated` is the safer choice because it also covers Example 1's scenario. When in doubt, wait for visibility.

#### 5.2.2 Experiment: See It Crash

Try this one without any wait; click "Start" and immediately call `driver.findElement(By.cssSelector("#finish h4"))`. Unlike Example 1, this throws a `NoSuchElementException` because the element simply does not exist yet. This is the more obvious failure mode that people typically think of when they hear "timing problem."

### 5.3 Comparing the Two Patterns

These two examples cover the two fundamental dynamic content scenarios:

|  | Example 1 | Example 2 |
| :---- | :---- | :---- |
| Element in DOM on page load? | Yes (hidden) | No |
| `findElement` without wait | Succeeds, but element is invisible | Throws `NoSuchElementException` |
| `presenceOfElementLocated` | Returns immediately (element already present) | Waits correctly (element not yet present) |
| `visibilityOfElementLocated` | Waits correctly (element hidden until loaded) | Waits correctly (element not yet present) |
| **Best wait condition** | `visibilityOfElementLocated` | `visibilityOfElementLocated` |

The takeaway: `visibilityOfElementLocated` handles both scenarios correctly. It is the safe default for most situations.

---

## 6\. Requirements: Update Saucedemo Tests

Now apply what you have learned to your saucedemo tests from Session 2\.

Saucedemo happens to be fast enough that your current tests pass without waits. That is luck, not good practice. In production test automation, you do not rely on the application always being fast. Network hiccups, overloaded CI servers, browser startup delays, and application changes can all introduce latency at any time. Adding explicit waits now makes your tests robust against these real-world conditions.

### 6.1 Create a New Branch

Create a new Git branch called `3-WaitDynamicContent` from your `2-RefactorExtend` branch. All Session 3 work happens on this branch.

### 6.2 Add the Wait Infrastructure

- Add a `WebDriverWait` field to your test class  
- Initialize it in `@BeforeEach` with a 10-second timeout  
- Add the necessary imports: `WebDriverWait`, `ExpectedConditions`, and `Duration`

### 6.3 Functional Requirements

#### 6.3.1 Update Existing Tests

Review every test from Session 2\. For each `driver.findElement(...)` call, decide: does this element appear after a navigation or page load? If so, replace the `findElement` call with an explicit wait.

At minimum, after every navigation action (login, page load, URL change), the first element interaction should use an explicit wait. Subsequent interactions on the same already-loaded page generally do not need additional waits unless the page has dynamic content.

For example, after clicking the login button, the first thing your test does on the inventory page should use `wait.until(ExpectedConditions.visibilityOfElementLocated(...))` rather than a bare `driver.findElement(...)`.

### 6.4 Design Constraints

- Do **not** use `Thread.sleep()` anywhere  
- Do **not** set an implicit wait on the driver  
- Use `WebDriverWait` with `ExpectedConditions` for all waiting  
- Choose the most appropriate `ExpectedCondition` for each situation (visibility vs. presence vs. clickable)

### 6.5 Push to GitHub

When all tests pass, commit your changes and push the `3-WaitDynamicContent` branch to your GitHub repository.

---

## 7\. Final Thoughts

### 7.1 Choosing the Right Condition

The difference between `presenceOfElementLocated`, `visibilityOfElementLocated`, and `elementToBeClickable` is subtle but important. Consider these scenarios:

- You need to read an element's attribute value, but the element might be hidden. Which condition do you use?  
- A button exists on the page but is disabled until a form is completed. You want to click it after filling out the form. Which condition do you use?  
- A loading spinner covers the page. You want to wait for it to go away before doing anything. Which condition do you use?

There is not always one right answer, but thinking through these cases builds the judgment you need for real-world test automation.

### 7.2 Timeout Strategy

In a real project, you would not hardcode timeout values in every test method. Think about how you might centralize your timeout configuration. Where would you define the default timeout? How would you override it for specific tests that interact with known-slow pages?   
---

## Appendix A: Reference Links

| Resource | URL |
| :---- | :---- |
| Selenium documentation | [https://www.selenium.dev/documentation/](https://www.selenium.dev/documentation/) |
| Selenium Java API docs | [https://www.selenium.dev/selenium/docs/api/java/](https://www.selenium.dev/selenium/docs/api/java/) |
| WebDriverWait Javadoc | [https://www.selenium.dev/selenium/docs/api/java/org/openqa/selenium/support/ui/WebDriverWait.html](https://www.selenium.dev/selenium/docs/api/java/org/openqa/selenium/support/ui/WebDriverWait.html) |
| ExpectedConditions Javadoc | [https://www.selenium.dev/selenium/docs/api/java/org/openqa/selenium/support/ui/ExpectedConditions.html](https://www.selenium.dev/selenium/docs/api/java/org/openqa/selenium/support/ui/ExpectedConditions.html) |
| Selenium Waits guide | [https://www.selenium.dev/documentation/webdriver/waits/](https://www.selenium.dev/documentation/webdriver/waits/) |
| Saucedemo practice site | [https://www.saucedemo.com](https://www.saucedemo.com) |
| the-internet.herokuapp.com | [https://the-internet.herokuapp.com/](https://the-internet.herokuapp.com/) |
| JUnit 5 User Guide | [https://junit.org/junit5/docs/current/user-guide/](https://junit.org/junit5/docs/current/user-guide/) |
| WebDriverManager docs | [https://bonigarcia.dev/webdrivermanager/](https://bonigarcia.dev/webdrivermanager/) |

## Appendix C: Troubleshooting

**Test fails with TimeoutException**

The element did not reach the expected state within the timeout. First, verify the test works manually in the browser. If the page loads correctly, check your locator; use Chrome DevTools to confirm the element's ID, class, or CSS selector. If the locator is correct but the page is slow, increase the timeout. If the page is broken, the test is correctly reporting a failure.

**NoSuchElementException despite using a wait**

Make sure you are using the return value of `wait.until(...)`. The method returns the element when the condition is met. A common mistake is to call `wait.until(...)` and then separately call `driver.findElement(...)`; this second call does not benefit from the wait.

**Empty string from getText() on a hidden element**

This is the Example 1 scenario. The element is in the DOM but hidden via CSS. `findElement` succeeds (the element is present), but `getText()` returns an empty string because Selenium reports the text of invisible elements as empty. Use `visibilityOfElementLocated` to wait until the element is actually displayed before reading its text.

**StaleElementReferenceException**

This means the element was found, but the DOM was rebuilt (for example, by a JavaScript framework re-rendering the page), and the reference is now invalid. The fix is to re-locate the element after the DOM update. We will cover this in more detail in Session 4 with the Page Object Model.

**Tests pass individually but fail when run together**

Check that your `@BeforeEach` creates a fresh driver and wait object for each test, and your `@AfterEach` calls `driver.quit()`. Shared state between tests is the most common source of this problem.  
