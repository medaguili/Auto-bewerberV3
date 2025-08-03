package org.example;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

import static org.example.auscrap.*;

public class RadarStl {
    private static final boolean ALLOW_EMPTY_EMAILS = false; // Toggle to allow companies with empty/"N/A" emails

    private final HttpClient httpClient;
    private String jsessionId;
    private static final String BASE_URL = "https://www.lehrstellen-radar.de";
    private static final String SEARCH_URL = BASE_URL + "/5100,90,lsrlist.html";
    private static final String SESSION_URL = BASE_URL + "/5100,90,lsrsearch.html";
    private static final int REQUEST_DELAY_MS = 1000; // Delay between page requests
    private static final int MAX_RETRIES = 3; // Retry attempts for failures
    private static final int SESSION_REFRESH_INTERVAL = 10; // Refresh session every 10 ZIPs
    private static final int MAX_CONCURRENT_ZIPS = 8; // Number of ZIP codes to process concurrently
    private final Semaphore requestSemaphore = new Semaphore(MAX_CONCURRENT_ZIPS); // Limit concurrent requests
    private final Object sessionLock = new Object(); // Lock for session refresh
    private final Object fileOperationLock = new Object(); // Lock for file and Excel operations

    /**
     * Inner class to hold company information.
     */
    public static class CompanyInfo {
        private String companyName;
        private String address;
        private String email;

        public CompanyInfo(String companyName, String address, String email) {
            this.companyName = companyName;
            this.address = address;
            this.email = email;
        }

        public String getCompanyName() {
            return companyName;
        }

        public String getAddress() {
            return address;
        }

        public String getEmail() {
            return email;
        }

        @Override
        public String toString() {
            return "CompanyInfo{" +
                    "companyName='" + companyName + '\'' +
                    ", address='" + address + '\'' +
                    ", email='" + email + '\'' +
                    '}';
        }
    }

