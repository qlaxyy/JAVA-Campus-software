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
    public static final String ADMIN_LIST_ACCOUNTS =
            ActionNames.of(ModuleNames.USER, "ADMIN_LIST_ACCOUNTS");
    public static final String ADMIN_CREATE_ACCOUNT =
            ActionNames.of(ModuleNames.USER, "ADMIN_CREATE_ACCOUNT");
    public static final String ADMIN_UPDATE_ACCOUNT =
            ActionNames.of(ModuleNames.USER, "ADMIN_UPDATE_ACCOUNT");
    public static final String ADMIN_UPDATE_STATUS =
            ActionNames.of(ModuleNames.USER, "ADMIN_UPDATE_STATUS");
    public static final String ADMIN_RESET_PASSWORD =
            ActionNames.of(ModuleNames.USER, "ADMIN_RESET_PASSWORD");

    private UserActions() {
    }
}
