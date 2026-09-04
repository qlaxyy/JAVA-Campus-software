package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.common.protocol.ModuleNames;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

/** Client entry point owned by the library module. */
public final class LibraryClientModule implements ClientModule {

    @Override
    public String id() {
        return ModuleNames.LIBRARY;
    }

    @Override
    public String displayName() {
        return "图书馆";
    }

    @Override
    public JComponent createView(ClientContext context) {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("library.navigation");
        MyBorrowPanel myBorrows = new MyBorrowPanel(context);
        LibraryPanel catalog = new LibraryPanel(context);
        tabs.addTab("馆藏查询", catalog);
        tabs.addTab("我的借阅", myBorrows);
        tabs.addChangeListener(event -> {
            if (tabs.getSelectedIndex() == 1) {
                myBorrows.refresh();
            } else {
                catalog.refreshIfSearched();
            }
        });
        return tabs;
    }
}
