package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
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
        setMinimumSize(new Dimension(520, 300));
        setLocationByPlatform(true);

        JLabel titleLabel = new JLabel("虚拟校园系统公共骨架", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24F));

        statusLabel.setFont(statusLabel.getFont().deriveFont(16F));
        pingButton.addActionListener(event -> pingServer());

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 16));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(32, 64, 32, 64));
        centerPanel.add(statusLabel);
        centerPanel.add(pingButton);

        add(titleLabel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
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
