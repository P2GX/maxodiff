package org.monarchinitiative.maxodiff.cli.cmd;

import org.monarchinitiative.biodownload.FileDownloadException;
import org.monarchinitiative.maxodiff.config.MaxodiffBuilder;
import org.monarchinitiative.maxodiff.core.analysis.MaxoDiffVisualizer;
import org.monarchinitiative.maxodiff.core.analysis.MaxodiffAnalyzer;
import org.monarchinitiative.maxodiff.core.io.InputFileParser;
import org.monarchinitiative.maxodiff.core.service.MaxoDiffService;
import org.monarchinitiative.maxodiff.core.service.PhenotypeService;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;






/**
 * Extract Maxo terms and HPO terms to differentiate a list of diseases
 * {@code data}, which is created if necessary. We download the files {@code hp.obo}, {@code phenotype.hpoa},
 * {@code Homo_sapiencs_gene_info.gz}, and {@code mim2gene_medgen}.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */

@CommandLine.Command(name = "org/monarchinitiative/maxodiff/cli", aliases = {"M"},
        mixinStandardHelpOptions = true,
        description = "maxodiff analysis")
public class MaxodiffCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaxodiffCommand.class);
    @CommandLine.Option(names={"-d","--data"}, description ="directory to download data (default: ${DEFAULT-VALUE})" )
    public String datadir="data";

    @CommandLine.Option(names={"-i","--input"},
            required = true,
            description = "Input file (diseases (OMIM), one to a line")
    public String inputFile;

    @Override
    public Integer call() throws FileDownloadException {
        MaxodiffBuilder builder = new MaxodiffBuilder(Path.of(datadir));
        PhenotypeService service = builder.phenotypeService();
        InputFileParser parser = new InputFileParser(Path.of(inputFile));
        List<TermId> diseaseTermIds = parser.getDiseaseTermIds();
        LOGGER.info("Got {} disease term IDs", diseaseTermIds.size());
        MaxodiffAnalyzer analyzer = new MaxodiffAnalyzer(service, diseaseTermIds);
        MaxoDiffService diffService = analyzer.maxoDiffService();
        MaxoDiffVisualizer visualizer = new MaxoDiffVisualizer(diffService);
        List<List<String>> matrix = visualizer.diseaseToMaxoMatrix();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("maxo_matrix.tsv"))) {
            for (var row: matrix) {
                String line = String.join("\t", row);
                bw.write(line + "\n");
            }
        } catch (IOException e ){
            e.printStackTrace();
        }
        matrix = visualizer.diseaseToHpoMatrix();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("hpo_matrix.tsv"))) {
            for (var row: matrix) {
                String line = String.join("\t", row);
                bw.write(line + "\n");
            }
        } catch (IOException e ){
            e.printStackTrace();
        }
        List<String> disease2HpMaxo = visualizer.diseaseToHpoMaxo();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("disease_hpo_maxo.tsv"))) {
            for (var line: disease2HpMaxo) {
                bw.write(line + "\n");
            }
        } catch (IOException e ){
            e.printStackTrace();
        }

        return 0;
    }
}
