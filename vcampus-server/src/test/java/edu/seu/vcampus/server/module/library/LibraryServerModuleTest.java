package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.*;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LibraryServerModuleTest {

    private static final SessionInfo READER = new SessionInfo("reader-token", "U-1", "reader", "读者", Role.USER);
    private static final SessionInfo ADMIN = new SessionInfo("admin-token", "A-1", "libraryadmin", "管理员",
            Role.USER, Set.of(AdminScope.LIBRARY));

    @Test
    void publicActionContractContainsTheReservationCoreActions() throws Exception {
        Set<String> actions = Arrays.stream(LibraryActions.class.getDeclaredFields())
                .filter(field -> Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())
                        && field.getType() == String.class)
                .map(this::read).collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of(LibraryActions.SEARCH_BOOKS, LibraryActions.GET_BORROW_RECORDS,
                LibraryActions.BORROW_COPY, LibraryActions.RETURN_COPY, LibraryActions.LIST_CATEGORIES,
                LibraryActions.ADMIN_SEARCH_BOOKS, LibraryActions.ADD_BOOK, LibraryActions.UPDATE_BOOK,
                LibraryActions.SET_BOOK_STATUS, LibraryActions.ADD_BOOK_COPY, LibraryActions.LIST_BOOK_COPIES,
                LibraryActions.UPDATE_BOOK_COPY, LibraryActions.SHELVE_BOOK_COPY,
                LibraryActions.WITHDRAW_BOOK_COPY, LibraryActions.RESTORE_BOOK_COPY,
                LibraryActions.ADMIN_QUERY_BORROWS, LibraryActions.CREATE_RESERVATION,
                LibraryActions.GET_MY_RESERVATIONS,
                LibraryActions.CANCEL_RESERVATION), actions);
    }

    @Test
    void handlersAuthenticateValidateDtoAndAuthorizeEveryAdminAction() {
        ActionRouter router = router();
        assertEquals(ErrorCodes.AUTH_REQUIRED, dispatch(router, LibraryActions.SEARCH_BOOKS,
                null, new BookSearchRequest("", null)).getCode());
        assertEquals(ErrorCodes.AUTH_REQUIRED, dispatch(router, LibraryActions.GET_BORROW_RECORDS,
                "expired-token", null).getCode());
        assertEquals(ErrorCodes.COMMON_INVALID_REQUEST, dispatch(router, LibraryActions.SEARCH_BOOKS,
                READER.getToken(), new CopyBorrowRequest("SEU-B001-001")).getCode());
        assertEquals(ErrorCodes.COMMON_INVALID_REQUEST, dispatch(router, LibraryActions.GET_BORROW_RECORDS,
                READER.getToken(), "forged-user-id").getCode());
        assertEquals(ErrorCodes.AUTH_REQUIRED, dispatch(router,
                LibraryActions.CREATE_RESERVATION, null,
                new CreateReservationRequest("B001", "九龙湖校区—中文图书阅览室3"))
                .getCode());
        assertEquals(ErrorCodes.COMMON_INVALID_REQUEST, dispatch(router,
                LibraryActions.CREATE_RESERVATION, READER.getToken(),
                new BookSearchRequest("", null)).getCode());
        assertEquals(ErrorCodes.COMMON_INVALID_REQUEST, dispatch(router,
                LibraryActions.GET_MY_RESERVATIONS, READER.getToken(),
                "forged-user-id").getCode());
        assertTrue(dispatch(router, LibraryActions.LIST_CATEGORIES, READER.getToken(), null).isSuccess());

        AddBookRequest add = new AddBookRequest("9787111000000", "测试", "作者", "C001", "", null, "");
        assertEquals(ErrorCodes.AUTH_FORBIDDEN,
                dispatch(router, LibraryActions.ADD_BOOK, READER.getToken(), add).getCode());
        assertEquals(ErrorCodes.AUTH_REQUIRED,
                dispatch(router, LibraryActions.ADMIN_QUERY_BORROWS, null,
                        new AdminBorrowQueryRequest(AdminBorrowQueryRequest.CURRENT)).getCode());
        assertEquals(ErrorCodes.COMMON_INVALID_REQUEST,
                dispatch(router, LibraryActions.ADD_BOOK, ADMIN.getToken(), new BookSearchRequest("", null)).getCode());
        assertTrue(dispatch(router, LibraryActions.ADD_BOOK, ADMIN.getToken(), add).isSuccess());

        assertTrue(dispatch(router, LibraryActions.WITHDRAW_BOOK_COPY, ADMIN.getToken(),
                new BookCopyIdRequest("CP-B001-001")).isSuccess());
        Response editWithdrawn = dispatch(router, LibraryActions.UPDATE_BOOK_COPY,
                ADMIN.getToken(), new UpdateBookCopyRequest(
                        "CP-B001-001", "九龙湖校区", "TP312/EDIT"));
        assertEquals(ErrorCodes.LIBRARY_INVALID_COPY_STATUS, editWithdrawn.getCode());
        assertEquals(ErrorCodes.AUTH_FORBIDDEN,
                dispatch(router, LibraryActions.RESTORE_BOOK_COPY, READER.getToken(),
                        new BookCopyIdRequest("CP-B001-001")).getCode());
        assertTrue(dispatch(router, LibraryActions.RESTORE_BOOK_COPY, ADMIN.getToken(),
                new BookCopyIdRequest("CP-B001-001")).isSuccess());
    }

    @Test
    void sessionIdentityRatherThanRequestPayloadOwnsBarcodeCirculation() {
        ActionRouter router = router();
        assertTrue(dispatch(router, LibraryActions.BORROW_COPY, READER.getToken(),
                new CopyBorrowRequest("SEU-B001-001")).isSuccess());
        Response records = dispatch(router, LibraryActions.GET_BORROW_RECORDS, READER.getToken(), null);
        assertTrue(records.isSuccess());
        BorrowRecordDTO record = assertInstanceOf(BorrowRecordDTO.class,
                assertInstanceOf(java.util.List.class, records.getData()).getFirst());
        assertEquals("SEU-B001-001", record.getBarcode());

        Response created = dispatch(router, LibraryActions.CREATE_RESERVATION,
                READER.getToken(), new CreateReservationRequest(
                        "B002", "九龙湖校区—中文图书阅览室3"));
        assertTrue(created.isSuccess());
        ReservationDTO reservation = assertInstanceOf(ReservationDTO.class, created.getData());
        Response mine = dispatch(router, LibraryActions.GET_MY_RESERVATIONS,
                READER.getToken(), null);
        ReservationDTO loaded = assertInstanceOf(ReservationDTO.class,
                assertInstanceOf(java.util.List.class, mine.getData()).getFirst());
        assertEquals(reservation.getReservationId(), loaded.getReservationId());
        assertTrue(dispatch(router, LibraryActions.CANCEL_RESERVATION,
                READER.getToken(), new ReservationIdRequest(
                        reservation.getReservationId())).isSuccess());
    }

    private static ActionRouter router() {
        ActionRouter router = new ActionRouter();
        new LibraryServerModule().registerHandlers(router, new ServerContext(token -> Optional.ofNullable(
                READER.getToken().equals(token) ? READER : ADMIN.getToken().equals(token) ? ADMIN : null)));
        return router;
    }

    private static Response dispatch(ActionRouter router, String action, String token, java.io.Serializable data) {
        return router.dispatch(Request.create(action, token, data));
    }

    private String read(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException exception) {
            throw new AssertionError(exception);
        }
    }
}
