package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Access-backed teaching relation; replaces the former hard-coded mapping. */
final class AccessCourseTeacherAssignmentRepository implements CourseTeacherAssignmentRepository {
    private static final String TABLE = "tblCourseTeacherAssignment";
    private final AccessDatabase database;

    AccessCourseTeacherAssignmentRepository(AccessDatabase database) {
        this.database = Objects.requireNonNull(database);
        initializeSchema();
        seedIfEmpty();
    }

    public boolean isAssigned(String teacherUserId, long offeringId) {
        if (teacherUserId == null || teacherUserId.isBlank()) return false;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT assignmentId FROM tblCourseTeacherAssignment WHERE teacherUserId = ? AND offeringId = ?")) {
            statement.setString(1, teacherUserId.trim()); statement.setLong(2, offeringId);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        } catch (SQLException exception) { throw failure("无法查询教师任课关系。", exception); }
    }

    @Override
    public List<String> findTeacherUserIds(long offeringId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT teacherUserId FROM tblCourseTeacherAssignment "
                             + "WHERE offeringId = ? ORDER BY teacherUserId")) {
            statement.setLong(1, offeringId);
            try (ResultSet result = statement.executeQuery()) {
                List<String> teacherUserIds = new ArrayList<>();
                while (result.next()) teacherUserIds.add(result.getString("teacherUserId"));
                return List.copyOf(teacherUserIds);
            }
        } catch (SQLException exception) {
            throw failure("无法查询教学班任课教师。", exception);
        }
    }

    private void initializeSchema() {
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            if (!tableExists(connection, TABLE)) {
                statement.executeUpdate("CREATE TABLE tblCourseTeacherAssignment ("
                        + "assignmentId AUTOINCREMENT PRIMARY KEY, teacherUserId TEXT(36) NOT NULL, offeringId LONG NOT NULL)");
                statement.executeUpdate("CREATE UNIQUE INDEX ux_tblCourseTeacherAssignment_pair "
                        + "ON tblCourseTeacherAssignment (teacherUserId, offeringId)");
                statement.executeUpdate("CREATE INDEX ix_tblCourseTeacherAssignment_offering "
                        + "ON tblCourseTeacherAssignment (offeringId)");
            }
        } catch (SQLException exception) { throw failure("无法初始化教师任课关系表。", exception); }
    }

    private void seedIfEmpty() {
        try (Connection connection = database.openConnection();
             Statement count = connection.createStatement();
             ResultSet result = count.executeQuery("SELECT COUNT(*) FROM tblCourseTeacherAssignment")) {
            result.next(); if (result.getInt(1) > 0) return;
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tblCourseTeacherAssignment (teacherUserId, offeringId) VALUES (?, ?)")) {
                for (Map.Entry<String, Set<Long>> entry : InMemoryCourseTeacherAssignmentRepository.seeds().entrySet()) {
                    for (Long offeringId : entry.getValue()) {
                        insert.setString(1, entry.getKey()); insert.setLong(2, offeringId); insert.addBatch();
                    }
                }
                insert.executeBatch();
            }
        } catch (SQLException exception) { throw failure("无法初始化教师任课关系。", exception); }
    }

    private static boolean tableExists(Connection connection, String expected) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) if (expected.equalsIgnoreCase(tables.getString("TABLE_NAME"))) return true;
        }
        return false;
    }

    private IllegalStateException failure(String message, SQLException cause) {
        return new IllegalStateException(message + " 数据库：" + database.path(), cause);
    }
}
