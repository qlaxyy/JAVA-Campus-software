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
 * Access 课程基本信息设置仓库。
 *
 * 课程设置按照 courseId 全局保存，
 * 与选课批次无关。
 */
final class AccessCourseSettingsRepository
    implements CourseSettingsRepository {

    private static final String TABLE =
        "tblCourseSettings";

    private static final String LEGACY_BATCH_COLUMN =
        "sourceBatchId";

    private final AccessDatabase database;

    AccessCourseSettingsRepository(
        AccessDatabase database) {

        this.database =
            Objects.requireNonNull(
                database);

        initialiseSchema();
    }

    @Override
    public Optional<CourseSettings> find(
        long courseId) {

        String sql =
            "SELECT courseId, courseCode, "
                + "courseName, credits, courseType "
                + "FROM tblCourseSettings "
                + "WHERE courseId = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                courseId);

            try (ResultSet result =
                     statement.executeQuery()) {

                if (!result.next()) {

                    return Optional.empty();
                }

                return Optional.of(
                    new CourseSettings(
                        result.getLong(
                            "courseId"),
                        result.getString(
                            "courseCode"),
                        result.getString(
                            "courseName"),
                        result.getDouble(
                            "credits"),
                        result.getString(
                            "courseType")));
            }

        } catch (SQLException exception) {

            throw failure(
                "无法读取课程基本信息设置。",
                exception);
        }
    }

    @Override
    public synchronized void save(
        CourseSettings settings) {

        Objects.requireNonNull(
            settings,
            "settings must not be null");

        if (find(
            settings.courseId())
            .isPresent()) {

            update(
                settings);

        } else {

            insert(
                settings);
        }
    }

    private void insert(
        CourseSettings settings) {

        try (Connection connection =
                 database.openConnection()) {

            boolean legacySchema =
                columnExists(
                    connection,
                    TABLE,
                    LEGACY_BATCH_COLUMN);

            String sql =
                legacySchema
                    ? "INSERT INTO tblCourseSettings "
                    + "(sourceBatchId, courseId, "
                    + "courseCode, courseName, "
                    + "credits, courseType) "
                    + "VALUES (?, ?, ?, ?, ?, ?)"
                    : "INSERT INTO tblCourseSettings "
                    + "(courseId, courseCode, "
                    + "courseName, credits, courseType) "
                    + "VALUES (?, ?, ?, ?, ?)";

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
                    settings.courseId());

                statement.setString(
                    index++,
                    settings.courseCode());

                statement.setString(
                    index++,
                    settings.courseName());

                statement.setDouble(
                    index++,
                    settings.credits());

                statement.setString(
                    index,
                    settings.courseType());

                statement.executeUpdate();
            }

        } catch (SQLException exception) {

            throw failure(
                "无法新增课程基本信息设置。",
                exception);
        }
    }

    private void update(
        CourseSettings settings) {

        String sql =
            "UPDATE tblCourseSettings "
                + "SET courseCode = ?, "
                + "courseName = ?, "
                + "credits = ?, "
                + "courseType = ? "
                + "WHERE courseId = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setString(
                1,
                settings.courseCode());

            statement.setString(
                2,
                settings.courseName());

            statement.setDouble(
                3,
                settings.credits());

            statement.setString(
                4,
                settings.courseType());

            statement.setLong(
                5,
                settings.courseId());

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法更新课程基本信息设置。",
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
                    "CREATE TABLE tblCourseSettings ("
                        + "courseId LONG NOT NULL, "
                        + "courseCode TEXT(30) NOT NULL, "
                        + "courseName TEXT(100) NOT NULL, "
                        + "credits DOUBLE NOT NULL, "
                        + "courseType TEXT(20) NOT NULL)");

                statement.executeUpdate(
                    "CREATE UNIQUE INDEX "
                        + "ux_tblCourseSettings_courseId "
                        + "ON tblCourseSettings "
                        + "(courseId)");
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化课程基本信息设置表。",
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
