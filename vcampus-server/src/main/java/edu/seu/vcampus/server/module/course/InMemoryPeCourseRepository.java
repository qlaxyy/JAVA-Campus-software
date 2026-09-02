package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.ScheduleInfo;

import java.util.List;

/**
 * 体育课程开发测试数据。
 */
final class InMemoryPeCourseRepository
    implements PeCourseRepository {

    @Override
    public List<PeCourseRecord> findPeCourses(
        long batchId) {

        return List.of(
            createBasketball(),
            createYoga(),
            createBadminton(),
            createHealthClass()
        );
    }

    /**
     * 篮球：仅男生。
     */
    private PeCourseRecord createBasketball() {

        OfferingInfo offering =
            new OfferingInfo(
                14001L,
                "01",
                List.of(
                    "周老师"
                ),
                List.of(
                    new ScheduleInfo(
                        1,
                        7,
                        8,
                        1,
                        16,
                        "EVERY"
                    )
                ),
                "体育馆-篮球场1",
                "九龙湖校区",
                "中文",
                20,
                30,
                10,
                false,
                "AVAILABLE"
            );

        CourseInfo course =
            new CourseInfo(
                301L,
                "PE1001",
                "篮球",
                1.0,
                "任选",
                false,
                List.of(
                    offering
                )
            );

        PeOfferingRule rule =
            new PeOfferingRule(
                14001L,
                PeGenderRestriction.MALE_ONLY,
                null,
                null,
                20,
                0
            );

        return new PeCourseRecord(
            course,
            "篮球",
            List.of(
                rule
            )
        );
    }

    /**
     * 瑜伽：仅女生。
     */
    private PeCourseRecord createYoga() {

        OfferingInfo offering =
            new OfferingInfo(
                14002L,
                "01",
                List.of(
                    "吴老师"
                ),
                List.of(
                    new ScheduleInfo(
                        2,
                        7,
                        8,
                        1,
                        16,
                        "EVERY"
                    )
                ),
                "体育馆-健身房2",
                "九龙湖校区",
                "中文",
                16,
                25,
                9,
                false,
                "AVAILABLE"
            );

        CourseInfo course =
            new CourseInfo(
                302L,
                "PE1002",
                "瑜伽",
                1.0,
                "任选",
                false,
                List.of(
                    offering
                )
            );

        PeOfferingRule rule =
            new PeOfferingRule(
                14002L,
                PeGenderRestriction.FEMALE_ONLY,
                null,
                null,
                0,
                16
            );

        return new PeCourseRecord(
            course,
            "瑜伽",
            List.of(
                rule
            )
        );
    }

    /**
     * 羽毛球：
     *
     * 男女都能选。
     *
     * 男生 20 人。
     * 女生 20 人。
     * 总容量 40 人。
     */
    private PeCourseRecord createBadminton() {

        OfferingInfo offering =
            new OfferingInfo(
                14003L,
                "01",
                List.of(
                    "郑老师"
                ),
                List.of(
                    new ScheduleInfo(
                        3,
                        5,
                        6,
                        1,
                        16,
                        "EVERY"
                    )
                ),
                "体育馆-羽毛球馆",
                "九龙湖校区",
                "中文",
                30,
                40,
                10,
                false,
                "AVAILABLE"
            );

        CourseInfo course =
            new CourseInfo(
                303L,
                "PE1003",
                "羽毛球",
                1.0,
                "任选",
                false,
                List.of(
                    offering
                )
            );

        PeOfferingRule rule =
            new PeOfferingRule(
                14003L,
                PeGenderRestriction.MIXED_SPLIT,
                20,
                20,
                18,
                12
            );

        return new PeCourseRecord(
            course,
            "羽毛球",
            List.of(
                rule
            )
        );
    }

    /**
     * 保健班：
     * 所有人都可以选择。
     */
    private PeCourseRecord createHealthClass() {

        OfferingInfo offering =
            new OfferingInfo(
                14004L,
                "01",
                List.of(
                    "许老师"
                ),
                List.of(
                    new ScheduleInfo(
                        4,
                        7,
                        8,
                        1,
                        16,
                        "EVERY"
                    )
                ),
                "体育馆-活动室1",
                "九龙湖校区",
                "中文",
                20,
                30,
                10,
                false,
                "AVAILABLE"
            );

        CourseInfo course =
            new CourseInfo(
                304L,
                "PE1099",
                "保健班",
                1.0,
                "任选",
                false,
                List.of(
                    offering
                )
            );

        PeOfferingRule rule =
            new PeOfferingRule(
                14004L,
                PeGenderRestriction.UNRESTRICTED,
                null,
                null,
                10,
                10
            );

        return new PeCourseRecord(
            course,
            "保健班",
            List.of(
                rule
            )
        );
    }
}
