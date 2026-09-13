package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.TriageFollowUpAnswer;
import edu.seu.vcampus.common.hospital.TriageFollowUpOptionView;
import edu.seu.vcampus.common.hospital.TriageMatchLevel;
import edu.seu.vcampus.common.hospital.TriageRecommendationView;
import edu.seu.vcampus.common.hospital.TriageRequest;
import edu.seu.vcampus.common.hospital.TriageResultView;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Patient-facing hybrid department guidance, deliberately not a diagnosis UI. */
final class SmartTriagePanel extends JPanel {

    private static final int TRIAGE_RESPONSE_TIMEOUT_MILLIS = 15_000;

    private static final String INPUT_PAGE = "input";
    private static final String FOLLOW_UP_PAGE = "followUp";
    private static final String RESULT_PAGE = "result";

    private final ClientContext context;
    private final Consumer<String> openDepartmentSlots;
    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final JTextArea description = new JTextArea(7, 50);
    private final JLabel counter = new JLabel("0/500", SwingConstants.RIGHT);
    private final JLabel status = new JLabel(" ");
    private final JButton submit = HospitalTheme.primaryButton("查看科室建议");
    private final JLabel followUpProgress = new JLabel();
    private final JTextArea followUpQuestion = HospitalResponsiveLayout.wrappingText(
            "", HospitalTheme.uiFont(Font.BOLD, 21F), HospitalTheme.TEXT);
    private final JTextArea followUpAnswer = new JTextArea(5, 50);
    private final JLabel followUpCounter = new JLabel("0/300", SwingConstants.RIGHT);
    private final JLabel followUpStatus = new JLabel(" ");
    private final JButton submitFollowUp = HospitalTheme.primaryButton("提交补充信息");
    private final JButton uncertainFollowUp = HospitalTheme.quietButton("不确定");
    private final JButton previousQuestion = HospitalTheme.quietButton("上一个问题");
    private final JPanel followUpAnswerHost = new JPanel(new BorderLayout());
    private final JPanel structuredAnswerPanel = new JPanel(new BorderLayout());
    private JPanel freeTextAnswerPanel;
    private final JPanel resultContent = new JPanel();
    private final List<TriageFollowUpAnswer> followUpAnswers = new ArrayList<>();
    private final Map<String, TriageResultView> seenFollowUpQuestions = new HashMap<>();
    private String pendingFollowUpQuestion;
    private String pendingFollowUpQuestionId;
    private List<TriageFollowUpOptionView> pendingFollowUpOptions = List.of();
    private int requestVersion;

    SmartTriagePanel(
            ClientContext context,
            Runnable goBack,
            Consumer<String> openDepartmentSlots) {
        this.context = context;
        this.openDepartmentSlots = openDepartmentSlots;
        setLayout(new BorderLayout());
        setBackground(HospitalTheme.BACKGROUND);
        pages.setOpaque(false);
        pages.add(createInputPage(goBack), INPUT_PAGE);
        pages.add(createFollowUpPage(goBack), FOLLOW_UP_PAGE);
        pages.add(createResultPage(goBack), RESULT_PAGE);
        add(pages, BorderLayout.CENTER);
    }

    void activate() {
        requestVersion++;
        description.setText("");
        followUpAnswers.clear();
        seenFollowUpQuestions.clear();
        pendingFollowUpQuestion = null;
        pendingFollowUpQuestionId = null;
        pendingFollowUpOptions = List.of();
        followUpAnswer.setText("");
        followUpStatus.setText(" ");
        resultContent.removeAll();
        submit.setEnabled(true);
        submit.setText("查看科室建议");
        submitFollowUp.setEnabled(true);
        submitFollowUp.setText("提交补充信息");
        uncertainFollowUp.setEnabled(true);
        previousQuestion.setEnabled(true);
        status.setText(" ");
        cards.show(pages, INPUT_PAGE);
    }

