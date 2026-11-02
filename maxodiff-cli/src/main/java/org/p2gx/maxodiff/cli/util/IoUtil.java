package org.p2gx.maxodiff.cli.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class IoUtil {

    /**
     * Resolves the name given with {@code -f} against the output directory and makes sure that it ends
     * with the suffix.
     *
     * @param userInput the name from the user: relative, absolute, or empty
     * @param baseDir   the directory for a relative name
     * @param suffix    the file type without a dot, for example {@code "html"}
     * @return the full path of the output file
     */
    public static Path resolveOutputFile(String userInput, Path baseDir, String suffix) {
        String dotted = "." + suffix;
        String filename = (userInput == null || userInput.isBlank()) ? "default_output" : userInput.trim();
        if (!filename.toLowerCase(Locale.ROOT).endsWith(dotted.toLowerCase(Locale.ROOT))) {
            filename += dotted;
        }
        Path path = Paths.get(filename);
        return path.isAbsolute() ? path : baseDir.resolve(path);
    }

    /**
     * The file that the results go to if the user gave no name: inside the output directory and named after
     * the sample.
     *
     * @param suffix the file type without a dot, for example {@code "html"}
     */
    public static Path defaultPath(Path outputDir, String sampleId, int nDiseases, int nRepetitions, String suffix) {
        String fileName = String.format(Locale.ROOT, "%s_%d_%d_maxodiff_results.%s",
                sampleId, nDiseases, nRepetitions, suffix);
        return outputDir.resolve(fileName);
    }

    /** Creates the folder of the output file if it does not exist yet. */
    public static void createParentDirectories(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}