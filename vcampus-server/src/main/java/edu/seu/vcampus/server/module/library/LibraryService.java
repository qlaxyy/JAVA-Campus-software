package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookBorrowRequest;
import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.BookReturnRequest;
import edu.seu.vcampus.common.library.BorrowRecordDTO;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.protocol.ErrorCodes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Library business rules independent from sockets and Swing. */
final class LibraryService {

    private static final int MAX_KEYWORD_LENGTH = 50;
    private static final int MAX_ACTIVE_BORROWS = 5;
    private static final int BORROW_DAYS = 30;

    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final Clock clock;
    private final Supplier<String> recordIdSupplier;
    private final Object circulationLock = new Object();

    LibraryService(BookRepository bookRepository) {
        this(bookRepository, new InMemoryBorrowRecordRepository());
    }

    LibraryService(
            BookRepository bookRepository,
            BorrowRecordRepository borrowRecordRepository) {
        this(
                bookRepository,
                borrowRecordRepository,
                Clock.systemDefaultZone(),
                () -> UUID.randomUUID().toString());
    }

    LibraryService(
            BookRepository bookRepository,
            BorrowRecordRepository borrowRecordRepository,
            Clock clock,
            Supplier<String> recordIdSupplier) {
        this.bookRepository = Objects.requireNonNull(
                bookRepository, "bookRepository must not be null");
        this.borrowRecordRepository = Objects.requireNonNull(
                borrowRecordRepository, "borrowRecordRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.recordIdSupplier = Objects.requireNonNull(
                recordIdSupplier, "recordIdSupplier must not be null");
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
        synchronized (circulationLock) {
            return new BookSearchResult(bookRepository.search(keyword.trim()));
        }
    }

    void borrowBook(String userId, BookBorrowRequest request) {
        String validatedUserId = requireText(userId, "userId");
        Objects.requireNonNull(request, "request must not be null");
        String bookId = requireText(request.getBookId(), "bookId");

        synchronized (circulationLock) {
            BookDTO book = bookRepository.findById(bookId)
                    .orElseThrow(() -> failure(
                            ErrorCodes.LIBRARY_BOOK_NOT_FOUND,
                            "The selected book does not exist."));
            List<BorrowRecord> currentBorrows =
                    borrowRecordRepository.findBorrowedByUserId(validatedUserId);
            LocalDateTime borrowTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);

            if (currentBorrows.stream().anyMatch(record -> record.isOverdueAt(borrowTime))) {
                throw failure(
                        ErrorCodes.LIBRARY_OVERDUE_BORROW_EXISTS,
                        "Return overdue books before borrowing another book.");
            }
            if (currentBorrows.size() >= MAX_ACTIVE_BORROWS) {
                throw failure(
                        ErrorCodes.LIBRARY_BORROW_LIMIT_REACHED,
                        "At most five books may be borrowed at the same time.");
            }
            if (currentBorrows.stream().anyMatch(record -> record.bookId().equals(bookId))) {
                throw failure(
                        ErrorCodes.LIBRARY_ALREADY_BORROWED,
                        "This book is already borrowed and has not been returned.");
            }
            if (book.getAvailableCount() <= 0) {
                throw failure(
                        ErrorCodes.LIBRARY_NO_AVAILABLE_COPY,
                        "No copy of this book is currently available.");
            }

            BorrowRecord record = new BorrowRecord(
                    recordIdSupplier.get(),
                    validatedUserId,
                    bookId,
                    borrowTime,
                    borrowTime.plusDays(BORROW_DAYS),
                    BorrowStatus.BORROWED);

            if (!bookRepository.decrementAvailableCount(bookId)) {
                throw failure(
                        ErrorCodes.LIBRARY_NO_AVAILABLE_COPY,
                        "No copy of this book is currently available.");
            }
            try {
                borrowRecordRepository.save(record);
            } catch (RuntimeException exception) {
                bookRepository.incrementAvailableCount(bookId);
                throw exception;
            }
        }
    }

    List<BorrowRecordDTO> getBorrowRecords(String userId) {
        String validatedUserId = requireText(userId, "userId");
        synchronized (circulationLock) {
            LocalDateTime now = LocalDateTime.now(clock);
            return borrowRecordRepository.findByUserId(validatedUserId).stream()
                    .sorted(Comparator.comparing(BorrowRecord::borrowTime).reversed()
                            .thenComparing(BorrowRecord::recordId))
                    .map(record -> new BorrowRecordDTO(
                            record.recordId(), record.bookId(),
                            bookRepository.findById(record.bookId()).map(BookDTO::getTitle)
                                    .orElse("图书信息不可用（" + record.bookId() + "）"),
                            record.borrowTime(), record.dueTime(), record.returnTime(),
                            record.status().name(), record.isOverdueAt(now)))
                    .toList();
        }
    }

    void returnBook(String userId, BookReturnRequest request) {
        String validatedUserId = requireText(userId, "userId");
        Objects.requireNonNull(request, "request must not be null");
        String recordId = requireText(request.getRecordId(), "recordId");
        synchronized (circulationLock) {
            // A foreign record is deliberately indistinguishable from a missing record.
            BorrowRecord record = borrowRecordRepository.findById(recordId)
                    .filter(value -> value.userId().equals(validatedUserId))
                    .orElseThrow(() -> failure(ErrorCodes.LIBRARY_BORROW_RECORD_NOT_FOUND,
                            "No matching borrow record exists for the current user."));
            if (record.status() == BorrowStatus.RETURNED) {
                throw failure(ErrorCodes.LIBRARY_ALREADY_RETURNED,
                        "This borrow record has already been returned.");
            }
            if (bookRepository.findById(record.bookId()).isEmpty()) {
                throw failure(ErrorCodes.LIBRARY_BOOK_NOT_FOUND, "The book does not exist.");
            }
            BorrowRecord returned = record.returnedAt(
                    LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
            // InMemory V1 compensation, not a JDBC transaction. All reads/writes share this lock.
            bookRepository.incrementAvailableCount(record.bookId());
            try {
                borrowRecordRepository.update(returned);
            } catch (RuntimeException exception) {
                if (!bookRepository.decrementAvailableCount(record.bookId())) {
                    exception.addSuppressed(new IllegalStateException("Cannot compensate return stock."));
                }
                throw exception;
            }
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private LibraryBusinessException failure(String code, String message) {
        return new LibraryBusinessException(code, message);
    }
}
