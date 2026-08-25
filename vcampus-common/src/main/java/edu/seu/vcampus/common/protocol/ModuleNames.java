package edu.seu.vcampus.common.protocol;

import java.util.Set;

/**
 * Stable module identifiers used as action prefixes and package names.
 */
public final class ModuleNames {

    public static final String COMMON = "COMMON";
    public static final String USER = "USER";
    public static final String STUDENT = "STUDENT";
    public static final String COURSE = "COURSE";
    public static final String LIBRARY = "LIBRARY";
    public static final String SHOP = "SHOP";
    public static final String HOSPITAL = "HOSPITAL";

    private static final Set<String> SUPPORTED = Set.of(
            COMMON, USER, STUDENT, COURSE, LIBRARY, SHOP, HOSPITAL);

    private ModuleNames() {
    }

    /**
     * Reports whether a module identifier belongs to the agreed project scope.
     *
     * @param module module identifier
     * @return {@code true} for a known module
     */
    public static boolean isSupported(String module) {
        return SUPPORTED.contains(module);
    }
}
