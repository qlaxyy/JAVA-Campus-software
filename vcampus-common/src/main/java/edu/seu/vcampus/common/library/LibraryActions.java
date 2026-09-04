package edu.seu.vcampus.common.library;

import edu.seu.vcampus.common.protocol.ActionNames;
import edu.seu.vcampus.common.protocol.ModuleNames;

/** Public actions owned by the library module. */
public final class LibraryActions {

    public static final String SEARCH_BOOKS =
            ActionNames.of(ModuleNames.LIBRARY, "SEARCH_BOOKS");

    /** Borrows one book for the authenticated user. */
    public static final String BORROW_BOOK =
            ActionNames.of(ModuleNames.LIBRARY, "BORROW_BOOK");

    /** Returns one active borrow record owned by the authenticated user. */
    public static final String RETURN_BOOK =
            ActionNames.of(ModuleNames.LIBRARY, "RETURN_BOOK");

    /** Gets current and historical records for the authenticated user; request data is null. */
    public static final String GET_BORROW_RECORDS =
            ActionNames.of(ModuleNames.LIBRARY, "GET_BORROW_RECORDS");

    private LibraryActions() {
    }
}
