package edu.seu.vcampus.client.module.card;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.common.protocol.ModuleNames;

import javax.swing.JComponent;

/** Client entry for the independent campus-card subsystem. */
public final class CardClientModule implements ClientModule {

    @Override
    public String id() {
        return ModuleNames.CARD;
    }

    @Override
    public String displayName() {
        return "校园卡";
    }

    @Override
    public JComponent createView(ClientContext context) {
        return new CardWalletPanel(context);
    }
}
