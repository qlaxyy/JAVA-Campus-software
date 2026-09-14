package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.shop.ListProductsRequest;
import edu.seu.vcampus.common.shop.ShoppingCartView;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessShopPersistenceTest {
    @TempDir Path directory;

    @Test
    void balanceAndCartSurviveRepositoryRestart() {
        Path path = directory.resolve("shop.accdb");
        AccessDatabase database = new AccessDatabase(path);
        SessionInfo student = new SessionInfo(
                "token", "U-STUDENT-001", "20260006", "周一", Role.USER);
        AccessShopCatalog firstCatalog = new AccessShopCatalog(database);
        AccessCampusCardStore firstCards = new AccessCampusCardStore(database);
        AccessShoppingCartStore firstCart = new AccessShoppingCartStore(database, firstCatalog);

        firstCards.recharge(student, 500);
        firstCart.setQuantity(student.getUserId(), 1L, 2);

        AccessShopCatalog restartedCatalog = new AccessShopCatalog(new AccessDatabase(path));
        AccessCampusCardStore restartedCards = new AccessCampusCardStore(new AccessDatabase(path));
        ShoppingCartView cart = new AccessShoppingCartStore(
                new AccessDatabase(path), restartedCatalog).view(student.getUserId());
        assertEquals(10_500, restartedCards.view(student).getBalanceFen());
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
    }

    @Test
    void seedPhotosRoundTripAsDecodableJpeg() throws Exception {
        Path path = directory.resolve("shop-photos.accdb");
        AccessShopCatalog catalog = new AccessShopCatalog(new AccessDatabase(path));
        var water = catalog.listOnSale(ListProductsRequest.allOnSale()).stream()
                .filter(item -> item.getProductId() == 8L)
                .findFirst()
                .orElseThrow();
        byte[] cover = water.getCoverPhoto();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(cover));
        assertNotNull(image);
        assertTrue(image.getWidth() >= 200);
        assertEquals((byte) 0xFF, cover[0]);
        assertEquals((byte) 0xD8, cover[1]);
    }
}
