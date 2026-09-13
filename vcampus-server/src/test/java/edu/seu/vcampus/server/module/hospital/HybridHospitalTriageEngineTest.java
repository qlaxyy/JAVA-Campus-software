package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.TriageFollowUpAnswer;
import edu.seu.vcampus.common.hospital.TriageRequest;
import edu.seu.vcampus.common.hospital.TriageResultView;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HybridHospitalTriageEngineTest {

    private final List<HospitalDepartment> departments = List.of(
            department("dept-general", "全科门诊", true),
            department("dept-respiratory", "呼吸内科", true),
            department("dept-hidden", "停用科室", false));

    @Test
    void asksOneQuestionAtATimeForUpToFourRoundsAndCanThenRecommend() {
        HospitalAiTriageClient client = client((request, ignored) -> {
            int answered = request.getFollowUpAnswers().size();
            if (answered < TriageRequest.MAX_FOLLOW_UPS) {
                return new HospitalAiTriageAnalysis(
                        "第" + (answered + 1) + "个补充问题？（请简要回答）", List.of());
            }
            return new HospitalAiTriageAnalysis(null, List.of(
                    new HospitalAiDepartmentCandidate(
                            "dept-respiratory", "综合四次补充信息后更符合呼吸系统分诊范围")));
        });
        HybridHospitalTriageEngine engine = engine(client);
        List<TriageFollowUpAnswer> answers = new ArrayList<>();

        for (int answered = 0; answered < TriageRequest.MAX_FOLLOW_UPS; answered++) {
            var result = engine.triage(new TriageRequest("身体不舒服", false, answers),
                    departments);
            assertTrue(result.needsFollowUp());
            assertEquals("第" + (answered + 1) + "个补充问题？（请简要回答）",
                    result.getFollowUpQuestion());
            answers.add(new TriageFollowUpAnswer(
                    result.getFollowUpQuestion(), "补充回答" + answered));
        }

        var finalResult = engine.triage(
                new TriageRequest("身体不舒服", false, answers), departments);
        assertFalse(finalResult.needsFollowUp());
        assertEquals("dept-respiratory",
                finalResult.getRecommendations().getFirst().getDepartmentId());
        assertTrue(finalResult.isAiAssisted());
    }

    @Test
    void refusesAFifthQuestionAndUsesValidatedCandidate() {
        HospitalAiTriageClient client = client((request, ignored) ->
                new HospitalAiTriageAnalysis("不应展示的第五个问题", List.of(
                        new HospitalAiDepartmentCandidate(
                                "dept-respiratory", "已有信息指向呼吸系统"))));
        List<TriageFollowUpAnswer> answers = List.of(
                answer(1), answer(2), answer(3), answer(4));

        var result = engine(client).triage(
                new TriageRequest("身体不舒服", false, answers), departments);

        assertFalse(result.needsFollowUp());
        assertEquals("dept-respiratory",
                result.getRecommendations().getFirst().getDepartmentId());
    }

    @Test
    void localUrgentGateAlsoChecksFollowUpAnswersAndBypassesTheModel() {
        AtomicInteger calls = new AtomicInteger();
        HospitalAiTriageClient client = client((request, ignored) -> {
            calls.incrementAndGet();
            return new HospitalAiTriageAnalysis(null, List.of());
        });

        var result = engine(client).triage(new TriageRequest(
                "胸口不舒服", false,
                List.of(new TriageFollowUpAnswer("具体是什么感觉？", "持续胸痛"))),
                departments);

        assertTrue(result.isUrgent());
        assertEquals(0, calls.get());

        var confirmedByAnswer = engine(client).triage(new TriageRequest(
                "头晕", false,
                List.of(new TriageFollowUpAnswer("有没有呼吸困难？", "有"))),
                departments);
        assertTrue(confirmedByAnswer.isUrgent());
        assertEquals(0, calls.get());
    }

    @Test
    void filtersUnknownOrInactiveDepartmentsAndFallsBackToLocalRules() {
        HospitalAiTriageClient client = client((request, ignored) ->
                new HospitalAiTriageAnalysis(null, List.of(
                        new HospitalAiDepartmentCandidate("dept-made-up", "不存在"),
                        new HospitalAiDepartmentCandidate("dept-hidden", "已经停用"))));

        var result = engine(client).triage(
                new TriageRequest("咳嗽发热", false,
                        safeRespiratoryAnswers()), departments);

        assertEquals("dept-respiratory",
                result.getRecommendations().getFirst().getDepartmentId());
        assertTrue(result.isFallbackUsed());
        assertFalse(result.isAiAssisted());
    }

    @Test
    void modelFailureDoesNotBreakExistingRuleGuidance() {
        HospitalAiTriageClient client = new HospitalAiTriageClient() {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public HospitalAiTriageAnalysis analyze(
                    TriageRequest request,
                    List<HospitalDepartment> bookableDepartments)
                    throws HospitalAiTriageException {
                throw new HospitalAiTriageException("timeout");
            }
        };

        var result = engine(client).triage(
                new TriageRequest("咳嗽发热", false,
                        safeRespiratoryAnswers()), departments);

        assertEquals("dept-respiratory",
                result.getRecommendations().getFirst().getDepartmentId());
        assertTrue(result.isFallbackUsed());
    }

    @Test
    void asksFixedBreathingQuestionForCoughBeforeCallingTheModel() {
        AtomicInteger calls = new AtomicInteger();
        HospitalAiTriageClient client = client((request, ignored) -> {
            calls.incrementAndGet();
            return new HospitalAiTriageAnalysis(null, List.of());
        });

        var result = engine(client).triage(
                new TriageRequest("只是咳嗽", false), departments);

        assertTrue(result.needsFollowUp());
        assertTrue(result.hasStructuredFollowUp());
        assertEquals("safety_breathing", result.getFollowUpQuestionId());
        assertEquals(0, calls.get());
    }

    @Test
    void rejectsModelGeneratedSafetyOrProfessionalQuestions() {
        HospitalAiTriageClient client = client((request, ignored) ->
                new HospitalAiTriageAnalysis("是否伴有呼吸困难或紫绀？", List.of()));

        var result = engine(client).triage(
                new TriageRequest("身体不舒服", false), departments);

        assertFalse(result.needsFollowUp());
        assertEquals("dept-general",
                result.getRecommendations().getFirst().getDepartmentId());
        assertTrue(result.isFallbackUsed());
    }

    @Test
    void rejectsModelQuestionThatMixesSeveralSymptomsWithASevereSign() {
        HospitalAiTriageClient client = client((request, ignored) ->
                new HospitalAiTriageAnalysis(
                        "有没有咳嗽、咳痰或喘不过气？", List.of()));

        var result = engine(client).triage(
                new TriageRequest("身体不舒服", false), departments);

        assertFalse(result.needsFollowUp());
        assertEquals("dept-general",
                result.getRecommendations().getFirst().getDepartmentId());
        assertTrue(result.isFallbackUsed());
    }

    @Test
    void fallsBackImmediatelyWhenTwoModelRequestsAreAlreadyRunning() throws Exception {
        CountDownLatch twoRequestsEnteredModel = new CountDownLatch(2);
        CountDownLatch releaseModel = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        HospitalAiTriageClient client = client((request, ignored) -> {
            calls.incrementAndGet();
            twoRequestsEnteredModel.countDown();
            try {
                if (!releaseModel.await(2, TimeUnit.SECONDS)) {
                    throw new HospitalAiTriageException("test model wait timed out");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new HospitalAiTriageException("test model wait interrupted");
            }
            return new HospitalAiTriageAnalysis(null, List.of(
                    new HospitalAiDepartmentCandidate(
                            "dept-general", "信息适合先由全科进行判断")));
        });
        HybridHospitalTriageEngine engine = engine(client);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            Future<TriageResultView> first = executor.submit(() -> engine.triage(
                    new TriageRequest("身体不舒服", false), departments));
            Future<TriageResultView> second = executor.submit(() -> engine.triage(
                    new TriageRequest("身体不舒服", false), departments));
            assertTrue(twoRequestsEnteredModel.await(1, TimeUnit.SECONDS));

            Future<TriageResultView> third = executor.submit(() -> engine.triage(
                    new TriageRequest("身体不舒服", false), departments));
            TriageResultView overloaded = third.get(500, TimeUnit.MILLISECONDS);

            assertTrue(overloaded.isFallbackUsed());
            assertFalse(overloaded.isAiAssisted());
            assertEquals(2, calls.get());
            releaseModel.countDown();
            assertTrue(first.get(1, TimeUnit.SECONDS).isAiAssisted());
            assertTrue(second.get(1, TimeUnit.SECONDS).isAiAssisted());
        } finally {
            releaseModel.countDown();
            executor.shutdownNow();
        }
    }

    private static HybridHospitalTriageEngine engine(HospitalAiTriageClient client) {
        return new HybridHospitalTriageEngine(new HospitalTriageEngine(), client);
    }

    private static HospitalAiTriageClient client(AnalysisFunction function) {
        return new HospitalAiTriageClient() {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public HospitalAiTriageAnalysis analyze(
                    TriageRequest request,
                    List<HospitalDepartment> bookableDepartments)
                    throws HospitalAiTriageException {
                return function.apply(request, bookableDepartments);
            }
        };
    }

    private static TriageFollowUpAnswer answer(int number) {
        return new TriageFollowUpAnswer("问题" + number, "回答" + number);
    }

    private static List<TriageFollowUpAnswer> safeRespiratoryAnswers() {
        return List.of(
                new TriageFollowUpAnswer(
                        "safety_breathing", "你现在呼吸是否明显费力？",
                        "normal", "没有，能够正常说话"),
                new TriageFollowUpAnswer(
                        "safety_temperature", "你最近一次测得的体温在哪个范围？",
                        "below_385", "低于 38.5℃"));
    }

    private static HospitalDepartment department(String id, String name, boolean active) {
        return new HospitalDepartment(id, name, null, true, active);
    }

    @FunctionalInterface
    private interface AnalysisFunction {
        HospitalAiTriageAnalysis apply(
                TriageRequest request,
                List<HospitalDepartment> departments) throws HospitalAiTriageException;
    }
}
