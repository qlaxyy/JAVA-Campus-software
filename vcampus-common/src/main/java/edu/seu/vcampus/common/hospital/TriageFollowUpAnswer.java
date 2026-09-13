package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;

/** One answered clarification in the current, non-persistent triage conversation. */
public final class TriageFollowUpAnswer implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String question;
    private final String answer;
    private final String questionId;
    private final String answerId;

    public TriageFollowUpAnswer(String question, String answer) {
        this(null, question, null, answer);
    }

    public TriageFollowUpAnswer(
            String questionId,
            String question,
            String answerId,
            String answer) {
        this.questionId = optionalId(questionId, "questionId");
        this.question = requireText(question, 200, "question");
        this.answerId = optionalId(answerId, "answerId");
        this.answer = requireText(answer, 300, "answer");
        if ((this.questionId == null) != (this.answerId == null)) {
            throw new IllegalArgumentException(
                    "questionId and answerId must either both be present or both be absent");
        }
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getAnswerId() {
        return answerId;
    }

    public boolean hasStructuredAnswer() {
        return questionId != null;
    }

    private static String optionalId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireText(value, 64, fieldName);
    }

    private static String requireText(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }
}
