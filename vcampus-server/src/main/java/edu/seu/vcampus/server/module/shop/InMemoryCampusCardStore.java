package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.user.SessionInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * In-memory virtual campus-card wallets. The login campus-card number is reused as card number.
 */
final class InMemoryCampusCardStore {

    static final int DEMO_BALANCE_FEN = 10_000;

    private final Map<String, CampusCardView> cards = new HashMap<>();
    InMemoryCampusCardStore() {
        cards.put("U-STUDENT-001", new CampusCardView(
                "U-STUDENT-001", "20260001", "20260001", DEMO_BALANCE_FEN));
        cards.put("U-SHOP-ADMIN-001", new CampusCardView(
                "U-SHOP-ADMIN-001", "20260007", "20260007", DEMO_BALANCE_FEN));
    }

    synchronized CampusCardView view(SessionInfo session) {
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

    synchronized CampusCardView recharge(SessionInfo session, int amountFen) {
        CampusCardView current = view(session);
        int next = Math.addExact(current.getBalanceFen(), amountFen);
        if (next > 1_000_000) {
            throw new ShopBusinessException(
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    "校园卡余额已达上限。");
        }
        CampusCardView updated = new CampusCardView(
                current.getUserId(), current.getUsername(), current.getCardNo(), next);
        cards.put(session.getUserId(), updated);
        return updated;
    }

    synchronized CampusCardView deduct(SessionInfo session, int amountFen) {
        CampusCardView current = view(session);
        if (current.getBalanceFen() < amountFen) {
            throw new ShopBusinessException(
                    ErrorCodes.SHOP_INSUFFICIENT_BALANCE,
                    "余额不足，请充值！");
        }
        CampusCardView updated = new CampusCardView(
                current.getUserId(),
                current.getUsername(),
                current.getCardNo(),
                current.getBalanceFen() - amountFen);
        cards.put(session.getUserId(), updated);
        return updated;
    }

    synchronized void refund(String userId, int amountFen) {
        CampusCardView current = cards.get(userId);
        if (current == null) {
            return;
        }
        cards.put(userId, new CampusCardView(
                current.getUserId(),
                current.getUsername(),
                current.getCardNo(),
                current.getBalanceFen() + amountFen));
    }
}
