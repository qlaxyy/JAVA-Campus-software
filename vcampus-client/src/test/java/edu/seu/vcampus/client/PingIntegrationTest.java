package edu.seu.vcampus.client;

import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PingIntegrationTest {

    @Test
    void clientReceivesPongFromRunningServer() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            CampusClient client = new CampusClient("127.0.0.1", server.getPort());

            Response response = client.ping();

            assertTrue(response.isSuccess());
            assertEquals(ErrorCodes.SUCCESS, response.getCode());
            assertEquals("PONG", response.getData());
        }
    }

    @Test
    void serverRejectsUnknownAction() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            CampusClient client = new CampusClient("127.0.0.1", server.getPort());
            Request request = Request.create("COMMON.NOT_SUPPORTED", null, null);

            Response response = client.send(request);

            assertFalse(response.isSuccess());
            assertEquals(ErrorCodes.COMMON_UNKNOWN_ACTION, response.getCode());
        }
    }
}
