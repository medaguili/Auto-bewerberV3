package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

import org.openqa.selenium.*;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Clicker {

    public static void main(String[] args) {

        String linktree = "links.txt";

        // Read the URL from the text file
        String url = readUrlFromFile(linktree);

        if (url == null || url.trim().isEmpty()) {
            System.out.println("No URL found in the file.");
            return;
        }


        // Create a Scanner object to read user input
        Scanner scanner = new Scanner(System.in);

        // Prompt the user to enter the number of browser instances
        System.out.print("Enter the number of browser instances to open: ");
        int numberOfInstances = scanner.nextInt();

        // Validate the input
        if (numberOfInstances <= 0) {
            System.out.println("Please enter a valid number greater than 0.");
            return;
        }
        // Loop to create and start the defined number of instances
        for (int i = 0; i < numberOfInstances; i++) {
            int instanceNumber = i + 1;

            String userAgent = User_Agents.USER_AGENTS[i % User_Agents.USER_AGENTS.length]; // Rotate user agents

            // Create a new thread for each instance
            new Thread(() -> {
                boolean signUpSuccessful = false;

                while (!signUpSuccessful) {
                    // Set up Edge options
                    EdgeOptions options = new EdgeOptions();
                    options.addArguments("disable-cache");
                    options.addArguments("disable-application-cache");
                    options.addArguments("--disable-blink-features=AutomationControlled");
                    options.addArguments("--disable-infobars");
                    options.addArguments("--no-sandbox");
                    options.addArguments("--user-agent=" + userAgent); // Set user agent
                    options.addArguments("--disable-webrtc"); // Disable WebRTC
                    options.addArguments("profile.default_content_setting_values.images=2"); // Block images
                    options.addArguments("--disable-features=VizDisplayCompositor"); // Mitigate canvas fingerprinting
                    options.addArguments("--disable-3d-apis"); // Mitigate canvas fingerprinting
                    options.addArguments("--load-extension=C:\\Users\\Med\\Desktop\\Clicker\\kamal_Clicker\\Auth_EXT");

                    // Emulate mobile device
                    Map<String, String> mobileEmulation = new HashMap<>();
                    mobileEmulation.put("deviceName", "Pixel 7");
                    options.setExperimentalOption("mobileEmulation", mobileEmulation);

                    WebDriver driver = new EdgeDriver(options);

                    try {
                        driver.manage().window().setSize(new Dimension(412, 915)); // Set to Pixel 7 screen size
                        driver.get(url);
                        System.out.println("Driver " + instanceNumber + " - Page title: " + driver.getTitle());

                        // Locate the container holding the links
                        WebElement linksContainer = driver.findElement(By.id("links-container"));

                        // Find all links within the container
                        List<WebElement> links = linksContainer.findElements(By.tagName("a"));

                        // Get the handle of the original tab
                        String originalTab = driver.getWindowHandle();

                        // Loop through each link
                        for (WebElement link : links) {
                            // Open the link in a new tab using Actions
                            Actions actions = new Actions(driver);
                            actions.keyDown(org.openqa.selenium.Keys.CONTROL).click(link).keyUp(org.openqa.selenium.Keys.CONTROL).build().perform();

                            // Switch to the new tab
                            Set<String> handles = driver.getWindowHandles();
                            for (String handle : handles) {
                                if (!handle.equals(originalTab)) {
                                    driver.switchTo().window(handle);
                                    break;
                                }
                            }

                            // Wait for the new page to load
                            new WebDriverWait(driver, Duration.ofSeconds(10))
                                    .until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete';"));



                            // Scroll down the page
                            JavascriptExecutor js = (JavascriptExecutor) driver;
                            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");

                            // Wait for a random amount of time (around 20 seconds)
                            int waitTime = new Random().nextInt(10) + 15; // Random time between 15 and 25 seconds
                            Thread.sleep(waitTime * 1000);

                            // Close the new tab
                            driver.close();

                            // Switch back to the original tab
                            driver.switchTo().window(originalTab);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        // Close the browser
                        driver.quit();
                    }
                }
            }).start();
        }
    }
    private static String readUrlFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // Read the first line (assuming the file contains only one URL)
            return reader.readLine();
        } catch (IOException e) {
            System.err.println("Error reading URL from file: " + e.getMessage());
            return null;
        }
    }
}