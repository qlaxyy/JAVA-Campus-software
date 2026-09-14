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

    /** Inspects one barcode and reports the authenticated user's allowed terminal actions. */
    public static final String INSPECT_COPY =
            ActionNames.of(ModuleNames.LIBRARY, "INSPECT_COPY");

    /** Gets current and historical records for the authenticated user; request data is null. */
    public static final String GET_BORROW_RECORDS =
            ActionNames.of(ModuleNames.LIBRARY, "GET_BORROW_RECORDS");

    /** Renews one active borrow owned by the authenticated user. */
    public static final String RENEW_BORROW =
            ActionNames.of(ModuleNames.LIBRARY, "RENEW_BORROW");

    /**
     * Settles the outstanding fee of one borrow record owned by the authenticated user,
     * charging the campus card. Repeating the same record is a no-op once settled.
     */
    public static final String PAY_FEE =
            ActionNames.of(ModuleNames.LIBRARY, "PAY_FEE");

    /**
     * Reports one active borrow as lost. The physical copy is withdrawn and lost-book
     * compensation becomes payable in the same transaction.
     */
    public static final String REPORT_LOST =
            ActionNames.of(ModuleNames.LIBRARY, "REPORT_LOST");

    /** Creates a reservation for the authenticated user. */
    public static final String CREATE_RESERVATION =
            ActionNames.of(ModuleNames.LIBRARY, "CREATE_RESERVATION");

    /** Gets all reservations for the authenticated user; request data is null. */
    public static final String GET_MY_RESERVATIONS =
            ActionNames.of(ModuleNames.LIBRARY, "GET_MY_RESERVATIONS");

    /** Cancels one active reservation owned by the authenticated user. */
    public static final String CANCEL_RESERVATION =
            ActionNames.of(ModuleNames.LIBRARY, "CANCEL_RESERVATION");

    /** Lists server-owned categories; null request, ArrayList of BookCategoryDTO response. */
    public static final String LIST_CATEGORIES = ActionNames.of(ModuleNames.LIBRARY, "LIST_CATEGORIES");
    /** Adds a reusable book category; library administrators only. */
    public static final String ADD_BOOK_CATEGORY =
            ActionNames.of(ModuleNames.LIBRARY, "ADD_BOOK_CATEGORY");
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
    /** Restores one logically withdrawn physical copy to the available collection. */
    public static final String RESTORE_BOOK_COPY =
            ActionNames.of(ModuleNames.LIBRARY, "RESTORE_BOOK_COPY");
    /** Queries all-library current, history or overdue borrow records. */
    public static final String ADMIN_QUERY_BORROWS =
            ActionNames.of(ModuleNames.LIBRARY, "ADMIN_QUERY_BORROWS");
    /** Queries the reservation queue across all readers; library administrators only. */
    public static final String ADMIN_QUERY_RESERVATIONS =
            ActionNames.of(ModuleNames.LIBRARY, "ADMIN_QUERY_RESERVATIONS");

    private LibraryActions() {
    }
}
