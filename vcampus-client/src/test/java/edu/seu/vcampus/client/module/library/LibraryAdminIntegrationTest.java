package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.library.*;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Covers the final phase-three administrator actions through a real server. */
class LibraryAdminIntegrationTest {

    @Test
    void librarianMaintainsCatalogCopiesStatusesAndAllLibraryBorrows() throws Exception {
        try (CampusServer server = new CampusServer(0, 4)) {
            server.start();
            ClientContext librarian = login(server, "20260005");
            ClientContext reader = login(server, "20260001");
            ClientContext shopAdmin = login(server, "20260006");

            AddBookRequest addBook = new AddBookRequest("9787111000000", "阶段三测试", "测试作者",
                    "C001", "东南大学出版社", 2026, "中文");
            assertEquals(ErrorCodes.AUTH_FORBIDDEN, reader.send(LibraryActions.ADD_BOOK, addBook).getCode());
            assertEquals(ErrorCodes.AUTH_FORBIDDEN, shopAdmin.send(LibraryActions.ADD_BOOK, addBook).getCode());
            BookDTO book = data(librarian.send(LibraryActions.ADD_BOOK, addBook), BookDTO.class);
            assertEquals(0, book.getTotalCount());
            assertEquals("ACTIVE", book.getStatus());

            AddBookCopyRequest addCopy = new AddBookCopyRequest(book.getBookId(), "SEU-STAGE3-001",
                    "九龙湖校区—中文图书阅览室3", "TP312/TEST");
            BookCopyDTO copy = data(librarian.send(LibraryActions.ADD_BOOK_COPY, addCopy), BookCopyDTO.class);
            assertEquals("AVAILABLE", copy.getStatus());
            assertEquals(ErrorCodes.LIBRARY_DUPLICATE_BARCODE,
                    librarian.send(LibraryActions.ADD_BOOK_COPY, addCopy).getCode());

            UpdateBookRequest update = new UpdateBookRequest(book.getBookId(), book.getIsbn(), "阶段三测试（修订）",
                    "测试作者", "C002", "东南大学出版社", 2026, "中文");
            BookDTO edited = data(librarian.send(LibraryActions.UPDATE_BOOK, update), BookDTO.class);
            assertEquals("文学", edited.getCategoryName());
            BookCopyDTO moved = data(librarian.send(LibraryActions.UPDATE_BOOK_COPY,
                    new UpdateBookCopyRequest(copy.getCopyId(), "四牌楼校区—中文书库二楼", "I247/TEST")),
                    BookCopyDTO.class);
            assertEquals(copy.getBarcode(), moved.getBarcode());
            assertEquals(copy.getStatus(), moved.getStatus(), "generic copy editing must not bypass the state machine");

            data(librarian.send(LibraryActions.SET_BOOK_STATUS,
                    new SetBookStatusRequest(book.getBookId(), "INACTIVE")), BookDTO.class);
            assertTrue(search(reader, book.getIsbn(), LibraryActions.SEARCH_BOOKS).isEmpty());
            BookDTO inactive = search(
                    librarian, book.getIsbn(), LibraryActions.ADMIN_SEARCH_BOOKS).getFirst();
            assertEquals(1, inactive.getTotalCount());
            assertEquals(0, inactive.getAvailableCount());
            assertEquals(ErrorCodes.LIBRARY_COPY_NOT_AVAILABLE,
                    reader.send(LibraryActions.BORROW_COPY, new CopyBorrowRequest(copy.getBarcode())).getCode());
            data(librarian.send(LibraryActions.SET_BOOK_STATUS,
                    new SetBookStatusRequest(book.getBookId(), "ACTIVE")), BookDTO.class);

            assertTrue(reader.send(LibraryActions.BORROW_COPY, new CopyBorrowRequest(copy.getBarcode())).isSuccess());
            List<AdminBorrowRecordDTO> current = list(librarian.send(LibraryActions.ADMIN_QUERY_BORROWS,
                    new AdminBorrowQueryRequest(AdminBorrowQueryRequest.CURRENT)), AdminBorrowRecordDTO.class);
            assertTrue(current.stream().anyMatch(record -> copy.getBarcode().equals(record.getBarcode())
                    && "U-STUDENT-001".equals(record.getUserId())));
            assertEquals(ErrorCodes.LIBRARY_INVALID_COPY_STATUS,
                    librarian.send(LibraryActions.WITHDRAW_BOOK_COPY,
                            new BookCopyIdRequest(copy.getCopyId())).getCode());

            assertTrue(reader.send(LibraryActions.RETURN_COPY, new CopyReturnRequest(copy.getBarcode())).isSuccess());
            assertTrue(librarian.send(LibraryActions.SHELVE_BOOK_COPY,
                    new BookCopyIdRequest(copy.getCopyId())).isSuccess());
            BookCopyDTO withdrawn = data(librarian.send(LibraryActions.WITHDRAW_BOOK_COPY,
                    new BookCopyIdRequest(copy.getCopyId())), BookCopyDTO.class);
            assertEquals("WITHDRAWN", withdrawn.getStatus());
            assertEquals(0, search(librarian, book.getIsbn(), LibraryActions.ADMIN_SEARCH_BOOKS)
                    .getFirst().getTotalCount());
            assertTrue(list(librarian.send(LibraryActions.ADMIN_QUERY_BORROWS,
                    new AdminBorrowQueryRequest(AdminBorrowQueryRequest.HISTORY)), AdminBorrowRecordDTO.class)
                    .stream().anyMatch(record -> copy.getBarcode().equals(record.getBarcode())));
        }
    }

    private static List<BookDTO> search(ClientContext context, String keyword, String action) throws Exception {
        return data(context.send(action, new BookSearchRequest(keyword, null)), BookSearchResult.class).getBooks();
    }

    private static <T> T data(Response response, Class<T> type) {
        assertTrue(response.isSuccess(), response.getMessage());
        return assertInstanceOf(type, response.getData());
    }

    private static <T> List<T> list(Response response, Class<T> type) {
        assertTrue(response.isSuccess(), response.getMessage());
        List<?> values = assertInstanceOf(List.class, response.getData());
        assertTrue(values.stream().allMatch(type::isInstance));
        return values.stream().map(type::cast).toList();
    }

    private static ClientContext login(CampusServer server, String username) throws Exception {
        ClientContext context = new ClientContext(new CampusClient("127.0.0.1", server.getPort(), 2000));
        assertTrue(context.login(username, "123456".toCharArray()).isSuccess());
        return context;
    }
}
