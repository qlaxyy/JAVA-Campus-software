package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.ScheduleInfo;

import java.util.List;
import java.util.Map;

/**
 * 开发阶段使用的方案外课程测试数据。
 */
final class InMemoryCourseSubstitutionRepository
    implements CourseSubstitutionRepository {

    /**
     * 方案外课程
     * ->
     * 被替代的方案内课程。
     *
     * 201 数学分析基础
     * ->
     * 101 高等数学
     */
    private static final Map<Long, Long>
        SUBSTITUTION_RELATIONS =
        Map.of(
            201L,
            101L
        );

    @Override
    public List<CourseInfo> findSubstituteCourses(
        long batchId) {

        return List.of(
            createAnalysisCourse()
        );
    }

    @Override
    public Long findReplacedCourseId(
        long substituteCourseId) {

        return SUBSTITUTION_RELATIONS.get(
            substituteCourseId);
    }

    /**
     * 测试方案外课程：
     * 数学分析基础。
     */
    private CourseInfo createAnalysisCourse() {

        OfferingInfo firstOffering =
            new OfferingInfo(
                2001L,
                "01",
                List.of(
                    "赵老师"
                ),
                List.of(
                    new ScheduleInfo(
                        2,
                        5,
                        6,
                        1,
                        16,
                        "EVERY"
                    )
                ),
                "教三-201",
                "九龙湖校区",
                "中文",
                18,
                30,
                12,
                false,
                "AVAILABLE"
            );

        OfferingInfo secondOffering =
            new OfferingInfo(
                2002L,
                "02",
                List.of(
                    "孙老师"
                ),
                List.of(
                    new ScheduleInfo(
                        4,
                        3,
                        4,
                        1,
                        16,
                        "EVERY"
                    )
                ),
                "教三-305",
                "九龙湖校区",
                "中文",
                30,
                30,
                0,
                false,
                "FULL"
            );

        return new CourseInfo(
            201L,
            "MATH1101",
            "数学分析基础",
            3.5,
            "任选",
            false,
            List.of(
                firstOffering,
                secondOffering
            )
        );
    }
}
