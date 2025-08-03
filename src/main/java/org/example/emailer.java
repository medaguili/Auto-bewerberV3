package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.example.UserInputForm;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class emailer {
    public static boolean s = false;
    public static Set<String> sentRecipients = new HashSet<>();

    private static void sent(WebDriver driver){
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

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        driver.get(baseSearch);


        while (true) {


            try {

                // ✅ Check for empty "Sent" folder by detecting <td class="TC">
                List<WebElement> emptyIndicators = driver.findElements(By.cssSelector("td.TC"));
                if (!emptyIndicators.isEmpty()) {
                    System.out.println("Empty Sent folder detected (td.TC found). Stopping pagination.");
                    break;
                }
                Thread.sleep(5000);

                WebElement aoDiv = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.AO")));
                List<WebElement> emailRows = aoDiv.findElements(By.cssSelector("tr.zA"));



                for (WebElement emailRow : emailRows) {
                    // Extract recipients inside this row
                    List<WebElement> recipientSpans = emailRow.findElements(By.cssSelector("span[email]"));

                    for (WebElement span : recipientSpans) {
                        String email = span.getAttribute("email").trim().toLowerCase();
                        if (!email.isEmpty()) {
                            sentRecipients.add(email);
                        }
                    }
                }
                // Find the "Next" button using class attribute only
                WebElement nextBtn = driver.findElement(By.cssSelector("div.T-I.J-J5-Ji.amD.T-I-awG.T-I-ax7.T-I-Js-Gs.L3"));

// Check if the button is disabled before clicking
                if ("true".equals(nextBtn.getAttribute("aria-disabled"))) {
                    System.out.println("No more pages — 'Next' is disabled.");
                    break;
                }

                nextBtn.click();
                Thread.sleep(3000); // Wait for the next page to load

            } catch (TimeoutException e) {
                System.out.println("Timeout waiting for AO div on page " );
                break;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

// Output recipients
        System.out.println("Recipients collected:");
        sentRecipients.forEach(System.out::println);
        s=true;

    }


    public static EdgeOptions options = new EdgeOptions();

    // Constants
    public static final int MAX_EMAILS_PER_DAY = 300;
    public static final File jobDocumentsFolder = new File("JobDocuments");

    private static final String STATE_FILE = "state.txt";
    private static final String EMAIL_LIMITS_FILE = "email_limits.txt";
    private static final long RESET_INTERVAL = 24 * 60 * 60; // 24 hours in seconds

    // Configuration
    public static String email;
    public static String pass;
    public static String sub;
    public static String msgbody;
    public static String ISP;
    public static String to;
    public static WebDriver driver;
    public static WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    public static String lastSentEmail = "";
    static String appData = System.getenv("APPDATA");



    // Email usage tracking
    public static Map<String, EmailUsage> emailLimits = new HashMap<>();

    // Inner class to store email usage and timestamp
    private static class EmailUsage {
        int count;
        long lastUsedTimestamp;

        EmailUsage(int count, long lastUsedTimestamp) {
            this.count = count;
            this.lastUsedTimestamp = lastUsedTimestamp;
        }
    }

    // Load email usage from file
    public static void loadEmailLimits() {
        emailLimits.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(appData +"/"+EMAIL_LIMITS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String[] values = parts[1].split(":");
                    if (values.length == 2) {
                        int count = Integer.parseInt(values[0]);
                        long timestamp = Long.parseLong(values[1]);
                        emailLimits.put(parts[0], new EmailUsage(count, timestamp));
                    }
                }
            }
        } catch (IOException e) {
            // File not found: start fresh
        }
    }

    // Save email usage to file
    public static void saveEmailLimits() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(EMAIL_LIMITS_FILE))) {
            for (Map.Entry<String, EmailUsage> entry : emailLimits.entrySet()) {

                writer.write(entry.getKey() + "=" + entry.getValue().count + ":" + entry.getValue().lastUsedTimestamp);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Check if email can send more today
    public static boolean canSend(String email) {
        // Ensure the email exists in the map
        if (!emailLimits.containsKey(email)) {
            // Initialize the email with default values
            emailLimits.put(email, new EmailUsage(0, Instant.now().getEpochSecond()));
            saveEmailLimits();
        }

        EmailUsage usage = emailLimits.get(email);
        long currentTime = Instant.now().getEpochSecond();

        // Reset count if 24 hours have passed
        if (currentTime - usage.lastUsedTimestamp > RESET_INTERVAL) {
            usage.count = 0;
            usage.lastUsedTimestamp = currentTime;
            saveEmailLimits();
        }

        return usage.count < MAX_EMAILS_PER_DAY;
    }

    // Update email usage
    public static void updateEmailCount(String email) {
        // Ensure the email exists in the map
        if (!emailLimits.containsKey(email)) {
            // Initialize the email with default values
            emailLimits.put(email, new EmailUsage(0, Instant.now().getEpochSecond()));
        }

        EmailUsage usage = emailLimits.get(email);
        usage.count++;
        usage.lastUsedTimestamp = Instant.now().getEpochSecond();
        saveEmailLimits();
    }
    public static boolean c=false;
    // Send Today Only Functionality
    public static void sendTodayOnly(File jobDocumentsFolder, String email) {
        loadEmailLimits();

        // Check if email is over the limit
        if (!canSend(email)) {
            System.out.println("Daily limit reached for: " + email);
            showNotification("Daily Limit Reached! ", "Emails Limits");
            driver.close();
            JobApplicationU.stopLoader();

            return;
        }

        // Get today's folder
        File todaysFolder = getTodaysFolder(jobDocumentsFolder);
        if (todaysFolder == null) {
            showNotification("No folder found for today's date.!", "Empty Folder");
//            driver.close();
//            return;
        }

        // Iterate through each company folder inside today's folder
        File[] companyFolders = todaysFolder.listFiles(File::isDirectory);
        if (companyFolders != null) {
            for (File companyFolder : companyFolders) {
                if (emailLimits.get(email).count >= MAX_EMAILS_PER_DAY) {
                    System.out.println("Daily email limit reached.");
                    showNotification("Daily Limit Reached! ", "Emails Limits");
                    driver.close();
                    JobApplicationU.stopLoader();

                    break;
                }
                gmailLOG(email, pass);
                ISP = "gmail";
//                if(s==false){
//                sent(driver);}
                // Skip if this company has already been processed
                if (companyFolder.getName().equals(lastSentEmail)) {
                    continue;
                }
// Extract company email (assuming folder name is the email)
                String companyEmail = companyFolder.getName();



                // ✅ Skip if this email is already in sentRecipients
                if (sentRecipients.contains(companyEmail)) {
                    System.out.println("EMAIL " + companyEmail + " already sent");
                    // Update state
//                    sentRecipients.add(companyEmail);
                    c = true;
                    continue;
                }

                // Find the PDF file in the company folder
                File resume = findPdfFile(companyFolder);
                if (resume == null) {
                    System.out.println("No PDF file found in folder: " + companyFolder.getName());
                    continue; // Skip this folder
                }


                // Send email with the dynamically located PDF
                sender(email , pass ,ISP, sub, companyEmail, msgbody, resume, UserInputForm.filePaths);

                // Update state
//                sentRecipients.add(companyEmail);
                lastSentEmail = companyEmail;
                updateEmailCount(email);
                c = true;
            }
        }

    }

    // Send Yesterday Only Functionality
    public static void sendYesterdayOnly(File jobDocumentsFolder, String email) {
        loadEmailLimits();
        // Get yesterday's folder
        File yesterdaysFolder = getYesterdaysFolder(jobDocumentsFolder);
        if (yesterdaysFolder == null) {
            System.out.println("No folder found for yesterday's date.");
            showNotification("No folder found for yesterday's date.", "Emails Limits");

            driver.close();
            JobApplicationU.stopLoader();

            return;
        }


        // Check if email is over the limit
        if (!canSend(email)) {
            System.out.println("Daily limit reached for: " + email);
            showNotification("Daily limit reached for: " + email, "Emails Limits");
            driver.close();
            JobApplicationU.stopLoader();

            return;
        }


        // Iterate through each company folder inside yesterday's folder
        File[] companyFolders = yesterdaysFolder.listFiles(File::isDirectory);
        if (companyFolders != null) {
            for (File companyFolder : companyFolders) {
                if (emailLimits.get(email).count >= MAX_EMAILS_PER_DAY) {
                    System.out.println("Daily email limit reached.");
                    showNotification("Daily limit reached for: " + email, "Emails Limits");
                    return;
                }

                gmailLOG(email, pass);
                ISP = "gmail";
                // Skip if this company has already been processed
                if (companyFolder.getName().equals(lastSentEmail)) {
                    continue;
                }

                // Find the PDF file in the company folder
                File resume = findPdfFile(companyFolder);
                if (resume == null) {
                    System.out.println("No PDF file found in folder: " + companyFolder.getName());
                    continue; // Skip this folder
                }

                // Extract company email (assuming folder name is the email)
                String companyEmail = companyFolder.getName();

                // Send email with the dynamically located PDF
                sender(email , pass ,ISP, sub, companyEmail, msgbody, resume, UserInputForm.filePaths);

                // Update state
                lastSentEmail = companyEmail;
                updateEmailCount(email);
                c = true;

            }
        }

    }

    public static void sendGlobally(File jobDocumentsFolder, String email) {
        loadEmailLimits();

        // Check if email is over the limit
        if (!canSend(email)) {
            System.out.println("Daily limit reached for: " + email);
            return;
        }


        gmailLOG(email, pass);
        ISP = "gmail";

        // Retrieve the last sent email
        String lastSentEmail = readLastSentEmail();
        boolean resumeSending = lastSentEmail.isEmpty(); // Flag to indicate when to resume sending

        // Get sorted folders (oldest to newest)
        List<File> dateFolders = getSortedJobFolders(jobDocumentsFolder);

        for (File dateFolder : dateFolders) {
            if (emailLimits.get(email).count >= MAX_EMAILS_PER_DAY) {
                System.out.println("Daily email limit reached.");
                break;
            }

            // Iterate through each company folder inside the date folder
            File[] companyFolders = dateFolder.listFiles(File::isDirectory);
            if (companyFolders != null) {
                for (File companyFolder : companyFolders) {
                    if (emailLimits.get(email).count >= MAX_EMAILS_PER_DAY) {
                        break;
                    }

                    // Skip emails until the last sent email is found
                    if (!resumeSending) {
                        if (companyFolder.getName().equals(lastSentEmail)) {
                            resumeSending = true; // Resume sending from the next email
                        }
                        continue;
                    }


                    // Find the PDF file in the company folder
                    File resume = findPdfFile(companyFolder);
                    if (resume == null) {
                        System.out.println("No PDF file found in folder: " + companyFolder.getName());
                        continue; // Skip this folder
                    }

                    // Extract company email (assuming folder name is the email)
                    String companyEmail = companyFolder.getName();

                    // Send email with the dynamically located PDF
                    sender(email , pass ,ISP, sub, companyEmail, msgbody, resume, UserInputForm.filePaths);

                    // Update state
                    writeLastSentEmail(companyEmail); // Update the last sent email
                    updateEmailCount(email);
                    c = true;
                }
            }
        }
    }

    private static String readLastSentEmail() {
        File file = new File(appData +"/"+"lastSentEmail.txt");
        if (!file.exists()) {
            return ""; // Start from the beginning if the file doesn't exist
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.readLine().trim();
        } catch (IOException e) {
            e.printStackTrace();
            return ""; // Default to empty string if there's an error
        }
    }

    private static void writeLastSentEmail(String email) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(appData +"/"+"lastSentEmail.txt"))) {
            writer.write(email);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to show a notification
    public static void showNotification(String message, String title) {
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true); // Ensure the notification is always on top
        JOptionPane.showConfirmDialog(frame, message, title, JOptionPane.DEFAULT_OPTION);
    }

    // Get sorted job folders (oldest to newest)
    public static List<File> getSortedJobFolders(File jobDocumentsFolder) {
        File[] dateFolders = jobDocumentsFolder.listFiles(File::isDirectory);
        List<File> sortedFolders = new ArrayList<>();

        if (dateFolders != null) {
            sortedFolders.addAll(Arrays.asList(dateFolders));
            sortedFolders.sort(Comparator.comparing(File::getName)); // Sort by date (oldest first)
        }

        return sortedFolders;
    }

    // Find the first PDF file in a folder
    public static File findPdfFile(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        if (files != null && files.length > 0) {
            return files[0]; // Return the first PDF file found
        }
        return null; // No PDF file found
    }


    public static void gmailLOG(String email, String password) {
        try {
            for(int i=0;i<UserInputForm.filePaths.size();i++){
                System.out.println(UserInputForm.filePaths.get(i));
            }             // Step 1: Open Google login via Stack Overflow
            driver.get("https://stackoverflow.com/users/login");
            Thread.sleep(3000); // Wait for the page to load

            // Step 2: Click "Log in with Google"
            WebElement googleLoginBtn = driver.findElement(By.xpath("//button[contains(@data-provider, 'google')]"));
            googleLoginBtn.click();
            Thread.sleep(3000);

            // Step 3: Enter email
            WebElement emailField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("identifierId")));
            emailField.sendKeys(email);
            driver.findElement(By.id("identifierNext")).click();
            Thread.sleep(3000);

            // Step 4: Check for email errors
            if (driver.getPageSource().contains("Couldn’t find your Google Account")) {
//                promptUserToResolve("Error: Incorrect email. Please enter a valid email manually and click Continue when done.");
                Thread.sleep(200000);

                checkLoginSuccess(driver);
                return;
            }

            // Step 5: Check for 2FA
            if (driver.getPageSource().contains("Verify it's you")) {
                JFrame frame = new JFrame();
                frame.setAlwaysOnTop(true);
                JOptionPane.showMessageDialog(
                        frame,
                        "Please complete the login process manually and click OK when done.",
                        "Login Completion",
                        JOptionPane.INFORMATION_MESSAGE);

//                promptUserToResolve("2-Step Verification detected! Please complete it manually and click Continue when done.");
                Thread.sleep(200000);

                checkLoginSuccess(driver);
                return;
            }

            try {
                // Step 6: Enter password
                WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='password'][jsname='YPqjbf']")));
                passwordField.sendKeys(password);
                driver.findElement(By.id("passwordNext")).click();
                Thread.sleep(3000);
            }catch (Exception e){
                checkLoginSuccess(driver);
                return;

            }
            // Step 7: Check for password errors
            if (driver.getPageSource().contains("Wrong password. Try again")) {
                JFrame frame = new JFrame();
                frame.setAlwaysOnTop(true);
                JOptionPane.showMessageDialog(
                        frame,
                        "Please complete the login process manually and click OK when done.",
                        "Login Completion",
                        JOptionPane.INFORMATION_MESSAGE);

                Thread.sleep(120000);
                checkLoginSuccess(driver);
                return;
            }

            // Step 8: Check for CAPTCHA or security challenges
            if (driver.getPageSource().contains("Verify it's you") || driver.getCurrentUrl().contains("challenge")) {
                JFrame frame = new JFrame();
                frame.setAlwaysOnTop(true);
                JOptionPane.showMessageDialog(
                        frame,
                        "Please complete the login process manually and click OK when done.",
                        "Login Completion",
                        JOptionPane.INFORMATION_MESSAGE);
                Thread.sleep(300000);

//                promptUserToResolve("Google security detected! Please complete the challenge manually and click Continue when done.");
                checkLoginSuccess(driver);
                return;
            }



            // Step 9: If no errors, check if login was successful
            checkLoginSuccess(driver);
            driver.get("https://mail.google.com/"); // Redirect to Gmail


        } catch (Exception e) {
            // Handle unexpected errors
            e.printStackTrace();
        }
    }

    private static void checkLoginSuccess(WebDriver driver) throws InterruptedException {
        // Create the frame (as in your original code)
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);

        // First check immediate success conditions (your original checks)
        if (driver.getCurrentUrl().contains("https://mail.google.com/") ||
                driver.getPageSource().contains("Something went wrong. Please contact us at community-support@stackoverflow.email for assistance.") ||
                driver.getCurrentUrl().contains("https://stackoverflow.com/users/oauth/google?code")) {
            System.out.println("Login successful");
            return;
        }

        // Enhanced checks for other success scenarios
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));

            // Check if we're on StackOverflow after successful login
            if (driver.getCurrentUrl().equals("https://stackoverflow.com/")) {
                // Look for multiple indicators of successful login
                boolean isLoggedIn = shortWait.until(d ->
                        d.findElements(By.id("hide-this-if-you-want")).size() > 0 ||  // Your original check
                                d.findElements(By.cssSelector(".my-profile.js-gps-track")).size() > 0 ||  // Profile icon
                                d.findElements(By.cssSelector("[data-testid='header-profile-icon']")).size() > 0);  // New profile indicator

                if (isLoggedIn)
                {
                    System.out.println("StackOverflow login confirmed, redirecting to Gmail");
                    driver.get("https://mail.google.com/");
                    return;
                }
            }

            // Check for Gmail elements in case we're already there but URL check missed it
            if (shortWait.until(d ->
                    d.findElements(By.cssSelector("[aria-label='Gmail']")).size() > 0 ||  // Gmail logo
                            d.findElements(By.cssSelector(".aic .z0 div")).size() > 0)) {  // Compose button
                System.out.println("Gmail elements detected, login successful");
                return;
            }

        } catch (Exception e) {
            // Ignore timeouts and continue to manual check
        }

        // Fall back to manual check (your original code)
        JOptionPane.showMessageDialog(
                frame,
                "Please complete the login process manually and click OK when done.",
                "Login Completion",
                JOptionPane.INFORMATION_MESSAGE);

        Thread.sleep(5000);  // Your original wait time

        // Final check after manual intervention
        try {
            if (!driver.getCurrentUrl().contains("mail.google.com")) {
                driver.get("https://mail.google.com/");
            }
            System.out.println("Proceeding after manual login");
        } catch (Exception e) {
            System.err.println("Error after manual login attempt: " + e.getMessage());
        }
    }





