package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.TriageChatMessage;
import edu.seu.vcampus.common.hospital.TriageChatRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Ensures a model cannot silently omit a separate patient statement before recommending. */
final class HospitalPatientStatementCoverage {

    private static final Set<String> GENERIC_REPLIES = Set.of(
            "是", "是的", "不是", "有", "没有", "没", "对", "不对", "否",
            "好的", "好", "嗯", "可以", "不可以", "能", "不能", "不知道",
            "不清楚", "不确定");
    private static final List<String> CORRECTION_MARKERS = List.of(
            "说错了", "写错了", "更正为", "应该是", "其实是");

    private HospitalPatientStatementCoverage() {
    }

    static List<PatientStatement> extract(TriageChatRequest request) {
        int effectiveStart = 0;
        for (int index = 0; index < request.messages().size(); index++) {
            TriageChatMessage message = request.messages().get(index);
            if ("user".equals(message.role()) && containsCorrection(message.text())) {
                effectiveStart = index;
            }
        }

        List<PatientStatement> statements = new ArrayList<>();
        HospitalSafetyQuestionCatalog safety = new HospitalSafetyQuestionCatalog();
        String previousAssistantReply = "";
        for (int index = effectiveStart; index < request.messages().size(); index++) {
            TriageChatMessage message = request.messages().get(index);
            if ("assistant".equals(message.role())) {
                previousAssistantReply = message.text();
                continue;
            }
            if (!"user".equals(message.role()) || message.questionId() != null) {
                continue;
            }
            // A typed answer to a server-owned safety question is an answer, not a new symptom.
            if (safety.explicitChatAnswer(previousAssistantReply, message.text()).isPresent()) {
                continue;
            }
            for (String part : message.text().split("[，,。；;！？?!\\n]+")) {
                String statement = clean(part);
                if (statement.length() >= 2 && !isMetaOrGeneric(statement)) {
                    statements.add(new PatientStatement(index, statement));
                }
            }
        }
        return List.copyOf(statements);
    }

    static List<PatientStatement> uncovered(
            List<PatientStatement> statements,
            List<HospitalChatFact> groundedFacts) {
        return statements.stream().filter(statement -> groundedFacts.stream()
                        .filter(fact -> fact.messageIndex() == statement.messageIndex())
                        .noneMatch(fact -> overlaps(statement.text(), fact.evidence())))
                .limit(3)
                .toList();
    }

    private static boolean overlaps(String statement, String evidence) {
        String normalizedStatement = normalize(statement);
        String normalizedEvidence = normalize(evidence);
        return normalizedEvidence.length() >= 2
                && (normalizedStatement.contains(normalizedEvidence)
                || normalizedEvidence.contains(normalizedStatement));
    }

    private static String clean(String value) {
        String cleaned = value.trim();
        for (String marker : CORRECTION_MARKERS) {
            int markerIndex = cleaned.indexOf(marker);
            if (markerIndex >= 0) {
                cleaned = cleaned.substring(markerIndex + marker.length())
                        .replaceFirst("^[是为：:，,\\s]+", "")
                        .trim();
                if (cleaned.isEmpty()) {
                    return "";
                }
                break;
            }
        }
        for (String prefix : List.of("另外", "还有", "同时", "并且", "而且")) {
            if (cleaned.startsWith(prefix)) {
                cleaned = cleaned.substring(prefix.length()).trim();
                break;
            }
        }
        return cleaned;
    }

    private static boolean isMetaOrGeneric(String value) {
        String normalized = normalize(value);
        return GENERIC_REPLIES.contains(normalized)
                || normalized.contains("什么意思")
                || normalized.startsWith("什么叫")
                || CORRECTION_MARKERS.stream().anyMatch(normalized::equals);
    }

    private static boolean containsCorrection(String value) {
        return CORRECTION_MARKERS.stream().anyMatch(value::contains);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s，,。；;！？?!、]", "");
    }

    record PatientStatement(int messageIndex, String text) {
    }
}
