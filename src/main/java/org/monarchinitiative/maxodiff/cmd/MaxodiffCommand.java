package org.monarchinitiative.maxodiff.cmd;

import org.monarchinitiative.biodownload.FileDownloadException;
import org.monarchinitiative.maxodiff.io.InputFileParser;
import org.monarchinitiative.maxodiff.io.MaxodiffBuilder;
import org.monarchinitiative.maxodiff.service.PhenotypeService;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;






/**
 * Extract Maxo terms and HPO terms to differentiate a list of diseases
 * {@code data}, which is created if necessary. We download the files {@code hp.obo}, {@code phenotype.hpoa},
 * {@code Homo_sapiencs_gene_info.gz}, and {@code mim2gene_medgen}.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */

@CommandLine.Command(name = "org/monarchinitiative/maxodiff", aliases = {"M"},
        mixinStandardHelpOptions = true,
        description = "maxodiff analysis")
public class MaxodiffCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadCommand.class);
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

        return 0;
    }
}
