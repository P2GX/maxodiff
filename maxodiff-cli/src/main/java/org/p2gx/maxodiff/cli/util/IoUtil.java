package org.p2gx.maxodiff.cli.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class IoUtil {
    /**
     * Resolves a user-provided file path against a base directory and ensures it has the required suffix.
     * 
     * @param userInput The string provided by the user (can be relative, absolute, or empty).
     * @param baseDir   The directory to resolve against if the user input is relative.
     * @param suffix    The required file extension (e.g., ".html").
     * @return A fully resolved Path object.
     */
    public static Path resolveOutputFile(String userInput, Path baseDir, String suffix) {
        // 1. Handle empty input case (if you have a default filename)
        if (userInput == null || userInput.isBlank()) {
            userInput = "default_output";
        }

        // 2. Ensure the string doesn't already have the suffix
        String filename = userInput.trim();
        if (!filename.toLowerCase().endsWith(suffix.toLowerCase())) {
            filename += suffix;
        }

        // 3. Create a Path object from the input
        Path path = Paths.get(filename);

        // 4. If absolute, return as is. If relative, resolve against baseDir.
        return path.isAbsolute() ? path : baseDir.resolve(path);
    }


    public static Path defaultPath(String sampleId, int nDiseases, int nRepetitions, String suffix ) {
        String fileName = String.format("%s_%d_%d_maxodiff_results.%s", sampleId, nDiseases, nRepetitions, suffix);
        return Path.of(fileName);
    }
}
