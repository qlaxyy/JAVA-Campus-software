package edu.seu.vcampus.server.module.library;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Access-backed history of copy circulation. */
final class AccessBorrowRecordRepository implements BorrowRecordRepository {

    private static final String SELECT_RECORD = "SELECT recordId, userId, copyId, "
            + "borrowTime, dueTime, renewalCount, returnTime, lostReportedAt, feeSettledAt, "
            + "[status] FROM tblBorrowRecord";

    private final AccessLibraryStore store;

    AccessBorrowRecordRepository(AccessLibraryStore store) {
        this.store = Objects.requireNonNull(store, "store must not be null");
    }

    @Override
    public List<BorrowRecord> findBorrowedByUserId(String userId) {
        return findMany(" WHERE userId = ? AND [status] = 'BORROWED'", userId);
    }

    @Override
    public List<BorrowRecord> findByUserId(String userId) {
        return findMany(" WHERE userId = ?", userId);
    }

    @Override
    public List<BorrowRecord> findAll() {
        return store.read("Cannot list library borrow records.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    SELECT_RECORD + " ORDER BY borrowTime DESC, recordId");
                 ResultSet result = statement.executeQuery()) {
                return readRecords(result);
            }
        });
    }

    @Override
    public Optional<BorrowRecord> findById(String recordId) {
        return findOne(" WHERE recordId = ?", recordId);
    }

    @Override
    public Optional<BorrowRecord> findBorrowedByCopyId(String copyId) {
        return findOne(" WHERE copyId = ? AND [status] = 'BORROWED'", copyId);
    }

    @Override
    public void save(BorrowRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        store.execute(() -> {
            if (record.status() == BorrowStatus.BORROWED
                    && findBorrowedByCopyId(record.copyId()).isPresent()) {
                throw new IllegalStateException(
                        "Physical copy already has an active borrow record.");
            }
            store.write("Cannot insert library borrow record.", connection -> {
                String sql = "INSERT INTO tblBorrowRecord (recordId, userId, copyId, "
                        + "borrowTime, dueTime, renewalCount, returnTime, lostReportedAt, "
                        + "feeSettledAt, [status]) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    bindRecord(statement, record, true);
                    requireOne(statement.executeUpdate(), "Borrow record was not inserted.");
                }
            });
        });
    }

    @Override
    public void update(BorrowRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        store.execute(() -> {
            BorrowRecord original = findById(record.recordId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Borrow record does not exist."));
            BorrowRecord.requireValidTransition(original, record);
            store.write("Cannot update library borrow record.", connection -> {
                String sql = "UPDATE tblBorrowRecord SET dueTime = ?, renewalCount = ?, "
                        + "returnTime = ?, lostReportedAt = ?, feeSettledAt = ?, [status] = ? "
                        + "WHERE recordId = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setTimestamp(1, Timestamp.valueOf(record.dueTime()));
                    statement.setInt(2, record.renewalCount());
                    setNullableTimestamp(statement, 3, record.returnTime());
                    setNullableTimestamp(statement, 4, record.lostReportedAt());
                    setNullableTimestamp(statement, 5, record.feeSettledAt());
                    statement.setString(6, record.status().name());
                    statement.setString(7, record.recordId());
                    requireOne(statement.executeUpdate(), "Borrow record does not exist.");
                }
            });
        });
    }

    private List<BorrowRecord> findMany(String condition, String value) {
        return store.read("Cannot list library borrow records.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    SELECT_RECORD + condition + " ORDER BY borrowTime DESC, recordId")) {
                statement.setString(1, value);
                try (ResultSet result = statement.executeQuery()) {
                    return readRecords(result);
                }
            }
        });
    }

    private Optional<BorrowRecord> findOne(String condition, String value) {
        return store.read("Cannot read library borrow record.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    SELECT_RECORD + condition)) {
                statement.setString(1, value);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(readRecord(result)) : Optional.empty();
                }
            }
        });
    }

    private List<BorrowRecord> readRecords(ResultSet result) throws java.sql.SQLException {
        List<BorrowRecord> records = new ArrayList<>();
        while (result.next()) {
            records.add(readRecord(result));
        }
        return records;
    }

    private BorrowRecord readRecord(ResultSet result) throws java.sql.SQLException {
        BorrowStatus status = BorrowStatus.valueOf(result.getString("status"));
        return new BorrowRecord(
                result.getString("recordId"),
                result.getString("userId"),
                result.getString("copyId"),
                result.getTimestamp("borrowTime").toLocalDateTime(),
                result.getTimestamp("dueTime").toLocalDateTime(),
                status,
                toLocalDateTime(result.getTimestamp("returnTime")),
                result.getInt("renewalCount"),
                toLocalDateTime(result.getTimestamp("lostReportedAt")),
                toLocalDateTime(result.getTimestamp("feeSettledAt")));
    }

    private void bindRecord(PreparedStatement statement, BorrowRecord record,
            boolean includeIdentity) throws java.sql.SQLException {
        int index = 1;
        if (includeIdentity) {
            statement.setString(index++, record.recordId());
            statement.setString(index++, record.userId());
            statement.setString(index++, record.copyId());
            statement.setTimestamp(index++, Timestamp.valueOf(record.borrowTime()));
            statement.setTimestamp(index++, Timestamp.valueOf(record.dueTime()));
            statement.setInt(index++, record.renewalCount());
        }
        setNullableTimestamp(statement, index++, record.returnTime());
        setNullableTimestamp(statement, index++, record.lostReportedAt());
        setNullableTimestamp(statement, index++, record.feeSettledAt());
        statement.setString(index, record.status().name());
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static void setNullableTimestamp(
            PreparedStatement statement, int index, LocalDateTime value)
            throws java.sql.SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private void requireOne(int affectedRows, String message) {
        if (affectedRows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
