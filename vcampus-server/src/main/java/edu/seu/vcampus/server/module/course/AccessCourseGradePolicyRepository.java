package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Microsoft Access 教学班成绩比例仓库。
 */
final class AccessCourseGradePolicyRepository
    implements CourseGradePolicyRepository {

    private static final String TABLE =
        "tblGradePolicy";

    private final AccessDatabase database;

    AccessCourseGradePolicyRepository(
        AccessDatabase database) {

        this.database =
            Objects.requireNonNull(
                database);

        initialiseSchema();
    }

    @Override
    public Optional<CourseGradePolicyRecord>
    findByOfferingId(
        long offeringId) {

        String sql =
            "SELECT offeringId, "
                + "usualWeightPercent, "
                + "finalExamWeightPercent, "
                + "updatedAt "
                + "FROM tblGradePolicy "
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
                    readRecord(
                        result));
            }

        } catch (SQLException exception) {

            throw failure(
                "无法查询教学班成绩比例。",
                exception);
        }
    }

    @Override
    public synchronized CourseGradePolicyRecord save(
        long offeringId,
        int usualWeightPercent,
        int finalExamWeightPercent,
        LocalDateTime updatedAt) {

        if (findByOfferingId(
            offeringId).isPresent()) {

            update(
                offeringId,
                usualWeightPercent,
                finalExamWeightPercent,
                updatedAt);

        } else {

            insert(
                offeringId,
                usualWeightPercent,
                finalExamWeightPercent,
                updatedAt);
        }

        return findByOfferingId(
            offeringId)
            .orElseThrow(() ->
                new IllegalStateException(
                    "成绩比例保存后无法重新读取。"));
    }

    /**
     * 新增成绩比例。
     */
    private void insert(
        long offeringId,
        int usualWeightPercent,
        int finalExamWeightPercent,
        LocalDateTime updatedAt) {

        String sql =
            "INSERT INTO tblGradePolicy "
                + "(offeringId, "
                + "usualWeightPercent, "
                + "finalExamWeightPercent, "
                + "updatedAt) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                offeringId);

            statement.setInt(
                2,
                usualWeightPercent);

            statement.setInt(
                3,
                finalExamWeightPercent);

            statement.setTimestamp(
                4,
                Timestamp.valueOf(
                    updatedAt));

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法新增教学班成绩比例。",
                exception);
        }
    }

    /**
     * 修改成绩比例。
     */
    private void update(
        long offeringId,
        int usualWeightPercent,
        int finalExamWeightPercent,
        LocalDateTime updatedAt) {

        String sql =
            "UPDATE tblGradePolicy "
                + "SET usualWeightPercent = ?, "
                + "finalExamWeightPercent = ?, "
                + "updatedAt = ? "
                + "WHERE offeringId = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setInt(
                1,
                usualWeightPercent);

            statement.setInt(
                2,
                finalExamWeightPercent);

            statement.setTimestamp(
                3,
                Timestamp.valueOf(
                    updatedAt));

            statement.setLong(
                4,
                offeringId);

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法修改教学班成绩比例。",
                exception);
        }
    }

    /**
     * 读取成绩比例记录。
     */
    private CourseGradePolicyRecord readRecord(
        ResultSet result)
        throws SQLException {

        Timestamp updatedAt =
            result.getTimestamp(
                "updatedAt");

        return new CourseGradePolicyRecord(
            result.getLong(
                "offeringId"),
            result.getInt(
                "usualWeightPercent"),
            result.getInt(
                "finalExamWeightPercent"),
            updatedAt.toLocalDateTime());
    }

    /**
     * 初始化数据表。
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
                    "CREATE TABLE tblGradePolicy ("
                        + "gradePolicyId COUNTER PRIMARY KEY, "
                        + "offeringId LONG NOT NULL, "
                        + "usualWeightPercent LONG NOT NULL, "
                        + "finalExamWeightPercent LONG NOT NULL, "
                        + "updatedAt DATETIME NOT NULL)");

                statement.executeUpdate(
                    "CREATE UNIQUE INDEX "
                        + "ux_tblGradePolicy_offeringId "
                        + "ON tblGradePolicy "
                        + "(offeringId)");
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化教学班成绩比例表。",
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
