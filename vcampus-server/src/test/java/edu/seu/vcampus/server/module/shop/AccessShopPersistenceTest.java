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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

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
    void existingUserRosterWinsOverReassignedDemoCardNumbers() throws Exception {
        Path path = directory.resolve("legacy-users.accdb");
        AccessDatabase database = new AccessDatabase(path);
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE tblUser ("
                    + "userId TEXT(64) PRIMARY KEY, username TEXT(8) NOT NULL)");
            statement.executeUpdate("CREATE TABLE tblCampusCard ("
                    + "userId TEXT(64) PRIMARY KEY, campusCardNumber TEXT(8) NOT NULL, "
                    + "balanceFen LONG NOT NULL)");
            statement.executeUpdate("CREATE UNIQUE INDEX ux_tblCampusCard_number "
                    + "ON tblCampusCard (campusCardNumber)");
            statement.executeUpdate("INSERT INTO tblUser (userId, username) "
                    + "VALUES ('U-HOSPITAL-ADMIN-001', '20260007')");
            statement.executeUpdate("INSERT INTO tblUser (userId, username) "
                    + "VALUES ('U-CURRENT-STUDENT', '20260009')");
            statement.executeUpdate("INSERT INTO tblCampusCard "
                    + "(userId, campusCardNumber, balanceFen) VALUES "
                    + "('U-HOSPITAL-ADMIN-001', '20260007', 4321)");
        }

        AccessCampusCardStore first = new AccessCampusCardStore(database);
        assertEquals(4321, first.view(session(
                "U-HOSPITAL-ADMIN-001", "20260007", "医院管理员")).getBalanceFen());
        assertEquals(10_000, first.view(session(
                "U-CURRENT-STUDENT", "20260009", "当前学生")).getBalanceFen());

        AccessCampusCardStore restarted = new AccessCampusCardStore(
                new AccessDatabase(path));
        assertEquals(4321, restarted.view(session(
                "U-HOSPITAL-ADMIN-001", "20260007", "医院管理员")).getBalanceFen());
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT COUNT(*) FROM tblCampusCard "
                             + "WHERE userId = 'U-STUDENT-002'")) {
            result.next();
            assertEquals(0, result.getInt(1));
        }
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

    private static SessionInfo session(String userId, String card, String name) {
        return new SessionInfo("token-" + card, userId, card, name, Role.USER);
    }
}
