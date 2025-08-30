package org.monarchinitiative.maxodiff.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class JpsChecker {

    public static boolean isMainClassRunning(String mainClassName) {
        try {
            Process process = Runtime.getRuntime().exec("jps -l");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // Example line: "12345 com.example.MyApplication"
                if (line.contains(mainClassName)) {
                    return true; // Main class found
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false; // Main class not found
    }

}
