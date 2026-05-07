package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginTest {
    private WebDriver webDriver;
    private WebDriverWait webDriverWait;

    @BeforeEach
    void beforeEach() {
        webDriver = new ChromeDriver();
        webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
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
        webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#login-button")));
        WebElement name = webDriver.findElement(By.id("user-name"));
        WebElement password = webDriver.findElement(By.id("password"));
        name.sendKeys("standard_user");
        password.sendKeys("secret_sauce");
        WebElement submit = webDriver.findElement(By.id("login-button"));
        submit.click();
        webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".title")));
        WebElement title = webDriver.findElement(By.className("title"));
        assertEquals("Products", title.getText());
        System.out.println("testLogin() success");
    }

    @Test
    void testLockedOut() {
        webDriver.get("https://www.saucedemo.com");
        webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#user-name")));
        WebElement name = webDriver.findElement(By.id("user-name"));
        WebElement password = webDriver.findElement(By.id("password"));
        name.sendKeys("locked_out_user");
        password.sendKeys("secret_sauce");
        WebElement submit = webDriver.findElement(By.id("login-button"));
        submit.click();
        webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-test='error']")));
        WebElement err = webDriver.findElement(By.cssSelector("[data-test='error']"));
        assertTrue(err.getText().contains("Sorry, this user has been locked out"));
        System.out.println("testLockedOut() success");
    }
}
