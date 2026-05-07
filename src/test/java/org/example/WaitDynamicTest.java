package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WaitDynamicTest {
    private WebDriver webDriver;
    private WebDriverWait wait;

    @BeforeEach
    void beforeEach() {
        webDriver = new ChromeDriver();
        wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
    }

    @AfterEach
    void afterEach() {
        if (webDriver != null) {
            webDriver.quit();
        }
    }

    @Test
    void testHiddenElementRevealed() {
        webDriver.get("https://the-internet.herokuapp.com/dynamic_loading/1");
        WebElement startElement = webDriver.findElement(By.cssSelector("#start button"));
        startElement.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#finish h4")));
        WebElement helloElement = webDriver.findElement(By.cssSelector("#finish h4"));
        assertEquals(helloElement.getText(), "Hello World!");
    }

    @Test
    void testRenderedAfterLoading() {
        webDriver.get("https://the-internet.herokuapp.com/dynamic_loading/2");
        WebElement startElement = webDriver.findElement(By.cssSelector("#start button"));
        startElement.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#finish h4")));
        WebElement helloElement = webDriver.findElement(By.cssSelector("#finish h4"));
        assertEquals(helloElement.getText(), "Hello World!");
    }

}

