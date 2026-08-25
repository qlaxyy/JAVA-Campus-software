package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.client.module.ClientModules;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * First shared window used to verify that Swing and the socket link work.
 */
public final class MainFrame extends JFrame {

    private final CampusClient client;
    private final JLabel statusLabel = new JLabel("服务器尚未检测", SwingConstants.CENTER);
    private final JButton pingButton = new JButton("测试服务器连接");

    /**
     * Creates the first shared client window.
     *
     * @param client network client used by the connectivity button
     */
    public MainFrame(CampusClient client) {
        super("虚拟校园系统");
        this.client = client;
        initializeWindow();
    }

    private void initializeWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 560));
        setLocationByPlatform(true);

        JLabel titleLabel = new JLabel("虚拟校园系统", SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24F));

        pingButton.addActionListener(event -> pingServer());
        JPanel statusPanel = new JPanel(new BorderLayout(16, 0));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(pingButton, BorderLayout.EAST);

        List<ClientModule> modules = ClientModules.all();
        CardLayout cardLayout = new CardLayout();
        JPanel contentPanel = new JPanel(cardLayout);
        JPanel navigationPanel = createNavigation(modules, contentPanel, cardLayout);

        add(titleLabel, BorderLayout.NORTH);
        add(navigationPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createNavigation(
            List<ClientModule> modules,
            JPanel contentPanel,
            CardLayout cardLayout) {
        JPanel navigation = new JPanel();
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        navigation.setPreferredSize(new Dimension(180, 0));

        for (ClientModule module : modules) {
            JButton button = new JButton(module.displayName());
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            button.addActionListener(event -> cardLayout.show(contentPanel, module.id()));
            navigation.add(button);
            navigation.add(Box.createVerticalStrut(8));
            contentPanel.add(module.createView(client), module.id());
        }
        if (!modules.isEmpty()) {
            cardLayout.show(contentPanel, modules.getFirst().id());
        }
        return navigation;
    }

    private void pingServer() {
        pingButton.setEnabled(false);
        statusLabel.setText("正在连接服务器……");

        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return client.ping();
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        statusLabel.setText("连接成功：" + response.getData());
                    } else {
                        statusLabel.setText("服务器拒绝请求：" + response.getCode());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("连接操作已中断");
                } catch (ExecutionException exception) {
                    statusLabel.setText("连接失败，请确认服务器已经启动");
                } finally {
                    pingButton.setEnabled(true);
                }
            }
        }.execute();
    }
}
