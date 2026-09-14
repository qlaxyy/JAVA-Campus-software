package edu.seu.vcampus.client.module.card;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.card.CardActions;
import edu.seu.vcampus.common.card.CardRechargeRequest;
import edu.seu.vcampus.common.card.CampusCardLedgerEntry;
import edu.seu.vcampus.common.card.CampusCardLedgerResponse;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.CampusCardView;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Balance, recharge and ledger for the campus-card TCP gateway.
 */
final class CardWalletPanel extends JPanel {

    private static final Color PRIMARY = new Color(15, 118, 110);
    private static final Color PAGE = new Color(244, 248, 247);
    private static final Color TEXT = new Color(25, 50, 47);

    private final ClientContext context;
    private final JLabel summary = new JLabel("请先登录后查看校园卡");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"时间", "类型", "系统", "单号", "金额"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    CardWalletPanel(ClientContext context) {
        this.context = context;
        setLayout(new BorderLayout(0, 12));
        setBackground(PAGE);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        summary.setFont(new Font("SansSerif", Font.BOLD, 20));
        summary.setForeground(PRIMARY);
        add(summary, BorderLayout.NORTH);

        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton refresh = new JButton("刷新");
        refresh.addActionListener(event -> reload());
        actions.add(refresh);
        for (int fen : new int[]{1000, 2000, 5000, 10_000}) {
            JButton button = new JButton("充值 " + yuan(fen));
            button.addActionListener(event -> recharge(fen));
            actions.add(button);
        }
        JButton custom = new JButton("自定义金额");
        custom.addActionListener(event -> {
            Integer fen = promptCustomAmount();
            if (fen != null) {
                recharge(fen);
            }
        });
        actions.add(custom);
        add(actions, BorderLayout.SOUTH);

        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                reload();
            }
        });
    }

    private Integer promptCustomAmount() {
        String input = (String) JOptionPane.showInputDialog(
                this,
                "请输入充值金额（元），范围 0.01–10000。",
                "自定义金额",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                "");
        if (input == null) {
            return null;
        }
        Integer fen = yuanToFen(input);
        if (fen == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "请输入 0.01 到 10000 之间的金额，最多两位小数。",
                    "自定义金额",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return fen;
    }

    private void recharge(int amountFen) {
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.sendCard(CardActions.RECHARGE, new CardRechargeRequest(amountFen));
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (!response.isSuccess()) {
                        JOptionPane.showMessageDialog(
                                CardWalletPanel.this, response.getMessage(), "校园卡",
                                JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(
                            CardWalletPanel.this, "无法连接校园卡服务。", "校园卡",
                            JOptionPane.ERROR_MESSAGE);
                }
                reload();
            }
        }.execute();
    }

    private void reload() {
        new SwingWorker<Void, Void>() {
            private String summaryText = "无法读取校园卡";
            private List<CampusCardLedgerEntry> entries = List.of();

            @Override
            protected Void doInBackground() throws Exception {
                Response cardResponse = context.sendCard(CardActions.GET, null);
                if (cardResponse.isSuccess() && cardResponse.getData() instanceof CampusCardView card) {
                    summaryText = "一卡通号 " + card.getCardNo() + "　　余额 " + yuan(card.getBalanceFen());
                } else {
                    summaryText = cardResponse.getMessage();
                }
                Response ledgerResponse = context.sendCard(CardActions.LIST_LEDGER, null);
                if (ledgerResponse.isSuccess()
                        && ledgerResponse.getData() instanceof CampusCardLedgerResponse ledger) {
                    entries = ledger.getEntries();
                }
                return null;
            }

            @Override
            protected void done() {
                summary.setText(summaryText);
                summary.setForeground(TEXT);
                tableModel.setRowCount(0);
                for (CampusCardLedgerEntry entry : entries) {
                    tableModel.addRow(new Object[]{
                            entry.getCreatedAt(),
                            label(entry.getEntryType()),
                            entry.getMerchant(),
                            entry.getReference(),
                            (CampusCardLedgerEntry.DEBIT.equals(entry.getEntryType()) ? "-" : "+")
                                    + yuan(entry.getAmountFen())
                    });
                }
            }
        }.execute();
    }

    private static String label(String entryType) {
        return switch (entryType) {
            case CampusCardLedgerEntry.RECHARGE -> "充值";
            case CampusCardLedgerEntry.DEBIT -> "扣款";
            case CampusCardLedgerEntry.CREDIT -> "退款";
            default -> entryType;
        };
    }

    private static String yuan(int fen) {
        return "¥" + String.format("%.2f", fen / 100.0);
    }

    private static Integer yuanToFen(String text) {
        String trimmed = text.trim().replace("¥", "").replace("元", "").replace(",", "");
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            BigDecimal fen = new BigDecimal(trimmed).movePointRight(2).setScale(0, RoundingMode.HALF_UP);
            int amountFen = fen.intValueExact();
            if (amountFen < CardRechargeRequest.MIN_FEN || amountFen > CardRechargeRequest.MAX_FEN) {
                return null;
            }
            return amountFen;
        } catch (ArithmeticException | NumberFormatException exception) {
            return null;
        }
    }
}
