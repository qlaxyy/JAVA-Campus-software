package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;

import java.util.List;
import java.util.Optional;

/** Data boundary that can later be implemented with Access/JDBC. */
interface BookRepository {

    List<BookDTO> search(String keyword);

    Optional<BookDTO> findById(String bookId);

    boolean decrementAvailableCount(String bookId);

    void incrementAvailableCount(String bookId);
}
