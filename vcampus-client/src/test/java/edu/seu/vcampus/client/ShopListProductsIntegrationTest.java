package edu.seu.vcampus.client;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.ListProductsRequest;
import edu.seu.vcampus.common.shop.ListProductsResponse;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.ShopActions;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopListProductsIntegrationTest {

    @Test
    void loggedInStudentCanListOnSaleProducts() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260006", "123456".toCharArray()).isSuccess());

            Response response = context.send(ShopActions.LIST_PRODUCTS, ListProductsRequest.allOnSale());

            assertTrue(response.isSuccess());
            ListProductsResponse payload = assertInstanceOf(ListProductsResponse.class, response.getData());
            assertEquals(11, payload.getProducts().size());
            assertTrue(payload.getProducts().stream().noneMatch(item -> item.getName().contains("停售")));
            assertTrue(payload.getProducts().stream().anyMatch(item ->
                    item.getName().contains("中性笔") && item.getName().contains("0.5mm")));
            assertTrue(payload.getProducts().getFirst().getPhotos().size() >= 1);
            assertFalse(payload.getProducts().getFirst().getDescription().isBlank());
        }
    }

    @Test
    void keywordAndCategoryFilterOnSaleCatalog() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260004", "123456".toCharArray()).isSuccess());

            Response response = context.send(
                    ShopActions.LIST_PRODUCTS,
                    new ListProductsRequest("中性", 1L));

            assertTrue(response.isSuccess());
            ListProductsResponse payload = assertInstanceOf(ListProductsResponse.class, response.getData());
            assertEquals(1, payload.getProducts().size());
            ProductSummaryDto product = payload.getProducts().getFirst();
            assertEquals("晨光中性笔 0.5mm 黑色按动", product.getName());
            assertEquals(350, product.getPriceFen());
        }
    }

    @Test
    void anonymousRequestIsRejected() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));

            Response response = context.send(ShopActions.LIST_PRODUCTS, ListProductsRequest.allOnSale());

            assertFalse(response.isSuccess());
            assertEquals(ErrorCodes.AUTH_REQUIRED, response.getCode());
        }
    }
}
