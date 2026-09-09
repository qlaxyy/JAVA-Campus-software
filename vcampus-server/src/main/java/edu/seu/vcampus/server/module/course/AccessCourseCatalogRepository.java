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
 * Microsoft Access 全校课程目录仓库。
 *
 * 全校查询课程使用独立的数据表，
 * 不与学生选课使用的 tblCourse 混合，
 * 避免相同课程号及教学班 ID 冲突。
 */
@SuppressWarnings({
    "SqlNoDataSourceInspection",
    "SqlResolve",
    "SqlDialectInspection"
})
final class AccessCourseCatalogRepository
    implements CourseCatalogRepository {

    private static final String COURSE_TABLE =
        "tblCatalogCourse";

    private static final String OFFERING_TABLE =
        "tblCatalogOffering";

    private static final String TEACHER_TABLE =
        "tblCatalogTeacher";

    private static final String SCHEDULE_TABLE =
        "tblCatalogSchedule";

    private final AccessDatabase database;

    AccessCourseCatalogRepository(
        AccessDatabase database,
        CourseCatalogRepository seedRepository) {

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
    public List<CourseCatalogRecord>
    findCurrentSemesterCourses() {

        String sql =
            "SELECT courseId, courseCode, "
                + "courseName, credits, "
                + "courseType, departmentName "
                + "FROM tblCatalogCourse "
                + "ORDER BY courseId";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql);
             ResultSet result =
                 statement.executeQuery()) {

            List<CourseCatalogRecord> records =
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
                    new CourseCatalogRecord(
                        course,
                        result.getString(
                            "departmentName")));
            }

            return List.copyOf(
                records);

        } catch (SQLException exception) {

            throw failure(
                "无法读取全校课程目录。",
                exception);
        }
    }

    /**
     * 查询课程的全部教学班。
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
                + "FROM tblCatalogOffering "
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
     * 查询教学班教师。
     */
    private List<String> findTeacherNames(
        Connection connection,
        long offeringId)
        throws SQLException {

        String sql =
            "SELECT teacherName "
                + "FROM tblCatalogTeacher "
                + "WHERE offeringId = ? "
                + "ORDER BY catalogTeacherId";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                offeringId);

            try (ResultSet result =
                     statement.executeQuery()) {

                List<String> teacherNames =
                    new ArrayList<>();

                while (result.next()) {

                    teacherNames.add(
                        result.getString(
                            "teacherName"));
                }

                return List.copyOf(
                    teacherNames);
            }
        }
    }

    /**
     * 查询教学班时间。
     */
    private List<ScheduleInfo> findSchedules(
        Connection connection,
        long offeringId)
        throws SQLException {

        String sql =
            "SELECT dayOfWeek, startPeriod, "
                + "endPeriod, startWeek, "
                + "endWeek, weekPattern "
                + "FROM tblCatalogSchedule "
                + "WHERE offeringId = ? "
                + "ORDER BY catalogScheduleId";

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
     * 初始化全校课程目录表。
     */
    private void initialiseSchema() {

        try (Connection connection =
                 database.openConnection();
             Statement statement =
                 connection.createStatement()) {

            if (!tableExists(
                connection,
                COURSE_TABLE)) {

                statement.executeUpdate(
                    "CREATE TABLE tblCatalogCourse ("
                        + "courseId LONG PRIMARY KEY, "
                        + "courseCode TEXT(50) NOT NULL, "
                        + "courseName TEXT(100) NOT NULL, "
                        + "credits DOUBLE NOT NULL, "
                        + "courseType TEXT(20) NOT NULL, "
                        + "departmentName TEXT(150) NOT NULL)");

                statement.executeUpdate(
                    "CREATE INDEX "
                        + "ix_tblCatalogCourse_courseCode "
                        + "ON tblCatalogCourse "
                        + "(courseCode)");

                statement.executeUpdate(
                    "CREATE INDEX "
                        + "ix_tblCatalogCourse_department "
                        + "ON tblCatalogCourse "
                        + "(departmentName)");
            }

            if (!tableExists(
                connection,
                OFFERING_TABLE)) {

                statement.executeUpdate(
                    "CREATE TABLE tblCatalogOffering ("
                        + "offeringId LONG PRIMARY KEY, "
                        + "courseId LONG NOT NULL, "
                        + "classNo TEXT(20) NOT NULL, "
                        + "locationName TEXT(100), "
                        + "campusName TEXT(100), "
                        + "teachingLanguage TEXT(30) NOT NULL, "
                        + "selectedCount LONG NOT NULL, "
                        + "capacity LONG NOT NULL, "
                        + "availabilityStatus TEXT(30) NOT NULL)");

                statement.executeUpdate(
                    "CREATE INDEX "
                        + "ix_tblCatalogOffering_courseId "
                        + "ON tblCatalogOffering "
                        + "(courseId)");
            }

            if (!tableExists(
                connection,
                TEACHER_TABLE)) {

                statement.executeUpdate(
                    "CREATE TABLE tblCatalogTeacher ("
                        + "catalogTeacherId COUNTER PRIMARY KEY, "
                        + "offeringId LONG NOT NULL, "
                        + "teacherName TEXT(50) NOT NULL)");

                statement.executeUpdate(
                    "CREATE INDEX "
                        + "ix_tblCatalogTeacher_offeringId "
                        + "ON tblCatalogTeacher "
                        + "(offeringId)");
            }

            if (!tableExists(
                connection,
                SCHEDULE_TABLE)) {

                statement.executeUpdate(
                    "CREATE TABLE tblCatalogSchedule ("
                        + "catalogScheduleId COUNTER PRIMARY KEY, "
                        + "offeringId LONG NOT NULL, "
                        + "dayOfWeek LONG NOT NULL, "
                        + "startPeriod LONG NOT NULL, "
                        + "endPeriod LONG NOT NULL, "
                        + "startWeek LONG NOT NULL, "
                        + "endWeek LONG NOT NULL, "
                        + "weekPattern TEXT(20) NOT NULL)");

                statement.executeUpdate(
                    "CREATE INDEX "
                        + "ix_tblCatalogSchedule_offeringId "
                        + "ON tblCatalogSchedule "
                        + "(offeringId)");
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化全校课程目录表。",
                exception);
        }
    }

    /**
     * 首次运行时导入原来的内存目录。
     */
    private void seedIfEmpty(
        CourseCatalogRepository seedRepository) {

        try (Connection connection =
                 database.openConnection()) {

            if (countCourses(
                connection) > 0) {

                return;
            }

            connection.setAutoCommit(
                false);

            try {

                for (CourseCatalogRecord record
                    : seedRepository
                    .findCurrentSemesterCourses()) {

                    insertRecord(
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
                "无法初始化全校课程目录数据。",
                exception);
        }
    }

    private int countCourses(
        Connection connection)
        throws SQLException {

        String sql =
            "SELECT COUNT(*) AS recordCount "
                + "FROM tblCatalogCourse";

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
     * 保存一条目录记录。
     *
     * 内存目录中同一课程的不同教学班
     * 可能分成多条记录，因此课程只插入一次。
     */
    private void insertRecord(
        Connection connection,
        CourseCatalogRecord record)
        throws SQLException {

        CourseInfo course =
            record.course();

        if (!courseExists(
            connection,
            course.getCourseId())) {

            insertCourse(
                connection,
                course,
                record.departmentName());
        }

        for (OfferingInfo offering
            : course.getOfferings()) {

            Long existingCourseId =
                findOfferingCourseId(
                    connection,
                    offering.getOfferingId());

            if (existingCourseId == null) {

                insertOffering(
                    connection,
                    course.getCourseId(),
                    offering);

            } else if (existingCourseId
                != course.getCourseId()) {

                throw new IllegalStateException(
                    "全校课程目录教学班 ID 冲突："
                        + offering.getOfferingId());
            }
        }
    }

    private boolean courseExists(
        Connection connection,
        long courseId)
        throws SQLException {

        String sql =
            "SELECT COUNT(*) AS recordCount "
                + "FROM tblCatalogCourse "
                + "WHERE courseId = ?";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                courseId);

            try (ResultSet result =
                     statement.executeQuery()) {

                return result.next()
                    && result.getInt(
                    "recordCount") > 0;
            }
        }
    }

    private Long findOfferingCourseId(
        Connection connection,
        long offeringId)
        throws SQLException {

        String sql =
            "SELECT courseId "
                + "FROM tblCatalogOffering "
                + "WHERE offeringId = ?";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                offeringId);

            try (ResultSet result =
                     statement.executeQuery()) {

                return result.next()
                    ? result.getLong(
                    "courseId")
                    : null;
            }
        }
    }

    private void insertCourse(
        Connection connection,
        CourseInfo course,
        String departmentName)
        throws SQLException {

        String sql =
            "INSERT INTO tblCatalogCourse "
                + "(courseId, courseCode, courseName, "
                + "credits, courseType, departmentName) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
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
                departmentName);

            statement.executeUpdate();
        }
    }

    private void insertOffering(
        Connection connection,
        long courseId,
        OfferingInfo offering)
        throws SQLException {

        String sql =
            "INSERT INTO tblCatalogOffering "
                + "(offeringId, courseId, classNo, "
                + "locationName, campusName, "
                + "teachingLanguage, selectedCount, "
                + "capacity, availabilityStatus) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
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
            "INSERT INTO tblCatalogTeacher "
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
            "INSERT INTO tblCatalogSchedule "
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
