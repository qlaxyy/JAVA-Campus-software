package edu.seu.vcampus.server.module.library;

import java.util.List;
import java.util.Optional;

/** Data boundary for physical library copies. */
interface BookCopyRepository {

    List<BookCopy> findByBookId(String bookId);

    Optional<BookCopy> findById(String copyId);

    Optional<BookCopy> findByBarcode(String barcode);

    /** Inserts one copy; identifiers and barcodes are globally unique. */
    void insert(BookCopy copy);

    /** Replaces one copy without allowing its identity, barcode or parent book to change. */
    void update(BookCopy copy);

    /** Atomically replaces all copies belonging to one book. */
    void replaceForBook(String bookId, List<BookCopy> copies);
}
