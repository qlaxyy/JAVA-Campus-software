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
            "SELECT gradeId, enrollmentId, score, recordedAt "
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
            "SELECT gradeId, enrollmentId, score, recordedAt "
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
        double score,
        LocalDateTime recordedAt) {

        Optional<CourseGradeRecord> existing =
            findByEnrollmentId(
                enrollmentId);

        if (existing.isPresent()) {

            update(
                enrollmentId,
                score,
                recordedAt);

        } else {

            insert(
                enrollmentId,
                score,
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
        double score,
        LocalDateTime recordedAt) {

        String sql =
            "INSERT INTO tblGrade "
                + "(enrollmentId, score, recordedAt) "
                + "VALUES (?, ?, ?)";

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
                score);

            statement.setTimestamp(
                3,
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
     * 更新已有成绩。
     */
    private void update(
        long enrollmentId,
        double score,
        LocalDateTime recordedAt) {

        String sql =
            "UPDATE tblGrade "
                + "SET score = ?, recordedAt = ? "
                + "WHERE enrollmentId = ?";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setDouble(
                1,
                score);

            statement.setTimestamp(
                2,
                Timestamp.valueOf(
                    recordedAt));

            statement.setLong(
                3,
                enrollmentId);

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法更新课程成绩。",
                exception);
        }
    }

    /**
     * 从查询结果读取成绩。
     */
    private CourseGradeRecord readRecord(
        ResultSet result)
        throws SQLException {

        Timestamp timestamp =
            result.getTimestamp(
                "recordedAt");

        return new CourseGradeRecord(
            result.getLong(
                "gradeId"),
            result.getLong(
                "enrollmentId"),
            result.getDouble(
                "score"),
            timestamp.toLocalDateTime());
    }

    /**
     * 初始化成绩表。
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
                    "CREATE TABLE tblGrade ("
                        + "gradeId COUNTER PRIMARY KEY, "
                        + "enrollmentId LONG NOT NULL, "
                        + "score DOUBLE NOT NULL, "
                        + "recordedAt DATETIME NOT NULL)");

                statement.executeUpdate(
                    "CREATE UNIQUE INDEX "
                        + "ux_tblGrade_enrollmentId "
                        + "ON tblGrade (enrollmentId)");
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化课程成绩表。",
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
