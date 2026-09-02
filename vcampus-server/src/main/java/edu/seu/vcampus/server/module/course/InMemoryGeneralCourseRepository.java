package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.ScheduleInfo;

import java.util.List;

/**
 * 开发阶段通选课程测试数据。
 */
final class InMemoryGeneralCourseRepository
    implements GeneralCourseRepository {

    @Override
    public List<GeneralCourseRecord> findGeneralCourses(
        long batchId) {

        return List.of(
            createNaturalScience(),
            createHumanities(),
            createInnovation(),
            createMentalHealth(),
            createAesthetic()
        );
    }

    /**
     * 自然科学类。
     *
     * 故意设置为周一 1-2 节，
     * 方便测试与高等数学的时间冲突。
     */
    private GeneralCourseRecord createNaturalScience() {

        OfferingInfo offering =
            new OfferingInfo(
                15001L,
                "01",
                List.of(
                    "刘老师"
                ),
                List.of(
                    new ScheduleInfo(
                        1,
                        1,
                        2,
                        1,
                        16,
                        "EVERY"
                    )
                ),
                "教四-101",
                "九龙湖校区",
                "中文",
                35,
                50,
                15,
                false,
                "AVAILABLE"
            );

        CourseInfo course =
            new CourseInfo(
                401L,
                "GEN1001",
                "宇宙与文明",
                2.0,
                "任选",
                false,
                List.of(
                    offering
                )
            );

        return new GeneralCourseRecord(
            course,
            "自然科学"
        );
    }

    /**
     * 人文社科类。
     */
    private GeneralCourseRecord createHumanities() {

        OfferingInfo offering =
            new OfferingInfo(
                15002L,
                "01",
                List.of(
                    "陈老师"
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
                "教五-203",
                "九龙湖校区",
                "中文",
                28,
                40,
                12,
                false,
                "AVAILABLE"
            );

        CourseInfo course =
            new CourseInfo(
                402L,
                "GEN1002",
                "中国传统文化",
                2.0,
                "任选",
                false,
                List.of(
                    offering
                )
            );

        return new GeneralCourseRecord(
            course,
            "人文社科"
        );
    }

    /**
     * 创新创业类。
     */
    private GeneralCourseRecord createInnovation() {

        OfferingInfo offering =
            new OfferingInfo(
                15003L,
                "01",
                List.of(
                    "杨老师"
                ),
                List.of(
                    new ScheduleInfo(
                        3,
                        7,
                        8,
                        1,
                        16,
                        "EVERY"
                    )
                ),
                "教五-305",
                "九龙湖校区",
                "中文",
                32,
                45,
                13,
                false,
                "AVAILABLE"
            );

        CourseInfo course =
            new CourseInfo(
                403L,
                "GEN1003",
                "创新思维与创业基础",
                2.0,
                "任选",
                false,
                List.of(
                    offering
                )
            );

        return new GeneralCourseRecord(
            course,
            "创新创业"
        );
    }

    /**
     * 心理健康类。
     */
    private GeneralCourseRecord createMentalHealth() {

        OfferingInfo offering =
            new OfferingInfo(
                15004L,
                "01",
                List.of(
                    "王老师"
                ),
                List.of(
                    new ScheduleInfo(
                        4,
                        5,
                        6,
                        1,
                        16,
                        "EVERY"
                    )
                ),
                "教六-201",
                "九龙湖校区",
                "中文",
                22,
                40,
                18,
                false,
                "AVAILABLE"
            );

        CourseInfo course =
            new CourseInfo(
                404L,
                "GEN1004",
                "大学生心理健康",
                2.0,
                "任选",
                false,
                List.of(
                    offering
                )
            );

        return new GeneralCourseRecord(
            course,
            "心理健康"
        );
    }

    /**
     * 美育类。
     *
     * 故意设置成满员，
     * 用于测试 FULL 状态。
     */
    private GeneralCourseRecord createAesthetic() {

        OfferingInfo offering =
            new OfferingInfo(
                15005L,
                "01",
                List.of(
                    "赵老师"
                ),
                List.of(
                    new ScheduleInfo(
                        5,
                        3,
                        4,
                        1,
                        16,
                        "EVERY"
                    )
                ),
                "艺术楼-报告厅",
                "九龙湖校区",
                "中文",
                40,
                40,
                0,
                false,
                "FULL"
            );

        CourseInfo course =
            new CourseInfo(
                405L,
                "GEN1005",
                "艺术鉴赏",
                2.0,
                "任选",
                false,
                List.of(
                    offering
                )
            );

        return new GeneralCourseRecord(
            course,
            "美育"
        );
    }
}
