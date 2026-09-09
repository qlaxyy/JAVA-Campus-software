package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Access 教务操作日志仓库。
 */
final class AccessCourseAdminAuditRepository
    implements CourseAdminAuditRepository {

    private static final String TABLE =
        "tblCourseAdminAudit";

    private final AccessDatabase database;

    AccessCourseAdminAuditRepository(
        AccessDatabase database) {

        this.database =
            Objects.requireNonNull(
                database);

        initialiseSchema();
    }

    @Override
    public synchronized void append(
        String operatorUsername,
        String studentId,
        CourseAdminOperationType operationType,
        Long batchId,
        Long offeringId,
        Long enrollmentId,
        String details,
        LocalDateTime operatedAt) {

        String sql =
            "INSERT INTO tblCourseAdminAudit "
                + "(operatorUsername, studentId, "
                + "operationType, batchId, offeringId, "
                + "enrollmentId, detailText, operatedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setString(
                1,
                operatorUsername);

            statement.setString(
                2,
                studentId);

            statement.setString(
                3,
                operationType.name());

            setNullableLong(
                statement,
                4,
                batchId);

            setNullableLong(
                statement,
                5,
                offeringId);

            setNullableLong(
                statement,
                6,
                enrollmentId);

            statement.setString(
                7,
                details);

            statement.setTimestamp(
                8,
                Timestamp.valueOf(
                    operatedAt));

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法保存教务操作日志。",
                exception);
        }
    }

    @Override
    public List<CourseAdminAuditRecord> findAll() {

        String sql =
            "SELECT operationId, operatorUsername, "
                + "studentId, operationType, "
                + "batchId, offeringId, enrollmentId, "
                + "detailText, operatedAt "
                + "FROM tblCourseAdminAudit "
                + "ORDER BY operationId DESC";

        try (Connection connection =
                 database.openConnection();
             Statement statement =
                 connection.createStatement();
             ResultSet result =
                 statement.executeQuery(
                     sql)) {

            List<CourseAdminAuditRecord> records =
                new ArrayList<>();

            while (result.next()) {

                records.add(
                    new CourseAdminAuditRecord(
                        result.getLong(
                            "operationId"),
                        result.getString(
                            "operatorUsername"),
                        result.getString(
                            "studentId"),
                        CourseAdminOperationType
                            .valueOf(
                                result.getString(
                                    "operationType")),
                        nullableLong(
                            result,
                            "batchId"),
                        nullableLong(
                            result,
                            "offeringId"),
                        nullableLong(
                            result,
                            "enrollmentId"),
                        result.getString(
                            "detailText"),
                        result.getTimestamp(
                                "operatedAt")
                            .toLocalDateTime()));
            }

            return List.copyOf(
                records);

        } catch (SQLException exception) {

            throw failure(
                "无法读取教务操作日志。",
                exception);
        }
    }

    /**
     * 设置可以为空的 Long 参数。
     */
    private void setNullableLong(
        PreparedStatement statement,
        int index,
        Long value)
        throws SQLException {

        if (value == null) {

            statement.setNull(
                index,
                Types.INTEGER);

        } else {

            statement.setLong(
                index,
                value);
        }
    }

    /**
     * 读取可以为空的 Long 字段。
     */
    private Long nullableLong(
        ResultSet result,
        String columnName)
        throws SQLException {

        long value =
            result.getLong(
                columnName);

        return result.wasNull()
            ? null
            : value;
    }

    /**
     * 初始化日志表。
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
                    "CREATE TABLE tblCourseAdminAudit ("
                        + "operationId COUNTER PRIMARY KEY, "
                        + "operatorUsername TEXT(100) NOT NULL, "
                        + "studentId TEXT(100) NOT NULL, "
                        + "operationType TEXT(40) NOT NULL, "
                        + "batchId LONG, "
                        + "offeringId LONG, "
                        + "enrollmentId LONG, "
                        + "detailText TEXT(255) NOT NULL, "
                        + "operatedAt DATETIME NOT NULL)");

                statement.executeUpdate(
                    "CREATE INDEX "
                        + "ix_tblCourseAdminAudit_operatedAt "
                        + "ON tblCourseAdminAudit "
                        + "(operatedAt)");
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化教务操作日志表。",
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
