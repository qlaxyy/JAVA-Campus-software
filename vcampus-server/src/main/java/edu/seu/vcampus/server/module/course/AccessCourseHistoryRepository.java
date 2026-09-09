package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Microsoft Access 学生历史修读记录仓库。
 */
@SuppressWarnings({
    "SqlNoDataSourceInspection",
    "SqlResolve",
    "SqlDialectInspection"
})
final class AccessCourseHistoryRepository
    implements CourseHistoryRepository {

    private static final String TABLE =
        "tblCourseHistory";

    private static final String DEMO_STUDENT_ID =
        "20260001";

    private final AccessDatabase database;

    AccessCourseHistoryRepository(
        AccessDatabase database) {

        this.database =
            Objects.requireNonNull(
                database);

        initialiseSchema();
        seedIfEmpty();
    }

    @Override
    public boolean hasTakenCourse(
        String userId,
        long courseId) {

        String sql =
            "SELECT COUNT(*) AS recordCount "
                + "FROM tblCourseHistory "
                + "WHERE userId = ? "
                + "AND courseId = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setString(
                1,
                userId);

            statement.setLong(
                2,
                courseId);

            try (ResultSet result =
                     statement.executeQuery()) {

                return result.next()
                    && result.getInt(
                    "recordCount") > 0;
            }

        } catch (SQLException exception) {

            throw failure(
                "无法检查历史修读记录。",
                exception);
        }
    }

    /**
     * 初始化历史修读记录表。
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
                    "CREATE TABLE tblCourseHistory ("
                        + "historyId COUNTER PRIMARY KEY, "
                        + "userId TEXT(100) NOT NULL, "
                        + "courseId LONG NOT NULL)");

                statement.executeUpdate(
                    "CREATE UNIQUE INDEX "
                        + "ux_tblCourseHistory_user_course "
                        + "ON tblCourseHistory "
                        + "(userId, courseId)");

                statement.executeUpdate(
                    "CREATE INDEX "
                        + "ix_tblCourseHistory_courseId "
                        + "ON tblCourseHistory "
                        + "(courseId)");
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化历史修读记录表。",
                exception);
        }
    }

    /**
     * 首次运行时写入原有演示学生的历史课程。
     */
    private void seedIfEmpty() {

        try (Connection connection =
                 database.openConnection()) {

            if (countRecords(
                connection) > 0) {

                return;
            }

            connection.setAutoCommit(
                false);

            try {

                insertHistory(
                    connection,
                    DEMO_STUDENT_ID,
                    101L);

                insertHistory(
                    connection,
                    DEMO_STUDENT_ID,
                    201L);

                connection.commit();

            } catch (SQLException exception) {

                connection.rollback();
                throw exception;
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化历史修读数据。",
                exception);
        }
    }

    private int countRecords(
        Connection connection)
        throws SQLException {

        String sql =
            "SELECT COUNT(*) AS recordCount "
                + "FROM tblCourseHistory";

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

    private void insertHistory(
        Connection connection,
        String userId,
        long courseId)
        throws SQLException {

        String sql =
            "INSERT INTO tblCourseHistory "
                + "(userId, courseId) "
                + "VALUES (?, ?)";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setString(
                1,
                userId);

            statement.setLong(
                2,
                courseId);

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
