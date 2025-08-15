package org.monarchinitiative.maxodiff.phenomizer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Load a map with information content (IC) of the most informative common ancestor (MICA) of a term pair
 * from a CSV file computed by the <code>precompute-resnik</code> command of Phenol's CLI.
 * <p>
 * The map only includes non-zero values - the IC<sub>MICA</sub> values of terms whose most informative common ancestor
 * is <em>not</em> the ontology root (Phenotypic abnormality).
 * <p>
 * Loading from a compressed file takes about 25s.
 */
public class IcMicaDictLoader {

    private static final CSVFormat CSV_FORMAT = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setCommentMarker('#')
            .setHeader("term_a", "term_b", "ic_mica")
            .setSkipHeaderRecord(true)
            .get();
    /**
     * Pattern of the file header with the metadata.
     * <p>
     * An example line:
     * <pre>
     * # HPO=2025-03-03;HPOA=2025-03-03;CREATED=2025-03-05
     * </pre>
     */
    private static final Pattern HEADER = Pattern.compile("HPO=(?<hpo>[\\w-]*);HPOA=(?<hpoa>[\\w-]*);CREATED=(?<created>[\\w-]*)");

    private IcMicaDictLoader() {
    }

    /**
     * Load IC MICA dictionary from a file path.
     * <p>
     * The file is uncompressed on the fly if the file name ends with <code>.gz</code> suffix.
     *
     * @param path path to a CSV file.
     */
    public static IcMicaData loadIcMicaDict(Path path) throws IOException {
        try (BufferedReader reader = path.toFile().getName().endsWith(".gz")
                ? new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(path))))
                : Files.newBufferedReader(path)
        ) {
            return loadIcMicaDict(reader);
        }
    }

    /**
     * Load IC MICA dictionary from a reader.
     *
     * @param reader reader with uncompressed CSV data.
     */
    public static IcMicaData loadIcMicaDict(Reader reader) throws IOException {
        CSVParser parser = CSV_FORMAT.parse(reader);

        IcMicaDictMetadata metadata = parseMetadata(parser.getHeaderComment());

        Map<TermPair, Double> icMicaDict = new HashMap<>();
        for (CSVRecord record : parser) {
            TermId a = TermId.of(record.get("term_a"));
            TermId b = TermId.of(record.get("term_b"));
            double icMica = Double.parseDouble(record.get("ic_mica"));

            // We use `asymmetric`, because Phenol serializes the table in an appropriate TermPair order.
            TermPair pair = TermPair.asymmetric(a, b);
            icMicaDict.put(pair, icMica);
        }

        return new IcMicaData(icMicaDict, metadata);
    }

    private static IcMicaDictMetadata parseMetadata(String headerComment) {
        for (String line : headerComment.split("\\n")) {
            Matcher matcher = HEADER.matcher(line);
            if (matcher.find()) {
                String hpoVersion = matcher.group("hpo");
                String hpoaVersion = matcher.group("hpoa");
                LocalDate created;
                try {
                    created = LocalDate.parse(matcher.group("created"));
                } catch (DateTimeParseException e) {
                    created = null;
                    // swallow
                }
                return new IcMicaDictMetadata(hpoVersion, hpoaVersion, created);
            }
        }

        return null;
    }
}
