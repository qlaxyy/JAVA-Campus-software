package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;

import java.util.Objects;

/** Library business rules independent from sockets and Swing. */
final class LibraryService {

    private static final int MAX_KEYWORD_LENGTH = 50;

    private final BookRepository repository;

    LibraryService(BookRepository repository) {
        this.repository = Objects.requireNonNull(
                repository, "repository must not be null");
    }

    BookSearchResult searchBooks(BookSearchRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String keyword = request.getKeyword();
        if (keyword == null
                || keyword.isBlank()
                || keyword.trim().length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Enter a search keyword of 1 to 50 characters.");
        }
        return new BookSearchResult(repository.search(keyword.trim()));
    }
}
