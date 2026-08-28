package edu.seu.vcampus.client.module;

import edu.seu.vcampus.client.application.ClientContext;

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
     * @param context shared network and authenticated-session context
     * @return module root component
     */
    JComponent createView(ClientContext context);

    /**
     * Creates a role-aware module view. Existing modules keep their current
     * user view until they override this method with management controls.
     *
     * @param context shared client context
     * @param mode authenticated presentation mode
     * @return module root component
     */
    default JComponent createView(ClientContext context, ModuleViewMode mode) {
        return createView(context);
    }
}
