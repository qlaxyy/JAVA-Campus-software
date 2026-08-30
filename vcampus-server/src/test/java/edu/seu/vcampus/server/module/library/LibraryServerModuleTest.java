package edu.seu.vcampus.server.module.library;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryServerModuleTest {

    private static final String TOKEN = "valid-library-test-token";

    private ActionRouter router;

    @BeforeEach
    void setUp() {
        SessionInfo session = new SessionInfo(
                TOKEN, "U-001", "student001", "演示学生", Role.USER);
        ServerContext context = new ServerContext(token ->
                TOKEN.equals(token) ? Optional.of(session) : Optional.empty());
        router = new ActionRouter();
        new LibraryServerModule().registerHandlers(router, context);
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
}
