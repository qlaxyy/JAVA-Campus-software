package edu.seu.vcampus.server.infrastructure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CampusServerTest {

    @Test
    void listensOnAllAvailableNetworkInterfaces() throws Exception {
        try (CampusServer server = new CampusServer(0, 1)) {
            server.start();

            assertTrue(server.getBindAddress().isAnyLocalAddress());
        }
    }
}
