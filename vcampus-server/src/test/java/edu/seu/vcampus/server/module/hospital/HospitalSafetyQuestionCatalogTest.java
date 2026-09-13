package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.TriageFollowUpAnswer;
import edu.seu.vcampus.common.hospital.TriageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HospitalSafetyQuestionCatalogTest {

    private final HospitalSafetyQuestionCatalog catalog =
            new HospitalSafetyQuestionCatalog();

    @Test
    void asksOnePatientReadableBreathingQuestionForACough() {
        var question = catalog.nextQuestion(new TriageRequest("只是咳嗽", false))
                .orElseThrow();

        assertEquals("safety_breathing", question.questionId());
        assertEquals("你现在呼吸是否明显费力？", question.prompt());
        assertTrue(question.options().stream()
                .anyMatch(option -> option.getOptionId().equals("normal")
                        && option.getLabel().contains("正常说话")));
        assertTrue(question.options().stream()
                .anyMatch(option -> option.getOptionId().equals("severe")
                        && option.getLabel().contains("停下来喘气")));
    }

    @Test
    void movesFromBreathingToTemperatureForFeverWithoutRepeating() {
        TriageFollowUpAnswer breathingNormal = new TriageFollowUpAnswer(
                "safety_breathing",
                "你现在呼吸是否明显费力？",
                "normal",
                "没有，能够正常说话");

        var question = catalog.nextQuestion(new TriageRequest(
                "咳嗽并伴有发烧", false, List.of(breathingNormal)))
                .orElseThrow();

        assertEquals("safety_temperature", question.questionId());
    }

    @Test
    void mapsOnlyServerKnownDangerousChoicesToRedFlags() {
        TriageFollowUpAnswer severe = new TriageFollowUpAnswer(
                "safety_breathing", "你现在呼吸是否明显费力？",
                "severe", "很费力，说话需要停下来喘气");
        TriageFollowUpAnswer normal = new TriageFollowUpAnswer(
                "safety_breathing", "你现在呼吸是否明显费力？",
                "normal", "没有，能够正常说话");

        assertTrue(catalog.redFlagReason(severe).isPresent());
        assertFalse(catalog.redFlagReason(normal).isPresent());
    }

    @Test
    void doesNotLetAnUnknownStructuredAnswerSkipSafetyScreening() {
        TriageFollowUpAnswer forged = new TriageFollowUpAnswer(
                "safety_breathing", "伪造的问题", "not_a_real_option", "伪造的回答");

        var question = catalog.nextQuestion(new TriageRequest(
                "只是咳嗽", false, List.of(forged))).orElseThrow();

        assertEquals("safety_breathing", question.questionId());
    }

    @Test
    void doesNotLetFourUnknownStructuredAnswersExhaustSafetyScreening() {
        TriageFollowUpAnswer forged = new TriageFollowUpAnswer(
                "safety_breathing", "伪造的问题", "not_a_real_option", "伪造的回答");

        var question = catalog.nextQuestion(new TriageRequest(
                "只是咳嗽", false, List.of(forged, forged, forged, forged)))
                .orElseThrow();

        assertEquals("safety_breathing", question.questionId());
    }
}
