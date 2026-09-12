package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.shop.CreateOrderRequest;
import edu.seu.vcampus.common.shop.ListProductsRequest;
import edu.seu.vcampus.common.shop.OrderLineRequest;
import edu.seu.vcampus.common.shop.PublishProductRequest;
import edu.seu.vcampus.common.shop.ShopPaymentMethods;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessShopAtomicCheckoutTest {
    @TempDir Path directory;

    @Test
    void onlyOneBuyerCanPurchaseTheLastItem() throws Exception {
        AccessDatabase database = new AccessDatabase(directory.resolve("atomic-shop.accdb"));
        AccessShopCatalog catalog = new AccessShopCatalog(database);
        AccessCampusCardStore cards = new AccessCampusCardStore(database);
        AccessShopOrderStore orders = new AccessShopOrderStore(database);
        AccessShopCheckoutTransaction checkout = new AccessShopCheckoutTransaction(database);
        long productId = catalog.publish(
                new PublishProductRequest(
                        "并发测试商品", 1, "仅一件库存", 100, 1,
                        List.of(new byte[]{1})),
                "葛丰玮").getProductId();
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderLineRequest(productId, 1)),
                ShopPaymentMethods.CAMPUS_CARD,
                "测试取货");
        SessionInfo first = session("U-STUDENT-001", "20260006", "周一");
        SessionInfo second = session("U-STUDENT-002", "20260007", "周二");

        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Callable<Boolean>> buyers = List.of(
                    () -> buy(checkout, first, request, productId),
                    () -> buy(checkout, second, request, productId));
            long successes = executor.invokeAll(buyers).stream()
                    .filter(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .count();
            assertEquals(1, successes);
        }

        assertEquals(0, catalog.listOnSale(new ListProductsRequest(null, null)).stream()
                .filter(product -> product.getProductId() == productId)
                .findFirst().orElseThrow().getStockQty());
        assertEquals(1, orders.listAllNewestFirst().size());
        assertEquals(19_900,
                cards.view(first).getBalanceFen() + cards.view(second).getBalanceFen());
    }

    private static boolean buy(
            AccessShopCheckoutTransaction checkout,
            SessionInfo session,
            CreateOrderRequest request,
            long productId) {
        try {
            checkout.createOrder(session, request, java.util.Map.of(productId, 1));
            return true;
        } catch (ShopBusinessException exception) {
            return false;
        }
    }

    private static SessionInfo session(String userId, String card, String name) {
        return new SessionInfo("token-" + card, userId, card, name, Role.USER, Set.of());
    }
}
