package org.monarchinitiative.maxodiff.core.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementResults;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

//TODO: this can be deleted at some point
public class RefinementResultsFileWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefinementResultsFileWriter.class);
    private static final CSVFormat CSV_FORMAT = CSVFormat.TDF;

    public RefinementResultsFileWriter(RefinementResults refinementResults, String filePath) {
        LOGGER.trace(String.format("Writing refinement results file %s", filePath));
        write(refinementResults, filePath);
    }

    private void write(RefinementResults refinementResults, String path) {
        File f = new File(path);
        try (FileWriter writer = new FileWriter(path);
             CSVPrinter printer = CSV_FORMAT.print(writer)) {
//            printer.printRecord("Rank", "Maxo ID", "N diseases", "N Hpo Terms", "Initial Score", "Final Score", "Score Diff");
            int maxoTermRank = 1;
            for (MaxodiffResult result : refinementResults.maxodiffResults().stream().toList()) {
                MaxoTermScore maxoTermScore = result.maxoTermScore();
                printer.print("Rank");
                printer.print(maxoTermRank);
                printer.println();
                printer.print("Maxo ID");
                printer.print(maxoTermScore.maxoId());
                printer.println();
                printer.print("N Diseases");
                printer.print(maxoTermScore.nOmimTerms());
                printer.println();
                printer.print("N HPO Terms");
                printer.print(maxoTermScore.nHpoTerms());
                printer.println();
                printer.print("Initial Score");
                printer.print(maxoTermScore.initialScore());
                printer.println();
                printer.print("Final Score");
                printer.print(maxoTermScore.score());
                printer.println();
                printer.print("Score Diff");
                printer.print(maxoTermScore.scoreDiff());
                printer.println();

                List<Frequencies> frequencies = result.frequencies();
                printer.print("HPO Term Frequency Table");
                printer.println();
                printer.print("");
                for (TermId omimId : maxoTermScore.omimTermIds()) {
                    printer.print(omimId);
                }
                printer.println();
                for (Frequencies freqs : frequencies) {
                    printer.print(freqs.hpoId());
                    for (Float freq : freqs.frequencies()) {
                        printer.print(freq);
                    }
                    printer.println();
                }
                printer.println();
                maxoTermRank += 1;
            }
            writer.flush();
        } catch (IOException ioe) {
            LOGGER.trace(ioe.getMessage());
        }
    }


}
