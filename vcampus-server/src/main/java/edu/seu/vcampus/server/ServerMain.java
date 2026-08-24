package edu.seu.vcampus.server;

import edu.seu.vcampus.server.infrastructure.CampusServer;

/**
 * Starts the virtual campus server.
 */
public final class ServerMain {

    private static final int DEFAULT_PORT = 8888;

    private ServerMain() {
    }

    /**
     * Starts the server on port 8888, or on the first command-line argument.
     *
     * @param args optional port
     * @throws Exception when the server cannot start or is interrupted
     */
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        CampusServer server = new CampusServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "vcampus-shutdown"));

        server.start();
        System.out.printf("Virtual Campus server started on port %d.%n", server.getPort());
        server.awaitTermination();
    }
}
