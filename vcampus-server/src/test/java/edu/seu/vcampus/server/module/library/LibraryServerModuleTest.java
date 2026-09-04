package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookBorrowRequest;
import edu.seu.vcampus.common.library.BookReturnRequest;
import edu.seu.vcampus.common.library.BorrowRecordDTO;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryServerModuleTest {

    private static final String TOKEN = "valid-library-test-token";

    private ActionRouter router;
    private InMemoryBookRepository books;
    private InMemoryBorrowRecordRepository records;

    @BeforeEach
    void setUp() {
        SessionInfo session = new SessionInfo(
                TOKEN, "U-001", "20260001", "演示学生", Role.USER);
        ServerContext context = new ServerContext(token ->
                TOKEN.equals(token) ? Optional.of(session) : Optional.empty());
        books = new InMemoryBookRepository();
        records = new InMemoryBorrowRecordRepository();
        router = new ActionRouter();
        new LibraryServerModule(new LibraryService(books, records))
                .registerHandlers(router, context);
    }

    @Test
    void authenticatedUserCanSearchBooksByKeyword() {
        Response response = router.dispatch(Request.create(
                LibraryActions.SEARCH_BOOKS,
                TOKEN,
                new BookSearchRequest("Java")));

        assertTrue(response.isSuccess());
        BookSearchResult result = assertInstanceOf(BookSearchResult.class, response.getData());
        assertEquals(3, result.getBooks().size());
        assertTrue(result.getBooks().stream()
                .allMatch(book -> book.getTitle().toLowerCase().contains("java")));
    }

    @Test
    void anonymousSearchIsRejected() {
        Response response = router.dispatch(Request.create(
                LibraryActions.SEARCH_BOOKS,
                null,
                new BookSearchRequest("Java")));

        assertFalse(response.isSuccess());
        assertEquals(ErrorCodes.AUTH_REQUIRED, response.getCode());
    }

    @Test
    void blankKeywordIsRejected() {
        Response response = router.dispatch(Request.create(
                LibraryActions.SEARCH_BOOKS,
                TOKEN,
                new BookSearchRequest("   ")));

        assertFalse(response.isSuccess());
        assertEquals(ErrorCodes.COMMON_INVALID_REQUEST, response.getCode());
    }

    @Test
    void authenticatedUserCanBorrowBookUsingSessionIdentity() {
        Response response = router.dispatch(Request.create(
                LibraryActions.BORROW_BOOK,
                TOKEN,
                new BookBorrowRequest("B001")));

        assertTrue(response.isSuccess());
        assertEquals(1, books.findById("B001").orElseThrow().getAvailableCount());
        BorrowRecord record = records.findBorrowedByUserId("U-001").getFirst();
        assertEquals("U-001", record.userId());
        assertEquals("B001", record.bookId());
    }

    @Test
    void anonymousBorrowIsRejected() {
        Response response = router.dispatch(Request.create(
                LibraryActions.BORROW_BOOK,
                null,
                new BookBorrowRequest("B001")));

        assertFalse(response.isSuccess());
        assertEquals(ErrorCodes.AUTH_REQUIRED, response.getCode());
        assertEquals(2, books.findById("B001").orElseThrow().getAvailableCount());
    }

    @Test
    void borrowWithWrongDtoTypeIsRejected() {
        Response response = router.dispatch(Request.create(
                LibraryActions.BORROW_BOOK,
                TOKEN,
                new BookSearchRequest("B001")));

        assertFalse(response.isSuccess());
        assertEquals(ErrorCodes.COMMON_INVALID_REQUEST, response.getCode());
    }

    @Test
    void authenticatedUserCanQueryAndReturnOwnRecord() {
        authenticatedUserCanBorrowBookUsingSessionIdentity();
        Request query = Request.create(LibraryActions.GET_BORROW_RECORDS, TOKEN, null);
        Response response = router.dispatch(query);
        assertTrue(response.isSuccess());
        assertEquals(query.getRequestId(), response.getRequestId());
        List<?> data = assertInstanceOf(List.class, response.getData());
        BorrowRecordDTO record = assertInstanceOf(BorrowRecordDTO.class, data.getFirst());
        assertEquals("BORROWED", record.getStatus());

        Response returned = router.dispatch(Request.create(LibraryActions.RETURN_BOOK,
                TOKEN, new BookReturnRequest(record.getRecordId())));
        assertTrue(returned.isSuccess());
        assertEquals(2, books.findById("B001").orElseThrow().getAvailableCount());
    }

    @Test
    void anonymousAndExpiredSessionsCannotQueryOrReturn() {
        for (String token : new String[] {null, "expired-token"}) {
            assertEquals(ErrorCodes.AUTH_REQUIRED, router.dispatch(Request.create(
                    LibraryActions.GET_BORROW_RECORDS, token, null)).getCode());
            assertEquals(ErrorCodes.AUTH_REQUIRED, router.dispatch(Request.create(
                    LibraryActions.RETURN_BOOK, token, new BookReturnRequest("R1"))).getCode());
        }
    }

    @Test
    void queryRejectsClientSuppliedUserIdentifier() {
        Response response = router.dispatch(Request.create(
                LibraryActions.GET_BORROW_RECORDS, TOKEN, "U-OTHER"));
        assertEquals(ErrorCodes.COMMON_INVALID_REQUEST, response.getCode());
    }

    @Test
    void returnRejectsWrongDtoAndBlankRecordIdentifier() {
        assertEquals(ErrorCodes.COMMON_INVALID_REQUEST, router.dispatch(Request.create(
                LibraryActions.RETURN_BOOK, TOKEN, new BookBorrowRequest("B001"))).getCode());
        assertEquals(ErrorCodes.COMMON_INVALID_REQUEST, router.dispatch(Request.create(
                LibraryActions.RETURN_BOOK, TOKEN, null)).getCode());
        assertEquals(ErrorCodes.COMMON_INVALID_ARGUMENT, router.dispatch(Request.create(
                LibraryActions.RETURN_BOOK, TOKEN, new BookReturnRequest("  "))).getCode());
    }

    @Test
    void foreignRecordIsNeitherListedNorReturnable() {
        records.save(new BorrowRecord("OTHER", "U-OTHER", "B001",
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusDays(30),
                BorrowStatus.BORROWED));
        Response response = router.dispatch(Request.create(LibraryActions.GET_BORROW_RECORDS, TOKEN, null));
        assertTrue(assertInstanceOf(List.class, response.getData()).isEmpty());
        assertEquals(ErrorCodes.LIBRARY_BORROW_RECORD_NOT_FOUND, router.dispatch(Request.create(
                LibraryActions.RETURN_BOOK, TOKEN, new BookReturnRequest("OTHER"))).getCode());
        assertEquals(2, books.findById("B001").orElseThrow().getAvailableCount());
    }

    @Test
    void repeatedReturnUsesBusinessErrorCode() {
        authenticatedUserCanBorrowBookUsingSessionIdentity();
        String id = records.findBorrowedByUserId("U-001").getFirst().recordId();
        assertTrue(router.dispatch(Request.create(LibraryActions.RETURN_BOOK,
                TOKEN, new BookReturnRequest(id))).isSuccess());
        assertEquals(ErrorCodes.LIBRARY_ALREADY_RETURNED, router.dispatch(Request.create(
                LibraryActions.RETURN_BOOK, TOKEN, new BookReturnRequest(id))).getCode());
    }
}
