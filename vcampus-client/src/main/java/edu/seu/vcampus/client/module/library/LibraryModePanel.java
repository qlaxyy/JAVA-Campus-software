package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.ModuleNames;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/** Selects and hosts the online library and simulated self-service terminal modes. */
public final class LibraryModePanel extends JPanel {

    private static final String MODE_SELECTION = "modeSelection";
    private static final String ONLINE_LIBRARY = "onlineLibrary";
    private static final String SELF_SERVICE_TERMINAL = "selfServiceTerminal";

    private final CardLayout cards = new CardLayout();

    /** @param context shared authenticated client context */
    public LibraryModePanel(ClientContext context) {
        LibraryPanel catalog = new LibraryPanel(context);
        MyLibraryPanel myLibrary = new MyLibraryPanel(
                context, catalog::reservationStateChanged);
        LibraryAdminPanel admin = context.currentSession()
                .filter(session -> session.canAdminister(ModuleNames.LIBRARY))
                .map(session -> new LibraryAdminPanel(context)).orElse(null);
        setName("library.modeRoot");
        setLayout(cards);
        add(createModeSelection(), MODE_SELECTION);
        add(createOnlineLibrary(catalog, myLibrary, admin), ONLINE_LIBRARY);
        add(createTerminal(context, () -> {
            catalog.reservationStateChanged();
            myLibrary.refresh();
        }), SELF_SERVICE_TERMINAL);
        showModeSelection();
    }

    private JPanel createModeSelection() {
        JPanel selection = new JPanel(new BorderLayout(24, 24));
        selection.setName("library.modeSelection");
        selection.setBorder(BorderFactory.createEmptyBorder(48, 72, 64, 72));

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("欢迎使用图书馆", SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26F));
        JLabel description = new JLabel("请选择本次要使用的服务模式", SwingConstants.CENTER);
        description.setAlignmentX(Component.CENTER_ALIGNMENT);
        heading.add(title);
        heading.add(Box.createVerticalStrut(10));
        heading.add(description);
        selection.add(heading, BorderLayout.NORTH);

        JPanel choices = new JPanel(new GridLayout(1, 2, 32, 0));
        choices.add(createModeChoice("线上图书馆", "library.mode.online",
                "查询与预约馆藏、查看个人借阅和预约，模块管理员可维护图书。",
                () -> cards.show(this, ONLINE_LIBRARY)));
        choices.add(createModeChoice("模拟自助终端", "library.mode.terminal",
                "模拟扫描实体单册条码，完成借书或归还登记。",
                () -> cards.show(this, SELF_SERVICE_TERMINAL)));
        selection.add(choices, BorderLayout.CENTER);
        return selection;
    }

    private JPanel createModeChoice(String title, String name, String description,
            Runnable action) {
        JPanel choice = new JPanel(new BorderLayout(12, 18));
        choice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(32, 28, 32, 28)));
        JButton button = new JButton(title);
        button.setName(name);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 20F));
        button.setPreferredSize(new Dimension(260, 64));
        button.addActionListener(event -> action.run());
        JLabel detail = new JLabel("<html><div style='text-align:center'>"
                + description + "</div></html>", SwingConstants.CENTER);
        choice.add(button, BorderLayout.NORTH);
        choice.add(detail, BorderLayout.CENTER);
        return choice;
    }

    private JPanel createOnlineLibrary(LibraryPanel catalog,
            MyLibraryPanel myLibrary, LibraryAdminPanel admin) {
        JPanel online = createModeContainer("线上图书馆", "library.mode.back.online");
        online.setName("library.online");

        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("library.navigation");
        tabs.addTab("馆藏查询", catalog);
        tabs.addTab("我的图书馆", myLibrary);

        if (admin != null) {
            tabs.addTab("图书管理", admin);
        }
        tabs.addChangeListener(event -> {
            if (admin != null && tabs.getSelectedComponent() == admin) {
                admin.refresh();
            } else if (tabs.getSelectedComponent() == myLibrary) {
                myLibrary.refresh();
            } else if (tabs.getSelectedComponent() == catalog) {
                catalog.refreshIfSearched();
            }
        });
        online.add(tabs, BorderLayout.CENTER);
        return online;
    }

    private JPanel createTerminal(ClientContext context, Runnable circulationChanged) {
        JPanel terminal = createModeContainer(
                "模拟自助借还终端", "library.mode.back.terminal");
        terminal.setName("library.terminal");
        terminal.add(new SelfServicePanel(context, circulationChanged), BorderLayout.CENTER);
        return terminal;
    }

    private JPanel createModeContainer(String title, String backButtonName) {
        JPanel container = new JPanel(new BorderLayout(0, 12));
        container.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
        JPanel header = new JPanel(new BorderLayout());
        JButton back = new JButton("← 返回模式选择");
        back.setName(backButtonName);
        back.addActionListener(event -> showModeSelection());
        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18F));
        header.add(back, BorderLayout.WEST);
        header.add(heading, BorderLayout.CENTER);
        header.add(Box.createHorizontalStrut(back.getPreferredSize().width), BorderLayout.EAST);
        container.add(header, BorderLayout.NORTH);
        return container;
    }

    private void showModeSelection() {
        cards.show(this, MODE_SELECTION);
    }
}
