package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;

/**
 * Access 教学班设置仓库。
 *
 * 教学班设置按照 offeringId 全局保存，
 * 与选课批次无关。
 */
final class AccessCourseOfferingSettingsRepository
    implements CourseOfferingSettingsRepository {

    private static final String TABLE =
        "tblCourseOfferingSettings";

    private static final String LEGACY_BATCH_COLUMN =
        "batchId";

    private final AccessDatabase database;

    AccessCourseOfferingSettingsRepository(
        AccessDatabase database) {

        this.database =
            Objects.requireNonNull(
                database);

        initialiseSchema();
    }

    @Override
    public Optional<CourseOfferingSettings> find(
        long offeringId) {

        String sql =
            "SELECT offeringId, capacity, openFlag "
                + "FROM tblCourseOfferingSettings "
                + "WHERE offeringId = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                offeringId);

            try (ResultSet result =
                     statement.executeQuery()) {

                if (!result.next()) {

                    return Optional.empty();
                }

                return Optional.of(
                    new CourseOfferingSettings(
                        result.getLong(
                            "offeringId"),
                        result.getInt(
                            "capacity"),
                        result.getBoolean(
                            "openFlag")));
            }

        } catch (SQLException exception) {

            throw failure(
                "无法读取教学班设置。",
                exception);
        }
    }

    @Override
    public synchronized void save(
        CourseOfferingSettings settings) {

        Objects.requireNonNull(
            settings,
            "settings must not be null");

        if (find(
            settings.offeringId())
            .isPresent()) {

            update(
                settings);

        } else {

            insert(
                settings);
        }
    }

    private void insert(
        CourseOfferingSettings settings) {

        try (Connection connection =
                 database.openConnection()) {

            boolean legacySchema =
                columnExists(
                    connection,
                    TABLE,
                    LEGACY_BATCH_COLUMN);

            String sql =
                legacySchema
                    ? "INSERT INTO tblCourseOfferingSettings "
                    + "(batchId, offeringId, "
                    + "capacity, openFlag) "
                    + "VALUES (?, ?, ?, ?)"
                    : "INSERT INTO tblCourseOfferingSettings "
                    + "(offeringId, capacity, openFlag) "
                    + "VALUES (?, ?, ?)";

            try (PreparedStatement statement =
                     connection.prepareStatement(
                         sql)) {

                int index =
                    1;

                if (legacySchema) {

                    /*
                     * 仅兼容旧数据库的 NOT NULL 字段。
                     * 该值不参与任何业务判断。
                     */
                    statement.setLong(
                        index++,
                        0L);
                }

                statement.setLong(
                    index++,
                    settings.offeringId());

                statement.setInt(
                    index++,
                    settings.capacity());

                statement.setBoolean(
                    index,
                    settings.open());

                statement.executeUpdate();
            }

        } catch (SQLException exception) {

            throw failure(
                "无法新增教学班设置。",
                exception);
        }
    }

    private void update(
        CourseOfferingSettings settings) {

        String sql =
            "UPDATE tblCourseOfferingSettings "
                + "SET capacity = ?, openFlag = ? "
                + "WHERE offeringId = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setInt(
                1,
                settings.capacity());

            statement.setBoolean(
                2,
                settings.open());

            statement.setLong(
                3,
                settings.offeringId());

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法更新教学班设置。",
                exception);
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
                    "CREATE TABLE tblCourseOfferingSettings ("
                        + "offeringId LONG NOT NULL, "
                        + "capacity LONG NOT NULL, "
                        + "openFlag YESNO NOT NULL)");

                statement.executeUpdate(
                    "CREATE UNIQUE INDEX "
                        + "ux_tblCourseOfferingSettings_offeringId "
                        + "ON tblCourseOfferingSettings "
                        + "(offeringId)");
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化教学班设置表。",
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

    private boolean columnExists(
        Connection connection,
        String expectedTable,
        String expectedColumn)
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

                if (expectedTable.equalsIgnoreCase(
                    columns.getString(
                        "TABLE_NAME"))
                    && expectedColumn.equalsIgnoreCase(
                    columns.getString(
                        "COLUMN_NAME"))) {

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
