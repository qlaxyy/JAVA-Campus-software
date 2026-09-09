package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.CourseAdminStatisticsInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 教务端选课数据统计页面。
 */
final class CourseAdminStatisticsPanel
    extends JPanel {

    private final ClientContext context;

    private final JComboBox<BatchChoice>
        batchBox =
        new JComboBox<>();

    private final JPanel statisticsPanel =
        new JPanel(
            new GridLayout(
                2,
                4,
                14,
                14));

    private final JLabel statusLabel =
        new JLabel(
            "正在加载选课批次……");

    CourseAdminStatisticsPanel(
        ClientContext context) {

        this.context =
            context;

        initialiseView();
        loadBatches();
    }

    /**
     * 初始化页面。
     */
    private void initialiseView() {

        setLayout(
            new BorderLayout(
                0,
                18));

        setBorder(
            BorderFactory.createEmptyBorder(
                18,
                18,
                18,
                18));

        setBackground(
            CourseTheme.BACKGROUND);

        JPanel header =
            new JPanel(
                new BorderLayout(
                    0,
                    12));

        header.setOpaque(
            false);

        JPanel titlePanel =
            new JPanel(
                new GridLayout(
                    0,
                    1,
                    0,
                    4));

        titlePanel.setOpaque(
            false);

        titlePanel.add(
            CourseTheme.title(
                "数据统计"));

        titlePanel.add(
            CourseTheme.subtitle(
                "查看指定选课批次的课程容量与选课情况"));

        header.add(
            titlePanel,
            BorderLayout.NORTH);

        JPanel toolbar =
            new JPanel(
                new FlowLayout(
                    FlowLayout.LEFT,
                    10,
                    0));

        toolbar.setOpaque(
            false);

        toolbar.add(
            new JLabel(
                "选课批次："));

        batchBox.setPrototypeDisplayValue(
            new BatchChoice(
                -1,
                "2026-2027-1 第一轮选课"));

        toolbar.add(
            batchBox);

        JButton refreshButton =
            CourseTheme.quietButton(
                "刷新统计");

        toolbar.add(
            refreshButton);

        header.add(
            toolbar,
            BorderLayout.CENTER);

        add(
            header,
            BorderLayout.NORTH);

        statisticsPanel.setOpaque(
            false);

        add(
            statisticsPanel,
            BorderLayout.CENTER);

        statusLabel.setForeground(
            CourseTheme.MUTED);

        add(
            statusLabel,
            BorderLayout.SOUTH);

        batchBox.addActionListener(
            event ->
                loadStatistics());

        refreshButton.addActionListener(
            event ->
                loadStatistics());

        showEmptyStatistics();
    }

    /**
     * 加载选课批次。
     */
    private void loadBatches() {

        batchBox.setEnabled(
            false);

        statusLabel.setText(
            "正在加载选课批次……");

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

                        Response response =
                            get();

                        if (!response.isSuccess()) {

                            showError(
                                response.getMessage());

                            return;
                        }

                        List<SelectionBatchInfo> batches =
                            readBatches(
                                response);

                        batchBox.removeAllItems();

                        for (SelectionBatchInfo batch
                            : batches) {

                            batchBox.addItem(
                                new BatchChoice(
                                    batch.getBatchId(),
                                    batch.getSemester()
                                        + " "
                                        + batch.getBatchName()));
                        }

                        batchBox.setEnabled(
                            true);

                        if (batches.isEmpty()) {

                            showEmptyStatistics();

                            statusLabel.setText(
                                "当前没有选课批次。");

                        } else {

                            batchBox.setSelectedIndex(
                                0);

                            loadStatistics();
                        }

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载选课批次被中断。");

                    } catch (ExecutionException
                             | IllegalStateException exception) {

                        Throwable cause =
                            exception instanceof
                                ExecutionException
                                ? exception.getCause()
                                : exception;

                        showError(
                            "无法加载选课批次："
                                + messageOf(
                                cause));
                    }
                }
            };

        worker.execute();
    }

    /**
     * 加载统计数据。
     */
    private void loadStatistics() {

        BatchChoice batch =
            (BatchChoice)
                batchBox.getSelectedItem();

        if (batch == null
            || batch.batchId() < 0) {

            return;
        }

        statusLabel.setText(
            "正在统计选课数据……");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .ADMIN_GET_STATISTICS,
                        new BatchRequest(
                            batch.batchId()));
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        if (!response.isSuccess()) {

                            showError(
                                response.getMessage());

                            return;
                        }

                        if (!(response.getData()
                            instanceof
                            CourseAdminStatisticsInfo
                                statistics)) {

                            throw new IllegalStateException(
                                "服务器返回的统计数据格式错误。");
                        }

                        renderStatistics(
                            statistics);

                        statusLabel.setText(
                            "统计数据加载成功。");

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载统计数据被中断。");

                    } catch (ExecutionException
                             | IllegalStateException exception) {

                        Throwable cause =
                            exception instanceof
                                ExecutionException
                                ? exception.getCause()
                                : exception;

                        showError(
                            "无法加载统计数据："
                                + messageOf(
                                cause));
                    }
                }
            };

        worker.execute();
    }

    /**
     * 显示统计卡片。
     */
    private void renderStatistics(
        CourseAdminStatisticsInfo statistics) {

        statisticsPanel.removeAll();

        statisticsPanel.add(
            createCard(
                "课程数量",
                String.valueOf(
                    statistics.getCourseCount()),
                "门"));

        statisticsPanel.add(
            createCard(
                "教学班数量",
                String.valueOf(
                    statistics.getOfferingCount()),
                "个"));

        statisticsPanel.add(
            createCard(
                "已选总人次",
                String.valueOf(
                    statistics.getSelectedCount()),
                "人次"));

        statisticsPanel.add(
            createCard(
                "总容量",
                String.valueOf(
                    statistics.getTotalCapacity()),
                "人"));

        statisticsPanel.add(
            createCard(
                "剩余名额",
                String.valueOf(
                    statistics.getRemainingCount()),
                "人"));

        statisticsPanel.add(
            createCard(
                "已满教学班",
                String.valueOf(
                    statistics.getFullOfferingCount()),
                "个"));

        statisticsPanel.add(
            createCard(
                "已关闭教学班",
                String.valueOf(
                    statistics.getClosedOfferingCount()),
                "个"));

        statisticsPanel.add(
            createCard(
                "平均选课率",
                String.format(
                    "%.1f%%",
                    statistics.getSelectionRate()),
                "已选人数 / 总容量"));

        statisticsPanel.revalidate();
        statisticsPanel.repaint();
    }

    /**
     * 创建一张统计卡片。
     */
    private JPanel createCard(
        String title,
        String value,
        String unit) {

        CourseTheme.SurfacePanel card =
            new CourseTheme.SurfacePanel();

        card.setLayout(
            new BorderLayout(
                0,
                10));

        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    CourseTheme.BORDER),
                BorderFactory.createEmptyBorder(
                    20,
                    18,
                    20,
                    18)));

        JLabel titleLabel =
            new JLabel(
                title,
                SwingConstants.CENTER);

        titleLabel.setForeground(
            CourseTheme.MUTED);

        titleLabel.setFont(
            titleLabel.getFont()
                .deriveFont(
                    Font.BOLD,
                    14F));

        JLabel valueLabel =
            new JLabel(
                value,
                SwingConstants.CENTER);

        valueLabel.setForeground(
            CourseTheme.PRIMARY_DARK);

        valueLabel.setFont(
            valueLabel.getFont()
                .deriveFont(
                    Font.BOLD,
                    30F));

        JLabel unitLabel =
            new JLabel(
                unit,
                SwingConstants.CENTER);

        unitLabel.setForeground(
            CourseTheme.MUTED);

        card.add(
            titleLabel,
            BorderLayout.NORTH);

        card.add(
            valueLabel,
            BorderLayout.CENTER);

        card.add(
            unitLabel,
            BorderLayout.SOUTH);

        return card;
    }

    /**
     * 未加载时显示空值。
     */
    private void showEmptyStatistics() {

        statisticsPanel.removeAll();

        String[] titles = {
            "课程数量",
            "教学班数量",
            "已选总人次",
            "总容量",
            "剩余名额",
            "已满教学班",
            "已关闭教学班",
            "平均选课率"
        };

        for (String title : titles) {

            statisticsPanel.add(
                createCard(
                    title,
                    "--",
                    "暂无数据"));
        }

        statisticsPanel.revalidate();
        statisticsPanel.repaint();
    }

    private List<SelectionBatchInfo> readBatches(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的批次数据格式错误。");
        }

        List<SelectionBatchInfo> result =
            new ArrayList<>();

        for (Object value : values) {

            if (!(value
                instanceof SelectionBatchInfo batch)) {

                throw new IllegalStateException(
                    "服务器返回的批次数据格式错误。");
            }

            result.add(
                batch);
        }

        return result;
    }

    private void showError(
        String message) {

        statusLabel.setText(
            message);

        JOptionPane.showMessageDialog(
            this,
            message,
            "数据统计",
            JOptionPane.ERROR_MESSAGE);
    }

    private String messageOf(
        Throwable throwable) {

        if (throwable == null
            || throwable.getMessage() == null) {

            return "未知错误";
        }

        return throwable.getMessage();
    }

    private record BatchChoice(
        long batchId,
        String text) {

        @Override
        public String toString() {

            return text;
        }
    }
}
