package edu.seu.vcampus.common.course;

import edu.seu.vcampus.common.protocol.ActionNames;
import edu.seu.vcampus.common.protocol.ModuleNames;

/**
 * Public actions owned by the course-selection module.
 */
public final class CourseActions {

    /** 查询当前学期选课批次。 */
    public static final String LIST_BATCHES =
        ActionNames.of(ModuleNames.COURSE, "LIST_BATCHES");

    /** 查询方案内课程。 */
    public static final String LIST_PLAN_COURSES =
        ActionNames.of(ModuleNames.COURSE, "LIST_PLAN_COURSES");

    /** 查询方案外课程。 */
    public static final String LIST_SUBSTITUTE_COURSES =
        ActionNames.of(ModuleNames.COURSE, "LIST_SUBSTITUTE_COURSES");

    /** 查询体育课程。 */
    public static final String LIST_PE_COURSES =
        ActionNames.of(ModuleNames.COURSE, "LIST_PE_COURSES");

    /** 查询通选课程。 */
    public static final String LIST_GENERAL_COURSES =
        ActionNames.of(ModuleNames.COURSE, "LIST_GENERAL_COURSES");

    /** 查询当前学期已选课程。 */
    public static final String LIST_ENROLLMENTS =
        ActionNames.of(ModuleNames.COURSE, "LIST_ENROLLMENTS");

    /** 全校课程查询。 */
    public static final String SEARCH_OFFERINGS =
        ActionNames.of(ModuleNames.COURSE, "SEARCH_OFFERINGS");

    /** 选择教学班。 */
    public static final String SELECT_COURSE =
        ActionNames.of(ModuleNames.COURSE, "SELECT_COURSE");

    /** 退选教学班。 */
    public static final String DROP_COURSE =
        ActionNames.of(ModuleNames.COURSE, "DROP_COURSE");

    private CourseActions() {
    }
}
