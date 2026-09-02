package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookBorrowRequest;
import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LibraryServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-02T08:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 2, 8, 0);

    @Test
    void validatedKeywordIsTrimmedAndPassedToRepository() {
        BookDTO book = new BookDTO(
                "B001", "9787111213826", "Java编程思想", "Bruce Eckel", "计算机", 5, 2);
        TrackingBookRepository repository = new TrackingBookRepository(book);
        LibraryService service = new LibraryService(repository);

        BookSearchResult result = service.searchBooks(new BookSearchRequest("  Java  "));

        assertEquals("Java", repository.receivedKeyword);
        assertEquals(List.of(book), result.getBooks());
    }

    @Test
    void blankKeywordIsRejectedBeforeRepositoryCall() {
        TrackingBookRepository repository = new TrackingBookRepository(null);
        LibraryService service = new LibraryService(repository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.searchBooks(new BookSearchRequest("   ")));
        assertEquals(null, repository.receivedKeyword);
    }

    @Test
    void borrowUsesSessionUserCreatesThirtyDayRecordAndDecrementsStock() {
        InMemoryBookRepository books = new InMemoryBookRepository();
        InMemoryBorrowRecordRepository records = new InMemoryBorrowRecordRepository();
        LibraryService service = service(books, records);

        service.borrowBook("U-001", new BookBorrowRequest("B001"));

        assertEquals(1, books.findById("B001").orElseThrow().getAvailableCount());
        BorrowRecord record = records.findBorrowedByUserId("U-001").getFirst();
        assertEquals("R-001", record.recordId());
        assertEquals("U-001", record.userId());
        assertEquals("B001", record.bookId());
        assertEquals(NOW, record.borrowTime());
        assertEquals(NOW.plusDays(30), record.dueTime());
        assertEquals(BorrowStatus.BORROWED, record.status());
    }

    @Test
    void borrowIsRejectedWhenNoCopyIsAvailable() {
        LibraryBusinessException exception = assertBorrowFailure(
                new InMemoryBookRepository(),
                new InMemoryBorrowRecordRepository(),
                "U-001",
                "B004");

        assertEquals(ErrorCodes.LIBRARY_NO_AVAILABLE_COPY, exception.code());
    }

    @Test
    void borrowIsRejectedWhenBookDoesNotExist() {
        LibraryBusinessException exception = assertBorrowFailure(
                new InMemoryBookRepository(),
                new InMemoryBorrowRecordRepository(),
                "U-001",
                "MISSING");

        assertEquals(ErrorCodes.LIBRARY_BOOK_NOT_FOUND, exception.code());
    }

    @Test
    void blankBookIdIsRejectedBeforeRepositoriesAreChanged() {
        InMemoryBookRepository books = new InMemoryBookRepository();
        InMemoryBorrowRecordRepository records = new InMemoryBorrowRecordRepository();
        LibraryService service = service(books, records);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.borrowBook("U-001", new BookBorrowRequest("   ")));

        assertEquals(2, books.findById("B001").orElseThrow().getAvailableCount());
        assertEquals(0, records.findBorrowedByUserId("U-001").size());
    }

    @Test
    void sixthActiveBorrowIsRejected() {
        InMemoryBorrowRecordRepository records = new InMemoryBorrowRecordRepository();
        for (int index = 1; index <= 5; index++) {
            records.save(record("R-00" + index, "U-001", "ACTIVE-" + index, NOW.plusDays(1)));
        }

        LibraryBusinessException exception = assertBorrowFailure(
                new InMemoryBookRepository(), records, "U-001", "B001");

        assertEquals(ErrorCodes.LIBRARY_BORROW_LIMIT_REACHED, exception.code());
    }

    @Test
    void sameBookCannotBeBorrowedTwiceBeforeReturn() {
        InMemoryBorrowRecordRepository records = new InMemoryBorrowRecordRepository();
        records.save(record("R-001", "U-001", "B001", NOW.plusDays(1)));

        LibraryBusinessException exception = assertBorrowFailure(
                new InMemoryBookRepository(), records, "U-001", "B001");

        assertEquals(ErrorCodes.LIBRARY_ALREADY_BORROWED, exception.code());
    }

    @Test
    void overdueBorrowPreventsNewBorrow() {
        InMemoryBorrowRecordRepository records = new InMemoryBorrowRecordRepository();
        records.save(record("R-001", "U-001", "B005", NOW.minusSeconds(1)));

        LibraryBusinessException exception = assertBorrowFailure(
                new InMemoryBookRepository(), records, "U-001", "B001");

        assertEquals(ErrorCodes.LIBRARY_OVERDUE_BORROW_EXISTS, exception.code());
    }

    @Test
    void stockIsRestoredWhenBorrowRecordCannotBeSaved() {
        InMemoryBookRepository books = new InMemoryBookRepository();
        BorrowRecordRepository failingRecords = new BorrowRecordRepository() {
            @Override
            public List<BorrowRecord> findBorrowedByUserId(String userId) {
                return List.of();
            }

            @Override
            public void save(BorrowRecord record) {
                throw new IllegalStateException("simulated record failure");
            }
        };
        LibraryService service = service(books, failingRecords);

        assertThrows(
                IllegalStateException.class,
                () -> service.borrowBook("U-001", new BookBorrowRequest("B001")));

        assertEquals(2, books.findById("B001").orElseThrow().getAvailableCount());
    }

    @Test
    void concurrentUsersCannotBothBorrowTheLastCopy() throws Exception {
        InMemoryBookRepository books = new InMemoryBookRepository();
        InMemoryBorrowRecordRepository records = new InMemoryBorrowRecordRepository();
        LibraryService service = service(books, records);
        Callable<Boolean> first = () -> attemptBorrow(service, "U-001", "B002");
        Callable<Boolean> second = () -> attemptBorrow(service, "U-002", "B002");

        List<Boolean> outcomes;
        try (var executor = Executors.newFixedThreadPool(2)) {
            outcomes = executor.invokeAll(List.of(first, second)).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .toList();
        }

        assertEquals(1, outcomes.stream().filter(Boolean::booleanValue).count());
        assertEquals(0, books.findById("B002").orElseThrow().getAvailableCount());
        int recordCount = records.findBorrowedByUserId("U-001").size()
                + records.findBorrowedByUserId("U-002").size();
        assertEquals(1, recordCount);
    }

    private LibraryBusinessException assertBorrowFailure(
            BookRepository books,
            BorrowRecordRepository records,
            String userId,
            String bookId) {
        LibraryService service = service(books, records);
        return assertThrows(
                LibraryBusinessException.class,
                () -> service.borrowBook(userId, new BookBorrowRequest(bookId)));
    }

    private LibraryService service(
            BookRepository books,
            BorrowRecordRepository records) {
        return new LibraryService(books, records, FIXED_CLOCK, () -> "R-001");
    }

    private BorrowRecord record(
            String recordId,
            String userId,
            String bookId,
            LocalDateTime dueTime) {
        return new BorrowRecord(
                recordId,
                userId,
                bookId,
                NOW.minusDays(1),
                dueTime,
                BorrowStatus.BORROWED);
    }

    private boolean attemptBorrow(LibraryService service, String userId, String bookId) {
        try {
            service.borrowBook(userId, new BookBorrowRequest(bookId));
            return true;
        } catch (LibraryBusinessException exception) {
            assertEquals(ErrorCodes.LIBRARY_NO_AVAILABLE_COPY, exception.code());
            return false;
        }
    }

    private static final class TrackingBookRepository implements BookRepository {

        private final BookDTO book;
        private String receivedKeyword;

        private TrackingBookRepository(BookDTO book) {
            this.book = book;
        }

        @Override
        public List<BookDTO> search(String keyword) {
            receivedKeyword = keyword;
            return book == null ? List.of() : List.of(book);
        }

        @Override
        public Optional<BookDTO> findById(String bookId) {
            return Optional.ofNullable(book);
        }

        @Override
        public boolean decrementAvailableCount(String bookId) {
            return false;
        }

        @Override
        public void incrementAvailableCount(String bookId) {
        }
    }
}
