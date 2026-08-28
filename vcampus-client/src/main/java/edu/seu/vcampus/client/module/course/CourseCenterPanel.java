package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.SelectionBatchStatus;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 选课中心首页。
 *
 * 当前第一版负责显示当前学期的选课批次。
 */
final class CourseCenterPanel extends JPanel {

    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ClientContext context;

    private final JPanel batchPanel = new JPanel();
    private final JLabel statusLabel = new JLabel("正在加载选课批次...");
    private final JButton refreshButton = new JButton("刷新");

    CourseCenterPanel(ClientContext context) {
        this.context = context;

        initializeView();
        loadBatches();
    }

    /**
     * 初始化界面。
     */
    private void initializeView() {
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(
            20, 24, 20, 24));

        // 顶部标题。
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(
            titlePanel,
            BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("选课中心");
        titleLabel.setFont(
            titleLabel.getFont().deriveFont(
                Font.BOLD,
                24F));

        JLabel subtitleLabel =
            new JLabel("请选择当前学期的选课批次");

        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(subtitleLabel);

        topPanel.add(titlePanel, BorderLayout.WEST);

        refreshButton.addActionListener(
            event -> loadBatches());

        topPanel.add(refreshButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 批次卡片区域。
        batchPanel.setLayout(new GridLayout(
            0,
            1,
            0,
            12));

        JScrollPane scrollPane =
            new JScrollPane(batchPanel);

        scrollPane.setBorder(
            BorderFactory.createEmptyBorder());

        scrollPane.getVerticalScrollBar()
            .setUnitIncrement(12);

        add(scrollPane, BorderLayout.CENTER);

        // 底部状态栏。
        statusLabel.setBorder(
            BorderFactory.createEmptyBorder(
                4, 2, 0, 2));

        add(statusLabel, BorderLayout.SOUTH);
    }

    /**
     * 从服务器加载选课批次。
     */
    private void loadBatches() {
        if (context.currentSession().isEmpty()) {
            statusLabel.setText("登录状态已失效，请重新登录。");
            batchPanel.removeAll();
            batchPanel.revalidate();
            batchPanel.repaint();
            return;
        }

        setBusy(true);
        statusLabel.setText("正在加载选课批次...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions.LIST_BATCHES,
                        null);
                }

                @Override
                protected void done() {
                    try {
                        Response response = get();

                        if (!response.isSuccess()) {
                            showError(response.getMessage());
                            return;
                        }

                        List<SelectionBatchInfo> batches =
                            readBatches(response);

                        renderBatches(batches);

                        statusLabel.setText(
                            "共加载 "
                                + batches.size()
                                + " 个选课批次");

                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        showError("加载选课批次被中断。");

                    } catch (ExecutionException exception) {
                        showError(
                            "无法连接服务器："
                                + exception
                                .getCause()
                                .getMessage());

                    } catch (IllegalStateException exception) {
                        showError(exception.getMessage());

                    } finally {
                        setBusy(false);
                    }
                }
            };

        worker.execute();
    }

    /**
     * 从 Response 中读取批次列表。
     */
    private List<SelectionBatchInfo> readBatches(
        Response response) {

        if (!(response.getData() instanceof List<?> values)) {
            throw new IllegalStateException(
                "服务器返回的选课批次数据格式错误。");
        }

        List<SelectionBatchInfo> result =
            new ArrayList<>();

        for (Object value : values) {

            if (!(value instanceof SelectionBatchInfo batch)) {
                throw new IllegalStateException(
                    "服务器返回的选课批次数据格式错误。");
            }

            result.add(batch);
        }

        return result;
    }

    /**
     * 显示所有批次。
     */
    private void renderBatches(
        List<SelectionBatchInfo> batches) {

        batchPanel.removeAll();

        if (batches.isEmpty()) {
            batchPanel.add(
                new JLabel("当前学期暂无选课批次。"));
        } else {
            for (SelectionBatchInfo batch : batches) {
                batchPanel.add(createBatchCard(batch));
            }
        }

        batchPanel.revalidate();
        batchPanel.repaint();
    }

    /**
     * 创建一个选课批次卡片。
     */
    private JPanel createBatchCard(
        SelectionBatchInfo batch) {

        JPanel card = new JPanel(
            new BorderLayout(16, 10));

        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(
                    16, 18, 16, 18)));

        card.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                140));

        JPanel information = new JPanel();

        information.setLayout(
            new BoxLayout(
                information,
                BoxLayout.Y_AXIS));

        JLabel nameLabel =
            new JLabel(batch.getBatchName());

        nameLabel.setFont(
            nameLabel.getFont().deriveFont(
                Font.BOLD,
                18F));

        JLabel semesterLabel =
            new JLabel(
                "学期：" + batch.getSemester());

        JLabel timeLabel =
            new JLabel(
                "开放时间："
                    + TIME_FORMAT.format(
                    batch.getStartTime())
                    + " ～ "
                    + TIME_FORMAT.format(
                    batch.getEndTime()));

        JLabel stateLabel =
            new JLabel(
                "状态："
                    + statusText(
                    batch.getStatus()));

        information.add(nameLabel);
        information.add(Box.createVerticalStrut(8));
        information.add(semesterLabel);
        information.add(Box.createVerticalStrut(4));
        information.add(timeLabel);
        information.add(Box.createVerticalStrut(4));
        information.add(stateLabel);

        card.add(information, BorderLayout.CENTER);

        JButton enterButton = new JButton("进入");

        /*
         * 未开始批次不能进入。
         *
         * OPEN 和 ENDED 都允许进入：
         * ENDED 后续只允许查看，不允许选退课。
         */
        enterButton.setEnabled(
            batch.getStatus()
                != SelectionBatchStatus.NOT_STARTED);

        enterButton.addActionListener(
            event -> enterBatch(batch));

        card.add(enterButton, BorderLayout.EAST);

        return card;
    }

    /**
     * 进入具体批次。
     *
     * 当前下一层选课页面还没有实现，
     * 所以暂时只验证进入逻辑。
     */
    private void enterBatch(
        SelectionBatchInfo batch) {

        JOptionPane.showMessageDialog(
            this,
            "已进入："
                + batch.getBatchName()
                + "\n\n"
                + "下一步将在这里显示："
                + "\n方案内课程"
                + "\n方案外课程"
                + "\n体育课"
                + "\n通选课"
                + "\n已选课程"
                + "\n全校课程查询",
            "选课批次",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private String statusText(
        SelectionBatchStatus status) {

        return switch (status) {
            case NOT_STARTED -> "未开始";
            case OPEN -> "进行中";
            case ENDED -> "已结束";
        };
    }

    private void setBusy(boolean busy) {
        refreshButton.setEnabled(!busy);
    }

    private void showError(String message) {
        batchPanel.removeAll();

        batchPanel.add(
            new JLabel("选课批次加载失败。"));

        batchPanel.revalidate();
        batchPanel.repaint();

        statusLabel.setText(message);
    }
}
