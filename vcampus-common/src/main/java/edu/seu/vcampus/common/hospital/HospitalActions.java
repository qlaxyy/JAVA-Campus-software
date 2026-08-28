package edu.seu.vcampus.common.hospital;

import edu.seu.vcampus.common.protocol.ActionNames;
import edu.seu.vcampus.common.protocol.ModuleNames;

/** Public actions owned by the hospital module. */
public final class HospitalActions {

    public static final String LIST_DEPARTMENTS =
            ActionNames.of(ModuleNames.HOSPITAL, "LIST_DEPARTMENTS");
    public static final String SEARCH_SLOTS =
            ActionNames.of(ModuleNames.HOSPITAL, "SEARCH_SLOTS");

    private HospitalActions() {
    }
}
