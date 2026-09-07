package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.AddBookCopyRequest;
import edu.seu.vcampus.common.library.AddBookRequest;
import edu.seu.vcampus.common.library.AdminBorrowQueryRequest;
import edu.seu.vcampus.common.library.AdminBorrowRecordDTO;
import edu.seu.vcampus.common.library.BookCategoryDTO;
import edu.seu.vcampus.common.library.BookCopyDTO;
import edu.seu.vcampus.common.library.BookCopyIdRequest;
import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.BookLocationDTO;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.library.BorrowRecordDTO;
import edu.seu.vcampus.common.library.CopyBorrowRequest;
import edu.seu.vcampus.common.library.CopyReturnRequest;
import edu.seu.vcampus.common.library.ListBookCopiesRequest;
import edu.seu.vcampus.common.library.SetBookStatusRequest;
import edu.seu.vcampus.common.library.UpdateBookCopyRequest;
import edu.seu.vcampus.common.library.UpdateBookRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.user.SessionInfo;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private final BookCategoryRepository categoryRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LibraryTransactionManager transactionManager;

    LibraryService(
            BookRepository bookRepository,
            BorrowRecordRepository borrowRecordRepository,
            BookCopyRepository bookCopyRepository) {
        this(bookRepository, borrowRecordRepository, Clock.systemDefaultZone(),
                () -> UUID.randomUUID().toString(),
                new InMemoryBookCategoryRepository(), bookCopyRepository,
                LibraryTransactionManager.passthrough());
    }

    LibraryService(BookRepository bookRepository, BorrowRecordRepository borrowRecordRepository,
            Clock clock, Supplier<String> recordIdSupplier, BookCopyRepository bookCopyRepository) {
        this(bookRepository, borrowRecordRepository, clock, recordIdSupplier,
                new InMemoryBookCategoryRepository(), bookCopyRepository,
                LibraryTransactionManager.passthrough());
    }

    LibraryService(BookRepository bookRepository, BorrowRecordRepository borrowRecordRepository,
            Clock clock, Supplier<String> recordIdSupplier, BookCategoryRepository categoryRepository,
            BookCopyRepository bookCopyRepository) {
        this(bookRepository, borrowRecordRepository, clock, recordIdSupplier,
                categoryRepository, bookCopyRepository,
                LibraryTransactionManager.passthrough());
    }

    LibraryService(BookRepository bookRepository, BorrowRecordRepository borrowRecordRepository,
            Clock clock, Supplier<String> recordIdSupplier, BookCategoryRepository categoryRepository,
            BookCopyRepository bookCopyRepository, LibraryTransactionManager transactionManager) {
        this.bookRepository = Objects.requireNonNull(
                bookRepository, "bookRepository must not be null");
        this.borrowRecordRepository = Objects.requireNonNull(
                borrowRecordRepository, "borrowRecordRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.recordIdSupplier = Objects.requireNonNull(
                recordIdSupplier, "recordIdSupplier must not be null");
        this.categoryRepository = Objects.requireNonNull(categoryRepository);
        this.bookCopyRepository = Objects.requireNonNull(
                bookCopyRepository, "bookCopyRepository must not be null");
        this.transactionManager = Objects.requireNonNull(
                transactionManager, "transactionManager must not be null");
    }

    BookSearchResult searchBooks(BookSearchRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String keyword = request.getKeyword() == null ? "" : request.getKeyword().strip();
        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("搜索关键词不能超过 50 个字符");
        }
        String categoryId = request.getCategoryId();
        synchronized (circulationLock) {
            if (categoryId != null) { categoryId = requireCategory(categoryId).getCategoryId(); }
            String filter = categoryId;
            return new BookSearchResult(bookRepository.search(keyword).stream()
                    .filter(book -> filter == null || book.getCategoryId().equals(filter))
                    .map(this::withInventorySummary)
                    .toList());
        }
    }

    BookSearchResult searchBooksForAdmin(SessionInfo actor, BookSearchRequest request) {
        requireAdministrator(actor);
        Objects.requireNonNull(request, "request must not be null");
        String keyword = request.getKeyword() == null ? "" : request.getKeyword().strip();
        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("搜索关键词不能超过 50 个字符");
        }
        synchronized (circulationLock) {
            String categoryId = request.getCategoryId();
            if (categoryId != null) { categoryId = requireCategory(categoryId).getCategoryId(); }
            String filter = categoryId;
            return new BookSearchResult(bookRepository.searchAll(keyword).stream()
                    .filter(book -> filter == null || book.getCategoryId().equals(filter))
                    .map(this::withInventorySummary)
                    .toList());
        }
    }

    List<BookCategoryDTO> listCategories() { return categoryRepository.findAll(); }

    BookDTO addBook(SessionInfo actor, AddBookRequest request) {
        requireAdministrator(actor);
        Objects.requireNonNull(request);
        synchronized (circulationLock) {
            String isbn = validatedIsbn(request.getIsbn());
            requireUniqueIsbn(isbn, null);
            BookCategoryDTO category = requireCategory(request.getCategoryId());
            String title = boundedText(request.getTitle(), "书名", 200);
            String author = boundedText(request.getAuthor(), "作者", 100);
            String publisher = optionalText(request.getPublisher(), "出版社", 100);
            Integer publicationYear = publicationYear(request.getPublicationYear());
            String language = optionalText(request.getLanguage(), "语种", 30);
            String id;
            do { id = "B" + UUID.randomUUID().toString().replace("-", "").substring(0, 19); }
            while (bookRepository.findIncludingInactive(id).isPresent());
            BookDTO book = new BookDTO(id, isbn, title, author,
                    category.getCategoryId(), category.getCategoryName(),
                    publisher, publicationYear, language, "ACTIVE", List.of());
            bookRepository.insert(book);
            return withInventorySummary(book);
        }
    }

    BookDTO updateBook(SessionInfo actor, UpdateBookRequest request) {
        requireAdministrator(actor);
        Objects.requireNonNull(request);
        synchronized (circulationLock) {
            BookDTO original = requireAnyBook(request.getBookId());
            String isbn = validatedIsbn(request.getIsbn());
            requireUniqueIsbn(isbn, original.getBookId());
            BookCategoryDTO category = requireCategory(request.getCategoryId());
            BookDTO updated = new BookDTO(original.getBookId(), isbn,
                    boundedText(request.getTitle(), "书名", 200),
                    boundedText(request.getAuthor(), "作者", 100),
                    category.getCategoryId(), category.getCategoryName(),
                    optionalText(request.getPublisher(), "出版社", 100),
                    publicationYear(request.getPublicationYear()),
                    optionalText(request.getLanguage(), "语种", 30),
                    original.getStatus(), original.getLocations());
            bookRepository.update(updated);
            return withInventorySummary(updated);
        }
    }

    BookDTO setBookStatus(SessionInfo actor, SetBookStatusRequest request) {
        requireAdministrator(actor);
        Objects.requireNonNull(request, "request must not be null");
        synchronized (circulationLock) {
            BookDTO original = requireAnyBook(request.getBookId());
            String status = boundedText(request.getStatus(), "书目状态", 20)
                    .toUpperCase(java.util.Locale.ROOT);
            if (!("ACTIVE".equals(status) || "INACTIVE".equals(status))) {
                throw failure(ErrorCodes.LIBRARY_INVALID_BOOK_STATUS,
                        "书目状态只能是 ACTIVE 或 INACTIVE");
            }
            BookDTO updated = copyBook(original, status);
            bookRepository.update(updated);
            return withInventorySummary(updated);
        }
    }

    BookCopyDTO addBookCopy(SessionInfo actor, AddBookCopyRequest request) {
        requireAdministrator(actor);
        Objects.requireNonNull(request, "request must not be null");
        synchronized (circulationLock) {
            BookDTO book = requireAnyBook(request.getBookId());
            String barcode = boundedText(request.getBarcode(), "馆藏条码", 50);
            if (bookCopyRepository.findByBarcode(barcode).isPresent()) {
                throw failure(ErrorCodes.LIBRARY_DUPLICATE_BARCODE, "馆藏条码已存在");
            }
            String copyId;
            do {
                copyId = "CP-" + UUID.randomUUID().toString().replace("-", "");
            } while (bookCopyRepository.findById(copyId).isPresent());
            BookCopy copy = new BookCopy(copyId, barcode, book.getBookId(),
                    boundedText(request.getLocation(), "馆藏地", 100),
                    boundedText(request.getCallNumber(), "索书号", 100),
                    BookCopyStatus.AVAILABLE);
            bookCopyRepository.insert(copy);
            return toBookCopyDTO(copy);
        }
    }

    List<BookCopyDTO> listBookCopies(SessionInfo actor, ListBookCopiesRequest request) {
        requireAdministrator(actor);
        Objects.requireNonNull(request, "request must not be null");
        synchronized (circulationLock) {
            String bookId = requireAnyBook(request.getBookId()).getBookId();
            return bookCopyRepository.findByBookId(bookId).stream()
                    .sorted(Comparator.comparing(BookCopy::barcode))
                    .map(this::toBookCopyDTO)
                    .toList();
        }
    }

    BookCopyDTO updateBookCopy(SessionInfo actor, UpdateBookCopyRequest request) {
        requireAdministrator(actor);
        Objects.requireNonNull(request, "request must not be null");
        synchronized (circulationLock) {
            BookCopy original = requireCopy(request.getCopyId());
            if (original.status() == BookCopyStatus.WITHDRAWN) {
                throw failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                        "已注销单册不能修改");
            }
            BookCopy updated = original.withLocation(
                    boundedText(request.getLocation(), "馆藏地", 100),
                    boundedText(request.getCallNumber(), "索书号", 100));
            bookCopyRepository.update(updated);
            return toBookCopyDTO(updated);
        }
    }

    BookCopyDTO withdrawBookCopy(SessionInfo actor, BookCopyIdRequest request) {
        requireAdministrator(actor);
        Objects.requireNonNull(request, "request must not be null");
        synchronized (circulationLock) {
            BookCopy copy = requireCopy(request.getCopyId());
            if (copy.status() == BookCopyStatus.LOANED
                    || borrowRecordRepository.findBorrowedByCopyId(copy.copyId()).isPresent()) {
                throw failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                        "借出中的单册不能注销");
            }
            if (copy.status() == BookCopyStatus.WITHDRAWN) {
                throw failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS, "该单册已经注销");
            }
            BookCopy withdrawn = copy.withStatus(BookCopyStatus.WITHDRAWN);
            bookCopyRepository.update(withdrawn);
            return toBookCopyDTO(withdrawn);
        }
    }

    BookCopyDTO restoreBookCopy(SessionInfo actor, BookCopyIdRequest request) {
        requireAdministrator(actor);
        Objects.requireNonNull(request, "request must not be null");
        synchronized (circulationLock) {
            BookCopy copy = requireCopy(request.getCopyId());
            if (copy.status() != BookCopyStatus.WITHDRAWN) {
                throw failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                        "只有已注销单册可以恢复");
            }
            if (borrowRecordRepository.findBorrowedByCopyId(copy.copyId()).isPresent()) {
                throw failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                        "存在未结束借阅记录的单册不能恢复");
            }
            BookCopy restored = copy.withStatus(BookCopyStatus.AVAILABLE);
            bookCopyRepository.update(restored);
            return toBookCopyDTO(restored);
        }
    }

    private void requireAdministrator(SessionInfo actor) {
        if (actor == null || !actor.canAdminister(ModuleNames.LIBRARY)) {
            throw failure(ErrorCodes.AUTH_FORBIDDEN, "需要图书馆管理权限");
        }
    }

    private BookDTO requireAnyBook(String id) {
        return bookRepository.findIncludingInactive(boundedText(id, "图书编号", 20)).orElseThrow(() ->
                failure(ErrorCodes.LIBRARY_BOOK_NOT_FOUND, "图书不存在，请刷新列表"));
    }

    private BookCopy requireCopy(String id) {
        return bookCopyRepository.findById(boundedText(id, "单册编号", 50)).orElseThrow(() ->
                failure(ErrorCodes.LIBRARY_COPY_NOT_FOUND, "实体单册不存在，请刷新列表"));
    }

    private BookCategoryDTO requireCategory(String id) {
        return categoryRepository.findById(boundedText(id, "分类", 20)).orElseThrow(() ->
                failure(ErrorCodes.LIBRARY_CATEGORY_NOT_FOUND, "分类不存在，请刷新分类列表"));
    }

    private String validatedIsbn(String value) {
        String isbn = boundedText(value, "ISBN", 40).replaceAll("[\\s-]", "")
                .toUpperCase(java.util.Locale.ROOT);
        if (!isbn.matches("(?:[0-9]{13}|[0-9]{9}[0-9X])")) {
            throw new IllegalArgumentException("ISBN 应为 10 位或 13 位，可包含空格和连字符");
        }
        return isbn;
    }

    private String boundedText(String value, String label, int limit) {
        if (value == null || value.isBlank() || value.strip().length() > limit) {
            throw new IllegalArgumentException(label + "须为 1 至 " + limit + " 个字符");
        }
        return value.strip();
    }

    private String optionalText(String value, String label, int limit) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.strip().length() > limit) {
            throw new IllegalArgumentException(label + "不能超过 " + limit + " 个字符");
        }
        return value.strip();
    }

    private Integer publicationYear(Integer value) {
        if (value != null && (value < 1000 || value > 9999)) {
            throw new IllegalArgumentException("出版年须为 1000 至 9999，或留空");
        }
        return value;
    }

    private BookDTO copyBook(BookDTO book, String status) {
        return new BookDTO(book.getBookId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getCategoryId(), book.getCategoryName(), book.getPublisher(),
                book.getPublicationYear(), book.getLanguage(), status, book.getLocations());
    }

    private void requireUniqueIsbn(String isbn, String currentId) {
        if (bookRepository.findByIsbn(isbn)
                .filter(book -> !book.getBookId().equals(currentId)).isPresent()) {
            throw failure(ErrorCodes.LIBRARY_DUPLICATE_ISBN,
                    "该 ISBN 已存在（包含已停用书目），不能重复新增");
        }
    }

    void borrowCopy(String userId, CopyBorrowRequest request) {
        String validatedUserId = requireText(userId, "userId");
        Objects.requireNonNull(request, "request must not be null");
        String barcode = requireText(request.getBarcode(), "barcode");
        synchronized (circulationLock) {
            transactionManager.execute(() -> borrowCopyAtomically(validatedUserId, barcode));
        }
    }

    private void borrowCopyAtomically(String userId, String barcode) {
        BookCopy copy = bookCopyRepository.findByBarcode(barcode)
                .orElseThrow(() -> failure(ErrorCodes.LIBRARY_COPY_NOT_FOUND,
                        "The scanned copy barcode does not exist."));
        BookDTO book = bookRepository.findIncludingInactive(copy.bookId())
                .orElseThrow(() -> failure(ErrorCodes.LIBRARY_BOOK_NOT_FOUND,
                        "The catalog record for this copy does not exist."));
        if (!"ACTIVE".equals(book.getStatus())) {
            throw failure(ErrorCodes.LIBRARY_COPY_NOT_AVAILABLE,
                    "This title is not open for borrowing.");
        }
        if (copy.status() != BookCopyStatus.AVAILABLE) {
            throw failure(ErrorCodes.LIBRARY_COPY_NOT_AVAILABLE,
                    "This physical copy is not available for borrowing.");
        }

        List<BorrowRecord> currentBorrows =
                borrowRecordRepository.findBorrowedByUserId(userId);
        LocalDateTime borrowTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        if (currentBorrows.stream().anyMatch(record -> record.isOverdueAt(borrowTime))) {
            throw failure(ErrorCodes.LIBRARY_OVERDUE_BORROW_EXISTS,
                    "Return overdue books before borrowing another book.");
        }
        if (currentBorrows.size() >= MAX_ACTIVE_BORROWS) {
            throw failure(ErrorCodes.LIBRARY_BORROW_LIMIT_REACHED,
                    "At most five books may be borrowed at the same time.");
        }
        if (currentBorrows.stream().map(this::requireCopyForRecord)
                .anyMatch(activeCopy -> activeCopy.bookId().equals(copy.bookId()))) {
            throw failure(ErrorCodes.LIBRARY_ALREADY_BORROWED,
                    "Another copy of this title is already borrowed and not returned.");
        }

        BorrowRecord record = new BorrowRecord(recordIdSupplier.get(), userId,
                copy.copyId(), borrowTime, borrowTime.plusDays(BORROW_DAYS),
                BorrowStatus.BORROWED);
        bookCopyRepository.update(copy.withStatus(BookCopyStatus.LOANED));
        try {
            borrowRecordRepository.save(record);
        } catch (RuntimeException exception) {
            bookCopyRepository.update(copy);
            throw exception;
        }
    }

    List<BorrowRecordDTO> getBorrowRecords(String userId) {
        String validatedUserId = requireText(userId, "userId");
        synchronized (circulationLock) {
            LocalDateTime now = LocalDateTime.now(clock);
            return borrowRecordRepository.findByUserId(validatedUserId).stream()
                    .sorted(Comparator.comparing(BorrowRecord::borrowTime).reversed()
                            .thenComparing(BorrowRecord::recordId))
                    .map(record -> toBorrowRecordDTO(record, now))
                    .toList();
        }
    }

    List<AdminBorrowRecordDTO> queryBorrows(
            SessionInfo actor, AdminBorrowQueryRequest request) {
        requireAdministrator(actor);
        Objects.requireNonNull(request, "request must not be null");
        String scope = boundedText(request.getScope(), "查询范围", 20)
                .toUpperCase(java.util.Locale.ROOT);
        if (!(AdminBorrowQueryRequest.CURRENT.equals(scope)
                || AdminBorrowQueryRequest.HISTORY.equals(scope)
                || AdminBorrowQueryRequest.OVERDUE.equals(scope))) {
            throw new IllegalArgumentException("查询范围只能是 CURRENT、HISTORY 或 OVERDUE");
        }
        synchronized (circulationLock) {
            LocalDateTime now = LocalDateTime.now(clock);
            return borrowRecordRepository.findAll().stream()
                    .filter(record -> switch (scope) {
                        case AdminBorrowQueryRequest.CURRENT ->
                                record.status() == BorrowStatus.BORROWED;
                        case AdminBorrowQueryRequest.HISTORY ->
                                record.status() == BorrowStatus.RETURNED;
                        case AdminBorrowQueryRequest.OVERDUE -> record.isOverdueAt(now);
                        default -> false;
                    })
                    .sorted(Comparator.comparing(BorrowRecord::borrowTime).reversed()
                            .thenComparing(BorrowRecord::recordId))
                    .map(record -> toAdminBorrowRecordDTO(record, now))
                    .toList();
        }
    }

    void returnCopy(String userId, CopyReturnRequest request) {
        String validatedUserId = requireText(userId, "userId");
        Objects.requireNonNull(request, "request must not be null");
        String barcode = requireText(request.getBarcode(), "barcode");
        synchronized (circulationLock) {
            transactionManager.execute(() -> returnCopyAtomically(validatedUserId, barcode));
        }
    }

    private void returnCopyAtomically(String userId, String barcode) {
        BookCopy copy = bookCopyRepository.findByBarcode(barcode)
                .orElseThrow(() -> failure(ErrorCodes.LIBRARY_COPY_NOT_FOUND,
                        "The scanned copy barcode does not exist."));
        BorrowRecord record = borrowRecordRepository.findBorrowedByCopyId(copy.copyId())
                .filter(value -> value.userId().equals(userId))
                .orElseThrow(() -> failure(ErrorCodes.LIBRARY_BORROW_RECORD_NOT_FOUND,
                        "No active borrow exists for this copy and the current user."));
        if (copy.status() != BookCopyStatus.LOANED) {
            throw failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                    "The copy and borrow record are inconsistent.");
        }
        if (bookRepository.findIncludingInactive(copy.bookId()).isEmpty()) {
            throw failure(ErrorCodes.LIBRARY_BOOK_NOT_FOUND,
                    "The catalog record for this copy does not exist.");
        }

        BorrowRecord returned = record.returnedAt(
                LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        bookCopyRepository.update(copy.withStatus(BookCopyStatus.WAITING_SHELVING));
        try {
            borrowRecordRepository.update(returned);
        } catch (RuntimeException exception) {
            bookCopyRepository.update(copy);
            throw exception;
        }
    }

    BookCopyDTO shelveBookCopy(SessionInfo actor, BookCopyIdRequest request) {
        requireAdministrator(actor);
        Objects.requireNonNull(request, "request must not be null");
        String copyId = requireText(request.getCopyId(), "copyId");
        synchronized (circulationLock) {
            BookCopy copy = bookCopyRepository.findById(copyId)
                    .orElseThrow(() -> failure(ErrorCodes.LIBRARY_COPY_NOT_FOUND,
                            "The physical copy does not exist."));
            if (copy.status() != BookCopyStatus.WAITING_SHELVING) {
                throw failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                        "Only a returned copy waiting for shelving can be shelved.");
            }
            if (borrowRecordRepository.findBorrowedByCopyId(copyId).isPresent()) {
                throw failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                        "A copy with an active borrow record cannot be shelved.");
            }
            BookCopy shelved = copy.withStatus(BookCopyStatus.AVAILABLE);
            bookCopyRepository.update(shelved);
            return toBookCopyDTO(shelved);
        }
    }

    private BookDTO withInventorySummary(BookDTO book) {
        List<BookCopy> copies = bookCopyRepository.findByBookId(book.getBookId());
        Map<String, int[]> totals = new LinkedHashMap<>();
        boolean active = "ACTIVE".equals(book.getStatus());
        for (BookCopy copy : copies) {
            if (copy.status() == BookCopyStatus.WITHDRAWN) {
                continue;
            }
            int[] counts = totals.computeIfAbsent(copy.location(), ignored -> new int[2]);
            counts[0]++;
            if (active && copy.status() == BookCopyStatus.AVAILABLE) {
                counts[1]++;
            }
        }
        List<BookLocationDTO> locations = totals.entrySet().stream()
                .map(entry -> new BookLocationDTO(
                        entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
        return new BookDTO(book.getBookId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getCategoryId(), book.getCategoryName(), book.getPublisher(),
                book.getPublicationYear(), book.getLanguage(), book.getStatus(), locations);
    }

    private BorrowRecordDTO toBorrowRecordDTO(BorrowRecord record, LocalDateTime now) {
        BookCopy copy = requireCopyForRecord(record);
        String title = bookRepository.findIncludingInactive(copy.bookId())
                .map(BookDTO::getTitle)
                .orElse("图书信息不可用（" + copy.bookId() + "）");
        return new BorrowRecordDTO(record.recordId(), copy.bookId(), title,
                copy.copyId(), copy.barcode(), record.borrowTime(), record.dueTime(),
                record.returnTime(), record.status().name(), record.isOverdueAt(now));
    }

    private AdminBorrowRecordDTO toAdminBorrowRecordDTO(
            BorrowRecord record, LocalDateTime now) {
        BookCopy copy = requireCopyForRecord(record);
        String title = bookRepository.findIncludingInactive(copy.bookId())
                .map(BookDTO::getTitle)
                .orElse("图书信息不可用（" + copy.bookId() + "）");
        return new AdminBorrowRecordDTO(record.recordId(), record.userId(), copy.bookId(),
                title, copy.copyId(), copy.barcode(), record.borrowTime(), record.dueTime(),
                record.returnTime(), record.status().name(), record.isOverdueAt(now));
    }

    private BookCopy requireCopyForRecord(BorrowRecord record) {
        return bookCopyRepository.findById(record.copyId()).orElseThrow(() ->
                new IllegalStateException("Borrow record references a missing physical copy."));
    }

    private BookCopyDTO toBookCopyDTO(BookCopy copy) {
        return new BookCopyDTO(copy.copyId(), copy.barcode(), copy.bookId(), copy.location(),
                copy.callNumber(), copy.status().name());
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
