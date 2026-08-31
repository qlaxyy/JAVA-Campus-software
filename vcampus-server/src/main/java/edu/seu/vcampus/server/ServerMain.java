package edu.seu.vcampus.server;

import edu.seu.vcampus.server.infrastructure.CampusServer;
import edu.seu.vcampus.server.module.ServerModules;

import java.nio.file.Path;

/**
 * Starts the virtual campus server.
 */
public final class ServerMain {

    private static final int DEFAULT_PORT = 8888;
    private static final Path DEFAULT_DATABASE_PATH = Path.of("database", "vCampus.accdb");

    private ServerMain() {
    }

    /**
     * Starts the server using an Access-backed account repository.
     *
     * @param args optional port and database path
     * @throws Exception when the server cannot start or is interrupted
     */
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Path databasePath = args.length > 1 ? Path.of(args[1]) : DEFAULT_DATABASE_PATH;
        CampusServer server = new CampusServer(
                port, ServerModules.createPersistentRouter(databasePath));
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "vcampus-shutdown"));

        server.start();
        System.out.printf("Virtual Campus server started on port %d.%n", server.getPort());
        System.out.printf("User accounts are stored in %s.%n",
                databasePath.toAbsolutePath().normalize());
        server.awaitTermination();
    }
}
