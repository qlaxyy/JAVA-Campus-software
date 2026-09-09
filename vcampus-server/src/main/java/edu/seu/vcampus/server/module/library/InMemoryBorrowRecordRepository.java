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
        if (record.status() == BorrowStatus.BORROWED
                && findBorrowedByCopyId(record.copyId()).isPresent()) {
            throw new IllegalStateException("Physical copy already has an active borrow record.");
        }
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
    public synchronized List<BorrowRecord> findAll() {
        return List.copyOf(records.values());
    }

    @Override
    public synchronized Optional<BorrowRecord> findById(String recordId) {
        return Optional.ofNullable(records.get(recordId));
    }

    @Override
    public synchronized Optional<BorrowRecord> findBorrowedByCopyId(String copyId) {
        return records.values().stream()
                .filter(record -> record.copyId().equals(copyId))
                .filter(record -> record.status() == BorrowStatus.BORROWED)
                .findFirst();
    }

    @Override
    public synchronized void update(BorrowRecord record) {
        BorrowRecord original = records.get(record.recordId());
        if (original == null) {
            throw new IllegalStateException("Borrow record does not exist.");
        }
        if (!original.userId().equals(record.userId())
                || !original.copyId().equals(record.copyId())
                || !original.borrowTime().equals(record.borrowTime())
                || !original.dueTime().equals(record.dueTime())
                || original.status() != BorrowStatus.BORROWED
                || record.status() != BorrowStatus.RETURNED) {
            throw new IllegalArgumentException("Invalid borrow-record state transition.");
        }
        records.put(record.recordId(), record);
    }

    synchronized Map<String, BorrowRecord> snapshot() {
        return new LinkedHashMap<>(records);
    }

    synchronized void restore(Map<String, BorrowRecord> snapshot) {
        records.clear();
        records.putAll(snapshot);
    }
}
