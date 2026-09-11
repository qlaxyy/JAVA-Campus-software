package edu.seu.vcampus.server.module.card;

import edu.seu.vcampus.common.card.CampusCardLedgerEntry;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.user.SessionInfo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * In-memory wallets used by automated tests.
 */
public final class InMemoryCampusCardWallet implements CampusCardWallet {

    static final int DEMO_BALANCE_FEN = 10_000;
    private static final int MAX_BALANCE_FEN = 1_000_000;
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, CampusCardView> cards = new HashMap<>();
    private final List<CampusCardLedgerEntry> ledger = new ArrayList<>();

    /** Seeds student and shop-admin demo cards with 100 yuan. */
    public InMemoryCampusCardWallet() {
        cards.put("U-STUDENT-001", new CampusCardView(
                "U-STUDENT-001", "20260006", "20260006", DEMO_BALANCE_FEN));
        cards.put("U-SHOP-ADMIN-001", new CampusCardView(
                "U-SHOP-ADMIN-001", "20260004", "20260004", DEMO_BALANCE_FEN));
    }

    @Override
    public synchronized CampusCardView view(SessionInfo session) {
        return ensure(session);
    }

    @Override
    public synchronized CampusCardView recharge(SessionInfo session, int amountFen) {
        CampusCardView current = ensure(session);
        int next = Math.addExact(current.getBalanceFen(), amountFen);
        if (next > MAX_BALANCE_FEN) {
            throw new CardBusinessException(ErrorCodes.COMMON_INVALID_REQUEST, "校园卡余额已达上限。");
        }
        CampusCardView updated = replace(current, next);
        record(session, ModuleNames.CARD, "recharge:" + UUID.randomUUID(),
                CampusCardLedgerEntry.RECHARGE, amountFen);
        return updated;
    }

    @Override
    public synchronized CampusCardView debit(
            SessionInfo session, int amountFen, String merchant, String reference) {
        requireTransfer(amountFen, merchant, reference);
        if (alreadyPosted(session.getUserId(), merchant, reference, CampusCardLedgerEntry.DEBIT)) {
            return ensure(session);
        }
        CampusCardView current = ensure(session);
        if (current.getBalanceFen() < amountFen) {
            throw new CardBusinessException(ErrorCodes.CARD_INSUFFICIENT_BALANCE, "余额不足，请充值！");
        }
        CampusCardView updated = replace(current, current.getBalanceFen() - amountFen);
        record(session, merchant, reference, CampusCardLedgerEntry.DEBIT, amountFen);
        return updated;
    }

    @Override
    public synchronized CampusCardView credit(
            SessionInfo session, int amountFen, String merchant, String reference) {
        requireTransfer(amountFen, merchant, reference);
        if (alreadyPosted(session.getUserId(), merchant, reference, CampusCardLedgerEntry.CREDIT)) {
            return ensure(session);
        }
        CampusCardView current = ensure(session);
        int next = Math.addExact(current.getBalanceFen(), amountFen);
        if (next > MAX_BALANCE_FEN) {
            throw new CardBusinessException(ErrorCodes.COMMON_INVALID_REQUEST, "校园卡余额已达上限。");
        }
        CampusCardView updated = replace(current, next);
        record(session, merchant, reference, CampusCardLedgerEntry.CREDIT, amountFen);
        return updated;
    }

    @Override
    public synchronized List<CampusCardLedgerEntry> listLedger(SessionInfo session) {
        Objects.requireNonNull(session, "session must not be null");
        List<CampusCardLedgerEntry> mine = new ArrayList<>();
        for (int index = ledger.size() - 1; index >= 0; index--) {
            CampusCardLedgerEntry entry = ledger.get(index);
            if (entry.getUserId().equals(session.getUserId())) {
                mine.add(entry);
            }
        }
        return List.copyOf(mine);
    }

    private CampusCardView ensure(SessionInfo session) {
        Objects.requireNonNull(session, "session must not be null");
        CampusCardView existing = cards.get(session.getUserId());
        if (existing != null) {
            return existing;
        }
        CampusCardView created = new CampusCardView(
                session.getUserId(),
                session.getUsername(),
                session.getUsername(),
                0);
        cards.put(session.getUserId(), created);
        return created;
    }

    private CampusCardView replace(CampusCardView current, int balanceFen) {
        CampusCardView updated = new CampusCardView(
                current.getUserId(), current.getUsername(), current.getCardNo(), balanceFen);
        cards.put(current.getUserId(), updated);
        return updated;
    }

    private void record(
            SessionInfo session,
            String merchant,
            String reference,
            String entryType,
            int amountFen) {
        ledger.add(new CampusCardLedgerEntry(
                UUID.randomUUID().toString(),
                session.getUserId(),
                merchant,
                reference,
                entryType,
                amountFen,
                LocalDateTime.now().format(TIME)));
    }

    private boolean alreadyPosted(String userId, String merchant, String reference, String entryType) {
        for (CampusCardLedgerEntry entry : ledger) {
            if (entry.getUserId().equals(userId)
                    && entry.getMerchant().equals(merchant)
                    && entry.getReference().equals(reference)
                    && entry.getEntryType().equals(entryType)) {
                return true;
            }
        }
        return false;
    }

    private static void requireTransfer(int amountFen, String merchant, String reference) {
        if (amountFen < 1) {
            throw new CardBusinessException(ErrorCodes.COMMON_INVALID_REQUEST, "金额无效。");
        }
        if (merchant == null || merchant.isBlank() || reference == null || reference.isBlank()) {
            throw new CardBusinessException(ErrorCodes.COMMON_INVALID_REQUEST, "商户或业务单号无效。");
        }
    }
}
