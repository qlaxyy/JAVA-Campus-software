package edu.seu.vcampus.server;

import edu.seu.vcampus.server.infrastructure.CampusServer;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import edu.seu.vcampus.server.module.ServerContext;
import edu.seu.vcampus.server.module.ServerModules;
import edu.seu.vcampus.server.module.card.AccessCampusCardStore;
import edu.seu.vcampus.server.module.card.CampusCardTcpClient;
import edu.seu.vcampus.server.module.card.CampusCardWallet;
import edu.seu.vcampus.server.module.course.CourseServerModule;
import edu.seu.vcampus.server.module.user.InMemoryAuthenticationService;
import edu.seu.vcampus.server.module.user.UserAuthenticationBootstrap;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Runs the campus server on one TCP port and the campus-card gateway on another.
 */
public final class VirtualCampusRuntime implements AutoCloseable {

    public static final int DEFAULT_CAMPUS_PORT = 8888;
    public static final int DEFAULT_CARD_PORT = 8889;

    private final CampusServer campusServer;
    private final CampusServer cardServer;

    private VirtualCampusRuntime(CampusServer campusServer, CampusServer cardServer) {
        this.campusServer = campusServer;
        this.cardServer = cardServer;
    }

    /**
     * Starts both listeners with Access persistence.
     *
     * @param campusPort campus services, or 0 for an ephemeral test port
     * @param cardPort campus-card gateway, or 0 for an ephemeral test port
     * @param databasePath shared Access file
     * @return started runtime
     * @throws IOException when a listening socket cannot be opened
     */
    public static VirtualCampusRuntime start(
            int campusPort, int cardPort, Path databasePath) throws IOException {
        InMemoryAuthenticationService authentication =
                UserAuthenticationBootstrap.createAccessBacked(databasePath);
        CourseServerModule courseModule = CourseServerModule.createAccessBacked(databasePath);
        ServerContext context = ServerModules.context(authentication, courseModule);
        CampusCardWallet wallet = new AccessCampusCardStore(new AccessDatabase(databasePath));
        CampusServer cardServer = new CampusServer(
                cardPort, ServerModules.createCardRouter(wallet, context));
        cardServer.start();
        CampusCardWallet gateway = new CampusCardTcpClient("127.0.0.1", cardServer.getPort());
        CampusServer campusServer = new CampusServer(
                campusPort,
                ServerModules.createAccessCampusRouter(
                        databasePath, authentication, courseModule, gateway));
        try {
            campusServer.start();
        } catch (IOException exception) {
            cardServer.close();
            throw exception;
        }
        return new VirtualCampusRuntime(campusServer, cardServer);
    }

    public int campusPort() {
        return campusServer.getPort();
    }

    public int cardPort() {
        return cardServer.getPort();
    }

    public void awaitTermination() throws InterruptedException {
        campusServer.awaitTermination();
    }

    @Override
    public void close() {
        campusServer.close();
        cardServer.close();
    }
}
