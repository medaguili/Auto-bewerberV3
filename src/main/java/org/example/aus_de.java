package org.example;
import org.apache.poi.ss.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.*;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.example.auscrap.*;

public class aus_de {

    // Construct the dynamic URL

    static String URLrest = "&form_main_search[where]=&t_search_type=root&t_what=&t_where=&form_main_search[radius]=1000";
    static String baseUrl = "https://www.ausbildung.de/suche/?form_main_search[what]=";
    static String prfl = jobProfile.replaceAll("//s", "+");
    static String searchUrl = baseUrl + prfl + URLrest;

    public static void main() {
        Set<String> uniqueEmails = new HashSet<>();


        try {
            auscrap.driver.manage().window().setSize(new Dimension(412, 915)); // Set to Pixel 7 screen size
            auscrap.driver.get(searchUrl);
            System.out.println("Driver  - Page title: " + auscrap.driver.getTitle());
            driver.manage().window().minimize();
            // Wait for the page to load (you can use WebDriverWait for better handling)
            Thread.sleep(8000);
            // Locate the "Allow all cookies" button using its ID
            WebElement allowButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("CybotCookiebotDialogBodyLevelButtonLevelOptinAllowAll")
            ));

            // Click the "Allow all cookies" button
            allowButton.click();

            System.out.println("Cookie banner accepted.");

            for (int l = 0; l < 6; l++) {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("window.scrollBy(0,document.body.scrollHeight)", "");
            }
            // Click "Weitere Ergebnisse" button until it disappears
            Thread.sleep(10000);
            while (true) {
                try {
                    WebElement loadMoreButton = wait.until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[contains(text(), 'Mehr Ergebnisse laden')]")
                    ));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", loadMoreButton);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loadMoreButton);
                    System.out.println("Clicked 'Weitere Ergebnisse' button. Loading more results...");
                    Thread.sleep(3000); // Wait for new results to load
                } catch (NoSuchElementException | TimeoutException e) {
                    System.out.println("No more results to load. 'Weitere Ergebnisse' button not found.");
                    break;
                }
            }

            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
            System.out.println("Scrolled to the top of the page.");
            Thread.sleep(3000);
            String pageSource = driver.getPageSource();
            Document document = Jsoup.parse(pageSource);
            Thread.sleep(3000);

            Elements ulElement = document.select(".search-result, .grid-item__main-search, .search-result__wrapper, .cards-grid, .infinite-scroll-component__outerdiv, .SearchResults_CardArea__LNZiM");
            Thread.sleep(2000);

            if (ulElement.isEmpty()) {
                System.out.println("The <ul> element with class 'search-result' was not found.");
                return;
            }
            // Step 8: Extract all <a> tags inside the <ul> element that match the pattern
            Elements links = ulElement.select("a[href^='/stellen/']");
// Method 1: Click using CSS Selector
            System.out.println("Found " + links.size() + " links.");

            // Determine the number of threads based on available processors
            int numThreads = 2;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            // Step 9: Print the extracted links
            for (Element link : links) {
                String href = "https://www.ausbildung.de" + link.attr("href");
                executor.submit(() -> {
                    EdgeOptions threadOptions = new EdgeOptions();
                    threadOptions.addArguments("--headless", "disable-cache", "disable-application-cache", "--disable-blink-features=AutomationControlled",
                            "--disable-infobars", "--no-sandbox", "--disable-webrtc", "profile.default_content_setting_values.images=2",
                            "--disable-features=VizDisplayCompositor", "--disable-3d-apis");


                    WebDriver threadDriver = new EdgeDriver(threadOptions);
                    try {
                        threadDriver.get(href); // Navigate to the job link
                        Thread.sleep(3000); // Wait for the page to load

                        String jobPageSource = threadDriver.getPageSource();
                        Document jobDocument = Jsoup.parse(jobPageSource);

                        Elements companies = jobDocument.getElementsByClass("jp-c-header__corporation-link");
                        String company = companies.text();

                        Elements addresses = jobDocument.getElementsByClass("jp-title__address");
                        String address = addresses.text().replace("📍", "").trim().replace(",", "\n");

                        Elements hrs = jobDocument.getElementsByClass("job-posting-contact-person__name");
                        String hr = hrs.text();

                        Elements emails = jobDocument.getElementsByClass("job-posting-contact-person__email");
                        String email = extractEmail(emails.text());

                        if(company == hr ){
                            hr=" ";
                        }

                        if (!emails.isEmpty() && !"N/A".equals(email) && !email.isEmpty() && uniqueEmails.add(email)) {

                            // Create a folder for the company
                            String folderName = "JobDocuments/" + java.time.LocalDate.now()+ "/" + email;
                            File folder = new File(folderName);
                            if (!folder.exists()) {
                                folder.mkdirs(); // Create the folder if it doesn't exist
                            }

                            // Generate a new Word document for this job
                            String docxPath = folderName + "/"+docu+".docx";
                            createWordDocument(templatePath, docxPath, company, hr, address, formatDate(currentDate));
                            System.out.println("Created Word document: " + docxPath);

                            // Convert the Word document to PDF
                            convertDocxToPdf(folderName, folderName);

                            appendDataToExcel(company, hr, address, email, href, formatDate(currentDate));


                            auscrap.f++;
                            System.out.println("Company: " + company);
                            System.out.println("HR: " + hr);
                            System.out.println("Address: " + address);
                            System.out.println("Email: " + email);
                            System.out.println("----------------------------------------");
                            ShortcutCreator.main();

                        } else {
                            appendVariableToNewSheet(href);
                            System.out.println("Email element not found on page: " + href);
                        }
                    } catch (Exception e) {
                        System.out.println("An error occurred while processing " + href + ": " + e.getMessage());
                    } finally {
                        threadDriver.quit(); // Close the browser
                    }
                });
            }

            // Shutdown the executor after all tasks are submitted
            executor.shutdown();

            // Wait for all threads to finish
            while (!executor.isTerminated()) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        }
    }

    













