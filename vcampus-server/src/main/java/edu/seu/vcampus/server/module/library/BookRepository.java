package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;

import java.util.List;

/** Data boundary that can later be implemented with Access/JDBC. */
@FunctionalInterface
interface BookRepository {

    List<BookDTO> search(String keyword);
}
