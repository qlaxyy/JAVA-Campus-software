package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Safe triage outcome containing either an urgent warning or department guidance. */
public final class TriageResultView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean urgent;
    private final String safetyMessage;
    private final List<TriageRecommendationView> recommendations;
    private final String followUpQuestionId;
    private final String followUpQuestion;
    private final List<TriageFollowUpOptionView> followUpOptions;
    private final int completedFollowUps;
    private final boolean aiAssisted;
    private final boolean fallbackUsed;

    public TriageResultView(
            boolean urgent,
            String safetyMessage,
            List<TriageRecommendationView> recommendations) {
        this(urgent, safetyMessage, recommendations, null, 0, false, false);
    }

    public TriageResultView(
            boolean urgent,
            String safetyMessage,
            List<TriageRecommendationView> recommendations,
            String followUpQuestion,
            int completedFollowUps,
            boolean aiAssisted,
            boolean fallbackUsed) {
        this(urgent, safetyMessage, recommendations, null, followUpQuestion,
                List.of(), completedFollowUps, aiAssisted, fallbackUsed);
    }

    public TriageResultView(
            boolean urgent,
            String safetyMessage,
            List<TriageRecommendationView> recommendations,
            String followUpQuestionId,
            String followUpQuestion,
            List<TriageFollowUpOptionView> followUpOptions,
            int completedFollowUps,
            boolean aiAssisted,
            boolean fallbackUsed) {
        if (safetyMessage == null || safetyMessage.isBlank()) {
            throw new IllegalArgumentException("safetyMessage must not be blank");
        }
        this.urgent = urgent;
        this.safetyMessage = safetyMessage.trim();
        this.recommendations = List.copyOf(Objects.requireNonNull(
                recommendations, "recommendations must not be null"));
        if (urgent && !this.recommendations.isEmpty()) {
            throw new IllegalArgumentException(
                    "urgent triage result must not include appointment recommendations");
        }
        if (completedFollowUps < 0
                || completedFollowUps > TriageRequest.MAX_FOLLOW_UP_ANSWERS) {
            throw new IllegalArgumentException("completedFollowUps is out of range");
        }
        String normalizedQuestion = followUpQuestion == null
                ? null : followUpQuestion.trim();
        if (normalizedQuestion != null && normalizedQuestion.isEmpty()) {
            normalizedQuestion = null;
        }
        if (normalizedQuestion != null && normalizedQuestion.length() > 200) {
            throw new IllegalArgumentException("followUpQuestion must not exceed 200 characters");
        }
        if (normalizedQuestion != null && (urgent || !this.recommendations.isEmpty())) {
            throw new IllegalArgumentException(
                    "a follow-up result cannot also be urgent or contain recommendations");
        }
        if (normalizedQuestion != null && !aiAssisted) {
            if (followUpQuestionId == null || followUpOptions == null
                    || followUpOptions.isEmpty()) {
                throw new IllegalArgumentException(
                        "a non-AI follow-up question must provide structured choices");
            }
        }
        String normalizedQuestionId = followUpQuestionId == null
                ? null : followUpQuestionId.trim();
        if (normalizedQuestionId != null
                && (normalizedQuestionId.isEmpty() || normalizedQuestionId.length() > 64)) {
            throw new IllegalArgumentException("followUpQuestionId is invalid");
        }
        List<TriageFollowUpOptionView> normalizedOptions = List.copyOf(
                Objects.requireNonNull(followUpOptions,
                        "followUpOptions must not be null"));
        if (!normalizedOptions.isEmpty() && normalizedQuestionId == null) {
            throw new IllegalArgumentException(
                    "structured follow-up choices require a question id");
        }
        if (normalizedQuestion == null
                && (normalizedQuestionId != null || !normalizedOptions.isEmpty())) {
            throw new IllegalArgumentException(
                    "structured follow-up choices require a question");
        }
        this.followUpQuestionId = normalizedQuestionId;
        this.followUpQuestion = normalizedQuestion;
        this.followUpOptions = normalizedOptions;
        this.completedFollowUps = completedFollowUps;
        this.aiAssisted = aiAssisted;
        this.fallbackUsed = fallbackUsed;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public String getSafetyMessage() {
        return safetyMessage;
    }

    public List<TriageRecommendationView> getRecommendations() {
        return recommendations;
    }

    public boolean needsFollowUp() {
        return followUpQuestion != null && !followUpQuestion.isBlank();
    }

    public String getFollowUpQuestion() {
        return followUpQuestion;
    }

    public String getFollowUpQuestionId() {
        return followUpQuestionId;
    }

    public List<TriageFollowUpOptionView> getFollowUpOptions() {
        return followUpOptions;
    }

    public boolean hasStructuredFollowUp() {
        return followUpQuestionId != null && !followUpOptions.isEmpty();
    }

    public int getCompletedFollowUps() {
        return completedFollowUps;
    }

    public boolean isAiAssisted() {
        return aiAssisted;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }
}
