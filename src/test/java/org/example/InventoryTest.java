package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.openqa.selenium.support.ui.Select;

public class InventoryTest {
    private WebDriver webDriver;

    @BeforeAll
    static void beforeAll() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void beforeEach() {
        webDriver = new ChromeDriver();
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
        List<WebElement> list = webDriver.findElements(By.cssSelector(".inventory_item"));
        assertEquals(6, list.size());
        System.out.println("testProductCount() success");
    }

    @Test
    public void testProductNameElements() {
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
        WebElement sortWebElement = webDriver.findElement(By.cssSelector(".product_sort_container"));
        Select select = new Select(sortWebElement);
        // this will update the display in selected order
        select.selectByValue("lohi");

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
        WebElement sortWebElement = webDriver.findElement(By.cssSelector(".product_sort_container"));
        Select select = new Select(sortWebElement);
        // this will update the display in selected order
        select.selectByValue("za");

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
        // find the name, by matching text
        WebElement nameWebElement = webDriver.findElement(By.xpath("//div[text()='Sauce Labs Backpack']"));

        // get the 4th parent, common to both name and price, by navigating the tree
        WebElement parent = nameWebElement.findElement(By.xpath("../../../.."));
        // get the inv item by ancestor search
        WebElement parent1 = nameWebElement.findElement(By.xpath("//ancestor::div[@class='inventory_item']"));

        // confirm you get the same result either way
        assertTrue(parent.equals(parent1));

        // dive into the price from the tree navigation element
        WebElement priceElement = parent.findElement(By.xpath("//descendant::div[@class='inventory_item_price']"));
        // dive into the price from the ancestor search element
        WebElement priceElement1 = parent1.findElement(By.xpath("//descendant::div[@class='inventory_item_price']"));

        // Confirm you get the same result either way
        assertTrue(priceElement1.equals(priceElement));

        assertEquals("$29.99", priceElement.getText());
        assertEquals("$29.99", priceElement1.getText());
        System.out.println("testFindNameAndPrice() success");
    }

}
