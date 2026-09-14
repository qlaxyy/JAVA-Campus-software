package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.*;
import java.util.*;
import java.util.concurrent.Semaphore;

/** Full transcript dialogue; local screening runs even when the model is unavailable. */
final class HospitalConversationEngine {
    private final HospitalTriageEngine local;
    private final HospitalAiTriageClient ai;
    private final Semaphore permits;
    private final HospitalSafetyQuestionCatalog safety = new HospitalSafetyQuestionCatalog();

    HospitalConversationEngine(HospitalTriageEngine local, HospitalAiTriageClient ai,
            Semaphore permits) {
        this.local = local;
        this.ai = ai;
        this.permits = permits;
    }

    TriageChatResult chat(TriageChatRequest request, List<HospitalDepartment> departments) {
        // Rebuild at the trust boundary; transcript is advice input, never clinical authority.
        request = new TriageChatRequest(request.messages());
        List<HospitalDepartment> bookable = departments.stream()
                .filter(HospitalDepartment::active).filter(HospitalDepartment::bookable).toList();
        List<TriageFollowUpAnswer> choices = new ArrayList<>();
        String previousQuestion = "";
        for (TriageChatMessage message : request.messages()) {
            if ("assistant".equals(message.role())) {
                previousQuestion = message.text();
                continue;
            }
            if (message.questionId() == null) {
                safety.explicitChatAnswer(previousQuestion, message.text()).ifPresent(choice -> {
                    choices.removeIf(previous -> previous.getQuestionId().equals(choice.getQuestionId()));
                    choices.add(choice);
                });
            }
            if (message.questionId() != null) {
                var choice = new TriageFollowUpAnswer(message.questionId(), "安全确认",
                        message.answerId(), message.text());
                if (!safety.isKnownAnswer(choice)) {
                    throw new IllegalArgumentException("invalid safety choice");
                }
                choices.removeIf(previous -> previous.getQuestionId().equals(message.questionId()));
                choices.add(choice);
            }
        }
        TriageResultView fallback = null;
        HospitalSafetyQuestionCatalog.SafetyQuestion pending = null;
        String previousReply = "";
        for (TriageChatMessage message : request.messages()) {
            if ("assistant".equals(message.role())) {
                previousReply = message.text();
                continue;
            }
            boolean exactChoice = message.questionId() != null
                    || safety.explicitChatAnswer(previousReply, message.text()).isPresent();
            List<TriageFollowUpAnswer> answers = new ArrayList<>(choices);
            // Numeric answers must keep the temperature question they answer.
            if (message.questionId() == null && !previousReply.isEmpty()
                    && !exactChoice && !isExplanation(message.text())) {
                answers.add(new TriageFollowUpAnswer(
                        lastSingleQuestion(previousReply), message.text()));
            }
            String clinicalText = clinicalText(message.text());
            TriageRequest projected = new TriageRequest(exactChoice ? "安全确认"
                    : clinicalText.length() < 2 ? clinicalText + "。" : clinicalText, false, answers);
            TriageResultView screened = local.triage(projected, bookable);
            if (screened.isUrgent()) {
                return urgent(screened.getSafetyMessage());
            }
            if (fallback == null || screened.getRecommendations().stream()
                    .anyMatch(item -> item.getMatchLevel() != TriageMatchLevel.GENERAL)) {
                fallback = screened;
            }
            if (pending == null) {
                pending = safety.nextQuestion(projected).orElse(null);
            }
        }
        String latestText = request.messages().getLast().text();
        boolean explicitCorrection = List.of("说错", "写错", "更正", "不是").stream()
                .anyMatch(latestText::contains);
        if (pending != null && !isExplanation(latestText) && !explicitCorrection) {
            String reply = "为避免遗漏需要及时处理的情况，请先确认下面这一项。";
            TriageResultView guidance = new TriageResultView(false, reply, List.of(),
                    pending.questionId(), pending.prompt(), pending.options(), 0,
                    false, false);
            return new TriageChatResult(
                    reply, patientSummary(request), List.of(), guidance);
        }
        HospitalAiChatAnalysis analysis = null;
        boolean unavailable = true;
        if (ai.isEnabled() && permits.tryAcquire()) {
            try {
                analysis = ai.chat(request, bookable);
                unavailable = false;
            } catch (HospitalAiTriageException | RuntimeException exception) {
                // Patient input remains available; a provider error never implies emergency.
            } finally {
                permits.release();
            }
        }
        if (analysis != null && groundedUrgency(analysis, request)) {
            return urgent("你提到“" + analysis.urgentEvidence()
                    + "”，这可能需要及时线下评估。请先联系线下医护人员；情况危急时联系当地急救服务。");
        }
        final TriageChatRequest transcript = request;
        List<HospitalChatFact> facts = analysis == null ? List.of() : analysis.facts().stream()
                .filter(fact -> grounded(fact.evidence(), fact.messageIndex(), transcript))
                .filter(fact -> List.of("主要不适", "持续时间", "变化", "伴随表现", "危险表现")
                        .contains(fact.topic()))
                .filter(fact -> List.of("reported", "denied").contains(fact.state())).toList();
        if (explicitCorrection && facts.stream().anyMatch(fact ->
                "主要不适".equals(fact.topic()) && "reported".equals(fact.state())
                        && fact.messageIndex() == transcript.messages().size() - 1)) {
            // Explicitly corrected ordinary complaints use the current evidence for follow-ups.
            // Earlier deterministic red flags have already stopped above and cannot be erased by AI.
            pending = null;
            for (HospitalChatFact fact : facts) {
                if (!"reported".equals(fact.state())) continue;
                String evidence = fact.evidence().length() < 2 ? fact.evidence() + "。" : fact.evidence();
                pending = safety.nextQuestion(new TriageRequest(evidence, false, choices)).orElse(null);
                if (pending != null) break;
            }
        }
        String summary = facts.isEmpty() ? patientSummary(request)
                : String.join("\n", facts.stream().map(fact -> fact.topic() + "："
                        + ("denied".equals(fact.state()) ? "明确否认 · " : "")
                        + "“" + fact.evidence() + "”").toList());
        List<String> missing = analysis == null ? List.of()
                : analysis.missingInformation().stream().filter(HospitalConversationEngine::safe).toList();
        if (pending != null) {
            String reply = analysis != null && safe(analysis.reply())
                    && !analysis.reply().contains("？") && !analysis.reply().contains("?")
                    ? analysis.reply() + "\n\n" : "";
            if (analysis != null && safe(analysis.reply())
                    && isExplanation(request.messages().getLast().text())) {
                // Preserve the explanation, but let the server ask the single safety question.
                int period = analysis.reply().lastIndexOf('。');
                if (period >= 0) reply = analysis.reply().substring(0, period + 1) + "\n\n";
            }
            reply += "为避免误解，请确认下面这项情况；也可以继续补充或询问选项的含义。";
            TriageResultView guidance = new TriageResultView(false, reply, List.of(),
                    pending.questionId(), pending.prompt(), pending.options(), 0,
                    analysis != null, unavailable);
            return new TriageChatResult(reply, summary, missing, guidance);
        }
        if (analysis != null && analysis.ready()) {
            List<HospitalPatientStatementCoverage.PatientStatement> uncovered =
                    HospitalPatientStatementCoverage.uncovered(
                            HospitalPatientStatementCoverage.extract(request), facts);
            if (!uncovered.isEmpty()) {
                String statement = uncovered.getFirst().text();
                List<String> coverageMissing = new ArrayList<>();
                coverageMissing.add("请确认是否仍有“" + statement + "”");
                missing.stream().filter(item -> !coverageMissing.contains(item))
                        .limit(5).forEach(coverageMissing::add);
                String reply = "我还没有完整记录你提到的“" + statement
                        + "”。这项现在仍然存在吗？如果是之前写错了，请修改那条消息。";
                return new TriageChatResult(reply, summary, coverageMissing,
                        new TriageResultView(false, "请先补充确认", List.of(),
                                null, 0, true, false));
            }
        }
        if (analysis == null || !safe(analysis.reply())) {
            return new TriageChatResult(
                    "对话助手暂时无法回答。以下仅为根据原话匹配的科室线索，请核对；"
                    + "若与你的情况不符，请咨询线下导诊。你也可以稍后再试。",
                    patientSummary(request), List.of("尚未完成对话核实"),
                    new TriageResultView(false, "本地科室参考", fallback.getRecommendations(),
                            null, 0, false, true));
        }
        if (!analysis.ready() || !missing.isEmpty()
                || facts.stream().noneMatch(fact -> "主要不适".equals(fact.topic()))) {
            String reply = analysis.ready() && missing.isEmpty()
                    ? "我还不能根据这些信息确定就医方向。你现在最主要的不适是什么？"
                    : analysis.reply();
            return new TriageChatResult(reply, summary, missing,
                    new TriageResultView(false, "请继续补充情况", List.of(), null, 0, true, false));
        }
        List<TriageRecommendationView> recommendations =
                HybridHospitalTriageEngine.validatedRecommendations(
                        analysis.candidates(), bookable, List.of());
        String reply = recommendations.isEmpty()
                ? "当前可挂号科室中没有通过核对的匹配项，请咨询线下导诊，或继续补充情况。"
                : analysis.reply();
        return new TriageChatResult(reply, summary, missing,
                new TriageResultView(false, "请核对本次描述后查看科室", recommendations,
                        null, 0, true, false));
    }

