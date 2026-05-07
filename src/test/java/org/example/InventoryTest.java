package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class InventoryTest {
    private WebDriver webDriver;
    private WebDriverWait webDriverWait;

    @BeforeEach
    void beforeEach() {
        webDriver = new ChromeDriver();
        webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
        webDriver.get("https://www.saucedemo.com");
        WebElement name = webDriver.findElement(By.id("user-name"));
        WebElement password = webDriver.findElement(By.id("password"));
        name.sendKeys("standard_user");
        password.sendKeys("secret_sauce");
        WebElement submit = webDriver.findElement(By.id("login-button"));
        submit.click();
    }

    @AfterEach
    void afterEach() {
        if (webDriver != null) {
            webDriver.quit();
        }
    }

    @Test
    public void testProductCount() {
        webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".inventory_item")));
        List<WebElement> list = webDriver.findElements(By.cssSelector(".inventory_item"));
        assertEquals(6, list.size());
        System.out.println("testProductCount() success");
    }

    @Test
    public void testProductNameElements() {
        webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".inventory_item_name")));
        List<WebElement> webElements = webDriver.findElements(By.cssSelector(".inventory_item_name"));
        assertEquals(6, webElements.size());
        for (WebElement webElement : webElements) {
            String name = webElement.getText();
            assertFalse(name.isEmpty());
        }

        // alternative implementation with assertAll(), to test all entries, even if 1 or more fail
        assertAll("All products should have non-empty names",
                webElements.stream() .map(
                        element -> () -> assertFalse(
                                element.getText().isEmpty(), "Empty name found for element: " + element)
                )
        );
        System.out.println("testProductNameElements() success");
    }

    @Test
    public void testItemPrice() {
        webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".inventory_item_price")));
        List<WebElement> webElements = webDriver.findElements(By.cssSelector(".inventory_item_price"));
        assertEquals(6, webElements.size());
        for (WebElement webElement : webElements) {
            String price = webElement.getText();
            assertTrue(price.startsWith("$"));
        }
        System.out.println("testItemPrice() success");
    }

    @Test
    public void testSortLowToHigh() {
        webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".product_sort_container")));
        WebElement sortWebElement = webDriver.findElement(By.cssSelector(".product_sort_container"));
        Select select = new Select(sortWebElement);
        // this will update the display in selected order
        select.selectByValue("lohi");

        webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".inventory_item_price")));
        List<WebElement> webElements = webDriver.findElements(By.cssSelector(".inventory_item_price"));
        assertEquals(6, webElements.size());
        double maxAmount = 0.0;
        for (WebElement webElement : webElements) {
            String price = webElement.getText();
            double amount = Double.parseDouble(price.substring(1));
            assertTrue(amount >= maxAmount);
            maxAmount = amount;
        }
        System.out.println("testSortLowToHigh() success");
    }

    @Test
    public void testSortZToA() {
        webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".product_sort_container")));
        WebElement sortWebElement = webDriver.findElement(By.cssSelector(".product_sort_container"));
        Select select = new Select(sortWebElement);
        // this will update the display in selected order
        select.selectByValue("za");

        webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".inventory_item_name")));
        List<WebElement> webElements = webDriver.findElements(By.cssSelector(".inventory_item_name"));
        assertEquals(6, webElements.size());
        String lastName = "zzzzz";
        for (WebElement webElement : webElements) {
            String name = webElement.getText();
            assertTrue(name.compareTo(lastName) <= 0);
            lastName = name;
        }
        System.out.println("testSortZToA() success");
    }

    @Test
    public void testFindNameAndPrice() {
        webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".inventory_item_description")));
        List<WebElement> items = webDriver.findElements(By.cssSelector(".inventory_item_description"));
        for (WebElement item: items) {
            WebElement nameElement = item.findElement(By.cssSelector(".inventory_item_name"));
            String name = nameElement.getText();
            if ("Sauce Labs Backpack".equals(name)) {
                WebElement priceElement = item.findElement(By.cssSelector(".inventory_item_price"));
                String price = priceElement.getText();
                assertEquals("$29.99", price);
                break;
            }
        }
//        // find the name, by matching text
//        WebElement nameWebElement = webDriver.findElement(By.xpath("//div[text()='Sauce Labs Backpack']"));
//
//        // get the 4th parent, common to both name and price, by navigating the tree
//        WebElement parent = nameWebElement.findElement(By.xpath("../../../.."));
//        // get the inv item by ancestor search
//        WebElement parent1 = nameWebElement.findElement(By.xpath("//ancestor::div[@class='inventory_item_description']"));
//
//        // confirm you get the same result either way
//        assertTrue(parent.equals(parent1));
//        // dive into the price from the tree navigation element
//        WebElement priceElement = parent.findElement(By.xpath("//descendant::div[@class='inventory_item_price']"));
//        // dive into the price from the ancestor search element
//        WebElement priceElement1 = parent1.findElement(By.xpath("//descendant::div[@class='inventory_item_price']"));
//
//        // Confirm you get the same result either way
//        assertTrue(priceElement1.equals(priceElement));
//
//        assertEquals("$29.99", priceElement.getText());
//        assertEquals("$29.99", priceElement1.getText());
//        System.out.println("testFindNameAndPrice() success");
    }

}
