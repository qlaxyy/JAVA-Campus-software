package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.ScheduleInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 全校课程查询开发阶段测试数据。
 */
final class InMemoryCourseCatalogRepository
    implements CourseCatalogRepository {

    @Override
    public List<CourseCatalogRecord>
    findCurrentSemesterCourses() {

        List<CourseCatalogRecord> records =
            new ArrayList<>();

        /*
         * 数学学院。
         */
        records.add(
            createRecord(
                601L,
                "MATH1001",
                "高等数学",
                4.0,
                "必修",
                "数学学院",
                6001L,
                "01",
                "张老师",
                1,
                1,
                2,
                38,
                50));

        records.add(
            createRecord(
                601L,
                "MATH1001",
                "高等数学",
                4.0,
                "必修",
                "数学学院",
                6002L,
                "02",
                "李老师",
                2,
                3,
                4,
                50,
                50));

        records.add(
            createRecord(
                602L,
                "MATH2001",
                "线性代数",
                3.0,
                "必修",
                "数学学院",
                6003L,
                "01",
                "赵老师",
                3,
                1,
                2,
                32,
                45));

        records.add(
            createRecord(
                603L,
                "MATH2002",
                "概率论与数理统计",
                3.0,
                "必修",
                "数学学院",
                6004L,
                "01",
                "孙老师",
                4,
                3,
                4,
                41,
                50));

        /*
         * 计算机科学与工程学院。
         */
        records.add(
            createRecord(
                604L,
                "CS1001",
                "程序设计基础",
                3.0,
                "必修",
                "计算机科学与工程学院",
                6005L,
                "01",
                "王老师",
                1,
                3,
                4,
                45,
                60));

        records.add(
            createRecord(
                604L,
                "CS1001",
                "程序设计基础",
                3.0,
                "必修",
                "计算机科学与工程学院",
                6006L,
                "02",
                "陈老师",
                2,
                1,
                2,
                52,
                60));

        records.add(
            createRecord(
                605L,
                "CS2001",
                "数据结构",
                3.0,
                "必修",
                "计算机科学与工程学院",
                6007L,
                "01",
                "周老师",
                3,
                3,
                4,
                60,
                60));

        records.add(
            createRecord(
                606L,
                "CS2002",
                "计算机组成原理",
                3.0,
                "必修",
                "计算机科学与工程学院",
                6008L,
                "01",
                "吴老师",
                4,
                1,
                2,
                48,
                60));

        records.add(
            createRecord(
                607L,
                "CS3001",
                "操作系统",
                3.0,
                "必修",
                "计算机科学与工程学院",
                6009L,
                "01",
                "郑老师",
                5,
                3,
                4,
                39,
                50));

        records.add(
            createRecord(
                608L,
                "CS3002",
                "计算机网络",
                3.0,
                "必修",
                "计算机科学与工程学院",
                6010L,
                "01",
                "徐老师",
                1,
                5,
                6,
                43,
                50));

        /*
         * 电子科学与工程学院。
         */
        records.add(
            createRecord(
                609L,
                "EE1001",
                "电路基础",
                3.0,
                "必修",
                "电子科学与工程学院",
                6011L,
                "01",
                "钱老师",
                2,
                5,
                6,
                44,
                50));

        records.add(
            createRecord(
                610L,
                "EE2001",
                "模拟电子技术",
                3.0,
                "必修",
                "电子科学与工程学院",
                6012L,
                "01",
                "朱老师",
                3,
                5,
                6,
                50,
                50));

        records.add(
            createRecord(
                611L,
                "EE2002",
                "数字电子技术",
                3.0,
                "必修",
                "电子科学与工程学院",
                6013L,
                "01",
                "马老师",
                4,
                5,
                6,
                35,
                50));

        /*
         * 物理学院。
         */
        records.add(
            createRecord(
                612L,
                "PHYS1001",
                "大学物理",
                3.0,
                "必修",
                "物理学院",
                6014L,
                "01",
                "王老师",
                1,
                7,
                8,
                46,
                55));

        records.add(
            createRecord(
                612L,
                "PHYS1001",
                "大学物理",
                3.0,
                "必修",
                "物理学院",
                6015L,
                "02",
                "陈老师",
                2,
                7,
                8,
                55,
                55));

        records.add(
            createRecord(
                613L,
                "PHYS2001",
                "大学物理实验",
                1.5,
                "必修",
                "物理学院",
                6016L,
                "01",
                "刘老师",
                3,
                7,
                8,
                28,
                35));

        /*
         * 外国语学院。
         */
        records.add(
            createRecord(
                614L,
                "ENG1001",
                "大学英语",
                2.0,
                "必修",
                "外国语学院",
                6017L,
                "01",
                "杨老师",
                4,
                7,
                8,
                30,
                40));

        records.add(
            createRecord(
                614L,
                "ENG1001",
                "大学英语",
                2.0,
                "必修",
                "外国语学院",
                6018L,
                "02",
                "胡老师",
                5,
                7,
                8,
                40,
                40));

        records.add(
            createRecord(
                615L,
                "ENG2001",
                "学术英语",
                2.0,
                "限选",
                "外国语学院",
                6019L,
                "01",
                "高老师",
                1,
                9,
                10,
                24,
                35));

        /*
         * 人文学院。
         */
        records.add(
            createRecord(
                616L,
                "HUM1001",
                "中国传统文化",
                2.0,
                "任选",
                "人文学院",
                6020L,
                "01",
                "陈老师",
                2,
                9,
                10,
                34,
                45));

        records.add(
            createRecord(
                617L,
                "HUM1002",
                "中国文学经典",
                2.0,
                "任选",
                "人文学院",
                6021L,
                "01",
                "林老师",
                3,
                9,
                10,
                29,
                45));

        /*
         * 艺术学院。
         */
        records.add(
            createRecord(
                618L,
                "ART1001",
                "艺术鉴赏",
                2.0,
                "任选",
                "艺术学院",
                6022L,
                "01",
                "赵老师",
                4,
                9,
                10,
                45,
                45));

        records.add(
            createRecord(
                619L,
                "ART1002",
                "音乐欣赏",
                2.0,
                "任选",
                "艺术学院",
                6023L,
                "01",
                "何老师",
                5,
                9,
                10,
                31,
                45));

        /*
         * 心理健康教育中心。
         */
        records.add(
            createRecord(
                620L,
                "PSY1001",
                "大学生心理健康",
                2.0,
                "任选",
                "心理健康教育中心",
                6024L,
                "01",
                "王老师",
                1,
                11,
                12,
                36,
                50));

        /*
         * 创新创业学院。
         */
        records.add(
            createRecord(
                621L,
                "INNO1001",
                "创新思维与创业基础",
                2.0,
                "任选",
                "创新创业学院",
                6025L,
                "01",
                "杨老师",
                2,
                11,
                12,
                25,
                40));

        return records;
    }

    /**
     * 创建一条课程目录记录。
     */
    private CourseCatalogRecord createRecord(
        long courseId,
        String courseCode,
        String courseName,
        double credits,
        String courseType,
        String departmentName,
        long offeringId,
        String classNo,
        String teacherName,
        int dayOfWeek,
        int startPeriod,
        int endPeriod,
        int selectedCount,
        int capacity) {

        int remainingCount =
            Math.max(
                capacity - selectedCount,
                0);

        String status =
            remainingCount > 0
                ? "AVAILABLE"
                : "FULL";

        OfferingInfo offering =
            new OfferingInfo(
                offeringId,
                classNo,
                List.of(
                    teacherName),
                List.of(
                    new ScheduleInfo(
                        dayOfWeek,
                        startPeriod,
                        endPeriod,
                        1,
                        16,
                        "EVERY")),
                "教学楼-" + classNo,
                "九龙湖校区",
                "中文",
                selectedCount,
                capacity,
                remainingCount,
                false,
                status);

        CourseInfo course =
            new CourseInfo(
                courseId,
                courseCode,
                courseName,
                credits,
                courseType,
                false,
                List.of(
                    offering));

        return new CourseCatalogRecord(
            course,
            departmentName);
    }
}
