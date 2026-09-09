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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Microsoft Access 成绩仓库。
 */
final class AccessCourseGradeRepository
    implements CourseGradeRepository {

    private static final String TABLE =
        "tblGrade";

    private final AccessDatabase database;

    AccessCourseGradeRepository(
        AccessDatabase database) {

        this.database =
            Objects.requireNonNull(
                database);

        initialiseSchema();
    }

    @Override
    public Optional<CourseGradeRecord>
    findByEnrollmentId(
        long enrollmentId) {

        String sql =
            "SELECT gradeId, enrollmentId, "
                + "score, usualScore, "
                + "finalExamScore, recordedAt "
                + "FROM tblGrade "
                + "WHERE enrollmentId = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                enrollmentId);

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
                "无法查询课程成绩。",
                exception);
        }
    }

    @Override
    public List<CourseGradeRecord> findAll() {

        String sql =
            "SELECT gradeId, enrollmentId, "
                + "score, usualScore, "
                + "finalExamScore, recordedAt "
                + "FROM tblGrade "
                + "ORDER BY gradeId";

        try (Connection connection =
                 database.openConnection();
             Statement statement =
                 connection.createStatement();
             ResultSet result =
                 statement.executeQuery(
                     sql)) {

            List<CourseGradeRecord> records =
                new ArrayList<>();

            while (result.next()) {

                records.add(
                    readRecord(
                        result));
            }

            return List.copyOf(
                records);

        } catch (SQLException exception) {

            throw failure(
                "无法读取课程成绩。",
                exception);
        }
    }

    @Override
    public synchronized CourseGradeRecord save(
        long enrollmentId,
        double usualScore,
        double finalExamScore,
        LocalDateTime recordedAt) {

        Optional<CourseGradeRecord> existing =
            findByEnrollmentId(
                enrollmentId);

        /*
         * score 是旧版数据库字段。
         *
         * 暂时继续写入默认 40% + 60% 的结果，
         * 保证旧代码和数据库约束可以继续工作。
         *
         * 新版页面显示的总成绩会由
         * CourseGradePolicyService 按实际比例计算。
         */
        double compatibilityScore =
            roundScore(
                usualScore * 0.4
                    + finalExamScore * 0.6);

        if (existing.isPresent()) {

            update(
                enrollmentId,
                usualScore,
                finalExamScore,
                compatibilityScore,
                recordedAt);

        } else {

            insert(
                enrollmentId,
                usualScore,
                finalExamScore,
                compatibilityScore,
                recordedAt);
        }

        return findByEnrollmentId(
            enrollmentId)
            .orElseThrow(() ->
                new IllegalStateException(
                    "成绩保存后无法重新读取。"));
    }

    /**
     * 新增成绩。
     */
    private void insert(
        long enrollmentId,
        double usualScore,
        double finalExamScore,
        double compatibilityScore,
        LocalDateTime recordedAt) {

        String sql =
            "INSERT INTO tblGrade "
                + "(enrollmentId, score, "
                + "usualScore, finalExamScore, "
                + "recordedAt) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                enrollmentId);

            statement.setDouble(
                2,
                compatibilityScore);

            statement.setDouble(
                3,
                usualScore);

            statement.setDouble(
                4,
                finalExamScore);

            statement.setTimestamp(
                5,
                Timestamp.valueOf(
                    recordedAt));

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法新增课程成绩。",
                exception);
        }
    }

    /**
     * 更新成绩。
     */
    private void update(
        long enrollmentId,
        double usualScore,
        double finalExamScore,
        double compatibilityScore,
        LocalDateTime recordedAt) {

        String sql =
            "UPDATE tblGrade "
                + "SET score = ?, "
                + "usualScore = ?, "
                + "finalExamScore = ?, "
                + "recordedAt = ? "
                + "WHERE enrollmentId = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setDouble(
                1,
                compatibilityScore);

            statement.setDouble(
                2,
                usualScore);

            statement.setDouble(
                3,
                finalExamScore);

            statement.setTimestamp(
                4,
                Timestamp.valueOf(
                    recordedAt));

            statement.setLong(
                5,
                enrollmentId);

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法更新课程成绩。",
                exception);
        }
    }

    /**
     * 读取成绩记录。
     */
    private CourseGradeRecord readRecord(
        ResultSet result)
        throws SQLException {

        double oldScore =
            result.getDouble(
                "score");

        Double usualScore =
            nullableDouble(
                result,
                "usualScore");

        Double finalExamScore =
            nullableDouble(
                result,
                "finalExamScore");

        Timestamp timestamp =
            result.getTimestamp(
                "recordedAt");

        /*
         * 兼容迁移前的旧成绩。
         *
         * 如果新字段为空，就使用原有 score，
         * 这样原有总成绩不会发生变化。
         */
        return new CourseGradeRecord(
            result.getLong(
                "gradeId"),
            result.getLong(
                "enrollmentId"),
            usualScore == null
                ? oldScore
                : usualScore,
            finalExamScore == null
                ? oldScore
                : finalExamScore,
            timestamp.toLocalDateTime());
    }

    /**
     * 读取可能为 null 的 DOUBLE 字段。
     */
    private Double nullableDouble(
        ResultSet result,
        String column)
        throws SQLException {

        double value =
            result.getDouble(
                column);

        return result.wasNull()
            ? null
            : value;
    }

    /**
     * 初始化或升级成绩表。
     */
    private void initialiseSchema() {

        try (Connection connection =
                 database.openConnection()) {

            if (!tableExists(
                connection,
                TABLE)) {

                createTable(
                    connection);

                return;
            }

            migrateExistingTable(
                connection);

        } catch (SQLException exception) {

            throw failure(
                "无法初始化课程成绩表。",
                exception);
        }
    }

    /**
     * 创建新版成绩表。
     */
    private void createTable(
        Connection connection)
        throws SQLException {

        try (Statement statement =
                 connection.createStatement()) {

            statement.executeUpdate(
                "CREATE TABLE tblGrade ("
                    + "gradeId COUNTER PRIMARY KEY, "
                    + "enrollmentId LONG NOT NULL, "
                    + "score DOUBLE NOT NULL, "
                    + "usualScore DOUBLE, "
                    + "finalExamScore DOUBLE, "
                    + "recordedAt DATETIME NOT NULL)");

            statement.executeUpdate(
                "CREATE UNIQUE INDEX "
                    + "ux_tblGrade_enrollmentId "
                    + "ON tblGrade "
                    + "(enrollmentId)");
        }
    }

    /**
     * 升级已经存在的旧成绩表。
     */
    private void migrateExistingTable(
        Connection connection)
        throws SQLException {

        try (Statement statement =
                 connection.createStatement()) {

            if (!columnExists(
                connection,
                TABLE,
                "usualScore")) {

                statement.executeUpdate(
                    "ALTER TABLE tblGrade "
                        + "ADD COLUMN usualScore DOUBLE");
            }

            if (!columnExists(
                connection,
                TABLE,
                "finalExamScore")) {

                statement.executeUpdate(
                    "ALTER TABLE tblGrade "
                        + "ADD COLUMN finalExamScore DOUBLE");
            }

            /*
             * 旧数据迁移：
             * 将原 score 同时作为平时和期末成绩，
             * 从而保持原总成绩不变。
             */
            statement.executeUpdate(
                "UPDATE tblGrade "
                    + "SET usualScore = score "
                    + "WHERE usualScore IS NULL");

            statement.executeUpdate(
                "UPDATE tblGrade "
                    + "SET finalExamScore = score "
                    + "WHERE finalExamScore IS NULL");
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

    /**
     * 判断字段是否存在。
     */
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

    /**
     * 成绩保留两位小数。
     */
    private double roundScore(
        double score) {

        return Math.round(
            score * 100.0)
            / 100.0;
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
