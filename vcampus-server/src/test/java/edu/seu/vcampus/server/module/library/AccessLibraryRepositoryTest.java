package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.CopyBorrowRequest;
import edu.seu.vcampus.common.library.CopyReturnRequest;
import edu.seu.vcampus.common.library.CreateReservationRequest;
import edu.seu.vcampus.common.library.ReservationDTO;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessLibraryRepositoryTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-06T02:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void createsSchemaSeedsCatalogAndPersistsRepositoryChanges() {
        Path databasePath = temporaryDirectory.resolve("library.accdb");
        Repositories first = repositories(databasePath);
        BookDTO book = first.books().findById("B001").orElseThrow();
        BookCopy copy = first.copies().findByBarcode("SEU-B001-001").orElseThrow();
        assertTrue(first.copies().findByBookId("B001").stream()
                .allMatch(value -> value.status() == BookCopyStatus.AVAILABLE),
                "a new database must not invent returned copies waiting for shelving");

        first.books().update(new BookDTO(
                book.getBookId(), book.getIsbn(), "持久化后的书名", book.getAuthor(),
                book.getCategoryId(), book.getCategoryName(), book.getPublisher(),
                book.getPublicationYear(), book.getLanguage(), book.getStatus(), List.of()));
        first.copies().update(copy.withLocation("九龙湖校区—测试馆藏地", "TP312/TEST"));

        Repositories restarted = repositories(databasePath);
        assertTrue(Files.exists(databasePath));
        assertEquals(5, restarted.books().searchAll("").size());
        assertEquals(3, restarted.categories().findAll().size());
        assertEquals("持久化后的书名",
                restarted.books().findById("B001").orElseThrow().getTitle());
        BookCopy persisted = restarted.copies()
                .findByBarcode("SEU-B001-001").orElseThrow();
        assertEquals("九龙湖校区—测试馆藏地", persisted.location());
        assertEquals("TP312/TEST", persisted.callNumber());
    }

    @Test
    void borrowAndReturnSurviveRepositoryRestart() {
        Path databasePath = temporaryDirectory.resolve("circulation.accdb");
        Repositories first = repositories(databasePath);
        service(first, "R-BORROW").borrowCopy(
                "U-PERSIST-001", new CopyBorrowRequest("SEU-B001-001"));

        Repositories afterBorrow = repositories(databasePath);
        assertEquals(BookCopyStatus.LOANED, afterBorrow.copies()
                .findByBarcode("SEU-B001-001").orElseThrow().status());
        BorrowRecord borrowed = afterBorrow.records()
                .findBorrowedByUserId("U-PERSIST-001").getFirst();
        assertEquals(LocalDateTime.of(2026, 9, 6, 2, 0), borrowed.borrowTime());
        assertEquals(LocalDateTime.of(2026, 10, 6, 2, 0), borrowed.dueTime());

        service(afterBorrow, "UNUSED").returnCopy(
                "U-PERSIST-001", new CopyReturnRequest("SEU-B001-001"));

        Repositories afterReturn = repositories(databasePath);
        assertEquals(BookCopyStatus.WAITING_SHELVING, afterReturn.copies()
                .findByBarcode("SEU-B001-001").orElseThrow().status());
        BorrowRecord returned = afterReturn.records().findById("R-BORROW").orElseThrow();
        assertEquals(BorrowStatus.RETURNED, returned.status());
        assertEquals(LocalDateTime.of(2026, 9, 6, 2, 0), returned.returnTime());
        assertTrue(afterReturn.records().findBorrowedByUserId("U-PERSIST-001").isEmpty());
    }

    @Test
    void borrowRollsBackCopyAndRecordWhenSecondWriteFails() {
        Path databasePath = temporaryDirectory.resolve("borrow-rollback.accdb");
        Repositories repositories = repositories(databasePath);
        BorrowRecordRepository failing = failAfterSave(repositories.records());
        LibraryService service = service(repositories, failing, "R-ROLLBACK-BORROW");

        assertThrows(IllegalStateException.class, () -> service.borrowCopy(
                "U-ROLLBACK-001", new CopyBorrowRequest("SEU-B001-001")));

        Repositories restarted = repositories(databasePath);
        assertEquals(BookCopyStatus.AVAILABLE, restarted.copies()
                .findByBarcode("SEU-B001-001").orElseThrow().status());
        assertTrue(restarted.records().findById("R-ROLLBACK-BORROW").isEmpty(),
                "the record inserted before the failure must be rolled back");

        service(repositories, "R-AFTER-ROLLBACK").borrowCopy(
                "U-ROLLBACK-001", new CopyBorrowRequest("SEU-B001-001"));
        Repositories afterRetry = repositories(databasePath);
        assertEquals(BookCopyStatus.LOANED, afterRetry.copies()
                .findByBarcode("SEU-B001-001").orElseThrow().status());
        assertTrue(afterRetry.records().findById("R-AFTER-ROLLBACK").isPresent(),
                "the failed transaction must not leave a stale thread-bound connection");
    }

    @Test
    void returnRollsBackCopyAndRecordWhenSecondWriteFails() {
        Path databasePath = temporaryDirectory.resolve("return-rollback.accdb");
        Repositories repositories = repositories(databasePath);
        service(repositories, "R-ROLLBACK-RETURN").borrowCopy(
                "U-ROLLBACK-002", new CopyBorrowRequest("SEU-B001-001"));
        BorrowRecordRepository failing = failAfterUpdate(repositories.records());
        LibraryService service = service(repositories, failing, "UNUSED");

        assertThrows(IllegalStateException.class, () -> service.returnCopy(
                "U-ROLLBACK-002", new CopyReturnRequest("SEU-B001-001")));

        Repositories restarted = repositories(databasePath);
        assertEquals(BookCopyStatus.LOANED, restarted.copies()
                .findByBarcode("SEU-B001-001").orElseThrow().status());
        assertEquals(BorrowStatus.BORROWED, restarted.records()
                .findById("R-ROLLBACK-RETURN").orElseThrow().status());
    }

    @Test
    void databaseUniqueIndexesRejectDuplicateIsbnAndBarcode() {
        Path databasePath = temporaryDirectory.resolve("unique.accdb");
        Repositories repositories = repositories(databasePath);
        BookDTO existing = repositories.books().findById("B001").orElseThrow();

        BookDTO duplicateIsbn = new BookDTO(
                "B-UNIQUE", existing.getIsbn(), "另一本书", "作者",
                "C001", "计算机", "出版社", 2026, "中文", "ACTIVE", List.of());
        assertThrows(LibraryPersistenceException.class,
                () -> repositories.books().insert(duplicateIsbn));

        BookCopy existingCopy = repositories.copies()
                .findByBarcode("SEU-B001-001").orElseThrow();
        BookCopy duplicateBarcode = new BookCopy(
                "CP-UNIQUE", existingCopy.barcode(), "B002",
                "九龙湖", "TP312", BookCopyStatus.AVAILABLE);
        assertThrows(LibraryPersistenceException.class,
                () -> repositories.copies().insert(duplicateBarcode));
        assertFalse(repositories.books().findIncludingInactive("B-UNIQUE").isPresent());
        assertFalse(repositories.copies().findById("CP-UNIQUE").isPresent());
    }

    @Test
    void reservationAndReservedCopySurviveRepositoryRestart() {
        Path databasePath = temporaryDirectory.resolve("reservation.accdb");
        Repositories first = repositories(databasePath);
        ReservationDTO created = service(first, "BR-UNUSED").createReservation(
                "U-RESERVE", new CreateReservationRequest(
                        "B001", "九龙湖校区—中文图书阅览室3"));

        Repositories restarted = repositories(databasePath);
        Reservation reservation = restarted.reservations()
                .findById(created.getReservationId()).orElseThrow();
        assertEquals(ReservationStatus.READY_FOR_PICKUP, reservation.status());
        assertEquals(created.getAssignedBarcode(), restarted.copies()
                .findById(reservation.assignedCopyId()).orElseThrow().barcode());
        assertEquals(BookCopyStatus.RESERVED, restarted.copies()
                .findById(reservation.assignedCopyId()).orElseThrow().status());
    }

    @Test
    void newLibrarySchemaReceivesConsistentDemonstrationActivity() {
        Path databasePath = temporaryDirectory.resolve("demonstration.accdb");
        Repositories repositories = repositories(databasePath);

        AccessLibraryDemonstrationData.seedIfEligible(
                repositories.store(), repositories.copies(), repositories.records(),
                repositories.reservations(), CLOCK);

        assertEquals(2, repositories.records().findAll().size());
        BorrowRecord current = repositories.records()
                .findById(AccessLibraryDemonstrationData.CURRENT_RECORD_ID)
                .orElseThrow();
        assertEquals(AccessLibraryDemonstrationData.DEMO_USER_ID, current.userId());
        assertEquals(BorrowStatus.BORROWED, current.status());
        assertEquals(BookCopyStatus.LOANED, repositories.copies()
                .findById(current.copyId()).orElseThrow().status());

        BorrowRecord history = repositories.records()
                .findById(AccessLibraryDemonstrationData.HISTORY_RECORD_ID)
                .orElseThrow();
        assertEquals(BorrowStatus.RETURNED, history.status());
        assertEquals(BookCopyStatus.AVAILABLE, repositories.copies()
                .findById(history.copyId()).orElseThrow().status());

        Reservation ready = repositories.reservations()
                .findById(AccessLibraryDemonstrationData.READY_RESERVATION_ID)
                .orElseThrow();
        assertEquals(ReservationStatus.READY_FOR_PICKUP, ready.status());
        assertEquals(24, java.time.Duration.between(
                ready.readyAt(), ready.expiresAt()).toHours());
        assertEquals(BookCopyStatus.RESERVED, repositories.copies()
                .findById(ready.assignedCopyId()).orElseThrow().status());
    }

    @Test
    void existingEmptyLibrarySchemaDoesNotReceiveDemonstrationActivity() {
        Path databasePath = temporaryDirectory.resolve("existing-empty.accdb");
        Repositories first = repositories(databasePath);
        assertTrue(first.store().isNewlyCreatedLibrarySchema());
        assertTrue(first.records().findAll().isEmpty());
        assertTrue(first.reservations().findAll().isEmpty());

        Repositories restarted = repositories(databasePath);
        assertFalse(restarted.store().isNewlyCreatedLibrarySchema());
        AccessLibraryDemonstrationData.seedIfEligible(
                restarted.store(), restarted.copies(), restarted.records(),
                restarted.reservations(), CLOCK);

        assertTrue(restarted.records().findAll().isEmpty());
        assertTrue(restarted.reservations().findAll().isEmpty());
        assertTrue(restarted.copies().findByBookId("B001").stream()
                .allMatch(copy -> copy.status() == BookCopyStatus.AVAILABLE));
    }

    @Test
    void demonstrationActivityRollsBackWhenFinalWriteFails() {
        Path databasePath = temporaryDirectory.resolve("demonstration-rollback.accdb");
        Repositories repositories = repositories(databasePath);
        ReservationRepository failing = failAfterUpdate(repositories.reservations());

        assertThrows(IllegalStateException.class,
                () -> AccessLibraryDemonstrationData.seedIfEligible(
                        repositories.store(), repositories.copies(), repositories.records(),
                        failing, CLOCK));

        Repositories restarted = repositories(databasePath);
        assertTrue(restarted.records().findAll().isEmpty());
        assertTrue(restarted.reservations().findAll().isEmpty());
        assertEquals(BookCopyStatus.AVAILABLE, restarted.copies()
                .findByBarcode(AccessLibraryDemonstrationData.LOANED_BARCODE)
                .orElseThrow().status());
        assertEquals(BookCopyStatus.AVAILABLE, restarted.copies()
                .findByBarcode(AccessLibraryDemonstrationData.RESERVED_BARCODE)
                .orElseThrow().status());
    }

    @Test
    void reservationAssignmentRollsBackWhenFinalWriteFails() {
        Path databasePath = temporaryDirectory.resolve("reservation-rollback.accdb");
        Repositories repositories = repositories(databasePath);
        ReservationRepository failing = failAfterUpdate(repositories.reservations());
        LibraryService service = service(repositories, repositories.records(), failing,
                "BR-UNUSED", "RS-ROLLBACK");

        assertThrows(IllegalStateException.class, () -> service.createReservation(
                "U-ROLLBACK", new CreateReservationRequest(
                        "B001", "九龙湖校区—中文图书阅览室3")));

        Repositories restarted = repositories(databasePath);
        assertTrue(restarted.reservations().findAll().isEmpty());
        assertEquals(BookCopyStatus.AVAILABLE, restarted.copies()
                .findByBarcode("SEU-B001-001").orElseThrow().status());
    }

    @Test
    void reservedBorrowRollsBackCopyRecordAndReservationTogether() {
        Path databasePath = temporaryDirectory.resolve("reserved-borrow-rollback.accdb");
        Repositories repositories = repositories(databasePath);
        ReservationDTO ready = service(repositories, "BR-SETUP").createReservation(
                "U-ROLLBACK", new CreateReservationRequest(
                        "B001", "九龙湖校区—中文图书阅览室3"));
        ReservationRepository failing = failAfterUpdate(repositories.reservations());
        LibraryService service = service(repositories, repositories.records(), failing,
                "BR-ROLLBACK", "RS-UNUSED");

        assertThrows(IllegalStateException.class, () -> service.borrowCopy(
                "U-ROLLBACK", new CopyBorrowRequest(ready.getAssignedBarcode())));

        Repositories restarted = repositories(databasePath);
        assertEquals(BookCopyStatus.RESERVED, restarted.copies()
                .findByBarcode(ready.getAssignedBarcode()).orElseThrow().status());
        assertEquals(ReservationStatus.READY_FOR_PICKUP, restarted.reservations()
                .findById(ready.getReservationId()).orElseThrow().status());
        assertTrue(restarted.records().findById("BR-ROLLBACK").isEmpty());
    }

    private LibraryService service(Repositories repositories, String recordId) {
        return service(repositories, repositories.records(), recordId);
    }

    private LibraryService service(Repositories repositories,
            BorrowRecordRepository records, String recordId) {
        return service(repositories, records, repositories.reservations(),
                recordId, "RS-ACCESS");
    }

    private LibraryService service(Repositories repositories,
            BorrowRecordRepository records, ReservationRepository reservations,
            String recordId, String reservationId) {
        return new LibraryService(
                repositories.books(), records, CLOCK, () -> recordId,
                repositories.categories(), repositories.copies(), reservations,
                repositories.store(), () -> reservationId);
    }

    private Repositories repositories(Path path) {
        AccessLibraryStore store = new AccessLibraryStore(new AccessDatabase(path));
        return new Repositories(
                store,
                new AccessBookRepository(store),
                new AccessBookCopyRepository(store),
                new AccessBorrowRecordRepository(store),
                new AccessBookCategoryRepository(store),
                new AccessReservationRepository(store));
    }

    private BorrowRecordRepository failAfterSave(BorrowRecordRepository delegate) {
        return new DelegatingBorrowRecordRepository(delegate) {
            @Override
            public void save(BorrowRecord record) {
                delegate.save(record);
                throw new IllegalStateException("simulated failure after record insert");
            }
        };
    }

    private BorrowRecordRepository failAfterUpdate(BorrowRecordRepository delegate) {
        return new DelegatingBorrowRecordRepository(delegate) {
            @Override
            public void update(BorrowRecord record) {
                delegate.update(record);
                throw new IllegalStateException("simulated failure after record update");
            }
        };
    }

    private ReservationRepository failAfterUpdate(ReservationRepository delegate) {
        return new DelegatingReservationRepository(delegate) {
            @Override
            public void update(Reservation reservation) {
                delegate.update(reservation);
                throw new IllegalStateException("simulated failure after reservation update");
            }
        };
    }

    private record Repositories(
            AccessLibraryStore store,
            AccessBookRepository books,
            AccessBookCopyRepository copies,
            AccessBorrowRecordRepository records,
            AccessBookCategoryRepository categories,
            AccessReservationRepository reservations) {
    }

    private static class DelegatingBorrowRecordRepository implements BorrowRecordRepository {
        private final BorrowRecordRepository delegate;

        DelegatingBorrowRecordRepository(BorrowRecordRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<BorrowRecord> findBorrowedByUserId(String userId) {
            return delegate.findBorrowedByUserId(userId);
        }

        @Override
        public List<BorrowRecord> findByUserId(String userId) {
            return delegate.findByUserId(userId);
        }

        @Override
        public List<BorrowRecord> findAll() {
            return delegate.findAll();
        }

        @Override
        public Optional<BorrowRecord> findById(String recordId) {
            return delegate.findById(recordId);
        }

        @Override
        public Optional<BorrowRecord> findBorrowedByCopyId(String copyId) {
            return delegate.findBorrowedByCopyId(copyId);
        }

        @Override
        public void save(BorrowRecord record) {
            delegate.save(record);
        }

        @Override
        public void update(BorrowRecord record) {
            delegate.update(record);
        }
    }

    private static class DelegatingReservationRepository implements ReservationRepository {
        private final ReservationRepository delegate;

        DelegatingReservationRepository(ReservationRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<Reservation> findAll() { return delegate.findAll(); }

        @Override
        public List<Reservation> findByUserId(String userId) {
            return delegate.findByUserId(userId);
        }

        @Override
        public Optional<Reservation> findById(String reservationId) {
            return delegate.findById(reservationId);
        }

        @Override
        public void save(Reservation reservation) { delegate.save(reservation); }

        @Override
        public void update(Reservation reservation) { delegate.update(reservation); }
    }
}
