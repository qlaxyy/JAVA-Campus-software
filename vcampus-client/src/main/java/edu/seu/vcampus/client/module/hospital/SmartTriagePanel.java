package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.*;
import edu.seu.vcampus.common.protocol.Response;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** A continuous conversation with a pinned composer and reviewable department guidance. */
final class SmartTriagePanel extends JPanel {
    private final ClientContext context;
    private final Consumer<String> openDepartmentSlots;
    private final List<TriageChatMessage> transcript = new ArrayList<>();
    private final List<TriageChatResult> results = new ArrayList<>();
    private final JPanel timeline = new JPanel();
    private final JPanel currentActions = new JPanel(new BorderLayout(0, 8));
    private final JTextArea input = new JTextArea(3, 0);
    private final JTextArea status = text("", HospitalTheme.MUTED);
    private final JButton send = HospitalTheme.primaryButton("发送");
    private final JButton edit = HospitalTheme.quietButton("修改上一条");
    private final JScrollPane scroll;
    private int version;
    private boolean busy;
    private TriageChatResult latest;

    SmartTriagePanel(ClientContext context, Runnable goBack, Consumer<String> openDepartmentSlots) {
        this.context = context;
        this.openDepartmentSlots = openDepartmentSlots;
        setLayout(new BorderLayout(0, 12));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        add(HospitalResponsiveLayout.constrainWidth(HospitalPageHeader.create(
                "智能导诊",
                "请描述自己的不适；症状严重或迅速加重时，请及时寻求线下医护帮助。",
                "医院首页",
                () -> { version++; goBack.run(); },
                null)), BorderLayout.NORTH);

        timeline.setOpaque(false);
        timeline.setLayout(new BoxLayout(timeline, BoxLayout.Y_AXIS));
        timeline.addComponentListener(new java.awt.event.ComponentAdapter() {
            private int previousWidth;
            @Override public void componentResized(java.awt.event.ComponentEvent event) {
                if (timeline.getWidth() != previousWidth) {
                    previousWidth = timeline.getWidth();
                    invalidateTree(timeline);
                    timeline.revalidate();
                }
            }
        });
        scroll = HospitalResponsiveLayout.verticalScroll(timeline);
        scroll.setName("triageConversationScroll");
        add(scroll, BorderLayout.CENTER);

        JPanel composer = new JPanel(new BorderLayout(0, 8));
        composer.setOpaque(false);
        composer.setName("triageComposer");
        input.setName("triageDescription");
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setFont(HospitalTheme.uiFont(Font.PLAIN, 15F));
        input.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        input.getAccessibleContext().setAccessibleName("描述、补充或纠正你的情况");
        composer.add(new JScrollPane(input), BorderLayout.CENTER);
        JPanel controls = HospitalResponsiveLayout.grid(3, 130, 8, 8);
        send.setName("submitTriageButton");
        edit.setName("editTriageMessageButton");
        JButton reset = HospitalTheme.quietButton("重新开始");
        reset.setName("resetTriageButton");
        reset.addActionListener(event -> activate());
        send.addActionListener(event -> submit(null, null));
        edit.addActionListener(event -> editLast());
        controls.add(edit);
        controls.add(reset);
        controls.add(send);
        JPanel footer = new JPanel(new BorderLayout(0, 4));
        footer.setOpaque(false);
        footer.add(status, BorderLayout.NORTH);
        footer.add(controls, BorderLayout.SOUTH);
        composer.add(footer, BorderLayout.SOUTH);
        add(HospitalResponsiveLayout.constrainWidth(composer, 900), BorderLayout.SOUTH);
        input.getInputMap().put(KeyStroke.getKeyStroke("ctrl ENTER"), "send");
        input.getActionMap().put("send", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                if (!busy) submit(null, null);
            }
        });
        activate();
    }

    void activate() {
        version++;
        transcript.clear();
        results.clear();
        latest = null;
        busy = false;
        input.setText("");
        status.setText("可随时补充或纠正，也可以问我某个问题是什么意思。Ctrl+Enter 发送。");
        rebuild();
        updateEnabled();
    }

    private void submit(String questionId, TriageFollowUpOptionView option) {
        if (busy) return;
        if (option != null && !input.getText().isBlank()) {
            status.setText("输入框中还有补充内容，请先点击“发送”，以免遗漏。");
            input.requestFocusInWindow();
            return;
        }
        String value = option == null ? input.getText().trim() : option.getLabel();
        if (value.isEmpty() || value.length() > 300) {
            status.setText("请填写 1 至 300 个字；较长的描述可以分次补充。");
            return;
        }
        List<TriageChatMessage> proposed = new ArrayList<>(transcript);
        proposed.add(new TriageChatMessage("user", value, questionId,
                option == null ? null : option.getOptionId()));
        final TriageChatRequest request;
        try {
            request = new TriageChatRequest(proposed);
        } catch (IllegalArgumentException exception) {
            status.setText("本次对话较长，请保存需要的信息后重新开始，或直接咨询线下导诊。");
            return;
        }
        int currentVersion = ++version;
        var owner = context.currentSession();
        busy = true;
        // Old advice becomes unavailable as soon as new information is submitted.
        currentActions.removeAll();
        currentActions.revalidate();
        currentActions.repaint();
        status.setText("正在阅读你的补充……");
        updateEnabled();
        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
                if (!Objects.equals(owner, context.currentSession())) {
                    throw new IllegalStateException("session changed");
                }
                return context.send(HospitalActions.GET_TRIAGE_RECOMMENDATION, request, 15_000);
            }
            @Override protected void done() {
                if (currentVersion != version) return;
                if (!Objects.equals(owner, context.currentSession())) {
                    activate();
                    return;
                }
                busy = false;
                try {
                    Response response = get();
                    if (response.isSuccess() && response.getData() instanceof TriageChatResult result) {
                        transcript.clear();
                        transcript.addAll(proposed);
                        String assistant = result.reply();
                        if (result.guidance().hasStructuredFollowUp()) {
                            assistant += "\n" + result.guidance().getFollowUpQuestion();
                        }
                        transcript.add(new TriageChatMessage("assistant", assistant));
                        results.add(result);
                        latest = result;
                        input.setText("");
                        status.setText("你可以继续补充，或修改上一条描述。");
                        rebuild();
                    } else {
                        status.setText("这条消息没有发送成功，请重试。");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    status.setText("对话已中断，输入内容已保留。");
                } catch (ExecutionException exception) {
                    status.setText("暂时无法取得回复，请重试。输入内容已保留。");
                }
                updateEnabled();
                input.requestFocusInWindow();
            }
        }.execute();
    }

    private void editLast() {
        if (busy || transcript.size() < 2) return;
        editMessage(transcript.size() - 2);
    }

    private void editMessage(int index) {
        if (busy || index < 0 || index >= transcript.size()) return;
        version++;
        TriageChatMessage previous = transcript.get(index);
        transcript.subList(index, transcript.size()).clear();
        results.subList(index / 2, results.size()).clear();
        latest = results.isEmpty() ? null : results.getLast();
        input.setText(previous.text());
        rebuild();
        // Advice for the old wording must stay hidden while a correction is being drafted.
        currentActions.removeAll();
        status.setText("修改后发送，将根据更新后的对话重新评估。");
        updateEnabled();
    }

    private void rebuild() {
        timeline.removeAll();
        addMessage("导诊助手", "请描述你现在最主要的不适。可以补充出现部位、"
                + "持续时间和变化情况；我会在必要时进一步确认，并帮助你选择就诊科室。",
                false, -1);
        for (int i = 0; i < transcript.size(); i++) {
            TriageChatMessage message = transcript.get(i);
            addMessage("user".equals(message.role()) ? "你" : "导诊助手",
                    message.text(), "user".equals(message.role()), i);
        }
        currentActions.removeAll();
        currentActions.setName("triageCurrentActions");
        currentActions.setOpaque(false);
        if (latest != null) showActions(latest);
        timeline.add(HospitalResponsiveLayout.constrainWidth(currentActions, 860));
        timeline.add(Box.createVerticalGlue());
        timeline.revalidate();
        timeline.repaint();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar()
                .setValue(scroll.getVerticalScrollBar().getMaximum()));
    }

    private void addMessage(String speaker, String content, boolean patient, int index) {
        JPanel card = new HospitalTheme.SurfacePanel(
                patient ? HospitalTheme.PRIMARY_LIGHT : HospitalTheme.SURFACE,
                14, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JLabel label = new JLabel(speaker);
        label.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        label.setForeground(HospitalTheme.PRIMARY_DARK);
        JPanel messageHeader = new JPanel(new BorderLayout());
        messageHeader.setOpaque(false);
        messageHeader.add(label, BorderLayout.CENTER);
        if (patient) {
            JButton correct = HospitalTheme.quietButton("修改这条");
            correct.setName("editTriageHistoryButton");
            correct.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            correct.addActionListener(event -> editMessage(index));
            messageHeader.add(correct, BorderLayout.EAST);
        }
        card.add(messageHeader, BorderLayout.NORTH);
        JTextArea body = text(content, HospitalTheme.TEXT);
        body.setName(patient ? "triagePatientMessage" : "triageAssistantMessage");
        card.add(body, BorderLayout.CENTER);
        timeline.add(HospitalResponsiveLayout.constrainWidth(card, 860));
        timeline.add(Box.createVerticalStrut(10));
    }

    private void showActions(TriageChatResult result) {
        TriageResultView guidance = result.guidance();
        if (guidance.isUrgent()) {
            JLabel warning = new JLabel("请先寻求线下帮助");
            warning.setForeground(HospitalTheme.WARNING);
            warning.setFont(HospitalTheme.uiFont(Font.BOLD, 18F));
            currentActions.add(warning, BorderLayout.NORTH);
            return;
        }
        if (guidance.hasStructuredFollowUp()) {
            JPanel choices = HospitalResponsiveLayout.grid(2, 250, 8, 8);
            for (TriageFollowUpOptionView option : guidance.getFollowUpOptions()) {
                JButton choice = HospitalTheme.quietButton(option.getLabel());
                choice.setName("triageFollowUpOptionButton");
                choice.setActionCommand(option.getOptionId());
                choice.addActionListener(event -> submit(guidance.getFollowUpQuestionId(), option));
                choices.add(choice);
            }
            currentActions.add(choices, BorderLayout.CENTER);
            return;
        }
        JPanel review = new JPanel(new BorderLayout(0, 10));
        review.setOpaque(false);
        if (!result.summary().isBlank()) {
            JTextArea summary = text("请核对我理解的情况\n" + result.summary(), HospitalTheme.TEXT);
            summary.setName("triageSummary");
            review.add(summary, BorderLayout.NORTH);
        }
        if (!guidance.getRecommendations().isEmpty()) {
            JButton confirm = HospitalTheme.primaryButton("描述无误，查看科室");
            confirm.setName("confirmTriageSummaryButton");
            confirm.addActionListener(event -> {
                if (!busy && latest == result) {
                    review.remove(confirm);
                    JPanel recommendations = new JPanel();
                    recommendations.setOpaque(false);
                    recommendations.setLayout(new BoxLayout(recommendations, BoxLayout.Y_AXIS));
                    guidance.getRecommendations().forEach(item -> {
                        JPanel card = new HospitalTheme.SurfacePanel();
                        card.setLayout(new BorderLayout(0, 8));
                        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
                        card.add(text(item.getDepartmentName() + "\n"
                                + String.join("\n", item.getReasons()), HospitalTheme.TEXT), BorderLayout.CENTER);
                        JButton open = HospitalTheme.primaryButton("查看该科室号源");
                        open.setName("openTriageDepartmentButton");
                        open.setActionCommand(item.getDepartmentId());
                        open.addActionListener(action -> {
                            if (!busy && latest == result) openDepartmentSlots.accept(item.getDepartmentId());
                        });
                        card.add(open, BorderLayout.SOUTH);
                        recommendations.add(card);
                        recommendations.add(Box.createVerticalStrut(8));
                    });
                    review.add(recommendations, BorderLayout.CENTER);
                    review.revalidate();
                    review.repaint();
                    SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar()
                            .setValue(scroll.getVerticalScrollBar().getMaximum()));
                }
            });
            review.add(confirm, BorderLayout.CENTER);
        } else if (!result.missingInformation().isEmpty()) {
            review.add(text("还需要了解：" + String.join("；", result.missingInformation()),
                    HospitalTheme.MUTED), BorderLayout.SOUTH);
        }
        currentActions.add(review, BorderLayout.CENTER);
    }

    private void updateEnabled() {
        send.setEnabled(!busy);
        edit.setEnabled(!busy && !transcript.isEmpty());
        input.setEditable(!busy);
        send.setText(busy ? "等待回复……" : "发送");
    }

    private static JTextArea text(String value, Color color) {
        return HospitalResponsiveLayout.wrappingText(value,
                HospitalTheme.uiFont(Font.PLAIN, 14F), color);
    }

    private static void invalidateTree(Container parent) {
        parent.invalidate();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container container) invalidateTree(container);
        }
    }
}
