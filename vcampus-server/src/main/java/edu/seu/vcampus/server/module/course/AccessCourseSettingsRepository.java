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
 */
final class AccessCourseSettingsRepository
    implements CourseSettingsRepository {

    private static final String TABLE =
        "tblCourseSettings";

    private final AccessDatabase database;

    AccessCourseSettingsRepository(
        AccessDatabase database) {

        this.database =
            Objects.requireNonNull(
                database);

        initialiseSchema();
    }

    /**
     * 查询课程基本信息设置。
     *
     * 课程信息按照 courseId 全局存储，
     * 不按照选课批次隔离。
     */
    @Override
    public Optional<CourseSettings> find(
        long batchId,
        long courseId) {

        String sql =
            "SELECT sourceBatchId, courseId, "
                + "courseCode, courseName, "
                + "credits, courseType "
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
                            "sourceBatchId"),
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

    /**
     * 新增或更新课程基本信息设置。
     */
    @Override
    public synchronized void save(
        CourseSettings settings) {

        if (find(
            settings.batchId(),
            settings.courseId())
            .isPresent()) {

            update(
                settings);

        } else {

            insert(
                settings);
        }
    }

    /**
     * 新增课程设置。
     */
    private void insert(
        CourseSettings settings) {

        String sql =
            "INSERT INTO tblCourseSettings "
                + "(sourceBatchId, courseId, "
                + "courseCode, courseName, "
                + "credits, courseType) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

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
                settings.courseId());

            statement.setString(
                3,
                settings.courseCode());

            statement.setString(
                4,
                settings.courseName());

            statement.setDouble(
                5,
                settings.credits());

            statement.setString(
                6,
                settings.courseType());

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法新增课程基本信息设置。",
                exception);
        }
    }

    /**
     * 更新已有课程设置。
     */
    private void update(
        CourseSettings settings) {

        String sql =
            "UPDATE tblCourseSettings "
                + "SET sourceBatchId = ?, "
                + "courseCode = ?, "
                + "courseName = ?, "
                + "credits = ?, "
                + "courseType = ? "
                + "WHERE courseId = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                settings.batchId());

            statement.setString(
                2,
                settings.courseCode());

            statement.setString(
                3,
                settings.courseName());

            statement.setDouble(
                4,
                settings.credits());

            statement.setString(
                5,
                settings.courseType());

            statement.setLong(
                6,
                settings.courseId());

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法更新课程基本信息设置。",
                exception);
        }
    }

    /**
     * 初始化数据库表。
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
                    "CREATE TABLE tblCourseSettings ("
                        + "sourceBatchId LONG NOT NULL, "
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

    /**
     * 判断数据表是否存在。
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
