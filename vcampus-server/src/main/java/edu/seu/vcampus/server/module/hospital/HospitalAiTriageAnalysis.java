package edu.seu.vcampus.server.module.hospital;

import java.util.List;

/** Untrusted model output. The hybrid engine validates every department against the repository. */
record HospitalAiTriageAnalysis(
        String followUpQuestion,
        List<HospitalAiDepartmentCandidate> candidates) {

    HospitalAiTriageAnalysis {
        followUpQuestion = normalize(followUpQuestion);
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    private static String normalize(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}

record HospitalAiDepartmentCandidate(String departmentId, String reason) {

    HospitalAiDepartmentCandidate {
        departmentId = normalize(departmentId);
        reason = normalize(reason);
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim();
    }
}
