package edu.seu.vcampus.client;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.shop.CancelOrderRequest;
import edu.seu.vcampus.common.shop.CreateOrderRequest;
import edu.seu.vcampus.common.shop.ListOrdersResponse;
import edu.seu.vcampus.common.shop.OrderLineRequest;
import edu.seu.vcampus.common.shop.RechargeCampusCardRequest;
import edu.seu.vcampus.common.shop.ShopActions;
import edu.seu.vcampus.common.shop.ShopOrderDto;
import edu.seu.vcampus.common.shop.ShopOrderStatus;
import edu.seu.vcampus.common.shop.ShopPaymentMethods;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopCampusCardCheckoutIntegrationTest {

    @Test
    void studentAndShopAdminStartWithOneHundredYuan() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            assertEquals(10_000, balance("student001", server.getPort()));
            assertEquals(10_000, balance("shopadmin", server.getPort()));
        }
    }

    @Test
    void campusCardPayDeductsBalanceAndListsOrder() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext student = login("student001", server.getPort());
            Response paid = student.send(
                    ShopActions.CREATE_ORDER,
                    new CreateOrderRequest(
                            List.of(new OrderLineRequest(8, 2)),
                            ShopPaymentMethods.CAMPUS_CARD,
                            "校内自提"));
            assertTrue(paid.isSuccess());
            ShopOrderDto order = assertInstanceOf(ShopOrderDto.class, paid.getData());
            assertEquals(400, order.getTotalFen());
            assertEquals(ShopOrderStatus.PAID, order.getStatus());

            CampusCardView card = assertInstanceOf(
                    CampusCardView.class, student.send(ShopActions.GET_CAMPUS_CARD, null).getData());
            assertEquals(9600, card.getBalanceFen());

            ListOrdersResponse orders = assertInstanceOf(
                    ListOrdersResponse.class, student.send(ShopActions.LIST_ORDERS, null).getData());
            assertEquals(1, orders.getOrders().size());
        }
    }

    @Test
    void insufficientBalanceAsksToRecharge() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext student = login("student001", server.getPort());
            Response paid = student.send(
                    ShopActions.CREATE_ORDER,
                    new CreateOrderRequest(
                            List.of(new OrderLineRequest(7, 30)),
                            ShopPaymentMethods.CAMPUS_CARD,
                            "校内自提"));
            assertFalse(paid.isSuccess());
            assertEquals(ErrorCodes.SHOP_INSUFFICIENT_BALANCE, paid.getCode());
            assertEquals("余额不足，请充值！", paid.getMessage());
        }
    }

    @Test
    void rechargeThenCancelRefundsCampusCard() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext student = login("student001", server.getPort());
            student.send(ShopActions.RECHARGE_CAMPUS_CARD, new RechargeCampusCardRequest(2000));
            Response paid = student.send(
                    ShopActions.CREATE_ORDER,
                    new CreateOrderRequest(
                            List.of(new OrderLineRequest(8, 1)),
                            ShopPaymentMethods.CAMPUS_CARD,
                            "校内自提"));
            ShopOrderDto order = assertInstanceOf(ShopOrderDto.class, paid.getData());
            Response cancelled = student.send(
                    ShopActions.CANCEL_ORDER, new CancelOrderRequest(order.getOrderId()));
            assertTrue(cancelled.isSuccess());
            CampusCardView card = assertInstanceOf(
                    CampusCardView.class, student.send(ShopActions.GET_CAMPUS_CARD, null).getData());
            assertEquals(12_000, card.getBalanceFen());
        }
    }

    @Test
    void studentCannotListSales() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext student = login("student001", server.getPort());
            Response response = student.send(ShopActions.LIST_SALES, null);
            assertFalse(response.isSuccess());
            assertEquals(ErrorCodes.AUTH_FORBIDDEN, response.getCode());
        }
    }

    private static int balance(String username, int port) throws Exception {
        ClientContext context = login(username, port);
        CampusCardView card = assertInstanceOf(
                CampusCardView.class, context.send(ShopActions.GET_CAMPUS_CARD, null).getData());
        return card.getBalanceFen();
    }

    private static ClientContext login(String username, int port) throws Exception {
        ClientContext context = new ClientContext(new CampusClient("127.0.0.1", port));
        assertTrue(context.login(username, "123456".toCharArray()).isSuccess());
        return context;
    }
}
