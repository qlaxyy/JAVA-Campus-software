package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookCopyDTO;
import edu.seu.vcampus.common.library.BookCopyIdRequest;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BorrowRecordDTO;
import edu.seu.vcampus.common.library.CopyBorrowRequest;
import edu.seu.vcampus.common.library.CopyReturnRequest;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.library.SetBookStatusRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerContext;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LibraryCirculationPhaseTwoTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-06T08:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 6, 8, 0);
    private static final SessionInfo ADMIN = new SessionInfo(
            "admin-token", "A-001", "libraryadmin", "图书管理员", Role.USER,
            Set.of(AdminScope.LIBRARY));
    private static final SessionInfo READER = new SessionInfo(
            "reader-token", "U-001", "reader", "读者", Role.USER);

    private final InMemoryBookRepository books = new InMemoryBookRepository();
    private final InMemoryBorrowRecordRepository records = new InMemoryBorrowRecordRepository();
    private final InMemoryBookCopyRepository copies =
            InMemoryBookCopyRepository.seededFrom(books.searchAll(""));
    private final AtomicInteger sequence = new AtomicInteger();
    private final LibraryService service = service(records, copies);

    @Test
    void barcodeBorrowCreatesCopyBoundRecordAndThirtyDayLoan() {
        service.borrowCopy("U-001", new CopyBorrowRequest("  SEU-B001-001  "));

        BorrowRecord record = records.findBorrowedByUserId("U-001").getFirst();
        assertEquals("U-001", record.userId());
        assertEquals("CP-B001-001", record.copyId());
        assertEquals(NOW, record.borrowTime());
        assertEquals(NOW.plusDays(30), record.dueTime());
        assertNull(record.returnTime());
        assertEquals(BookCopyStatus.LOANED,
                copies.findById(record.copyId()).orElseThrow().status());
        assertEquals(record.recordId(),
                records.findBorrowedByCopyId(record.copyId()).orElseThrow().recordId());

        BorrowRecordDTO dto = service.getBorrowRecords("U-001").getFirst();
        assertEquals("B001", dto.getBookId());
        assertEquals("Java编程思想", dto.getBookTitle());
        assertEquals("CP-B001-001", dto.getCopyId());
        assertEquals("SEU-B001-001", dto.getBarcode());
        assertEquals(4, available("B001"));
    }

    @Test
    void borrowRejectsUnknownInactiveAndUnavailableCopies() {
        failure(ErrorCodes.LIBRARY_COPY_NOT_FOUND,
                () -> service.borrowCopy("U-001", new CopyBorrowRequest("UNKNOWN")));
        assertThrows(IllegalArgumentException.class,
                () -> service.borrowCopy("U-001", new CopyBorrowRequest("  ")));

        BookCopy waiting = copies.findById("CP-B001-003").orElseThrow();
        copies.update(waiting.withStatus(BookCopyStatus.WAITING_SHELVING));
        BookCopy persistedWaiting = copies.findById("CP-B001-003").orElseThrow();
        assertEquals(BookCopyStatus.WAITING_SHELVING, persistedWaiting.status());
        failure(ErrorCodes.LIBRARY_COPY_NOT_AVAILABLE,
                () -> service.borrowCopy(
                        "U-001", new CopyBorrowRequest(persistedWaiting.barcode())));

        service.setBookStatus(ADMIN, new SetBookStatusRequest("B003", "INACTIVE"));
        failure(ErrorCodes.LIBRARY_COPY_NOT_AVAILABLE,
                () -> service.borrowCopy("U-001", new CopyBorrowRequest("SEU-B003-001")));
        assertTrue(records.findByUserId("U-001").isEmpty());
    }

    @Test
    void duplicateTitleLimitAndOverdueRulesUseActiveCopyRecords() {
        service.borrowCopy("U-001", new CopyBorrowRequest("SEU-B001-001"));
        failure(ErrorCodes.LIBRARY_ALREADY_BORROWED,
                () -> service.borrowCopy("U-001", new CopyBorrowRequest("SEU-B001-002")));

        for (String bookId : List.of("B002", "B003", "B004", "B005")) {
            BookCopy copy = copies.findByBookId(bookId).getFirst();
            if (copy.status() != BookCopyStatus.AVAILABLE) {
                copies.update(copy.withStatus(BookCopyStatus.AVAILABLE));
            }
            service.borrowCopy("U-001", new CopyBorrowRequest(copy.barcode()));
        }
        assertEquals(5, records.findBorrowedByUserId("U-001").size());
        failure(ErrorCodes.LIBRARY_BORROW_LIMIT_REACHED,
                () -> service.borrowCopy("U-001", new CopyBorrowRequest("SEU-B001-002")));

        BookCopy overdueCopy = copies.findById("CP-B005-002").orElseThrow();
        copies.update(overdueCopy.withStatus(BookCopyStatus.LOANED));
        records.save(new BorrowRecord("OVERDUE", "U-OVERDUE", overdueCopy.copyId(),
                NOW.minusDays(31), NOW.minusDays(1), BorrowStatus.BORROWED));
        failure(ErrorCodes.LIBRARY_OVERDUE_BORROW_EXISTS,
                () -> service.borrowCopy("U-OVERDUE", new CopyBorrowRequest("SEU-B001-002")));
    }

    @Test
    void returnEndsRecordButCopyWaitsForAdministratorShelving() {
        service.borrowCopy("U-001", new CopyBorrowRequest("SEU-B001-001"));
        failure(ErrorCodes.LIBRARY_BORROW_RECORD_NOT_FOUND,
                () -> service.returnCopy("U-OTHER", new CopyReturnRequest("SEU-B001-001")));

        service.returnCopy("U-001", new CopyReturnRequest("SEU-B001-001"));

        BorrowRecord record = records.findByUserId("U-001").getFirst();
        assertEquals(BorrowStatus.RETURNED, record.status());
        assertEquals(NOW, record.returnTime());
        assertEquals(BookCopyStatus.WAITING_SHELVING,
                copies.findById(record.copyId()).orElseThrow().status());
        assertEquals(4, available("B001"));
        assertTrue(records.findBorrowedByCopyId(record.copyId()).isEmpty());
        failure(ErrorCodes.LIBRARY_BORROW_RECORD_NOT_FOUND,
                () -> service.returnCopy("U-001", new CopyReturnRequest("SEU-B001-001")));

        failure(ErrorCodes.AUTH_FORBIDDEN,
                () -> service.shelveBookCopy(READER, new BookCopyIdRequest(record.copyId())));
        BookCopyDTO shelved = service.shelveBookCopy(
                ADMIN, new BookCopyIdRequest(record.copyId()));
        assertEquals("AVAILABLE", shelved.getStatus());
        assertEquals(5, available("B001"));
        failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                () -> service.shelveBookCopy(ADMIN, new BookCopyIdRequest(record.copyId())));
    }

    @Test
    void inactiveTitleDoesNotPreventReturnOfExistingLoan() {
        service.borrowCopy("U-001", new CopyBorrowRequest("SEU-B003-001"));
        service.setBookStatus(ADMIN, new SetBookStatusRequest("B003", "INACTIVE"));

        assertDoesNotThrow(() -> service.returnCopy(
                "U-001", new CopyReturnRequest("SEU-B003-001")));
        assertEquals(BookCopyStatus.WAITING_SHELVING,
                copies.findByBarcode("SEU-B003-001").orElseThrow().status());
    }

    @Test
    void borrowRecordFailureRestoresAvailableCopy() {
        BorrowRecordRepository failing = new RecordDelegate(records) {
            @Override
            public void save(BorrowRecord record) {
                throw new IllegalStateException("simulated record insert failure");
            }
        };
        LibraryService failingService = service(failing, copies);

        assertThrows(IllegalStateException.class, () -> failingService.borrowCopy(
                "U-001", new CopyBorrowRequest("SEU-B001-001")));

        assertEquals(BookCopyStatus.AVAILABLE,
                copies.findByBarcode("SEU-B001-001").orElseThrow().status());
        assertTrue(records.findByUserId("U-001").isEmpty());
    }

    @Test
    void returnRecordFailureRestoresLoanedCopy() {
        service.borrowCopy("U-001", new CopyBorrowRequest("SEU-B001-001"));
        BorrowRecord original = records.findBorrowedByUserId("U-001").getFirst();
        BorrowRecordRepository failing = new RecordDelegate(records) {
            @Override
            public void update(BorrowRecord record) {
                throw new IllegalStateException("simulated record update failure");
            }
        };

        assertThrows(IllegalStateException.class, () -> service(failing, copies).returnCopy(
                "U-001", new CopyReturnRequest("SEU-B001-001")));

        assertEquals(BookCopyStatus.LOANED,
                copies.findByBarcode("SEU-B001-001").orElseThrow().status());
        assertSame(original, records.findById(original.recordId()).orElseThrow());
    }

    @Test
    void copyTransitionFailureDoesNotCreateBorrowRecord() {
        BookCopyRepository failing = new CopyDelegate(copies) {
            @Override
            public void update(BookCopy copy) {
                if (copy.status() == BookCopyStatus.LOANED) {
                    throw new IllegalStateException("simulated copy update failure");
                }
                super.update(copy);
            }
        };

        assertThrows(IllegalStateException.class, () -> service(records, failing).borrowCopy(
                "U-001", new CopyBorrowRequest("SEU-B001-001")));

        assertEquals(BookCopyStatus.AVAILABLE,
                copies.findByBarcode("SEU-B001-001").orElseThrow().status());
        assertTrue(records.findByUserId("U-001").isEmpty());
    }

    @Test
    void copyTransitionFailureDoesNotReturnBorrowRecord() {
        service.borrowCopy("U-001", new CopyBorrowRequest("SEU-B001-001"));
        BorrowRecord original = records.findBorrowedByUserId("U-001").getFirst();
        BookCopyRepository failing = new CopyDelegate(copies) {
            @Override
            public void update(BookCopy copy) {
                if (copy.status() == BookCopyStatus.WAITING_SHELVING) {
                    throw new IllegalStateException("simulated copy update failure");
                }
                super.update(copy);
            }
        };

        assertThrows(IllegalStateException.class, () -> service(records, failing).returnCopy(
                "U-001", new CopyReturnRequest("SEU-B001-001")));

        assertSame(original, records.findById(original.recordId()).orElseThrow());
        assertEquals(BorrowStatus.BORROWED, original.status());
        assertEquals(BookCopyStatus.LOANED,
                copies.findByBarcode("SEU-B001-001").orElseThrow().status());
    }

    @Test
    void concurrentBorrowOfOneBarcodeCreatesExactlyOneLoan() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> tryBorrow("U-001"));
            var second = executor.submit(() -> tryBorrow("U-002"));
            assertEquals(1, (first.get(5, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(5, TimeUnit.SECONDS) ? 1 : 0));
        }
        assertEquals(1, records.findBorrowedByCopyId("CP-B001-001").stream().count());
        assertEquals(BookCopyStatus.LOANED,
                copies.findById("CP-B001-001").orElseThrow().status());
    }

    @Test
    void repositoryRejectsTwoActiveRecordsForOneCopy() {
        BorrowRecord first = new BorrowRecord("R-1", "U-001", "CP-B001-001",
                NOW, NOW.plusDays(30), BorrowStatus.BORROWED);
        BorrowRecord second = new BorrowRecord("R-2", "U-002", "CP-B001-001",
                NOW, NOW.plusDays(30), BorrowStatus.BORROWED);
        records.save(first);

        assertThrows(IllegalStateException.class, () -> records.save(second));
        assertSame(first, records.findBorrowedByCopyId("CP-B001-001").orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> new BorrowRecord(
                "bad", "U-1", "CP-1", NOW, NOW.minusSeconds(1), BorrowStatus.BORROWED));
    }

    @Test
    void handlersUseSessionIdentityAndProtectShelvingAction() {
        SessionInfo other = new SessionInfo(
                "other-token", "U-002", "other", "其他读者", Role.USER);
        ActionRouter router = new ActionRouter();
        new LibraryServerModule(service).registerHandlers(router,
                new ServerContext(token -> token == null
                        ? Optional.empty()
                        : Optional.ofNullable(switch (token) {
                            case "reader-token" -> READER;
                            case "other-token" -> other;
                            case "admin-token" -> ADMIN;
                            default -> null;
                        })));

        Response borrowed = router.dispatch(Request.create(LibraryActions.BORROW_COPY,
                READER.getToken(), new CopyBorrowRequest("SEU-B001-001")));
        assertTrue(borrowed.isSuccess());
        assertEquals("U-001", records.findBorrowedByCopyId("CP-B001-001")
                .orElseThrow().userId());
        assertEquals(ErrorCodes.LIBRARY_BORROW_RECORD_NOT_FOUND,
                router.dispatch(Request.create(LibraryActions.RETURN_COPY, other.getToken(),
                        new CopyReturnRequest("SEU-B001-001"))).getCode());
        assertTrue(router.dispatch(Request.create(LibraryActions.RETURN_COPY, READER.getToken(),
                new CopyReturnRequest("SEU-B001-001"))).isSuccess());
        assertEquals(ErrorCodes.AUTH_FORBIDDEN,
                router.dispatch(Request.create(LibraryActions.SHELVE_BOOK_COPY,
                        READER.getToken(), new BookCopyIdRequest("CP-B001-001"))).getCode());
        assertTrue(router.dispatch(Request.create(LibraryActions.SHELVE_BOOK_COPY,
                ADMIN.getToken(), new BookCopyIdRequest("CP-B001-001"))).isSuccess());
        assertEquals(ErrorCodes.AUTH_REQUIRED,
                router.dispatch(Request.create(LibraryActions.BORROW_COPY, null,
                        new CopyBorrowRequest("SEU-B001-002"))).getCode());
        assertEquals(ErrorCodes.COMMON_INVALID_REQUEST,
                router.dispatch(Request.create(LibraryActions.BORROW_COPY,
                        READER.getToken(), new BookCopyIdRequest("CP-B001-002"))).getCode());
        assertEquals(ErrorCodes.COMMON_INVALID_REQUEST,
                router.dispatch(Request.create(LibraryActions.RETURN_COPY,
                        READER.getToken(), new CopyBorrowRequest("SEU-B001-002"))).getCode());
        assertEquals(ErrorCodes.COMMON_INVALID_REQUEST,
                router.dispatch(Request.create(LibraryActions.SHELVE_BOOK_COPY,
                        ADMIN.getToken(), new CopyReturnRequest("SEU-B001-002"))).getCode());
    }

    private LibraryService service(BorrowRecordRepository recordRepository,
            BookCopyRepository copyRepository) {
        return new LibraryService(books, recordRepository, CLOCK,
                () -> "R-" + sequence.incrementAndGet(), copyRepository);
    }

    private int available(String bookId) {
        return service.searchBooks(new BookSearchRequest("", null)).getBooks().stream()
                .filter(book -> book.getBookId().equals(bookId)).findFirst().orElseThrow()
                .getAvailableCount();
    }

    private boolean tryBorrow(String userId) {
        try {
            service.borrowCopy(userId, new CopyBorrowRequest("SEU-B001-001"));
            return true;
        } catch (LibraryBusinessException exception) {
            assertEquals(ErrorCodes.LIBRARY_COPY_NOT_AVAILABLE, exception.code());
            return false;
        }
    }

    private static void failure(String code, Runnable action) {
        assertEquals(code, assertThrows(LibraryBusinessException.class, action::run).code());
    }

    private static class RecordDelegate implements BorrowRecordRepository {
        private final BorrowRecordRepository delegate;

        RecordDelegate(BorrowRecordRepository delegate) {
            this.delegate = delegate;
        }

        public List<BorrowRecord> findBorrowedByUserId(String userId) {
            return delegate.findBorrowedByUserId(userId);
        }

        public List<BorrowRecord> findByUserId(String userId) {
            return delegate.findByUserId(userId);
        }

        public Optional<BorrowRecord> findById(String recordId) {
            return delegate.findById(recordId);
        }

        public Optional<BorrowRecord> findBorrowedByCopyId(String copyId) {
            return delegate.findBorrowedByCopyId(copyId);
        }

        public List<BorrowRecord> findAll() {
            return delegate.findAll();
        }

        public void save(BorrowRecord record) {
            delegate.save(record);
        }

        public void update(BorrowRecord record) {
            delegate.update(record);
        }
    }

    private static class CopyDelegate implements BookCopyRepository {
        private final BookCopyRepository delegate;

        CopyDelegate(BookCopyRepository delegate) {
            this.delegate = delegate;
        }

        public List<BookCopy> findByBookId(String bookId) {
            return delegate.findByBookId(bookId);
        }

        public Optional<BookCopy> findById(String copyId) {
            return delegate.findById(copyId);
        }

        public Optional<BookCopy> findByBarcode(String barcode) {
            return delegate.findByBarcode(barcode);
        }

        public void insert(BookCopy copy) {
            delegate.insert(copy);
        }

        public void update(BookCopy copy) {
            delegate.update(copy);
        }

        public void replaceForBook(String bookId, List<BookCopy> replacements) {
            delegate.replaceForBook(bookId, replacements);
        }
    }
}
