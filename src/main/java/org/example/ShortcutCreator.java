package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ShortcutCreator {
    public static void main() {
        String desktopPath = System.getProperty("user.home") + "\\Desktop";
        String excelFilePath = new File("DATA.xlsx").getAbsolutePath();
        String folderPath = new File("JobDocuments").getAbsolutePath();

        createShortcuts(desktopPath, excelFilePath, folderPath);
    }

    public static void createShortcuts(String desktopPath, String excelPath, String folderPath) {
        try {
            String vbsScript =
                    "Set oWS = WScript.CreateObject(\"WScript.Shell\")\n" +
                            "\n' Create folder shortcut\n" +
                            "sFolderShortcut = \"" + desktopPath + "\\Ausbildungsplatzangebote .lnk\"\n" +
                            "Set oFolderLink = oWS.CreateShortcut(sFolderShortcut)\n" +
                            "oFolderLink.TargetPath = \"" + folderPath + "\"\n" +
                            "oFolderLink.Save\n" +
                            "\n' Create Excel shortcut\n" +
                            "sFileShortcut = \"" + desktopPath + "\\DATA.xlsx .lnk\"\n" +
                            "Set oFileLink = oWS.CreateShortcut(sFileShortcut)\n" +
                            "oFileLink.TargetPath = \"" + excelPath + "\"\n" +
                            "oFileLink.Save";

            // Write the VBScript to a file
            File scriptFile = new File("create_shortcuts.vbs");
            FileWriter writer = new FileWriter(scriptFile);
            writer.write(vbsScript);
            writer.close();

            // Execute the VBScript
            Process process = Runtime.getRuntime().exec("wscript create_shortcuts.vbs");
            process.waitFor();

            // Delete the VBScript after execution
            scriptFile.delete();

            System.out.println("Shortcuts created successfully.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Failed to create shortcuts.");
        }
    }
}
