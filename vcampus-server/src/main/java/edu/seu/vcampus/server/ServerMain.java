package edu.seu.vcampus.server;

import edu.seu.vcampus.server.infrastructure.CampusServer;
import edu.seu.vcampus.server.module.ServerModules;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

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
        System.out.println("The server is listening on all available network interfaces.");
        printClientConnectionAddresses(server.getPort());
        System.out.printf("User accounts are stored in %s.%n",
                databasePath.toAbsolutePath().normalize());
        server.awaitTermination();
    }

    private static void printClientConnectionAddresses(int port) {
        try {
            List<ConnectionAddress> addresses = new ArrayList<>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                Enumeration<java.net.InetAddress> interfaceAddresses = networkInterface.getInetAddresses();
                while (interfaceAddresses.hasMoreElements()) {
                    java.net.InetAddress address = interfaceAddresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        addresses.add(new ConnectionAddress(
                                networkInterface.getDisplayName(), address.getHostAddress()));
                    }
                }
            }
            addresses.sort(Comparator.comparing(ConnectionAddress::isRadmin).reversed()
                    .thenComparing(ConnectionAddress::interfaceName));
            for (ConnectionAddress address : addresses) {
                String recommendation = address.isRadmin() ? " [Radmin VPN - recommended]" : "";
                System.out.printf("Client address%s: %s:%d (%s)%n", recommendation,
                        address.host(), port, address.interfaceName());
            }
        } catch (SocketException exception) {
            System.out.println("Unable to list client addresses; read the Radmin VPN IPv4 address manually.");
        }
    }

    private record ConnectionAddress(String interfaceName, String host) {

        private boolean isRadmin() {
            return interfaceName.toLowerCase(Locale.ROOT).contains("radmin");
        }
    }
}
