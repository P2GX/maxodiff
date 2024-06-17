package org.monarchinitiative.maxodiff.cli.cmd;

import org.monarchinitiative.biodownload.BioDownloader;
import org.monarchinitiative.maxodiff.config.PropertiesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * Download a number of files needed for the analysis. We download by default to a subdirectory called
 * {@code data}, which is created if necessary. We download the files {@code hp.obo}, {@code phenotype.hpoa},
 * {@code Homo_sapiencs_gene_info.gz}, and {@code mim2gene_medgen}.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */

@CommandLine.Command(name = "download", aliases = {"D"},
        mixinStandardHelpOptions = true,
        description = "Download files for maxodiff")
public class DownloadCommand implements Callable<Integer>{
    private static final Logger logger = LoggerFactory.getLogger(DownloadCommand.class);
    @CommandLine.Option(names={"-d","--data"}, description ="directory to download data (default: ${DEFAULT-VALUE})" )
    public String datadir="data";

    @CommandLine.Option(names={"-w","--overwrite"}, description = "overwrite previously downloaded files (default: ${DEFAULT-VALUE})")
    public boolean overwrite;

    @Override
    public Integer call() throws Exception {
        String propFilename = "application.properties";
        Properties properties = PropertiesLoader.loadProperties(propFilename);
        Path maxodiffDataPath = Path.of(datadir);
        Path liricalDataPath = Path.of(String.join(File.separator, datadir, "lirical"));
        logger.info(String.format("Download analysis to %s", datadir));
        String propFilepath = PropertiesLoader.getPropertiesFilepath(propFilename);

        PropertiesLoader.addToPropertiesFile(propFilepath, "lirical-data-directory", liricalDataPath.toString());
        PropertiesLoader.addToPropertiesFile(propFilepath, "maxodiff-data-directory", maxodiffDataPath.toString());

        downloadMaxodiffData(properties, maxodiffDataPath);
        downloadLiricalData(properties, liricalDataPath);

        setDefaultLiricalProperties();
        setDefaultMaxodiffProperties();

        return 0;
    }

    public void downloadLiricalData(Properties properties, Path destinationFolder) throws Exception {
        logger.info("Downloading LIRICAL data files to " + destinationFolder.toAbsolutePath());
        BioDownloader downloader = BioDownloader.builder(destinationFolder)
                .overwrite(overwrite)
                .hpoJson()
                .hpDiseaseAnnotations()
                .hgnc()
                .medgene2MIM()
                // Jannovar v0.35 transcript databases
                .custom("hg19_ucsc.ser", createUrlOrExplode(properties.getProperty("jannovar-hg19-ucsc-url")))
                .custom("hg19_refseq.ser", createUrlOrExplode(properties.getProperty("jannovar-hg19-refseq-url")))
                .custom("hg38_ucsc.ser", createUrlOrExplode(properties.getProperty("jannovar-hg38-ucsc-url")))
                .custom("hg38_refseq.ser", createUrlOrExplode(properties.getProperty("jannovar-hg38-refseq-url")))
                .build();
        downloader.download();
    }

    public void downloadMaxodiffData(Properties properties, Path destinationFolder) throws Exception {
        logger.info("Downloading maxodiff data files to " + destinationFolder.toAbsolutePath());
        BioDownloader downloader = BioDownloader.builder(destinationFolder)
                .overwrite(overwrite)
                .hpoJson()
                .hpDiseaseAnnotations()
                .hgnc()
                .medgene2MIM()
                .maxoJson()
                .custom("maxo_diagnostic_annotations.tsv", createUrlOrExplode(properties.getProperty("maxo-diagnostic-annotations-url")))
                .build();
        downloader.download();
    }

    private URL createUrlOrExplode(String url) throws Exception {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new Exception(e);
        }
    }

    private void setDefaultLiricalProperties() {
        String liricalPropFilepath = PropertiesLoader.getPropertiesFilepath("maxodiff.lirical.properties");
        Map<String, String> liricalDefaultProperties = Map.of("genome-build", "hg38",
                "transcript-database", "REFSEQ",
                "pathogenicity-threshold", "0.8",
                "default-variant-background-frequency", "0.1",
                "strict", "true",
                "global-analysis-mode", "false");

        for (Map.Entry<String, String> entry : liricalDefaultProperties.entrySet()) {
            PropertiesLoader.addToPropertiesFile(liricalPropFilepath, entry.getKey(), entry.getValue());
        }
    }

    private void setDefaultMaxodiffProperties() {
        String maxodiffPropFilepath = PropertiesLoader.getPropertiesFilepath("maxodiff.properties");
        Map<String, String> maxodiffDefaultProperties = Map.of("n-diseases", "20",
                "weight", "0.5",
                "n-maxo-results", "10");

        for (Map.Entry<String, String> entry : maxodiffDefaultProperties.entrySet()) {
            PropertiesLoader.addToPropertiesFile(maxodiffPropFilepath, entry.getKey(), entry.getValue());
        }
    }

}

