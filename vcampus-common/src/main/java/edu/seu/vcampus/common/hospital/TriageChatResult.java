package edu.seu.vcampus.common.hospital;

import java.io.Serializable;
import java.util.List;

public record TriageChatResult(String reply, String summary, List<String> missingInformation,
        TriageResultView guidance) implements Serializable {
    public TriageChatResult {
        missingInformation = List.copyOf(missingInformation);
    }
}
