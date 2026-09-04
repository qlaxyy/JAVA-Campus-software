package edu.seu.vcampus.common.hospital;

import edu.seu.vcampus.common.protocol.ActionNames;
import edu.seu.vcampus.common.protocol.ModuleNames;

/** Public actions owned by the hospital module. */
public final class HospitalActions {

    public static final String GET_MODE_ACCESS =
            ActionNames.of(ModuleNames.HOSPITAL, "GET_MODE_ACCESS");
    public static final String LIST_DEPARTMENTS =
            ActionNames.of(ModuleNames.HOSPITAL, "LIST_DEPARTMENTS");
    public static final String SEARCH_SLOTS =
            ActionNames.of(ModuleNames.HOSPITAL, "SEARCH_SLOTS");
    public static final String SUBMIT_DOCTOR_APPLICATION =
            ActionNames.of(ModuleNames.HOSPITAL, "SUBMIT_DOCTOR_APPLICATION");
    public static final String LIST_DOCTOR_APPLICATIONS =
            ActionNames.of(ModuleNames.HOSPITAL, "LIST_DOCTOR_APPLICATIONS");
    public static final String REVIEW_DOCTOR_APPLICATION =
            ActionNames.of(ModuleNames.HOSPITAL, "REVIEW_DOCTOR_APPLICATION");

    private HospitalActions() {
    }
}
