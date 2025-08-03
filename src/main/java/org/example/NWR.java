package org.example;

import org.openqa.selenium.*;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.example.auscrap.*;

public class NWR {

    private static final String BASE_URL = "https://suche.ausbildung.nrw/?query=";
    private static String SEARCH_URL;

    private static final int THREAD_COUNT = 2;

    public static String extractCompanyName(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement h1 = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("h1.text-3xl.font-bold")
            ));
            return h1.getText().trim();
        } catch (TimeoutException e) {
            System.out.println("Company name not found on page.");
            return null;
        }
    }


    public static String extractAddress(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            WebElement addressElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("p.mt-2.text-lg.truncate.max-w-full")));

            // Use JavaScript to get innerHTML (preserves <br> content)
            String html = (String) ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].innerHTML;", addressElement);

            // Replace <br> with comma and clean the result
            String address = html.replaceAll("(?i)<br\\s*/?>", ", ").replaceAll("\\s+", " ").trim();

            System.out.println("Extracted address: " + address);
            return address;

        } catch (Exception e) {
            System.out.println("Address extraction failed: " + e.getMessage());
            return null;
        }
    }




    public static String extractHrName(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            WebElement nameElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("p.font-semibold.text-lg.truncate")));

            // Use JavaScript to extract inner text (in case of formatting)
            String name = (String) ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].innerText;", nameElement);

            name = name.trim();
            System.out.println("Extracted HR name: " + name);
            return name;

        } catch (Exception e) {
            System.out.println("HR name extraction failed: " + e.getMessage());
            return null;
        }
    }


    public static String extractHrEmail(WebDriver driver) {
        try {
            List<WebElement> emailLinks = driver.findElements(By.cssSelector("a[href^='mailto:']"));
            for (WebElement email : emailLinks) {
                String address = email.getAttribute("href");
                // only return the one containing a person's name (likely HR)
                if (address != null && address.contains("@") && !address.contains("ausbildung@")) {
                    return address.replace("mailto:", "").trim();
                }
            }
            return null;
        } catch (Exception e) {
            System.out.println("HR email not found.");
            return null;
        }
    }


    public static void main() throws InterruptedException {
        Set<String> uniqueEmails = new HashSet<>();




        try {
            SEARCH_URL = BASE_URL + auscrap.jobProfile;
            System.out.println(auscrap.jobProfile);
            Thread.sleep(2000);
            auscrap.driver.manage().window().setSize(new Dimension(412, 915)); // Set to Pixel 7 screen size
            auscrap.driver.get(SEARCH_URL);
            System.out.println("Driver  - Page title: " + auscrap.driver.getTitle());
            auscrap.driver.manage().window().minimize();
            // Wait for the page to load (you can use WebDriverWait for better handling)
            Thread.sleep(8000);

            // Step 2: Set radius to 100+ km
            try {

                String jsSetRadius =
                        "let slider = document.getElementById('input-radius');" +
                                "if (slider) {" +
                                "  slider.value = 100;" +  // set to max
                                "  slider.dispatchEvent(new Event('input'));" +  // simulate slider movement
                                "  slider.dispatchEvent(new Event('change'));" +
                                "} else {" +
                                "  console.log('Slider not found');" +
                                "}";


                ((JavascriptExecutor) auscrap.driver).executeScript(jsSetRadius);
                Object value = ((JavascriptExecutor) auscrap.driver).executeScript("return document.getElementById('input-radius').value;");
                System.out.println("Slider is set to: " + value + " km");

                Thread.sleep(4000); // wait for results to reload
            } catch (TimeoutException e) {
                System.out.println("Radius filter element not found or already set.");
            }

            // Step 3: Scroll down until no more new content loads
            JavascriptExecutor js = (JavascriptExecutor) auscrap.driver;
            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");
            while (true) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(4000); // wait for loading

                long newHeight = (long) js.executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) {
                    System.out.println("Reached bottom of page, no more content to load.");
                    break;
                }
                lastHeight = newHeight;
            }

            WebDriverWait wait = new WebDriverWait(auscrap.driver, Duration.ofSeconds(30));
            WebElement resultsContainer = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div[class*='md:col-span-6'] div.flex.flex-col.gap-4")
            ));

// Extract all job link elements
            List<WebElement> jobLinkElements = resultsContainer.findElements(
                    By.cssSelector("a[href^='unternehmen/']")
            );


            Set<String> jobLinks = new HashSet<>();
            for (WebElement linkEl : jobLinkElements) {
                String href = linkEl.getAttribute("href");
                if (href != null && !href.isEmpty()) {
                    // Make sure it's a full URL starting with https://
                    if (!href.startsWith("http")) {
                        href = BASE_URL + href;
                    }
                    jobLinks.add(href);
                }
            }

            System.out.println("Total job links found: " + jobLinks.size());
            for (String link : jobLinks) {
                System.out.println(link);
            }


            // Step 5: Visit each job offer link in a headless browser for inspection
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            for (String jobLink : jobLinks) {
                executor.submit(() -> {
                    EdgeOptions threadOptions = new EdgeOptions();
                    threadOptions.addArguments("--headless", "disable-cache", "disable-application-cache", "--disable-blink-features=AutomationControlled",
                            "--disable-infobars", "--no-sandbox", "--disable-webrtc", "profile.default_content_setting_values.images=2",
                            "--disable-features=VizDisplayCompositor", "--disable-3d-apis");

                    WebDriver threadDriver = new EdgeDriver(threadOptions);
                    try {
                        threadDriver.get(jobLink);
                        Thread.sleep(4000); // Adjust wait for page to load fully
                        System.out.println("Visited job page: " + jobLink);
                        // TODO: Add extraction code here after inspection

                        String companyName = extractCompanyName(threadDriver);
                        String address = extractAddress(threadDriver);
                        String hrName = extractHrName(threadDriver);
                        String hrEmail = extractHrEmail(threadDriver);

                        System.out.println("Company: " + companyName);
                        System.out.println("Address: " + address);
                        System.out.println("HR Name: " + hrName);
                        System.out.println("HR Email: " + hrEmail);

                        if(companyName == hrName ){
                            hrName=" ";
                        }

                        if (!hrEmail.isEmpty() && !"N/A".equals(hrEmail) && !hrEmail.isEmpty() && uniqueEmails.add(hrEmail)) {

                            // Create a folder for the company
                            String folderName = "JobDocuments/" + java.time.LocalDate.now()+ "/" + hrEmail;
                            File folder = new File(folderName);
                            if (!folder.exists()) {
                                folder.mkdirs(); // Create the folder if it doesn't exist
                            }


                        // Generate a new Word document for this job
                        String docxPath = folderName + "/"+docu+".docx";
                        createWordDocument(templatePath, docxPath, companyName, hrName, address, formatDate(currentDate));
                        System.out.println("Created Word document: " + docxPath);
                        Thread.sleep(3000);
                        // Convert the Word document to PDF
                        convertDocxToPdf(folderName, folderName);
                        Thread.sleep(2000);

                        appendDataToExcel(companyName, hrName, address, hrEmail, jobLink, formatDate(currentDate));
                        auscrap.f++;
                        ShortcutCreator.main();
                        } else {
                            appendVariableToNewSheet(jobLink);
                            System.out.println("Email element not found on page: " + jobLink);
                        }

                        } catch (Exception e) {
                        System.err.println("Error visiting job page " + jobLink + ": " + e.getMessage());
                    } finally {
                        threadDriver.quit();
                    }
                });
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
                Thread.sleep(2000);
            }

        } finally {

//            auscrap.driver.quit();
        }
    }
}
