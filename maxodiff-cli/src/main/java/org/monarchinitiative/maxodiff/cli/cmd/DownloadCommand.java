package org.monarchinitiative.maxodiff.cli.cmd;

import org.monarchinitiative.biodownload.BioDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Download a number of files needed for the analysis.
 * <p>
 * By default, we download by default to a subdirectory called {@code data}, which is created if necessary. 
 *
 * @author <a href="mailto:martha.beckwith@jax.org">Martha Beckwith</a>
 * @author <a href="mailto:daniel.gordon.danis@protonmail.com">Daniel Danis</a>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@CommandLine.Command(
    name = "download", 
    aliases = {"D"},
    mixinStandardHelpOptions = true,
    description = "Download files for maxodiff"
)
public class DownloadCommand implements Callable<Integer>{

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadCommand.class);
    
    @CommandLine.Option(
        names={"-d","--data"}, 
        description ="directory to download data (default: ${DEFAULT-VALUE})"
    )
    public Path datadir= Path.of("data");

    @CommandLine.Option(
        names={"-w","--overwrite"}, 
        description = "overwrite previously downloaded files (default: ${DEFAULT-VALUE})"
    )
    public boolean overwrite;

    @Override
    public Integer call() throws Exception {
        LOGGER.info("Downloading maxodiff data files to {}", datadir.toAbsolutePath());
        downloadMaxodiffData(datadir, overwrite);
        
        Path liricalDataPath = datadir.resolve("lirical");
        LOGGER.info("Downloading LIRICAL data files to {}", liricalDataPath.toAbsolutePath());
        downloadLiricalData(liricalDataPath, overwrite);
        
        LOGGER.info("Download is complete!");
        LOGGER.info("Bye! 😎");
        return 0;
    }

    private static void downloadLiricalData(Path destinationFolder, boolean overwrite) throws Exception {
        BioDownloader downloader = BioDownloader.builder(destinationFolder)
                .overwrite(overwrite)
                .hpoJson()
                .hpDiseaseAnnotations()
                .hgnc()
                .medgene2MIM()
                // Jannovar v0.35 transcript databases
                .custom("hg19_ucsc.ser", createUrlOrExplode("https://storage.googleapis.com/ielis/jannovar/v0.35/hg19_ucsc.ser"))
                .custom("hg19_refseq.ser", createUrlOrExplode("https://storage.googleapis.com/ielis/jannovar/v0.35/hg19_refseq.ser"))
                .custom("hg38_ucsc.ser", createUrlOrExplode("https://storage.googleapis.com/ielis/jannovar/v0.35/hg38_ucsc.ser"))
                .custom("hg38_refseq.ser", createUrlOrExplode("https://storage.googleapis.com/ielis/jannovar/v0.35/hg38_refseq.ser"))
                .build();
        downloader.download();
    }

    private static void downloadMaxodiffData(Path destinationFolder, boolean overwrite) throws Exception { 
        BioDownloader downloader = BioDownloader.builder(destinationFolder)
                .overwrite(overwrite)
                .hpoJson()
                .hpDiseaseAnnotations()
                .hgnc()
                .medgene2MIM()
                .maxoJson()
                .custom("maxo_diagnostic_annotations.tsv", createUrlOrExplode("https://raw.githubusercontent.com/monarch-initiative/maxo-annotations/master/annotations/maxo_diagnostic_annotations.tsv"))
                .build();
        downloader.download();
    }

    private static URL createUrlOrExplode(String url) throws Exception {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new Exception(e);
        }
    }

}

