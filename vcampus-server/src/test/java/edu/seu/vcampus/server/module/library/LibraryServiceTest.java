package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LibraryServiceTest {

    @Test
    void validatedKeywordIsTrimmedAndPassedToRepository() {
        BookDTO book = new BookDTO(
                "B001", "9787111213826", "Java编程思想", "Bruce Eckel", "计算机", 5, 2);
        String[] receivedKeyword = new String[1];
        BookRepository repository = keyword -> {
            receivedKeyword[0] = keyword;
            return List.of(book);
        };
        LibraryService service = new LibraryService(repository);

        BookSearchResult result = service.searchBooks(new BookSearchRequest("  Java  "));

        assertEquals("Java", receivedKeyword[0]);
        assertEquals(List.of(book), result.getBooks());
    }

    @Test
    void blankKeywordIsRejectedBeforeRepositoryCall() {
        LibraryService service = new LibraryService(keyword -> {
            throw new AssertionError("repository must not be called");
        });

        assertThrows(
                IllegalArgumentException.class,
                () -> service.searchBooks(new BookSearchRequest("   ")));
    }
}