//
//    // Helper method to check if login was successful
//    private static void checkLoginSuccess(WebDriver driver) throws InterruptedException {
//        if (!driver.getCurrentUrl().equals("https://stackoverflow.com")) {
//            // Wait for the user to complete the login process
//            JFrame frame = new JFrame();
//            JOptionPane.showMessageDialog(frame,"Please complete the login process manually and click OK when done.", "Login Completion", JOptionPane.INFORMATION_MESSAGE);
//
//            frame.setAlwaysOnTop(true); // Ensure the notification is always on top
//            Thread.sleep(5000);
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10000));
//            try {
//
//                WebElement welcom = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("hide-this-if-you-want")));
//                Thread.sleep(3000);
//                driver.get("https://mail.google.com/");
//            } catch (Exception e) {
//                if (driver.getPageSource().contains("Something went wrong. Please contact us at community-support@stackoverflow.email for assistance.\n" +
//                        "\n") || driver.getPageSource().contains("https://stackoverflow.com/users/oauth/google?code=")){
//                    driver.get("https://mail.google.com/");
//                }
//
//            }finally{
//                System.out.println("Welcome message not detected within the timeout period.");
//                JOptionPane.showMessageDialog(null, "Nothing was done by The User , Program Closing.", "Login ERROR", JOptionPane.INFORMATION_MESSAGE);
//                driver.close();
//            }
//        }
//
//        // Check if the current URL matches the main Gmail URL
//        if (driver.getCurrentUrl().startsWith("https://mail.google.com/"))
//        {
//            System.out.println("Login successful!");
//        }
//    }
//

    public static void waitForAttachmentsToFinish(WebDriver driver, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

        wait.until(webDriver -> {
            List<WebElement> oldList = webDriver.findElements(By.cssSelector("div.a1.aaA.aMZ"));
            int stableCount = oldList.size();
            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 3000) { // 3 seconds stable window
                try {
                    Thread.sleep(500); // check every 0.5s
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }

                List<WebElement> newList = webDriver.findElements(By.cssSelector("div.a1.aaA.aMZ"));
                if (newList.size() != stableCount) {
                    stableCount = newList.size();
                    startTime = System.currentTimeMillis(); // reset the timer
                }
            }
            return true;
        });
    }



    // Send Email
    public static void sender(String email , String pass , String ISP, String sub, String to, String msg, File attachment , List<String> attachmentPaths) {
        try {

            if (ISP.equals("gmail")) {

                String encodedMsg = URLEncoder.encode(msg, StandardCharsets.UTF_8.toString());
                String gmailComposeLink = "https://mail.google.com/mail/?view=cm&to=" + to + "&su=" + sub + "&body=" + encodedMsg;
                driver.get(gmailComposeLink);
                Thread.sleep(3000);

                // Combine all valid attachments
                List<String> validAttachments = new ArrayList<>();
                if (attachment != null && attachment.exists()) {
                    validAttachments.add(attachment.getAbsolutePath());
                    System.out.println("Added Anschreiben to attachments: " + attachment.getAbsolutePath());
                } else if (attachment == null) {
                    System.err.println("Warning: Anschreiben file does not exist: " + attachment.getAbsolutePath());
                } else {
                    System.out.println("No Anschreiben provided.");
                }

                if (attachmentPaths != null && !attachmentPaths.isEmpty()) {
                    for (String path : attachmentPaths) {
                        if (new File(path).exists()) {
                            validAttachments.add(path);
                            System.out.println("Added other files to attachments: " + path);
                        } else {
                            System.err.println("Warning: File does not exist: " + path);
                        }
                    }
                } else {
                    System.out.println("No additional files provided (attachmentPaths is " + (attachmentPaths == null ? "null" : "empty") + ").");
                }
                if (!validAttachments.isEmpty()) {
                    WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type=file]")));

                    // Reset file input to avoid state issues
                    ((JavascriptExecutor) driver).executeScript("arguments[0].value = null;", fileInput);
                    System.out.println("File input reset.");

                    // Send all files in one go
                    String paths = validAttachments.stream().collect(Collectors.joining("\n"));
                    System.out.println("Sending paths to file input: " + paths.replace("\n", ", "));
                    fileInput.sendKeys(paths);

//                        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".a1.aaA.aMZ"))); // Gmail's attach preview element
                    System.out.println("Waiting for Gmail to finish attaching files...");
                    waitForAttachmentsToFinish(driver, 30); // Wait up to 20 seconds
                    System.out.println("Attachments uploaded.");


                } else {
                    System.out.println("No attachments to upload.");
                }

                Thread.sleep(100000);
                // Wait for attachments to appear (increased timeout to 30s)


                // Try multiple methods to click the Send button
                boolean clicked = false;
                List<String> clickMethods = Arrays.asList(
                        "CSS Selector",
                        "ID",
                        "XPath Aria-Label",
                        "XPath Text",
                        "JavaScript Click",
                        "Action Chains",
                        "Ctrl+Enter"
                );

                for (String method : clickMethods) {
                    System.out.println("Attempting to click Send button using: " + method);
                    wait = new WebDriverWait(driver, Duration.ofSeconds(8));

                    // CSS Selector
                    if (method.equals("CSS Selector") && !clicked) {
                        try {
                            WebElement sendButton = wait.until(ExpectedConditions.elementToBeClickable(
                                    By.cssSelector("div.T-I.aoO.T-I-atl.L3[role='button']")
                            ));
                            sendButton.click();
                            wait.until(ExpectedConditions.urlContains("sent"));
                            wait.until(ExpectedConditions.or(
                                    ExpectedConditions.visibilityOfElementLocated(By.id("link_undo")),
                                    ExpectedConditions.visibilityOfElementLocated(By.id("link_vsm"))
                            ));
                            clicked = true;
                            System.out.println("Send button clicked successfully using: " + method);
                        } catch (Exception e) {
                            System.err.println("Failed to click Send button with " + method + ": " + e.getMessage());
                        }
                    }

                    // ID
                    if (method.equals("ID") && !clicked) {
                        try {
                            WebElement sendButton = wait.until(ExpectedConditions.elementToBeClickable(
                                    By.id(":ot")
                            ));
                            sendButton.click();
                            wait.until(ExpectedConditions.urlContains("sent"));
                            wait.until(ExpectedConditions.or(
                                    ExpectedConditions.visibilityOfElementLocated(By.id("link_undo")),
                                    ExpectedConditions.visibilityOfElementLocated(By.id("link_vsm"))
                            ));
                            clicked = true;
                            System.out.println("Send button clicked successfully using: " + method);
                        } catch (Exception e) {
                            System.err.println("Failed to click Send button with " + method + ": " + e.getMessage());
                        }
                    }

                    // XPath Aria-Label
                    if (method.equals("XPath Aria-Label") && !clicked) {
                        try {
                            WebElement sendButton = wait.until(ExpectedConditions.elementToBeClickable(
                                    By.xpath("//div[@aria-label='Send ‪(Ctrl-Enter)‬']")
                            ));
                            sendButton.click();
                            wait.until(ExpectedConditions.urlContains("sent"));
                            wait.until(ExpectedConditions.or(
                                    ExpectedConditions.visibilityOfElementLocated(By.id("link_undo")),
                                    ExpectedConditions.visibilityOfElementLocated(By.id("link_vsm"))
                            ));
                            clicked = true;
                            System.out.println("Send button clicked successfully using: " + method);
                        } catch (Exception e) {
                            System.err.println("Failed to click Send button with " + method + ": " + e.getMessage());
                        }
                    }

                    // XPath Text
                    if (method.equals("XPath Text") && !clicked) {
                        try {
                            WebElement sendButton = wait.until(ExpectedConditions.elementToBeClickable(
                                    By.xpath("//div[text()='Send']")
                            ));
                            sendButton.click();
                            wait.until(ExpectedConditions.urlContains("sent"));
                            wait.until(ExpectedConditions.or(
                                    ExpectedConditions.visibilityOfElementLocated(By.id("link_undo")),
                                    ExpectedConditions.visibilityOfElementLocated(By.id("link_vsm"))
                            ));
                            clicked = true;
                            System.out.println("Send button clicked successfully using: " + method);
                        } catch (Exception e) {
                            System.err.println("Failed to click Send button with " + method + ": " + e.getMessage());
                        }
                    }

                    // JavaScript Click
                    if (method.equals("JavaScript Click") && !clicked) {
                        try {
                            WebElement sendButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                                    By.cssSelector("div.T-I.aoO.T-I-atl.L3[role='button']")
                            ));
                            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", sendButton);
                            wait.until(ExpectedConditions.urlContains("sent"));
                            wait.until(ExpectedConditions.or(
                                    ExpectedConditions.visibilityOfElementLocated(By.id("link_undo")),
                                    ExpectedConditions.visibilityOfElementLocated(By.id("link_vsm"))
                            ));
                            clicked = true;
                            System.out.println("Send button clicked successfully using: " + method);
                        } catch (Exception e) {
                            System.err.println("Failed to click Send button with " + method + ": " + e.getMessage());
                        }
                    }

                    // Action Chains
                    if (method.equals("Action Chains") && !clicked) {
                        try {
                            WebElement sendButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                                    By.cssSelector("div.T-I.aoO.T-I-atl.L3[role='button']")
                            ));
                            new Actions(driver).moveToElement(sendButton).click().perform();
                            wait.until(ExpectedConditions.urlContains("sent"));
                            wait.until(ExpectedConditions.or(
                                    ExpectedConditions.visibilityOfElementLocated(By.id("link_undo")),
                                    ExpectedConditions.visibilityOfElementLocated(By.id("link_vsm"))
                            ));
                            clicked = true;
                            System.out.println("Send button clicked successfully using: " + method);
                        } catch (Exception e) {
                            System.err.println("Failed to click Send button with " + method + ": " + e.getMessage());
                        }
                    }

                    // Ctrl+Enter
                    if (method.equals("Ctrl+Enter") && !clicked) {
                        try {
                            WebElement sendButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                                    By.cssSelector("div.T-I.aoO.T-I-atl.L3[role='button']")
                            ));
                            sendButton.sendKeys(Keys.chord(Keys.CONTROL, Keys.ENTER));
                            wait.until(ExpectedConditions.urlContains("sent"));
                            wait.until(ExpectedConditions.or(
                                    ExpectedConditions.visibilityOfElementLocated(By.id("link_undo")),
                                    ExpectedConditions.visibilityOfElementLocated(By.id("link_vsm"))
                            ));
                            clicked = true;
                            System.out.println("Send button clicked successfully using: " + method);
                        } catch (Exception e) {
                            System.err.println("Failed to click Send button with " + method + ": " + e.getMessage());
                        }
                    }

                    if (clicked) {
                        break;
                    }
                }

                if (!clicked) {
                    validAttachments.clear();
                    throw new RuntimeException("All methods to click Send button failed.");

                }


