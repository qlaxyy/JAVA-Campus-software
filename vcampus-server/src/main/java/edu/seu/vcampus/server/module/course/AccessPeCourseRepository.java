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
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Microsoft Access 体育课程仓库。
 */
@SuppressWarnings({
    "SqlNoDataSourceInspection",
    "SqlResolve",
    "SqlDialectInspection"
})
final class AccessPeCourseRepository
    implements PeCourseRepository {

    private static final String PE_COURSE_TABLE =
        "tblPeCourse";

    private static final String PE_RULE_TABLE =
        "tblPeOfferingRule";

    private static final String COURSE_GROUP =
        "PE";

    private final AccessDatabase database;

    AccessPeCourseRepository(
        AccessDatabase database,
        PeCourseRepository seedRepository) {

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
    public List<PeCourseRecord> findPeCourses(
        long batchId) {

        String sql =
            "SELECT c.courseId, c.courseCode, "
                + "c.courseName, c.credits, "
                + "c.courseType, c.departmentName, "
                + "p.sportProject "
                + "FROM tblCourse AS c "
                + "INNER JOIN tblPeCourse AS p "
                + "ON c.courseId = p.courseId "
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

                List<PeCourseRecord> records =
                    new ArrayList<>();

                while (result.next()) {

                    long courseId =
                        result.getLong(
                            "courseId");

                    List<OfferingInfo> offerings =
                        findOfferings(
                            connection,
                            courseId);

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
                            result.getString(
                                "departmentName"),
                            false,
                            offerings);


                    records.add(
                        new PeCourseRecord(
                            course,
                            result.getString(
                                "sportProject"),
                            findRules(
                                connection,
                                offerings)));
                }

                return List.copyOf(
                    records);
            }

        } catch (SQLException exception) {

            throw failure(
                "无法读取体育课程。",
                exception);
        }
    }

    /**
     * 读取一门体育课的全部教学班。
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
     * 读取体育教学班规则。
     */
    private List<PeOfferingRule> findRules(
        Connection connection,
        List<OfferingInfo> offerings)
        throws SQLException {

        List<PeOfferingRule> rules =
            new ArrayList<>();

        for (OfferingInfo offering
            : offerings) {

            PeOfferingRule rule =
                findRule(
                    connection,
                    offering.getOfferingId());

            if (rule != null) {

                rules.add(
                    rule);
            }
        }

        return List.copyOf(
            rules);
    }

    /**
     * 读取一条体育教学班规则。
     */
    private PeOfferingRule findRule(
        Connection connection,
        long offeringId)
        throws SQLException {

        String sql =
            "SELECT genderRestriction, "
                + "maleCapacity, femaleCapacity, "
                + "maleSelectedCount, "
                + "femaleSelectedCount "
                + "FROM tblPeOfferingRule "
                + "WHERE offeringId = ?";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                offeringId);

            try (ResultSet result =
                     statement.executeQuery()) {

                if (!result.next()) {

                    return null;
                }

                return new PeOfferingRule(
                    offeringId,
                    PeGenderRestriction.valueOf(
                        result.getString(
                            "genderRestriction")),
                    nullableInteger(
                        result,
                        "maleCapacity"),
                    nullableInteger(
                        result,
                        "femaleCapacity"),
                    result.getInt(
                        "maleSelectedCount"),
                    result.getInt(
                        "femaleSelectedCount"));
            }
        }
    }

    /**
     * 初始化体育课专用表。
     */
    private void initialiseSchema() {

        try (Connection connection =
                 database.openConnection();
             Statement statement =
                 connection.createStatement()) {

            if (!tableExists(
                connection,
                PE_COURSE_TABLE)) {

                statement.executeUpdate(
                    "CREATE TABLE tblPeCourse ("
                        + "courseId LONG PRIMARY KEY, "
                        + "sportProject TEXT(100) NOT NULL)");
            }

            if (!tableExists(
                connection,
                PE_RULE_TABLE)) {

                statement.executeUpdate(
                    "CREATE TABLE tblPeOfferingRule ("
                        + "offeringId LONG PRIMARY KEY, "
                        + "genderRestriction TEXT(30) NOT NULL, "
                        + "maleCapacity LONG, "
                        + "femaleCapacity LONG, "
                        + "maleSelectedCount LONG NOT NULL, "
                        + "femaleSelectedCount LONG NOT NULL)");
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化体育课程表。",
                exception);
        }
    }

    /**
     * 首次运行时复制原有内存体育课数据。
     */
    private void seedIfEmpty(
        PeCourseRepository seedRepository) {

        try (Connection connection =
                 database.openConnection()) {

            if (countPeCourses(
                connection) > 0) {

                return;
            }

            connection.setAutoCommit(
                false);

            try {

                for (PeCourseRecord record
                    : seedRepository.findPeCourses(
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
                "无法初始化体育课程数据。",
                exception);
        }
    }

    private int countPeCourses(
        Connection connection)
        throws SQLException {

        String sql =
            "SELECT COUNT(*) AS recordCount "
                + "FROM tblPeCourse";

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
     * 保存一门体育课及其教学班。
     */
    private void insertCourse(
        Connection connection,
        PeCourseRecord record)
        throws SQLException {

        CourseInfo course =
            record.course();

        String courseSql =
            "INSERT INTO tblCourse "
                + "(courseId, courseCode, courseName, "
                + "credits, courseType, departmentName, "
                + "courseGroup) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

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
                course.getDepartmentName());

            statement.setString(
                7,
                COURSE_GROUP);

            statement.executeUpdate();
        }

        String peCourseSql =
            "INSERT INTO tblPeCourse "
                + "(courseId, sportProject) "
                + "VALUES (?, ?)";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     peCourseSql)) {

            statement.setLong(
                1,
                course.getCourseId());

            statement.setString(
                2,
                record.sportProject());

            statement.executeUpdate();
        }

        for (OfferingInfo offering
            : course.getOfferings()) {

            insertOffering(
                connection,
                course.getCourseId(),
                offering);
        }

        for (PeOfferingRule rule
            : record.offeringRules()) {

            insertRule(
                connection,
                rule);
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

    private void insertRule(
        Connection connection,
        PeOfferingRule rule)
        throws SQLException {

        String sql =
            "INSERT INTO tblPeOfferingRule "
                + "(offeringId, genderRestriction, "
                + "maleCapacity, femaleCapacity, "
                + "maleSelectedCount, femaleSelectedCount) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                rule.offeringId());

            statement.setString(
                2,
                rule.genderRestriction()
                    .name());

            setNullableInteger(
                statement,
                3,
                rule.maleCapacity());

            setNullableInteger(
                statement,
                4,
                rule.femaleCapacity());

            statement.setInt(
                5,
                rule.maleSelectedCount());

            statement.setInt(
                6,
                rule.femaleSelectedCount());

            statement.executeUpdate();
        }
    }

    private void setNullableInteger(
        PreparedStatement statement,
        int index,
        Integer value)
        throws SQLException {

        if (value == null) {

            statement.setNull(
                index,
                Types.INTEGER);

        } else {

            statement.setInt(
                index,
                value);
        }
    }

    private Integer nullableInteger(
        ResultSet result,
        String column)
        throws SQLException {

        int value =
            result.getInt(
                column);

        return result.wasNull()
            ? null
            : value;
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
