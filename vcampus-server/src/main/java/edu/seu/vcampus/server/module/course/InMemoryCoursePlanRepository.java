package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.ScheduleInfo;

import java.util.List;

/**
 * 开发阶段使用的内存方案内课程数据。
 *
 * 后续接入数据库后由 JDBC/Access Repository 替代。
 */
final class InMemoryCoursePlanRepository
    implements CoursePlanRepository {

    @Override
    public List<CourseInfo> findPlanCourses(long batchId) {

        return List.of(
            createCalculusCourse(),
            createPhysicsCourse()
        );
    }

    /**
     * 测试课程：高等数学。
     */
    private CourseInfo createCalculusCourse() {

        ScheduleInfo monday =
            new ScheduleInfo(
                1,
                1,
                2,
                1,
                16,
                "EVERY"
            );

        ScheduleInfo wednesday =
            new ScheduleInfo(
                3,
                1,
                2,
                1,
                16,
                "EVERY"
            );

        OfferingInfo firstOffering =
            new OfferingInfo(
                1001L,
                "01",
                List.of("张老师"),
                List.of(
                    monday,
                    wednesday
                ),
                "教一-101",
                "九龙湖校区",
                "中文",
                32,
                40,
                8,
                false,
                "AVAILABLE"
            );

        OfferingInfo secondOffering =
            new OfferingInfo(
                1002L,
                "02",
                List.of("李老师"),
                List.of(
                    new ScheduleInfo(
                        2,
                        3,
                        4,
                        1,
                        16,
                        "EVERY"
                    )
                ),
                "教一-205",
                "九龙湖校区",
                "中文",
                40,
                40,
                0,
                false,
                "FULL"
            );

        return new CourseInfo(
            101L,
            "MATH1001",
            "高等数学",
            4.0,
            "必修",
            false,
            List.of(
                firstOffering,
                secondOffering
            )
        );
    }

    /**
     * 测试课程：大学物理。
     * <p>
     * 当前时间故意设置为与高等数学 01 班冲突，
     * 用于测试服务器时间冲突检测。
     */
    private CourseInfo createPhysicsCourse() {

        OfferingInfo offering =
            new OfferingInfo(
                2001L,
                "01",
                List.of(
                    "王老师",
                    "陈老师"
                ),
                List.of(
                    new ScheduleInfo(
                        1,      // 周一
                        2,      // 第2节开始
                        3,      // 第3节结束
                        1,      // 第1周
                        16,     // 第16周
                        "EVERY"
                    )
                ),
                "教二-303",
                "九龙湖校区",
                "双语",
                25,
                35,
                10,
                false,
                "AVAILABLE"
            );

        return new CourseInfo(
            102L,
            "PHYS1001",
            "大学物理",
            3.0,
            "必修",
            false,
            List.of(offering)
        );
    }
}
