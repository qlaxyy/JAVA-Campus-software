package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.HospitalBillType;
import edu.seu.vcampus.common.hospital.PatientBillListResponse;
import edu.seu.vcampus.common.hospital.PatientBillView;
import edu.seu.vcampus.common.hospital.PayHospitalBillRequest;
import edu.seu.vcampus.common.hospital.PaymentStatus;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

/** Patient-owned hospital fee statement and simulated payment workflow. */
final class PatientBillsPanel extends JPanel {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ClientContext context;
    private final JLabel summaryLabel = new JLabel("正在读取费用……");
    private final JLabel statusLabel = new JLabel(" ");
    private final JPanel billList = new JPanel();
    private final JButton allButton = HospitalTheme.quietButton("全部");
    private final JButton unpaidButton = HospitalTheme.quietButton("待缴费");
    private final JButton historyButton = HospitalTheme.quietButton("已支付 / 已退款");
    private final JButton retryButton = HospitalTheme.quietButton("重试");

    private List<PatientBillView> bills = List.of();
    private BillFilter filter = BillFilter.ALL;
    private boolean busy;

    PatientBillsPanel(ClientContext context, Runnable back) {
        this.context = context;
        setLayout(new BorderLayout(0, 18));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(HospitalResponsiveLayout.constrainWidth(createHeader(back)),
                BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);

        billList.setName("patientBillList");
        allButton.setName("allBillsFilterButton");
        unpaidButton.setName("unpaidBillsFilterButton");
        historyButton.setName("historyBillsFilterButton");
        retryButton.setName("retryBillsButton");
        allButton.addActionListener(event -> setFilter(BillFilter.ALL));
        unpaidButton.addActionListener(event -> setFilter(BillFilter.UNPAID));
        historyButton.addActionListener(event -> setFilter(BillFilter.HISTORY));
        retryButton.addActionListener(event -> loadBills());
        retryButton.setVisible(false);
        updateFilterStyles();
    }

    void activate() {
        loadBills();
    }

    private JComponent createHeader(Runnable back) {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(false);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("费用清单");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 28F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel subtitle = new JLabel("查看挂号、检查和诊疗费用；支付仅为课程流程模拟");
        subtitle.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
        subtitle.setForeground(HospitalTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(subtitle);
        JButton backButton = HospitalTheme.quietButton("返回医院首页");
        backButton.addActionListener(event -> back.run());
        header.add(copy, BorderLayout.CENTER);
        header.add(backButton, BorderLayout.EAST);
        return header;
    }

    private JComponent createContent() {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);

        HospitalTheme.SurfacePanel statement = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY_DARK, 16);
        statement.setLayout(new BorderLayout(16, 0));
        statement.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JLabel caption = new JLabel("PATIENT ACCOUNT · 校医院收费流水");
        caption.setFont(HospitalTheme.dataFont(Font.BOLD, 12F));
        caption.setForeground(new Color(179, 215, 209));
        summaryLabel.setFont(HospitalTheme.uiFont(Font.BOLD, 17F));
        summaryLabel.setForeground(Color.WHITE);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(caption);
        copy.add(Box.createVerticalStrut(5));
        copy.add(summaryLabel);
        statement.add(copy, BorderLayout.CENTER);

        JPanel controls = new JPanel(new BorderLayout(12, 0));
        controls.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(allButton);
        filters.add(unpaidButton);
        filters.add(historyButton);
        JPanel state = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        state.setOpaque(false);
        statusLabel.setForeground(HospitalTheme.MUTED);
        state.add(statusLabel);
        state.add(retryButton);
        controls.add(filters, BorderLayout.WEST);
        controls.add(state, BorderLayout.EAST);

        billList.setOpaque(false);
        billList.setLayout(new BoxLayout(billList, BoxLayout.Y_AXIS));
        JScrollPane scroll = HospitalResponsiveLayout.verticalScroll(billList);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(statement, BorderLayout.NORTH);
        top.add(controls, BorderLayout.SOUTH);
        content.add(top, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        return content;
    }

