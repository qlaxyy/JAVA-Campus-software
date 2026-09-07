package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.SelectionBatchStatus;
import edu.seu.vcampus.common.course.SelectionBatchType;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;

/**
 * Access 选课批次设置仓库。
 */
final class AccessCourseBatchSettingsRepository
    implements CourseBatchSettingsRepository {

    private static final String TABLE =
        "tblCourseBatchSettings";

    private final AccessDatabase database;

    AccessCourseBatchSettingsRepository(
        AccessDatabase database) {

        this.database =
            Objects.requireNonNull(
                database);

        initialiseSchema();
    }

    @Override
    public Optional<CourseBatchSettings> find(
        long batchId) {

        String sql =
            "SELECT batchId, semester, batchName, "
                + "batchType, startTime, endTime, "
                + "batchStatus, allowSelect, allowDrop "
                + "FROM tblCourseBatchSettings "
                + "WHERE batchId = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                batchId);

            try (ResultSet result =
                     statement.executeQuery()) {

                if (!result.next()) {

                    return Optional.empty();
                }

                return Optional.of(
                    new CourseBatchSettings(
                        result.getLong(
                            "batchId"),
                        result.getString(
                            "semester"),
                        result.getString(
                            "batchName"),
                        SelectionBatchType.valueOf(
                            result.getString(
                                "batchType")),
                        result.getTimestamp(
                                "startTime")
                            .toLocalDateTime(),
                        result.getTimestamp(
                                "endTime")
                            .toLocalDateTime(),
                        SelectionBatchStatus.valueOf(
                            result.getString(
                                "batchStatus")),
                        result.getBoolean(
                            "allowSelect"),
                        result.getBoolean(
                            "allowDrop")));
            }

        } catch (SQLException exception) {

            throw failure(
                "无法读取选课批次设置。",
                exception);
        }
    }

    @Override
    public synchronized void save(
        CourseBatchSettings settings) {

        if (find(
            settings.batchId())
            .isPresent()) {

            update(
                settings);

        } else {

            insert(
                settings);
        }
    }

    private void insert(
        CourseBatchSettings settings) {

        String sql =
            "INSERT INTO tblCourseBatchSettings "
                + "(batchId, semester, batchName, "
                + "batchType, startTime, endTime, "
                + "batchStatus, allowSelect, allowDrop) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            writeValues(
                statement,
                settings,
                false);

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法新增选课批次设置。",
                exception);
        }
    }

    private void update(
        CourseBatchSettings settings) {

        String sql =
            "UPDATE tblCourseBatchSettings "
                + "SET semester = ?, "
                + "batchName = ?, "
                + "batchType = ?, "
                + "startTime = ?, "
                + "endTime = ?, "
                + "batchStatus = ?, "
                + "allowSelect = ?, "
                + "allowDrop = ? "
                + "WHERE batchId = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            writeValues(
                statement,
                settings,
                true);

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法更新选课批次设置。",
                exception);
        }
    }

    /**
     * 给新增或更新语句设置参数。
     */
    private void writeValues(
        PreparedStatement statement,
        CourseBatchSettings settings,
        boolean update)
        throws SQLException {

        if (update) {

            statement.setString(
                1,
                settings.semester());

            statement.setString(
                2,
                settings.batchName());

            statement.setString(
                3,
                settings.batchType()
                    .name());

            statement.setTimestamp(
                4,
                Timestamp.valueOf(
                    settings.startTime()));

            statement.setTimestamp(
                5,
                Timestamp.valueOf(
                    settings.endTime()));

            statement.setString(
                6,
                settings.status()
                    .name());

            statement.setBoolean(
                7,
                settings.allowSelect());

            statement.setBoolean(
                8,
                settings.allowDrop());

            statement.setLong(
                9,
                settings.batchId());

        } else {

            statement.setLong(
                1,
                settings.batchId());

            statement.setString(
                2,
                settings.semester());

            statement.setString(
                3,
                settings.batchName());

            statement.setString(
                4,
                settings.batchType()
                    .name());

            statement.setTimestamp(
                5,
                Timestamp.valueOf(
                    settings.startTime()));

            statement.setTimestamp(
                6,
                Timestamp.valueOf(
                    settings.endTime()));

            statement.setString(
                7,
                settings.status()
                    .name());

            statement.setBoolean(
                8,
                settings.allowSelect());

            statement.setBoolean(
                9,
                settings.allowDrop());
        }
    }

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
                    "CREATE TABLE tblCourseBatchSettings ("
                        + "batchId LONG NOT NULL, "
                        + "semester TEXT(30) NOT NULL, "
                        + "batchName TEXT(100) NOT NULL, "
                        + "batchType TEXT(30) NOT NULL, "
                        + "startTime DATETIME NOT NULL, "
                        + "endTime DATETIME NOT NULL, "
                        + "batchStatus TEXT(30) NOT NULL, "
                        + "allowSelect YESNO NOT NULL, "
                        + "allowDrop YESNO NOT NULL)");

                statement.executeUpdate(
                    "CREATE UNIQUE INDEX "
                        + "ux_tblCourseBatchSettings_batchId "
                        + "ON tblCourseBatchSettings "
                        + "(batchId)");
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化选课批次设置表。",
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
