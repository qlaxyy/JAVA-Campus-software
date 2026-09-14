package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BorrowRecordDTO;
import edu.seu.vcampus.common.library.BorrowRecordIdRequest;
import edu.seu.vcampus.common.library.CopyBorrowRequest;
import edu.seu.vcampus.common.library.CreateReservationRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.module.card.InMemoryCampusCardWallet;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 逾期滞纳金的计算、缴费与拦截。
 *
 * <p>费用金额从不落库，只由 {@code dueTime} 与 {@code returnTime} 推导，因此这里直接构造
 * 不同逾期天数的记录来验证费率与封顶，并用既有的 {@link MutableClock} 推进时间。
 */
class LibraryFeeServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 9, 8, 0);
    private static final String USER_ID = "U-STUDENT-001";
    private static final int DEMO_BALANCE_FEN = 10_000;
    private static final int FINE_PER_DAY_FEN = 50;
    private static final int MAX_FINE_FEN = 5_000;

    private static final SessionInfo READER =
            new SessionInfo("token-1", USER_ID, "20260006", "读者", Role.USER);

    private final MutableClock clock =
            new MutableClock(Instant.parse("2026-09-09T08:00:00Z"), ZoneOffset.UTC);
    private final InMemoryBookRepository books = new InMemoryBookRepository();
    private final InMemoryBorrowRecordRepository records = new InMemoryBorrowRecordRepository();
    private final InMemoryBookCopyRepository copies =
            InMemoryBookCopyRepository.seededFrom(books.searchAll(""));
    private final InMemoryReservationRepository reservations = new InMemoryReservationRepository();
    private final InMemoryCampusCardWallet wallet = new InMemoryCampusCardWallet();
    private final AtomicInteger sequence = new AtomicInteger();
    private final LibraryService service = new LibraryService(
            books, records, clock, () -> "BR-" + sequence.incrementAndGet(),
            new InMemoryBookCategoryRepository(), copies, reservations,
            LibraryTransactionManager.passthrough(),
            () -> "RS-%03d".formatted(sequence.incrementAndGet()),
            wallet);

    @Test
    void overdueFineIsZeroWhenReturnedOnTime() {
        savedReturned("BR-ONTIME", 0);
        assertEquals(0, feeOf("BR-ONTIME"));
        assertFalse(isSettled("BR-ONTIME"));
    }

    @Test
    void overdueFineChargesPerDay() {
        savedReturned("BR-1DAY", 1);
        assertEquals(FINE_PER_DAY_FEN, feeOf("BR-1DAY"));

        savedReturned("BR-15DAY", 15);
        assertEquals(15 * FINE_PER_DAY_FEN, feeOf("BR-15DAY"));
    }

    @Test
    void overdueFineIsCapped() {
        savedReturned("BR-CAP", MAX_FINE_FEN / FINE_PER_DAY_FEN);
        assertEquals(MAX_FINE_FEN, feeOf("BR-CAP"));

        savedReturned("BR-HUGE", 1_000);
        assertEquals(MAX_FINE_FEN, feeOf("BR-HUGE"), "逾期再久也不会超过封顶金额");
    }

    @Test
    void activeLoanProducesNoFee() {
        // 未归还的逾期记录不产生费用：此时仍由“有逾期未还”这条既有规则拦截
        records.save(new BorrowRecord("BR-ACTIVE", USER_ID, "CP-B001-001",
                NOW.minusDays(30), NOW, BorrowStatus.BORROWED));
        assertEquals(0, feeOf("BR-ACTIVE"));
    }

    @Test
    void payingSettlesTheFeeAndDebitsTheCard() {
        savedReturned("BR-PAY", 15);
        int before = balance();
        assertEquals(15 * FINE_PER_DAY_FEN, feeOf("BR-PAY"));

        BorrowRecordDTO settled = service.payFee(READER, new BorrowRecordIdRequest("BR-PAY"));

        assertTrue(settled.isFeeSettled());
        assertEquals(15 * FINE_PER_DAY_FEN, settled.getFeeFen());
        assertEquals(before - 15 * FINE_PER_DAY_FEN, balance());
    }

    @Test
    void payingAgainDoesNotChargeTwice() {
        savedReturned("BR-TWICE", 10);
        service.payFee(READER, new BorrowRecordIdRequest("BR-TWICE"));
        int afterFirst = balance();

        failure(ErrorCodes.LIBRARY_FEE_NOT_PAYABLE,
                () -> service.payFee(READER, new BorrowRecordIdRequest("BR-TWICE")));
        assertEquals(afterFirst, balance(), "重复缴费不能再次扣款");
    }

    @Test
    void payingARecordWithoutFeeIsRejected() {
        savedReturned("BR-NOFEE", 0);
        failure(ErrorCodes.LIBRARY_FEE_NOT_PAYABLE,
                () -> service.payFee(READER, new BorrowRecordIdRequest("BR-NOFEE")));
        assertEquals(DEMO_BALANCE_FEN, balance());
    }

    @Test
    void anotherReaderCannotPaySomeoneElsesFee() {
        savedReturned("BR-OTHER", 10);
        SessionInfo stranger = new SessionInfo("token-2", "U-STUDENT-002", "20260007", "旁人", Role.USER);
        failure(ErrorCodes.LIBRARY_BORROW_RECORD_NOT_FOUND,
                () -> service.payFee(stranger, new BorrowRecordIdRequest("BR-OTHER")));
    }

    @Test
    void insufficientBalanceIsReportedWithTheCardErrorCode() {
        savedReturned("BR-POOR", 15);
        wallet.debit(READER, DEMO_BALANCE_FEN - 100, "SHOP", "drain-1");

        failure(ErrorCodes.CARD_INSUFFICIENT_BALANCE,
                () -> service.payFee(READER, new BorrowRecordIdRequest("BR-POOR")));
    }

    @Test
    void afailedLocalWriteIsRefundedToTheCard() {
        savedReturned("BR-REFUND", 15);
        FailingUpdateRecords failing = new FailingUpdateRecords(records);
        LibraryService failingService = serviceWith(failing);
        failing.failNextUpdate();

        assertThrows(RuntimeException.class,
                () -> failingService.payFee(READER, new BorrowRecordIdRequest("BR-REFUND")));

        assertEquals(DEMO_BALANCE_FEN, balance(), "扣款成功但本地写入失败时必须补偿退款");
        assertFalse(isSettled("BR-REFUND"), "记录仍应处于未缴状态");
    }

    @Test
    void outstandingFeeBlocksBorrowingAndReservingUntilSettled() {
        savedReturned("BR-BLOCK", 15);
        String barcode = availableBarcode("B002");
        String location = locationOf("B004");

        failure(ErrorCodes.LIBRARY_OUTSTANDING_FEE,
                () -> service.borrowCopy(USER_ID, new CopyBorrowRequest(barcode)));
        failure(ErrorCodes.LIBRARY_OUTSTANDING_FEE,
                () -> service.createReservation(USER_ID,
                        new CreateReservationRequest("B004", location)));

        service.payFee(READER, new BorrowRecordIdRequest("BR-BLOCK"));

        service.borrowCopy(USER_ID, new CopyBorrowRequest(barcode));
        assertEquals(1, service.getBorrowRecords(USER_ID).stream()
                .filter(record -> "BORROWED".equals(record.getStatus())).count());
        assertEquals("READY_FOR_PICKUP", service.createReservation(USER_ID,
                new CreateReservationRequest("B004", location)).getStatus());
    }

    @Test
    void settledFeeSurvivesClockAdvance() {
        savedReturned("BR-STABLE", 15);
        int before = balance();
        service.payFee(READER, new BorrowRecordIdRequest("BR-STABLE"));
        int after = balance();

        clock.advance(Duration.ofDays(365));

        assertEquals(after, balance());
        assertEquals(before - 15 * FINE_PER_DAY_FEN, after);
        assertTrue(isSettled("BR-STABLE"));
        assertEquals(15 * FINE_PER_DAY_FEN, feeOf("BR-STABLE"), "结清后金额不再变化");
    }

    /** 存入一条“到期后第 overdueDays 天归还”的记录；0 表示恰好在到期时刻归还。 */
    private void savedReturned(String recordId, long overdueDays) {
        records.save(new BorrowRecord(recordId, USER_ID, "CP-B001-001",
                NOW.minusDays(30), NOW, BorrowStatus.BORROWED)
                .returnedAt(NOW.plusDays(overdueDays)));
    }

    private BorrowRecordDTO record(String recordId) {
        return service.getBorrowRecords(USER_ID).stream()
                .filter(value -> value.getRecordId().equals(recordId))
                .findFirst().orElseThrow();
    }

    private int feeOf(String recordId) {
        return record(recordId).getFeeFen();
    }

    private boolean isSettled(String recordId) {
        return record(recordId).isFeeSettled();
    }

    private int balance() {
        return wallet.view(READER).getBalanceFen();
    }

    private String availableBarcode(String bookId) {
        return copies.findByBookId(bookId).stream()
                .filter(copy -> copy.status() == BookCopyStatus.AVAILABLE)
                .findFirst().orElseThrow().barcode();
    }

    private String locationOf(String bookId) {
        return copies.findByBookId(bookId).getFirst().location();
    }

    private LibraryService serviceWith(BorrowRecordRepository recordRepository) {
        return new LibraryService(
                books, recordRepository, clock, () -> "BR-X-" + sequence.incrementAndGet(),
                new InMemoryBookCategoryRepository(), copies, reservations,
                LibraryTransactionManager.passthrough(),
                () -> "RS-X-%03d".formatted(sequence.incrementAndGet()),
                wallet);
    }

    private static void failure(String code, Runnable action) {
        assertEquals(code, assertThrows(LibraryBusinessException.class, action::run).code());
    }

    /** 在指定的那一次更新上注入失败，用于验证补偿退款。 */
    private static final class FailingUpdateRecords implements BorrowRecordRepository {

        private final BorrowRecordRepository delegate;
        private boolean failNextUpdate;

        private FailingUpdateRecords(BorrowRecordRepository delegate) {
            this.delegate = delegate;
        }

        void failNextUpdate() {
            failNextUpdate = true;
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
            if (failNextUpdate) {
                failNextUpdate = false;
                throw new IllegalStateException("simulated borrow-record write failure");
            }
            delegate.update(record);
        }
    }
}
