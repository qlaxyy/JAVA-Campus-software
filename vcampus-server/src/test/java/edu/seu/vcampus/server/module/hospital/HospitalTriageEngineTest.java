package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.TriageMatchLevel;
import edu.seu.vcampus.common.hospital.TriageFollowUpAnswer;
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

    @Test
    void treatsAnExplicitDangerouslyHighTemperatureAsUrgent() {
        var withUnit = engine.triage(
                new TriageRequest("发烧", false, List.of(
                        new TriageFollowUpAnswer("测得体温是多少？", "50度"))),
                departments);
        var numberOnlyAnswer = engine.triage(
                new TriageRequest("发烧", false, List.of(
                        new TriageFollowUpAnswer("测得体温是多少？", "41"))),
                departments);
        var ordinaryFever = engine.triage(
                new TriageRequest("发烧", false, List.of(
                        new TriageFollowUpAnswer("测得体温是多少？", "38.5度"))),
                departments);

        assertTrue(withUnit.isUrgent());
        assertTrue(numberOnlyAnswer.isUrgent());
        assertFalse(ordinaryFever.isUrgent());
    }

    @Test
    void usesRedFlagsAcrossSymptomFamiliesButRespectsExplicitNegation() {
        var neurological = engine.triage(
                new TriageRequest("突然嘴歪，说话含糊", false), departments);
        var poisoning = engine.triage(
                new TriageRequest("不小心服用过量药物", false), departments);
        var negated = engine.triage(
                new TriageRequest("咳嗽，没有呼吸困难，也不是持续胸痛", false),
                departments);

        assertTrue(neurological.isUrgent());
        assertTrue(neurological.getSafetyMessage().contains("肢体活动"));
        assertTrue(poisoning.isUrgent());
        assertFalse(negated.isUrgent());
    }

    private static HospitalDepartment department(String id, String name) {
        return new HospitalDepartment(id, name, null, true, true);
    }
}
