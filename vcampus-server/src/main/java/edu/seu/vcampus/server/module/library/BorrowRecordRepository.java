package edu.seu.vcampus.server.module.library;

import java.util.List;

/** Data boundary for library borrow records. */
interface BorrowRecordRepository {

    List<BorrowRecord> findBorrowedByUserId(String userId);

    void save(BorrowRecord record);
}
