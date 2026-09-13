package edu.seu.vcampus.client;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.card.CardActions;
import edu.seu.vcampus.common.card.CardTransferRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.shop.CreateOrderRequest;
import edu.seu.vcampus.common.shop.OrderLineRequest;
import edu.seu.vcampus.common.shop.ShopActions;
import edu.seu.vcampus.common.shop.ShopPaymentMethods;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.VirtualCampusRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampusCardGatewayIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shopHospitalAndLibraryShareAccessBackedCardPort() throws Exception {
        Path databasePath = temporaryDirectory.resolve("campus-card-gateway.accdb");
        try (VirtualCampusRuntime runtime = VirtualCampusRuntime.start(0, 0, databasePath)) {
            ClientContext student = login("20260001", runtime.campusPort(), runtime.cardPort());
            CampusCardView opened = assertInstanceOf(
                    CampusCardView.class, student.sendCard(CardActions.GET, null).getData());
            assertEquals("20260001", opened.getCardNo());
            assertEquals(10_000, opened.getBalanceFen());

            assertTrue(student.send(
                    ShopActions.CREATE_ORDER,
                    new CreateOrderRequest(
                            List.of(new OrderLineRequest(8, 1)),
                            ShopPaymentMethods.CAMPUS_CARD,
                            "校内自提")).isSuccess());
            assertEquals(9_800, balance(student));

            assertTrue(student.sendCard(
                    CardActions.DEBIT,
                    new CardTransferRequest(1_800, ModuleNames.HOSPITAL, "bill:demo-1")).isSuccess());
            assertEquals(8_000, balance(student));

            SessionInfo session = student.currentSession().orElseThrow();
            edu.seu.vcampus.server.module.library.LibraryServerModule library =
                    edu.seu.vcampus.server.module.library.LibraryServerModule.createAccessBacked(
                            databasePath,
                            new edu.seu.vcampus.server.module.card.CampusCardTcpClient(
                                    "127.0.0.1", runtime.cardPort()));
            library.settleFee(session, 200, "overdue:demo-1");
            assertEquals(7_800, balance(student));
        }
    }

    @Test
    void cardPortRejectsUnknownCampusActions() throws Exception {
        Path databasePath = temporaryDirectory.resolve("card-only.accdb");
        try (VirtualCampusRuntime runtime = VirtualCampusRuntime.start(0, 0, databasePath)) {
            ClientContext student = login("20260001", runtime.campusPort(), runtime.cardPort());
            var response = student.sendCard(ShopActions.LIST_PRODUCTS, null);
            assertEquals(ErrorCodes.COMMON_UNKNOWN_ACTION, response.getCode());
        }
    }

    private static int balance(ClientContext context) throws Exception {
        CampusCardView card = assertInstanceOf(
                CampusCardView.class, context.sendCard(CardActions.GET, null).getData());
        return card.getBalanceFen();
    }

    private static ClientContext login(String username, int campusPort, int cardPort) throws Exception {
        ClientContext context = new ClientContext(
                new CampusClient("127.0.0.1", campusPort),
                new CampusClient("127.0.0.1", cardPort));
        assertTrue(context.login(username, "123456".toCharArray()).isSuccess());
        return context;
    }
}
