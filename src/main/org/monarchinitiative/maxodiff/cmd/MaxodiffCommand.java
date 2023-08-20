package org.monarchinitiative.maxodiff.cmd;

import org.monarchinitiative.biodownload.FileDownloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;






/**
 * Extract Maxo terms and HPO terms to differentiate a list of diseases
 * {@code data}, which is created if necessary. We download the files {@code hp.obo}, {@code phenotype.hpoa},
 * {@code Homo_sapiencs_gene_info.gz}, and {@code mim2gene_medgen}.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */

@CommandLine.Command(name = "maxodiff", aliases = {"M"},
        mixinStandardHelpOptions = true,
        description = "maxodiff analysis")
public class MaxodiffCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(DownloadCommand.class);
    @CommandLine.Option(names={"-d","--data"}, description ="directory to download data (default: ${DEFAULT-VALUE})" )
    public String datadir="data";

    @CommandLine.Option(names={"-i","--input"},
            required = true,
            description = "Input file (diseases (OMIM), one to a line")
    public String inputFile;

    @Override
    public Integer call() throws FileDownloadException {


        return 0;
    }
}
