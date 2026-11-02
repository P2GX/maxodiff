package org.p2gx.maxodiff.cli.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IoUtilTest {

    private static final Path OUT = Path.of("results");

    @Test
    void defaultNameIsInsideTheOutputDirectory() {
        Path path = IoUtil.defaultPath(OUT, "PMID_1", 20, 80, "json");

        assertEquals(OUT.resolve("PMID_1_20_80_maxodiff_results.json"), path);
    }

    @Test
    void theSuffixIsAddedWithADot() {
        assertEquals(OUT.resolve("patient1.html"), IoUtil.resolveOutputFile("patient1", OUT, "html"));
    }

    @Test
    void aNameThatAlreadyHasTheSuffixIsNotChanged() {
        assertEquals(OUT.resolve("patient1.html"), IoUtil.resolveOutputFile("patient1.html", OUT, "html"));
        assertEquals(OUT.resolve("patient1.HTML"), IoUtil.resolveOutputFile("patient1.HTML", OUT, "html"));
    }

    @Test
    void aNameThatOnlyEndsWithTheLettersOfTheSuffixStillGetsTheSuffix() {
        assertEquals(OUT.resolve("myjson.json"), IoUtil.resolveOutputFile("myjson", OUT, "json"));
    }

    @Test
    void anAbsoluteNameIgnoresTheOutputDirectory() {
        Path absolute = Path.of("/tmp/x").toAbsolutePath();

        assertEquals(absolute.resolveSibling("x.json"), IoUtil.resolveOutputFile(absolute.toString(), OUT, "json"));
    }

    @Test
    void anEmptyNameFallsBackToADefault() {
        assertEquals(OUT.resolve("default_output.json"), IoUtil.resolveOutputFile("  ", OUT, "json"));
    }

    @Test
    void missingFoldersAreCreated(@TempDir Path temp) throws Exception {
        Path file = temp.resolve("a").resolve("b").resolve("out.html");

        IoUtil.createParentDirectories(file);

        assertTrue(Files.isDirectory(file.getParent()));
    }
}