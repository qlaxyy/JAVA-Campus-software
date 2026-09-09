package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.SetBookStatusRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LibraryServiceTest {

    private static final SessionInfo ADMIN = new SessionInfo("token", "A-1", "admin", "管理员",
            Role.USER, Set.of(AdminScope.LIBRARY));
    private final InMemoryBookRepository books = new InMemoryBookRepository();
    private final InMemoryBookCopyRepository copies = InMemoryBookCopyRepository.seededFrom(books.searchAll(""));
    private final LibraryService service = new LibraryService(books, new InMemoryBorrowRecordRepository(), copies);

    @Test
    void searchTrimsKeywordFiltersCategoryAndAggregatesPhysicalCopiesByLocation() {
        var result = service.searchBooks(new BookSearchRequest("  java  ", "C001"));
        assertEquals(3, result.getBooks().size());
        BookDTO book = result.getBooks().stream().filter(value -> "B001".equals(value.getBookId()))
                .findFirst().orElseThrow();
        assertEquals(copies.findByBookId("B001").stream()
                .filter(copy -> copy.status() != BookCopyStatus.WITHDRAWN).count(), book.getTotalCount());
        assertEquals(copies.findByBookId("B001").stream()
                .filter(copy -> copy.status() == BookCopyStatus.AVAILABLE).count(), book.getAvailableCount());
        assertFalse(book.getLocations().isEmpty());
    }

    @Test
    void readerSearchExcludesInactiveBookButAdministratorSearchIncludesIt() {
        service.setBookStatus(ADMIN, new SetBookStatusRequest("B001", "INACTIVE"));

        assertTrue(service.searchBooks(new BookSearchRequest("9787111213826", null)).getBooks().isEmpty());
        BookDTO inactive = service.searchBooksForAdmin(ADMIN,
                new BookSearchRequest("9787111213826", null)).getBooks().getFirst();
        assertEquals("INACTIVE", inactive.getStatus());
        assertEquals(5, inactive.getTotalCount(),
                "inactive titles retain their physical inventory");
        assertEquals(0, inactive.getAvailableCount(),
                "copies under an inactive title are not currently borrowable");
    }

    @Test
    void searchRejectsUnknownCategoryAndOverlongKeyword() {
        LibraryBusinessException category = assertThrows(LibraryBusinessException.class,
                () -> service.searchBooks(new BookSearchRequest("", "UNKNOWN")));
        assertEquals(ErrorCodes.LIBRARY_CATEGORY_NOT_FOUND, category.code());
        assertThrows(IllegalArgumentException.class,
                () -> service.searchBooks(new BookSearchRequest("x".repeat(51), null)));
    }
}
