package edu.seu.vcampus.common.hospital;

import java.io.Serializable;

/** Transcript data, never an instruction to the model or a signed clinical record. */
public record TriageChatMessage(String role, String text, String questionId,
        String answerId) implements Serializable {
    public TriageChatMessage(String role, String text) {
        this(role, text, null, null);
    }
    public TriageChatMessage {
        if (!"user".equals(role) && !"assistant".equals(role)) {
            throw new IllegalArgumentException("invalid chat role");
        }
        text = text == null ? "" : text.trim();
        if (text.isEmpty() || text.length() > ("user".equals(role) ? 300 : 1800)) {
            throw new IllegalArgumentException("invalid chat message length");
        }
        if ((questionId == null) != (answerId == null)
                || (questionId != null && (!"user".equals(role)
                || questionId.length() > 64 || answerId.length() > 64))) {
            throw new IllegalArgumentException("invalid chat choice");
        }
    }
}
