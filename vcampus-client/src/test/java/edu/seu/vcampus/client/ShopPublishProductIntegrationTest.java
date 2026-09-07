package edu.seu.vcampus.client;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.ListProductsRequest;
import edu.seu.vcampus.common.shop.ListProductsResponse;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.PublishProductRequest;
import edu.seu.vcampus.common.shop.ShopActions;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopPublishProductIntegrationTest {

    @Test
    void shopAdminCanPublishProductVisibleToStudents() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext admin = new ClientContext(new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(admin.login("20260007", "123456".toCharArray()).isSuccess());

            Response published = admin.send(
                    ShopActions.PUBLISH_PRODUCT,
                    new PublishProductRequest(
                            "二手计算器",
                            1L,
                            "按键灵敏，适合期末带去考场。",
                            1500,
                            1,
                            List.of(samplePhoto())));
            assertTrue(published.isSuccess());
            ProductSummaryDto created = assertInstanceOf(ProductSummaryDto.class, published.getData());
            assertEquals(1, created.getPhotos().size());
            assertEquals("按键灵敏，适合期末带去考场。", created.getDescription());

            ClientContext student = new ClientContext(new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(student.login("20260001", "123456".toCharArray()).isSuccess());
            Response listed = student.send(ShopActions.LIST_PRODUCTS, ListProductsRequest.allOnSale());
            ListProductsResponse payload = assertInstanceOf(ListProductsResponse.class, listed.getData());
            assertEquals(9, payload.getProducts().size());
            assertTrue(payload.getProducts().stream().anyMatch(item -> "二手计算器".equals(item.getName())));
        }
    }

    @Test
    void studentCannotPublishProduct() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext student = new ClientContext(new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(student.login("20260001", "123456".toCharArray()).isSuccess());

            Response response = student.send(
                    ShopActions.PUBLISH_PRODUCT,
                    new PublishProductRequest(
                            "二手计算器",
                            1L,
                            "学生不能上架。",
                            1500,
                            1,
                            List.of(samplePhoto())));

            assertFalse(response.isSuccess());
            assertEquals(ErrorCodes.AUTH_FORBIDDEN, response.getCode());
        }
    }

    @Test
    void shopAdminCanUpdateProductAndReadListingLog() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext admin = new ClientContext(new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(admin.login("shopadmin", "123456".toCharArray()).isSuccess());

            Response updated = admin.send(
                    ShopActions.UPDATE_PRODUCT,
                    new edu.seu.vcampus.common.shop.UpdateProductRequest(
                            8L,
                            "矿泉水 550ml",
                            "冰柜常温都有。上课带走方便，瓶身轻。",
                            250,
                            5));
            assertTrue(updated.isSuccess());
            ProductSummaryDto product = assertInstanceOf(ProductSummaryDto.class, updated.getData());
            assertEquals(250, product.getPriceFen());
            assertEquals(205, product.getStockQty());

            Response listings = admin.send(ShopActions.LIST_LISTINGS, null);
            edu.seu.vcampus.common.shop.ListListingsResponse payload = assertInstanceOf(
                    edu.seu.vcampus.common.shop.ListListingsResponse.class, listings.getData());
            assertTrue(payload.getRecords().size() >= 9);
            assertEquals("调整", payload.getRecords().getFirst().getAction());
        }
    }

    @Test
    void studentCannotUpdateProduct() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext student = new ClientContext(new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(student.login("student001", "123456".toCharArray()).isSuccess());
            Response response = student.send(
                    ShopActions.UPDATE_PRODUCT,
                    new edu.seu.vcampus.common.shop.UpdateProductRequest(
                            8L, "矿泉水 550ml", "描述", 200, 0));
            assertFalse(response.isSuccess());
            assertEquals(ErrorCodes.AUTH_FORBIDDEN, response.getCode());
        }
    }

    private static byte[] samplePhoto() throws Exception {
        BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(15, 118, 110));
        graphics.fillRect(0, 0, 80, 80);
        graphics.dispose();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(image, "png", buffer);
        return buffer.toByteArray();
    }
}
