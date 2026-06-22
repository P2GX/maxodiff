package org.p2gx.maxodiff.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

/**
 * Java Virtual Machine Process Status Tool.
 */
public class JpsChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpsChecker.class);

    public static boolean isMainClassRunning(String mainClassName) {
        try {
            return ProcessHandle.allProcesses()
                .map(ProcessHandle::info)
                .map(ProcessHandle.Info::commandLine)
                .flatMap(Optional::stream)
                .anyMatch(cmdLine -> cmdLine.contains(mainClassName));
        } catch (SecurityException e) {
            LOGGER.error("Permission denied while accessing process list: {}", e.getMessage());
            return false;
        }
    }

}
