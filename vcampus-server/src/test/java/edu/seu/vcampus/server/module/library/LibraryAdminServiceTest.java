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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class LibraryAdminServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-06T08:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 6, 8, 0);
    private static final String JIULONGHU = "九龙湖校区—中文图书阅览室3";
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
    void administratorCanExtendReusableCategoryDictionary() {
        assertEquals(3, service.listCategories().size());
        BookCategoryDTO added = service.addCategory(
                ADMIN, new AddBookCategoryRequest("  艺术设计  "));
        assertEquals("艺术设计", added.getCategoryName());
        assertTrue(service.listCategories().stream()
                .anyMatch(category -> category.getCategoryId().equals(added.getCategoryId())));
        failure(ErrorCodes.LIBRARY_DUPLICATE_CATEGORY,
                () -> service.addCategory(ADMIN,
                        new AddBookCategoryRequest("艺术设计")));
        failure(ErrorCodes.AUTH_FORBIDDEN,
                () -> service.addCategory(READER,
                        new AddBookCategoryRequest("医学")));
    }

    @Test
    void catalogMetadataAndCopyLifecycleStaySeparate() {
        AddBookRequest request = new AddBookRequest("978-7-111-00000-0", "新书", "作者", "C001",
                "出版社", 2026, "中文", 5_000);
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
        assertEquals(5_000, created.getPriceFen());
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
        failure(ErrorCodes.AUTH_FORBIDDEN,
                () -> service.restoreBookCopy(READER, new BookCopyIdRequest(copy.getCopyId())));

        BookCopyDTO restored = service.restoreBookCopy(ADMIN, new BookCopyIdRequest(copy.getCopyId()));
        assertEquals("AVAILABLE", restored.getStatus());
        assertEquals(1, service.searchBooksForAdmin(ADMIN,
                new BookSearchRequest(created.getIsbn(), null)).getBooks().getFirst().getTotalCount());
        failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                () -> service.restoreBookCopy(ADMIN, new BookCopyIdRequest(copy.getCopyId())));
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
                "新书名", inactive.getAuthor(), "C002", "新出版社", 2025, "中文", 7_200));
        assertEquals(7_200, edited.getPriceFen());
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

    @Test
    void administratorCanNarrowBorrowsToASingleReader() {
        BookCopy copy = copies.findById("CP-B001-001").orElseThrow();
        copies.update(copy.withStatus(BookCopyStatus.LOANED));
        records.save(new BorrowRecord("R-1", "U-1", copy.copyId(),
                NOW.minusDays(5), NOW.plusDays(25), BorrowStatus.BORROWED));
        records.save(new BorrowRecord("R-2", "U-2", "CP-B002-001",
                NOW.minusDays(5), NOW.plusDays(25), BorrowStatus.BORROWED));

        assertEquals(List.of("R-1", "R-2"), currentRecordIds(null), "留空表示查看全部读者");
        assertEquals(List.of("R-1"), currentRecordIds("U-1"));
        assertEquals(List.of("R-2"), currentRecordIds("U-2"));
        assertEquals(List.of(), currentRecordIds("U-NOBODY"), "不存在的读者返回空列表");
    }

    @Test
    void administratorCanSeeTheReservationQueueAcrossReaders() {
        // B003 在九龙湖只有两册，先让两位读者借走，第三位读者预约就只能排队
        service.borrowCopy("U-1", new CopyBorrowRequest("SEU-B003-001"));
        service.borrowCopy("U-2", new CopyBorrowRequest("SEU-B003-002"));
        ReservationDTO queued = service.createReservation("U-3",
                new CreateReservationRequest("B003", JIULONGHU));
        assertEquals("WAITING", queued.getStatus());

        // B001 在九龙湖还有可借册，预约立即保留
        ReservationDTO held = service.createReservation("U-4",
                new CreateReservationRequest("B001", JIULONGHU));
        assertEquals("READY_FOR_PICKUP", held.getStatus());

        List<String> waiting = reservationIdsFor(AdminReservationQueryRequest.WAITING);
        List<String> ready = reservationIdsFor(AdminReservationQueryRequest.READY);
        List<String> all = reservationIdsFor(AdminReservationQueryRequest.ALL);

        assertEquals(1, waiting.size());
        assertEquals(1, ready.size());
        assertEquals(2, all.size(), "全部范围应同时包含排队中与待取书");
        assertTrue(all.containsAll(waiting) && all.containsAll(ready));

        List<AdminReservationDTO> adminView = service.queryReservations(
                ADMIN, new AdminReservationQueryRequest(AdminReservationQueryRequest.ALL));
        assertEquals(Set.of("U-3", "U-4"),
                adminView.stream().map(AdminReservationDTO::getUserId).collect(Collectors.toSet()),
                "管理员视图必须带上预约属于哪位读者");

        failure(ErrorCodes.AUTH_FORBIDDEN, () -> service.queryReservations(READER,
                new AdminReservationQueryRequest(AdminReservationQueryRequest.ALL)));
        assertThrows(IllegalArgumentException.class, () -> service.queryReservations(
                ADMIN, new AdminReservationQueryRequest("UPCOMING")));
    }

    private List<String> currentRecordIds(String userId) {
        return service.queryBorrows(ADMIN,
                        new AdminBorrowQueryRequest(AdminBorrowQueryRequest.CURRENT, userId))
                .stream().map(AdminBorrowRecordDTO::getRecordId).toList();
    }

    private List<String> reservationIdsFor(String scope) {
        return service.queryReservations(ADMIN, new AdminReservationQueryRequest(scope))
                .stream().map(AdminReservationDTO::getReservationId).toList();
    }

    private static void failure(String code, Runnable action) {
        assertEquals(code, assertThrows(LibraryBusinessException.class, action::run).code());
    }
}
