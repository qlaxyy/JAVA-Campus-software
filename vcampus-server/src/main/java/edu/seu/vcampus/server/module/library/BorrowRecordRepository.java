package edu.seu.vcampus.server.module.library;

import java.util.List;
import java.util.Optional;

/** Data boundary for library borrow records. */
interface BorrowRecordRepository {

    List<BorrowRecord> findBorrowedByUserId(String userId);

    List<BorrowRecord> findByUserId(String userId);

    Optional<BorrowRecord> findById(String recordId);

    /** Inserts a new record; on failure the repository must remain unchanged. */
    void save(BorrowRecord record);

    /** Replaces an existing record atomically; on failure the repository must remain unchanged. */
    void update(BorrowRecord record);
}
