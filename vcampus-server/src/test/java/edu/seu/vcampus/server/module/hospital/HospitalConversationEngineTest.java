package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class HospitalConversationEngineTest {
    private final List<HospitalDepartment> departments = List.of(
            new HospitalDepartment("dept-general", "全科", null, true, true),
            new HospitalDepartment("dept-respiratory", "呼吸内科", null, true, true),
            new HospitalDepartment("dept-dental", "口腔科", null, true, true),
            new HospitalDepartment("dept-eye", "眼科", null, true, true),
            new HospitalDepartment("dept-hidden", "停用科室", null, true, false));

    @Test void providerFailureDoesNotMeanEmergency() {
        var result = engine(HospitalAiTriageClient.disabled()).chat(request("牙疼两天"), departments);
        assertFalse(result.guidance().isUrgent());
        assertTrue(result.guidance().isFallbackUsed());
        assertTrue(result.reply().contains("暂时"));
        assertEquals("dept-dental", result.guidance().getRecommendations().getFirst().getDepartmentId());
    }

    @Test void aiCanRecommendANewlyAddedBookableDepartmentFromCurrentDirectory() {
        List<HospitalDepartment> currentDepartments = new java.util.ArrayList<>(departments);
        currentDepartments.add(new HospitalDepartment(
                "dept-sleep", "睡眠健康门诊", null, true, true));
        HospitalAiChatAnalysis analysis = new HospitalAiChatAnalysis(
                "请核对描述后查看科室。", "睡眠节律紊乱", List.of(), true,
                "", -1,
                List.of(new HospitalAiDepartmentCandidate(
                        "dept-sleep", "睡眠相关不适可由该科室评估")),
                List.of(new HospitalChatFact(
                        "主要不适", "reported", "睡眠节律紊乱", 0)));

        var result = engine(client(ignored -> analysis)).chat(
                request("睡眠节律紊乱"), currentDepartments);

        assertEquals("dept-sleep",
                result.guidance().getRecommendations().getFirst().getDepartmentId());
    }

    @Test void newDangerAndNumericTemperatureBypassModel() {
        HospitalAiTriageClient client = client(request -> { fail("must not call model"); return null; });
        var conversation = new TriageChatRequest(List.of(
                user("发烧"), assistant("最近测量的体温是多少度？"), user("50")));
        assertTrue(engine(client).chat(conversation, departments).guidance().isUrgent());
        assertTrue(engine(client).chat(new TriageChatRequest(List.of(
                user("牙疼两天"), assistant("什么时候开始的？"),
                user("现在突然喘不过气"))), departments).guidance().isUrgent());
    }

    @Test void deterministicSafetyQuestionDoesNotWaitForModel() {
        HospitalAiTriageClient client = client(request -> {
            fail("local safety question must be returned before calling the model");
            return null;
        });
        var result = engine(client).chat(request("只是咳嗽两天"), departments);
        assertTrue(result.guidance().hasStructuredFollowUp());
        assertFalse(result.guidance().isAiAssisted());
    }

    @Test void explanationAndNegationAreNotPositiveSymptoms() {
        var analysis = new HospitalAiChatAnalysis("是指喘气很费力。你现在呼吸是否明显费力？",
                "", List.of("呼吸情况"), false, "呼吸困难", 2, List.of(), List.of());
        var request = new TriageChatRequest(List.of(user("咳嗽两天"),
                assistant("你现在呼吸是否明显费力？"), user("没有呼吸困难")));
        assertFalse(engine(client(ignored -> analysis)).chat(request, departments).guidance().isUrgent());
        assertFalse(engine(HospitalAiTriageClient.disabled()).chat(
                request("呼吸困难是什么意思？"), departments).guidance().isUrgent());
        assertTrue(engine(HospitalAiTriageClient.disabled()).chat(
                request("有持续胸痛，呼吸困难是什么意思？"), departments).guidance().isUrgent());
    }

    @Test void freeTextCanAnswerSafetyQuestionWithoutChoiceClick() {
        var request = new TriageChatRequest(List.of(user("咳嗽两天"),
                assistant("你现在呼吸是否明显费力？"), user("没有")));
        var result = engine(HospitalAiTriageClient.disabled()).chat(request, departments);
        assertFalse(result.guidance().hasStructuredFollowUp());
        assertFalse(result.guidance().isUrgent());
    }

    @Test void freeTextSafetyAnswerIsNotMistakenForAnUncoveredComplaint() {
        var request = new TriageChatRequest(List.of(user("咳嗽两天"),
                assistant("你现在呼吸是否明显费力？"), user("没有，可以正常说话")));
        var analysis = new HospitalAiChatAnalysis("请核对后查看科室。", "咳嗽两天",
                List.of(), true, "", -1,
                List.of(new HospitalAiDepartmentCandidate(
                        "dept-respiratory", "咳嗽可由呼吸内科评估")),
                List.of(new HospitalChatFact(
                        "主要不适", "reported", "咳嗽两天", 0)));

        var result = engine(client(ignored -> analysis)).chat(request, departments);

        assertFalse(result.guidance().getRecommendations().isEmpty(),
                () -> "reply=" + result.reply() + ", summary=" + result.summary()
                        + ", missing=" + result.missingInformation());
        assertEquals("dept-respiratory",
                result.guidance().getRecommendations().getFirst().getDepartmentId());
        assertTrue(result.missingInformation().isEmpty());
    }

    @Test void modelReceivesCorrectionsAndOnlyGroundedCurrentFactsAreDisplayed() {
        AtomicReference<TriageChatRequest> captured = new AtomicReference<>();
        var request = new TriageChatRequest(List.of(user("牙疼"),
                assistant("哪里疼？"), user("刚才说错了，是眼睛干涩两天")));
        var analysis = new HospitalAiChatAnalysis("请核对情况，再查看科室。",
                "错误摘要：患者高烧", List.of(), true, "", -1,
                List.of(new HospitalAiDepartmentCandidate("dept-eye", "眼部不适属于眼科服务范围"),
                        new HospitalAiDepartmentCandidate("dept-hidden", "不应出现"),
                        new HospitalAiDepartmentCandidate("not-real", "不应出现")),
                List.of(new HospitalChatFact("主要不适", "reported", "眼睛干涩两天", 2),
                        new HospitalChatFact("伴随表现", "reported", "高烧", 2)));
        var result = engine(client(input -> { captured.set(input); return analysis; }))
                .chat(request, departments);
        assertEquals(request, captured.get());
        assertTrue(result.summary().contains("眼睛干涩两天"));
        assertFalse(result.summary().contains("高烧"));
        assertEquals(List.of("dept-eye"), result.guidance().getRecommendations().stream()
                .map(TriageRecommendationView::getDepartmentId).toList());
    }

    @Test void readyModelCannotSilentlySkipASeparatePatientComplaint() {
        var request = new TriageChatRequest(List.of(
                user("咳嗽两天，另外牙疼"),
                assistant("你现在呼吸是否明显费力？"),
                new TriageChatMessage("user", "没有，能够正常说话",
                        "safety_breathing", "normal")));
        var incomplete = new HospitalAiChatAnalysis("请核对后查看科室。", "咳嗽两天",
                List.of(), true, "", -1,
                List.of(new HospitalAiDepartmentCandidate(
                        "dept-respiratory", "咳嗽可由呼吸内科评估")),
                List.of(new HospitalChatFact(
                        "主要不适", "reported", "咳嗽两天", 0)));

        var result = engine(client(ignored -> incomplete)).chat(request, departments);

        assertTrue(result.guidance().getRecommendations().isEmpty());
        assertTrue(result.reply().contains("牙疼"));
        assertTrue(result.missingInformation().getFirst().contains("牙疼"));
    }

    @Test void patientReadableMentalAndNeurologicalSafetyChoicesCanEscalateLocally() {
        HospitalAiTriageClient disabled = HospitalAiTriageClient.disabled();
        var mentalQuestion = engine(disabled).chat(request("最近一直情绪低落"), departments);
        assertEquals("safety_self_harm",
                mentalQuestion.guidance().getFollowUpQuestionId());
        var mentalAnswer = new TriageChatRequest(List.of(
                user("最近一直情绪低落"),
                assistant(mentalQuestion.guidance().getFollowUpQuestion()),
                new TriageChatMessage("user", "有这样的想法，但没有具体计划",
                        "safety_self_harm", "thoughts")));
        assertTrue(engine(disabled).chat(mentalAnswer, departments).guidance().isUrgent());

        var neurologicalAnswer = new TriageChatRequest(List.of(
                user("突然头痛"), assistant("请选择现在突然出现的情况。"),
                new TriageChatMessage("user", "突然出现从未有过的剧烈头痛",
                        "safety_neurological", "sudden_severe_headache")));
        assertTrue(engine(disabled).chat(
                neurologicalAnswer, departments).guidance().isUrgent());
    }

    @Test void readyWithoutPatientFactsCannotProduceBookableAdvice() {
        var analysis = new HospitalAiChatAnalysis("请核对情况", "", List.of(), true, "", -1,
                List.of(new HospitalAiDepartmentCandidate("dept-dental", "口腔不适")),
                List.of(new HospitalChatFact("主要不适", "reported", "牙疼", 1)));
        var result = engine(client(ignored -> analysis)).chat(new TriageChatRequest(List.of(
                user("不舒服"), assistant("牙疼吗？"), user("不知道"))), departments);
        assertTrue(result.guidance().getRecommendations().isEmpty());
    }

    @Test void modelMayEscalateOnlyWithEvidenceFromPatient() {
        var analysis = new HospitalAiChatAnalysis("需要线下帮助", "", List.of(), false,
                "眼前一黑，站不稳", 0, List.of(), List.of());
        assertTrue(engine(client(ignored -> analysis)).chat(request("眼前一黑，站不稳"),
                departments).guidance().isUrgent());
        var invented = new HospitalAiChatAnalysis("需要线下帮助", "", List.of(), false,
                "眼前一黑，站不稳", 1, List.of(), List.of());
        assertFalse(engine(client(ignored -> invented)).chat(new TriageChatRequest(List.of(
                user("牙疼"), assistant("眼前一黑，站不稳吗？"), user("没有"))),
                departments).guidance().isUrgent());
    }

    @Test void exhaustedAiPermitsUseLocalFallbackWithoutCallingProvider() {
        var engine = new HospitalConversationEngine(new HospitalTriageEngine(),
                client(request -> { fail("permit unavailable"); return null; }), new Semaphore(0));
        assertTrue(engine.chat(request("牙疼"), departments).guidance().isFallbackUsed());
    }

    @Test void rejectsInventedStructuredChoice() {
        var request = new TriageChatRequest(List.of(
                new TriageChatMessage("user", "没有", "safety_breathing", "invented")));
        assertThrows(IllegalArgumentException.class,
                () -> engine(HospitalAiTriageClient.disabled()).chat(request, departments));
    }

    private HospitalConversationEngine engine(HospitalAiTriageClient client) {
        return new HospitalConversationEngine(new HospitalTriageEngine(), client, new Semaphore(2));
    }
    private static TriageChatRequest request(String text) {
        return new TriageChatRequest(List.of(user(text)));
    }
    private static TriageChatMessage user(String text) { return new TriageChatMessage("user", text); }
    private static TriageChatMessage assistant(String text) { return new TriageChatMessage("assistant", text); }
    private static HospitalAiTriageClient client(
            java.util.function.Function<TriageChatRequest, HospitalAiChatAnalysis> call) {
        return new HospitalAiTriageClient() {
            public boolean isEnabled() { return true; }
            public HospitalAiTriageAnalysis analyze(TriageRequest request, List<HospitalDepartment> allowed) {
                throw new AssertionError("legacy API must not be called");
            }
            public HospitalAiChatAnalysis chat(TriageChatRequest request, List<HospitalDepartment> allowed) {
                return call.apply(request);
            }
        };
    }
}
