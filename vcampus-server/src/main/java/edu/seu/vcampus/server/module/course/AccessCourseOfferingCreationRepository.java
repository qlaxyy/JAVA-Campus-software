package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.ScheduleInfo;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Access 教学班新增仓库。
 *
 * 教学班和首条上课时间在同一个事务中写入，避免留下
 * 没有时间信息的半成品教学班。
 */
final class AccessCourseOfferingCreationRepository
    implements CourseOfferingCreationRepository {

    private final AccessDatabase database;

    AccessCourseOfferingCreationRepository(
        AccessDatabase database) {

        this.database = Objects.requireNonNull(database);
    }

    @Override
    public synchronized long create(
        long courseId,
        String classNo,
        String locationName,
        String campusName,
        String teachingLanguage,
        int capacity,
        ScheduleInfo schedule) {

        try (Connection connection =
                 database.openConnection()) {

            connection.setAutoCommit(false);

            try {
                if (!courseExists(connection, courseId)) {
                    throw new IllegalArgumentException(
                        "课程不存在。");
                }
                if (classNumberExists(
                    connection,
                    courseId,
                    classNo)) {
                    throw new IllegalArgumentException(
                        "该课程已经存在相同班号。");
                }

                long offeringId = nextOfferingId(connection);
                insertOffering(
                    connection,
                    offeringId,
                    courseId,
                    classNo,
                    locationName,
                    campusName,
                    teachingLanguage,
                    capacity);
                insertSchedule(
                    connection,
                    offeringId,
                    schedule);
                connection.commit();
                return offeringId;

            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }

        } catch (SQLException exception) {
            throw new IllegalStateException(
                "无法新增教学班。",
                exception);
        }
    }

    private boolean courseExists(
        Connection connection,
        long courseId)
        throws SQLException {

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     "SELECT courseId FROM tblCourse "
                         + "WHERE courseId = ?")) {
            statement.setLong(1, courseId);
            try (ResultSet result =
                     statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private boolean classNumberExists(
        Connection connection,
        long courseId,
        String classNo)
        throws SQLException {

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     "SELECT offeringId "
                         + "FROM tblCourseOffering "
                         + "WHERE courseId = ? AND classNo = ?")) {
            statement.setLong(1, courseId);
            statement.setString(2, classNo);
            try (ResultSet result =
                     statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private long nextOfferingId(
        Connection connection)
        throws SQLException {

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     "SELECT MAX(offeringId) AS maxId "
                         + "FROM tblCourseOffering");
             ResultSet result = statement.executeQuery()) {

            long maxId = result.next()
                ? result.getLong("maxId")
                : 0L;
            return Math.max(maxId + 1L, 1L);
        }
    }

    private void insertOffering(
        Connection connection,
        long offeringId,
        long courseId,
        String classNo,
        String locationName,
        String campusName,
        String teachingLanguage,
        int capacity)
        throws SQLException {

        String sql =
            "INSERT INTO tblCourseOffering "
                + "(offeringId, courseId, classNo, "
                + "locationName, campusName, teachingLanguage, "
                + "selectedCount, capacity, availabilityStatus) "
                + "VALUES (?, ?, ?, ?, ?, ?, 0, ?, 'AVAILABLE')";

        try (PreparedStatement statement =
                 connection.prepareStatement(sql)) {
            statement.setLong(1, offeringId);
            statement.setLong(2, courseId);
            statement.setString(3, classNo);
            statement.setString(4, locationName);
            statement.setString(5, campusName);
            statement.setString(6, teachingLanguage);
            statement.setInt(7, capacity);
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
                + "(offeringId, dayOfWeek, startPeriod, endPeriod, "
                + "startWeek, endWeek, weekPattern) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                 connection.prepareStatement(sql)) {
            statement.setLong(1, offeringId);
            statement.setInt(2, schedule.getDayOfWeek());
            statement.setInt(3, schedule.getStartPeriod());
            statement.setInt(4, schedule.getEndPeriod());
            statement.setInt(5, schedule.getStartWeek());
            statement.setInt(6, schedule.getEndWeek());
            statement.setString(7, schedule.getWeekPattern());
            statement.executeUpdate();
        }
    }
}
