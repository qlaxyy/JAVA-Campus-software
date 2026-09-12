package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.TriageFollowUpAnswer;
import edu.seu.vcampus.common.hospital.TriageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HospitalRedFlagEngineTest {

    private final HospitalRedFlagEngine engine = new HospitalRedFlagEngine();

    @Test
    void detectsCoreRedFlagCategoriesWithoutNamingADisease() {
        List<String> descriptions = List.of(
                "现在呼吸困难，喘不过气",
                "持续胸痛已经十分钟",
                "突然嘴歪，说话含糊",
                "伤口大量出血",
                "吃药后舌头肿胀",
                "突然剧烈腹痛",
                "不小心服用过量药物",
                "我有明确的自杀计划");

        for (String description : descriptions) {
            assertTrue(engine.evaluate(new TriageRequest(description, false)).isPresent(),
                    description);
        }
    }

    @Test
    void understandsAffirmativeAndNegativeFollowUpAnswersInQuestionContext() {
        TriageRequest confirmed = new TriageRequest(
                "头晕", false, List.of(
                        new TriageFollowUpAnswer("有没有呼吸困难？", "有")));
        TriageRequest denied = new TriageRequest(
                "头晕", false, List.of(
                        new TriageFollowUpAnswer("有没有呼吸困难？", "没有")));

        assertTrue(engine.evaluate(confirmed).isPresent());
        assertFalse(engine.evaluate(denied).isPresent());
    }

    @Test
    void doesNotTriggerOnNegatedDangerPhrases() {
        TriageRequest request = new TriageRequest(
                "只是咳嗽，没有呼吸困难，也没有持续胸痛", false);

        assertFalse(engine.evaluate(request).isPresent());
    }

    @Test
    void interpretsANumberOnlyAnswerAsTemperatureOnlyForATemperatureQuestion() {
        TriageRequest temperature = new TriageRequest(
                "发烧", false, List.of(
                        new TriageFollowUpAnswer("体温是多少度？", "50")));
        TriageRequest unrelatedNumber = new TriageRequest(
                "腹痛", false, List.of(
                        new TriageFollowUpAnswer("疼痛持续多少分钟？", "50")));

        assertTrue(engine.evaluate(temperature).isPresent());
        assertFalse(engine.evaluate(unrelatedNumber).isPresent());
    }
}
