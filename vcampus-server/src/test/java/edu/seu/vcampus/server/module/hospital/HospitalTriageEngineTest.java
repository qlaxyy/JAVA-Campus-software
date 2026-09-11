package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.TriageMatchLevel;
import edu.seu.vcampus.common.hospital.TriageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HospitalTriageEngineTest {

    private final HospitalTriageEngine engine = new HospitalTriageEngine();
    private final List<HospitalDepartment> departments = List.of(
            department("dept-general", "全科门诊"),
            department("dept-respiratory", "呼吸内科"),
            department("dept-gastroenterology", "消化内科"),
            department("dept-sports-medicine", "运动医学科"));

    @Test
    void recommendsExistingBookableDepartmentWithExplainableMatch() {
        var result = engine.triage(
                new TriageRequest("咳嗽发热，同时觉得胸闷", false),
                departments);

        assertFalse(result.isUrgent());
        assertEquals("dept-respiratory",
                result.getRecommendations().getFirst().getDepartmentId());
        assertEquals(TriageMatchLevel.HIGH,
                result.getRecommendations().getFirst().getMatchLevel());
        assertEquals(2, result.getRecommendations().getFirst().getReasons().size());
    }

    @Test
    void fallsBackToGeneralPracticeWhenNoSpecialtyRuleMatches() {
        var result = engine.triage(
                new TriageRequest("最近总觉得不舒服，无法确定具体部位", false),
                departments);

        assertFalse(result.isUrgent());
        assertEquals(1, result.getRecommendations().size());
        assertEquals("dept-general",
                result.getRecommendations().getFirst().getDepartmentId());
        assertEquals(TriageMatchLevel.GENERAL,
                result.getRecommendations().getFirst().getMatchLevel());
    }

    @Test
    void stopsOrdinaryRecommendationsForUrgentConcernOrKeyword() {
        var explicit = engine.triage(new TriageRequest("", true), departments);
        var keyword = engine.triage(
                new TriageRequest("出现持续胸痛", false), departments);

        assertTrue(explicit.isUrgent());
        assertTrue(explicit.getRecommendations().isEmpty());
        assertTrue(keyword.isUrgent());
        assertTrue(keyword.getRecommendations().isEmpty());
    }

    @Test
    void neverRecommendsANonBookableDepartment() {
        List<HospitalDepartment> limited = List.of(
                department("dept-general", "全科门诊"),
                new HospitalDepartment(
                        "dept-respiratory", "呼吸内科", null, false, true));

        var result = engine.triage(
                new TriageRequest("咳嗽发热", false), limited);

        assertEquals("dept-general",
                result.getRecommendations().getFirst().getDepartmentId());
    }

    private static HospitalDepartment department(String id, String name) {
        return new HospitalDepartment(id, name, null, true, true);
    }
}
