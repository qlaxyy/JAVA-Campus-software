package edu.seu.vcampus.common.user;

import edu.seu.vcampus.common.protocol.ActionNames;
import edu.seu.vcampus.common.protocol.ModuleNames;

/**
 * Public actions owned by the user-management module.
 */
public final class UserActions {

    public static final String LOGIN = ActionNames.of(ModuleNames.USER, "LOGIN");
    public static final String LOGOUT = ActionNames.of(ModuleNames.USER, "LOGOUT");
    public static final String CURRENT_SESSION = ActionNames.of(ModuleNames.USER, "CURRENT_SESSION");

    private UserActions() {
    }
}
