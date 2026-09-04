package edu.seu.vcampus.common.course;

import edu.seu.vcampus.common.protocol.ActionNames;
import edu.seu.vcampus.common.protocol.ModuleNames;

/**
 * 选课模块公开 Action。
 */
public final class CourseActions {
    public static final String ADMIN_UPDATE_OFFERING =
        ActionNames.of(
            ModuleNames.COURSE,
            "ADMIN_UPDATE_OFFERING");
    public static final String ADMIN_LIST_AUDIT_LOGS =
        ActionNames.of(
            ModuleNames.COURSE,
            "ADMIN_LIST_AUDIT_LOGS");
    /** 查询当前学期选课批次。 */
    public static final String LIST_BATCHES =
        ActionNames.of(
            ModuleNames.COURSE,
            "LIST_BATCHES");

    /** 查询方案内课程。 */
    public static final String LIST_PLAN_COURSES =
        ActionNames.of(
            ModuleNames.COURSE,
            "LIST_PLAN_COURSES");

    /** 查询方案外课程。 */
    public static final String LIST_SUBSTITUTE_COURSES =
        ActionNames.of(
            ModuleNames.COURSE,
            "LIST_SUBSTITUTE_COURSES");

    /** 查询体育课程。 */
    public static final String LIST_PE_COURSES =
        ActionNames.of(
            ModuleNames.COURSE,
            "LIST_PE_COURSES");

    /** 查询通选课程。 */
    public static final String LIST_GENERAL_COURSES =
        ActionNames.of(
            ModuleNames.COURSE,
            "LIST_GENERAL_COURSES");

    /** 教务修改选课批次。 */
    public static final String ADMIN_UPDATE_BATCH =
        ActionNames.of(
            ModuleNames.COURSE,
            "ADMIN_UPDATE_BATCH");
    /** 查询当前学生已选课程。 */
    public static final String LIST_ENROLLMENTS =
        ActionNames.of(
            ModuleNames.COURSE,
            "LIST_ENROLLMENTS");
    /** 教务修改课程基本信息。 */
    public static final String ADMIN_UPDATE_COURSE =
        ActionNames.of(
            ModuleNames.COURSE,
            "ADMIN_UPDATE_COURSE");
    /** 全校课程查询。 */
    public static final String SEARCH_OFFERINGS =
        ActionNames.of(
            ModuleNames.COURSE,
            "SEARCH_OFFERINGS");

    /** 学生选择教学班。 */
    public static final String SELECT_COURSE =
        ActionNames.of(
            ModuleNames.COURSE,
            "SELECT_COURSE");

    /** 学生退选教学班。 */
    public static final String DROP_COURSE =
        ActionNames.of(
            ModuleNames.COURSE,
            "DROP_COURSE");

    /*
     * =========================
     * 教务老师 / 超级管理员 Action
     * =========================
     */

    /**
     * 管理员查询指定学生的已选课程。
     */
    public static final String
        ADMIN_LIST_STUDENT_ENROLLMENTS =
        ActionNames.of(
            ModuleNames.COURSE,
            "ADMIN_LIST_STUDENT_ENROLLMENTS");

    /**
     * 管理员为指定学生强制选课。
     */
    public static final String
        ADMIN_FORCE_SELECT_COURSE =
        ActionNames.of(
            ModuleNames.COURSE,
            "ADMIN_FORCE_SELECT_COURSE");
    public static final String ADMIN_LIST_OFFERINGS =
        ActionNames.of(
            ModuleNames.COURSE,
            "ADMIN_LIST_OFFERINGS");
    /**
     * 管理员为指定学生强制退课。
     */
    public static final String
        ADMIN_FORCE_DROP_COURSE =
        ActionNames.of(
            ModuleNames.COURSE,
            "ADMIN_FORCE_DROP_COURSE");

    private CourseActions() {
    }
}
