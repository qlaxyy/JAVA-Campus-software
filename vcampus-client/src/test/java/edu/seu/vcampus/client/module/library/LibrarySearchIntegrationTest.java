package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.library.BookBorrowRequest;
import edu.seu.vcampus.common.library.BookReturnRequest;
import edu.seu.vcampus.common.library.BorrowRecordDTO;
import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LibrarySearchIntegrationTest {

    @Test
    void loggedInClientReceivesSearchResultsThroughSocket() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            Response login = context.login("student001", "123456".toCharArray());
            assertTrue(login.isSuccess());

            Response search = context.send(
                    LibraryActions.SEARCH_BOOKS,
                    new BookSearchRequest("Java"));

            assertTrue(search.isSuccess());
            BookSearchResult result = assertInstanceOf(BookSearchResult.class, search.getData());
            assertEquals(3, result.getBooks().size());
        }
    }

    @Test
    void loggedInClientBorrowsBookAndSeesUpdatedStockThroughSocket() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("student001", "123456".toCharArray()).isSuccess());

            Response borrow = context.send(
                    LibraryActions.BORROW_BOOK,
                    new BookBorrowRequest("B001"));
            Response search = context.send(
                    LibraryActions.SEARCH_BOOKS,
                    new BookSearchRequest("9787111213826"));

            assertTrue(borrow.isSuccess());
            BookSearchResult result = assertInstanceOf(BookSearchResult.class, search.getData());
            BookDTO book = result.getBooks().getFirst();
            assertEquals("B001", book.getBookId());
            assertEquals(1, book.getAvailableCount());
        }
    }

    @Test
    void borrowQueryReturnAndHistorySurviveSocketSerialization() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = login(server, "student001");
            ClientContext otherUser = login(server, "teacher001");
            assertTrue(context.send(LibraryActions.BORROW_BOOK, new BookBorrowRequest("B001")).isSuccess());
            Response query = context.send(LibraryActions.GET_BORROW_RECORDS, null);
            assertTrue(query.isSuccess());
            BorrowRecordDTO record = assertInstanceOf(BorrowRecordDTO.class,
                    assertInstanceOf(List.class, query.getData()).getFirst());
            assertEquals("BORROWED", record.getStatus());
            assertNull(record.getReturnTime());
            assertEquals(record.getBorrowTime().plusDays(30), record.getDueTime());
            assertTrue(assertInstanceOf(List.class,
                    otherUser.send(LibraryActions.GET_BORROW_RECORDS, null).getData()).isEmpty());
            assertEquals(ErrorCodes.LIBRARY_BORROW_RECORD_NOT_FOUND,
                    otherUser.send(LibraryActions.RETURN_BOOK,
                            new BookReturnRequest(record.getRecordId())).getCode());

            assertTrue(context.send(LibraryActions.RETURN_BOOK,
                    new BookReturnRequest(record.getRecordId())).isSuccess());
            BorrowRecordDTO history = assertInstanceOf(BorrowRecordDTO.class,
                    assertInstanceOf(List.class,
                            context.send(LibraryActions.GET_BORROW_RECORDS, null).getData()).getFirst());
            assertEquals(record.getRecordId(), history.getRecordId());
            assertEquals("RETURNED", history.getStatus());
            assertNotNull(history.getReturnTime());
            assertFalse(history.isOverdue());
            assertEquals(ErrorCodes.LIBRARY_ALREADY_RETURNED,
                    context.send(LibraryActions.RETURN_BOOK,
                            new BookReturnRequest(record.getRecordId())).getCode());
            BookSearchResult search = assertInstanceOf(BookSearchResult.class,
                    context.send(LibraryActions.SEARCH_BOOKS,
                            new BookSearchRequest("9787111213826")).getData());
            assertEquals(2, search.getBooks().getFirst().getAvailableCount());
            assertTrue(context.send(LibraryActions.BORROW_BOOK, new BookBorrowRequest("B001")).isSuccess());
            assertEquals(2, assertInstanceOf(List.class,
                    context.send(LibraryActions.GET_BORROW_RECORDS, null).getData()).size());
        }
    }

    private ClientContext login(CampusServer server, String username) throws Exception {
        ClientContext context = new ClientContext(new CampusClient("127.0.0.1", server.getPort()));
        assertTrue(context.login(username, "123456".toCharArray()).isSuccess());
        return context;
    }
}
