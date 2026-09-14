package edu.seu.vcampus.server.module.hospital;

import java.util.List;

/** Untrusted model response. Evidence indexes refer only to patient turns. */
record HospitalAiChatAnalysis(String reply, String summary, List<String> missingInformation,
        boolean ready, String urgentEvidence, int urgentMessageIndex,
        List<HospitalAiDepartmentCandidate> candidates, List<HospitalChatFact> facts) { }

record HospitalChatFact(String topic, String state, String evidence, int messageIndex) { }
