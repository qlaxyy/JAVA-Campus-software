package edu.seu.vcampus.server.module.card;

import edu.seu.vcampus.common.card.CampusCardLedgerEntry;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.user.SessionInfo;

import java.util.List;

/**
 * Campus-card wallet used by the card TCP gateway and by other server modules.
 */
public interface CampusCardWallet {

    /**
     * Returns the caller's wallet, creating a zero-balance card when needed.
     *
     * @param session authenticated payer
     * @return current snapshot
     */
    CampusCardView view(SessionInfo session);

    /**
     * Adds money to the caller's card.
     *
     * @param session authenticated payer
     * @param amountFen 10–100 yuan in fen
     * @return updated snapshot
     */
    CampusCardView recharge(SessionInfo session, int amountFen);

    /**
     * Deducts money for a merchant. Repeating the same merchant and reference is a no-op.
     *
     * @param session authenticated payer
     * @param amountFen positive amount
     * @param merchant subsystem id
     * @param reference merchant-unique key
     * @return updated snapshot
     */
    CampusCardView debit(SessionInfo session, int amountFen, String merchant, String reference);

    /**
     * Refunds money for a merchant. Repeating the same merchant and reference is a no-op.
     *
     * @param session authenticated payer
     * @param amountFen positive amount
     * @param merchant subsystem id
     * @param reference merchant-unique key
     * @return updated snapshot
     */
    CampusCardView credit(SessionInfo session, int amountFen, String merchant, String reference);

    /**
     * Lists recent movements for the caller, newest first.
     *
     * @param session authenticated payer
     * @return ledger rows
     */
    List<CampusCardLedgerEntry> listLedger(SessionInfo session);
}
