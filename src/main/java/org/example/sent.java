package org.example;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import static org.example.auscrap.driver;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class sent {

    static WebDriver driver; // Assume this is initialized elsewhere

    public static void main(String[] args) throws InterruptedException {
        Set<String> sentRecipients = new HashSet<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        // Get dynamic date range
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusMonths(1).withDayOfMonth(1); // 1st of previous month
        LocalDate endDate = today.plusDays(1); // Gmail 'before' is exclusive

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String after = startDate.format(formatter);
        String before = endDate.format(formatter);

        // Build Gmail search query
        String query = "in:sent after:" + after + " before:" + before;
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String baseSearch = "https://mail.google.com/mail/u/0/#search/" + encodedQuery;

        int page = 1;

        while (true) {
            String pageUrl = page == 1 ? baseSearch : baseSearch + "/p" + page;

            driver.get(pageUrl);
            Thread.sleep(3000);

            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.z0")));

            System.out.println("Checking page: " + pageUrl);

            try {
                WebElement aoDiv = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.AO")));
                List<WebElement> emailRows = aoDiv.findElements(By.cssSelector("tr.zA"));

                if (emailRows.isEmpty()) {
                    System.out.println("No more emails on this page. Stopping.");
                    break;
                }

                for (WebElement emailRow : emailRows) {
                    List<WebElement> recipientSpans = emailRow.findElements(By.cssSelector("span[email]"));
                    for (WebElement span : recipientSpans) {
                        String email = span.getAttribute("email").trim().toLowerCase();
                        if (!email.isEmpty()) {
                            sentRecipients.add(email);
                        }
                    }
                }

            } catch (TimeoutException e) {
                System.out.println("Timeout on page " + page);
                break;
            }

            page++;
        }

        // Output
        System.out.println("Recipients collected:");
        sentRecipients.forEach(System.out::println);
    }
}



//
//
//public class sent {
//
//
//
//    public static void main(String[] args) throws InterruptedException {
//
//
//
//    Set<String> sentRecipients = new HashSet<>();
//    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
//
//    int page = 1;
//
//
//
//    while(true){
//        String pageUrl = page == 1
//                ? "https://mail.google.com/mail/u/0/#sent"
//                : "https://mail.google.com/mail/u/0/#sent/p" + page;
//
//        driver.get(pageUrl);
//        Thread.sleep(3000); // Wait for the page to load
//
//
//        Thread.sleep(3000);
//
//        WebElement loged = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.z0")));
//
//        System.out.println("Checking page: " + pageUrl);
//
//        try {
//            WebElement aoDiv = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.AO")));
//            List<WebElement> emailRows = aoDiv.findElements(By.cssSelector("tr.zA"));
//
//            if (emailRows.isEmpty()) {
//                System.out.println("No more emails on this page. Stopping.");
//                break;
//            }
//
//            for (WebElement emailRow : emailRows) {
//                // Extract recipients inside this row
//                List<WebElement> recipientSpans = emailRow.findElements(By.cssSelector("span[email]"));
//
//                for (WebElement span : recipientSpans) {
//                    String email = span.getAttribute("email").trim().toLowerCase();
//                    if (!email.isEmpty()) {
//                        sentRecipients.add(email);
//                    }
//                }
//            }
//        } catch (TimeoutException e) {
//            System.out.println("Timeout waiting for AO div on page " + page);
//            break;
//        }
//
//        page++;
//    }
//        // Output recipients
//        System.out.println("Recipients collected:");
//        sentRecipients.forEach(System.out::println);
//}
//
//}
