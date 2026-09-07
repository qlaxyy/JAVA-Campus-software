package edu.seu.vcampus.common.library;

import edu.seu.vcampus.common.protocol.ActionNames;
import edu.seu.vcampus.common.protocol.ModuleNames;

/** Public actions owned by the library module. */
public final class LibraryActions {

    public static final String SEARCH_BOOKS =
            ActionNames.of(ModuleNames.LIBRARY, "SEARCH_BOOKS");

    /** Searches all catalog records, including inactive ones; library administrators only. */
    public static final String ADMIN_SEARCH_BOOKS =
            ActionNames.of(ModuleNames.LIBRARY, "ADMIN_SEARCH_BOOKS");

    /** Borrows the physical copy identified by a scanned barcode. */
    public static final String BORROW_COPY =
            ActionNames.of(ModuleNames.LIBRARY, "BORROW_COPY");

    /** Returns the physical copy identified by a scanned barcode. */
    public static final String RETURN_COPY =
            ActionNames.of(ModuleNames.LIBRARY, "RETURN_COPY");

    /** Gets current and historical records for the authenticated user; request data is null. */
    public static final String GET_BORROW_RECORDS =
            ActionNames.of(ModuleNames.LIBRARY, "GET_BORROW_RECORDS");

    /** Lists server-owned categories; null request, ArrayList of BookCategoryDTO response. */
    public static final String LIST_CATEGORIES = ActionNames.of(ModuleNames.LIBRARY, "LIST_CATEGORIES");
    /** Adds a book for a library administrator; AddBookRequest -> BookDTO. */
    public static final String ADD_BOOK = ActionNames.of(ModuleNames.LIBRARY, "ADD_BOOK");
    /** Edits metadata for a library administrator; UpdateBookRequest -> BookDTO. */
    public static final String UPDATE_BOOK = ActionNames.of(ModuleNames.LIBRARY, "UPDATE_BOOK");
    /** Changes a catalog record between ACTIVE and INACTIVE. */
    public static final String SET_BOOK_STATUS =
            ActionNames.of(ModuleNames.LIBRARY, "SET_BOOK_STATUS");
    /** Registers one physical copy. */
    public static final String ADD_BOOK_COPY =
            ActionNames.of(ModuleNames.LIBRARY, "ADD_BOOK_COPY");
    /** Lists all physical copies of one catalog record. */
    public static final String LIST_BOOK_COPIES =
            ActionNames.of(ModuleNames.LIBRARY, "LIST_BOOK_COPIES");
    /** Edits the location and call number of one physical copy. */
    public static final String UPDATE_BOOK_COPY =
            ActionNames.of(ModuleNames.LIBRARY, "UPDATE_BOOK_COPY");
    /** Confirms that a returned physical copy is back on its shelf. */
    public static final String SHELVE_BOOK_COPY =
            ActionNames.of(ModuleNames.LIBRARY, "SHELVE_BOOK_COPY");
    /** Logically withdraws one physical copy. */
    public static final String WITHDRAW_BOOK_COPY =
            ActionNames.of(ModuleNames.LIBRARY, "WITHDRAW_BOOK_COPY");
    /** Queries all-library current, history or overdue borrow records. */
    public static final String ADMIN_QUERY_BORROWS =
            ActionNames.of(ModuleNames.LIBRARY, "ADMIN_QUERY_BORROWS");

    private LibraryActions() {
    }
}
