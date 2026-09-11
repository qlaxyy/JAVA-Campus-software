package edu.seu.vcampus.common.hospital;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriageRequestTest {

    @Test
    void validatesDescriptionWithoutBlockingAnExplicitUrgentConcern() {
        assertEquals("咳嗽两天", new TriageRequest("  咳嗽两天  ", false)
                .getSymptomDescription());
        assertThrows(IllegalArgumentException.class,
                () -> new TriageRequest("", false));
        assertThrows(IllegalArgumentException.class,
                () -> new TriageRequest("症".repeat(501), false));
        assertTrue(new TriageRequest("", true).hasUrgentConcern());
    }

    @Test
    void urgentResultCannotContainOrdinaryRecommendations() {
        TriageRecommendationView recommendation = new TriageRecommendationView(
                "dept-general", "全科门诊", TriageMatchLevel.GENERAL,
                List.of("先由全科评估"));
        assertThrows(IllegalArgumentException.class,
                () -> new TriageResultView(
                        true, "请立即寻求线下帮助", List.of(recommendation)));
    }
}
