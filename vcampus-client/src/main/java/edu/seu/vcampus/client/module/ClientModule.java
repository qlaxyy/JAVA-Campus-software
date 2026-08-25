package edu.seu.vcampus.client.module;

import edu.seu.vcampus.client.infrastructure.CampusClient;

import javax.swing.JComponent;

/**
 * Extension point for one client-side business module.
 */
public interface ClientModule {

    /** @return stable module identifier */
    String id();

    /** @return Chinese navigation label */
    String displayName();

    /**
     * Creates the module's root Swing view.
     *
     * @param client shared network client
     * @return module root component
     */
    JComponent createView(CampusClient client);
}
