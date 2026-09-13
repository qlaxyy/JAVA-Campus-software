package edu.seu.vcampus.common.card;

import edu.seu.vcampus.common.protocol.ActionNames;
import edu.seu.vcampus.common.protocol.ModuleNames;

/**
 * Public actions of the campus-card TCP gateway.
 *
 * <p>Other subsystems connect to the card port (default 8889) and send these
 * actions with the paying user's session token.
 */
public final class CardActions {

    public static final String GET = ActionNames.of(ModuleNames.CARD, "GET");
    public static final String RECHARGE = ActionNames.of(ModuleNames.CARD, "RECHARGE");
    public static final String DEBIT = ActionNames.of(ModuleNames.CARD, "DEBIT");
    public static final String CREDIT = ActionNames.of(ModuleNames.CARD, "CREDIT");
    public static final String LIST_LEDGER = ActionNames.of(ModuleNames.CARD, "LIST_LEDGER");

    private CardActions() {
    }
}
