package edu.seu.vcampus.server.module.library;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Access-backed history of copy circulation. */
final class AccessBorrowRecordRepository implements BorrowRecordRepository {

    private static final String SELECT_RECORD = "SELECT recordId, userId, copyId, "
            + "borrowTime, dueTime, returnTime, [status] FROM tblBorrowRecord";

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
                        + "borrowTime, dueTime, returnTime, [status]) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)";
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
            requireReturnTransition(original, record);
            store.write("Cannot update library borrow record.", connection -> {
                String sql = "UPDATE tblBorrowRecord SET returnTime = ?, [status] = ? "
                        + "WHERE recordId = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setTimestamp(1, Timestamp.valueOf(record.returnTime()));
                    statement.setString(2, record.status().name());
                    statement.setString(3, record.recordId());
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
        Timestamp returnTime = result.getTimestamp("returnTime");
        BorrowStatus status = BorrowStatus.valueOf(result.getString("status"));
        if (status == BorrowStatus.BORROWED) {
            return new BorrowRecord(
                    result.getString("recordId"),
                    result.getString("userId"),
                    result.getString("copyId"),
                    result.getTimestamp("borrowTime").toLocalDateTime(),
                    result.getTimestamp("dueTime").toLocalDateTime(),
                    status);
        }
        BorrowRecord borrowed = new BorrowRecord(
                result.getString("recordId"),
                result.getString("userId"),
                result.getString("copyId"),
                result.getTimestamp("borrowTime").toLocalDateTime(),
                result.getTimestamp("dueTime").toLocalDateTime(),
                BorrowStatus.BORROWED);
        return borrowed.returnedAt(returnTime.toLocalDateTime());
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
        }
        if (record.returnTime() == null) {
            statement.setNull(index++, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index++, Timestamp.valueOf(record.returnTime()));
        }
        statement.setString(index, record.status().name());
    }

    private void requireReturnTransition(BorrowRecord original, BorrowRecord updated) {
        if (!original.userId().equals(updated.userId())
                || !original.copyId().equals(updated.copyId())
                || !original.borrowTime().equals(updated.borrowTime())
                || !original.dueTime().equals(updated.dueTime())
                || original.status() != BorrowStatus.BORROWED
                || updated.status() != BorrowStatus.RETURNED) {
            throw new IllegalArgumentException("Invalid borrow-record state transition.");
        }
    }

    private void requireOne(int affectedRows, String message) {
        if (affectedRows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
