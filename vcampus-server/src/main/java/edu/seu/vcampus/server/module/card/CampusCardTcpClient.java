package edu.seu.vcampus.server.module.card;

import edu.seu.vcampus.common.card.CardActions;
import edu.seu.vcampus.common.card.CardRechargeRequest;
import edu.seu.vcampus.common.card.CardTransferRequest;
import edu.seu.vcampus.common.card.CampusCardLedgerEntry;
import edu.seu.vcampus.common.card.CampusCardLedgerResponse;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.CampusSocketClient;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Connects shop, hospital, library and other subsystems to the campus-card TCP port.
 */
public final class CampusCardTcpClient implements CampusCardWallet {

    private final CampusSocketClient client;

    /**
     * Creates a client for the campus-card gateway.
     *
     * @param host card-server host, usually {@code 127.0.0.1} on the same JVM
     * @param port card-server TCP port
     */
    public CampusCardTcpClient(String host, int port) {
        this.client = new CampusSocketClient(host, port);
    }

    @Override
    public CampusCardView view(SessionInfo session) {
        return requireCard(send(session, CardActions.GET, null));
    }

    @Override
    public CampusCardView recharge(SessionInfo session, int amountFen) {
        return requireCard(send(session, CardActions.RECHARGE, new CardRechargeRequest(amountFen)));
    }

    @Override
    public CampusCardView debit(
            SessionInfo session, int amountFen, String merchant, String reference) {
        return requireCard(send(
                session,
                CardActions.DEBIT,
                new CardTransferRequest(amountFen, merchant, reference)));
    }

    @Override
    public CampusCardView credit(
            SessionInfo session, int amountFen, String merchant, String reference) {
        return requireCard(send(
                session,
                CardActions.CREDIT,
                new CardTransferRequest(amountFen, merchant, reference)));
    }

    @Override
    public List<CampusCardLedgerEntry> listLedger(SessionInfo session) {
        Response response = send(session, CardActions.LIST_LEDGER, null);
        if (response.getData() instanceof CampusCardLedgerResponse ledger) {
            return ledger.getEntries();
        }
        throw new CardBusinessException(
                ErrorCodes.COMMON_SERVER_ERROR, "校园卡流水格式无效。");
    }

    private Response send(SessionInfo session, String action, Serializable data) {
        Objects.requireNonNull(session, "session must not be null");
        try {
            Response response = client.send(Request.create(action, session.getToken(), data));
            if (!response.isSuccess()) {
                throw new CardBusinessException(response.getCode(), response.getMessage());
            }
            return response;
        } catch (IOException exception) {
            throw new CardBusinessException(
                    ErrorCodes.COMMON_SERVER_ERROR,
                    "无法连接校园卡服务，请确认独立端口已经启动。");
        }
    }

    private static CampusCardView requireCard(Response response) {
        if (response.getData() instanceof CampusCardView card) {
            return card;
        }
        throw new CardBusinessException(ErrorCodes.COMMON_SERVER_ERROR, "校园卡响应格式无效。");
    }
}
