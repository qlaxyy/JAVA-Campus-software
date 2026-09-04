package edu.seu.vcampus.server.module.library;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory borrow records used until the Access repository is introduced. */
final class InMemoryBorrowRecordRepository implements BorrowRecordRepository {

    private final Map<String, BorrowRecord> records = new LinkedHashMap<>();

    @Override
    public synchronized List<BorrowRecord> findBorrowedByUserId(String userId) {
        return records.values().stream()
                .filter(record -> record.userId().equals(userId))
                .filter(record -> record.status() == BorrowStatus.BORROWED)
                .toList();
    }

    @Override
    public synchronized void save(BorrowRecord record) {
        if (records.putIfAbsent(record.recordId(), record) != null) {
            throw new IllegalStateException("Borrow record identifier already exists.");
        }
    }

    @Override
    public synchronized List<BorrowRecord> findByUserId(String userId) {
        return records.values().stream()
                .filter(record -> record.userId().equals(userId))
                .toList();
    }

    @Override
    public synchronized Optional<BorrowRecord> findById(String recordId) {
        return Optional.ofNullable(records.get(recordId));
    }

    @Override
    public synchronized void update(BorrowRecord record) {
        if (!records.containsKey(record.recordId())) {
            throw new IllegalStateException("Borrow record does not exist.");
        }
        records.put(record.recordId(), record);
    }
}
