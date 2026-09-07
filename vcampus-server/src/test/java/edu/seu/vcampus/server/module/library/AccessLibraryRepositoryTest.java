package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.CopyBorrowRequest;
import edu.seu.vcampus.common.library.CopyReturnRequest;
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

    private LibraryService service(Repositories repositories, String recordId) {
        return service(repositories, repositories.records(), recordId);
    }

    private LibraryService service(Repositories repositories,
            BorrowRecordRepository records, String recordId) {
        return new LibraryService(
                repositories.books(), records, CLOCK, () -> recordId,
                repositories.categories(), repositories.copies(), repositories.store());
    }

    private Repositories repositories(Path path) {
        AccessLibraryStore store = new AccessLibraryStore(new AccessDatabase(path));
        return new Repositories(
                store,
                new AccessBookRepository(store),
                new AccessBookCopyRepository(store),
                new AccessBorrowRecordRepository(store),
                new AccessBookCategoryRepository(store));
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

    private record Repositories(
            AccessLibraryStore store,
            AccessBookRepository books,
            AccessBookCopyRepository copies,
            AccessBorrowRecordRepository records,
            AccessBookCategoryRepository categories) {
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
}
