package edu.seu.vcampus.server.module.course;

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
 * 教师与教学班任课关系的 Access 实现。
 */
final class AccessCourseTeacherAssignmentRepository
    implements CourseTeacherAssignmentRepository {

    private static final String TABLE =
        "tblOfferingTeacher";

    private static final String OLD_UNIQUE_INDEX =
        "ux_tblOfferingTeacher_offering_teacher";



    private final AccessDatabase database;

    AccessCourseTeacherAssignmentRepository(
        AccessDatabase database) {

        this.database =
            Objects.requireNonNull(
                database);

        initialiseSchema();
    }

    @Override
    public boolean isAssigned(
        String teacherUserId,
        long offeringId) {

        String userId =
            normalizeUserId(
                teacherUserId);

        if (userId == null
            || offeringId <= 0) {

            return false;
        }

        String sql =
            "SELECT offeringTeacherId "
                + "FROM tblOfferingTeacher "
                + "WHERE offeringId = ? "
                + "AND teacherUserId = ?";

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
                userId);

            try (ResultSet result =
                     statement.executeQuery()) {

                return result.next();
            }

        } catch (SQLException exception) {

            throw failure(
                "无法查询教师任课关系。",
                exception);
        }
    }

    @Override
    public List<Long> findOfferingIds(
        String teacherUserId) {

        String userId =
            normalizeUserId(
                teacherUserId);

        if (userId == null) {

            return List.of();
        }

        String sql =
            "SELECT offeringId "
                + "FROM tblOfferingTeacher "
                + "WHERE teacherUserId = ? "
                + "ORDER BY offeringId";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setString(
                1,
                userId);

            List<Long> offeringIds =
                new ArrayList<>();

            try (ResultSet result =
                     statement.executeQuery()) {

                while (result.next()) {

                    offeringIds.add(
                        result.getLong(
                            "offeringId"));
                }
            }

            return List.copyOf(
                offeringIds);

        } catch (SQLException exception) {

            throw failure(
                "无法查询教师负责的教学班。",
                exception);
        }
    }

    @Override
    public List<String> findTeacherUserIds(
        long offeringId) {

        if (offeringId <= 0) {

            return List.of();
        }

        String sql =
            "SELECT teacherUserId "
                + "FROM tblOfferingTeacher "
                + "WHERE offeringId = ? "
                + "AND teacherUserId IS NOT NULL "
                + "ORDER BY teacherUserId";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                offeringId);

            List<String> teacherUserIds =
                new ArrayList<>();

            try (ResultSet result =
                     statement.executeQuery()) {

                while (result.next()) {

                    String userId =
                        result.getString(
                            "teacherUserId");

                    if (userId != null
                        && !userId.isBlank()) {

                        teacherUserIds.add(
                            userId.trim());
                    }
                }
            }

            return List.copyOf(
                teacherUserIds);

        } catch (SQLException exception) {

            throw failure(
                "无法查询教学班任课教师。",
                exception);
        }
    }

    @Override
    public synchronized boolean assign(
        long offeringId,
        String teacherUserId,
        String teacherName) {

        String userId =
            normalizeUserId(
                teacherUserId);

        if (offeringId <= 0
            || userId == null) {

            return false;
        }

        if (isAssigned(
            userId,
            offeringId)) {

            return false;
        }

        String displayName =
            normalizeTeacherName(
                teacherName,
                userId);

        String sql =
            "INSERT INTO tblOfferingTeacher "
                + "(offeringId, teacherUserId, "
                + "teacherName) "
                + "VALUES (?, ?, ?)";

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
                userId);

            statement.setString(
                3,
                displayName);

            return statement.executeUpdate()
                > 0;

        } catch (SQLException exception) {

            throw failure(
                "无法添加教师任课关系。",
                exception);
        }
    }

    @Override
    public synchronized boolean remove(
        long offeringId,
        String teacherUserId) {

        String userId =
            normalizeUserId(
                teacherUserId);

        if (offeringId <= 0
            || userId == null) {

            return false;
        }

        String sql =
            "DELETE FROM tblOfferingTeacher "
                + "WHERE offeringId = ? "
                + "AND teacherUserId = ?";

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
                userId);

            return statement.executeUpdate()
                > 0;

        } catch (SQLException exception) {

            throw failure(
                "无法删除教师任课关系。",
                exception);
        }
    }

    /**
     * 初始化或升级任课关系表。
     */
    private void initialiseSchema() {

        try (Connection connection =
                 database.openConnection()) {

            boolean shouldCreateDemoAssignment;

            if (!tableExists(
                connection,
                TABLE)) {

                createTable(
                    connection);

                shouldCreateDemoAssignment =
                    true;

            } else {

                shouldCreateDemoAssignment =
                    migrateExistingTable(
                        connection);
            }

            /*
             * 只在新建表或首次迁移旧表时
             * 写入演示任课关系。
             *
             * 后续启动不能恢复管理员已删除的关系。
             */
            if (shouldCreateDemoAssignment) {

                ensureDemoAssignment(
                    connection);
            }

            removeLegacyUniqueIndex(
                connection);

        } catch (SQLException exception) {

            throw failure(
                "无法初始化教师任课关系表。",
                exception);
        }
    }

    /**
     * 创建任课关系表。
     */
    private void createTable(
        Connection connection)
        throws SQLException {

        try (Statement statement =
                 connection.createStatement()) {

            statement.executeUpdate(
                "CREATE TABLE tblOfferingTeacher ("
                    + "offeringTeacherId "
                    + "COUNTER PRIMARY KEY, "
                    + "offeringId LONG NOT NULL, "
                    + "teacherUserId TEXT(64), "
                    + "teacherName TEXT(50))");
        }
    }

    /**
     * 给旧表增加稳定的教师 userId。
     */
    /**
     * 给旧表增加稳定的教师 userId。
     *
     * @return 是否执行了首次迁移
     */
    private boolean migrateExistingTable(
        Connection connection)
        throws SQLException {

        if (columnExists(
            connection,
            TABLE,
            "teacherUserId")) {

            return false;
        }

        try (Statement statement =
                 connection.createStatement()) {

            statement.executeUpdate(
                "ALTER TABLE tblOfferingTeacher "
                    + "ADD COLUMN teacherUserId "
                    + "TEXT(64)");
        }

        return true;
    }

    /**
     * 保留原演示教师和教学班 1001 的关系。
     */
    private void ensureDemoAssignment(
        Connection connection)
        throws SQLException {

        String existingSql =
            "SELECT offeringTeacherId "
                + "FROM tblOfferingTeacher "
                + "WHERE offeringId = ? "
                + "AND teacherUserId = ?";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     existingSql)) {

            statement.setLong(
                1,
                1001L);

            statement.setString(
                2,
                "U-TEACHER-001");

            try (ResultSet result =
                     statement.executeQuery()) {

                if (result.next()) {

                    return;
                }
            }
        }

        Long unassignedRecordId =
            findFirstUnassignedRecord(
                connection,
                1001L);

        if (unassignedRecordId != null) {

            String updateSql =
                "UPDATE tblOfferingTeacher "
                    + "SET teacherUserId = ? "
                    + "WHERE offeringTeacherId = ?";

            try (PreparedStatement statement =
                     connection.prepareStatement(
                         updateSql)) {

                statement.setString(
                    1,
                    "U-TEACHER-001");

                statement.setLong(
                    2,
                    unassignedRecordId);

                statement.executeUpdate();
            }

            return;
        }

        String insertSql =
            "INSERT INTO tblOfferingTeacher "
                + "(offeringId, teacherUserId, "
                + "teacherName) "
                + "VALUES (?, ?, ?)";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     insertSql)) {

            statement.setLong(
                1,
                1001L);

            statement.setString(
                2,
                "U-TEACHER-001");

            statement.setString(
                3,
                "演示教师");

            statement.executeUpdate();
        }
    }

    private Long findFirstUnassignedRecord(
        Connection connection,
        long offeringId)
        throws SQLException {

        String sql =
            "SELECT TOP 1 offeringTeacherId "
                + "FROM tblOfferingTeacher "
                + "WHERE offeringId = ? "
                + "AND teacherUserId IS NULL "
                + "ORDER BY offeringTeacherId";

        try (PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                offeringId);

            try (ResultSet result =
                     statement.executeQuery()) {

                if (result.next()) {

                    return result.getLong(
                        "offeringTeacherId");
                }
            }
        }

        return null;
    }

    /**
     * 删除旧版本中不兼容 NULL 字段的唯一索引。
     *
     * 任课关系唯一性由 assign() 的重复检查保证。
     */
    private void removeLegacyUniqueIndex(
        Connection connection)
        throws SQLException {

        if (!indexExists(
            connection,
            TABLE,
            OLD_UNIQUE_INDEX)) {

            return;
        }

        try (Statement statement =
                 connection.createStatement()) {

            statement.executeUpdate(
                "DROP INDEX "
                    + OLD_UNIQUE_INDEX
                    + " ON tblOfferingTeacher");
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

    private boolean columnExists(
        Connection connection,
        String tableName,
        String columnName)
        throws SQLException {

        DatabaseMetaData metadata =
            connection.getMetaData();

        try (ResultSet columns =
                 metadata.getColumns(
                     null,
                     null,
                     "%",
                     "%")) {

            while (columns.next()) {

                String actualTable =
                    columns.getString(
                        "TABLE_NAME");

                String actualColumn =
                    columns.getString(
                        "COLUMN_NAME");

                if (tableName.equalsIgnoreCase(
                    actualTable)
                    && columnName.equalsIgnoreCase(
                    actualColumn)) {

                    return true;
                }
            }
        }

        return false;
    }

    private boolean indexExists(
        Connection connection,
        String tableName,
        String indexName)
        throws SQLException {

        DatabaseMetaData metadata =
            connection.getMetaData();

        try (ResultSet indexes =
                 metadata.getIndexInfo(
                     null,
                     null,
                     tableName,
                     false,
                     false)) {

            while (indexes.next()) {

                String actualName =
                    indexes.getString(
                        "INDEX_NAME");

                if (indexName.equalsIgnoreCase(
                    actualName)) {

                    return true;
                }
            }
        }

        return false;
    }

    private String normalizeUserId(
        String teacherUserId) {

        if (teacherUserId == null
            || teacherUserId.isBlank()) {

            return null;
        }

        return teacherUserId.trim();
    }

    private String normalizeTeacherName(
        String teacherName,
        String fallback) {

        String result =
            teacherName == null
                || teacherName.isBlank()
                ? fallback
                : teacherName.trim();

        /*
         * 兼容旧表 teacherName TEXT(50)。
         */
        return result.length() <= 50
            ? result
            : result.substring(
            0,
            50);
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
