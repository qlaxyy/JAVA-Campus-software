package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.AddBookCopyRequest;
import edu.seu.vcampus.common.library.AdminBorrowQueryRequest;
import edu.seu.vcampus.common.library.AdminBorrowRecordDTO;
import edu.seu.vcampus.common.library.BookCopyIdRequest;
import edu.seu.vcampus.common.library.BorrowRecordIdRequest;
import edu.seu.vcampus.common.library.CopyBorrowRequest;
import edu.seu.vcampus.common.library.CopyInspectionDTO;
import edu.seu.vcampus.common.library.CopyInspectionRequest;
import edu.seu.vcampus.common.library.CreateReservationRequest;
import edu.seu.vcampus.common.library.ListBookCopiesRequest;
import edu.seu.vcampus.common.library.ReservationDTO;
import edu.seu.vcampus.common.library.SetBookStatusRequest;
import edu.seu.vcampus.common.library.UpdateBookCopyRequest;
import edu.seu.vcampus.common.library.UpdateBookRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 时刻边界、数量上限边界与低频错误分支。
 *
 * <p>时刻语义在实现中有意不同，这里用测试固定下来：
 * 逾期判定用严格比较（到期时刻本身<b>不算</b>逾期），预约过期用非严格比较
 * （截止时刻本身<b>算</b>过期）。两者都容易在重构时被无意改掉。
 */
class LibraryBoundaryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 9, 8, 0);
    private static final SessionInfo ADMIN = new SessionInfo("admin", "A-1", "libraryadmin", "管理员",
            Role.USER, Set.of(AdminScope.LIBRARY));

    private final MutableClock clock =
            new MutableClock(Instant.parse("2026-09-09T08:00:00Z"), ZoneOffset.UTC);
    private final InMemoryBookRepository books = new InMemoryBookRepository();
    private final InMemoryBorrowRecordRepository records = new InMemoryBorrowRecordRepository();
    private final InMemoryBookCopyRepository copies =
            InMemoryBookCopyRepository.seededFrom(books.searchAll(""));
    private final InMemoryReservationRepository reservations = new InMemoryReservationRepository();
    private final AtomicInteger sequence = new AtomicInteger();
    private final LibraryService service = new LibraryService(
            books, records, clock, () -> "BR-" + sequence.incrementAndGet(),
            new InMemoryBookCategoryRepository(), copies, reservations,
            LibraryTransactionManager.passthrough(),
            () -> "RS-%03d".formatted(sequence.incrementAndGet()));

    @Test
    void loanBecomesOverdueOnlyAfterItsDueMoment() {
        BookCopy copy = copies.findById("CP-B001-001").orElseThrow();
        copies.update(copy.withStatus(BookCopyStatus.LOANED));
        records.save(new BorrowRecord("BR-EDGE", "U-1", copy.copyId(),
                NOW.minusDays(30), NOW, BorrowStatus.BORROWED));

        assertEquals(List.of(), overdueRecordIds());
        assertFalse(service.getBorrowRecords("U-1").getFirst().isOverdue());

        clock.advance(Duration.ofSeconds(1));
        assertEquals(List.of("BR-EDGE"), overdueRecordIds());
        assertTrue(service.getBorrowRecords("U-1").getFirst().isOverdue());
    }

    @Test
    void reservationExpiresExactlyAtItsDeadline() {
        ReservationDTO created = service.createReservation("U-1",
                new CreateReservationRequest("B001", locationOf("B001")));
        assertEquals("READY_FOR_PICKUP", created.getStatus());
        assertEquals(NOW.plusHours(24), created.getExpiresAt());

        clock.advance(Duration.ofHours(23).plusMinutes(59).plusSeconds(59));
        assertEquals("READY_FOR_PICKUP", reload(created).getStatus());

        clock.advance(Duration.ofSeconds(1));
        assertEquals("EXPIRED", reload(created).getStatus());
    }

    @Test
    void fifthActiveLoanIsAcceptedAndSixthIsRejected() {
        for (String bookId : List.of("B001", "B002", "B003", "B004", "B005")) {
            service.borrowCopy("U-1", new CopyBorrowRequest(availableCopy(bookId).barcode()));
        }
        assertEquals(5, service.getBorrowRecords("U-1").size());

        // B001 还有其它在架单册，第六次借阅应先撞上限而不是重复借阅
        failure(ErrorCodes.LIBRARY_BORROW_LIMIT_REACHED,
                () -> service.borrowCopy("U-1",
                        new CopyBorrowRequest(availableCopy("B001").barcode())));
    }

    @Test
    void thirdActiveReservationIsAcceptedAndFourthIsRejected() {
        for (String bookId : List.of("B001", "B002", "B003")) {
            assertEquals("READY_FOR_PICKUP", service.createReservation("U-1",
                    new CreateReservationRequest(bookId, locationOf(bookId))).getStatus());
        }
        failure(ErrorCodes.LIBRARY_RESERVATION_LIMIT_REACHED, () -> service.createReservation("U-1",
                new CreateReservationRequest("B004", locationOf("B004"))));
    }

    @Test
    void borrowingAWithdrawnCopyExplainsThatItIsWithdrawn() {
        BookCopy copy = availableCopy("B001");
        service.withdrawBookCopy(ADMIN, new BookCopyIdRequest(copy.copyId()));

        LibraryBusinessException error = assertThrows(LibraryBusinessException.class,
                () -> service.borrowCopy("U-1", new CopyBorrowRequest(copy.barcode())));
        assertEquals(ErrorCodes.LIBRARY_COPY_NOT_AVAILABLE, error.code());
        assertTrue(error.getMessage().contains("注销"), error.getMessage());
    }

    @Test
    void reservedCopyWithoutMatchingReservationReportsInconsistentState() {
        BookCopy copy = availableCopy("B003");
        copies.update(copy.withStatus(BookCopyStatus.RESERVED));

        LibraryBusinessException error = assertThrows(LibraryBusinessException.class,
                () -> service.borrowCopy("U-1", new CopyBorrowRequest(copy.barcode())));
        assertEquals(ErrorCodes.LIBRARY_COPY_RESERVED_FOR_OTHER, error.code());
        assertTrue(error.getMessage().contains("异常"), error.getMessage());
    }

    @Test
    void inspectionFlagsACopyWhoseStateContradictsItsBorrowRecord() {
        BookCopy copy = availableCopy("B001");
        copies.update(copy.withStatus(BookCopyStatus.LOANED));

        CopyInspectionDTO inspection = service.inspectCopy("U-1",
                new CopyInspectionRequest(copy.barcode()));
        assertFalse(inspection.isBorrowAllowed());
        assertFalse(inspection.isReturnAllowed());
        assertTrue(inspection.getStatusMessage().contains("不一致"),
                inspection.getStatusMessage());
    }

    @Test
    void renewingAReturnedLoanIsRejected() {
        records.save(new BorrowRecord("BR-RETURNED", "U-1", "CP-B001-001",
                NOW.minusDays(40), NOW.minusDays(10), BorrowStatus.BORROWED)
                .returnedAt(NOW.minusDays(20)));

        failure(ErrorCodes.LIBRARY_RENEWAL_NOT_ALLOWED,
                () -> service.renewBorrow("U-1", new BorrowRecordIdRequest("BR-RETURNED")));
    }

    @Test
    void shelvingACopyThatStillHasAnActiveLoanIsRejected() {
        BookCopy copy = copies.findById("CP-B001-001").orElseThrow();
        copies.update(copy.withStatus(BookCopyStatus.WAITING_SHELVING));
        records.save(new BorrowRecord("BR-ACTIVE", "U-1", copy.copyId(),
                NOW.minusDays(1), NOW.plusDays(29), BorrowStatus.BORROWED));

        failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                () -> service.shelveBookCopy(ADMIN, new BookCopyIdRequest(copy.copyId())));
    }

    @Test
    void restoringAWithdrawnCopyThatStillHasAnActiveLoanIsRejected() {
        BookCopy copy = copies.findById("CP-B002-001").orElseThrow();
        copies.update(copy.withStatus(BookCopyStatus.WITHDRAWN));
        records.save(new BorrowRecord("BR-ACTIVE", "U-1", copy.copyId(),
                NOW.minusDays(1), NOW.plusDays(29), BorrowStatus.BORROWED));

        failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                () -> service.restoreBookCopy(ADMIN, new BookCopyIdRequest(copy.copyId())));
    }

    @Test
    void unknownIdentifiersAreReportedAsNotFound() {
        failure(ErrorCodes.LIBRARY_BOOK_NOT_FOUND, () -> service.setBookStatus(ADMIN,
                new SetBookStatusRequest("B-MISSING", "INACTIVE")));
        failure(ErrorCodes.LIBRARY_BOOK_NOT_FOUND, () -> service.updateBook(ADMIN,
                new UpdateBookRequest("B-MISSING", "9787111000000", "书名", "作者",
                        "C001", "出版社", 2026, "中文", 5_000)));
        failure(ErrorCodes.LIBRARY_BOOK_NOT_FOUND, () -> service.addBookCopy(ADMIN,
                new AddBookCopyRequest("B-MISSING", "BC-NEW", "九龙湖", "索书号")));
        failure(ErrorCodes.LIBRARY_BOOK_NOT_FOUND, () -> service.listBookCopies(ADMIN,
                new ListBookCopiesRequest("B-MISSING")));

        failure(ErrorCodes.LIBRARY_COPY_NOT_FOUND, () -> service.updateBookCopy(ADMIN,
                new UpdateBookCopyRequest("CP-MISSING", "九龙湖", "索书号")));
        failure(ErrorCodes.LIBRARY_COPY_NOT_FOUND, () -> service.withdrawBookCopy(ADMIN,
                new BookCopyIdRequest("CP-MISSING")));
        failure(ErrorCodes.LIBRARY_COPY_NOT_FOUND, () -> service.restoreBookCopy(ADMIN,
                new BookCopyIdRequest("CP-MISSING")));
        failure(ErrorCodes.LIBRARY_COPY_NOT_FOUND, () -> service.shelveBookCopy(ADMIN,
                new BookCopyIdRequest("CP-MISSING")));
    }

    /** 取该书目当前在架的第一份单册；种子数据里每个书目都有可借单册。 */
    private BookCopy availableCopy(String bookId) {
        return copies.findByBookId(bookId).stream()
                .filter(copy -> copy.status() == BookCopyStatus.AVAILABLE)
                .findFirst().orElseThrow();
    }

    /** 种子数据把每个书目的前一半单册放在第一个馆藏地，这里按实际数据取，避免硬编码馆藏地名。 */
    private String locationOf(String bookId) {
        return copies.findByBookId(bookId).getFirst().location();
    }

    private List<String> overdueRecordIds() {
        return service.queryBorrows(ADMIN,
                        new AdminBorrowQueryRequest(AdminBorrowQueryRequest.OVERDUE))
                .stream().map(AdminBorrowRecordDTO::getRecordId).toList();
    }

    /** 重新读取预约；查询会顺带清理到期预约，因此能观察到状态变化。 */
    private ReservationDTO reload(ReservationDTO reservation) {
        return service.getMyReservations("U-1").stream()
                .filter(value -> value.getReservationId().equals(reservation.getReservationId()))
                .findFirst().orElseThrow();
    }

    private static void failure(String code, Runnable action) {
        assertEquals(code, assertThrows(LibraryBusinessException.class, action::run).code());
    }
}
