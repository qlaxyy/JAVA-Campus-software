package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.library.BookBorrowRequest;
import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
