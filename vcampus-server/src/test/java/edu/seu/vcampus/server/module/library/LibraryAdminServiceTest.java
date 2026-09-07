package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.*;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LibraryAdminServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-06T08:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 6, 8, 0);
    private static final SessionInfo ADMIN = new SessionInfo("admin", "A-1", "libraryadmin", "管理员",
            Role.USER, Set.of(AdminScope.LIBRARY));
    private static final SessionInfo READER = new SessionInfo("reader", "U-1", "reader", "读者", Role.USER);

    private final InMemoryBookRepository books = new InMemoryBookRepository();
    private final InMemoryBorrowRecordRepository records = new InMemoryBorrowRecordRepository();
    private final InMemoryBookCopyRepository copies = InMemoryBookCopyRepository.seededFrom(books.searchAll(""));
    private final AtomicInteger sequence = new AtomicInteger();
    private final LibraryService service = new LibraryService(books, records, CLOCK,
            () -> "R-" + sequence.incrementAndGet(), new InMemoryBookCategoryRepository(), copies);

    @Test
    void catalogMetadataAndCopyLifecycleStaySeparate() {
        AddBookRequest request = new AddBookRequest("978-7-111-00000-0", "新书", "作者", "C001",
                "出版社", 2026, "中文");
        failure(ErrorCodes.AUTH_FORBIDDEN, () -> service.addBook(READER, request));
        BookDTO created = service.addBook(ADMIN, request);
        assertEquals("9787111000000", created.getIsbn());
        assertEquals(0, created.getTotalCount());
        failure(ErrorCodes.LIBRARY_DUPLICATE_ISBN, () -> service.addBook(ADMIN, request));

        BookCopyDTO copy = service.addBookCopy(ADMIN,
                new AddBookCopyRequest(created.getBookId(), " BC-001 ", "九龙湖", "TP312/1"));
        assertEquals("BC-001", copy.getBarcode());
        assertEquals("AVAILABLE", copy.getStatus());
        failure(ErrorCodes.LIBRARY_DUPLICATE_BARCODE,
                () -> service.addBookCopy(ADMIN,
                        new AddBookCopyRequest("B001", "BC-001", "四牌楼", "TP312/2")));
        assertEquals(1, service.searchBooksForAdmin(ADMIN,
                new BookSearchRequest(created.getIsbn(), null)).getBooks().getFirst().getTotalCount());

        BookCopyDTO moved = service.updateBookCopy(ADMIN,
                new UpdateBookCopyRequest(copy.getCopyId(), "四牌楼", "I247/2"));
        assertEquals(copy.getBarcode(), moved.getBarcode());
        assertEquals("AVAILABLE", moved.getStatus());
        BookCopyDTO withdrawn = service.withdrawBookCopy(ADMIN, new BookCopyIdRequest(copy.getCopyId()));
        assertEquals("WITHDRAWN", withdrawn.getStatus());
        assertEquals(0, service.searchBooksForAdmin(ADMIN,
                new BookSearchRequest(created.getIsbn(), null)).getBooks().getFirst().getTotalCount());
        assertEquals("WITHDRAWN", service.listBookCopies(ADMIN,
                new ListBookCopiesRequest(created.getBookId())).getFirst().getStatus());
        failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                () -> service.updateBookCopy(ADMIN,
                        new UpdateBookCopyRequest(copy.getCopyId(), "九龙湖", "TP312/2")));
        failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                () -> service.withdrawBookCopy(ADMIN, new BookCopyIdRequest(copy.getCopyId())));
    }

    @Test
    void statusHasDedicatedValidatedOperationAndDoesNotLoseMetadata() {
        BookDTO inactive = service.setBookStatus(ADMIN, new SetBookStatusRequest("B001", "inactive"));
        assertEquals("INACTIVE", inactive.getStatus());
        assertEquals("Java编程思想", inactive.getTitle());
        assertEquals(5, inactive.getTotalCount());
        assertEquals(0, inactive.getAvailableCount());
        assertTrue(service.searchBooks(new BookSearchRequest("B001", null)).getBooks().isEmpty());
        BookDTO edited = service.updateBook(ADMIN, new UpdateBookRequest("B001", inactive.getIsbn(),
                "新书名", inactive.getAuthor(), "C002", "新出版社", 2025, "中文"));
        assertEquals("INACTIVE", edited.getStatus());
        assertEquals("文学", edited.getCategoryName());
        assertEquals(5, edited.getTotalCount());
        assertEquals(0, edited.getAvailableCount());
        failure(ErrorCodes.LIBRARY_INVALID_BOOK_STATUS,
                () -> service.setBookStatus(ADMIN, new SetBookStatusRequest("B001", "DELETED")));
    }

    @Test
    void administratorCanQueryCurrentHistoryAndOverdueAcrossUsers() {
        BookCopy overdueCopy = copies.findById("CP-B001-001").orElseThrow();
        copies.update(overdueCopy.withStatus(BookCopyStatus.LOANED));
        records.save(new BorrowRecord("OVERDUE", "U-1", overdueCopy.copyId(),
                NOW.minusDays(40), NOW.minusDays(10), BorrowStatus.BORROWED));
        BorrowRecord returned = new BorrowRecord("RETURNED", "U-2", "CP-B002-001",
                NOW.minusDays(20), NOW.plusDays(10), BorrowStatus.BORROWED).returnedAt(NOW.minusDays(1));
        records.save(returned);

        List<AdminBorrowRecordDTO> current = service.queryBorrows(ADMIN,
                new AdminBorrowQueryRequest(AdminBorrowQueryRequest.CURRENT));
        List<AdminBorrowRecordDTO> history = service.queryBorrows(ADMIN,
                new AdminBorrowQueryRequest(AdminBorrowQueryRequest.HISTORY));
        List<AdminBorrowRecordDTO> overdue = service.queryBorrows(ADMIN,
                new AdminBorrowQueryRequest(AdminBorrowQueryRequest.OVERDUE));
        assertEquals(List.of("OVERDUE"), current.stream().map(AdminBorrowRecordDTO::getRecordId).toList());
        assertEquals(List.of("RETURNED"), history.stream().map(AdminBorrowRecordDTO::getRecordId).toList());
        assertEquals(List.of("OVERDUE"), overdue.stream().map(AdminBorrowRecordDTO::getRecordId).toList());
        assertTrue(overdue.getFirst().isOverdue());
        assertEquals("SEU-B001-001", overdue.getFirst().getBarcode());
        failure(ErrorCodes.AUTH_FORBIDDEN, () -> service.queryBorrows(READER,
                new AdminBorrowQueryRequest(AdminBorrowQueryRequest.CURRENT)));
        assertThrows(IllegalArgumentException.class, () -> service.queryBorrows(ADMIN,
                new AdminBorrowQueryRequest("UPCOMING")));
    }

    private static void failure(String code, Runnable action) {
        assertEquals(code, assertThrows(LibraryBusinessException.class, action::run).code());
    }
}
