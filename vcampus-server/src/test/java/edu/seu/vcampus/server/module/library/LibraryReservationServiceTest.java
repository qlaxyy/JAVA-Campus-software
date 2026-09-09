package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.AddBookCopyRequest;
import edu.seu.vcampus.common.library.BookCopyIdRequest;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.CopyBorrowRequest;
import edu.seu.vcampus.common.library.CopyReturnRequest;
import edu.seu.vcampus.common.library.CreateReservationRequest;
import edu.seu.vcampus.common.library.ReservationDTO;
import edu.seu.vcampus.common.library.ReservationIdRequest;
import edu.seu.vcampus.common.library.SetBookStatusRequest;
import edu.seu.vcampus.common.library.UpdateBookCopyRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryReservationServiceTest {

    private static final String JIULONGHU = "九龙湖校区—中文图书阅览室3";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 9, 8, 0);
    private static final SessionInfo ADMIN = new SessionInfo(
            "admin-token", "A-001", "libraryadmin", "图书管理员", Role.USER,
            Set.of(AdminScope.LIBRARY));

    private final MutableClock clock = new MutableClock(
            Instant.parse("2026-09-09T08:00:00Z"), ZoneOffset.UTC);
    private final InMemoryBookRepository books = new InMemoryBookRepository();
    private final InMemoryBorrowRecordRepository records = new InMemoryBorrowRecordRepository();
    private final InMemoryBookCopyRepository copies =
            InMemoryBookCopyRepository.seededFrom(books.searchAll(""));
    private final InMemoryReservationRepository reservations =
            new InMemoryReservationRepository();
    private final AtomicInteger recordSequence = new AtomicInteger();
    private final AtomicInteger reservationSequence = new AtomicInteger();
    private final LibraryService service = new LibraryService(
            books, records, clock, () -> "BR-" + recordSequence.incrementAndGet(),
            new InMemoryBookCategoryRepository(), copies, reservations,
            LibraryTransactionManager.passthrough(),
            () -> "RS-%03d".formatted(reservationSequence.incrementAndGet()));

    @Test
    void availableCopyIsImmediatelyHeldForTwentyFourHours() {
        ReservationDTO created = reserve("U-1", "B001", JIULONGHU);

        assertEquals("READY_FOR_PICKUP", created.getStatus());
        assertEquals("SEU-B001-001", created.getAssignedBarcode());
        assertEquals(NOW, created.getCreatedAt());
        assertEquals(NOW, created.getReadyAt());
        assertEquals(NOW.plusHours(24), created.getExpiresAt());
        assertNull(created.getQueuePosition());
        assertEquals(BookCopyStatus.RESERVED,
                copies.findByBarcode(created.getAssignedBarcode()).orElseThrow().status());
        assertEquals(5, inventory("B001")[0]);
        assertEquals(4, inventory("B001")[1]);
    }

    @Test
    void unavailableLocationQueuesInStableFifoAndCancellationAdvancesHead() {
        List<Loan> loans = loanAllAt("B001", JIULONGHU);
        ReservationDTO first = reserve("U-WAIT-1", "B001", JIULONGHU);
        ReservationDTO second = reserve("U-WAIT-2", "B001", JIULONGHU);
        assertEquals("WAITING", first.getStatus());
        assertEquals(1, first.getQueuePosition());
        assertEquals(2, second.getQueuePosition());

        Loan returned = loans.getFirst();
        service.returnCopy(returned.userId(), new CopyReturnRequest(returned.barcode()));
        service.shelveBookCopy(ADMIN, new BookCopyIdRequest(returned.copyId()));

        ReservationDTO ready = reservation("U-WAIT-1", first.getReservationId());
        assertEquals("READY_FOR_PICKUP", ready.getStatus());
        assertEquals(returned.barcode(), ready.getAssignedBarcode());
        assertEquals(1, reservation("U-WAIT-2", second.getReservationId())
                .getQueuePosition());

        service.cancelReservation("U-WAIT-1",
                new ReservationIdRequest(first.getReservationId()));
        ReservationDTO advanced = reservation("U-WAIT-2", second.getReservationId());
        assertEquals("READY_FOR_PICKUP", advanced.getStatus());
        assertEquals(returned.barcode(), advanced.getAssignedBarcode());
    }

    @Test
    void fifoUsesReservationIdAsStableTieBreakerForEqualCreationTimes() {
        ArrayDeque<String> ids = new ArrayDeque<>(List.of("RS-Z", "RS-A"));
        LibraryService tiedService = new LibraryService(
                books, records, clock, () -> "BR-" + recordSequence.incrementAndGet(),
                new InMemoryBookCategoryRepository(), copies, reservations,
                LibraryTransactionManager.passthrough(), ids::removeFirst);
        List<Loan> loans = copies.findByBookId("B001").stream()
                .filter(copy -> copy.location().equals(JIULONGHU))
                .map(copy -> {
                    String userId = "U-TIE-LOAN-" + copy.copyId();
                    tiedService.borrowCopy(userId, new CopyBorrowRequest(copy.barcode()));
                    return new Loan(userId, copy.copyId(), copy.barcode());
                }).toList();
        tiedService.createReservation("U-Z",
                new CreateReservationRequest("B001", JIULONGHU));
        tiedService.createReservation("U-A",
                new CreateReservationRequest("B001", JIULONGHU));

        assertEquals(1, tiedService.getMyReservations("U-A").getFirst().getQueuePosition());
        assertEquals(2, tiedService.getMyReservations("U-Z").getFirst().getQueuePosition());
        Loan returned = loans.getFirst();
        tiedService.returnCopy(returned.userId(), new CopyReturnRequest(returned.barcode()));
        tiedService.shelveBookCopy(ADMIN, new BookCopyIdRequest(returned.copyId()));

        assertEquals("READY_FOR_PICKUP",
                tiedService.getMyReservations("U-A").getFirst().getStatus());
        assertEquals("WAITING", tiedService.getMyReservations("U-Z").getFirst().getStatus());
    }

    @Test
    void reservationRulesRejectInvalidDuplicateLimitBorrowedAndOverdueRequests() {
        failure(ErrorCodes.LIBRARY_INVALID_PICKUP_LOCATION,
                () -> reserve("U-1", "B001", "不存在馆藏地"));

        reserve("U-1", "B001", JIULONGHU);
        failure(ErrorCodes.LIBRARY_DUPLICATE_RESERVATION,
                () -> reserve("U-1", "B001", JIULONGHU));
        reserve("U-1", "B002", JIULONGHU);
        reserve("U-1", "B003", JIULONGHU);
        failure(ErrorCodes.LIBRARY_RESERVATION_LIMIT_REACHED,
                () -> reserve("U-1", "B004", JIULONGHU));

        service.borrowCopy("U-BORROWED", new CopyBorrowRequest("SEU-B004-001"));
        failure(ErrorCodes.LIBRARY_ALREADY_BORROWED,
                () -> reserve("U-BORROWED", "B004", JIULONGHU));

        BookCopy overdueCopy = copies.findByBarcode("SEU-B005-001").orElseThrow();
        copies.update(overdueCopy.withStatus(BookCopyStatus.LOANED));
        records.save(new BorrowRecord("OVERDUE", "U-OVERDUE", overdueCopy.copyId(),
                NOW.minusDays(40), NOW.minusDays(10), BorrowStatus.BORROWED));
        failure(ErrorCodes.LIBRARY_OVERDUE_BORROW_EXISTS,
                () -> reserve("U-OVERDUE", "B004", JIULONGHU));
    }

    @Test
    void expiryReleasesCopyAndAppliesSevenDaySameTitleCooldown() {
        ReservationDTO created = reserve("U-1", "B001", JIULONGHU);
        clock.advance(Duration.ofHours(24));

        ReservationDTO expired = reservation("U-1", created.getReservationId());
        assertEquals("EXPIRED", expired.getStatus());
        assertEquals(NOW.plusHours(24), expired.getClosedAt());
        assertEquals(BookCopyStatus.AVAILABLE,
                copies.findByBarcode(created.getAssignedBarcode()).orElseThrow().status());
        failure(ErrorCodes.LIBRARY_RESERVATION_COOLDOWN,
                () -> reserve("U-1", "B001", JIULONGHU));

        clock.advance(Duration.ofDays(7));
        assertEquals("READY_FOR_PICKUP", reserve("U-1", "B001", JIULONGHU).getStatus());
    }

    @Test
    void delayedCleanupDoesNotExtendNoShowCooldown() {
        reserve("U-1", "B001", JIULONGHU);
        clock.advance(Duration.ofDays(8));

        ReservationDTO replacement = reserve("U-1", "B001", JIULONGHU);

        assertEquals("READY_FOR_PICKUP", replacement.getStatus());
        ReservationDTO expired = service.getMyReservations("U-1").stream()
                .filter(value -> "EXPIRED".equals(value.getStatus()))
                .findFirst().orElseThrow();
        assertEquals(NOW.plusHours(24), expired.getClosedAt());
    }

    @Test
    void activeCancellationDoesNotStartCooldown() {
        ReservationDTO created = reserve("U-1", "B001", JIULONGHU);
        ReservationDTO canceled = service.cancelReservation(
                "U-1", new ReservationIdRequest(created.getReservationId()));

        assertEquals("CANCELED", canceled.getStatus());
        assertEquals("READY_FOR_PICKUP", reserve("U-1", "B001", JIULONGHU).getStatus());
        failure(ErrorCodes.LIBRARY_RESERVATION_NOT_CANCELLABLE,
                () -> service.cancelReservation(
                        "U-1", new ReservationIdRequest(created.getReservationId())));
        failure(ErrorCodes.LIBRARY_RESERVATION_NOT_FOUND,
                () -> service.cancelReservation(
                        "U-OTHER", new ReservationIdRequest(created.getReservationId())));
    }

    @Test
    void onlyReservationOwnerCanBorrowHeldCopyAndBorrowFulfillsReservation() {
        ReservationDTO ready = reserve("U-OWNER", "B001", JIULONGHU);
        failure(ErrorCodes.LIBRARY_COPY_RESERVED_FOR_OTHER,
                () -> service.borrowCopy("U-OTHER",
                        new CopyBorrowRequest(ready.getAssignedBarcode())));

        service.borrowCopy("U-OWNER", new CopyBorrowRequest(ready.getAssignedBarcode()));

        assertEquals(BookCopyStatus.LOANED,
                copies.findByBarcode(ready.getAssignedBarcode()).orElseThrow().status());
        assertEquals("FULFILLED", reservation(
                "U-OWNER", ready.getReservationId()).getStatus());
        assertEquals(1, records.findBorrowedByUserId("U-OWNER").size());
    }

    @Test
    void borrowingAnotherAvailableCopyFulfillsAndReleasesExistingHold() {
        ReservationDTO ready = reserve("U-OWNER", "B001", JIULONGHU);
        loanAllAt("B001", JIULONGHU);
        ReservationDTO waiting = reserve("U-NEXT", "B001", JIULONGHU);
        String otherBarcode = copies.findByBookId("B001").stream()
                .filter(copy -> !copy.location().equals(JIULONGHU))
                .filter(copy -> copy.status() == BookCopyStatus.AVAILABLE)
                .map(BookCopy::barcode).findFirst().orElseThrow();

        service.borrowCopy("U-OWNER", new CopyBorrowRequest(otherBarcode));

        assertEquals("FULFILLED", reservation(
                "U-OWNER", ready.getReservationId()).getStatus());
        ReservationDTO advanced = reservation("U-NEXT", waiting.getReservationId());
        assertEquals("READY_FOR_PICKUP", advanced.getStatus());
        assertEquals(ready.getAssignedBarcode(), advanced.getAssignedBarcode());
    }

    @Test
    void inactiveTitleCancelsReservationsAndReservedCopiesCannotBeEditedOrWithdrawn() {
        ReservationDTO first = reserve("U-1", "B001", JIULONGHU);
        ReservationDTO second = reserve("U-2", "B001", JIULONGHU);
        BookCopy reservedCopy = copies.findByBarcode(first.getAssignedBarcode()).orElseThrow();
        failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                () -> service.updateBookCopy(ADMIN, new UpdateBookCopyRequest(
                        reservedCopy.copyId(), reservedCopy.location(), "NEW/CALL")));
        failure(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                () -> service.withdrawBookCopy(
                        ADMIN, new BookCopyIdRequest(reservedCopy.copyId())));

        service.setBookStatus(ADMIN, new SetBookStatusRequest("B001", "INACTIVE"));

        assertEquals("CANCELED", reservation("U-1", first.getReservationId()).getStatus());
        assertEquals("CANCELED", reservation("U-2", second.getReservationId()).getStatus());
        assertTrue(copies.findByBookId("B001").stream()
                .noneMatch(copy -> copy.status() == BookCopyStatus.RESERVED));
        assertEquals(0, service.searchBooksForAdmin(
                ADMIN, new BookSearchRequest("Java编程思想", null))
                .getBooks().getFirst().getAvailableCount());
    }

    @Test
    void addingOrRestoringCopyAllocatesItToLocationQueueHead() {
        loanAllAt("B001", JIULONGHU);
        ReservationDTO waitingForAdded = reserve("U-ADD", "B001", JIULONGHU);
        service.addBookCopy(ADMIN, new AddBookCopyRequest(
                "B001", "SEU-B001-NEW", JIULONGHU, "C001/B001"));
        assertEquals("SEU-B001-NEW", reservation(
                "U-ADD", waitingForAdded.getReservationId()).getAssignedBarcode());

        BookCopy restoreCandidate = copies.findByBarcode("SEU-B002-001").orElseThrow();
        service.withdrawBookCopy(ADMIN, new BookCopyIdRequest(restoreCandidate.copyId()));
        loanAllAt("B002", JIULONGHU);
        ReservationDTO waitingForRestore = reserve("U-RESTORE", "B002", JIULONGHU);
        service.restoreBookCopy(ADMIN, new BookCopyIdRequest(restoreCandidate.copyId()));
        assertEquals(restoreCandidate.barcode(), reservation(
                "U-RESTORE", waitingForRestore.getReservationId()).getAssignedBarcode());
    }

    @Test
    void concurrentReservationsAssignOnlyOneRemainingCopy() throws Exception {
        List<BookCopy> locationCopies = copies.findByBookId("B001").stream()
                .filter(copy -> copy.location().equals(JIULONGHU)).toList();
        copies.update(locationCopies.get(1).withStatus(BookCopyStatus.LOANED));
        copies.update(locationCopies.get(2).withStatus(BookCopyStatus.LOANED));

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> reserve("U-A", "B001", JIULONGHU));
            var second = executor.submit(() -> reserve("U-B", "B001", JIULONGHU));
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        }

        assertEquals(1, reservations.findAll().stream()
                .filter(value -> value.status() == ReservationStatus.READY_FOR_PICKUP)
                .count());
        assertEquals(1, reservations.findAll().stream()
                .filter(value -> value.status() == ReservationStatus.WAITING).count());
        assertEquals(1, copies.findByBookId("B001").stream()
                .filter(copy -> copy.status() == BookCopyStatus.RESERVED).count());
    }

    @Test
    void inMemoryTransactionRestoresReservationAndCopyAfterPartialFailure() {
        ReservationRepository failing = new ReservationDelegate(reservations) {
            @Override
            public void update(Reservation reservation) {
                super.update(reservation);
                throw new IllegalStateException("simulated reservation write failure");
            }
        };
        LibraryService failingService = new LibraryService(
                books, records, clock, () -> "BR-FAIL",
                new InMemoryBookCategoryRepository(), copies, failing,
                new InMemoryLibraryTransactionManager(
                        books, copies, records, reservations),
                () -> "RS-FAIL");

        assertThrows(IllegalStateException.class, () -> failingService.createReservation(
                "U-FAIL", new CreateReservationRequest("B001", JIULONGHU)));

        assertTrue(reservations.findAll().isEmpty());
        assertEquals(BookCopyStatus.AVAILABLE,
                copies.findByBarcode("SEU-B001-001").orElseThrow().status());
    }

    private ReservationDTO reserve(String userId, String bookId, String location) {
        return service.createReservation(
                userId, new CreateReservationRequest(bookId, location));
    }

    private ReservationDTO reservation(String userId, String reservationId) {
        ReservationDTO result = service.getMyReservations(userId).stream()
                .filter(value -> value.getReservationId().equals(reservationId))
                .findFirst().orElseThrow();
        assertNotNull(result);
        return result;
    }

    private List<Loan> loanAllAt(String bookId, String location) {
        AtomicInteger sequence = new AtomicInteger();
        return copies.findByBookId(bookId).stream()
                .filter(copy -> copy.location().equals(location))
                .filter(copy -> copy.status() == BookCopyStatus.AVAILABLE)
                .map(copy -> {
                    String userId = "U-LOAN-" + bookId + "-" + sequence.incrementAndGet();
                    service.borrowCopy(userId, new CopyBorrowRequest(copy.barcode()));
                    return new Loan(userId, copy.copyId(), copy.barcode());
                }).toList();
    }

    private int[] inventory(String bookId) {
        var book = service.searchBooks(new BookSearchRequest("", null)).getBooks().stream()
                .filter(value -> value.getBookId().equals(bookId)).findFirst().orElseThrow();
        return new int[]{book.getTotalCount(), book.getAvailableCount()};
    }

    private static void failure(String code, Runnable action) {
        assertEquals(code, assertThrows(
                LibraryBusinessException.class, action::run).code());
    }

    private record Loan(String userId, String copyId, String barcode) {
    }

    private static class ReservationDelegate implements ReservationRepository {
        private final ReservationRepository delegate;

        ReservationDelegate(ReservationRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<Reservation> findAll() { return delegate.findAll(); }

        @Override
        public List<Reservation> findByUserId(String userId) {
            return delegate.findByUserId(userId);
        }

        @Override
        public java.util.Optional<Reservation> findById(String reservationId) {
            return delegate.findById(reservationId);
        }

        @Override
        public void save(Reservation reservation) { delegate.save(reservation); }

        @Override
        public void update(Reservation reservation) { delegate.update(reservation); }
    }

    private static final class MutableClock extends Clock {
        private volatile Instant current;
        private final ZoneId zone;

        private MutableClock(Instant current, ZoneId zone) {
            this.current = current;
            this.zone = zone;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() { return zone; }

        @Override
        public Clock withZone(ZoneId newZone) {
            return new MutableClock(current, newZone);
        }

        @Override
        public Instant instant() { return current; }
    }
}
