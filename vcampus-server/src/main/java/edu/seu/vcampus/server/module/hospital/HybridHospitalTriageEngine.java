package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.TriageMatchLevel;
import edu.seu.vcampus.common.hospital.TriageRecommendationView;
import edu.seu.vcampus.common.hospital.TriageRequest;
import edu.seu.vcampus.common.hospital.TriageResultView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;

/** Keeps safety and repository constraints deterministic while using AI for dialogue. */
final class HybridHospitalTriageEngine {

    private static final int MAX_CONCURRENT_AI_REQUESTS = 2;
    private static final List<String> PROHIBITED_MEDICAL_CLAIMS = List.of(
            "诊断为", "确诊为", "你患有", "你得了", "可能是", "考虑为",
            "建议服用", "应该服用", "建议用药", "治疗方案", "处方为");
    private static final List<String> SERVER_OWNED_SAFETY_TERMS = List.of(
            "呼吸困难", "无法呼吸", "持续胸痛", "剧烈胸痛", "意识不清",
            "昏迷", "抽搐", "嘴歪", "言语不清", "单侧肢体", "大量出血",
            "止不住血", "舌头肿胀", "喉咙肿胀", "剧烈腹痛", "自杀", "中毒");
    private static final List<String> PATIENT_UNFRIENDLY_TERMS = List.of(
            "咯血", "紫绀", "偏瘫", "呼吸窘迫", "心前区", "放射痛", "黑便", "吞咽障碍");
    private final HospitalTriageEngine localEngine;
    private final HospitalAiTriageClient aiClient;
    private final HospitalSafetyQuestionCatalog safetyQuestions =
            new HospitalSafetyQuestionCatalog();
    private final Semaphore aiPermits = new Semaphore(MAX_CONCURRENT_AI_REQUESTS, true);

    HybridHospitalTriageEngine(
            HospitalTriageEngine localEngine,
            HospitalAiTriageClient aiClient) {
        this.localEngine = localEngine;
        this.aiClient = aiClient;
    }

    TriageResultView triage(
            TriageRequest request,
            List<HospitalDepartment> activeDepartments) {
        validateStructuredAnswers(request);
        TriageResultView local = localEngine.triage(request, activeDepartments);
        int answered = request.getFollowUpAnswers().size();
        long aiAnswered = request.getFollowUpAnswers().stream()
                .filter(answer -> !answer.hasStructuredAnswer())
                .count();
        if (local.isUrgent()) {
            return withMetadata(local, answered, false, false);
        }
        Optional<HospitalSafetyQuestionCatalog.SafetyQuestion> safetyQuestion =
                safetyQuestions.nextQuestion(request);
        if (safetyQuestion.isPresent()) {
            HospitalSafetyQuestionCatalog.SafetyQuestion question =
                    safetyQuestion.orElseThrow();
            return new TriageResultView(
                    false,
                    "请根据现在能观察到的情况选择最符合的一项。",
                    List.of(),
                    question.questionId(),
                    question.prompt(),
                    question.options(),
                    answered,
                    false,
                    false);
        }
        if (!aiClient.isEnabled()) {
            return withMetadata(local, answered, false, false);
        }

        List<HospitalDepartment> bookable = activeDepartments.stream()
                .filter(HospitalDepartment::active)
                .filter(HospitalDepartment::bookable)
                .toList();
        if (!aiPermits.tryAcquire()) {
            return withMetadata(local, answered, false, true);
        }
        try {
            HospitalAiTriageAnalysis analysis = aiClient.analyze(request, bookable);
            if (isSafeQuestion(analysis.followUpQuestion())
                    && aiAnswered < TriageRequest.MAX_FOLLOW_UPS) {
                return new TriageResultView(
                        false,
                        "为了更准确地区分可挂号科室，请再补充一个关键信息。",
                        List.of(),
                        analysis.followUpQuestion(),
                        answered,
                        true,
                        false);
            }
            if (!hasValidCandidate(analysis.candidates(), bookable)) {
                return withMetadata(local, answered, false, true);
            }
            List<TriageRecommendationView> recommendations = validatedRecommendations(
                    analysis.candidates(), bookable, local.getRecommendations());
            if (recommendations.isEmpty()) {
                return withMetadata(local, answered, false, true);
            }
            return new TriageResultView(
                    false,
                    "以下建议仅用于选择就诊科室，不构成疾病诊断或治疗意见。",
                    recommendations,
                    null,
                    answered,
                    true,
                    false);
        } catch (HospitalAiTriageException | RuntimeException exception) {
            return withMetadata(local, answered, false, true);
        } finally {
            aiPermits.release();
        }
    }

