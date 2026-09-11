package edu.seu.vcampus.common.hospital;

import edu.seu.vcampus.common.protocol.ActionNames;
import edu.seu.vcampus.common.protocol.ModuleNames;

/** Public actions owned by the hospital module. */
public final class HospitalActions {

    public static final String GET_MODE_ACCESS =
            ActionNames.of(ModuleNames.HOSPITAL, "GET_MODE_ACCESS");
    public static final String LIST_DEPARTMENTS =
            ActionNames.of(ModuleNames.HOSPITAL, "LIST_DEPARTMENTS");
    public static final String GET_TRIAGE_RECOMMENDATION =
            ActionNames.of(ModuleNames.HOSPITAL, "GET_TRIAGE_RECOMMENDATION");
    public static final String SEARCH_SLOTS =
            ActionNames.of(ModuleNames.HOSPITAL, "SEARCH_SLOTS");
    public static final String SUBMIT_DOCTOR_APPLICATION =
            ActionNames.of(ModuleNames.HOSPITAL, "SUBMIT_DOCTOR_APPLICATION");
    public static final String LIST_DOCTOR_APPLICATIONS =
            ActionNames.of(ModuleNames.HOSPITAL, "LIST_DOCTOR_APPLICATIONS");
    public static final String REVIEW_DOCTOR_APPLICATION =
            ActionNames.of(ModuleNames.HOSPITAL, "REVIEW_DOCTOR_APPLICATION");
    public static final String GET_ADMIN_SCHEDULE_WORKSPACE =
            ActionNames.of(ModuleNames.HOSPITAL, "GET_ADMIN_SCHEDULE_WORKSPACE");
    public static final String CREATE_SCHEDULE =
            ActionNames.of(ModuleNames.HOSPITAL, "CREATE_SCHEDULE");
    public static final String SET_SCHEDULE_PUBLICATION =
            ActionNames.of(ModuleNames.HOSPITAL, "SET_SCHEDULE_PUBLICATION");
    public static final String GET_ADMIN_DEPARTMENT_WORKSPACE =
            ActionNames.of(ModuleNames.HOSPITAL, "GET_ADMIN_DEPARTMENT_WORKSPACE");
    public static final String CREATE_DEPARTMENT =
            ActionNames.of(ModuleNames.HOSPITAL, "CREATE_DEPARTMENT");
    public static final String UPDATE_DEPARTMENT =
            ActionNames.of(ModuleNames.HOSPITAL, "UPDATE_DEPARTMENT");
    public static final String LIST_ADMIN_APPOINTMENTS =
            ActionNames.of(ModuleNames.HOSPITAL, "LIST_ADMIN_APPOINTMENTS");
    public static final String ADMIN_CANCEL_APPOINTMENT =
            ActionNames.of(ModuleNames.HOSPITAL, "ADMIN_CANCEL_APPOINTMENT");
    public static final String LIST_MY_BILLS =
            ActionNames.of(ModuleNames.HOSPITAL, "LIST_MY_BILLS");
    public static final String PAY_BILL =
            ActionNames.of(ModuleNames.HOSPITAL, "PAY_BILL");
    public static final String BOOK_APPOINTMENT =
            ActionNames.of(ModuleNames.HOSPITAL, "BOOK_APPOINTMENT");
    public static final String SEARCH_APPOINTMENTS =
            ActionNames.of(ModuleNames.HOSPITAL, "SEARCH_APPOINTMENTS");
    public static final String CANCEL_APPOINTMENT =
            ActionNames.of(ModuleNames.HOSPITAL, "CANCEL_APPOINTMENT");
    public static final String GET_DOCTOR_WORKSPACE =
            ActionNames.of(ModuleNames.HOSPITAL, "GET_DOCTOR_WORKSPACE");
    public static final String GET_DOCTOR_CONSULTATION_CONTEXT =
            ActionNames.of(ModuleNames.HOSPITAL, "GET_DOCTOR_CONSULTATION_CONTEXT");
    public static final String MARK_APPOINTMENT_NO_SHOW =
            ActionNames.of(ModuleNames.HOSPITAL, "MARK_APPOINTMENT_NO_SHOW");
    public static final String SUBMIT_CONSULTATION =
            ActionNames.of(ModuleNames.HOSPITAL, "SUBMIT_CONSULTATION");
    public static final String LIST_MY_CONSULTATIONS =
            ActionNames.of(ModuleNames.HOSPITAL, "LIST_MY_CONSULTATIONS");
    public static final String GET_MY_HEALTH_RECORD =
            ActionNames.of(ModuleNames.HOSPITAL, "GET_MY_HEALTH_RECORD");
    public static final String UPDATE_MY_HEALTH_PROFILE =
            ActionNames.of(ModuleNames.HOSPITAL, "UPDATE_MY_HEALTH_PROFILE");
    public static final String SUBMIT_EXAMINATION_PLAN =
            ActionNames.of(ModuleNames.HOSPITAL, "SUBMIT_EXAMINATION_PLAN");
    public static final String PUBLISH_DEMO_EXAMINATION_REPORT =
            ActionNames.of(ModuleNames.HOSPITAL, "PUBLISH_DEMO_EXAMINATION_REPORT");
    public static final String BOOK_RESULT_REVIEW =
            ActionNames.of(ModuleNames.HOSPITAL, "BOOK_RESULT_REVIEW");

    private HospitalActions() {
    }
}
