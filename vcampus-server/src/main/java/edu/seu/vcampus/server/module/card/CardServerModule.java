package edu.seu.vcampus.server.module.card;

import edu.seu.vcampus.common.card.CardActions;
import edu.seu.vcampus.common.card.CardRechargeRequest;
import edu.seu.vcampus.common.card.CardTransferRequest;
import edu.seu.vcampus.common.card.CampusCardLedgerResponse;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.RechargeCampusCardRequest;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerContext;
import edu.seu.vcampus.server.module.ServerModule;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Independent campus-card subsystem registered on its own TCP port.
 */
public final class CardServerModule implements ServerModule {

    private final CampusCardWallet wallet;

    /**
     * Creates the card module.
     *
     * @param wallet Access or memory wallet
     */
    public CardServerModule(CampusCardWallet wallet) {
        this.wallet = Objects.requireNonNull(wallet, "wallet must not be null");
    }

    @Override
    public String id() {
        return ModuleNames.CARD;
    }

    @Override
    public void registerHandlers(ActionRouter router, ServerContext context) {
        router.register(CardActions.GET, request -> requireSession(request, context,
                session -> Response.success(request, "已返回校园卡余额。", wallet.view(session))));
        router.register(CardActions.RECHARGE, request -> requireSession(request, context, session -> {
            int amountFen = rechargeAmount(request.getData());
            if (amountFen < 1) {
                return Response.failure(
                        request.getRequestId(),
                        ErrorCodes.COMMON_INVALID_REQUEST,
                        "充值金额无效。");
            }
            return Response.success(request, "充值成功。", wallet.recharge(session, amountFen));
        }));
        router.register(CardActions.DEBIT, request -> requireSession(request, context, session -> {
            if (!(request.getData() instanceof CardTransferRequest payload)) {
                return Response.failure(
                        request.getRequestId(),
                        ErrorCodes.COMMON_INVALID_REQUEST,
                        "扣款信息不完整。");
            }
            return Response.success(
                    request,
                    "校园卡已扣款。",
                    wallet.debit(
                            session,
                            payload.getAmountFen(),
                            payload.getMerchant(),
                            payload.getReference()));
        }));
        router.register(CardActions.CREDIT, request -> requireSession(request, context, session -> {
            if (!(request.getData() instanceof CardTransferRequest payload)) {
                return Response.failure(
                        request.getRequestId(),
                        ErrorCodes.COMMON_INVALID_REQUEST,
                        "退款信息不完整。");
            }
            return Response.success(
                    request,
                    "校园卡已入账。",
                    wallet.credit(
                            session,
                            payload.getAmountFen(),
                            payload.getMerchant(),
                            payload.getReference()));
        }));
        router.register(CardActions.LIST_LEDGER, request -> requireSession(request, context,
                session -> Response.success(
                        request,
                        "已返回校园卡流水。",
                        new CampusCardLedgerResponse(wallet.listLedger(session)))));
    }

    private Response requireSession(
            Request request,
            ServerContext context,
            Function<SessionInfo, Response> action) {
        Optional<SessionInfo> session = context.sessions().findSession(request.getToken());
        if (session.isEmpty()) {
            return Response.failure(request.getRequestId(), ErrorCodes.AUTH_REQUIRED, "请先登录。");
        }
        try {
            return action.apply(session.get());
        } catch (CardBusinessException exception) {
            return Response.failure(request.getRequestId(), exception.code(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    "请求参数无效。");
        }
    }

    private static int rechargeAmount(Object data) {
        if (data instanceof CardRechargeRequest payload) {
            return payload.getAmountFen();
        }
        if (data instanceof RechargeCampusCardRequest payload) {
            return payload.getAmountFen();
        }
        return -1;
    }
}
