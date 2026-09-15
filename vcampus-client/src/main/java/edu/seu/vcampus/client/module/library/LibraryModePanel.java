package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.module.ModuleViewLifecycle;
import edu.seu.vcampus.client.view.ResponsiveLayout;
import edu.seu.vcampus.common.protocol.ModuleNames;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/** Selects and hosts the online library and simulated self-service terminal modes. */
public final class LibraryModePanel extends JPanel implements ModuleViewLifecycle {

    private static final String MODE_SELECTION = "modeSelection";
    private static final String ONLINE_LIBRARY = "onlineLibrary";
    private static final String ADMIN_LIBRARY = "adminLibrary";
    private static final String SELF_SERVICE_TERMINAL = "selfServiceTerminal";

    private final CardLayout cards = new CardLayout();
    private final MyLibraryPanel myLibrary;
    private final LibraryAdminPanel admin;
    private JTabbedPane onlineTabs;

    /** @param context shared authenticated client context */
    public LibraryModePanel(ClientContext context) {
        LibraryPanel catalog = new LibraryPanel(context);
        myLibrary = new MyLibraryPanel(
                context, catalog::reservationStateChanged);
        admin = context.currentSession()
                .filter(session -> session.canAdminister(ModuleNames.LIBRARY))
                .map(session -> new LibraryAdminPanel(context)).orElse(null);
        setName("library.modeRoot");
        setLayout(cards);
        add(createModeSelection(admin), MODE_SELECTION);
        add(createOnlineLibrary(catalog, myLibrary), ONLINE_LIBRARY);
        if (admin != null) {
            add(createAdminLibrary(admin), ADMIN_LIBRARY);
        }
        add(createTerminal(context, () -> {
            catalog.reservationStateChanged();
            myLibrary.refresh();
        }), SELF_SERVICE_TERMINAL);
        showModeSelection();
    }

    private JPanel createModeSelection(LibraryAdminPanel admin) {
        JPanel selection = new JPanel(new BorderLayout(24, 24));
        selection.setName("library.modeSelection");
        LibraryUiTheme.installPage(selection);
        selection.setBorder(BorderFactory.createEmptyBorder(18, 36, 24, 36));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("VIRTUAL CAMPUS LIBRARY", SwingConstants.CENTER);
        eyebrow.setAlignmentX(Component.CENTER_ALIGNMENT);
        eyebrow.setForeground(LibraryUiTheme.PRIMARY);
        eyebrow.setFont(eyebrow.getFont().deriveFont(Font.BOLD, 11F));
        JLabel title = new JLabel("校园图书馆", SwingConstants.CENTER);
        title.putClientProperty("module.pageTitle", true);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(LibraryUiTheme.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 25F));
        heading.add(eyebrow);
        heading.add(Box.createVerticalStrut(8));
        heading.add(title);
        selection.add(heading, BorderLayout.NORTH);

        JPanel choices = ResponsiveLayout.entranceRows(16);
        choices.setName("library.modeChoices");
        choices.setOpaque(false);
        choices.add(createModeChoice("线上图书馆", "library.mode.online",
                "ONLINE SERVICES",
                "馆藏查询 · 预约 · 我的借阅",
                "进入  →", true,
                () -> cards.show(this, ONLINE_LIBRARY)));
        if (admin != null) {
            choices.add(createModeChoice("图书管理员工作台", "library.mode.admin",
                    "LIBRARIAN WORKSPACE",
                    "书目 · 单册 · 借阅 · 统计",
                    "进入  →", false,
                    () -> {
                        cards.show(this, ADMIN_LIBRARY);
                        admin.refresh();
                    }));
        }
        choices.add(createModeChoice("模拟自助终端", "library.mode.terminal",
                "SELF-SERVICE TERMINAL",
                "借书 · 还书",
                "进入  →", false,
                () -> cards.show(this, SELF_SERVICE_TERMINAL)));
        if (admin == null) {
            JPanel emptySlot = new JPanel();
            emptySlot.setOpaque(false);
            choices.add(emptySlot);
        }
        selection.add(ResponsiveLayout.verticalScroll(choices), BorderLayout.CENTER);

