# Intro to Selenium

# Session 2: Refactor and Extend

## 1\. Overview

This session addresses the problems left over from Session 1; duplicated setup/teardown code and the zombie browser bug; and then expands your Selenium vocabulary. You will learn the full set of element locator strategies, work with multiple assertion types, interact with HTML dropdowns, and find collections of elements on a page.

By the end of this session, your test class will be cleanly structured with shared setup and teardown, and you will have tests that verify product listing and sorting behavior on the saucedemo inventory page.

---

## 2\. Refactoring: Test Lifecycle

### 2.1 The Problem

Look at your `LoginTest` from Session 1\. Both test methods contain the same WebDriverManager setup, ChromeDriver creation, and `driver.quit()` call. This violates the DRY (Don't Repeat Yourself) principle: if you add more tests, you have to copy-paste the setup and teardown into every single method.

Worse, as noted in the Session 1 checkpoint, if an assertion fails the `driver.quit()` line is never reached. The browser stays open as a zombie process.

### 2.2 JUnit 5 Lifecycle Annotations

JUnit 5 provides annotations that run methods automatically at specific points in the test lifecycle:

**@BeforeEach**; Runs before every `@Test` method. Use this to set up a fresh WebDriver instance so each test starts with a clean browser.

**@AfterEach**; Runs after every `@Test` method, **even if the test fails or throws an exception**. This is the key insight: putting `driver.quit()` in an `@AfterEach` method guarantees the browser is always closed, regardless of what happens in the test.

**@BeforeAll**; Runs once before all tests in the class. Must be `static`. Use this for one-time setup that doesn't need to be repeated per test. WebDriverManager configuration (`WebDriverManager.chromedriver().setup()`) belongs here because the driver binary only needs to be resolved once; it doesn't change between tests.

### 2.3 Requirements: Refactor LoginTest

Refactor your `LoginTest` class from Session 1 to use lifecycle annotations.

**Functional Specification:**

1. Move the `WebDriverManager.chromedriver().setup()` call into a `@BeforeAll` static method  
2. Move the `ChromeDriver` creation (`new ChromeDriver()`) into a `@BeforeEach` method  
3. Move the `driver.quit()` call into an `@AfterEach` method  
4. Remove all setup and teardown code from the individual test methods; they should contain only navigation, interaction, and assertions  
5. The `driver` variable must be an instance field on the class so that all methods can access it

**Verification:**

- Both existing tests still pass  
- Run a test with a deliberately wrong assertion. Confirm that the browser now closes automatically even when the test fails. This proves the `@AfterEach` method is working.

**Design Notes:**

- The `@BeforeEach` method creates a brand new `ChromeDriver` for every test. This means each test gets an isolated browser session with no cookies, no history, and no state from previous tests. Test isolation is important: tests should not depend on each other or on execution order.  
- The `@AfterEach` method should check that `driver` is not null before calling `quit()`, as a defensive measure against setup failures.

---

## 3\. Concepts: Locator Strategies

In Session 1 you used `By.id` and `By.className` to find elements. Selenium provides several additional strategies. Each has different strengths and appropriate use cases.

### 3.1 By.id

Finds an element by its HTML `id` attribute. IDs are intended to be unique per page, making this the most reliable and fastest locator. Always prefer `By.id` when the element has one.

Example HTML: `<input id="user-name" type="text" />` Locator: `By.id("user-name")`

### 3.2 By.name

Finds an element by its HTML `name` attribute. Common on form inputs. Unlike IDs, multiple elements can share the same `name` (e.g. radio buttons in a group), so `findElement` returns the first match.

Example HTML: `<input name="user-name" type="text" />` Locator: `By.name("user-name")`

### 3.3 By.className

Finds an element by a single CSS class name. Elements often have multiple classes (e.g. `class="btn btn_action btn_medium"`). With `By.className`, you specify exactly one of those classes, not the full string.

Example HTML: `<div class="inventory_item_name">Sauce Labs Backpack</div>` Locator: `By.className("inventory_item_name")`

### 3.4 By.tagName

Finds an element by its HTML tag. Most useful with `findElements` (plural) to get all elements of a given tag, for example all `<option>` elements inside a `<select>`.

Example HTML: `<select>...</select>` Locator: `By.tagName("select")`

### 3.5 By.cssSelector

Finds an element using a CSS selector; the same syntax used in CSS stylesheets. This is a powerful and flexible strategy that can match elements by any combination of tag, class, ID, attribute, or structural position.

Common patterns:

| Selector | Meaning |
| :---- | :---- |
| `#user-name` | Element with id `user-name` |
| `.inventory_item` | Element with class `inventory_item` |
| `div.inventory_item` | A `<div>` with class `inventory_item` |
| `[data-test='error']` | Element with attribute `data-test` equal to `error` |
| `div.inventory_item_name` | A `<div>` with class `inventory_item_name` |
| `.inventory_list .inventory_item` | An `.inventory_item` that is a descendant of `.inventory_list` |

`By.cssSelector` is the recommended general-purpose locator when `By.id` is not available. It is readable, performant, and familiar to anyone who has worked with CSS or browser DevTools.

### 3.6 By.xpath

Finds an element using an XPath expression. XPath can navigate the HTML document tree in any direction; up to parents, down to children, sideways to siblings; and can match on text content, which CSS selectors cannot.

Common patterns:

| XPath | Meaning |
| :---- | :---- |
| `//div[@class='inventory_item_name']` | Any `<div>` with exact class `inventory_item_name` |
| `//div[contains(@class, 'inventory')]` | Any `<div>` whose class contains `inventory` |
| `//div[text()='Sauce Labs Backpack']` | A `<div>` whose exact text is `Sauce Labs Backpack` |
| `//div[contains(text(), 'Backpack')]` | A `<div>` whose text contains `Backpack` |
| `//div[@class='inventory_item_name']/..` | The parent of a `<div>` with that class |

XPath is more powerful than CSS selectors but also more verbose and slower in most browsers. Use it when you need to match on text content or navigate to parent/sibling elements; otherwise prefer CSS selectors.

### 3.7 Choosing a Locator Strategy

As a general priority order:

1. **By.id**; Use whenever available. Fast, unique, stable.  
2. **By.cssSelector**; The versatile default. Use for classes, data attributes, and structural matching.  
3. **By.xpath**; Use when you need text matching or parent/sibling navigation that CSS cannot do.  
4. **By.name**, **By.className**, **By.tagName**; Simpler alternatives when they fit. `By.className` and `By.tagName` are essentially shortcuts for simple CSS selectors.

Avoid locators that depend on page structure or position (e.g. "the third div inside the second section"); these break when the page layout changes. Prefer locators that match on meaningful attributes like `id`, `data-test`, or descriptive class names.

### 3.8 Chrome DevTools: Locator Techniques

Chrome DevTools is your primary tool for finding and testing locators:

1. **Inspect an element:** Right-click on any element in the browser and select "Inspect." The Elements panel opens with that element highlighted in the HTML tree.  
2. **Search the DOM:** In the Elements panel, press `Ctrl+F` (or `Cmd+F` on Mac) to open the search bar. You can type a CSS selector or XPath expression here. DevTools will show the number of matches and highlight them in the page. This is the fastest way to verify a locator before putting it in code.  
3. **Console testing:** In the Console panel, you can test locators using JavaScript:  
   - CSS selector: `document.querySelector('.inventory_item_name')` (first match) or `document.querySelectorAll('.inventory_item_name')` (all matches)  
   - XPath: `$x("//div[@class='inventory_item_name']")`

---

## 4\. Concepts: Assertions

Session 1 used `assertEquals`. JUnit 5 provides several other assertion methods, all as static imports from `org.junit.jupiter.api.Assertions`.

**assertEquals(expected, actual)**; Verifies two values are equal. The most common assertion. Note that the expected value comes first; this matters for the failure message.

**assertTrue(condition)**; Verifies a boolean condition is true. Useful with `String.contains()`, comparisons, and other boolean expressions. You can provide a custom failure message as the second argument: `assertTrue(condition, "message if it fails")`.

**assertFalse(condition)**; Verifies a boolean condition is false.

**assertNotNull(object)**; Verifies an object reference is not null. Useful for confirming an element was found.

**assertAll(executables...)**; Groups multiple assertions together and reports all failures, not just the first one. Without `assertAll`, JUnit stops at the first failure. With it, you get a complete picture of what passed and what didn't. This is particularly useful when verifying multiple properties of a page at once.

Example usage of `assertAll`:

assertAll(

    () \-\> assertEquals("Products", titleElement.getText()),

    () \-\> assertEquals(6, itemElements.size()),

    () \-\> assertTrue(firstPrice.contains("$"))

);

---

## 5\. Concepts: Finding Multiple Elements

The `findElement` method returns a single `WebElement`; the first match. If no match is found, it throws a `NoSuchElementException`.

The `findElements` method (note the plural) returns a `List<WebElement>` of all matches. If no matches are found, it returns an empty list; it does not throw an exception. This makes it useful for:

- Counting elements: "How many products are displayed?"  
- Iterating: "Get the text of every product name on the page"  
- Checking presence: "Is the list empty?"

---

## 6\. Concepts: Working with Dropdowns

HTML `<select>` elements (dropdowns) require special handling in Selenium. While you can locate a `<select>` element with any `By` locator, interacting with its options requires Selenium's `Select` class from the `org.openqa.selenium.support.ui` package.

To work with a dropdown:

1. Locate the `<select>` element using `findElement`  
2. Wrap it in a `Select` object  
3. Use `Select` methods to interact with the options

Key `Select` methods:

- **selectByValue(value)**; Selects the option whose `value` attribute matches. On the saucedemo inventory page, the sort dropdown options have values `az`, `za`, `lohi`, and `hilo`.  
- **selectByVisibleText(text)**; Selects the option whose visible text matches, e.g. `"Name (A to Z)"`.  
- **selectByIndex(index)**; Selects by position (zero-based).  
- **getFirstSelectedOption()**; Returns the currently selected `WebElement`.

The saucedemo inventory page has a sort dropdown with the CSS class `product_sort_container`. Its options are:

| Value | Visible Text |
| :---- | :---- |
| `az` | Name (A to Z) |
| `za` | Name (Z to A) |
| `lohi` | Price (low to high) |
| `hilo` | Price (high to low) |

---

## 7\. Requirements: InventoryTest; Product Listing

Create a new test class called `InventoryTest` in the `org.example` package. This class tests the inventory (products) page of saucedemo.com.

### 7.1 Structure

Since every inventory test needs to start from a logged-in state on the products page, the `@BeforeEach` method in this class should:

1. Create a new `ChromeDriver`  
2. Navigate to saucedemo.com  
3. Log in with `standard_user` / `secret_sauce`

The `@AfterEach` method should quit the driver, same as in `LoginTest`.

The `@BeforeAll` method should configure WebDriverManager, same as in `LoginTest`.

### 7.2 Functional Specification: Verify Product Count

Write a test that verifies the inventory page displays the expected number of products.

**Steps:**

1. After login (handled by `@BeforeEach`), find all product item elements on the page

**Verification:**

- Assert that the number of product items is 6

**Design Notes:**

- Use `findElements` (plural) to get a list of all product items  
- The product items can be located by the CSS class `inventory_item`  
- Assert on the `size()` of the returned list

### 7.3 Functional Specification: Verify Product Names Displayed

Write a test that verifies every product on the page has a non-empty name.

**Steps:**

1. After login, find all product name elements on the page

**Verification:**

- The number of product name elements equals 6  
- Every product name has non-empty text

**Design Notes:**

- Product name elements have the CSS class `inventory_item_name`  
- Iterate over the list and use `assertFalse(name.getText().isEmpty())` for each, or use `assertAll` to group the assertions  
  Note: You could use `assertAll()`, but this is an advanced operation requiring a lambda and streaming. The code is provided here for your learning:

  assertAll("All products should have non-empty names",   
      nameElements.stream() .map(  
          element \-\> () \-\> assertFalse(   
              element.getText().isEmpty(), "Empty name found for element: " \+ element)  
          )   
      );

### 7.4 Functional Specification: Verify Product Prices Displayed

Write a test that verifies every product on the page has a visible price that starts with a dollar sign.

**Steps:**

1. After login, find all product price elements on the page

**Verification:**

- The number of product price elements equals 6  
- Every price string starts with `$`

**Design Notes:**

- Product price elements have the CSS class `inventory_item_price`  
- Use `assertTrue` with `startsWith("$")`

---

## 8\. Requirements: InventoryTest; Sorting

### 8.1 Functional Specification: Sort by Price Low to High

Write a test that selects "Price (low to high)" from the sort dropdown and verifies the products are sorted correctly.

**Steps:**

1. After login, locate the sort dropdown element (CSS class: `product_sort_container`)  
2. Wrap it in a `Select` object  
3. Select the option with value `lohi`  
4. Find all product price elements on the page  
5. Extract the numeric price from each element's text (strip the `$` prefix and parse to `double`)

**Verification:**

- Each price is less than or equal to the next price in the list

**Design Notes:**

- You will need to convert the price text (e.g. `"$7.99"`) to a numeric value for comparison. Use `substring(1)` to remove the `$`, then `Double.parseDouble()` to convert.  
- To verify sort order, loop through the list of prices and compare each one to the next. If any price is greater than the one after it, the sort is wrong.  
- Alternatively, you can collect all prices into a `List<Double>`, make a sorted copy, and assert the two lists are equal.

### 8.2 Functional Specification: Sort by Name Z to A

Write a test that selects "Name (Z to A)" from the sort dropdown and verifies the products are sorted in reverse alphabetical order.

**Steps:**

1. After login, locate the sort dropdown and select the option with value `za`  
2. Find all product name elements on the page  
3. Extract the text from each

**Verification:**

- Each name is alphabetically greater than or equal to the next name (reverse order). String comparison with `compareTo` works here.

**Design Notes:**

- The approach is similar to the price sort test: extract values, verify ordering.  
- `String.compareTo()` returns a negative value if the first string comes before the second alphabetically. For Z-to-A order, each name's `compareTo` against the next name should be \>= 0\.

---

## 9\. Requirements: Locator Practice (Exercise)

### 9.1 Functional Specification: Find a Specific Product

Write a test in `InventoryTest` that locates a specific product by name and verifies its price.

**Steps:**

1. After login, locate the element whose text is `Sauce Labs Backpack` using an XPath text match  
2. Read its associated price

**Verification:**

- Assert that the price text is `$29.99`

**Design Notes:**

- This exercise practices XPath text matching: `//div[text()='Sauce Labs Backpack']`  
- The price element is not a child of the name element; they are siblings within the same parent container. You will need to use Chrome DevTools to inspect the page structure and determine how to navigate from the name element to the price element. Consider using XPath to navigate to the parent first, then find the price within that parent. Alternatively, use a CSS selector that targets the price within the same `inventory_item` ancestor.  
- There is more than one correct way to solve this. The goal is to practice using DevTools to understand page structure and build a locator that works.

---

## 10\. Push to GitHub

Once all tests pass in both `LoginTest` and `InventoryTest`, commit your work and push to GitHub.

git add .

git commit \-m "Session 2: refactored lifecycle, locator strategies, inventory tests"

git push origin main

Visit your repository on GitHub and verify the updated files are there.

---

## 11\. Session 2 Checkpoint

After completing this session, you should have:

- A refactored `LoginTest` using `@BeforeAll`, `@BeforeEach`, and `@AfterEach`; with the zombie browser problem fixed  
- A new `InventoryTest` class with tests for product count, product names, product prices, sort by price, sort by name, and finding a specific product  
- Familiarity with all six locator strategies: `By.id`, `By.name`, `By.className`, `By.tagName`, `By.cssSelector`, `By.xpath`  
- Experience using `findElements` to work with collections of elements  
- Experience using the `Select` class for dropdown interaction  
- All code committed and pushed to GitHub

### 11.1 Observation: Duplicated Login Code

Notice that both `LoginTest` and `InventoryTest` contain login logic; `LoginTest` has it in individual test methods, and `InventoryTest` has it in `@BeforeEach`. As you add more test classes, this login code would get copied everywhere. Think about how you might extract it into a reusable component. We will address this in Session 4 when we introduce the Page Object Model.

### 11.2 Preview: Session 3

In Session 3, we will:

- Explore explicit waits with `WebDriverWait` and `ExpectedConditions`  
- Understand why tests can be "flaky"; passing sometimes and failing other times  
- Use the `performance_glitch_user` account on saucedemo to experience slow page loads  
- Practice with the-internet.herokuapp.com for isolated examples of dynamic content

---

## Appendix A: Reference Links

| Resource | URL |
| :---- | :---- |
| Selenium locator strategies | [https://www.selenium.dev/documentation/webdriver/elements/locators/](https://www.selenium.dev/documentation/webdriver/elements/locators/) |
| CSS selector reference (MDN) | [https://developer.mozilla.org/en-US/docs/Web/CSS/CSS\_selectors](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_selectors) |
| XPath reference (MDN) | [https://developer.mozilla.org/en-US/docs/Web/XPath](https://developer.mozilla.org/en-US/docs/Web/XPath) |
| JUnit 5 Assertions API | [https://junit.org/junit5/docs/current/api/org.junit.jupiter.api/org/junit/jupiter/api/Assertions.html](https://junit.org/junit5/docs/current/api/org.junit.jupiter.api/org/junit/jupiter/api/Assertions.html) |
| Selenium Select class docs | [https://www.selenium.dev/selenium/docs/api/java/org/openqa/selenium/support/ui/Select.html](https://www.selenium.dev/selenium/docs/api/java/org/openqa/selenium/support/ui/Select.html) |
| Chrome DevTools docs | [https://developer.chrome.com/docs/devtools/](https://developer.chrome.com/docs/devtools/) |
| Saucedemo practice site | [https://www.saucedemo.com](https://www.saucedemo.com) |

## Appendix B: Troubleshooting

**Test fails with NoSuchElementException**

- The element was not found on the page. Verify the locator by testing it in Chrome DevTools (Ctrl+F in the Elements panel). Make sure you are on the correct page; if login didn't complete, you won't find inventory elements.

**Sort test passes locally but the ordering seems wrong**

- Make sure you are reading prices/names after selecting the sort option, not before. The DOM updates after the sort selection.  
- Double-check your comparison logic: for ascending order, each item should be less than or equal to the next. For descending, each item should be greater than or equal to the next.

**Select class throws UnexpectedTagNameException**

- You are wrapping a non-`<select>` element in a `Select` object. The `Select` class only works with `<select>` HTML elements. Verify with Chrome DevTools that the element you found is actually a `<select>` tag.

**findElements returns an empty list**

- Unlike `findElement`, this method does not throw an exception when nothing is found. If your list is empty, your locator is not matching anything. Test it in DevTools.

**@BeforeAll method fails with "must be static"**

- JUnit 5 requires `@BeforeAll` methods to be `static` (unless you configure per-class test instance lifecycle, which is not recommended for this course). Make sure the method and any fields it accesses are declared `static`.

