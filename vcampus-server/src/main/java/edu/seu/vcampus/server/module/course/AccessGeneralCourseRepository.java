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
 * Microsoft Access 通选课程仓库。
 */
@SuppressWarnings({
    "SqlNoDataSourceInspection",
    "SqlResolve",
    "SqlDialectInspection"
})
final class AccessGeneralCourseRepository
    implements GeneralCourseRepository {

    private static final String TABLE =
        "tblGeneralCourse";

    private static final String COURSE_GROUP =
        "GENERAL";

    private final AccessDatabase database;

    AccessGeneralCourseRepository(
        AccessDatabase database,
        GeneralCourseRepository seedRepository) {

        this.database =
            Objects.requireNonNull(
                database);

        Objects.requireNonNull(
            seedRepository);

        initialiseSchema();
        seedIfEmpty(
            seedRepository);
    }

    @Override
    public List<GeneralCourseRecord> findGeneralCourses(
        long batchId) {

        String sql =
            "SELECT c.courseId, c.courseCode, "
                + "c.courseName, c.credits, "
                + "c.courseType, g.generalCategory "
                + "FROM tblCourse AS c "
                + "INNER JOIN tblGeneralCourse AS g "
                + "ON c.courseId = g.courseId "
                + "WHERE c.courseGroup = ? "
                + "ORDER BY c.courseId";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setString(
                1,
                COURSE_GROUP);

            try (ResultSet result =
                     statement.executeQuery()) {

                List<GeneralCourseRecord> records =
                    new ArrayList<>();

                while (result.next()) {

                    long courseId =
                        result.getLong(
                            "courseId");

                    CourseInfo course =
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
                                connection,
                                courseId));

                    records.add(
                        new GeneralCourseRecord(
                            course,
                            result.getString(
                                "generalCategory")));
                }

                return List.copyOf(
                    records);
            }

        } catch (SQLException exception) {

            throw failure(
                "无法读取通选课程。",
                exception);
        }
    }

    /**
     * 读取一门通选课的全部教学班。
     */
    private List<OfferingInfo> findOfferings(
        Connection connection,
        long courseId)
        throws SQLException {

        String sql =
            "SELECT offeringId, classNo, "
                + "locationName, campusName, "
                + "teachingLanguage, selectedCount, "
                + "capacity, availabilityStatus "
                + "FROM tblCourseOffering "
                + "WHERE courseId = ? "
                + "ORDER BY offeringId";

        try (PreparedStatement statement =
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
                            findTeacherNames(
                                connection,
                                offeringId),
                            findSchedules(
                                connection,
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
        }
    }

    /**
     * 读取教学班教师。
     */
    private List<String> findTeacherNames(
        Connection connection,
        long offeringId)
        throws SQLException {

        String sql =
            "SELECT teacherName "
                + "FROM tblOfferingTeacher "
                + "WHERE offeringId = ? "
                + "ORDER BY offeringTeacherId";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                offeringId);

            try (ResultSet result =
                     statement.executeQuery()) {

                List<String> names =
                    new ArrayList<>();

                while (result.next()) {

                    names.add(
                        result.getString(
                            "teacherName"));
                }

                return List.copyOf(
                    names);
            }
        }
    }

    /**
     * 读取教学班时间。
     */
    private List<ScheduleInfo> findSchedules(
        Connection connection,
        long offeringId)
        throws SQLException {

        String sql =
            "SELECT dayOfWeek, startPeriod, "
                + "endPeriod, startWeek, "
                + "endWeek, weekPattern "
                + "FROM tblCourseSchedule "
                + "WHERE offeringId = ? "
                + "ORDER BY scheduleId";

        try (PreparedStatement statement =
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
        }
    }

    /**
     * 初始化通选课专用表。
     */
    private void initialiseSchema() {

        try (Connection connection =
                 database.openConnection()) {

            if (tableExists(
                connection,
                TABLE)) {

                return;
            }

            try (Statement statement =
                     connection.createStatement()) {

                statement.executeUpdate(
                    "CREATE TABLE tblGeneralCourse ("
                        + "courseId LONG PRIMARY KEY, "
                        + "generalCategory TEXT(100) NOT NULL)");
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化通选课程表。",
                exception);
        }
    }

    /**
     * 首次运行时复制原有内存通选课数据。
     */
    private void seedIfEmpty(
        GeneralCourseRepository seedRepository) {

        try (Connection connection =
                 database.openConnection()) {

            if (countGeneralCourses(
                connection) > 0) {

                return;
            }

            connection.setAutoCommit(
                false);

            try {

                for (GeneralCourseRecord record
                    : seedRepository.findGeneralCourses(
                    0L)) {

                    insertCourse(
                        connection,
                        record);
                }

                connection.commit();

            } catch (SQLException
                     | RuntimeException exception) {

                connection.rollback();
                throw exception;
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化通选课程数据。",
                exception);
        }
    }

    private int countGeneralCourses(
        Connection connection)
        throws SQLException {

        String sql =
            "SELECT COUNT(*) AS recordCount "
                + "FROM tblGeneralCourse";

        try (Statement statement =
                 connection.createStatement();
             ResultSet result =
                 statement.executeQuery(
                     sql)) {

            return result.next()
                ? result.getInt(
                "recordCount")
                : 0;
        }
    }

    /**
     * 保存一门通选课及其教学班。
     */
    private void insertCourse(
        Connection connection,
        GeneralCourseRecord record)
        throws SQLException {

        CourseInfo course =
            record.course();

        String courseSql =
            "INSERT INTO tblCourse "
                + "(courseId, courseCode, courseName, "
                + "credits, courseType, courseGroup) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     courseSql)) {

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
                COURSE_GROUP);

            statement.executeUpdate();
        }

        String categorySql =
            "INSERT INTO tblGeneralCourse "
                + "(courseId, generalCategory) "
                + "VALUES (?, ?)";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     categorySql)) {

            statement.setLong(
                1,
                course.getCourseId());

            statement.setString(
                2,
                record.generalCategory());

            statement.executeUpdate();
        }

        for (OfferingInfo offering
            : course.getOfferings()) {

            insertOffering(
                connection,
                course.getCourseId(),
                offering);
        }
    }

    /**
     * 保存教学班及教师、时间。
     */
    private void insertOffering(
        Connection connection,
        long courseId,
        OfferingInfo offering)
        throws SQLException {

        String offeringSql =
            "INSERT INTO tblCourseOffering "
                + "(offeringId, courseId, classNo, "
                + "locationName, campusName, "
                + "teachingLanguage, selectedCount, "
                + "capacity, availabilityStatus) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     offeringSql)) {

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
        }

        for (String teacherName
            : offering.getTeacherNames()) {

            insertTeacher(
                connection,
                offering.getOfferingId(),
                teacherName);
        }

        for (ScheduleInfo schedule
            : offering.getSchedules()) {

            insertSchedule(
                connection,
                offering.getOfferingId(),
                schedule);
        }
    }

    private void insertTeacher(
        Connection connection,
        long offeringId,
        String teacherName)
        throws SQLException {

        String sql =
            "INSERT INTO tblOfferingTeacher "
                + "(offeringId, teacherName) "
                + "VALUES (?, ?)";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                offeringId);

            statement.setString(
                2,
                teacherName);

            statement.executeUpdate();
        }
    }

    private void insertSchedule(
        Connection connection,
        long offeringId,
        ScheduleInfo schedule)
        throws SQLException {

        String sql =
            "INSERT INTO tblCourseSchedule "
                + "(offeringId, dayOfWeek, "
                + "startPeriod, endPeriod, "
                + "startWeek, endWeek, weekPattern) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
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