    private void loadBills() {
        if (busy) {
            return;
        }
        busy = true;
        statusLabel.setText("正在更新……");
        statusLabel.setForeground(HospitalTheme.MUTED);
        retryButton.setVisible(false);
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.LIST_MY_BILLS, null);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof PatientBillListResponse data) {
                        bills = data.getBills();
                        updateSummary();
                        renderBills();
                        statusLabel.setText("已自动更新");
                    } else {
                        showLoadError(response.getMessage());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showLoadError("读取已中断，请重试。");
                } catch (ExecutionException exception) {
                    showLoadError("无法连接服务器，请确认服务器已经启动。");
                } finally {
                    busy = false;
                }
            }
        }.execute();
    }

    private void updateSummary() {
        List<PatientBillView> unpaid = bills.stream()
                .filter(bill -> bill.getPaymentStatus() == PaymentStatus.UNPAID)
                .toList();
        long amount = unpaid.stream().mapToLong(PatientBillView::getAmountCents).sum();
        summaryLabel.setText(unpaid.isEmpty()
                ? "目前没有待缴费用，历史费用可在下方查询"
                : "待缴 " + unpaid.size() + " 笔，共 " + money(amount));
    }

    private void setFilter(BillFilter next) {
        filter = next;
        updateFilterStyles();
        renderBills();
    }

    private void updateFilterStyles() {
        styleFilter(allButton, filter == BillFilter.ALL);
        styleFilter(unpaidButton, filter == BillFilter.UNPAID);
        styleFilter(historyButton, filter == BillFilter.HISTORY);
    }

    private static void styleFilter(JButton button, boolean selected) {
        if (selected) {
            HospitalTheme.applyPrimaryStyle(button);
        } else {
            HospitalTheme.applyQuietStyle(button);
        }
    }

    private void renderBills() {
        billList.removeAll();
        Predicate<PatientBillView> predicate = switch (filter) {
            case ALL -> ignored -> true;
            case UNPAID -> bill -> bill.getPaymentStatus() == PaymentStatus.UNPAID;
            case HISTORY -> bill -> bill.getPaymentStatus() != PaymentStatus.UNPAID;
        };
        List<PatientBillView> visible = bills.stream().filter(predicate).toList();
        if (visible.isEmpty()) {
            String message = filter == BillFilter.UNPAID
                    ? "当前没有待缴费用。"
                    : filter == BillFilter.HISTORY
                            ? "当前没有已支付或已退款记录。"
                            : "还没有费用记录，完成预约后挂号费会显示在这里。";
            JLabel empty = new JLabel(message);
            empty.setForeground(HospitalTheme.MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(28, 8, 0, 0));
            billList.add(empty);
        } else {
            for (PatientBillView bill : visible) {
                JComponent card = billCard(bill);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 164));
                billList.add(card);
                billList.add(Box.createVerticalStrut(10));
            }
        }
        billList.revalidate();
        billList.repaint();
    }

    private JComponent billCard(PatientBillView bill) {
        Color statusColor = statusColor(bill.getPaymentStatus());
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 14, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createEmptyBorder(17, 18, 17, 18));

        JPanel rail = new JPanel();
        rail.setBackground(statusColor);
        rail.setPreferredSize(new Dimension(5, 105));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel(typeText(bill.getBillType()) + " · "
                + bill.getItemName());
        heading.setFont(HospitalTheme.uiFont(Font.BOLD, 18F));
        heading.setForeground(HospitalTheme.TEXT);
        JLabel visit = new JLabel(bill.getDepartmentName() + " · "
                + bill.getDoctorName() + " · 就诊时间 "
                + DATE_TIME_FORMAT.format(bill.getVisitTime()));
        visit.setForeground(HospitalTheme.MUTED);
        JLabel billNumber = new JLabel("费用单 " + bill.getBillId()
                + "  · 生成于 " + DATE_TIME_FORMAT.format(bill.getCreatedAt()));
        billNumber.setFont(HospitalTheme.dataFont(Font.PLAIN, 12F));
        billNumber.setForeground(HospitalTheme.MUTED);
        copy.add(heading);
        copy.add(Box.createVerticalStrut(7));
        copy.add(visit);
        copy.add(Box.createVerticalStrut(9));
        copy.add(billNumber);

        JPanel amount = new JPanel();
        amount.setOpaque(false);
        amount.setLayout(new BoxLayout(amount, BoxLayout.Y_AXIS));
        JLabel state = new JLabel(statusText(bill.getPaymentStatus()));
        state.setForeground(statusColor);
        state.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        state.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel value = new JLabel(money(bill.getAmountCents()));
        value.setFont(HospitalTheme.dataFont(Font.BOLD, 23F));
        value.setForeground(HospitalTheme.TEXT);
        value.setAlignmentX(Component.RIGHT_ALIGNMENT);
        amount.add(state);
        amount.add(Box.createVerticalStrut(5));
        amount.add(value);
        amount.add(Box.createVerticalStrut(10));
        if (bill.getPaymentStatus() == PaymentStatus.UNPAID) {
            JButton pay = HospitalTheme.primaryButton("模拟缴费");
            pay.setName("payHospitalBillButton");
            pay.setActionCommand(bill.getBillId());
            pay.setAlignmentX(Component.RIGHT_ALIGNMENT);
            pay.addActionListener(event -> confirmPayment(bill));
            amount.add(pay);
        } else {
            JLabel completedAt = new JLabel(completionText(bill));
            completedAt.setForeground(HospitalTheme.MUTED);
            completedAt.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
            completedAt.setAlignmentX(Component.RIGHT_ALIGNMENT);
            amount.add(completedAt);
        }

        card.add(rail, BorderLayout.WEST);
        card.add(copy, BorderLayout.CENTER);
        card.add(amount, BorderLayout.EAST);
        return card;
    }

    private void confirmPayment(PatientBillView bill) {
        if (busy) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                "确认模拟支付以下费用？\n\n"
                        + bill.getItemName() + "\n" + money(bill.getAmountCents())
                        + "\n\n本操作只用于课程项目，不会发生真实扣款。",
                "模拟缴费",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            payBill(bill);
        }
    }

    private void payBill(PatientBillView bill) {
        busy = true;
        statusLabel.setText("正在完成模拟缴费……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(
                        HospitalActions.PAY_BILL,
                        new PayHospitalBillRequest(bill.getBillId()));
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        busy = false;
                        loadBills();
                    } else {
                        statusLabel.setText("缴费失败：" + safeMessage(response.getMessage()));
                        statusLabel.setForeground(HospitalTheme.WARNING);
                        busy = false;
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("缴费已中断，请重试。");
                    busy = false;
                } catch (ExecutionException exception) {
                    statusLabel.setText("无法连接服务器，请确认服务器已经启动。");
                    busy = false;
                }
            }
        }.execute();
    }

    private void showLoadError(String message) {
        statusLabel.setText("读取失败：" + safeMessage(message));
        statusLabel.setForeground(HospitalTheme.WARNING);
        retryButton.setVisible(true);
    }

    private static String money(long cents) {
        return String.format("¥%.2f", cents / 100.0);
    }

    private static String typeText(HospitalBillType type) {
        return switch (type) {
            case REGISTRATION -> "挂号费用";
            case EXAMINATION -> "检查费用";
            case TREATMENT -> "诊疗费用";
        };
    }

    private static String statusText(PaymentStatus status) {
        return switch (status) {
            case UNPAID -> "待缴费";
            case PAID -> "已支付";
            case REFUNDED -> "已退款";
        };
    }

    private static Color statusColor(PaymentStatus status) {
        return switch (status) {
            case UNPAID -> HospitalTheme.WARNING;
            case PAID -> HospitalTheme.SUCCESS;
            case REFUNDED -> HospitalTheme.MUTED;
        };
    }

    private static String completionText(PatientBillView bill) {
        if (bill.getPaymentStatus() == PaymentStatus.REFUNDED
                && bill.getRefundedAt() != null) {
            return "退款于 " + DATE_TIME_FORMAT.format(bill.getRefundedAt());
        }
        if (bill.getPaidAt() != null) {
            return "支付于 " + DATE_TIME_FORMAT.format(bill.getPaidAt());
        }
        return "状态已完成";
    }

    private static String safeMessage(String message) {
        return message == null || message.isBlank() ? "服务器未返回具体原因。" : message;
    }

    private enum BillFilter {
        ALL,
        UNPAID,
        HISTORY
    }
}
