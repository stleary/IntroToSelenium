# Session 1 Review Notes  What We Learned

1. # Keep it Simple

In Session 1 we were able to perform some basic tests using these Selenium classes and methods

* WebDriver  
  * findElement()  
  * quit()  
* WebElement  
  * sendKeys()  
  * click()  
  * getText()  
* By  
  * id()  
  * className()  
  * cssSelector()

**Note:** We also used WebDriverManager and and ChromeDriver, but only to set up our testing environment.

2. # Take Small Steps to Make Progress

There are many more classes and methods in Selenium, but there is an important lesson here on how to learn and use a new library:  
Don't try to learn everything at once. Trying to learn an entire library can be overwhelming and confusing. Many libraries included rarely used classes and methods that you will never need.  
Instead, just learn a small subset to accomplish your goal in the simplest way possible. In many cases, this is all you will ever need.  
   
We also learned that coding in Selenium follows this process:

* Bring up the page in your browser  
* Inspect the element you need to test in the debug view  
* Decide how to find this element using Selenium  
* Write code to retrieve and test the element  
* Execute the test to make sure it works as intended

   
Often you will need to test many different elements and pages as part of an end-to-end test. This will help you make steady, incremental progress to achieve your goals.

3. # Use the Testing Framework

We imported and used the JUnit testing library to execute our tests. This gave us our @Test annotation that runs each test method automatically. It also gave us the assertEquals() and assertTrue() methods to confirm a successful test. Later, it will give us more tools for testing.

Rule number 1 applies here, too. Just use a small subset of the testing lib to accomplish your immediate goal in a simple way. For most tests, this all you will need. 