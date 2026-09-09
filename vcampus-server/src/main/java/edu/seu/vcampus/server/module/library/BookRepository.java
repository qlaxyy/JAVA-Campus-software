package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;

import java.util.List;
import java.util.Optional;

/** Data boundary that can later be implemented with Access/JDBC. */
interface BookRepository {

    List<BookDTO> search(String keyword);

    /** Searches active and retained inactive catalog records for administrators. */
    default List<BookDTO> searchAll(String keyword) {
        return search(keyword);
    }

    /** Finds only active books available for normal business operations. */
    Optional<BookDTO> findById(String bookId);

    /** Finds catalog metadata regardless of ACTIVE or INACTIVE status. */
    Optional<BookDTO> findIncludingInactive(String bookId);

    Optional<BookDTO> findByIsbn(String isbn);

    /** Inserts a unique book. Failure must leave all repository data unchanged. */
    void insert(BookDTO book);

    /** Atomically replaces one catalog record; failure changes nothing. */
    void update(BookDTO book);

}
