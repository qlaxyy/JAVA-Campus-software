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
 */
final class AccessCourseOfferingSettingsRepository
    implements CourseOfferingSettingsRepository {

    private static final String TABLE =
        "tblCourseOfferingSettings";

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
        long batchId,
        long offeringId) {

        String sql =
            "SELECT batchId, offeringId, capacity, openFlag "
                + "FROM tblCourseOfferingSettings "
                + "WHERE batchId = ? "
                + "AND offeringId = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                batchId);

            statement.setLong(
                2,
                offeringId);

            try (ResultSet result =
                     statement.executeQuery()) {

                if (!result.next()) {

                    return Optional.empty();
                }

                return Optional.of(
                    new CourseOfferingSettings(
                        result.getLong(
                            "batchId"),
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

        if (find(
            settings.batchId(),
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

        String sql =
            "INSERT INTO tblCourseOfferingSettings "
                + "(batchId, offeringId, capacity, openFlag) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                settings.batchId());

            statement.setLong(
                2,
                settings.offeringId());

            statement.setInt(
                3,
                settings.capacity());

            statement.setBoolean(
                4,
                settings.open());

            statement.executeUpdate();

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
                + "WHERE batchId = ? "
                + "AND offeringId = ?";

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
                settings.batchId());

            statement.setLong(
                4,
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
                        + "batchId LONG NOT NULL, "
                        + "offeringId LONG NOT NULL, "
                        + "capacity LONG NOT NULL, "
                        + "openFlag YESNO NOT NULL)");

                statement.executeUpdate(
                    "CREATE UNIQUE INDEX "
                        + "ux_tblCourseOfferingSettings "
                        + "ON tblCourseOfferingSettings "
                        + "(batchId, offeringId)");
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
