package org.monarchinitiative.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.maxodiff.core.TestResources;
import org.monarchinitiative.maxodiff.core.service.DfsHpoTermArranger;
import org.monarchinitiative.maxodiff.core.service.HpoTermArranger;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public class DfsHpoTermArrangerTest {

    private static final List<TermId> HPO_TERM_LIST = List.of(
            TermId.of("HP:4000154"), //liver_leiomyoma
            TermId.of("HP:0033132"), //renal_cortical_hyperechogenicity
            TermId.of("HP:0002066"), //gait_ataxia
            TermId.of("HP:0001629"), //vsd
            TermId.of("HP:0001260"), //dysarthria
            TermId.of("HP:0001682"), //subvalvular_as
            TermId.of("HP:0009321"), //absent_epiphysis
            TermId.of("HP:0001263"), //gdd
            TermId.of("HP:0031207"), //hepatic_hemangioma
            TermId.of("HP:0031600"), //p_wave_inversion
            TermId.of("HP:0005584"), //renal_cell_carcinoma
            TermId.of("HP:0000108"), //renal_corticomedullary_cysts
            TermId.of("HP:0034548"), //portal_vein_hypoplasia
            TermId.of("HP:0031333"), //myocardial_sarcomeric_disarray
            TermId.of("HP:0041239") //fractured_thumb_phalanx
    );

    private static final Ontology ONTOLOGY = TestResources.hpo();
    private final HpoTermArranger hpoTermArranger = new DfsHpoTermArranger(ONTOLOGY);


    @Test
    public void testDfsHpoTermArranger() {
        List<TermId> arrangedTerms = hpoTermArranger.arrangeTerms(HPO_TERM_LIST);
        System.out.println(arrangedTerms);
    }
}