    private static List<TriageRecommendationView> validatedRecommendations(
            List<HospitalAiDepartmentCandidate> candidates,
            List<HospitalDepartment> bookable,
            List<TriageRecommendationView> local) {
        Map<String, HospitalDepartment> allowed = new LinkedHashMap<>();
        for (HospitalDepartment department : bookable) {
            allowed.put(department.departmentId(), department);
        }
        Map<String, TriageRecommendationView> results = new LinkedHashMap<>();
        for (HospitalAiDepartmentCandidate candidate : candidates) {
            HospitalDepartment department = allowed.get(candidate.departmentId());
            if (department == null || !isSafeExplanation(candidate.reason())) {
                continue;
            }
            results.putIfAbsent(department.departmentId(), new TriageRecommendationView(
                    department.departmentId(),
                    department.departmentName(),
                    TriageMatchLevel.MEDIUM,
                    List.of(limit(candidate.reason(), 220))));
            if (results.size() == 3) {
                break;
            }
        }
        for (TriageRecommendationView recommendation : local) {
            if (results.size() == 3) {
                break;
            }
            if (allowed.containsKey(recommendation.getDepartmentId())) {
                results.putIfAbsent(recommendation.getDepartmentId(), recommendation);
            }
        }
        return new ArrayList<>(results.values());
    }

    private static boolean hasValidCandidate(
            List<HospitalAiDepartmentCandidate> candidates,
            List<HospitalDepartment> bookable) {
        return candidates.stream().anyMatch(candidate -> !candidate.reason().isBlank()
                && isSafeExplanation(candidate.reason())
                && bookable.stream().anyMatch(department -> department.departmentId()
                        .equals(candidate.departmentId())));
    }

    private static boolean isSafeQuestion(String question) {
        return question != null
                && question.length() <= 60
                && (question.contains("？") || question.contains("?"))
                && !question.contains("、")
                && !question.contains("以及")
                && PROHIBITED_MEDICAL_CLAIMS.stream().noneMatch(question::contains)
                && SERVER_OWNED_SAFETY_TERMS.stream().noneMatch(question::contains)
                && PATIENT_UNFRIENDLY_TERMS.stream().noneMatch(question::contains);
    }

    private void validateStructuredAnswers(TriageRequest request) {
        boolean invalid = request.getFollowUpAnswers().stream()
                .filter(answer -> answer.hasStructuredAnswer())
                .anyMatch(answer -> !safetyQuestions.isKnownAnswer(answer));
        if (invalid) {
            throw new IllegalArgumentException("structured triage answer is invalid");
        }
    }

    private static boolean isSafeExplanation(String explanation) {
        return explanation != null
                && !explanation.isBlank()
                && PROHIBITED_MEDICAL_CLAIMS.stream().noneMatch(explanation::contains);
    }

    private static TriageResultView withMetadata(
            TriageResultView result,
            int answered,
            boolean aiAssisted,
            boolean fallbackUsed) {
        String message = fallbackUsed
                ? "以下建议仅用于选择就诊科室，不构成疾病诊断或治疗意见。"
                : result.getSafetyMessage();
        return new TriageResultView(
                result.isUrgent(),
                message,
                result.getRecommendations(),
                null,
                answered,
                aiAssisted,
                fallbackUsed);
    }

    private static String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
