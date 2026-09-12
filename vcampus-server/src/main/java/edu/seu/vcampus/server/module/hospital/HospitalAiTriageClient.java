package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.TriageRequest;

import java.util.List;

/** Server-only seam for natural-language clarification and department extraction. */
interface HospitalAiTriageClient {

    boolean isEnabled();

    HospitalAiTriageAnalysis analyze(
            TriageRequest request,
            List<HospitalDepartment> bookableDepartments) throws HospitalAiTriageException;

    static HospitalAiTriageClient disabled() {
        return new HospitalAiTriageClient() {
            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public HospitalAiTriageAnalysis analyze(
                    TriageRequest request,
                    List<HospitalDepartment> bookableDepartments) {
                throw new IllegalStateException("AI triage is disabled");
            }
        };
    }
}
