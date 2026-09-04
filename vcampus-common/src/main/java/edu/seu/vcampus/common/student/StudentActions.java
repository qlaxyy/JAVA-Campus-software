package edu.seu.vcampus.common.student;

public final class StudentActions {
    private StudentActions() {}

    public static final String GET_PROFILE = "STUDENT.GET_PROFILE";
    public static final String UPDATE_PROFILE = "STUDENT.UPDATE_PROFILE";

    // 异动相关动作
    public static final String APPLY_STATUS_CHANGE = "STUDENT.APPLY_STATUS_CHANGE";
    public static final String LIST_STATUS_CHANGES = "STUDENT.LIST_STATUS_CHANGES";
    public static final String AUDIT_STATUS_CHANGE = "STUDENT.AUDIT_STATUS_CHANGE";
}
