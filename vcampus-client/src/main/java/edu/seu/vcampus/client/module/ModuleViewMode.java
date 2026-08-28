package edu.seu.vcampus.client.module;

/**
 * Presentation mode selected after server-authenticated role resolution.
 */
public enum ModuleViewMode {
    USER("用户模式"),
    MANAGEMENT("管理模式");

    private final String displayName;

    ModuleViewMode(String displayName) {
        this.displayName = displayName;
    }

    /** @return Chinese label shown in the module toolbar */
    public String displayName() {
        return displayName;
    }
}