        return selection;
    }

    private JPanel createModeChoice(String title, String name, String eyebrow,
            String description, String actionText, boolean primary, Runnable action) {
        JPanel choice = new JPanel(new BorderLayout(24, 0));
        choice.setName(name + ".card");
        choice.setPreferredSize(new Dimension(800, 130));
        LibraryUiTheme.styleCard(choice);
        choice.setBorder(LibraryUiTheme.cardBorder(20, 20));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel kind = new JLabel(eyebrow);
        kind.setAlignmentX(Component.LEFT_ALIGNMENT);
        kind.setForeground(LibraryUiTheme.PRIMARY);
        kind.setFont(kind.getFont().deriveFont(Font.BOLD, 11F));
        JLabel heading = new JLabel(title);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.setForeground(LibraryUiTheme.TEXT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22F));
        JLabel detail = new JLabel(description);
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);
        detail.setForeground(LibraryUiTheme.MUTED);
        detail.setFont(detail.getFont().deriveFont(14F));
        copy.add(kind);
        copy.add(Box.createVerticalStrut(8));
        copy.add(heading);
        copy.add(Box.createVerticalStrut(8));
        copy.add(detail);

        JButton button = new JButton(actionText);
        button.setName(name);
        if (primary) {
            LibraryUiTheme.stylePrimaryButton(button);
        } else {
            LibraryUiTheme.styleSecondaryButton(button);
        }
        LibraryUiTheme.makeLargeButton(button);
        button.setPreferredSize(new Dimension(230, 48));
        button.addActionListener(event -> action.run());
        choice.add(copy, BorderLayout.CENTER);
        choice.add(button, BorderLayout.EAST);
        return choice;
    }

    private JPanel createOnlineLibrary(LibraryPanel catalog,
            MyLibraryPanel myLibrary) {
        JPanel online = createModeContainer("线上图书馆", "library.mode.back.online");
        online.setName("library.online");

        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("library.navigation");
        LibraryUiTheme.styleTabbedPane(tabs);
        tabs.addTab("馆藏查询", catalog);
        tabs.addTab("我的图书馆", myLibrary);
        onlineTabs = tabs;

        tabs.addChangeListener(event -> {
            if (tabs.getSelectedComponent() == myLibrary) {
                myLibrary.refresh();
            } else if (tabs.getSelectedComponent() == catalog) {
                catalog.refreshIfSearched();
            }
        });
        online.add(tabs, BorderLayout.CENTER);
        return online;
    }

    private JPanel createAdminLibrary(LibraryAdminPanel admin) {
        JPanel panel = createModeContainer("图书管理员工作台", "library.mode.back.admin");
        panel.setName("library.adminMode");
        panel.add(admin, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTerminal(ClientContext context, Runnable circulationChanged) {
        JPanel terminal = createModeContainer(
                "模拟自助终端", "library.mode.back.terminal");
        terminal.setName("library.terminal");
        terminal.add(new SelfServicePanel(context, circulationChanged), BorderLayout.CENTER);
        return terminal;
    }

    private JPanel createModeContainer(String title, String backButtonName) {
        JPanel container = new JPanel(new BorderLayout(0, 6));
        LibraryUiTheme.installPage(container);
        container.setBorder(BorderFactory.createEmptyBorder(6, 12, 10, 12));
        JPanel header = new JPanel(new BorderLayout());
        LibraryUiTheme.styleCard(header);
        header.setBorder(LibraryUiTheme.cardBorder(5, 8));
        JButton back = new JButton("← 返回模式选择");
        back.setName(backButtonName);
        LibraryUiTheme.styleSecondaryButton(back);
        back.addActionListener(event -> showModeSelection());
        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.putClientProperty("module.pageTitle", true);
        heading.setForeground(LibraryUiTheme.TEXT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16F));
        header.add(back, BorderLayout.WEST);
        header.add(heading, BorderLayout.CENTER);
        header.add(Box.createHorizontalStrut(back.getPreferredSize().width), BorderLayout.EAST);
        container.add(header, BorderLayout.NORTH);
        return container;
    }

    @Override
    public void onModuleExit() {
        showModeSelection();
    }

    /**
     * 回到模式选择时顺带把各模式内部的页签复位，这样再次进入模块不会停在
     * 上次离开时的那一页。
     */
    private void showModeSelection() {
        cards.show(this, MODE_SELECTION);
        if (onlineTabs != null) {
            onlineTabs.setSelectedIndex(0);
        }
        myLibrary.resetTabs();
        if (admin != null) {
            admin.resetTabs();
        }
    }
}