    private JPanel createInputPage(Runnable goBack) {
        JPanel page = basePage();
        page.setName("triageInputPage");
        page.add(header("智能导诊", "描述主要不适，找到合适的就诊科室", goBack),
                BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(safetyStrip());
        body.add(Box.createVerticalStrut(16));
        body.add(inputCard());
        body.add(Box.createVerticalStrut(14));
        body.add(exampleCard());
        page.add(HospitalResponsiveLayout.verticalScroll(body), BorderLayout.CENTER);
        return page;
    }

    private JPanel createResultPage(Runnable goBack) {
        JPanel page = basePage();
        page.setName("triageResultPage");
        JPanel heading = header("导诊建议", "请核对推荐理由，再决定是否查看号源", goBack);
        JButton revise = HospitalTheme.quietButton("‹ 修改症状描述");
        revise.setName("reviseTriageButton");
        revise.addActionListener(event -> {
            followUpAnswers.clear();
            seenFollowUpQuestions.clear();
            pendingFollowUpQuestion = null;
            pendingFollowUpQuestionId = null;
            pendingFollowUpOptions = List.of();
            cards.show(pages, INPUT_PAGE);
        });
        heading.add(revise, BorderLayout.WEST);
        page.add(heading, BorderLayout.NORTH);
        resultContent.setOpaque(false);
        resultContent.setLayout(new BoxLayout(resultContent, BoxLayout.Y_AXIS));
        page.add(HospitalResponsiveLayout.verticalScroll(resultContent),
                BorderLayout.CENTER);
        return page;
    }

    private JPanel createFollowUpPage(Runnable goBack) {
        JPanel page = basePage();
        page.setName("triageFollowUpPage");
        page.add(header("补充症状信息", "回答后继续为你匹配合适的科室", goBack),
                BorderLayout.NORTH);

        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 18, HospitalTheme.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 26, 24, 26));
        followUpProgress.setName("triageFollowUpProgress");
        followUpProgress.setText("再确认一下");
        followUpProgress.setFont(HospitalTheme.uiFont(Font.BOLD, 12F));
        followUpProgress.setForeground(HospitalTheme.PRIMARY);
        followUpQuestion.setName("triageFollowUpQuestion");
        followUpAnswer.setName("triageFollowUpAnswer");
        followUpAnswer.setLineWrap(true);
        followUpAnswer.setWrapStyleWord(true);
        followUpAnswer.setFont(HospitalTheme.uiFont(Font.PLAIN, 15F));
        followUpAnswer.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        followUpAnswer.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateFollowUpCounter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateFollowUpCounter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateFollowUpCounter();
            }
        });
        JScrollPane answerScroll = new JScrollPane(followUpAnswer);
        answerScroll.setBorder(BorderFactory.createLineBorder(HospitalTheme.BORDER));
        answerScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        followUpCounter.setName("triageFollowUpCounter");
        followUpCounter.setFont(HospitalTheme.dataFont(Font.PLAIN, 12F));
        followUpCounter.setForeground(HospitalTheme.MUTED);
        submitFollowUp.setName("submitTriageFollowUpButton");
        submitFollowUp.addActionListener(event -> requestFollowUp(false));
        uncertainFollowUp.setName("uncertainTriageFollowUpButton");
        uncertainFollowUp.addActionListener(event -> requestFollowUp(true));
        previousQuestion.setName("previousTriageQuestionButton");
        previousQuestion.addActionListener(event -> goToPreviousQuestion());
        followUpStatus.setName("triageFollowUpStatus");
        followUpStatus.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        followUpStatus.setForeground(HospitalTheme.WARNING);
        JPanel actions = HospitalResponsiveLayout.grid(2, 150, 8, 8);
        actions.setOpaque(false);
        actions.add(submitFollowUp);
        actions.add(uncertainFollowUp);

        freeTextAnswerPanel = new JPanel();
        freeTextAnswerPanel.setName("triageFreeTextAnswerPanel");
        freeTextAnswerPanel.setOpaque(false);
        freeTextAnswerPanel.setLayout(new BoxLayout(
                freeTextAnswerPanel, BoxLayout.Y_AXIS));
        freeTextAnswerPanel.add(answerScroll);
        freeTextAnswerPanel.add(Box.createVerticalStrut(5));
        freeTextAnswerPanel.add(followUpCounter);
        freeTextAnswerPanel.add(Box.createVerticalStrut(16));
        freeTextAnswerPanel.add(actions);

        followUpAnswerHost.setOpaque(false);
        structuredAnswerPanel.setOpaque(false);
        structuredAnswerPanel.setName("triageStructuredAnswerPanel");
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        navigation.setOpaque(false);
        navigation.add(previousQuestion);

        card.add(followUpProgress);
        card.add(Box.createVerticalStrut(9));
        card.add(followUpQuestion);
        card.add(Box.createVerticalStrut(16));
        card.add(followUpAnswerHost);
        card.add(Box.createVerticalStrut(14));
        card.add(navigation);
        card.add(Box.createVerticalStrut(10));
        card.add(followUpStatus);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(card);
        page.add(HospitalResponsiveLayout.verticalScroll(body), BorderLayout.CENTER);
        return page;
    }

    private JPanel safetyStrip() {
        HospitalTheme.SurfacePanel strip = new HospitalTheme.SurfacePanel(
                HospitalTheme.WARNING_LIGHT, 14, HospitalTheme.WARNING);
        strip.setLayout(new BorderLayout());
        strip.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JTextArea text = HospitalResponsiveLayout.wrappingText(
                "如果症状严重、意识不清、呼吸困难或大量出血，请立即前往急诊或联系急救服务。本功能不能代替医生诊断。",
                HospitalTheme.uiFont(Font.PLAIN, 13F), HospitalTheme.TEXT);
        strip.add(text, BorderLayout.CENTER);
        return strip;
    }

    private JPanel inputCard() {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 18, HospitalTheme.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        JLabel title = new JLabel("主要不适是什么？");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 20F));
        title.setForeground(HospitalTheme.TEXT);
        JTextArea hint = HospitalResponsiveLayout.wrappingText(
                "可以写明不舒服的部位、持续时间、主要感受，以及是否由运动或受伤引起。",
                HospitalTheme.uiFont(Font.PLAIN, 13F), HospitalTheme.MUTED);
        description.setName("triageDescription");
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setFont(HospitalTheme.uiFont(Font.PLAIN, 15F));
        description.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JScrollPane inputScroll = new JScrollPane(description);
        inputScroll.setBorder(BorderFactory.createLineBorder(HospitalTheme.BORDER));
        inputScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        counter.setName("triageCharacterCounter");
        counter.setFont(HospitalTheme.dataFont(Font.PLAIN, 12F));
        counter.setForeground(HospitalTheme.MUTED);
        description.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateCounter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateCounter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateCounter();
            }
        });
        submit.setName("submitTriageButton");
        submit.addActionListener(event -> requestTriage());
        status.setName("triageStatus");
        status.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        status.setForeground(HospitalTheme.WARNING);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.add(submit);

        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(hint);
        card.add(Box.createVerticalStrut(14));
        card.add(inputScroll);
        card.add(Box.createVerticalStrut(5));
        card.add(counter);
        card.add(Box.createVerticalStrut(16));
        card.add(actions);
        card.add(Box.createVerticalStrut(8));
        card.add(status);
        return card;
    }

    private JPanel exampleCard() {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY_LIGHT, 16);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JLabel label = new JLabel("不知道怎么写？选择一个示例后再修改");
        label.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        label.setForeground(HospitalTheme.PRIMARY_DARK);
        JPanel examples = HospitalResponsiveLayout.grid(4, 110, 8, 8);
        examples.setOpaque(false);
        addExample(examples, "咳嗽发热", "咳嗽并伴有发热，已经持续两天");
        addExample(examples, "运动扭伤", "打球时扭伤脚踝，现在肿胀并且走路疼");
        addExample(examples, "牙齿疼痛", "右侧牙齿持续疼痛，咬东西时更明显");
        addExample(examples, "睡眠困扰", "最近压力较大，连续一周难以入睡");
        card.add(label);
        card.add(Box.createVerticalStrut(10));
        card.add(examples);
        return card;
    }

    private void addExample(JPanel parent, String label, String text) {
        JButton button = HospitalTheme.quietButton(label);
        button.addActionListener(event -> {
            description.setText(text);
            description.requestFocusInWindow();
        });
        parent.add(button);
    }

    private void requestTriage() {
        TriageRequest request;
        try {
            followUpAnswers.clear();
            seenFollowUpQuestions.clear();
            request = new TriageRequest(
                    description.getText(), false);
        } catch (IllegalArgumentException exception) {
            status.setText(description.getText().length() > 500
                    ? "症状描述不能超过 500 个字符。"
                    : "请至少用两个字描述主要不适。");
            return;
        }
        sendTriage(request, submit, status, List.of());
    }

    private void requestFollowUp(boolean uncertain) {
        if (pendingFollowUpQuestion == null || pendingFollowUpQuestionId != null) {
            return;
        }
        String answer = uncertain ? "不确定" : followUpAnswer.getText().trim();
        if (answer.isBlank()) {
            followUpStatus.setText("请填写补充信息，无法确定时可点击“不确定”。");
            return;
        }
        if (answer.length() > 300) {
            followUpStatus.setText("补充信息不能超过 300 个字符。");
            return;
        }
        List<TriageFollowUpAnswer> proposed = new ArrayList<>(followUpAnswers);
        try {
            proposed.add(new TriageFollowUpAnswer(pendingFollowUpQuestion, answer));
            TriageRequest request = new TriageRequest(
                    description.getText(), false, proposed);
            sendTriage(request, submitFollowUp, followUpStatus, proposed);
        } catch (IllegalArgumentException exception) {
            followUpStatus.setText("补充信息无效，请修改后重试。");
        }
    }

    private void requestStructuredFollowUp(
            TriageFollowUpOptionView selected,
            JButton activeButton) {
        if (pendingFollowUpQuestion == null || pendingFollowUpQuestionId == null) {
            return;
        }
        List<TriageFollowUpAnswer> proposed = new ArrayList<>(followUpAnswers);
        try {
            proposed.add(new TriageFollowUpAnswer(
                    pendingFollowUpQuestionId,
                    pendingFollowUpQuestion,
                    selected.getOptionId(),
                    selected.getLabel()));
            TriageRequest request = new TriageRequest(
                    description.getText(), false, proposed);
            sendTriage(request, activeButton, followUpStatus, proposed);
        } catch (IllegalArgumentException exception) {
            followUpStatus.setText("该选项暂时无法提交，请重新选择。");
        }
    }

    private void sendTriage(
            TriageRequest request,
            JButton activeButton,
            JLabel activeStatus,
            List<TriageFollowUpAnswer> acceptedAnswers) {
        int version = ++requestVersion;
        String idleText = activeButton.getText();
        activeButton.setEnabled(false);
        if (activeButton == submitFollowUp) {
            uncertainFollowUp.setEnabled(false);
            previousQuestion.setEnabled(false);
        }
        if (pendingFollowUpQuestionId != null) {
            setStructuredAnswerButtonsEnabled(false);
            previousQuestion.setEnabled(false);
        }
        activeButton.setText("正在分析……");
        activeStatus.setForeground(HospitalTheme.MUTED);
        activeStatus.setText("正在整理你的症状信息……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(
                        HospitalActions.GET_TRIAGE_RECOMMENDATION,
                        request,
                        TRIAGE_RESPONSE_TIMEOUT_MILLIS);
            }

            @Override
            protected void done() {
                if (version != requestVersion) {
                    return;
                }
                activeButton.setEnabled(true);
                activeButton.setText(idleText);
                uncertainFollowUp.setEnabled(true);
                previousQuestion.setEnabled(true);
                setStructuredAnswerButtonsEnabled(true);
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof TriageResultView result) {
                        followUpAnswers.clear();
                        followUpAnswers.addAll(acceptedAnswers);
                        displayTriageResult(result);
                    } else {
                        showFailure(activeStatus, "导诊暂时不可用：" + response.getMessage());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showFailure(activeStatus, "导诊已中断，请重新尝试。");
                } catch (ExecutionException exception) {
                    showFailure(activeStatus, causedByTimeout(exception)
                            ? "智能导诊响应超时，请稍后重试。"
                            : "无法连接服务器，请确认服务器已经启动。");
                }
            }
        }.execute();
    }

    void displayTriageResult(TriageResultView result) {
        if (result.needsFollowUp()) {
            pendingFollowUpQuestion = result.getFollowUpQuestion();
            seenFollowUpQuestions.put(questionKey(
                    result.getFollowUpQuestionId(), result.getFollowUpQuestion()), result);
            showFollowUp(result, null);
            return;
        }
        pendingFollowUpQuestion = null;
        pendingFollowUpQuestionId = null;
        pendingFollowUpOptions = List.of();
        resultContent.removeAll();
        resultContent.add(resultBanner(result));
        if (!result.isUrgent()) {
            resultContent.add(Box.createVerticalStrut(16));
            if (result.getRecommendations().isEmpty()) {
                resultContent.add(emptyResultCard());
            } else {
                int index = 1;
                for (TriageRecommendationView recommendation
                        : result.getRecommendations()) {
                    resultContent.add(recommendationCard(index++, recommendation));
                    resultContent.add(Box.createVerticalStrut(12));
                }
            }
        }
        resultContent.revalidate();
        resultContent.repaint();
        cards.show(pages, RESULT_PAGE);
    }

    private void goToPreviousQuestion() {
        requestVersion++;
        submitFollowUp.setEnabled(true);
        submitFollowUp.setText("提交补充信息");
        uncertainFollowUp.setEnabled(true);
        previousQuestion.setEnabled(true);
        followUpStatus.setText(" ");
        if (followUpAnswers.isEmpty()) {
            pendingFollowUpQuestion = null;
            pendingFollowUpQuestionId = null;
            pendingFollowUpOptions = List.of();
            followUpAnswer.setText("");
            cards.show(pages, INPUT_PAGE);
            return;
        }
        TriageFollowUpAnswer previous = followUpAnswers.remove(
                followUpAnswers.size() - 1);
        TriageResultView previousQuestionView = seenFollowUpQuestions.get(
                questionKey(previous.getQuestionId(), previous.getQuestion()));
        if (previousQuestionView == null) {
            previousQuestionView = new TriageResultView(
                    false,
                    "请补充一个关键信息。",
                    List.of(),
                    previous.getQuestion(),
                    followUpAnswers.size(),
                    true,
                    false);
        }
        showFollowUp(previousQuestionView, previous);
    }

    private void showFollowUp(
            TriageResultView questionView,
            TriageFollowUpAnswer previousAnswer) {
        pendingFollowUpQuestion = questionView.getFollowUpQuestion();
        pendingFollowUpQuestionId = questionView.getFollowUpQuestionId();
        pendingFollowUpOptions = questionView.getFollowUpOptions();
        followUpQuestion.setText(pendingFollowUpQuestion);
        followUpAnswerHost.removeAll();
        if (questionView.hasStructuredFollowUp()) {
            followUpProgress.setText("请选择最符合的一项");
            renderStructuredAnswers(previousAnswer == null
                    ? null : previousAnswer.getAnswerId());
            followUpAnswerHost.add(structuredAnswerPanel, BorderLayout.CENTER);
        } else {
            followUpProgress.setText("再确认一下");
            followUpAnswer.setText(previousAnswer == null
                    ? "" : previousAnswer.getAnswer());
            followUpAnswerHost.add(freeTextAnswerPanel, BorderLayout.CENTER);
        }
        followUpAnswerHost.revalidate();
        followUpAnswerHost.repaint();
        followUpStatus.setText(" ");
        submitFollowUp.setEnabled(true);
        uncertainFollowUp.setEnabled(true);
        previousQuestion.setEnabled(true);
        cards.show(pages, FOLLOW_UP_PAGE);
        if (!questionView.hasStructuredFollowUp()) {
            SwingUtilities.invokeLater(followUpAnswer::requestFocusInWindow);
        }
    }

    private void renderStructuredAnswers(String selectedOptionId) {
        structuredAnswerPanel.removeAll();
        JPanel choices = HospitalResponsiveLayout.grid(2, 260, 10, 10);
        choices.setOpaque(false);
        for (TriageFollowUpOptionView option : pendingFollowUpOptions) {
            JButton choice = HospitalTheme.quietButton(option.getLabel());
            choice.setName("triageFollowUpOptionButton");
            choice.setActionCommand(option.getOptionId());
            choice.setToolTipText(option.getLabel());
            if (option.getOptionId().equals(selectedOptionId)) {
                HospitalTheme.applyPrimaryStyle(choice);
            }
            choice.addActionListener(event -> requestStructuredFollowUp(option, choice));
            choices.add(choice);
        }
        structuredAnswerPanel.add(choices, BorderLayout.CENTER);
        structuredAnswerPanel.revalidate();
        structuredAnswerPanel.repaint();
    }

    private void setStructuredAnswerButtonsEnabled(boolean enabled) {
        for (JButton button : buttonsInside(structuredAnswerPanel)) {
            button.setEnabled(enabled);
        }
    }

    private static List<JButton> buttonsInside(java.awt.Container root) {
        List<JButton> buttons = new ArrayList<>();
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof JButton button) {
                buttons.add(button);
            }
            if (child instanceof java.awt.Container container) {
                buttons.addAll(buttonsInside(container));
            }
        }
        return buttons;
    }

    private static String questionKey(String questionId, String question) {
        return questionId == null ? "text:" + question : "id:" + questionId;
    }

    private JPanel resultBanner(TriageResultView result) {
        Color accent = result.isUrgent() ? HospitalTheme.WARNING : HospitalTheme.PRIMARY;
        Color background = result.isUrgent()
                ? HospitalTheme.WARNING_LIGHT : HospitalTheme.PRIMARY_LIGHT;
        HospitalTheme.SurfacePanel banner = new HospitalTheme.SurfacePanel(
                background, 18, accent);
        banner.setLayout(new BorderLayout(18, 0));
        banner.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JLabel title = new JLabel(result.isUrgent()
                ? "请先寻求线下帮助" : "已生成科室建议");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 21F));
        title.setForeground(result.isUrgent() ? HospitalTheme.WARNING : HospitalTheme.PRIMARY_DARK);
        JTextArea message = HospitalResponsiveLayout.wrappingText(
                result.getSafetyMessage(),
                HospitalTheme.uiFont(Font.PLAIN, 13F), HospitalTheme.TEXT);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(title);
        copy.add(Box.createVerticalStrut(7));
        copy.add(message);
        banner.add(copy, BorderLayout.CENTER);
        return banner;
    }

    private JPanel recommendationCard(
            int index,
            TriageRecommendationView recommendation) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 16, HospitalTheme.BORDER);
        card.setName("triageRecommendationCard");
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel route = new JLabel("建议 " + index + "  ·  "
                + matchLevelText(recommendation.getMatchLevel()));
        route.setFont(HospitalTheme.uiFont(Font.BOLD, 12F));
        route.setForeground(HospitalTheme.PRIMARY);
        JLabel department = new JLabel(recommendation.getDepartmentName());
        department.setFont(HospitalTheme.uiFont(Font.BOLD, 21F));
        department.setForeground(HospitalTheme.TEXT);
        JTextArea reasons = HospitalResponsiveLayout.wrappingText(
                recommendation.getReasons().stream()
                        .map(reason -> "• " + reason)
                        .reduce((left, right) -> left + System.lineSeparator() + right)
                        .orElse(""),
                HospitalTheme.uiFont(Font.PLAIN, 13F), HospitalTheme.MUTED);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(route);
        copy.add(Box.createVerticalStrut(4));
        copy.add(department);
        copy.add(Box.createVerticalStrut(9));
        copy.add(reasons);
        JButton open = HospitalTheme.primaryButton("查看该科室号源");
        open.setName("openTriageDepartmentButton");
        open.setActionCommand(recommendation.getDepartmentId());
        open.addActionListener(event -> openDepartmentSlots.accept(
                recommendation.getDepartmentId()));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.add(open);
        card.add(copy, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel emptyResultCard() {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        JTextArea label = HospitalResponsiveLayout.wrappingText(
                "当前没有可用的匹配科室，请返回首页后直接查看科室目录。",
                HospitalTheme.uiFont(Font.PLAIN, 14F), HospitalTheme.MUTED);
        card.add(label, BorderLayout.CENTER);
        return card;
    }

    private void updateCounter() {
        int length = description.getText().length();
        counter.setText(length + "/500");
        counter.setForeground(length > 500 ? HospitalTheme.WARNING : HospitalTheme.MUTED);
    }

    private void updateFollowUpCounter() {
        int length = followUpAnswer.getText().length();
        followUpCounter.setText(length + "/300");
        followUpCounter.setForeground(
                length > 300 ? HospitalTheme.WARNING : HospitalTheme.MUTED);
    }

    private void showFailure(JLabel target, String message) {
        target.setForeground(HospitalTheme.WARNING);
        target.setText(message);
    }

    private static boolean causedByTimeout(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static JPanel header(String titleText, String subtitleText, Runnable goBack) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(titleText);
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 26F));
        title.setForeground(HospitalTheme.TEXT);
        JTextArea subtitle = HospitalResponsiveLayout.wrappingText(
                subtitleText,
                HospitalTheme.uiFont(Font.PLAIN, 13F), HospitalTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(subtitle);
        JButton back = HospitalTheme.quietButton("返回医院首页");
        back.addActionListener(event -> goBack.run());
        header.add(copy, BorderLayout.CENTER);
        header.add(back, BorderLayout.EAST);
        return HospitalResponsiveLayout.constrainWidth(header);
    }

    private static JPanel basePage() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(HospitalTheme.BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        return page;
    }

    private static String matchLevelText(TriageMatchLevel level) {
        return switch (level) {
            case HIGH -> "线索较集中";
            case MEDIUM -> "有相关线索";
            case GENERAL -> "建议先行评估";
        };
    }
}
