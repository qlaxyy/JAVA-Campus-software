package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.user.SessionInfo;

/** Persistence boundary for campus-card balances. */
interface CampusCardRepository {
    CampusCardView view(SessionInfo session);
    CampusCardView recharge(SessionInfo session, int amountFen);
    CampusCardView deduct(SessionInfo session, int amountFen);
    void refund(String userId, int amountFen);
}
