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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * 选课中心首页。
 */
final class CourseCenterPanel
    extends JPanel {

    private static final DateTimeFormatter
        TIME_FORMAT =
        DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm");

    private final ClientContext context;

    private final Consumer<SelectionBatchInfo>
        onEnterBatch;

    private final JPanel batchPanel =
        new JPanel();

    private final JLabel statusLabel =
        new JLabel(
            "正在加载选课批次...");

    private final JButton refreshButton =
        CourseTheme.quietButton(
            "刷新");

    CourseCenterPanel(
        ClientContext context,
        Consumer<SelectionBatchInfo>
            onEnterBatch) {

        this.context = context;
        this.onEnterBatch =
            onEnterBatch;

        initializeView();

        loadBatches();
    }

    /**
     * 初始化页面。
     */
    private void initializeView() {

        setLayout(
            new BorderLayout(
                0,
                18));

        setBackground(
            CourseTheme.BACKGROUND);

        setBorder(
            BorderFactory.createEmptyBorder(
                24,
                28,
                24,
                28));

        /*
         * =========================
         * 页面标题
         * =========================
         */
        JPanel header =
            new JPanel(
                new BorderLayout(
                    20,
                    0));

        header.setOpaque(
            false);

        JPanel titleArea =
            new JPanel();

        titleArea.setOpaque(
            false);

        titleArea.setLayout(
            new BoxLayout(
                titleArea,
                BoxLayout.Y_AXIS));

        JLabel title =
            CourseTheme.title(
                "选课中心");

        JLabel subtitle =
            CourseTheme.subtitle(
                "选择当前学期的选课批次，进入课程选择与调整");

        titleArea.add(
            title);

        titleArea.add(
            Box.createVerticalStrut(
                6));

        titleArea.add(
            subtitle);

        header.add(
            titleArea,
            BorderLayout.CENTER);

        refreshButton.addActionListener(
            event ->
                loadBatches());

        header.add(
            refreshButton,
            BorderLayout.EAST);

        add(
            header,
            BorderLayout.NORTH);

        /*
         * =========================
         * 批次卡片区域
         * =========================
         */
        batchPanel.setLayout(
            new GridLayout(
                0,
                1,
                0,
                14));

        batchPanel.setBackground(
            CourseTheme.BACKGROUND);

        JScrollPane scrollPane =
            new JScrollPane(
                batchPanel);

        scrollPane.setBorder(
            BorderFactory
                .createEmptyBorder());

        scrollPane.setBackground(
            CourseTheme.BACKGROUND);

        scrollPane
            .getViewport()
            .setBackground(
                CourseTheme.BACKGROUND);

        scrollPane
            .getVerticalScrollBar()
            .setUnitIncrement(
                16);

        add(
            scrollPane,
            BorderLayout.CENTER);

        /*
         * =========================
         * 状态文字
         * =========================
         */
        statusLabel.setForeground(
            CourseTheme.MUTED);

        statusLabel.setFont(
            statusLabel
                .getFont()
                .deriveFont(
                    13F));

        statusLabel.setBorder(
            BorderFactory.createEmptyBorder(
                2,
                2,
                0,
                2));

        add(
            statusLabel,
            BorderLayout.SOUTH);
    }

    /**
     * 加载批次。
     */
    private void loadBatches() {

        if (context
            .currentSession()
            .isEmpty()) {

            statusLabel.setText(
                "登录状态已失效，请重新登录。");

            statusLabel.setForeground(
                CourseTheme.DANGER);

            batchPanel.removeAll();
            batchPanel.revalidate();
            batchPanel.repaint();

            return;
        }

        setBusy(true);

        statusLabel.setText(
            "正在加载选课批次...");

        statusLabel.setForeground(
            CourseTheme.MUTED);

        SwingWorker<Response, Void>
            worker =
            new SwingWorker<>() {

                @Override
                protected Response
                doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .LIST_BATCHES,
                        null);
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        if (!response
                            .isSuccess()) {

                            showError(
                                response
                                    .getMessage());

                            return;
                        }

                        List<SelectionBatchInfo>
                            batches =
                            readBatches(
                                response);

                        renderBatches(
                            batches);

                        statusLabel.setText(
                            "共加载 "
                                + batches.size()
                                + " 个选课批次");

                        statusLabel.setForeground(
                            CourseTheme.MUTED);

                    } catch (
                        InterruptedException
                            exception) {

                        Thread
                            .currentThread()
                            .interrupt();

                        showError(
                            "加载选课批次被中断。");

                    } catch (
                        ExecutionException
                            exception) {

                        Throwable cause =
                            exception
                                .getCause();

                        showError(
                            "无法连接服务器："
                                + (cause == null
                                ? exception
                                .getMessage()
                                : cause
                                .getMessage()));

                    } catch (
                        IllegalStateException
                            exception) {

                        showError(
                            exception
                                .getMessage());

                    } finally {

                        setBusy(
                            false);
                    }
                }
            };

        worker.execute();
    }

    /**
     * 解析批次列表。
     */
    private List<SelectionBatchInfo>
    readBatches(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new
                IllegalStateException(
                "服务器返回的选课批次数据格式错误。");
        }

        List<SelectionBatchInfo>
            result =
            new ArrayList<>();

        for (Object value
            : values) {

            if (!(value
                instanceof
                SelectionBatchInfo batch)) {

                throw new
                    IllegalStateException(
                    "服务器返回的选课批次数据格式错误。");
            }

            result.add(
                batch);
        }

        return result;
    }

    /**
     * 显示所有批次。
     */
    private void renderBatches(
        List<SelectionBatchInfo>
            batches) {

        batchPanel.removeAll();

        if (batches.isEmpty()) {

            JLabel empty =
                CourseTheme.subtitle(
                    "当前学期暂无选课批次。");

            batchPanel.add(
                empty);

        } else {

            for (SelectionBatchInfo batch
                : batches) {

                batchPanel.add(
                    createBatchCard(
                        batch));
            }
        }

        batchPanel.revalidate();
        batchPanel.repaint();
    }

    /**
     * 批次卡片。
     */
    private JPanel createBatchCard(
        SelectionBatchInfo batch) {

        CourseTheme.SurfacePanel card =
            new CourseTheme
                .SurfacePanel();

        card.setLayout(
            new BorderLayout(
                20,
                0));

        card.setBorder(
            BorderFactory.createEmptyBorder(
                18,
                20,
                18,
                20));

        card.setPreferredSize(
            new Dimension(
                0,
                130));

        /*
         * =========================
         * 左侧信息
         * =========================
         */
        JPanel information =
            new JPanel();

        information.setOpaque(
            false);

        information.setLayout(
            new BoxLayout(
                information,
                BoxLayout.Y_AXIS));

        JLabel nameLabel =
            new JLabel(
                batch.getBatchName());

        nameLabel.setForeground(
            CourseTheme.TEXT);

        nameLabel.setFont(
            nameLabel
                .getFont()
                .deriveFont(
                    Font.BOLD,
                    18F));

        JLabel semesterLabel =
            createMutedLabel(
                "学期："
                    + batch
                    .getSemester());

        JLabel timeLabel =
            createMutedLabel(
                "开放时间："
                    + TIME_FORMAT
                    .format(
                        batch
                            .getStartTime())
                    + "  ～  "
                    + TIME_FORMAT
                    .format(
                        batch
                            .getEndTime()));

        JLabel stateLabel =
            new JLabel(
                statusText(
                    batch.getStatus()));

        stateLabel.setFont(
            stateLabel
                .getFont()
                .deriveFont(
                    Font.BOLD,
                    13F));

        stateLabel.setForeground(
            statusColor(
                batch.getStatus()));

        information.add(
            nameLabel);

        information.add(
            Box.createVerticalStrut(
                10));

        information.add(
            semesterLabel);

        information.add(
            Box.createVerticalStrut(
                5));

        information.add(
            timeLabel);

        information.add(
            Box.createVerticalStrut(
                7));

        information.add(
            stateLabel);

        card.add(
            information,
            BorderLayout.CENTER);

        /*
         * =========================
         * 右侧按钮
         * =========================
         */
        JButton enterButton;

        if (batch.getStatus()
            == SelectionBatchStatus
            .NOT_STARTED) {

            enterButton =
                CourseTheme
                    .quietButton(
                        "尚未开始");

            enterButton.setEnabled(
                false);

        } else {

            enterButton =
                CourseTheme
                    .primaryButton(
                        batch.getStatus()
                            == SelectionBatchStatus
                            .ENDED
                            ? "查看"
                            : "进入选课");
        }

        enterButton.setPreferredSize(
            new Dimension(
                110,
                42));

        enterButton.addActionListener(
            event ->
                onEnterBatch.accept(
                    batch));

        JPanel actionArea =
            new JPanel(
                new BorderLayout());

        actionArea.setOpaque(
            false);

        actionArea.add(
            enterButton,
            BorderLayout.CENTER);

        card.add(
            actionArea,
            BorderLayout.EAST);

        return card;
    }

    private JLabel createMutedLabel(
        String text) {

        JLabel label =
            new JLabel(text);

        label.setForeground(
            CourseTheme.MUTED);

        label.setFont(
            label.getFont()
                .deriveFont(
                    13F));

        return label;
    }

    /**
     * 批次状态颜色。
     */
    private Color statusColor(
        SelectionBatchStatus status) {

        return switch (status) {

            case OPEN ->
                CourseTheme.SUCCESS;

            case NOT_STARTED ->
                CourseTheme.WARNING;

            case ENDED ->
                CourseTheme.MUTED;
        };
    }

    /**
     * 批次状态中文。
     */
    private String statusText(
        SelectionBatchStatus status) {

        return switch (status) {

            case NOT_STARTED ->
                "● 未开始";

            case OPEN ->
                "● 正在进行";

            case ENDED ->
                "● 已结束";
        };
    }

    private void setBusy(
        boolean busy) {

        refreshButton.setEnabled(
            !busy);
    }

    private void showError(
        String message) {

        batchPanel.removeAll();

        JLabel errorLabel =
            new JLabel(
                "选课批次加载失败。");

        errorLabel.setForeground(
            CourseTheme.DANGER);

        batchPanel.add(
            errorLabel);

        batchPanel.revalidate();
        batchPanel.repaint();

        statusLabel.setText(
            message);

        statusLabel.setForeground(
            CourseTheme.DANGER);
    }
}
