package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;

import javax.swing.JPanel;
import java.awt.CardLayout;

/** Root view that keeps hospital pages inside the shared application window. */
public final class HospitalView extends JPanel {

    private static final String HOME = "home";
    private static final String SLOT_SEARCH = "slot-search";

    private final ClientContext context;
    private final CardLayout cards = new CardLayout();
    private final HospitalHomePanel homePanel;
    private final SlotSearchPanel slotSearchPanel;

    public HospitalView(ClientContext context) {
        this.context = context;
        setLayout(cards);
        setBackground(HospitalTheme.BACKGROUND);

        homePanel = new HospitalHomePanel(this::openSlotSearch);
        slotSearchPanel = new SlotSearchPanel(context, this::openHome);
        add(homePanel, HOME);
        add(slotSearchPanel, SLOT_SEARCH);
        cards.show(this, HOME);
    }

    private void openSlotSearch() {
        if (context.currentSession().isEmpty()) {
            homePanel.showMessage("请先到“用户”模块登录，再使用预约挂号。");
            return;
        }
        homePanel.showMessage(" ");
        cards.show(this, SLOT_SEARCH);
        slotSearchPanel.activate();
    }

    private void openHome() {
        cards.show(this, HOME);
    }
}
