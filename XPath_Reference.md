# XPath for Selenium WebDriver

A complete reference and hands-on exercise set for locating elements with XPath in Selenium WebDriver (Java). 
Selenium delegates XPath evaluation to the browser's native XPath engine, which implements XPath 1.0. Despite being 
an older spec (1999), XPath 1.0 is the only version browsers support natively, and it remains useful; 
it can do things CSS selectors still cannot.

Note: XPath was originally designed for navigating XML documents. HTML is close enough to XML that the same language 
works, and browsers have built-in XPath engines for exactly this purpose. When you write `By.xpath(...)` in Selenium, 
you're calling that built-in engine directly.

Exercises target [saucedemo.com](https://www.saucedemo.com), which uses `data-test` attributes consistently and makes an excellent teaching 
target for comparing XPath and CSS selector approaches side by side.

**Additional Reference**: [https://devhints.io/xpath](https://devhints.io/xpath)

---

## Table of Contents

**Part 1 Reference**
1. [The Basics: Path Expressions](#1-the-basics-path-expressions)
2. [Predicates](#2-predicates)
3. [Axes: Navigating the Tree](#3-axes-navigating-the-tree)
4. [XPath Functions](#4-xpath-functions)
5. [Operators](#5-operators)
6. [What XPath Can Do That CSS Cannot](#6-what-xpath-can-do-that-css-cannot)
7. [Selenium-Specific Notes](#7-selenium-specific-notes)

**Part 2 Exercises on saucedemo.com**

- [Section 1: Basics](#section-1-basics-path-expressions-and-attributes)
- [Section 2: Predicates](#section-2-predicates)
- [Section 3: Axes](#section-3-axes)

---

# Part 1 Reference

## 1. The Basics: Path Expressions

XPath expressions describe a path through the DOM tree, much like a file system path. The two fundamental forms are 
absolute paths (starting from the document root) and relative paths (starting from anywhere). Use a single slash for absolute paths, and a double slash for relative paths.

```java
driver.findElement(By.xpath("/html/body/div/input"));   // absolute path from the root
driver.findElement(By.xpath("//input"));                 // relative: any input anywhere in the document
driver.findElement(By.xpath("//div/input"));              // relative: any input that is a direct child of a div
driver.findElement(By.xpath("//div//input"));             // relative: any input anywhere inside a div
```

The key symbols:

| Symbol | Meaning |
|---|---|
| `/` | Direct child (or document root when at the start) |
| `//` | Descendant (any depth); at the start, means "anywhere in the document" |
| `.` | Current node |
| `..` | Parent node |
| `*` | Any element |
| `@` | Attribute prefix |

**Absolute paths** like `/html/body/div[2]/form/input[1]` are brittle; they break the moment anyone adds a wrapper 
`div` or rearranges the page. They have almost no place in test code. **Relative paths** starting with `//` are the 
standard approach because they match on element characteristics rather than tree position.

The wildcard `*` matches any element name:

```java
By.xpath("//*[@id='login-button']");    // any element with this ID
By.xpath("//form/*");                    // all direct children of any form
```

Note: `//*` ("any element anywhere in the document") is the XPath equivalent of CSS's `*`, but you'll see it much 
more often in XPath because it's the standard entry point for attribute-based searches.

## 2. Predicates

Predicates are XPath's filtering mechanism; they go inside square brackets `[]` and narrow a node set down to only 
the nodes that satisfy a condition. This is where most of the real work happens.

### Attribute predicates

The `@` symbol accesses attributes:

```java
By.xpath("//input[@type='password']");                       // exact attribute match
By.xpath("//*[@data-test='login-button']");                  // the saucedemo pattern
By.xpath("//input[@id='user-name']");                        // by ID
By.xpath("//div[@class='inventory_item']");                  // by class (exact match on entire attribute)
By.xpath("//*[@data-test]");                                  // attribute exists (any value)
```

**Important**: `[@class='inventory_item']` does an exact string match on the *entire* class attribute value. If the 
element has `class="inventory_item active"`, this won't match because the full string is `"inventory_item active"`, 
not `"inventory_item"`. This is a critical difference from CSS's `.inventory_item`, which matches any element whose 
class list includes `inventory_item` regardless of other classes. To handle multi-class elements in XPath, use 
`contains()`:

```java
By.xpath("//div[contains(@class, 'inventory_item')]");
```

But be aware that `contains(@class, 'item')` would also match a class like `line-item` or `item-list`; it's a 
substring match, not a word match. The precise equivalent of CSS's `.foo` is:

```java
By.xpath("//div[contains(concat(' ', normalize-space(@class), ' '), ' inventory_item ')]");
```

This is ugly enough that most people just use `contains()` and accept the substring risk, or use CSS selectors 
when class matching is the primary need.

### Positional predicates

XPath positions are 1-indexed:

```java
By.xpath("//ul/li[1]");                  // first li child
By.xpath("//ul/li[last()]");             // last li child
By.xpath("//ul/li[last()-1]");           // second-to-last
By.xpath("//ul/li[position()<=3]");      // first three
By.xpath("(//div[@class='inventory_item'])[3]");  // third match overall
```

Note the critical difference between `//ul/li[3]` and `(//ul/li)[3]`. The first means "for each `ul`, select its 
third `li` child"; if there are multiple `ul` elements, you could get multiple results. The second means "find all 
`ul/li` matches in the document, then take the third one overall." The parentheses change grouping, not just precedence.

### Text predicates

This is XPath's killer feature; CSS has no equivalent:

```java
By.xpath("//button[text()='Login']");                    // exact text match
By.xpath("//button[normalize-space(text())='Login']");   // ignoring leading/trailing whitespace
By.xpath("//button[contains(text(), 'Log')]");           // text contains substring
By.xpath("//*[text()='Sauce Labs Backpack']");           // any element with this exact text
```

`text()` matches direct text node children of the element. If the text is spread across child elements (e.g., 
`<span>Sauce <b>Labs</b> Backpack</span>`), `text()` only sees the direct text nodes (`"Sauce "` and `" Backpack"`). 
To match the full concatenated text including children, use `.` (the current node, which evaluates to the string 
value of the entire subtree):

```java
By.xpath("//span[contains(., 'Sauce Labs Backpack')]");  // matches text across child elements
By.xpath("//span[.='Sauce Labs Backpack']");             // exact match on full concatenated text
```

### Multiple predicates

Predicates can be chained; each one filters the results of the previous:

```java
By.xpath("//input[@type='text'][@name='username']");     // must satisfy both
By.xpath("//div[@class='inventory_item'][3]");            // inventory_items, then the third one
```

This is equivalent to logical AND. For OR, use the `or` operator inside a single predicate (see Section 5).

## 3. Axes: Navigating the Tree

Axes are XPath's mechanism for traversing the DOM in any direction. This is the fundamental advantage over CSS, 
which can only go down and forward. The full axis syntax is `axis::node-test[predicate]`, though most people use 
the abbreviated forms where they exist.

| Axis | Meaning | Abbreviated Form |
|---|---|---|
| `self` | The current node itself | `.` |
| `child` | Direct children | (default; just the element name) |
| `parent` | Direct parent | `..` |
| `ancestor` | All ancestors up to the root | (none) |
| `ancestor-or-self` | Current node plus all ancestors | (none) |
| `descendant` | All descendants at any depth | (none; but `//` after a step is `descendant-or-self`) |
| `descendant-or-self` | Current node plus all descendants | `//` |
| `following-sibling` | Siblings after the current node | (none) |
| `preceding-sibling` | Siblings before the current node | (none) |
| `following` | Everything after in document order (not ancestors) | (none) |
| `preceding` | Everything before in document order (not descendants) | (none) |
| `attribute` | Attributes of the current node | `@` |

The axes you'll use most in Selenium tests:

```java
// parent: go up one level
By.xpath("//input[@id='user-name']/parent::div");         // the div that directly contains this input
By.xpath("//input[@id='user-name']/..");                   // same thing, abbreviated

// ancestor: go up to any level
By.xpath("//div[@class='inventory_item_name']/ancestor::div[@class='inventory_item']");
// "find the product name, then go up to the card that contains it"

// following-sibling: next siblings
By.xpath("//label[text()='Username']/following-sibling::input");
// "find the label with text 'Username', then the input that comes after it among siblings"

// preceding-sibling: previous siblings
By.xpath("//input[@id='password']/preceding-sibling::input");
// "find the input before the password field"
```

The `ancestor` axis is the classic reason to reach for XPath over CSS. The pattern is: find an element you can 
identify (like a product name by its text), then navigate *up* to a container (like the product card), then 
optionally back *down* to a different child of that container (like the price or add-to-cart button):

```java
// "Find the price of the Sauce Labs Backpack"
By.xpath("//*[text()='Sauce Labs Backpack']/ancestor::div[@class='inventory_item']//div[@class='inventory_item_price']");
```

This three-step pattern; find known element → go up → come back down; is the most powerful and most common XPath 
idiom in Selenium testing. CSS's `:has()` can now do some of this, but XPath's version is more flexible because 
it can climb an arbitrary number of levels and the `ancestor` axis can have its own predicates.

**Axes you'll rarely use**: `following` and `preceding` (not `following-sibling`/`preceding-sibling`) select *all* 
nodes after/before the current node in document order, crossing parent boundaries. They're powerful in theory but 
produce brittle selectors in practice because they depend on the full page structure. Stick to the sibling variants.

## 4. XPath Functions

XPath 1.0 includes a small but useful function library. The ones that matter for Selenium:

### String functions

```java
By.xpath("//button[contains(text(), 'Add')]");            // substring search
By.xpath("//button[starts-with(text(), 'Add')]");         // prefix match
By.xpath("//input[string-length(@value) > 0]");           // attribute has non-empty value
By.xpath("//div[normalize-space(text())='Login']");       // trim whitespace before comparing
By.xpath("//div[contains(normalize-space(.), 'Total')]"); // combining functions
```

| Function | Signature | Returns |
|---|---|---|
| `contains(haystack, needle)` | `contains(string, string)` | `true` if haystack contains needle |
| `starts-with(string, prefix)` | `starts-with(string, string)` | `true` if string starts with prefix |
| `normalize-space(string)` | `normalize-space(string?)` | String with leading/trailing whitespace removed and internal runs collapsed to a single space |
| `string-length(string)` | `string-length(string?)` | Number of characters |
| `concat(str1, str2, ...)` | `concat(string, string, string*)` | Concatenated string |
| `substring(str, start, len)` | `substring(string, number, number?)` | Substring (1-indexed) |
| `translate(str, from, to)` | `translate(string, string, string)` | Character-by-character replacement |

Note: XPath 1.0 has no `ends-with()` function (that was added in XPath 2.0, which browsers don't support). 
The workaround:

```java
// "src attribute ends with '.jpg'"
By.xpath("//img[substring(@src, string-length(@src) - 3) = '.jpg']");
```

This is awkward enough that for suffix matching, CSS's `[src$='.jpg']` is cleaner.

`translate()` is XPath 1.0's only tool for case-insensitive matching. It does character-by-character substitution:

```java
// case-insensitive text match
By.xpath("//button[translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='login']");
```

This is verbose but works. CSS's `[attr='val' i]` flag is far more readable for attribute matching, but for 
text-content matching, this is your only option.

### Numeric functions

Rarely used in Selenium, but available:

```java
By.xpath("//ul/li[position() mod 2 = 1]");   // odd-positioned items (like CSS :nth-child(odd))
By.xpath("//ul/li[count(./a) > 1]");          // li elements containing more than one link
```

### Boolean functions

```java
By.xpath("//input[not(@disabled)]");              // not disabled
By.xpath("//div[@class='a' or @class='b']");      // either class (but see the exact-match caveat)
By.xpath("//input[@type='text' and @name='q']");  // both conditions
```

`not()` is a function in XPath, not a pseudo-class as in CSS. It can wrap any predicate expression.

## 5. Operators

XPath supports comparison and logical operators inside predicates:

| Operator | Meaning | Example |
|---|---|---|
| `=` | Equals | `@type='text'` |
| `!=` | Not equals | `@type!='hidden'` |
| `<`, `>`, `<=`, `>=` | Numeric comparison | `position()<=3` |
| `and` | Logical AND | `@type='text' and @name='q'` |
| `or` | Logical OR | `@class='a' or @class='b'` |
| `not()` | Logical NOT (function) | `not(@disabled)` |
| `\|` | Union (combines node sets) | `//h1 \| //h2 \| //h3` |
| `+`, `-`, `*`, `div`, `mod` | Arithmetic | `position() mod 2 = 0` |

Note: the union operator `|` is *not* the same as `or`. The union `|` combines two complete XPath expressions 
into a single result set (like CSS's comma). The `or` keyword is a logical operator used inside a predicate. Compare:

```java
By.xpath("//h1 | //h2 | //h3");                         // union: all h1, h2, and h3 elements
By.xpath("//*[@class='a' or @class='b']");               // or: any element whose class is 'a' or 'b'
```

Also note: in Java string literals, `<` and `>` work fine inside XPath strings passed to `By.xpath()`. But if 
you're embedding XPath in XML (like a test configuration file), you'd need to use `&lt;` and `&gt;`. In normal 
Selenium Java code this isn't a concern.

## 6. What XPath Can Do That CSS Cannot

XPath's advantages over CSS selectors boil down to three capabilities:

- **Text matching.** `//button[text()='Login']` and `//button[contains(., 'Add')]` have no CSS equivalent. 
This is the single most common reason to use XPath.
- **Upward navigation.** `parent::`, `ancestor::`, and `..` let you climb the tree. CSS's `:has()` covers some 
of this, but XPath's ancestor axis is more general; you can climb multiple levels and apply predicates at each step.
- **Preceding sibling navigation.** CSS's `+` and `~` only go forward. XPath's `preceding-sibling::` goes backward.

In return, CSS has a few advantages over XPath:

- **Class matching.** CSS's `.foo` handles multi-class attributes naturally; XPath requires the `contains()` workaround.
- **Suffix matching.** CSS's `[attr$='val']` is clean; XPath 1.0 has no `ends-with()`.
- **Pseudo-classes for state.** CSS's `:checked`, `:disabled`, `:placeholder-shown`, etc., reflect live DOM state. 
XPath can only check attributes (`@checked`, `@disabled`), which reflect the initial HTML, not the current state.
- **Readability.** For simple selectors, CSS is shorter and more familiar to most developers.

Rule of thumb: reach for CSS by default; reach for XPath when you need text matching, ancestor navigation, or 
backward sibling traversal. It's perfectly fine to mix both in the same test suite; use whichever is clearer for 
each specific element.

## 7. Selenium-Specific Notes

- `By.xpath` throws `InvalidSelectorException` on malformed XPath, not `NoSuchElementException`. Same as CSS; 
worth distinguishing so a syntax error doesn't masquerade as a missing element.
- `findElement` returns the first match in document order; `findElements` returns all matches, just like CSS.
- You can scope an XPath search to a subtree by calling `findElement` on an existing `WebElement`. When you do 
this, start the XPath with `.//` (dot-slash-slash), not `//`. Plain `//` searches from the document root regardless 
of the calling element; `.//` means "descendants of *this* node":

```java
WebElement card = driver.findElement(By.xpath("//div[@class='inventory_item'][1]"));
// WRONG: searches entire document despite being called on card
WebElement price = card.findElement(By.xpath("//div[@class='inventory_item_price']"));
// RIGHT: searches only within card
WebElement price = card.findElement(By.xpath(".//div[@class='inventory_item_price']"));
```

This is one of the most common XPath mistakes in Selenium tests, and it leads to subtle bugs; the test passes 
because it happens to find the right element from the full document, then breaks when the page order changes.

- XPath evaluation is somewhat slower than CSS in most browsers because CSS selectors are more heavily optimized 
internally. In practice, the difference is negligible next to the WebDriver round-trip time. Don't let performance 
be the reason you avoid XPath; let readability and resilience guide the choice.
- The `normalize-space()` function is your friend. Real-world HTML is full of whitespace inconsistencies; a 
`text()='Login'` match that works in dev can break in production because of an extra newline. 
`normalize-space(.)='Login'` is more robust.

---

# Part 2 Exercises on saucedemo.com

Work through each exercise in your IDE before expanding the solution. Setup: all exercises assume a `WebDriver driver` 
already logged in with `standard_user` / `secret_sauce` and sitting on `/inventory.html`, unless stated otherwise.

## Section 1: Basics (path expressions and attributes)

### Exercise 1.1
Locate the username input on the login page using an XPath attribute selector.

*Hint: use `//*` with an attribute predicate.*

<details>
<summary>Show solution</summary>

```java
WebElement username = driver.findElement(By.xpath("//*[@id='user-name']"));
```

`//*` means "any element anywhere in the document." The predicate `[@id='user-name']` filters to the one with a 
matching ID. You could also write `//input[@id='user-name']` to be more specific about element type; both work.
</details>

### Exercise 1.2
Count how many inventory items are on the page using an XPath class selector.

*Hint: use `findElements` with a class attribute predicate. Watch out for the exact-match gotcha.*

<details>
<summary>Show solution</summary>

```java
int count = driver.findElements(By.xpath("//div[@class='inventory_item']")).size();
assertEquals(6, count);
```

This works because saucedemo's inventory items have `class="inventory_item"` as the *entire* class attribute value; 
no additional classes. If the element had `class="inventory_item highlighted"`, you'd need 
`//div[contains(@class, 'inventory_item')]` instead. Always inspect the actual DOM to check.
</details>

### Exercise 1.3
Select the shopping cart link using an XPath that specifies both element type and class.

*Hint: combine a tag name with a class attribute predicate.*

<details>
<summary>Show solution</summary>

```java
WebElement cart = driver.findElement(By.xpath("//a[@class='shopping_cart_link']"));
```

Compare to the CSS version `a.shopping_cart_link`. XPath's attribute syntax is more verbose, but the logic is 
the same: match an `a` element whose class is `shopping_cart_link`.
</details>

### Exercise 1.4
Find any element on the page that has a `data-test` attribute, regardless of its value.

*Hint: attribute-exists predicate with no value comparison.*

<details>
<summary>Show solution</summary>

```java
List<WebElement> testElements = driver.findElements(By.xpath("//*[@data-test]"));
```

`[@data-test]` without `='...'` means "the attribute exists." This is the same as CSS's `[data-test]`. Useful for 
discovering all test-hookable elements on a page during test development.
</details>

---

## Section 2: Predicates

### Exercise 2.1
Click the "Add to cart" button for the Sauce Labs Backpack using its `data-test` attribute.

*Hint: exact-match attribute predicate on `data-test`.*

<details>
<summary>Show solution</summary>

```java
driver.findElement(By.xpath("//*[@data-test='add-to-cart-sauce-labs-backpack']")).click();
```

Identical in logic to the CSS version `[data-test='add-to-cart-sauce-labs-backpack']`. For exact attribute matches, 
CSS and XPath are equally capable; CSS is just shorter.
</details>

### Exercise 2.2
Find all "Add to cart" buttons using a substring match on the `data-test` attribute.

*Hint: XPath 1.0 has no `starts-with` equivalent of CSS's `^=`... or does it?*

<details>
<summary>Show solution</summary>

```java
List<WebElement> addButtons = driver.findElements(
    By.xpath("//*[starts-with(@data-test, 'add-to-cart')]"));
assertEquals(6, addButtons.size());
```

XPath does have `starts-with()`; it's a function rather than an operator. Compare to CSS's `[data-test^='add-to-cart']`; 
same result, different syntax.
</details>

### Exercise 2.3
Find the Sauce Labs Backpack product by its visible text on the page.

*Hint: this is XPath's killer feature; something CSS cannot do at all.*

<details>
<summary>Show solution</summary>

```java
WebElement backpackName = driver.findElement(
    By.xpath("//*[text()='Sauce Labs Backpack']"));
```

No CSS equivalent exists. `text()='Sauce Labs Backpack'` matches an element whose direct text content is exactly 
that string. If the page had extra whitespace (e.g., `\n  Sauce Labs Backpack\n`), you'd need 
`normalize-space(text())='Sauce Labs Backpack'` or `normalize-space(.)='Sauce Labs Backpack'` instead.
</details>

### Exercise 2.4
Select the third inventory item on the page using a positional predicate.

*Hint: position predicates are 1-indexed in XPath.*

<details>
<summary>Show solution</summary>

```java
WebElement third = driver.findElement(
    By.xpath("(//div[@class='inventory_item'])[3]"));
```

The parentheses matter. `(//div[@class='inventory_item'])[3]` means "collect all inventory items, then take the 
third." Without parentheses, `//div[@class='inventory_item'][3]` means "for each parent, take its third 
`inventory_item` child"; if there's only one parent, the result is the same, but the semantics differ and can 
produce different results in more complex DOMs.
</details>

---

## Section 3: Axes

### Exercise 3.1
Given the Sauce Labs Backpack product name, navigate up to the card container that holds it.

*Hint: the `ancestor` axis lets you climb the tree; something CSS cannot do.*

<details>
<summary>Show solution</summary>

```java
WebElement card = driver.findElement(
    By.xpath("//*[text()='Sauce Labs Backpack']/ancestor::div[@class='inventory_item']"));
```

This is the signature XPath pattern: find a known element by text, then navigate up to a container. Read it as 
"find any element whose text is 'Sauce Labs Backpack', then go up through ancestors until you hit a div with class 
'inventory_item'." CSS's `:has()` can do something similar (`.inventory_item:has(...)`) but can't match on text content.
</details>

### Exercise 3.2
Find the price of the Sauce Labs Backpack without knowing its position on the page.

*Hint: combine the ancestor pattern from 3.1 with a descendant step back down.*

<details>
<summary>Show solution</summary>

```java
WebElement price = driver.findElement(
    By.xpath("//*[text()='Sauce Labs Backpack']/ancestor::div[@class='inventory_item']//div[@class='inventory_item_price']"));
```

The three-step pattern: find by text → climb to container → descend to target. Read it as: "find 'Sauce Labs 
Backpack' text → go up to the inventory item card → go back down to the price div inside that card." This works 
regardless of how many products are on the page or what order they're in.
</details>

### Exercise 3.3
From the cart page (after adding an item and navigating there), find the item description that comes 
*before* the price element, using backward sibling navigation.

*Hint: `preceding-sibling` goes backward among siblings; something CSS's `+` and `~` cannot do.*

<details>
<summary>Show solution</summary>

```java
driver.findElement(By.xpath("//*[@data-test='add-to-cart-sauce-labs-backpack']")).click();
driver.findElement(By.xpath("//a[@class='shopping_cart_link']")).click();

WebElement description = driver.findElement(
    By.xpath("//div[@class='inventory_item_price']/preceding-sibling::*[1]"));
```

`preceding-sibling::*[1]` means "the first sibling element before me." Note the `[1]` here counts from the 
current node outward; `[1]` is the closest preceding sibling, not the first child of the parent. CSS has no way 
to navigate backward among siblings.
</details>

### Exercise 3.4
Navigate from the username input up to its parent and then find the password input that is a sibling of the username.

*Hint: use `..` to go up one level, then `/` back down. Compare to using `following-sibling`.*

<details>
<summary>Show solution</summary>

```java
// Approach 1: parent then back down
WebElement password = driver.findElement(
    By.xpath("//input[@id='user-name']/../input[@type='password']"));

// Approach 2: following-sibling
WebElement password2 = driver.findElement(
    By.xpath("//input[@id='user-name']/following-sibling::input[@type='password']"));
```

Both work. `..` goes up to the parent and then the `/` comes back down to all children, so Approach 1 would 
also match a password input that *precedes* the username in the DOM. `following-sibling` is more precise; it only 
looks forward. In saucedemo the username comes before the password, so both give the same result, but 
`following-sibling` better expresses the intent.
</details>