    private static boolean groundedUrgency(HospitalAiChatAnalysis analysis, TriageChatRequest request) {
        int index = analysis.urgentMessageIndex();
        if (!grounded(analysis.urgentEvidence(), index, request)) return false;
        String original = request.messages().get(index).text();
        if (!clinicalText(original).contains(analysis.urgentEvidence())) return false;
        int position = original.indexOf(analysis.urgentEvidence());
        String prefix = original.substring(Math.max(0, position - 6), position);
        return List.of("没有", "不是", "否认", "无", "没", "假如", "如果").stream()
                .noneMatch(prefix::endsWith)
                && !analysis.urgentEvidence().matches("^(没有|不是|否认|假如|如果).*");
    }

    private static boolean grounded(String evidence, int index, TriageChatRequest request) {
        return evidence != null && !evidence.isBlank() && evidence.length() <= 300
                && index >= 0 && index < request.messages().size()
                && "user".equals(request.messages().get(index).role())
                && request.messages().get(index).text().contains(evidence);
    }

    private static boolean isExplanation(String text) {
        return (text.contains("什么意思") || text.startsWith("什么叫"))
                && !text.contains("我现在") && !text.contains("我有");
    }

    private static String clinicalText(String text) {
        // Remove only a terminology question, never other symptoms in the same message.
        String remaining = String.join("，", Arrays.stream(text.split("[，。；！？?\\n]"))
                .filter(clause -> !isExplanation(clause)).toList()).trim();
        return remaining.isEmpty() ? "询问词语含义" : remaining;
    }

    private static String lastSingleQuestion(String reply) {
        String last = reply.substring(reply.lastIndexOf('\n') + 1).trim();
        if (last.length() > 200 || last.contains("、") || last.contains("或")
                || last.chars().filter(c -> c == '？' || c == '?').count() != 1) {
            return "补充情况";
        }
        return last;
    }

    private static boolean safe(String value) {
        return value != null && !value.isBlank() && value.length() <= 500
                && List.of("确诊", "你患有", "你得了", "可能是", "考虑为", "建议服用", "建议用药", "无需就医",
                        "没有危险", "排除急症", "治疗方案", "诊断为").stream().noneMatch(value::contains);
    }

    private static String patientSummary(TriageChatRequest request) {
        StringBuilder result = new StringBuilder();
        request.messages().stream().filter(message -> "user".equals(message.role()))
                .forEach(message -> result.append("\n").append(message.text()));
        return result.toString().trim();
    }

    private static TriageChatResult urgent(String message) {
        message += " 如果先前的描述是输入错误，可用该条消息旁的“修改这条”更正，再重新评估。";
        return new TriageChatResult(message, "", List.of(),
                new TriageResultView(true, message, List.of()));
    }
}
