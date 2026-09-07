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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Microsoft Access 选课记录仓库。
 */
final class AccessCourseEnrollmentRepository
    implements CourseEnrollmentRepository {

    private static final String TABLE =
        "tblEnrollment";

    private static final String SELECTED =
        "SELECTED";

    private static final String DROPPED =
        "DROPPED";

    private final AccessDatabase database;

    AccessCourseEnrollmentRepository(
        AccessDatabase database) {

        this.database =
            Objects.requireNonNull(
                database);

        initialiseSchema();
    }

    @Override
    public boolean isOfferingSelected(
        String userId,
        long offeringId) {

        String sql =
            "SELECT COUNT(*) AS recordCount "
                + "FROM tblEnrollment "
                + "WHERE userId = ? "
                + "AND offeringId = ? "
                + "AND enrollmentStatus = ?";

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
                offeringId);

            statement.setString(
                3,
                SELECTED);

            try (ResultSet result =
                     statement.executeQuery()) {

                return result.next()
                    && result.getInt(
                    "recordCount") > 0;
            }

        } catch (SQLException exception) {

            throw failure(
                "无法检查选课状态。",
                exception);
        }
    }

    @Override
    public Set<Long> findSelectedOfferingIds(
        String userId) {

        String sql =
            "SELECT offeringId "
                + "FROM tblEnrollment "
                + "WHERE userId = ? "
                + "AND enrollmentStatus = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setString(
                1,
                userId);

            statement.setString(
                2,
                SELECTED);

            try (ResultSet result =
                     statement.executeQuery()) {

                Set<Long> offeringIds =
                    new HashSet<>();

                while (result.next()) {

                    offeringIds.add(
                        result.getLong(
                            "offeringId"));
                }

                return Set.copyOf(
                    offeringIds);
            }

        } catch (SQLException exception) {

            throw failure(
                "无法读取已选教学班。",
                exception);
        }
    }

    @Override
    public List<CourseEnrollmentRecord>
    findSelectedEnrollments(
        String userId) {

        String sql =
            "SELECT enrollmentId, userId, studentId, "
                + "selectedBatchId, offeringId "
                + "FROM tblEnrollment "
                + "WHERE userId = ? "
                + "AND enrollmentStatus = ? "
                + "ORDER BY enrollmentId";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setString(
                1,
                userId);

            statement.setString(
                2,
                SELECTED);

            try (ResultSet result =
                     statement.executeQuery()) {

                return readRecords(
                    result);
            }

        } catch (SQLException exception) {

            throw failure(
                "无法读取学生选课记录。",
                exception);
        }
    }

    @Override
    public CourseEnrollmentRecord
    findSelectedEnrollment(
        String userId,
        long enrollmentId) {

        String sql =
            "SELECT enrollmentId, userId, studentId, "
                + "selectedBatchId, offeringId "
                + "FROM tblEnrollment "
                + "WHERE userId = ? "
                + "AND enrollmentId = ? "
                + "AND enrollmentStatus = ?";

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
                enrollmentId);

            statement.setString(
                3,
                SELECTED);

            try (ResultSet result =
                     statement.executeQuery()) {

                return result.next()
                    ? readRecord(
                    result)
                    : null;
            }

        } catch (SQLException exception) {

            throw failure(
                "无法读取选课记录。",
                exception);
        }
    }

    @Override
    public List<CourseEnrollmentRecord>
    findSelectedEnrollmentsByOffering(
        long offeringId) {

        String sql =
            "SELECT enrollmentId, userId, studentId, "
                + "selectedBatchId, offeringId "
                + "FROM tblEnrollment "
                + "WHERE offeringId = ? "
                + "AND enrollmentStatus = ? "
                + "ORDER BY enrollmentId";

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
                SELECTED);

            try (ResultSet result =
                     statement.executeQuery()) {

                return readRecords(
                    result);
            }

        } catch (SQLException exception) {

            throw failure(
                "无法读取教学班选课记录。",
                exception);
        }
    }

    @Override
    public int countAdditionalSelections(
        long offeringId) {

        String sql =
            "SELECT COUNT(*) AS selectedCount "
                + "FROM tblEnrollment "
                + "WHERE offeringId = ? "
                + "AND enrollmentStatus = ?";

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
                SELECTED);

            try (ResultSet result =
                     statement.executeQuery()) {

                return result.next()
                    ? result.getInt(
                    "selectedCount")
                    : 0;
            }

        } catch (SQLException exception) {

            throw failure(
                "无法统计教学班选课人数。",
                exception);
        }
    }

    @Override
    public synchronized void select(
        String userId,
        String studentId,
        long batchId,
        long offeringId) {

        if (isOfferingSelected(
            userId,
            offeringId)) {

            return;
        }

        Long existingEnrollmentId =
            findExistingEnrollmentId(
                userId,
                offeringId);

        if (existingEnrollmentId == null) {

            insertEnrollment(
                userId,
                studentId,
                batchId,
                offeringId);

        } else {

            reactivateEnrollment(
                existingEnrollmentId,
                studentId,
                batchId);
        }
    }

    @Override
    public synchronized boolean drop(
        String userId,
        long enrollmentId) {

        String sql =
            "UPDATE tblEnrollment "
                + "SET enrollmentStatus = ?, "
                + "droppedAt = ? "
                + "WHERE userId = ? "
                + "AND enrollmentId = ? "
                + "AND enrollmentStatus = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setString(
                1,
                DROPPED);

            statement.setTimestamp(
                2,
                Timestamp.valueOf(
                    LocalDateTime.now()));

            statement.setString(
                3,
                userId);

            statement.setLong(
                4,
                enrollmentId);

            statement.setString(
                5,
                SELECTED);

            return statement.executeUpdate()
                > 0;

        } catch (SQLException exception) {

            throw failure(
                "无法保存退课记录。",
                exception);
        }
    }

    /**
     * 查询学生是否曾经选过这个教学班。
     */
    private Long findExistingEnrollmentId(
        String userId,
        long offeringId) {

        String sql =
            "SELECT TOP 1 enrollmentId "
                + "FROM tblEnrollment "
                + "WHERE userId = ? "
                + "AND offeringId = ? "
                + "ORDER BY enrollmentId DESC";

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
                offeringId);

            try (ResultSet result =
                     statement.executeQuery()) {

                return result.next()
                    ? result.getLong(
                    "enrollmentId")
                    : null;
            }

        } catch (SQLException exception) {

            throw failure(
                "无法检查历史选课记录。",
                exception);
        }
    }

    /**
     * 新增选课记录。
     */
    private void insertEnrollment(
        String userId,
        String studentId,
        long batchId,
        long offeringId) {

        String sql =
            "INSERT INTO tblEnrollment "
                + "(userId, studentId, selectedBatchId, "
                + "offeringId, enrollmentStatus, "
                + "selectedAt, droppedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setString(
                1,
                userId);

            statement.setString(
                2,
                studentId);

            statement.setLong(
                3,
                batchId);

            statement.setLong(
                4,
                offeringId);

            statement.setString(
                5,
                SELECTED);

            statement.setTimestamp(
                6,
                Timestamp.valueOf(
                    LocalDateTime.now()));

            statement.setNull(
                7,
                Types.TIMESTAMP);

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法保存选课记录。",
                exception);
        }
    }

    /**
     * 将已经退课的记录恢复为已选状态。
     */
    private void reactivateEnrollment(
        long enrollmentId,
        String studentId,
        long batchId) {

        String sql =
            "UPDATE tblEnrollment "
                + "SET studentId = ?, "
                + "selectedBatchId = ?, "
                + "enrollmentStatus = ?, "
                + "selectedAt = ?, "
                + "droppedAt = ? "
                + "WHERE enrollmentId = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setString(
                1,
                studentId);

            statement.setLong(
                2,
                batchId);

            statement.setString(
                3,
                SELECTED);

            statement.setTimestamp(
                4,
                Timestamp.valueOf(
                    LocalDateTime.now()));

            statement.setNull(
                5,
                Types.TIMESTAMP);

            statement.setLong(
                6,
                enrollmentId);

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法恢复选课记录。",
                exception);
        }
    }

    /**
     * 读取多条选课记录。
     */
    private List<CourseEnrollmentRecord> readRecords(
        ResultSet result)
        throws SQLException {

        List<CourseEnrollmentRecord> records =
            new ArrayList<>();

        while (result.next()) {

            records.add(
                readRecord(
                    result));
        }

        return List.copyOf(
            records);
    }

    /**
     * 读取一条选课记录。
     */
    private CourseEnrollmentRecord readRecord(
        ResultSet result)
        throws SQLException {

        return new CourseEnrollmentRecord(
            result.getLong(
                "enrollmentId"),
            result.getString(
                "userId"),
            result.getString(
                "studentId"),
            result.getLong(
                "selectedBatchId"),
            result.getLong(
                "offeringId"));
    }

    /**
     * 初始化选课记录表。
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
                    "CREATE TABLE tblEnrollment ("
                        + "enrollmentId COUNTER PRIMARY KEY, "
                        + "userId TEXT(100) NOT NULL, "
                        + "studentId TEXT(100) NOT NULL, "
                        + "selectedBatchId LONG NOT NULL, "
                        + "offeringId LONG NOT NULL, "
                        + "enrollmentStatus TEXT(20) NOT NULL, "
                        + "selectedAt DATETIME NOT NULL, "
                        + "droppedAt DATETIME)");

                statement.executeUpdate(
                    "CREATE INDEX "
                        + "ix_tblEnrollment_userId "
                        + "ON tblEnrollment (userId)");

                statement.executeUpdate(
                    "CREATE INDEX "
                        + "ix_tblEnrollment_offeringId "
                        + "ON tblEnrollment (offeringId)");

                statement.executeUpdate(
                    "CREATE UNIQUE INDEX "
                        + "ux_tblEnrollment_user_offering "
                        + "ON tblEnrollment "
                        + "(userId, offeringId)");
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化选课记录表。",
                exception);
        }
    }

    /**
     * 判断表是否存在。
     */
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
