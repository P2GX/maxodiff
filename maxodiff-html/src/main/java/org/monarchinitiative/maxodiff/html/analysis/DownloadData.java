package org.monarchinitiative.maxodiff.html.analysis;

import org.monarchinitiative.biodownload.BioDownloader;
import org.monarchinitiative.maxodiff.html.config.MaxodiffConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

public class DownloadData {

    public void downloadLiricalData(MaxodiffConfig config, Path destinationFolder) throws Exception {
        System.out.println("Downloading LIRICAL data files to " + destinationFolder.toAbsolutePath());
        BioDownloader downloader = BioDownloader.builder(destinationFolder)
                .overwrite(false)
                .hpoJson()
                .hpDiseaseAnnotations()
                .hgnc()
                .medgene2MIM()
                // Jannovar v0.35 transcript databases
                .custom("hg19_ucsc.ser", createUrlOrExplode(config.jannovarHg19UcscUrl()))
                .custom("hg19_refseq.ser", createUrlOrExplode(config.jannovarHg19RefseqUrl()))
                .custom("hg38_ucsc.ser", createUrlOrExplode(config.jannovarHg38UcscUrl()))
                .custom("hg38_refseq.ser", createUrlOrExplode(config.jannovarHg38RefseqUrl()))
                                .build();
        downloader.download();
    }

    public void downloadMaxodiffData(MaxodiffConfig config, Path destinationFolder) throws Exception {
        System.out.println("Downloading maxodiff data files to " + destinationFolder.toAbsolutePath());
        BioDownloader downloader = BioDownloader.builder(destinationFolder)
                .overwrite(false)
                .hpoJson()
                .hpDiseaseAnnotations()
                .hgnc()
                .medgene2MIM()
                .maxoJson()
                .custom("maxo_diagnostic_annotations.tsv", createUrlOrExplode(config.maxoDiagnosticAnnotsUrl()))
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

}
