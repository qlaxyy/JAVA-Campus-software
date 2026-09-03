package edu.seu.vcampus.server.module.library;

import java.util.ArrayList;
import java.util.List;

/** In-memory borrow records used until the Access repository is introduced. */
final class InMemoryBorrowRecordRepository implements BorrowRecordRepository {

    private final List<BorrowRecord> records = new ArrayList<>();

    @Override
    public synchronized List<BorrowRecord> findBorrowedByUserId(String userId) {
        return records.stream()
                .filter(record -> record.userId().equals(userId))
                .filter(record -> record.status() == BorrowStatus.BORROWED)
                .toList();
    }

    @Override
    public synchronized void save(BorrowRecord record) {
        records.add(record);
    }
}
