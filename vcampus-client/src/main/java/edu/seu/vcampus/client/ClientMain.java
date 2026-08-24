package edu.seu.vcampus.client;

import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.client.view.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Starts the Swing desktop client.
 */
public final class ClientMain {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8888;

    private ClientMain() {
    }

    /**
     * Opens the Swing client for localhost:8888 or optional host and port arguments.
     *
     * @param args optional host followed by optional port
     */
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        useSystemLookAndFeel();
        SwingUtilities.invokeLater(() -> {
            CampusClient client = new CampusClient(host, port);
            MainFrame mainFrame = new MainFrame(client);
            mainFrame.setVisible(true);
        });
    }

    private static void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException exception) {
            System.err.println("Unable to use system look and feel: " + exception.getMessage());
        }
    }
}
