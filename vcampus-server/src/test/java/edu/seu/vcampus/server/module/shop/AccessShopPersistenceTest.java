package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.shop.ShoppingCartView;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
