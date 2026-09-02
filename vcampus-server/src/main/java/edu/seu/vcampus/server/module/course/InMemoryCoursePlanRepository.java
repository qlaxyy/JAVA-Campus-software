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
            createPhysicsCourse(),
            createDataStructureCourse(),
            createOperatingSystemCourse(),
            createDatabaseCourse(),
            createNetworkCourse(),
            createSoftwareEngineeringCourse(),
            createAiCourse()
        );
    }

    /**
     * 测试课程：高等数学。
     */
    private CourseInfo createCalculusCourse() {

        OfferingInfo offering =
            new OfferingInfo(
                1001L,
                "01",
                List.of("张老师"),
                List.of(
                    new ScheduleInfo(
                        1,
                        1,
                        2,
                        1,
                        16,
                        "EVERY"
                    ),
                    new ScheduleInfo(
                        3,
                        1,
                        2,
                        1,
                        16,
                        "EVERY"
                    )
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

        return new CourseInfo(
            101L,
            "MATH1001",
            "高等数学",
            4.0,
            "必修",
            false,
            List.of(offering)
        );
    }

    /**
     * 测试课程：大学物理。
     *
     * 与高等数学制造时间冲突。
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
                        1,
                        2,
                        3,
                        1,
                        16,
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


    /**
     * 测试课程：数据结构。
     *
     * 普通课程测试。
     */
    private CourseInfo createDataStructureCourse() {

        OfferingInfo offering =
            new OfferingInfo(
                3001L,
                "01",
                List.of("刘老师"),
                List.of(
                    new ScheduleInfo(
                        2,
                        3,
                        4,
                        1,
                        8,
                        "EVERY"
                    )
                ),
                "教三-101",
                "九龙湖校区",
                "中文",
                20,
                40,
                20,
                false,
                "AVAILABLE"
            );

        return new CourseInfo(
            103L,
            "CS2001",
            "数据结构",
            4.0,
            "必修",
            false,
            List.of(offering)
        );
    }


    /**
     * 测试课程：操作系统。
     *
     * 与数据结构部分节次重叠，
     * 但教学周不同。
     */
    private CourseInfo createOperatingSystemCourse() {

        OfferingInfo offering =
            new OfferingInfo(
                4001L,
                "01",
                List.of("陈老师"),
                List.of(
                    new ScheduleInfo(
                        2,
                        4,
                        5,
                        9,
                        16,
                        "EVERY"
                    )
                ),
                "教三-102",
                "九龙湖校区",
                "中文",
                30,
                40,
                10,
                false,
                "AVAILABLE"
            );

        return new CourseInfo(
            104L,
            "CS3001",
            "操作系统",
            3.5,
            "必修",
            false,
            List.of(offering)
        );
    }


    /**
     * 测试课程：数据库原理。
     *
     * 单周测试。
     */
    private CourseInfo createDatabaseCourse() {

        OfferingInfo offering =
            new OfferingInfo(
                5001L,
                "01",
                List.of("赵老师"),
                List.of(
                    new ScheduleInfo(
                        3,
                        5,
                        6,
                        1,
                        16,
                        "ODD"
                    )
                ),
                "教四-201",
                "九龙湖校区",
                "中文",
                15,
                40,
                25,
                false,
                "AVAILABLE"
            );

        return new CourseInfo(
            105L,
            "CS3002",
            "数据库原理",
            3.0,
            "必修",
            false,
            List.of(offering)
        );
    }


    /**
     * 测试课程：计算机网络。
     *
     * 与数据库原理同时间，
     * 双周测试。
     */
    private CourseInfo createNetworkCourse() {

        OfferingInfo offering =
            new OfferingInfo(
                6001L,
                "01",
                List.of("孙老师"),
                List.of(
                    new ScheduleInfo(
                        3,
                        5,
                        6,
                        1,
                        16,
                        "EVEN"
                    )
                ),
                "教四-202",
                "九龙湖校区",
                "中文",
                18,
                40,
                22,
                false,
                "AVAILABLE"
            );

        return new CourseInfo(
            106L,
            "CS3003",
            "计算机网络",
            3.0,
            "必修",
            false,
            List.of(offering)
        );
    }


    /**
     * 测试课程：软件工程。
     *
     * 测试连续三节课合并。
     */
    private CourseInfo createSoftwareEngineeringCourse() {

        OfferingInfo offering =
            new OfferingInfo(
                7001L,
                "01",
                List.of("周老师"),
                List.of(
                    new ScheduleInfo(
                        5,
                        1,
                        3,
                        1,
                        16,
                        "EVERY"
                    )
                ),
                "教五-301",
                "九龙湖校区",
                "中文",
                20,
                40,
                20,
                false,
                "AVAILABLE"
            );

        return new CourseInfo(
            107L,
            "CS4001",
            "软件工程",
            3.0,
            "限选",
            false,
            List.of(offering)
        );
    }


    /**
     * 测试课程：人工智能导论。
     *
     * 测试中间周开始课程。
     */
    private CourseInfo createAiCourse() {

        OfferingInfo offering =
            new OfferingInfo(
                8001L,
                "01",
                List.of("吴老师"),
                List.of(
                    new ScheduleInfo(
                        1,
                        7,
                        8,
                        5,
                        12,
                        "EVERY"
                    )
                ),
                "教六-401",
                "九龙湖校区",
                "中文",
                10,
                40,
                30,
                false,
                "AVAILABLE"
            );

        return new CourseInfo(
            108L,
            "AI4001",
            "人工智能导论",
            2.5,
            "任选",
            false,
            List.of(offering)
        );
    }
}
