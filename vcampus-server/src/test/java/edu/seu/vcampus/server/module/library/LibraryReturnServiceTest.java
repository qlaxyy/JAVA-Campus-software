package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookBorrowRequest;
import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.BookReturnRequest;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BorrowRecordDTO;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LibraryReturnServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-04T08:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 4, 8, 0);
    private final InMemoryBookRepository books = new InMemoryBookRepository();
    private final InMemoryBorrowRecordRepository records = new InMemoryBorrowRecordRepository();
    private final LibraryService service = service(books, records);

    @Test
    void returnRestoresStockSetsServerTimeAndPreservesHistory() {
        String recordId = borrow("U1", "B001");
        service.returnBook("U1", new BookReturnRequest(recordId));

        BorrowRecord record = records.findById(recordId).orElseThrow();
        assertEquals(BorrowStatus.RETURNED, record.status());
        assertEquals(NOW, record.returnTime());
        assertEquals(NOW, record.borrowTime());
        assertEquals(NOW.plusDays(30), record.dueTime());
        assertEquals(2, stock("B001"));
        assertTrue(records.findBorrowedByUserId("U1").isEmpty());
        BorrowRecordDTO history = service.getBorrowRecords("U1").getFirst();
        assertEquals("Java编程思想", history.getBookTitle());
        assertEquals("RETURNED", history.getStatus());
        assertEquals(NOW, history.getReturnTime());
        assertFalse(history.isOverdue());
    }

    @Test
    void returningAgainDoesNotIncreaseStockOrRewriteTime() {
        String id = borrow("U1", "B001");
        service.returnBook("U1", new BookReturnRequest(id));
        BorrowRecord returned = records.findById(id).orElseThrow();

        assertReturnFailure(service, "U1", id, ErrorCodes.LIBRARY_ALREADY_RETURNED);

        assertEquals(2, stock("B001"));
        assertSame(returned, records.findById(id).orElseThrow());
    }

    @Test
    void foreignAndMissingRecordsHaveSameErrorAndDoNotChangeStock() {
        String id = borrow("U1", "B001");
        assertReturnFailure(service, "U2", id, ErrorCodes.LIBRARY_BORROW_RECORD_NOT_FOUND);
        assertReturnFailure(service, "U2", "missing", ErrorCodes.LIBRARY_BORROW_RECORD_NOT_FOUND);
        assertEquals(1, stock("B001"));
        assertEquals(BorrowStatus.BORROWED, records.findById(id).orElseThrow().status());
        assertTrue(service.getBorrowRecords("U2").isEmpty());
    }

    @Test
    void queryIncludesOnlyOwnCurrentAndHistoricalRecords() {
        String first = borrow("U1", "B001");
        service.returnBook("U1", new BookReturnRequest(first));
        borrow("U1", "B003");
        borrow("U2", "B002");

        List<BorrowRecordDTO> result = service.getBorrowRecords("U1");
        assertEquals(2, result.size());
        assertEquals(1, result.stream().filter(r -> r.getStatus().equals("BORROWED")).count());
        assertEquals(1, result.stream().filter(r -> r.getStatus().equals("RETURNED")).count());
        assertFalse(result.stream().anyMatch(r -> r.getBookId().equals("B002")));
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test
    void blankOrNullRecordIdIsRejectedWithoutMutation() {
        borrow("U1", "B001");
        for (String id : new String[] {null, "", "  "}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.returnBook("U1", new BookReturnRequest(id)));
        }
        assertEquals(1, stock("B001"));
        assertEquals(1, records.findBorrowedByUserId("U1").size());
    }

    @Test
    void overdueIsComputedAtQueryTimeAndExactDueTimeIsNotOverdue() {
        records.save(new BorrowRecord("R1", "U1", "B001", NOW.minusDays(30), NOW,
                BorrowStatus.BORROWED));
        assertFalse(service.getBorrowRecords("U1").getFirst().isOverdue());

        LibraryService later = new LibraryService(books, records,
                Clock.offset(CLOCK, java.time.Duration.ofSeconds(1)), () -> UUID.randomUUID().toString());
        assertTrue(later.getBorrowRecords("U1").getFirst().isOverdue());
        later.returnBook("U1", new BookReturnRequest("R1"));
        assertFalse(later.getBorrowRecords("U1").getFirst().isOverdue());
    }

    @Test
    void returningOverdueBookRestoresBorrowEligibility() {
        records.save(new BorrowRecord("OVERDUE", "U1", "B005", NOW.minusDays(31),
                NOW.minusDays(1), BorrowStatus.BORROWED));
        assertEquals(ErrorCodes.LIBRARY_OVERDUE_BORROW_EXISTS,
                assertThrows(LibraryBusinessException.class,
                        () -> service.borrowBook("U1", new BookBorrowRequest("B001"))).code());

        service.returnBook("U1", new BookReturnRequest("OVERDUE"));
        assertDoesNotThrow(() -> service.borrowBook("U1", new BookBorrowRequest("B001")));
    }

    @Test
    void returningOneOfFiveActiveRecordsFreesASlot() {
        for (int index = 1; index <= 5; index++) {
            records.save(new BorrowRecord("R" + index, "U1", "B00" + index,
                    NOW.minusDays(1), NOW.plusDays(29), BorrowStatus.BORROWED));
        }
        service.returnBook("U1", new BookReturnRequest("R1"));
        service.borrowBook("U1", new BookBorrowRequest("B001"));
        assertEquals(5, records.findBorrowedByUserId("U1").size());
        assertEquals(6, records.findByUserId("U1").size());
    }

    @Test
    void recordUpdateFailureCompensatesStockAndAllowsRetry() {
        String id = borrow("U1", "B001");
        BorrowRecord before = records.findById(id).orElseThrow();
        BorrowRecordRepository failing = new RecordDelegate(records) {
            @Override
            public void update(BorrowRecord record) { throw new IllegalStateException("update failed"); }
        };
        assertThrows(IllegalStateException.class,
                () -> service(books, failing).returnBook("U1", new BookReturnRequest(id)));

        assertEquals(1, stock("B001"));
        assertSame(before, records.findById(id).orElseThrow());
        service.returnBook("U1", new BookReturnRequest(id));
        assertEquals(2, stock("B001"));
    }

    @Test
    void stockIncrementFailureDoesNotMarkRecordReturned() {
        String id = borrow("U1", "B001");
        BookRepository failing = new BookRepository() {
            public List<BookDTO> search(String keyword) { return books.search(keyword); }
            public Optional<BookDTO> findById(String bookId) { return books.findById(bookId); }
            public boolean decrementAvailableCount(String bookId) {
                return books.decrementAvailableCount(bookId);
            }
            public void incrementAvailableCount(String bookId) {
                throw new IllegalStateException("stock failure");
            }
        };
        assertThrows(IllegalStateException.class,
                () -> service(failing, records).returnBook("U1", new BookReturnRequest(id)));
        assertEquals(BorrowStatus.BORROWED, records.findById(id).orElseThrow().status());
        assertEquals(1, stock("B001"));
    }

    @Test
    void missingBookCannotCreatePartialReturn() {
        records.save(new BorrowRecord("R1", "U1", "missing", NOW, NOW.plusDays(30),
                BorrowStatus.BORROWED));
        assertReturnFailure(service, "U1", "R1", ErrorCodes.LIBRARY_BOOK_NOT_FOUND);
        assertEquals(BorrowStatus.BORROWED, records.findById("R1").orElseThrow().status());
        assertEquals(1, service.getBorrowRecords("U1").size());
    }

    @Test
    void simultaneousReturnsRestoreExactlyOneCopy() throws Exception {
        String id = borrow("U1", "B001");
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> attemptReturn(start, id));
            var second = executor.submit(() -> attemptReturn(start, id));
            start.countDown();
            assertEquals(1, (first.get(5, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(5, TimeUnit.SECONDS) ? 1 : 0));
        }
        assertEquals(2, stock("B001"));
        assertEquals(1, records.findByUserId("U1").size());
    }

    @Test
    void queryWaitsUntilBothReturnWritesAreComplete() throws Exception {
        String id = borrow("U1", "B001");
        CountDownLatch updating = new CountDownLatch(1);
        CountDownLatch releaseUpdate = new CountDownLatch(1);
        LibraryService blocking = service(books, new RecordDelegate(records) {
            @Override
            public void update(BorrowRecord record) {
                updating.countDown();
                try {
                    if (!releaseUpdate.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("test update timeout");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                super.update(record);
            }
        });
        try (var executor = Executors.newFixedThreadPool(3)) {
            var returning = executor.submit(() -> blocking.returnBook("U1", new BookReturnRequest(id)));
            assertTrue(updating.await(5, TimeUnit.SECONDS));
            var querying = executor.submit(() -> blocking.getBorrowRecords("U1"));
            var searching = executor.submit(() -> blocking.searchBooks(new BookSearchRequest("Java")));
            try {
                assertThrows(java.util.concurrent.TimeoutException.class,
                        () -> querying.get(100, TimeUnit.MILLISECONDS));
                assertThrows(java.util.concurrent.TimeoutException.class,
                        () -> searching.get(100, TimeUnit.MILLISECONDS));
            } finally {
                releaseUpdate.countDown();
            }
            returning.get(5, TimeUnit.SECONDS);
            assertEquals("RETURNED", querying.get(5, TimeUnit.SECONDS).getFirst().getStatus());
            assertEquals(2, searching.get(5, TimeUnit.SECONDS).getBooks().getFirst().getAvailableCount());
        }
    }

    private boolean attemptReturn(CountDownLatch start, String id) throws InterruptedException {
        assertTrue(start.await(5, TimeUnit.SECONDS));
        try {
            service.returnBook("U1", new BookReturnRequest(id));
            return true;
        } catch (LibraryBusinessException exception) {
            assertEquals(ErrorCodes.LIBRARY_ALREADY_RETURNED, exception.code());
            return false;
        }
    }

    private String borrow(String userId, String bookId) {
        service.borrowBook(userId, new BookBorrowRequest(bookId));
        return records.findBorrowedByUserId(userId).stream()
                .filter(record -> record.bookId().equals(bookId)).findFirst().orElseThrow().recordId();
    }

    private int stock(String bookId) { return books.findById(bookId).orElseThrow().getAvailableCount(); }

    private static LibraryService service(BookRepository books, BorrowRecordRepository records) {
        return new LibraryService(books, records, CLOCK, () -> UUID.randomUUID().toString());
    }

    private static void assertReturnFailure(LibraryService service, String userId, String id, String code) {
        assertEquals(code, assertThrows(LibraryBusinessException.class,
                () -> service.returnBook(userId, new BookReturnRequest(id))).code());
    }

    private static class RecordDelegate implements BorrowRecordRepository {
        private final BorrowRecordRepository delegate;
        RecordDelegate(BorrowRecordRepository delegate) { this.delegate = delegate; }
        public List<BorrowRecord> findBorrowedByUserId(String userId) {
            return delegate.findBorrowedByUserId(userId);
        }
        public List<BorrowRecord> findByUserId(String userId) { return delegate.findByUserId(userId); }
        public Optional<BorrowRecord> findById(String id) { return delegate.findById(id); }
        public void save(BorrowRecord record) { delegate.save(record); }
        public void update(BorrowRecord record) { delegate.update(record); }
    }
}
