package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.TriageMatchLevel;
import edu.seu.vcampus.common.hospital.TriageRecommendationView;
import edu.seu.vcampus.common.hospital.TriageRequest;
import edu.seu.vcampus.common.hospital.TriageResultView;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Patient-facing local-rule department guidance, deliberately not a diagnosis UI. */
final class SmartTriagePanel extends JPanel {

    private static final String INPUT_PAGE = "input";
    private static final String RESULT_PAGE = "result";

    private final ClientContext context;
    private final Consumer<String> openDepartmentSlots;
    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final JTextArea description = new JTextArea(7, 50);
    private final JCheckBox urgentConcern = new JCheckBox(
            "我认为情况紧急，或不确定是否可以等待普通预约");
    private final JLabel counter = new JLabel("0/500", SwingConstants.RIGHT);
    private final JLabel status = new JLabel(" ");
    private final JButton submit = HospitalTheme.primaryButton("查看科室建议");
    private final JPanel resultContent = new JPanel();
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
        pages.add(createResultPage(goBack), RESULT_PAGE);
        add(pages, BorderLayout.CENTER);
    }

    void activate() {
        requestVersion++;
        description.setText("");
        urgentConcern.setSelected(false);
        resultContent.removeAll();
        submit.setEnabled(true);
        submit.setText("查看科室建议");
        status.setText(" ");
        cards.show(pages, INPUT_PAGE);
    }

    private JPanel createInputPage(Runnable goBack) {
        JPanel page = basePage();
        page.add(header("智能导诊", "描述主要不适，获得可解释的科室建议", goBack),
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
        JPanel heading = header("导诊建议", "请核对推荐理由，再决定是否查看号源", goBack);
        JButton revise = HospitalTheme.quietButton("‹ 修改症状描述");
        revise.setName("reviseTriageButton");
        revise.addActionListener(event -> cards.show(pages, INPUT_PAGE));
        heading.add(revise, BorderLayout.WEST);
        page.add(heading, BorderLayout.NORTH);
        resultContent.setOpaque(false);
        resultContent.setLayout(new BoxLayout(resultContent, BoxLayout.Y_AXIS));
        page.add(HospitalResponsiveLayout.verticalScroll(resultContent),
                BorderLayout.CENTER);
        return page;
    }

    private JPanel safetyStrip() {
        HospitalTheme.SurfacePanel strip = new HospitalTheme.SurfacePanel(
                HospitalTheme.WARNING_LIGHT, 14, HospitalTheme.WARNING);
        strip.setLayout(new BorderLayout());
        strip.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JLabel text = new JLabel("<html><b>先判断是否适合等待</b><br>"
                + "本功能只帮助选择科室。如果存在明显紧急情况，请直接寻求线下医护人员帮助。"
                + "</html>");
        text.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        text.setForeground(HospitalTheme.TEXT);
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
        JLabel hint = new JLabel("建议写明部位、持续时间、主要感受和是否由运动或受伤引起。");
        hint.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        hint.setForeground(HospitalTheme.MUTED);
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
        urgentConcern.setName("triageUrgentConcern");
        urgentConcern.setOpaque(false);
        urgentConcern.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        urgentConcern.setForeground(HospitalTheme.WARNING);
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
        card.add(Box.createVerticalStrut(14));
        card.add(urgentConcern);
        card.add(Box.createVerticalStrut(16));
        card.add(actions);
        card.add(Box.createVerticalStrut(8));
        card.add(status);
        return card;
    }

    private JPanel exampleCard() {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY_LIGHT, 16);
        card.setLayout(new BorderLayout(14, 0));
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
        card.add(label, BorderLayout.WEST);
        card.add(examples, BorderLayout.CENTER);
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
            request = new TriageRequest(
                    description.getText(), urgentConcern.isSelected());
        } catch (IllegalArgumentException exception) {
            status.setText(description.getText().length() > 500
                    ? "症状描述不能超过 500 个字符。"
                    : "请至少用两个字描述主要不适，或勾选紧急情况。");
            return;
        }
        int version = ++requestVersion;
        submit.setEnabled(false);
        submit.setText("正在分析……");
        status.setForeground(HospitalTheme.MUTED);
        status.setText("正在使用本地规则匹配可挂号科室，不会保存这段描述。");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.GET_TRIAGE_RECOMMENDATION, request);
            }

            @Override
            protected void done() {
                if (version != requestVersion) {
                    return;
                }
                submit.setEnabled(true);
                submit.setText("查看科室建议");
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof TriageResultView result) {
                        renderResult(result);
                    } else {
                        showFailure("导诊暂时不可用：" + response.getMessage());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showFailure("导诊已中断，请重新尝试。");
                } catch (ExecutionException exception) {
                    showFailure("无法连接服务器，请确认服务器已经启动。");
                }
            }
        }.execute();
    }

    private void renderResult(TriageResultView result) {
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
        JLabel message = new JLabel("<html><body style='width:650px'>"
                + html(result.getSafetyMessage()) + "</body></html>");
        message.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        message.setForeground(HospitalTheme.TEXT);
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
        card.setLayout(new BorderLayout(20, 0));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel route = new JLabel("建议 " + index + "  ·  "
                + matchLevelText(recommendation.getMatchLevel()));
        route.setFont(HospitalTheme.uiFont(Font.BOLD, 12F));
        route.setForeground(HospitalTheme.PRIMARY);
        JLabel department = new JLabel(recommendation.getDepartmentName());
        department.setFont(HospitalTheme.uiFont(Font.BOLD, 21F));
        department.setForeground(HospitalTheme.TEXT);
        JLabel reasons = new JLabel("<html>"
                + recommendation.getReasons().stream()
                        .map(reason -> "• " + html(reason))
                        .reduce((left, right) -> left + "<br>" + right)
                        .orElse("")
                + "</html>");
        reasons.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        reasons.setForeground(HospitalTheme.MUTED);
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
        card.add(copy, BorderLayout.CENTER);
        card.add(open, BorderLayout.EAST);
        return card;
    }

    private JPanel emptyResultCard() {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        JLabel label = new JLabel("当前没有可用的匹配科室，请返回首页后直接查看科室目录。");
        label.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
        label.setForeground(HospitalTheme.MUTED);
        card.add(label, BorderLayout.CENTER);
        return card;
    }

    private void updateCounter() {
        int length = description.getText().length();
        counter.setText(length + "/500");
        counter.setForeground(length > 500 ? HospitalTheme.WARNING : HospitalTheme.MUTED);
    }

    private void showFailure(String message) {
        status.setForeground(HospitalTheme.WARNING);
        status.setText(message);
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
        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        subtitle.setForeground(HospitalTheme.MUTED);
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

    private static String html(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
