package org.p2gx.maxodiff.html.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.p2gx.maxodiff.html.config.AnalysisGate;
import org.p2gx.maxodiff.html.config.ServerBusyException;
import org.phenopackets.schema.v2.Phenopacket;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MaxodiffControllerGateTest {

    private static final Phenopacket PPKT = Phenopacket.newBuilder().setId("p1").build();

    private static MaxodiffController controllerWith(AnalysisGate gate) {
        return new MaxodiffController(null, null, null, gate);   // the gate comes before any use of the others
    }

    @Test
    void analyzeAnswersBusyWhenNoSlotIsFree() {
        AnalysisGate gate = new AnalysisGate(1, Duration.ZERO);
        gate.enter();                                   // the only slot stays taken

        assertThrows(ServerBusyException.class, () -> controllerWith(gate).analyzeJson(PPKT, 20, 80));
    }

    @Test
    void modalityAnswersBusyWhenNoSlotIsFree() {
        AnalysisGate gate = new AnalysisGate(1, Duration.ZERO);
        gate.enter();

        assertThrows(ServerBusyException.class,
                () -> controllerWith(gate).analyzeBestModalityJson(PPKT, "OMIM:157700", 20, 80));
    }

    @Test
    void theSlotIsGivenBackWhenTheAnalysisFails() {
        AnalysisGate gate = new AnalysisGate(1, Duration.ZERO);

        // there is no context, so the analysis fails once it has the slot
        assertThrows(Exception.class, () -> controllerWith(gate).analyzeJson(PPKT, 20, 80));

        assertDoesNotThrow(gate::enter);                // the slot is free again
    }

    @Test
    void invalidInputGetsA400EvenWhenTheServerIsBusy() throws Exception {
        AnalysisGate gate = new AnalysisGate(1, Duration.ZERO);
        gate.enter();                                   // no slot is free

        ResponseEntity<?> response = controllerWith(gate).analyzeJson(PPKT, 5000, 80);

        assertEquals(400, response.getStatusCode().value());
        assertInstanceOf(ApiProblems.class, response.getBody());
    }

    @Test
    void aPhenopacketWithoutIdGetsA400() throws Exception {
        ResponseEntity<?> response = controllerWith(new AnalysisGate(1, Duration.ZERO))
                .analyzeJson(Phenopacket.getDefaultInstance(), 20, 80);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void anInvalidTargetDiseaseGetsA400() throws Exception {
        ResponseEntity<?> response = controllerWith(new AnalysisGate(1, Duration.ZERO))
                .analyzeBestModalityJson(PPKT, "foo", 20, 80);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void theBusyAnswerIsJsonWithARetryHint() {
        ResponseEntity<ApiProblems> response = controllerWith(new AnalysisGate(1, Duration.ZERO)).serverBusy();

        assertEquals(503, response.getStatusCode().value());
        assertEquals("60", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        Assertions.assertNotNull(response.getBody());
        assertEquals(1, response.getBody().problems().size());
    }
}