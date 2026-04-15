# Intro to Selenium
# Session 1: Setup and First Test

## 1. Overview

This session introduces Selenium WebDriver, a library that automates real web browsers. By the end of the session, you will have a working Java project that opens Chrome, logs into a practice website, and verifies the result using JUnit 5.

### 1.1 What is Selenium?

Selenium is an open-source library that controls a real web browser programmatically. When you run a Selenium test, an actual Chrome (or Firefox, Edge, etc.) window opens on your machine, and your code drives it: clicking buttons, typing into fields, reading text from the page; everything a human user would do, but automated.

Selenium is used for end-to-end testing of web applications. It verifies that the user interface works correctly from the user's perspective. It is not an HTTP client; it does not make REST API calls. It does not test backend logic directly. It tests what a real user sees and does in a browser.

Selenium supports multiple programming languages (Java, Python, C#, Ruby, JavaScript). In this course we use the Java bindings.

### 1.2 What is WebDriver?

WebDriver is the component of Selenium that communicates with a browser. The communication works through a browser-specific driver program:

Your Java code talks to the **Selenium library**, which talks to a **driver program** (e.g. ChromeDriver), which talks to the **browser** (e.g. Chrome).

Each browser has its own driver program. ChromeDriver works with Chrome, GeckoDriver works with Firefox, and so on. The driver must be installed on your machine and must be compatible with the version of the browser you have installed.

Managing these driver programs manually is tedious and error-prone, which is why we use a helper library to handle it automatically (see Section 2.6).

### 1.3 The Application Under Test

Throughout this course, we use **saucedemo.com** (`https://www.saucedemo.com`), a practice e-commerce application maintained by Sauce Labs. It has a login page, a product catalog, a shopping cart, and a checkout flow.

The site provides several test user accounts, each designed to produce different behaviors:

| Username | Behavior |
|---|---|
| `standard_user` | Normal, fully functional user |
| `locked_out_user` | Login is rejected with an error message |
| `problem_user` | Login succeeds, but the site has visual bugs |
| `performance_glitch_user` | Login succeeds, but pages load slowly |

All accounts use the same password: `secret_sauce`

These accounts are listed on the login page itself; this is a practice site, not a real application.

---

## 2. Repository and Project Setup

This section walks through creating a GitHub repository, cloning it locally, and initializing a Gradle project inside it.

### 2.1 Prerequisites

Before starting, make sure the following are installed on your machine:

- **Java 17 or later**: You can verify by running `java -version` in a terminal. If Java is not installed, download it from https://adoptium.net (Temurin is a good default choice).
- **Gradle**: You can verify by running `gradle --version`. If Gradle is not installed, follow the instructions at https://gradle.org/install/. On macOS you can use `brew install gradle`; on Windows, the Gradle website provides an installer.
- **Git**: You can verify by running `git --version`. If Git is not installed, download it from https://git-scm.com/downloads.
- **Chrome browser**: Selenium will automate this browser. Any recent version is fine.
- **A GitHub account**: If you don't have one, create a free account at https://github.com.
- **An IDE**: IntelliJ IDEA Community Edition (free) or VS Code with the Java Extension Pack. IntelliJ is recommended if you have no preference.

### 2.2 Create the GitHub Repository

1. Go to https://github.com and sign in.
2. Click the **+** button in the top-right corner and select **New repository**.
3. Name the repository `intro-to-selenium` (or another name of your choosing).
4. Add a short description, e.g. "Selenium WebDriver practice project."
5. Set the repository to **Public** (or Private if you prefer).
6. Check **Add a README file**: this creates an initial commit so the repo is ready to clone.
7. Under **Add .gitignore**, select the **Gradle** template. This prevents build output, IDE files, and other generated files from being committed. If the Gradle template is not available, you can add a `.gitignore` file manually later.
8. Click **Create repository**.

### 2.3 Clone the Repository Locally

Open a terminal and navigate to the directory where you keep your projects. Then clone the repository:

```
cd ~/projects
git clone https://github.com/YOUR-USERNAME/intro-to-selenium.git
cd intro-to-selenium
```

Replace `YOUR-USERNAME` with your actual GitHub username. If you named the repository something different, use that name instead.

You should now be inside the project directory, which contains the `README.md` and `.gitignore` files created by GitHub.

### 2.4 Initialize the Gradle Project

Run the Gradle init command from inside the project directory:

```
gradle init
```

Gradle will ask a series of interactive questions. Choose the following options:

1. **Project type:** `application` (Note: we won't use the main application class, but this generates the full project structure we need including the test directory)
2. **Implementation language:** `Java`
3. **Build script DSL:** `Groovy`
4. **Test framework:** `JUnit Jupiter`
5. **Project name:** accept the default (it will use the directory name)
6. **Source package:** `org.example`
7. For any other prompts (e.g. version of Java, generating multiple subprojects), accept the defaults.

After Gradle init completes, your project directory should contain:

- `build.gradle` (or `app/build.gradle` depending on your Gradle version); the build configuration
- `settings.gradle`: project settings
- `src/main/java/org/example/`: application source directory (we won't use this much)
- `src/test/java/org/example/`: test source directory (this is where our Selenium tests go)
- `gradlew` and `gradlew.bat`:  Gradle wrapper scripts

Verify the project works by running the generated sample test:

```
./gradlew test
```

On Windows, use `gradlew.bat test` instead. You should see `BUILD SUCCESSFUL`. If so, the project is set up correctly.

### 2.5 Clean Up Generated Files

Gradle init creates a sample application class (`App.java`) and a sample test class (`AppTest.java`). You can delete these; we will write our own test class from scratch:

- Delete `src/main/java/org/example/App.java`
- Delete `src/test/java/org/example/AppTest.java`

Keep the directory structure intact. You should still have the empty directories `src/main/java/org/example/` and `src/test/java/org/example/`.

### 2.6 Dependencies

The project requires three dependencies, all with `testImplementation` scope.

**Selenium Java**: `org.seleniumhq.selenium:selenium-java`

This is the core Selenium library. It provides the `WebDriver` interface, the `ChromeDriver` class, the `By` class for locating elements, and the `WebElement` interface for interacting with page elements. The current version is 4.41.0.

**WebDriverManager**: `io.github.bonigarcia:webdrivermanager`

This is the dependency that deserves the most explanation, because without understanding the problem it solves, students often don't understand why it exists.

As described in Section 1.2, Selenium needs a driver program to communicate with a browser. For Chrome, this driver is called ChromeDriver. The catch is that **ChromeDriver must match the version of Chrome installed on your machine**. If you have Chrome 134 and download ChromeDriver 132, things will break.

Before WebDriverManager existed, the standard setup process was:

1. Check which version of Chrome is installed on your machine
2. Go to the ChromeDriver download page
3. Find and download the ChromeDriver version that matches your Chrome version
4. Extract the binary to some location on your file system
5. Tell Selenium where to find it, usually with: `System.setProperty("webdriver.chrome.driver", "/path/to/chromedriver");`
6. When Chrome auto-updates (which it does frequently), repeat steps 1–5

This is fragile, manual, and causes a lot of broken test environments; especially in team settings where everyone has different Chrome versions.

**WebDriverManager automates this entire process.** When you call `WebDriverManager.chromedriver().setup()`, it:

1. Detects which version of Chrome is installed on the machine
2. Determines the compatible ChromeDriver version
3. Downloads the correct ChromeDriver binary (if not already cached)
4. Configures the system property so Selenium can find it

All of this happens at runtime, automatically. The first run downloads the driver; subsequent runs use the cached version. If Chrome updates, WebDriverManager detects the new version and downloads the matching driver on the next run.

WebDriverManager is an open-source project (Apache 2.0 license) created and maintained by Boni García. It is the #2 ranked library in the Web Browser Automation category on Maven Central.

Note: Selenium 4.6+ includes a built-in feature called Selenium Manager that does something similar. However, WebDriverManager is more established, more explicit (you can see exactly what it's doing), and provides additional features. We use it in this course because the explicit setup call makes the driver management step visible rather than hidden.

**JUnit Jupiter**: `org.junit.jupiter:junit-jupiter`

JUnit 5 is the test framework. It provides the `@Test` annotation, assertion methods like `assertEquals`, and the test lifecycle annotations (`@BeforeEach`, `@AfterEach`, etc.). Your Gradle build file must include `useJUnitPlatform()` in the `test` block so that Gradle knows to use JUnit 5.

### 2.7 Project Structure

After setup, your test code for Session 1 goes in a single file within the structure that Gradle init created:

```
intro-to-selenium/
  build.gradle
  settings.gradle
  gradlew
  gradlew.bat
  .gitignore
  README.md
  src/
    main/
      java/
        org/
          example/          (empty, no application code in this project)
    test/
      java/
        org/
          example/
            LoginTest.java  (you will create this)
```

The `LoginTest` class should be in the `org.example` package.

---

## 3. Concepts

This section describes the Selenium concepts you need for Session 1. Read through these before writing code.

### 3.1 The WebDriver Lifecycle

Every Selenium test follows three phases:

1. **Create**: Instantiate a WebDriver. This opens a browser window. For Chrome, you create a `ChromeDriver` object. Before creating it, WebDriverManager must have run its setup.
2. **Interact**: Navigate to URLs, find elements, type text, click buttons, read content.
3. **Quit**: Call `quit()` on the driver. This closes the browser window and releases all associated resources (processes, ports, temporary files).

If you skip the quit step, the browser window stays open and the ChromeDriver process keeps running. Over time this consumes system resources and can cause port conflicts in subsequent test runs.

### 3.2 Navigating to a URL

The `WebDriver` interface provides a `get(String url)` method that tells the browser to navigate to a URL, just as if a user typed it into the address bar.

### 3.3 Finding Elements on the Page

To interact with a page, you first need to locate the element you want to interact with. The `WebDriver` interface provides a `findElement` method that takes a `By` locator and returns a `WebElement`.

A `By` locator describes how to find an element in the page's HTML. There are several strategies (we will cover more in Session 2), but for Session 1 you need two:

**By.id**: Finds an element by its HTML `id` attribute. This is the most reliable locator strategy because IDs are meant to be unique on a page. Example: if the HTML contains `<input id="user-name" />`, then `By.id("user-name")` locates that input.

**By.className**: Finds an element by its CSS class name. Less precise than `By.id` because multiple elements can share the same class, but useful when an element has no ID.

To discover element IDs and class names, use Chrome DevTools: right-click on an element in the browser and select "Inspect" to see its HTML attributes.

### 3.4 Interacting with Elements

Once you have a `WebElement`, you can interact with it:

- **sendKeys(text)**: Types text into an input field
- **click()**: Clicks the element (buttons, links, etc.)
- **getText()**: Returns the visible text content of the element

### 3.5 Making Assertions

Use JUnit 5's `assertEquals(expected, actual)` to verify that the page is in the expected state. Import it as a static method from `org.junit.jupiter.api.Assertions`.

---

## 4. Requirements: Test 1: Successful Login

### 4.1 Functional Specification

Write a JUnit 5 test method that verifies a successful login to saucedemo.com.

**Preconditions:**
- WebDriverManager has been configured for ChromeDriver
- A ChromeDriver instance has been created

**Steps:**
1. Navigate to `https://www.saucedemo.com`
2. Locate the username input field (HTML id: `user-name`) and type `standard_user`
3. Locate the password input field (HTML id: `password`) and type `secret_sauce`
4. Locate the login button (HTML id: `login-button`) and click it
5. The browser should now be on the products page

**Verification:**
- Locate the page title element (CSS class: `title`)
- Assert that its text content equals `Products`

**Postcondition:**
- The browser is closed and the driver resources are released

### 4.2 Design Notes

- The test class should be named `LoginTest` in the `org.example` package
- For Session 1, perform all setup (WebDriverManager configuration and ChromeDriver creation) and teardown (driver quit) directly inside the test method. We will refactor this in Session 2.
- Store the `WebElement` returned by `findElement` in a local variable before calling methods on it. This makes the code readable and debuggable.

---

## 5. Requirements: Test 2: Failed Login (Exercise)

### 5.1 Functional Specification

Write a second test method in the same `LoginTest` class that verifies the error message displayed when a locked-out user attempts to log in.

**Preconditions:**
- WebDriverManager has been configured for ChromeDriver
- A ChromeDriver instance has been created

**Steps:**
1. Navigate to `https://www.saucedemo.com`
2. Locate the username input field and type `locked_out_user`
3. Locate the password input field and type `secret_sauce`
4. Locate the login button and click it
5. The browser should remain on the login page and display an error message

**Verification:**
- Locate the error message element and read its text
- Assert that the text contains the phrase `Sorry, this user has been locked out`

**Postcondition:**
- The browser is closed and the driver resources are released

### 5.2 Design Notes

- The error message element does not have a simple HTML id. You will need to inspect the saucedemo login page using Chrome DevTools to determine how to locate it. This is intentional; inspecting elements in the browser is a fundamental skill in Selenium testing.
- **Hint:** Look for the `data-test` attribute on the error message container. The `By.cssSelector` method can match attributes using the syntax `[attribute='value']`.
- Use `assertTrue` with the `String.contains()` method rather than `assertEquals` for the error message, since the full error text may include additional content like an icon character.

---

## 6. Push to GitHub

Once both tests pass, commit your work and push it to GitHub.

### 6.1 Review What Has Changed

Before committing, it's good practice to see what files have been added or modified. From the project root directory:

```
git status
```

You should see new or modified files including `build.gradle`, `settings.gradle`, the Gradle wrapper files, and your `LoginTest.java`. The `.gitignore` file from the Gradle template should be keeping build output (the `build/` directory) and IDE-specific files (`.idea/`, `.gradle/`, etc.) out of the list.

If you see `build/`, `.idea/`, or `.gradle/` in the list of untracked files, your `.gitignore` is not set up correctly. Add the following entries to your `.gitignore` before committing:

```
.gradle/
build/
.idea/
*.iml
```

### 6.2 Stage, Commit, and Push

```
git add .
git commit -m "Session 1: project setup and login tests"
git push origin main
```

If your default branch is named `master` instead of `main`, use `master` in the push command. You can check your branch name with `git branch`.

After pushing, visit your repository on GitHub and verify that your files are there. You should see the Gradle build files, the Gradle wrapper, and your `LoginTest.java` under `src/test/java/org/example/`.

---

## 7. Session 1 Checkpoint

After completing this session, you should have:

- A GitHub repository containing a Gradle project with Selenium, WebDriverManager, and JUnit 5 dependencies
- A `LoginTest` class in the `org.example` package with two test methods
- Both tests should pass when run from your IDE or via `./gradlew test`
- You should have seen Chrome physically open, perform the login actions, and close during each test
- Your code should be committed and pushed to GitHub

### 7.1 Observation: The driver.quit() Problem

Notice that in your current test code, the `driver.quit()` call comes at the end of the test method. Consider what happens if the assertion fails: execution jumps to the JUnit failure handler, the `driver.quit()` line is never reached, and the Chrome browser window stays open.

Run one of your tests with a deliberately wrong assertion (for example, assert that the page title is `"Wrong"` instead of `"Products"`). Observe that Chrome stays open after the test fails.

This is a real problem in practice; it leaves zombie browser processes consuming resources. Think about how you might fix this. We will address it properly in Session 2.

### 7.2 Preview: Session 2

In Session 2, we will:
- Refactor the duplicated setup and teardown code using JUnit 5 lifecycle annotations (`@BeforeEach` and `@AfterEach`)
- Fix the `driver.quit()` problem
- Learn the full set of element locator strategies: `By.id`, `By.name`, `By.className`, `By.tagName`, `By.cssSelector`, `By.xpath`
- Write tests that verify product listing and sorting behavior on the inventory page

---

## Appendix A: Reference Links

| Resource | URL |
|---|---|
| Selenium documentation | https://www.selenium.dev/documentation/ |
| Selenium Java API docs | https://www.selenium.dev/selenium/docs/api/java/ |
| WebDriverManager documentation | https://bonigarcia.dev/webdrivermanager/ |
| WebDriverManager GitHub | https://github.com/bonigarcia/webdrivermanager |
| Saucedemo practice site | https://www.saucedemo.com |
| JUnit 5 User Guide | https://junit.org/junit5/docs/current/user-guide/ |
| Chrome DevTools docs | https://developer.chrome.com/docs/devtools/ |
| Git documentation | https://git-scm.com/doc |
| GitHub Getting Started | https://docs.github.com/en/get-started |
| Gradle User Guide | https://docs.gradle.org/current/userguide/userguide.html |

## Appendix B: Troubleshooting

**Chrome doesn't open when I run the test**
- Verify Chrome is installed. Selenium cannot use a browser that isn't installed.
- Check that WebDriverManager setup runs before the ChromeDriver is created. The setup call must come first.

**Test fails with "session not created" or version mismatch error**
- This usually means the cached ChromeDriver is out of date. Delete the WebDriverManager cache directory (typically `~/.cache/selenium` or `~/.m2/repository/webdriver`) and run again. WebDriverManager will download a fresh driver.

**Test passes but Chrome stays open**
- Make sure `driver.quit()` is being called. If the test fails before reaching that line, Chrome will stay open. This is expected for Session 1; we fix it in Session 2.

**Gradle build fails to resolve dependencies**
- Make sure `mavenCentral()` is listed in the `repositories` block of your build file.
- Check that the dependency coordinates and version numbers are correct.
- Run `./gradlew dependencies` to see what Gradle resolved.

**`git push` is rejected**
- If you get an error like "Updates were rejected because the remote contains work that you do not have locally," run `git pull --rebase origin main` first, then try the push again.
- If you are prompted for credentials, you may need to set up a GitHub personal access token or SSH key. See https://docs.github.com/en/authentication for instructions.

**`gradle init` is not recognized or fails**
- Make sure Gradle is installed and on your system PATH. Run `gradle --version` to verify.
- On macOS, if you installed Gradle via Homebrew, try `brew reinstall gradle`.
- Alternatively, you can download the Gradle binary distribution from https://gradle.org/releases/ and add its `bin/` directory to your PATH.
