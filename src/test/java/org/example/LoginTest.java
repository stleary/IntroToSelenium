package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginTest {
    private WebDriver webDriver;

    @BeforeAll
    static void beforeAll() {
        //WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void beforeEach() {
        webDriver = new ChromeDriver();
    }

    @AfterEach
    void afterEach() {
        if (webDriver != null) {
            webDriver.quit();
        }
    }

    @Test
    void testLogin() {
        webDriver.get("https://www.saucedemo.com");
        WebElement name = webDriver.findElement(By.id("user-name"));
        WebElement password = webDriver.findElement(By.id("password"));
        name.sendKeys("standard_user");
        password.sendKeys("secret_sauce");
        WebElement submit = webDriver.findElement(By.id("login-button"));
        submit.click();
        WebElement title = webDriver.findElement(By.className("title"));
        assertEquals("Products", title.getText());
        System.out.println("testLogin() success");
    }

    @Test
    void testLockedOut() {
        webDriver.get("https://www.saucedemo.com");
        WebElement name = webDriver.findElement(By.id("user-name"));
        WebElement password = webDriver.findElement(By.id("password"));
        name.sendKeys("locked_out_user");
        password.sendKeys("secret_sauce");
        WebElement submit = webDriver.findElement(By.id("login-button"));
        submit.click();
        WebElement err = webDriver.findElement(By.cssSelector("[data-test='error']"));
        assertTrue(err.getText().contains("Sorry, this user has been locked out"));
        System.out.println("testLockedOut() success");
    }
}
