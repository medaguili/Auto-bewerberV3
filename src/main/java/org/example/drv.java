package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xwpf.usermodel.*;
//import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.docx4j.wml.Fonts;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import javax.swing.*;
import java.awt.*;
import java.awt.Color;
//import java.awt.Font;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import java.time.Duration;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class drv {

    public static WebDriver driver ;

    public static WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    public static EdgeOptions options = new EdgeOptions();


    JavascriptExecutor js = (JavascriptExecutor) driver;
    public static void main() throws InterruptedException {

    // Initialize WebDriverManager and set up Edge driver options
        WebDriverManager.edgedriver().setup();

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

}
}
