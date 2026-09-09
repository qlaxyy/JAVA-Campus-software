package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.ScheduleInfo;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Access 普通方案内课程仓库。
 */
@SuppressWarnings({
    "SqlNoDataSourceInspection",
    "SqlResolve",
    "SqlDialectInspection"
})
final class AccessCoursePlanRepository
    implements CoursePlanRepository {

    private final AccessDatabase database;

    private final CoursePlanRepository
        seedRepository;

    AccessCoursePlanRepository(
        AccessDatabase database,
        CoursePlanRepository seedRepository) {

        this.database =
            Objects.requireNonNull(
                database);

        this.seedRepository =
            Objects.requireNonNull(
                seedRepository);

        initialiseSchema();
        initialiseDemoData();
    }

    /**
     * 查询普通方案内课程。
     *
     * 当前演示数据的三个批次使用相同课程范围，
     * 因此暂时不按照 batchId 筛选。
     */
    @Override
    public List<CourseInfo> findPlanCourses(
        long batchId) {

        String sql =
            "SELECT courseId, courseCode, courseName, "
                + "credits, courseType "
                + "FROM tblCourse "
                + "WHERE courseGroup = ? "
                + "ORDER BY courseId";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setString(
                1,
                "REGULAR");

            try (ResultSet result =
                     statement.executeQuery()) {

                List<CourseInfo> courses =
                    new ArrayList<>();

                while (result.next()) {

                    long courseId =
                        result.getLong(
                            "courseId");

                    courses.add(
                        new CourseInfo(
                            courseId,
                            result.getString(
                                "courseCode"),
                            result.getString(
                                "courseName"),
                            result.getDouble(
                                "credits"),
                            result.getString(
                                "courseType"),
                            false,
                            findOfferings(
                                courseId)));
                }

                return List.copyOf(
                    courses);
            }

        } catch (SQLException exception) {

            throw failure(
                "无法读取普通课程。",
                exception);
        }
    }

    /**
     * 查询一门课程的教学班。
     */
    private List<OfferingInfo> findOfferings(
        long courseId) {

        String sql =
            "SELECT offeringId, classNo, locationName, "
                + "campusName, teachingLanguage, "
                + "selectedCount, capacity, "
                + "availabilityStatus "
                + "FROM tblCourseOffering "
                + "WHERE courseId = ? "
                + "ORDER BY offeringId";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                courseId);

            try (ResultSet result =
                     statement.executeQuery()) {

                List<OfferingInfo> offerings =
                    new ArrayList<>();

                while (result.next()) {

                    long offeringId =
                        result.getLong(
                            "offeringId");

                    int selectedCount =
                        result.getInt(
                            "selectedCount");

                    int capacity =
                        result.getInt(
                            "capacity");

                    offerings.add(
                        new OfferingInfo(
                            offeringId,
                            result.getString(
                                "classNo"),
                            findTeachers(
                                offeringId),
                            findSchedules(
                                offeringId),
                            result.getString(
                                "locationName"),
                            result.getString(
                                "campusName"),
                            result.getString(
                                "teachingLanguage"),
                            selectedCount,
                            capacity,
                            Math.max(
                                capacity
                                    - selectedCount,
                                0),
                            false,
                            result.getString(
                                "availabilityStatus")));
                }

                return List.copyOf(
                    offerings);
            }

        } catch (SQLException exception) {

            throw failure(
                "无法读取普通课程教学班。",
                exception);
        }
    }

    /**
     * 查询教学班教师。
     */
    private List<String> findTeachers(
        long offeringId) {

        String sql =
            "SELECT teacherName "
                + "FROM tblOfferingTeacher "
                + "WHERE offeringId = ? "
                + "ORDER BY offeringTeacherId";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                offeringId);

            try (ResultSet result =
                     statement.executeQuery()) {

                List<String> teachers =
                    new ArrayList<>();

                while (result.next()) {

                    teachers.add(
                        result.getString(
                            "teacherName"));
                }

                return List.copyOf(
                    teachers);
            }

        } catch (SQLException exception) {

            throw failure(
                "无法读取教学班教师。",
                exception);
        }
    }

    /**
     * 查询教学班时间。
     */
    private List<ScheduleInfo> findSchedules(
        long offeringId) {

        String sql =
            "SELECT dayOfWeek, startPeriod, endPeriod, "
                + "startWeek, endWeek, weekPattern "
                + "FROM tblCourseSchedule "
                + "WHERE offeringId = ? "
                + "ORDER BY scheduleId";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                offeringId);

            try (ResultSet result =
                     statement.executeQuery()) {

                List<ScheduleInfo> schedules =
                    new ArrayList<>();

                while (result.next()) {

                    schedules.add(
                        new ScheduleInfo(
                            result.getInt(
                                "dayOfWeek"),
                            result.getInt(
                                "startPeriod"),
                            result.getInt(
                                "endPeriod"),
                            result.getInt(
                                "startWeek"),
                            result.getInt(
                                "endWeek"),
                            result.getString(
                                "weekPattern")));
                }

                return List.copyOf(
                    schedules);
            }

        } catch (SQLException exception) {

            throw failure(
                "无法读取教学班时间。",
                exception);
        }
    }

    /**
     * 初始化课程相关表。
     */
    private void initialiseSchema() {

        try (Connection connection =
                 database.openConnection()) {

            if (!tableExists(
                connection,
                "tblCourse")) {

                try (Statement statement =
                         connection.createStatement()) {

                    statement.executeUpdate(
                        "CREATE TABLE tblCourse ("
                            + "courseId LONG NOT NULL, "
                            + "courseCode TEXT(30) NOT NULL, "
                            + "courseName TEXT(100) NOT NULL, "
                            + "credits DOUBLE NOT NULL, "
                            + "courseType TEXT(20) NOT NULL, "
                            + "courseGroup TEXT(20) NOT NULL)");

                    statement.executeUpdate(
                        "CREATE UNIQUE INDEX "
                            + "ux_tblCourse_courseId "
                            + "ON tblCourse (courseId)");

                    statement.executeUpdate(
                        "CREATE UNIQUE INDEX "
                            + "ux_tblCourse_courseCode "
                            + "ON tblCourse (courseCode)");
                }
            }

            if (!tableExists(
                connection,
                "tblCourseOffering")) {

                try (Statement statement =
                         connection.createStatement()) {

                    statement.executeUpdate(
                        "CREATE TABLE tblCourseOffering ("
                            + "offeringId LONG NOT NULL, "
                            + "courseId LONG NOT NULL, "
                            + "classNo TEXT(20) NOT NULL, "
                            + "locationName TEXT(100), "
                            + "campusName TEXT(100), "
                            + "teachingLanguage TEXT(20) NOT NULL, "
                            + "selectedCount LONG NOT NULL, "
                            + "capacity LONG NOT NULL, "
                            + "availabilityStatus TEXT(30) NOT NULL)");

                    statement.executeUpdate(
                        "CREATE UNIQUE INDEX "
                            + "ux_tblCourseOffering_offeringId "
                            + "ON tblCourseOffering (offeringId)");

                    statement.executeUpdate(
                        "CREATE INDEX "
                            + "ix_tblCourseOffering_courseId "
                            + "ON tblCourseOffering (courseId)");
                }
            }

            if (!tableExists(
                connection,
                "tblOfferingTeacher")) {

                try (Statement statement =
                         connection.createStatement()) {

                    statement.executeUpdate(
                        "CREATE TABLE tblOfferingTeacher ("
                            + "offeringTeacherId COUNTER PRIMARY KEY, "
                            + "offeringId LONG NOT NULL, "
                            + "teacherName TEXT(50) NOT NULL)");

                    statement.executeUpdate(
                        "CREATE INDEX "
                            + "ix_tblOfferingTeacher_offeringId "
                            + "ON tblOfferingTeacher (offeringId)");
                }
            }

            if (!tableExists(
                connection,
                "tblCourseSchedule")) {

                try (Statement statement =
                         connection.createStatement()) {

                    statement.executeUpdate(
                        "CREATE TABLE tblCourseSchedule ("
                            + "scheduleId COUNTER PRIMARY KEY, "
                            + "offeringId LONG NOT NULL, "
                            + "dayOfWeek LONG NOT NULL, "
                            + "startPeriod LONG NOT NULL, "
                            + "endPeriod LONG NOT NULL, "
                            + "startWeek LONG NOT NULL, "
                            + "endWeek LONG NOT NULL, "
                            + "weekPattern TEXT(20) NOT NULL)");

                    statement.executeUpdate(
                        "CREATE INDEX "
                            + "ix_tblCourseSchedule_offeringId "
                            + "ON tblCourseSchedule (offeringId)");
                }
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化普通课程数据表。",
                exception);
        }
    }

    /**
     * 将原有内存演示课程写入空数据库。
     */
    private void initialiseDemoData() {

        List<CourseInfo> demoCourses =
            seedRepository.findPlanCourses(
                1L);

        for (CourseInfo course
            : demoCourses) {

            if (!courseExists(
                course.getCourseId())) {

                insertCourse(
                    course);
            }

            for (OfferingInfo offering
                : course.getOfferings()) {

                if (!offeringExists(
                    offering.getOfferingId())) {

                    insertOffering(
                        course.getCourseId(),
                        offering);

                    for (String teacher
                        : offering.getTeacherNames()) {

                        insertTeacher(
                            offering.getOfferingId(),
                            teacher);
                    }

                    for (ScheduleInfo schedule
                        : offering.getSchedules()) {

                        insertSchedule(
                            offering.getOfferingId(),
                            schedule);
                    }
                }
            }
        }
    }

    private boolean courseExists(
        long courseId) {

        return recordExists(
            "SELECT COUNT(*) AS recordCount "
                + "FROM tblCourse WHERE courseId = ?",
            courseId);
    }

    private boolean offeringExists(
        long offeringId) {

        return recordExists(
            "SELECT COUNT(*) AS recordCount "
                + "FROM tblCourseOffering "
                + "WHERE offeringId = ?",
            offeringId);
    }

    private boolean recordExists(
        String sql,
        long id) {

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                id);

            try (ResultSet result =
                     statement.executeQuery()) {

                return result.next()
                    && result.getInt(
                    "recordCount") > 0;
            }

        } catch (SQLException exception) {

            throw failure(
                "无法检查课程演示数据。",
                exception);
        }
    }

    private void insertCourse(
        CourseInfo course) {

        String sql =
            "INSERT INTO tblCourse "
                + "(courseId, courseCode, courseName, "
                + "credits, courseType, courseGroup) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                course.getCourseId());

            statement.setString(
                2,
                course.getCourseCode());

            statement.setString(
                3,
                course.getCourseName());

            statement.setDouble(
                4,
                course.getCredits());

            statement.setString(
                5,
                course.getCourseType());

            statement.setString(
                6,
                "REGULAR");

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法写入普通课程。",
                exception);
        }
    }

    private void insertOffering(
        long courseId,
        OfferingInfo offering) {

        String sql =
            "INSERT INTO tblCourseOffering "
                + "(offeringId, courseId, classNo, "
                + "locationName, campusName, "
                + "teachingLanguage, selectedCount, "
                + "capacity, availabilityStatus) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                offering.getOfferingId());

            statement.setLong(
                2,
                courseId);

            statement.setString(
                3,
                offering.getClassNo());

            statement.setString(
                4,
                offering.getLocationName());

            statement.setString(
                5,
                offering.getCampusName());

            statement.setString(
                6,
                offering.getTeachingLanguage());

            statement.setInt(
                7,
                offering.getSelectedCount());

            statement.setInt(
                8,
                offering.getCapacity());

            statement.setString(
                9,
                offering.getAvailabilityStatus());

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法写入普通课程教学班。",
                exception);
        }
    }

    private void insertTeacher(
        long offeringId,
        String teacherName) {

        String sql =
            "INSERT INTO tblOfferingTeacher "
                + "(offeringId, teacherName) "
                + "VALUES (?, ?)";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                offeringId);

            statement.setString(
                2,
                teacherName);

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法写入教学班教师。",
                exception);
        }
    }

    private void insertSchedule(
        long offeringId,
        ScheduleInfo schedule) {

        String sql =
            "INSERT INTO tblCourseSchedule "
                + "(offeringId, dayOfWeek, "
                + "startPeriod, endPeriod, "
                + "startWeek, endWeek, weekPattern) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                offeringId);

            statement.setInt(
                2,
                schedule.getDayOfWeek());

            statement.setInt(
                3,
                schedule.getStartPeriod());

            statement.setInt(
                4,
                schedule.getEndPeriod());

            statement.setInt(
                5,
                schedule.getStartWeek());

            statement.setInt(
                6,
                schedule.getEndWeek());

            statement.setString(
                7,
                schedule.getWeekPattern());

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法写入教学班时间。",
                exception);
        }
    }

    private boolean tableExists(
        Connection connection,
        String expected)
        throws SQLException {

        DatabaseMetaData metadata =
            connection.getMetaData();

        try (ResultSet tables =
                 metadata.getTables(
                     null,
                     null,
                     "%",
                     new String[]{"TABLE"})) {

            while (tables.next()) {

                if (expected.equalsIgnoreCase(
                    tables.getString(
                        "TABLE_NAME"))) {

                    return true;
                }
            }
        }

        return false;
    }

    private IllegalStateException failure(
        String message,
        SQLException cause) {

        return new IllegalStateException(
            message
                + " 数据库："
                + database.path(),
            cause);
    }
}
