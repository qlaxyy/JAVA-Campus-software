package edu.seu.vcampus.common.library;

import edu.seu.vcampus.common.protocol.ActionNames;
import edu.seu.vcampus.common.protocol.ModuleNames;

/** Public actions owned by the library module. */
public final class LibraryActions {

    public static final String SEARCH_BOOKS =
            ActionNames.of(ModuleNames.LIBRARY, "SEARCH_BOOKS");

    private LibraryActions() {
    }
}
