package org.p2gx.maxodiff.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java Virtual Machine Process Status Tool.
 */
public class JpsChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpsChecker.class);

    public static boolean isMainClassRunning(String mainClassName) {
        try {
            return ProcessHandle.current().info().commandLine()
                    .map(cmdLine -> cmdLine.contains(mainClassName))
                    .orElse(false);
        } catch (SecurityException e) {
            LOGGER.error("Permission denied while accessing process list: {}", e.getMessage());
            return false;
        }
    }
}
