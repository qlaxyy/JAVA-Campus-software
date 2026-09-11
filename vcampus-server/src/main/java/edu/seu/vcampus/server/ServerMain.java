package edu.seu.vcampus.server;

import edu.seu.vcampus.server.module.user.LocalSuperAdminRecovery;

import java.io.Console;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * Starts the virtual campus server.
 */
public final class ServerMain {

    private static final Path DEFAULT_DATABASE_PATH = Path.of("database", "vCampus.accdb");

    private ServerMain() {
    }

    /**
     * Starts the server using the shared Access-backed production repositories.
     *
     * @param args optional port and database path
     * @throws Exception when the server cannot start or is interrupted
     */
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--reset-super-admin-password".equals(args[0])) {
            resetSuperAdministratorPassword(args);
            return;
        }
        int port = args.length > 0 ? Integer.parseInt(args[0]) : VirtualCampusRuntime.DEFAULT_CAMPUS_PORT;
        Path databasePath = args.length > 1 ? Path.of(args[1]) : DEFAULT_DATABASE_PATH;
        int cardPort = port == 0 ? 0 : port + 1;
        VirtualCampusRuntime runtime = VirtualCampusRuntime.start(port, cardPort, databasePath);
        Runtime.getRuntime().addShutdownHook(new Thread(runtime::close, "vcampus-shutdown"));

        System.out.printf("Virtual Campus server started on port %d.%n", runtime.campusPort());
        System.out.printf("Campus card gateway started on port %d.%n", runtime.cardPort());
        System.out.println("The server is listening on all available network interfaces.");
        printClientConnectionAddresses("Campus service", runtime.campusPort());
        printClientConnectionAddresses("Campus card gateway", runtime.cardPort());
        System.out.printf("Persistent server data is stored in %s.%n",
                databasePath.toAbsolutePath().normalize());
        runtime.awaitTermination();
    }

    private static void resetSuperAdministratorPassword(String[] args) {
        if (args.length > 3) {
            throw new IllegalArgumentException(
                    "Usage: --reset-super-admin-password [campusCardNumber] [databasePath]");
        }
        String campusCardNumber = args.length > 1 ? args[1] : "20260000";
        Path databasePath = args.length > 2 ? Path.of(args[2]) : DEFAULT_DATABASE_PATH;
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException(
                    "无法安全读取密码。请停止服务器后，在 PowerShell 或 CMD 中执行该命令。");
        }

        console.printf("即将重置超级管理员 %s 的密码。%n", campusCardNumber);
        console.printf("数据库：%s%n", databasePath.toAbsolutePath().normalize());
        char[] password = console.readPassword("请输入新密码（至少 6 个字符）：");
        char[] confirmation = console.readPassword("请再次输入新密码：");
        try {
            if (password == null || confirmation == null
                    || !Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("两次输入的密码不一致。");
            }
            LocalSuperAdminRecovery.resetPassword(
                    databasePath, campusCardNumber, password);
            console.printf("密码重置成功。请重新启动服务器并使用新密码登录。%n");
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
            if (confirmation != null) {
                Arrays.fill(confirmation, '\0');
            }
        }
    }

    private static void printClientConnectionAddresses(String role, int port) {
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
                System.out.printf("%s%s: %s:%d (%s)%n", role, recommendation,
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