//                // Locate the file input element and upload the file
//                WebElement fileInput = driver.findElement(By.cssSelector("input[type=file]"));
//                fileInput.sendKeys(attachment.getAbsolutePath());
//                    Thread.sleep(5000); // Wait for the file to upload
//                    String paths = attachmentPaths.stream().collect(Collectors.joining("\n"));
//                    fileInput.sendKeys(paths);
//                Thread.sleep(15000); // Wait for the file to upload
//
//                // Send the email
//                WebElement sendButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.T-I.J-J5-Ji.aoO.T-I-atl.L3")));
//                sendButton.click();
//                Thread.sleep(5000);
            }

            System.out.println("Email sent successfully to: " + to);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Main Method
    public static void main(){

        // Initialize WebDriverManager and set up Edge driver options
        WebDriverManager.edgedriver().setup();

        // Set language preferences
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("intl.accept_languages", "en-US");
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--lang=en-US");

        // Set EdgeOptions to open in private mode
        options.addArguments("disable-cache");
        options.addArguments("disable-application-cache");
        options.addArguments("--disable-infobars");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-webrtc");
        options.addArguments("--disable-features=VizDisplayCompositor");
        options.addArguments("--disable-3d-apis");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--inprivate");
        options.addArguments("--accept-lang=en-US");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("useAutomationExtension", false);
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        // Initialize the driver after setting options
        driver = new EdgeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // Remove selenium detection and navigator.webdriver properties
        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

        Map<String, Object> params = new HashMap<>();
        params.put("source", "delete Object.getPrototypeOf(navigator).webdriver;");
        ((EdgeDriver) driver).executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", params);



        // Wait until the user has entered data
        while (!UserInputForm.S) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        // Send emails based on the selected option
        if (UserInputForm.selectedOption.equals("Send Today Only")) {

            sendTodayOnly(jobDocumentsFolder, email);
            // Show notification after all emails are processed
            if (c==true){
            showNotification("Emails Are Done!", "Email Process");

            driver.close();
            sentRecipients.clear();
            s=false;
                JobApplicationU.stopLoader();

            }
        } else if (UserInputForm.selectedOption.equals("Send Yesterday Only")) {

            sendYesterdayOnly(jobDocumentsFolder, email);
            // Show notification after all emails are processed
            if (c==true){
                showNotification("Emails Are Done!", "Email Process");
                driver.close();
                JobApplicationU.stopLoader();

            }
        } else if (UserInputForm.selectedOption.equals("Send Globally")) {

            sendGlobally(jobDocumentsFolder, email);
            // Show notification after all emails are processed
            if (c==true){
                showNotification("Emails Are Done!", "Email Process");
                driver.close();
                JobApplicationU.stopLoader();

            }
        }
    }

    // Get today's folder
    public static File getTodaysFolder(File jobDocumentsFolder) {
        String todaysDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        File[] dateFolders = jobDocumentsFolder.listFiles(File::isDirectory);
        if (dateFolders != null) {
            for (File dateFolder : dateFolders) {
                if (dateFolder.getName().equals(todaysDate)) {
                    return dateFolder;
                }
            }
        }
        return null; // No folder found for today's date
    }

    // Get yesterday's folder
    public static File getYesterdaysFolder(File jobDocumentsFolder) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DATE, -1); // Subtract 1 day to get yesterday's date
        String yesterdaysDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

        File[] dateFolders = jobDocumentsFolder.listFiles(File::isDirectory);
        if (dateFolders != null) {
            for (File dateFolder : dateFolders) {
                if (dateFolder.getName().equals(yesterdaysDate)) {
                    return dateFolder;
                }
            }
        }
        return null; // No folder found for yesterday's date
    }
}
