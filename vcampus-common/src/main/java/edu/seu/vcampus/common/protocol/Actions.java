package edu.seu.vcampus.common.protocol;

/**
 * Public action names shared by the client and server.
 */
public final class Actions {

    public static final String PING = ActionNames.of(ModuleNames.COMMON, "PING");

    private Actions() {
    }
}
