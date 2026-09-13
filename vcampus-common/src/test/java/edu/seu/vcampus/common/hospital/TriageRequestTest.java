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

    @Test
    void allowsSafetyAnswersInAdditionToFourAiFollowUps() {
        TriageFollowUpAnswer answer = new TriageFollowUpAnswer(
                "疼痛持续多久？", " 大约两天 ");
        TriageRequest request = new TriageRequest(
                "腹痛", false, List.of(answer));

        assertEquals("大约两天", request.getFollowUpAnswers().getFirst().getAnswer());
        assertEquals(5, new TriageRequest(
                "腹痛", false, List.of(answer, answer, answer, answer, answer))
                .getFollowUpAnswers().size());
        assertThrows(IllegalArgumentException.class, () -> new TriageRequest(
                "腹痛", false,
                java.util.Collections.nCopies(
                        TriageRequest.MAX_FOLLOW_UP_ANSWERS + 1, answer)));
        assertThrows(IllegalArgumentException.class,
                () -> new TriageFollowUpAnswer("疼痛持续多久？", " "));
    }

    @Test
    void representsAnAiFollowUpWithoutPretendingItIsARecommendation() {
        TriageResultView result = new TriageResultView(
                false,
                "请补充一个关键信息",
                List.of(),
                "疼痛主要位于哪里？",
                2,
                true,
                false);

        assertTrue(result.needsFollowUp());
        assertEquals(2, result.getCompletedFollowUps());
        assertTrue(result.isAiAssisted());
        assertThrows(IllegalArgumentException.class,
                () -> new TriageResultView(
                        false, "请补充", List.of(), "问题", 0, false, false));
    }

    @Test
    void carriesAStructuredServerOwnedQuestionAndAnswer() {
        TriageResultView question = new TriageResultView(
                false,
                "请选择最符合的一项",
                List.of(),
                "safety_breathing",
                "你现在呼吸是否明显费力？",
                List.of(
                        new TriageFollowUpOptionView("normal", "没有，能正常说话"),
                        new TriageFollowUpOptionView("severe", "很费力")),
                0,
                false,
                false);
        TriageFollowUpAnswer answer = new TriageFollowUpAnswer(
                "safety_breathing", question.getFollowUpQuestion(),
                "normal", "没有，能正常说话");

        assertTrue(question.hasStructuredFollowUp());
        assertEquals(2, question.getFollowUpOptions().size());
        assertTrue(answer.hasStructuredAnswer());
        assertEquals("normal", answer.getAnswerId());
        assertThrows(IllegalArgumentException.class,
                () -> new TriageFollowUpAnswer(
                        "safety_breathing", "问题？", null, "回答"));
    }
}
