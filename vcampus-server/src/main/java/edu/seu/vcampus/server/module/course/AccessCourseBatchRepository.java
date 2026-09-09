package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.SelectionBatchType;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Access 原始选课批次仓库。
 */
@SuppressWarnings({
    "SqlNoDataSourceInspection",
    "SqlResolve"
})
final class AccessCourseBatchRepository
    implements CourseBatchRepository {

    private static final String TABLE =
        "tblSelectionBatch";

    private final AccessDatabase database;

    private final Clock clock;

    AccessCourseBatchRepository(
        AccessDatabase database,
        Clock clock) {

        this.database =
            Objects.requireNonNull(
                database);

        this.clock =
            Objects.requireNonNull(
                clock);

        initialiseSchema();
        initialiseDemoData();
    }

    /**
     * 查询全部选课批次。
     */
    @Override
    public List<CourseBatchRecord>
    findCurrentSemesterBatches() {

        String sql =
            "SELECT batchId, semester, batchName, "
                + "batchType, startTime, endTime, "
                + "allowSelect, allowDrop, enabled "
                + "FROM tblSelectionBatch "
                + "ORDER BY batchId";

        try (Connection connection =
                 database.openConnection();
             Statement statement =
                 connection.createStatement();
             ResultSet result =
                 statement.executeQuery(
                     sql)) {

            List<CourseBatchRecord> batches =
                new ArrayList<>();

            while (result.next()) {

                batches.add(
                    new CourseBatchRecord(
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
                        result.getBoolean(
                            "allowSelect"),
                        result.getBoolean(
                            "allowDrop"),
                        result.getBoolean(
                            "enabled")));
            }

            return List.copyOf(
                batches);

        } catch (SQLException exception) {

            throw failure(
                "无法读取选课批次。",
                exception);
        }
    }

    /**
     * 初始化选课批次表。
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
                    "CREATE TABLE tblSelectionBatch ("
                        + "batchId LONG NOT NULL, "
                        + "semester TEXT(30) NOT NULL, "
                        + "batchName TEXT(100) NOT NULL, "
                        + "batchType TEXT(30) NOT NULL, "
                        + "startTime DATETIME NOT NULL, "
                        + "endTime DATETIME NOT NULL, "
                        + "allowSelect YESNO NOT NULL, "
                        + "allowDrop YESNO NOT NULL, "
                        + "enabled YESNO NOT NULL)");

                statement.executeUpdate(
                    "CREATE UNIQUE INDEX "
                        + "ux_tblSelectionBatch_batchId "
                        + "ON tblSelectionBatch "
                        + "(batchId)");
            }

        } catch (SQLException exception) {

            throw failure(
                "无法初始化选课批次表。",
                exception);
        }
    }

    /**
     * 数据库为空时写入演示批次。
     */
    private void initialiseDemoData() {

        if (countBatches() > 0) {

            return;
        }

        LocalDateTime now =
            LocalDateTime.now(
                clock);

        insert(
            new CourseBatchRecord(
                1L,
                "2026-2027-1",
                "2026-2027秋季学期预选课",
                SelectionBatchType.PRE_SELECTION,
                now.minusDays(10),
                now.minusDays(5),
                true,
                true,
                true));

        insert(
            new CourseBatchRecord(
                2L,
                "2026-2027-1",
                "2026-2027秋季学期重修选课",
                SelectionBatchType.RETAKE,
                now.minusDays(1),
                now.plusDays(1),
                true,
                true,
                true));

        insert(
            new CourseBatchRecord(
                3L,
                "2026-2027-1",
                "2026-2027秋季学期退改补",
                SelectionBatchType.ADD_DROP,
                now.minusDays(1),
                now.plusDays(7),
                true,
                true,
                true));
    }

    /**
     * 查询批次数量。
     */
    private int countBatches() {

        String sql =
            "SELECT COUNT(*) AS batchCount "
                + "FROM tblSelectionBatch";

        try (Connection connection =
                 database.openConnection();
             Statement statement =
                 connection.createStatement();
             ResultSet result =
                 statement.executeQuery(
                     sql)) {

            return result.next()
                ? result.getInt(
                "batchCount")
                : 0;

        } catch (SQLException exception) {

            throw failure(
                "无法统计选课批次。",
                exception);
        }
    }

    /**
     * 写入一条原始批次。
     */
    private void insert(
        CourseBatchRecord batch) {

        String sql =
            "INSERT INTO tblSelectionBatch "
                + "(batchId, semester, batchName, "
                + "batchType, startTime, endTime, "
                + "allowSelect, allowDrop, enabled) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection =
                 database.openConnection();
             PreparedStatement statement =
                 connection.prepareStatement(
                     sql)) {

            statement.setLong(
                1,
                batch.batchId());

            statement.setString(
                2,
                batch.semester());

            statement.setString(
                3,
                batch.batchName());

            statement.setString(
                4,
                batch.batchType()
                    .name());

            statement.setTimestamp(
                5,
                Timestamp.valueOf(
                    batch.startTime()));

            statement.setTimestamp(
                6,
                Timestamp.valueOf(
                    batch.endTime()));

            statement.setBoolean(
                7,
                batch.allowSelect());

            statement.setBoolean(
                8,
                batch.allowDrop());

            statement.setBoolean(
                9,
                batch.enabled());

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw failure(
                "无法写入演示选课批次。",
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
