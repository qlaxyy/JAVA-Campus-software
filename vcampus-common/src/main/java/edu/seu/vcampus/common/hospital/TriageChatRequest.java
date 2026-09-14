package edu.seu.vcampus.common.hospital;

import java.io.Serializable;
import java.util.List;

public record TriageChatRequest(List<TriageChatMessage> messages) implements Serializable {
    public static final int MAX_MESSAGES = 40;
    public TriageChatRequest {
        messages = List.copyOf(messages);
        if (messages.isEmpty() || messages.size() > MAX_MESSAGES
                || !"user".equals(messages.getLast().role())) {
            throw new IllegalArgumentException("invalid conversation");
        }
        for (int i = 0; i < messages.size(); i++) {
            if (!(i % 2 == 0 ? "user" : "assistant").equals(messages.get(i).role())) {
                throw new IllegalArgumentException("conversation must alternate");
            }
        }
    }
}