    /**
     * Constructor initializes HttpClient with timeout and fetches JSESSIONID.
     */
    public RadarStl() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        // Validate ttemplatePath
        File templateFile = new File(templatePath);
        if (!templateFile.exists()) {
            System.err.println("Warning: Template file " + templatePath + " does not exist");
        }
        refreshSession();
    }

    /**
     * Fetches a new JSESSIONID via GET request to the search page.
     */
    private void refreshSession() {
        synchronized (sessionLock) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SESSION_URL))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                        .header("Accept-Language", "fr-FR,fr;q=0.5")
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                jsessionId = response.headers().firstValue("Set-Cookie")
                        .map(cookie -> cookie.split(";")[0].split("=")[1])
                        .orElse("");
                System.out.println("Fetched JSESSIONID: " + jsessionId);
            } catch (Exception e) {
                System.err.println("Failed to fetch JSESSIONID: " + e.getMessage());
                jsessionId = "";
            }
        }
    }

    /**
     * Builds and sends a POST request for a specific ZIP code, page, and search term.
     * @param zipCode 5-digit ZIP code (e.g., "70173").
     * @param page Page number (1-based).
     * @param searchTerm Job search term (e.g., "Network" or empty).
     * @return CompletableFuture with the HTML response.
     */
    private CompletableFuture<HttpResponse<String>> sendPostRequestAsync(String zipCode, int page, String searchTerm) {
        // Validate inputs
        if (zipCode == null || !zipCode.matches("\\d{5}")) {
            throw new IllegalArgumentException("Invalid ZIP code: " + zipCode);
        }
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be positive: " + page);
        }

        // Prepare form data
        Map<String, String> formData = new HashMap<>();
        formData.put("search-fromsearchform", "1");
        formData.put("search-ls", "1");
        formData.put("search-pr", "1");
        formData.put("search-plz", zipCode);
        formData.put("search-radius", "250");
        formData.put("search-switchtype", "");
        formData.put("search-searchterm", searchTerm != null ? searchTerm : "");
        formData.put("send", "Suchen");
        formData.put("page", String.valueOf(page));

        // Encode form data and log it
        String encodedFormData = formData.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        System.out.println("Form data for ZIP " + zipCode + ", page " + page + ": " + encodedFormData);

        // Build POST request to match Postman
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SEARCH_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Origin", BASE_URL)
                .header("Referer", SESSION_URL)
                .header("Cookie", "JSESSIONID=" + jsessionId + "; ROUTEID=.node23")
                .POST(HttpRequest.BodyPublishers.ofString(encodedFormData))
                .build();

        // Acquire semaphore permit
        try {
            requestSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }

        // Execute with retry logic
        CompletableFuture<HttpResponse<String>> future = new CompletableFuture<>();
        executeWithRetry(request, zipCode, page, 1, future);
        return future;
    }

    /**
     * Executes a request with retry logic for failures.
     * @param request The HTTP request.
     * @param zipCode The ZIP code being queried.
     * @param page The page number.
     * @param attempt Current attempt number.
     * @param future CompletableFuture to complete with the response.
     */
    private void executeWithRetry(HttpRequest request, String zipCode, int page, int attempt, CompletableFuture<HttpResponse<String>> future) {
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, throwable) -> {
                    requestSemaphore.release(); // Release semaphore
                    if (throwable != null) {
                        if (attempt < MAX_RETRIES) {
                            try {
                                Thread.sleep(1000 * attempt); // Exponential backoff
                                requestSemaphore.acquire();
                                executeWithRetry(request, zipCode, page, attempt + 1, future);
                            } catch (InterruptedException e) {
                                future.completeExceptionally(e);
                            }
                        } else {
                            System.err.println("Failed for ZIP " + zipCode + ", page " + page + " after " + attempt + " attempts: " + throwable.getMessage());
                            future.completeExceptionally(throwable);
                        }
                    } else if (response.statusCode() == 403 && attempt < MAX_RETRIES) {
                        System.out.println("403 Forbidden for ZIP " + zipCode + ", page " + page + ". Refreshing session...");
                        refreshSession();
                        try {
                            Thread.sleep(1000 * attempt);
                            requestSemaphore.acquire();
                            executeWithRetry(request, zipCode, page, attempt + 1, future);
                        } catch (InterruptedException e) {
                            future.completeExceptionally(e);
                        }
                    } else {
                        if (response.statusCode() != 200) {
                            System.out.println("Non-200 status for ZIP " + zipCode + ", page " + page + ": " + response.statusCode());
                        }
                        future.complete(response);
                    }
                });
    }

    /**
     * Fetches all pages of job listings for a single ZIP code and search term.
     * @param zipCode 5-digit ZIP code (e.g., "70173").
     * @param searchTerm Job search term (e.g., "Network" or empty).
     * @return CompletableFuture with a list of HTML responses.
     */
    public CompletableFuture<List<String>> fetchJobListingsAsync(String zipCode, String searchTerm) {
        List<String> htmlResponses = new ArrayList<>();

        // Fetch page 1 to determine total pages
        return sendPostRequestAsync(zipCode, 1, searchTerm).thenCompose(firstResponse -> {
            if (firstResponse == null || firstResponse.body() == null) {
                System.err.println("Null response for ZIP " + zipCode + ", page 1");
                return CompletableFuture.completedFuture(htmlResponses);
            }
            if (firstResponse.statusCode() != 200) {
                System.out.println("Skipping ZIP " + zipCode + ": Page 1 status " + firstResponse.statusCode());
                return CompletableFuture.completedFuture(htmlResponses);
            }

            String html = firstResponse.body();
            // Validate response contains job listings
            if (!html.contains("<div class=\"list-group\">")) {
                System.err.println("No job listings found in page 1 for ZIP " + zipCode);
                // Save page for debugging
                try {
                    String errorFilePath = "error_no_listings_" + zipCode + "_page1.html";
                    Files.writeString(Paths.get(errorFilePath), html);
                    System.err.println("Saved problematic HTML to " + errorFilePath);
                } catch (Exception e) {
                    System.err.println("Failed to save problematic HTML for ZIP " + zipCode + ": " + e.getMessage());
                }
                return CompletableFuture.completedFuture(htmlResponses);
            }
            htmlResponses.add(html);
            // Save HTML for debugging
            try {
                String debugFilePath = "debug_" + zipCode + "_page1.html";
                Files.writeString(Paths.get(debugFilePath), html);
                System.out.println("Saved HTML to " + debugFilePath);
            } catch (Exception e) {
                System.err.println("Failed to save HTML for ZIP " + zipCode + ": " + e.getMessage());
            }

            int totalPages = parseTotalPages(html);
            System.out.println("ZIP " + zipCode + ": " + totalPages + " pages found");

            if (totalPages <= 1) {
                return CompletableFuture.completedFuture(htmlResponses);
            }

            // Sequentially fetch remaining pages (2 to totalPages)
            CompletableFuture<List<String>> resultFuture = CompletableFuture.completedFuture(htmlResponses);
            for (int page = 2; page <= totalPages; page++) {
                final int currentPage = page;
                resultFuture = resultFuture.thenCompose(responses ->
                        sendPostRequestAsync(zipCode, currentPage, searchTerm).thenApply(response -> {
                            try {
                                System.out.println("Fetched ZIP " + zipCode + ", page " + currentPage + ": Status " + response.statusCode());
                                Thread.sleep(REQUEST_DELAY_MS); // Rate limit
                                if (response.statusCode() == 200 && response.body() != null) {
                                    String pageHtml = response.body();
                                    // Validate response
                                    if (!pageHtml.contains("<div class=\"list-group\">")) {
                                        System.err.println("No job listings found in page " + currentPage + " for ZIP " + zipCode);
                                        // Save page for debugging
                                        try {
                                            String errorFilePath = "error_no_listings_" + zipCode + "_page" + currentPage + ".html";
                                            Files.writeString(Paths.get(errorFilePath), pageHtml);
                                            System.err.println("Saved problematic HTML to " + errorFilePath);
                                        } catch (Exception e) {
                                            System.err.println("Failed to save problematic HTML for ZIP " + zipCode + ", page " + currentPage + ": " + e.getMessage());
                                        }
                                        return responses;
                                    }
                                    // Save HTML for debugging
                                    String debugFilePath = "debug_" + zipCode + "_page" + currentPage + ".html";
                                    Files.writeString(Paths.get(debugFilePath), pageHtml);
                                    System.out.println("Saved HTML to " + debugFilePath);
                                    synchronized (responses) {
                                        responses.add(pageHtml);
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("Error processing page " + currentPage + " for ZIP " + zipCode + ": " + e.getMessage());
                            }
                            return responses;
                        })
                );
            }

            return resultFuture;
        });
    }

    /**
     * Fetches job listings for all ZIP codes in GermanZipCode enum using multi-threading.
     * @param searchTerm Job search term (e.g., "Network" or empty).
     * @param uniqueEmails Thread-safe set to track unique emails.
     * @return CompletableFuture with a map of ZIP codes to their HTML responses.
     */
    public CompletableFuture<Map<String, List<String>>> fetchAllZipCodesAsync(String searchTerm, KeySetView<String, Boolean> uniqueEmails) {
        Map<String, List<String>> allResponses = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_ZIPS);
        AtomicInteger zipCount = new AtomicInteger(0);
        AtomicInteger totalCompanies = new AtomicInteger(0);
        AtomicInteger processedCompanies = new AtomicInteger(0);
        AtomicInteger skippedCompanies = new AtomicInteger(0);

        // Log ttemplatePath and currentDate
        System.out.println("ttemplatePath: " + templatePath);
        System.out.println("currentDate: " + (currentDate != null ? currentDate : "null"));

        // Collect all ZIP codes
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (GermanZipCode zipEnum : GermanZipCode.values()) {
            String zipCode = zipEnum.getZipCode();
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                int currentCount = zipCount.incrementAndGet();
                System.out.println("Starting ZIP " + zipCode + " (" + currentCount + "/" + GermanZipCode.values().length + ")");

                // Refresh session every SESSION_REFRESH_INTERVAL ZIPs
                if (currentCount % SESSION_REFRESH_INTERVAL == 0) {
                    System.out.println("Refreshing session at ZIP " + zipCode);
                    refreshSession();
                }

                fetchJobListingsAsync(zipCode, searchTerm)
                        .handle((htmls, throwable) -> {
                            if (throwable != null) {
                                System.err.println("Error for ZIP " + zipCode + ": " + throwable.getMessage());
                                return new ArrayList<String>();
                            }
                            return htmls;
                        })
                        .thenAccept(htmls -> {
                            allResponses.put(zipCode, htmls);
                            System.out.println("Completed ZIP " + zipCode + ": " + htmls.size() + " pages");

                            // Process company info for this ZIP code
                            for (int i = 0; i < htmls.size(); i++) {
                                String html = htmls.get(i);
                                System.out.println("  Page " + (i + 1) + " Length: " + html.length() + " chars");
                                List<CompanyInfo> companies = extractCompanyInfo(html);
                                totalCompanies.addAndGet(companies.size());
                                System.out.println("  Page " + (i + 1) + ": Found " + companies.size() + " companies (Total: " + totalCompanies.get() + ")");

                                for (CompanyInfo company : companies) {
                                    String email = company.getEmail() != null ? company.getEmail().toLowerCase() : "N/A";
                                    boolean hasValidEmail = !email.isEmpty() && !"N/A".equals(email);
                                    boolean isUnique = hasValidEmail && uniqueEmails.add(email);
                                    boolean shouldProcess = ALLOW_EMPTY_EMAILS || (hasValidEmail && isUnique);

                                    if (!hasValidEmail) {
                                        System.out.println("    Skipping company: No valid email (Name: " + company.getCompanyName() + ")");
                                        skippedCompanies.incrementAndGet();
                                        continue;
                                    }
                                    if (!isUnique && hasValidEmail) {
                                        System.out.println("    Skipping company: Duplicate email " + email);
                                        skippedCompanies.incrementAndGet();
                                        continue;
                                    }

                                    if (shouldProcess) {
                                        LocalDate effectiveDate;
                                        synchronized (fileOperationLock) {
                                            // Use fallback for currentDate
                                            effectiveDate = currentDate != null ? currentDate : LocalDate.now();
                                            String formattedDate;
                                            try {
                                                formattedDate = formatDate(effectiveDate);
                                            } catch (Exception e) {
                                                System.err.println("Error formatting date for " + email + ": " + e.getMessage());
                                                formattedDate = effectiveDate.toString();
                                            }

                                            // Create a folder for the company
                                            String folderName = "JobDocuments/" + effectiveDate + "/" + email;
                                            File folder = new File(folderName);
                                            if (!folder.exists()) {
                                                folder.mkdirs();
                                            }

                                            // Generate a new Word document for this job
                                            String docxPath = folderName + "/"+docu+".docx";
                                            String hr = "";
                                            try {
                                                createWordDocument(templatePath, docxPath, company.getCompanyName(), hr, company.getAddress(), formattedDate);
                                                convertDocxToPdf(folderName, folderName);
                                                String href = null;
                                                appendDataToExcel(company.getCompanyName(), hr, company.getAddress(), email, href, formattedDate);
                                                auscrap.f++;
                                                processedCompanies.incrementAndGet();
                                                System.out.println("    Processed company: " + email + " (Total processed: " + processedCompanies.get() + ")");
                                            } catch (Exception e) {
                                                System.err.println("Error processing documents for " + email + ": " + e.getMessage());
                                                e.printStackTrace();
                                            }
                                        }

                                        System.out.println("    Company Name: " + company.getCompanyName());
                                        System.out.println("    Address: " + company.getAddress());
                                        System.out.println("    Email: " + email);
                                        System.out.println("    Template used: " + templatePath);
                                        System.out.println("    Date used: " + effectiveDate);
                                        System.out.println("    ---");
                                    }
                                }
                            }

                            try {
                                Thread.sleep(REQUEST_DELAY_MS); // Delay between ZIPs
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        })
                        .join();
            }, executor);
            futures.add(future);
        }

        // Wait for all ZIP codes to complete and clean up HTML files
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    executor.shutdown();
                    System.out.println("Completed fetching for all " + zipCount.get() + " ZIP codes");
                    System.out.println("Total companies found: " + totalCompanies.get());
                    System.out.println("Total companies processed: " + processedCompanies.get());
                    System.out.println("Total companies skipped: " + skippedCompanies.get());
                    deleteAllHtmlFiles();
                    return allResponses;
                })
                .exceptionally(throwable -> {
                    System.err.println("Fatal error in fetchAllZipCodesAsync: " + throwable.getMessage());
                    throwable.printStackTrace();
                    return null;
                });
    }

    /**
     * Deletes all files with .html extension in the current working directory.
     */
    private void deleteAllHtmlFiles() {
        try {
            Files.list(Paths.get("."))
                    .filter(p -> p.getFileName().toString().endsWith(".html"))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                            System.out.println("Deleted HTML file: " + p.getFileName());
                        } catch (Exception e) {
                            System.err.println("Failed to delete HTML file " + p.getFileName() + ": " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error listing files for HTML deletion: " + e.getMessage());
        }
    }

    /**
     * Parses the total number of pages from the HTML response using regex and Jsoup fallback.
     * @param html The HTML response body.
     * @return Total number of pages, or 1 if not found.
     */
    private int parseTotalPages(String html) {
        // Normalize HTML to handle non-breaking spaces
        String normalizedHtml = html.replaceAll(" ", " ").replaceAll("\\s+", " ");

        // Step 1: Try regex for "Seite X von Y"
        Pattern pattern = Pattern.compile("Seite\\s*\\d+\\s*von\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(normalizedHtml);
        if (matcher.find()) {
            try {
                String totalPagesStr = matcher.group(1);
                System.out.println("Regex matched: " + matcher.group(0) + ", Total pages: " + totalPagesStr);
                return Integer.parseInt(totalPagesStr);
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse total pages from regex match: " + matcher.group(0));
            }
        } else {
            System.err.println("Regex 'Seite X von Y' not found. Searching HTML snippet...");
            int index = normalizedHtml.indexOf("Seite");
            if (index != -1) {
                String snippet = normalizedHtml.substring(Math.max(0, index - 50), Math.min(normalizedHtml.length(), index + 50));
                System.err.println("HTML snippet near 'Seite': " + snippet);
            } else {
                System.err.println("No 'Seite' found in HTML");
            }
        }

        // Step 2: Fallback to Jsoup
        try {
            Document doc = Jsoup.parse(html);
            String paginationText = doc.select("div:contains(Seite)").text();
            System.out.println("Jsoup pagination text: " + paginationText);
            Pattern jsoupPattern = Pattern.compile("Seite\\s*\\d+\\s*von\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher jsoupMatcher = jsoupPattern.matcher(paginationText);
            if (jsoupMatcher.find()) {
                String totalPagesStr = jsoupMatcher.group(1);
                System.out.println("Jsoup matched: " + jsoupMatcher.group(0) + ", Total pages: " + totalPagesStr);
                return Integer.parseInt(totalPagesStr);
            } else {
                System.err.println("Jsoup failed to match 'Seite X von Y' in: " + paginationText);
            }
        } catch (Exception e) {
            System.err.println("Jsoup parsing failed: " + e.getMessage());
        }

        // Step 3: Default to 1 if all parsing fails
        System.err.println("No pagination marker found, defaulting to 1 page");
        return 1;
    }

    /**
     * Extracts company name, address, and email from HTML content containing lsrdetail elements.
     * @param htmlContent The HTML string to parse.
     * @return List of CompanyInfo objects containing extracted data.
     */
    public List<CompanyInfo> extractCompanyInfo(String htmlContent) {
        List<CompanyInfo> results = new ArrayList<>();
        Document doc = Jsoup.parse(htmlContent);

        // Find all lsrdetail divs
        Elements detailDivs = doc.select("div.lsrdetail");
        System.out.println("Found " + detailDivs.size() + " lsrdetail divs");

        for (Element detail : detailDivs) {
            String companyName = null;
            String address = null;
            String email = null;

            // Search all rows for company info
            Elements rows = detail.select("div.row");
            for (Element row : rows) {
                Elements spans = row.select("span");
                Elements ps = row.select("p");

                // Try to identify company name and address
                if (ps.size() > 0 && spans.size() >= 1) {
                    String pText = ps.first().text().toLowerCase();
                    if (pText.contains("firma") || pText.contains("unternehmen") || pText.contains("betrieb")) {
                        companyName = spans.get(0).text().trim();
                        StringBuilder addressBuilder = new StringBuilder();
                        for (int i = 1; i < spans.size(); i++) {
                            String part = spans.get(i).text().trim();
                            if (!part.isEmpty()) {
                                if (addressBuilder.length() > 0) {
                                    addressBuilder.append(", ");
                                }
                                addressBuilder.append(part);
                            }
                        }
                        address = addressBuilder.length() > 0 ? addressBuilder.toString() : "N/A";
                    }
                }

                // Extract email from any mailto link in the row
                Element emailLink = row.selectFirst("a[href^=mailto:]");
                if (emailLink != null) {
                    email = emailLink.text().trim();
                }
            }

            // Fallback: Check entire detail for email if not found in rows
            if (email == null) {
                Element emailLink = detail.selectFirst("a[href^=mailto:]");
                email = emailLink != null ? emailLink.text().trim() : "N/A";
            }

            // Add to results if any info was found
            if (companyName != null || address != null || !email.equals("N/A") || !email.equals("n/a")) {
                CompanyInfo company = new CompanyInfo(
                        companyName != null ? companyName : "N/A",
                        address != null ? address : "N/A",
                        email
                );
                results.add(company);
                System.out.println("Extracted company: " + company);
            } else {
                System.out.println("No valid company info extracted from lsrdetail div");
            }
        }

        if (detailDivs.isEmpty()) {
            // Save HTML for debugging
            try {
                String errorFilePath = "error_no_lsrdetail_" + System.currentTimeMillis() + ".html";
                Files.writeString(Paths.get(errorFilePath), htmlContent);
                System.err.println("No lsrdetail divs found; saved HTML to " + errorFilePath);
            } catch (Exception e) {
                System.err.println("Failed to save problematic HTML: " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Gets the current JSESSIONID.
     * @return The JSESSIONID or empty string if not set.
     */
    public String getJsessionId() {
        return jsessionId;
    }

    /**
     * Main method for testing with all ZIP codes and search term "Network".
     */
    public static void main() {
        RadarStl radar = new RadarStl();
        KeySetView<String, Boolean> uniqueEmails = ConcurrentHashMap.newKeySet();

        String searchTerm = auscrap.jobProfile;

        radar.fetchAllZipCodesAsync(searchTerm, uniqueEmails)
                .thenAccept(responses -> {
                    System.out.println("Completed processing for all ZIP codes");
                })
                .exceptionally(throwable -> {
                    System.err.println("Error in main: " + throwable.getMessage());
                    throwable.printStackTrace();
                    return null;
                })
                .join();
    }

    /**
     * Enum of 240 German ZIP codes covering all of Germany with 250 km radius.
     */
    enum GermanZipCode {
        ZIP_01099("01099"),
        ZIP_04109("04109"),
        ZIP_10115("10115"),
        ZIP_14473("14473"),
        ZIP_15517("15517"),
        ZIP_17033("17033"),
        ZIP_18055("18055"),
        ZIP_20253("20253"),
        ZIP_23552("23552"),
        ZIP_24103("24103"),
        ZIP_26122("26122"),
        ZIP_28195("28195"),
        ZIP_30159("30159"),
        ZIP_32105("32105"),
        ZIP_34117("34117"),
        ZIP_39104("39104"),
        ZIP_40210("40210"),
        ZIP_42103("42103"),
        ZIP_44135("44135"),
        ZIP_45127("45127"),
        ZIP_46145("46145"),
        ZIP_48143("48143"),
        ZIP_50667("50667"),
        ZIP_52062("52062"),
        ZIP_53111("53111"),
        ZIP_55116("55116"),
        ZIP_56068("56068"),
        ZIP_57072("57072"),
        ZIP_58095("58095"),
        ZIP_59065("59065"),
        ZIP_60311("60311"),
        ZIP_63065("63065"),
        ZIP_65183("65183"),
        ZIP_66111("66111"),
        ZIP_70173("70173"),
        ZIP_71634("71634"),
        ZIP_72762("72762"),
        ZIP_74072("74072"),
        ZIP_76131("76131"),
        ZIP_80331("80331"),
        ZIP_82110("82110"),
        ZIP_83022("83022"),
        ZIP_85049("85049"),
        ZIP_86150("86150"),
        ZIP_87435("87435"),
        ZIP_88212("88212"),
        ZIP_89073("89073"),
        ZIP_90402("90402"),
        ZIP_91052("91052"),
        ZIP_92637("92637"),
        ZIP_93047("93047"),
        ZIP_94032("94032"),
        ZIP_95028("95028"),
        ZIP_96047("96047"),
        ZIP_97070("97070"),
        ZIP_99084("99084"),
        ZIP_04157("04157"),
        ZIP_10178("10178"),
        ZIP_14467("14467"),
        ZIP_15344("15344"),
        ZIP_17192("17192"),
        ZIP_18106("18106"),
        ZIP_20255("20255"),
        ZIP_23566("23566"),
        ZIP_24114("24114"),
        ZIP_26133("26133"),
        ZIP_28203("28203"),
        ZIP_30169("30169"),
        ZIP_32108("32108"),
        ZIP_34123("34123"),
        ZIP_39108("39108"),
        ZIP_40212("40212"),
        ZIP_42107("42107"),
        ZIP_44137("44137"),
        ZIP_45128("45128"),
        ZIP_46147("46147"),
        ZIP_48145("48145"),
        ZIP_50668("50668"),
        ZIP_52064("52064"),
        ZIP_53113("53113"),
        ZIP_55118("55118"),
        ZIP_56070("56070"),
        ZIP_57074("57074"),
        ZIP_58097("58097"),
        ZIP_59067("59067"),
        ZIP_60313("60313"),
        ZIP_63067("63067"),
        ZIP_65185("65185"),
        ZIP_66113("66113"),
        ZIP_70174("70174"),
        ZIP_71636("71636"),
        ZIP_72764("72764"),
        ZIP_74074("74074"),
        ZIP_76133("76133"),
        ZIP_80333("80333"),
        ZIP_82152("82152"),
        ZIP_83024("83024"),
        ZIP_85051("85051"),
        ZIP_86152("86152"),
        ZIP_87437("87437"),
        ZIP_88214("88214"),
        ZIP_89077("89077"),
        ZIP_90403("90403"),
        ZIP_91054("91054"),
        ZIP_92655("92655"),
        ZIP_93049("93049"),
        ZIP_94034("94034"),
        ZIP_95030("95030"),
        ZIP_96049("96049"),
        ZIP_97072("97072"),
        ZIP_99086("99086"),
        ZIP_78048("78048"), // Villingen-Schwenningen, Baden-Württemberg
        ZIP_79098("79098"), // Freiburg, Baden-Württemberg
        ZIP_73430("73430"), // Aalen, Baden-Württemberg
        ZIP_70190("70190"), // Stuttgart, Baden-Württemberg
        ZIP_74076("74076"), // Heilbronn, Baden-Württemberg
        ZIP_85055("85055"), // Ingolstadt, Bavaria
        ZIP_86199("86199"), // Augsburg, Bavaria
        ZIP_95444("95444"), // Bayreuth, Bavaria
        ZIP_80336("80336"), // Munich, Bavaria
        ZIP_93059("93059"), // Regensburg, Bavaria
        ZIP_12043("12043"), // Berlin-Neukölln
        ZIP_10785("10785"), // Berlin-Mitte
        ZIP_12435("12435"), // Berlin-Treptow
        ZIP_10179("10179"), // Berlin-Mitte
        ZIP_13507("13507"), // Berlin-Reinickendorf
        ZIP_14770("14770"), // Brandenburg an der Havel, Brandenburg
        ZIP_15230("15230"), // Frankfurt (Oder), Brandenburg
        ZIP_14482("14482"), // Potsdam, Brandenburg
        ZIP_16515("16515"), // Oranienburg, Brandenburg
        ZIP_16761("16761"), // Hennigsdorf, Brandenburg
        ZIP_28215("28215"), // Bremen-Findorff
        ZIP_28199("28199"), // Bremen-Neustadt
        ZIP_28309("28309"), // Bremen-Hemelingen
        ZIP_28757("28757"), // Bremen-Vegesack
        ZIP_28237("28237"), // Bremen-Grohn
        ZIP_22767("22767"), // Hamburg-Altona
        ZIP_21073("21073"), // Hamburg-Harburg
        ZIP_22525("22525"), // Hamburg-Eimsbüttel
        ZIP_20095("20095"), // Hamburg-Altstadt
        ZIP_22111("22111"), // Hamburg-Billstedt
        ZIP_64283("64283"), // Darmstadt, Hesse
        ZIP_65189("65189"), // Wiesbaden, Hesse
        ZIP_34125("34125"), // Kassel, Hesse
        ZIP_63450("63450"), // Hanau, Hesse
        ZIP_36037("36037"), // Fulda, Hesse
        ZIP_18057("18057"), // Rostock, Mecklenburg-Vorpommern
        ZIP_17489("17489"), // Greifswald, Mecklenburg-Vorpommern
        ZIP_19053("19053"), // Schwerin, Mecklenburg-Vorpommern
        ZIP_18273("18273"), // Güstrow, Mecklenburg-Vorpommern
        ZIP_18528("18528"), // Bergen auf Rügen, Mecklenburg-Vorpommern
        ZIP_29221("29221"), // Celle, Lower Saxony
        ZIP_31785("31785"), // Hameln, Lower Saxony
        ZIP_37073("37073"), // Göttingen, Lower Saxony
        ZIP_38440("38440"), // Wolfsburg, Lower Saxony
        ZIP_49074("49074"), // Osnabrück, Lower Saxony
        ZIP_41061("41061"), // Mönchengladbach, North Rhine-Westphalia
        ZIP_58452("58452"), // Witten, North Rhine-Westphalia
        ZIP_50674("50674"), // Cologne, North Rhine-Westphalia
        ZIP_47051("47051"), // Duisburg, North Rhine-Westphalia
        ZIP_42275("42275"), // Wuppertal, North Rhine-Westphalia
        ZIP_55122("55122"), // Mainz, Rhineland-Palatinate
        ZIP_67655("67655"), // Kaiserslautern, Rhineland-Palatinate
        ZIP_54290("54290"), // Trier, Rhineland-Palatinate
        ZIP_67059("67059"), // Ludwigshafen, Rhineland-Palatinate
        ZIP_56073("56073"), // Koblenz, Rhineland-Palatinate
        ZIP_66115("66115"), // Saarbrücken, Saarland
        ZIP_66538("66538"), // Neunkirchen, Saarland
        ZIP_66121("66121"), // Saarbrücken-Dudweiler, Saarland
        ZIP_66333("66333"), // Völklingen, Saarland
        ZIP_66606("66606"), // St. Wendel, Saarland
        ZIP_08056("08056"), // Zwickau, Saxony
        ZIP_09111("09111"), // Chemnitz, Saxony
        ZIP_04103("04103"), // Leipzig, Saxony
        ZIP_01307("01307"), // Dresden, Saxony
        ZIP_08371("08371"), // Glauchau, Saxony
        ZIP_06108("06108"), // Halle, Saxony-Anhalt
        ZIP_06844("06844"), // Dessau-Roßlau, Saxony-Anhalt
        ZIP_38855("38855"), // Wernigerode, Saxony-Anhalt
        ZIP_06449("06449"), // Aschersleben, Saxony-Anhalt
        ZIP_24116("24116"), // Kiel, Schleswig-Holstein
        ZIP_23730("23730"), // Neustadt in Holstein, Schleswig-Holstein
        ZIP_23554("23554"), // Lübeck, Schleswig-Holstein
        ZIP_25746("25746"), // Heide, Schleswig-Holstein
        ZIP_24937("24937"), // Flensburg, Schleswig-Holstein
        ZIP_07743("07743"), // Jena, Thuringia
        ZIP_07545("07545"), // Gera, Thuringia
        ZIP_98527("98527"), // Suhl, Thuringia
        ZIP_99974("99974"), // Mühlhausen, Thuringia
        ZIP_50676("50676"), // Cologne, North Rhine-Westphalia
        ZIP_80337("80337"), // Munich, Bavaria
        ZIP_04177("04177"), // Leipzig, Saxony
        ZIP_70176("70176"), // Stuttgart, Baden-Württemberg
        ZIP_10119("10119"), // Berlin-Prenzlauer Berg
        ZIP_80339("80339"), // Munich, Bavaria
        ZIP_50679("50679"), // Cologne, North Rhine-Westphalia
        ZIP_04129("04129"), // Leipzig, Saxony
        ZIP_70188("70188"), // Stuttgart, Baden-Württemberg
        ZIP_12047("12047"), // Berlin-Neukölln
        ZIP_80335("80335"), // Munich, Bavaria
        ZIP_50672("50672"), // Cologne, North Rhine-Westphalia
        ZIP_04155("04155"), // Leipzig, Saxony
        ZIP_70180("70180"), // Stuttgart, Baden-Württemberg
        ZIP_12059("12059"), // Berlin-Neukölln
        ZIP_80320("80320"), // Munich, Bavaria
        ZIP_50670("50670"), // Cologne, North Rhine-Westphalia
        ZIP_04179("04179"), // Leipzig, Saxony
        ZIP_70197("70197"), // Stuttgart, Baden-Württemberg
        ZIP_12099("12099"); // Berlin-Tempelhof

        private final String zipCode;

        GermanZipCode(String zipCode) {
            this.zipCode = zipCode;
        }

        public String getZipCode() {
            return zipCode;
        }
    }
}