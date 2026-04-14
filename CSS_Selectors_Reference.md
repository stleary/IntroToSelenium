# CSS Selectors for Selenium WebDriver

A complete reference and hands-on exercise set for locating elements with CSS selectors in Selenium WebDriver (Java). 
Selenium delegates CSS selector evaluation to the browser's native `querySelector`/`querySelectorAll` engine, so you 
get everything the browser supports; essentially CSS Selectors Level 4, minus a few pseudo-classes that don't make 
sense for element location.

Exercises target [saucedemo.com](https://www.saucedemo.com), which uses `data-test` attributes consistently and makes an excellent teaching 
target for modern, resilient selector patterns.

**Additional Reference**: [http://pragmatictestlabs.com/2021/01/05/mastering-css-for-selenium-test-automation-2/](http://pragmatictestlabs.com/2021/01/05/mastering-css-for-selenium-test-automation-2/)

---

## Table of Contents

**Part 1 Reference**
1. [Basics: Element, ID, Class](#1-the-basics-element-id-class)
2. [Attribute Selectors](#2-attribute-selectors)
3. [Combinators](#3-combinators-navigating-the-tree)
4. [Selector Lists (Grouping)](#4-selector-lists-grouping)
5. [Structural Pseudo-Classes](#5-structural-pseudo-classes)
6. [State and UI Pseudo-Classes](#6-state-and-ui-pseudo-classes)
7. [Negation and Logical Pseudo-Classes](#7-negation-and-logical-pseudo-classes)
8. [Pseudo-Elements](#8-pseudo-elements-and-why-they-dont-help-you)
9. [Escaping and Quoting](#9-escaping-and-quoting)
10. [What CSS Cannot Do](#10-what-css-cannot-do)
11. [Selenium-Specific Notes](#11-selenium-specific-notes)

**Part 2 Exercises on saucedemo.com**

- [Section 1: Basics](#section-1-basics-id-class-type)
- [Section 2: Attribute Selectors](#section-2-attribute-selectors)
- [Section 3: Combinators](#section-3-combinators)
- [Section 4: Structural Pseudo-Classes](#section-4-structural-pseudo-classes)
- [Section 5: State Pseudo-Classes](#section-5-state-pseudo-classes)
- [Section 6: Logical Pseudo-Classes](#section-6-logical-pseudo-classes-not-is-has)
- [Capstone Challenge](#capstone-challenge)

---

# Part 1 Reference

## 1. The Basics: Element, ID, Class

The three atoms of CSS selection are type, ID, and class. Everything else composes from these.

```java
driver.findElement(By.cssSelector("input"));           // type selector
driver.findElement(By.cssSelector("#login-button"));   // ID selector
driver.findElement(By.cssSelector(".btn-primary"));    // class selector
driver.findElement(By.cssSelector("*"));               // universal selector
```

Type selectors match the element's tag name (case-insensitive in HTML). The universal `*` matches any element and is most useful in combinators. IDs use `#`, classes use `.`, and both can be chained without whitespace to intersect on the same element:

```java
// <button id="submit" class="btn btn-primary disabled">
By.cssSelector("button#submit.btn-primary.disabled");
```

Chaining `.a.b.c` means "has all three classes." Order doesn't matter and there's no limit.

## 2. Attribute Selectors

Attribute selectors are where CSS gets genuinely expressive, and for Selenium they're often more robust than classes because they tie to semantic HTML rather than presentational class names.

| Syntax | Meaning |
|---|---|
| `[attr]` | Attribute exists (any value, including empty) |
| `[attr="val"]` | Exact match |
| `[attr~="val"]` | Whitespace-separated list contains `val` as a whole word |
| `[attr\|="val"]` | Equals `val` or starts with `val-` (designed for language codes like `en-US`) |
| `[attr^="val"]` | Starts with `val` |
| `[attr$="val"]` | Ends with `val` |
| `[attr*="val"]` | Contains `val` as a substring |
| `[attr="val" i]` | Case-insensitive match (Level 4 flag) |
| `[attr="val" s]` | Case-sensitive match (explicit; rarely needed) |

Examples:

```java
By.cssSelector("input[type='password']");              // exact
By.cssSelector("[data-test='login-button']");          // the saucedemo pattern
By.cssSelector("a[href^='https://']");                 // external links
By.cssSelector("img[src$='.svg']");                    // SVG images
By.cssSelector("div[class*='error']");                 // any class containing "error"
By.cssSelector("input[name~='user']");                 // name is a space-separated list containing "user"
By.cssSelector("html[lang|='en']");                    // en, en-US, en-GB, ...
By.cssSelector("a[title*='login' i]");                 // case-insensitive contains
```

A subtlety worth internalizing: `[class~="foo"]` is functionally identical to `.foo` because the `class` attribute is defined as space-separated. By contrast, `[class*="foo"]` matches any substring, so it would also match `foobar` and `not-foo`; useful for loose matching, dangerous when you don't want it.

Another subtlety: attribute selectors operate on the attribute in the HTML source, not the IDL property. For most attributes these agree, but `value` on an `<input>` is notorious; `[value='x']` matches the initial HTML attribute, not what the user typed. A frequent source of "why doesn't my selector work" in Selenium tests.

## 3. Combinators: Navigating the Tree

Combinators express structural relationships between elements. There are four, and the whitespace around them matters.

```java
By.cssSelector("form input");           // descendant: input anywhere inside form
By.cssSelector("form > input");         // direct child: input whose parent is form
By.cssSelector("label + input");        // adjacent sibling: input immediately after a label
By.cssSelector("h2 ~ p");               // general sibling: any p after an h2, same parent
```

The descendant combinator (whitespace) is the workhorse but the most expensive; the browser walks the entire subtree. The child combinator (`>`) is stricter and usually a better choice when the DOM structure is known and stable. The adjacent sibling (`+`) and general sibling (`~`) combinators only look *forward* among siblings; there is no "previous sibling" or "parent" combinator in traditional CSS. This is historically the most important limitation of CSS selectors versus XPath, and the reason people reached for XPath to locate a row by its cell's text and then navigate back up to the row. (See `:has()` in Section 7 for the modern answer.)

Combinators compose freely:

```java
By.cssSelector("table.inventory > tbody > tr.selected + tr > td:first-child");
```

Read left-to-right: a table with class `inventory`, its direct `tbody` child, a `tr.selected` direct child of that, the `tr` immediately following, and the first `td` direct child of *that*.

## 4. Selector Lists (Grouping)

A comma creates a union; "any of these":

```java
By.cssSelector("h1, h2, h3");
By.cssSelector("input[type='submit'], button[type='submit']");
```

`findElements` returns all matches from all branches of the union, in document order. The historical rule is that if *any* selector in the list is invalid, the entire list is discarded; Level 4 introduces `:is()` with forgiving parsing to avoid this (Section 7).

## 5. Structural Pseudo-Classes

Pseudo-classes match based on state or position rather than attributes. The structural ones let you pick "the third row" or "the last item" without relying on IDs.

```java
By.cssSelector("li:first-child");      // first child of its parent
By.cssSelector("li:last-child");       // last child of its parent
By.cssSelector("li:only-child");       // the sole child of its parent
By.cssSelector("tr:nth-child(3)");     // third tr child (1-indexed)
By.cssSelector("tr:nth-child(odd)");   // 1st, 3rd, 5th, ...
By.cssSelector("tr:nth-child(even)");
By.cssSelector("tr:nth-child(2n+1)");  // formula form: odd rows
By.cssSelector("tr:nth-child(3n)");    // every third row
By.cssSelector("tr:nth-last-child(1)");// counting from the end
```

The formula is `an+b` where `n` starts at 0 and increments. `2n` is every even, `2n+1` every odd, `3n+2` the 2nd/5th/8th, and so on. You can also write `-n+3` to mean "the first three."

There's a critical gotcha with `:nth-child` that catches even experienced engineers: it counts among *all siblings*, not siblings of the same type. If your parent contains a mix of elements, `li:nth-child(2)` means "the second child of its parent, which also happens to be an `li`"; if the second child is a `div`, the selector matches nothing. The type-aware variants fix this:

```java
By.cssSelector("li:first-of-type");
By.cssSelector("li:nth-of-type(3)");    // third li among its siblings, regardless of other elements
By.cssSelector("li:last-of-type");
By.cssSelector("li:nth-last-of-type(2)");
By.cssSelector("li:only-of-type");
```

For tables where `<tbody>` contains only `<tr>` elements, `nth-child` and `nth-of-type` are equivalent; but in heterogeneous containers they diverge, and `nth-of-type` is almost always what you actually mean.

There's also `:root` (the `<html>` element) and `:empty` (elements with no children, not even text nodes; whitespace counts as a text node, so beware).

## 6. State and UI Pseudo-Classes

These match form and interaction state, which is exactly what you want when testing.

```java
By.cssSelector("input:checked");        // checkboxes and radios that are checked
By.cssSelector("input:disabled");
By.cssSelector("input:enabled");
By.cssSelector("input:required");
By.cssSelector("input:optional");
By.cssSelector("input:read-only");
By.cssSelector("input:read-write");
By.cssSelector("input:placeholder-shown");  // the placeholder is currently visible (i.e., empty)
By.cssSelector("input:focus");          // currently focused
By.cssSelector(":target");              // element whose ID matches the URL fragment
```

`:checked` deserves special mention because it reflects the *current* state, unlike the `checked` attribute, which reflects the initial HTML. If the user (or your test) clicks a checkbox, `[checked]` won't match but `:checked` will. This is the canonical example of the attribute-vs-property distinction biting you in tests.

`:valid`, `:invalid`, `:in-range`, and `:out-of-range` exist for form validation state but are less commonly used in Selenium tests.

## 7. Negation and Logical Pseudo-Classes

The Level 3 `:not()` takes a simple selector and inverts it:

```java
By.cssSelector("input:not([type='hidden'])");
By.cssSelector("li:not(.disabled):not(.hidden)");
By.cssSelector("tr:not(:first-child)");              // all rows except the header row
```

Level 4 expanded `:not()` to accept full selector lists, and added three new logical pseudo-classes that all modern browsers (and therefore Selenium) support:

```java
By.cssSelector("button:not(.primary, .secondary)");  // Level 4: selector list in :not
By.cssSelector(":is(h1, h2, h3) + p");               // any of h1/h2/h3 followed by p
By.cssSelector(":where(section, article) .title");   // like :is but zero specificity
By.cssSelector("li:has(> a.active)");                // has: parent selector! li containing an active anchor child
```

`:has()` is the long-awaited "parent selector." Before it landed in browsers (Chrome 105, Safari 15.4, Firefox 121), the absence of a parent combinator was *the* reason Selenium users fell back to XPath. It's now genuinely usable:

```java
// A row that contains a checked checkbox
By.cssSelector("tr:has(input:checked)");
// An inventory card whose add-to-cart button has been replaced by a remove button
By.cssSelector(".inventory_item:has([data-test^='remove'])");
```

The one thing `:has()` still can't do is match on text content; for that, XPath's `contains(text(), ...)` remains the only option. Keep this in mind; it's the main residual reason to reach for XPath.

`:is()` and `:where()` differ only in CSS specificity, which is irrelevant to Selenium since you're only using them for matching. Both accept a selector list and match if any branch matches, with forgiving parsing; if one branch is invalid, the others still work.

## 8. Pseudo-Elements (and Why They Don't Help You)

CSS also has pseudo-*elements* (`::before`, `::after`, `::first-line`, `::placeholder`, etc.), written with double colons. These are generated or abstract boxes that don't correspond to real DOM nodes, so Selenium cannot locate them with `findElement`. If you need to assert on content generated by `::before`, use `JavascriptExecutor` with `getComputedStyle(el, '::before').getPropertyValue('content')`. Don't waste time trying `By.cssSelector("div::before")` and wondering why it throws.

## 9. Escaping and Quoting

When attribute values or IDs contain special characters, you need to escape them. This matters in practice because modern frameworks generate IDs with colons, slashes, and brackets.

```java
// <div id="user:profile[0]">
By.cssSelector("#user\\:profile\\[0\\]");              // backslash-escape special chars
// Or, more readably, use an attribute selector:
By.cssSelector("[id='user:profile[0]']");
```

Note the double backslash in the Java string literal; one for Java, one for CSS. For anything non-trivial, the attribute-selector form is more readable and avoids the double-escaping headache. **When an ID looks weird, reach for `[id='...']` instead of `#...`.**

Quotes inside attribute selectors can be either single or double; pick whichever doesn't collide with your Java string:

```java
By.cssSelector("input[placeholder='Enter your name']");
By.cssSelector("input[placeholder=\"Enter your name\"]");
```

## 10. What CSS Cannot Do

Knowing the limits is what separates junior from senior selector work. CSS selectors in Selenium cannot:

- **Match on text content.** No `:contains()`; it was proposed and dropped. XPath's `//button[text()='Login']` or `//button[contains(., 'Login')]` has no CSS equivalent.
- **Walk backward to ancestors by arbitrary depth.** `:has()` closes most of this gap, but it's forward-scoped from a starting element; there's no "give me the ancestor matching X" operator. XPath's `ancestor::` axis does this directly.
- **Match on computed style.** CSS selectors match on the DOM, not on rendered appearance. An element hidden by `display: none` still matches; use `WebElement.isDisplayed()` to filter.
- **Match on arbitrary JavaScript conditions.** For that, you're in `JavascriptExecutor` territory.

Rule of thumb: reach for CSS by default because it's faster, more readable, and the engine is native to the browser; reach for XPath specifically when you need text matching, ancestor navigation, or complex positional logic across the tree.

## 11. Selenium-Specific Notes

- `By.cssSelector` throws `InvalidSelectorException` on malformed selectors, not `NoSuchElementException`. Worth distinguishing so a typo doesn't masquerade as a missing element.
- `findElement` returns the first match in document order; `findElements` returns all matches. "First" means first in tree traversal, which is not necessarily first visually when CSS reorders with flex/grid.
- You can scope a search to a subtree by calling `findElement` on an existing `WebElement`. The CSS selector is evaluated relative to that element; selectors beginning with a combinator (e.g., `> .child`) are supported by the browser but not always portable; prefer `.child` or `:scope > .child`.
- Selenium 4 added `RelativeLocator` (`By.below(...)`, `By.toRightOf(...)`) for positional queries that CSS can't express.
- Performance-wise, CSS selectors are very fast; IDs and classes are indexed by the browser. The cost is almost always in the WebDriver-to-browser round-trip, not in selector evaluation. Optimize for readability and resilience to DOM changes, not for shaved microseconds.

---

# Part 2 Exercises on saucedemo.com

Work through each exercise in your IDE before expanding the solution. Setup: all exercises assume a `WebDriver driver` already logged in with `standard_user` / `secret_sauce` and sitting on `/inventory.html`, unless stated otherwise.

## Section 1: Basics (ID, class, type)

### Exercise 1.1
Locate the username input on the login page by its ID.

*Hint: the simplest selector that works.*

<details>
<summary>Show solution</summary>

```java
WebElement username = driver.findElement(By.cssSelector("#user-name"));
```

Saucedemo gives this input both `id="user-name"` and `data-test="username"`. Either works, but for a pure ID exercise `#user-name` is canonical. In real test code I'd actually prefer `[data-test='username']`; see Exercise 2.1.
</details>

### Exercise 1.2
Count how many inventory items are on the page.

*Hint: a class selector and `findElements`.*

<details>
<summary>Show solution</summary>

```java
int count = driver.findElements(By.cssSelector(".inventory_item")).size();
assertEquals(6, count);
```

`.inventory_item` is the card wrapper for each product. Note the distinction between `.inventory_item` (the card), `.inventory_item_name` (the product name), and `.inventory_item_price`.
</details>

### Exercise 1.3
Select the shopping cart link in the header.

*Hint: chain a type and a class.*

<details>
<summary>Show solution</summary>

```java
WebElement cart = driver.findElement(By.cssSelector("a.shopping_cart_link"));
```

You could write just `.shopping_cart_link` and it would work. Chaining the type is slightly more defensive; if the page later wraps the class on a `div`, your test fails loudly rather than silently picking up the wrong element.
</details>

---

## Section 2: Attribute Selectors

### Exercise 2.1
Click the "Add to cart" button for the Sauce Labs Backpack using its `data-test` attribute.

*Hint: exact-match attribute selector.*

<details>
<summary>Show solution</summary>

```java
driver.findElement(By.cssSelector("[data-test='add-to-cart-sauce-labs-backpack']")).click();
```

This is *the* saucedemo pattern worth drilling in. The `data-test` attribute exists precisely so tests don't chase CSS classes that change when designers redecorate. Compare to a fragile alternative like `.inventory_item:nth-child(1) button.btn_inventory`; works today, breaks the moment someone re-sorts the default product order.
</details>

### Exercise 2.2
Find all "Add to cart" buttons on the page regardless of which product they belong to.

*Hint: prefix match with `^=`.*

<details>
<summary>Show solution</summary>

```java
List<WebElement> addButtons = driver.findElements(By.cssSelector("[data-test^='add-to-cart']"));
assertEquals(6, addButtons.size());
```

Every add button's `data-test` starts with `add-to-cart-` followed by the slugified product name. `^=` gives you the whole family in one selector.
</details>

### Exercise 2.3
Find the product image for the backpack. All product images end in `.jpg`, but the backpack's URL specifically contains `backpack`.

*Hint: substring match with `*=`.*

<details>
<summary>Show solution</summary>

```java
WebElement img = driver.findElement(By.cssSelector("img[src*='backpack']"));
```

`*=` is the loosest attribute matcher and the most dangerous. It's fine here because `backpack` is unambiguous in the image URL space, but I'd think twice before using `[class*='error']` in production; a later refactor introducing a `cleared-error-state` class will silently match.
</details>

### Exercise 2.4
Locate the password field specifically; not just any input of type text.

*Hint: exact-match on `type`.*

<details>
<summary>Show solution</summary>

```java
WebElement password = driver.findElement(By.cssSelector("input[type='password']"));
```

Unambiguous on the login page. On a registration page with "password" and "confirm password," you'd need to disambiguate further; selectors should be specific enough but no more.
</details>

---

## Section 3: Combinators

### Exercise 3.1
Select the product name that lives inside the first inventory item's name container.

*Hint: direct-child combinator combined with a descendant combinator.*

<details>
<summary>Show solution</summary>

```java
WebElement name = driver.findElement(
    By.cssSelector(".inventory_item_label > a .inventory_item_name"));
```

Saucedemo wraps each product name in `<div class="inventory_item_label"><a ...><div class="inventory_item_name">Sauce Labs Backpack</div></a>...</div>`. The `>` says "anchor that is a direct child of the label div"; the following whitespace says "name div anywhere inside that anchor."
</details>

### Exercise 3.2
From the cart page (after adding an item and navigating there), select the cart item's price.

*Hint: start by inspecting the cart DOM.*

<details>
<summary>Show solution</summary>

```java
driver.findElement(By.cssSelector("[data-test='add-to-cart-sauce-labs-backpack']")).click();
driver.findElement(By.cssSelector(".shopping_cart_link")).click();

WebElement price = driver.findElement(By.cssSelector(".cart_item .inventory_item_price"));
```

I deliberately used a descendant combinator rather than a strict sibling chain; an over-specified selector would be brittle. Combinators are a tool for precision, but precision is only a virtue when the structure is stable. Under-specify when you can.
</details>

### Exercise 3.3
Select every "Add to cart" button *except* the one belonging to the first product card, using a sibling combinator.

*Hint: general sibling `~` on `.inventory_item`.*

<details>
<summary>Show solution</summary>

```java
List<WebElement> buttons = driver.findElements(
    By.cssSelector(".inventory_item:first-child ~ .inventory_item button"));
assertEquals(5, buttons.size());
```

This is contrived on purpose; `.inventory_item:not(:first-child) button` is easier to read; but it's the clearest demonstration of `~` on real markup. Which form would you prefer in a code review? Both are valid answers.
</details>

---

## Section 4: Structural Pseudo-Classes

### Exercise 4.1
Select the third inventory item on the page.

*Hint: `nth-of-type` vs `nth-child`; does it matter here?*

<details>
<summary>Show solution</summary>

```java
WebElement third = driver.findElement(
    By.cssSelector(".inventory_list > .inventory_item:nth-of-type(3)"));
```

`.inventory_list` currently contains only `.inventory_item` children, so `nth-child(3)` and `nth-of-type(3)` agree today. But if saucedemo later inserted a promotional banner as the first child, `nth-child(3)` would silently return the second *product* while `nth-of-type(3)` would still correctly return the third `.inventory_item`. Use the resilient form as the default.
</details>

### Exercise 4.2
Select every other row of products (1st, 3rd, 5th).

*Hint: formula or keyword form of `nth-of-type`.*

<details>
<summary>Show solution</summary>

```java
List<WebElement> odds = driver.findElements(
    By.cssSelector(".inventory_list > .inventory_item:nth-of-type(odd)"));
assertEquals(3, odds.size());
```

Equivalent formula: `:nth-of-type(2n+1)`. Try writing `:nth-of-type(3n)` for every third item as a variation.
</details>

### Exercise 4.3
Select the last inventory item regardless of how many items the page has.

*Hint: `:last-of-type`.*

<details>
<summary>Show solution</summary>

```java
WebElement last = driver.findElement(
    By.cssSelector(".inventory_list > .inventory_item:last-of-type"));
```

Contrast with a hardcoded `:nth-of-type(6)`. Both work today; `:last-of-type` survives a product catalog change. Prefer selectors that encode intent ("the last one") over selectors that encode current state ("the sixth one").
</details>

---

## Section 5: State Pseudo-Classes

### Exercise 5.1
After navigating to checkout step one and leaving the first name field blank, verify the field is showing its placeholder.

*Hint: `:placeholder-shown`.*

<details>
<summary>Show solution</summary>

```java
// assume: added an item, clicked cart, clicked checkout
WebElement firstName = driver.findElement(
    By.cssSelector("[data-test='firstName']:placeholder-shown"));
assertNotNull(firstName);
```

`:placeholder-shown` matches when the field is empty and displaying its placeholder. It's the cleanest way to assert "this field hasn't been filled in" without resorting to `getAttribute("value")` comparisons. Note that `[placeholder]` (attribute exists) would match regardless of whether the placeholder is currently visible; that's the key distinction.
</details>

### Exercise 5.2
On the checkout overview page, locate a disabled button. Saucedemo doesn't natively disable buttons, so inject a disabled state via JS first; the selector is the point.

*Hint: `:disabled`.*

<details>
<summary>Show solution</summary>

```java
((JavascriptExecutor) driver).executeScript(
    "document.querySelector('[data-test=finish]').disabled = true;");

WebElement disabled = driver.findElement(By.cssSelector("button:disabled"));
```

`:disabled` reflects the live property. For boolean attributes like `disabled`, setting the property reflects to the attribute, so `[disabled]` also matches; but for form validation state (`:valid`, `:invalid`), the pseudo-class is the only thing that tracks live state.
</details>

---

## Section 6: Logical Pseudo-Classes (`:not`, `:is`, `:has`)

### Exercise 6.1
After adding the backpack to the cart, select all product cards that *don't* currently have a "Remove" button.

*Hint: `:not()` combined with `:has()`.*

<details>
<summary>Show solution</summary>

```java
driver.findElement(By.cssSelector("[data-test='add-to-cart-sauce-labs-backpack']")).click();

List<WebElement> notAdded = driver.findElements(
    By.cssSelector(".inventory_item:not(:has([data-test^='remove']))"));
assertEquals(5, notAdded.size());
```

Before `:has()` shipped in browsers, this required either XPath or two queries with Java-side filtering. Now it reads almost like English: "inventory items that do not have a descendant whose data-test starts with remove."
</details>

### Exercise 6.2
Find the inventory item card that contains the Sauce Labs Backpack; selecting the *card*, not the name; and make it work whether the item is currently added to cart or not.

*Hint: `:has()` with a selector list to cover both states.*

<details>
<summary>Show solution</summary>

```java
WebElement card = driver.findElement(
    By.cssSelector(".inventory_item:has([data-test='add-to-cart-sauce-labs-backpack'], " +
                   "[data-test='remove-sauce-labs-backpack'])"));
```

Two teaching points. First, `:has()` is the parent selector; we're selecting the card based on a property of one of its children, which was impossible in pure CSS until recently. Second, the selector list inside `:has()` handles both cart states. If you needed to match on the product *name text* ("Sauce Labs Backpack"), CSS still can't do it; that's the residual XPath use case.
</details>

### Exercise 6.3
Select every heading-like element in the header area; the app logo and the page title; in a single selector.

*Hint: `:is()` for brevity.*

<details>
<summary>Show solution</summary>

```java
List<WebElement> headings = driver.findElements(
    By.cssSelector(":is(.app_logo, .title)"));
```

This is behaviorally identical to `.app_logo, .title`. The reason to show `:is()` is that it composes: `header :is(.app_logo, .title)` is cleaner than `header .app_logo, header .title`. On small selectors it's overkill; on larger ones it saves duplication. `:is()` also has forgiving parsing; an invalid branch doesn't kill the whole selector.
</details>

---

## Capstone Challenge

After sorting the inventory page by price high-to-low, select the price element of the most expensive item using only a CSS selector. Think about which selector will remain correct if the catalog grows or shrinks, and which will break.

<details>
<summary>Show one possible solution and discussion</summary>

```java
Select sort = new Select(driver.findElement(By.cssSelector("[data-test='product-sort-container']")));
sort.selectByValue("hilo");

WebElement topPrice = driver.findElement(
    By.cssSelector(".inventory_list > .inventory_item:first-of-type .inventory_item_price"));
```

After the sort, the DOM order reflects the price order, so "the first `.inventory_item`" *is* "the most expensive item." That's a subtle dependency: the selector is correct only because of the sort state, not because of anything structural. In a code review I'd insist on a comment tying the selector to the sort action, or better, a helper method `mostExpensiveItemPrice()` that encapsulates both the sort and the selection so the coupling is explicit.

This is also the moment to note what CSS *can't* do here: "select the price element whose numeric value is the maximum" is not expressible in CSS at any level. Max-finding is a Java-side operation; grab all `.inventory_item_price` elements, parse, reduce. The selector's job ends at "get me the candidates."
</details>

---

## Rendering Note

This document uses `<details>`/`<summary>` for collapsible solutions. This renders on GitHub, most static site generators (Jekyll, Hugo, Docusaurus, MkDocs), and VS Code's preview. If your publishing pipeline strips HTML, the solutions will appear expanded rather than hidden, but the document remains readable.
