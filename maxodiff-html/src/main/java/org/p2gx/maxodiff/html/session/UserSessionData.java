package org.p2gx.maxodiff.html.session;


import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.HpoFrequency;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.p2gx.maxodiff.core.model.PhenopacketData;
import org.p2gx.maxodiff.core.model.RankMaxo;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MaxodiffController is a Singleton (as is every Spring controller).
 * Thus, If User A uploads a phenopacket and then User B uploads a
 * different one, User A’s data is overwritten.
 * A Session-scoped bean is an object that Spring creates once per user session.
 * It lives as long as the user's browser tab is open (or until the session times out).
 */
@Component
@SessionScope
public class UserSessionData implements Serializable {
    private PhenopacketData ppkt;
    private RankMaxo rankMaxo;
    private List<DifferentialDiagnosis> orderedDiagnoses;
    private Map<String, List<HpoFrequency>> hpoTermCounts;
    Map<String, Set<String>> maxoToHpoTermIdMap;

    public Map<String, Set<String>> getMaxoToHpoTermIdMap() {
        return maxoToHpoTermIdMap;
    }

    public void setMaxoToHpoTermIdMap(Map<String, Set<String>> maxoToHpoTermIdMap) {
        this.maxoToHpoTermIdMap = maxoToHpoTermIdMap;
    }

    public Map<String, List<HpoFrequency>> getHpoTermCounts() {
        return hpoTermCounts;
    }

    public void setHpoTermCounts(Map<String, List<HpoFrequency>> hpoTermCounts) {
        this.hpoTermCounts = hpoTermCounts;
    }

    public List<DifferentialDiagnosis> getOrderedDiagnoses() {
        return orderedDiagnoses;
    }

    public int getDiagnosesCount() {
        return orderedDiagnoses == null ? 0 : orderedDiagnoses.size();
    }

    public void setOrderedDiagnoses(List<DifferentialDiagnosis> orderedDiagnoses) {
        this.orderedDiagnoses = orderedDiagnoses;
    }



    public PhenopacketData getPpkt() {
        return ppkt;
    }
    public void setPpkt(PhenopacketData ppkt) {
        this.ppkt = ppkt;
    }
    public RankMaxo getRankMaxo() {
        return rankMaxo;
    }
    public void setRankMaxo(RankMaxo rankMaxo) {
        this.rankMaxo = rankMaxo;
    }

    public List<TermId> getPpltMaxoIds() {
        return this.ppkt.maxoProcedureIds();
    }
}
