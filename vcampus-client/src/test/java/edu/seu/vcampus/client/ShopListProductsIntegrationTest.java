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
            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());

            Response response = context.send(ShopActions.LIST_PRODUCTS, ListProductsRequest.allOnSale());

            assertTrue(response.isSuccess());
            ListProductsResponse payload = assertInstanceOf(ListProductsResponse.class, response.getData());
            assertEquals(8, payload.getProducts().size());
            assertTrue(payload.getProducts().stream().noneMatch(item -> item.getName().contains("停售")));
            assertTrue(payload.getProducts().stream().anyMatch(item -> "中性笔 0.5mm".equals(item.getName())));
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
            assertTrue(context.login("20260007", "123456".toCharArray()).isSuccess());

            Response response = context.send(
                    ShopActions.LIST_PRODUCTS,
                    new ListProductsRequest("中性", 1L));

            assertTrue(response.isSuccess());
            ListProductsResponse payload = assertInstanceOf(ListProductsResponse.class, response.getData());
            assertEquals(1, payload.getProducts().size());
            ProductSummaryDto product = payload.getProducts().getFirst();
            assertEquals("中性笔 0.5mm", product.getName());
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
