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

/** Verifies the reader's phase-three workflow across real Socket serialization. */
class LibrarySearchIntegrationTest {

    @Test
    void searchBarcodeCirculationAndPersonalHistoryRoundTripThroughSocket() throws Exception {
        try (CampusServer server = new CampusServer(0, 3)) {
            server.start();
            ClientContext reader = login(server, "student001");
            ClientContext other = login(server, "teacher001");
            ClientContext librarian = login(server, "libraryadmin");

            BookDTO before = search(reader, "9787111213826").getFirst();
            int availableBefore = before.getAvailableCount();
            String barcode = "SEU-B001-001";

            Response borrowed = reader.send(LibraryActions.BORROW_COPY, new CopyBorrowRequest(barcode));
            assertTrue(borrowed.isSuccess(), borrowed.getMessage());
            BorrowRecordDTO current = list(reader.send(LibraryActions.GET_BORROW_RECORDS, null),
                    BorrowRecordDTO.class).getFirst();
            assertEquals(barcode, current.getBarcode());
            assertEquals(current.getBorrowTime().plusDays(30), current.getDueTime());
            assertEquals(availableBefore - 1, search(reader, "9787111213826").getFirst().getAvailableCount());

            List<BorrowRecordDTO> records = list(reader.send(LibraryActions.GET_BORROW_RECORDS, null),
                    BorrowRecordDTO.class);
            assertEquals(1, records.size());
            assertEquals("BORROWED", records.getFirst().getStatus());
            assertTrue(list(other.send(LibraryActions.GET_BORROW_RECORDS, null), BorrowRecordDTO.class).isEmpty());
            assertEquals(ErrorCodes.LIBRARY_BORROW_RECORD_NOT_FOUND,
                    other.send(LibraryActions.RETURN_COPY, new CopyReturnRequest(barcode)).getCode());

            assertTrue(reader.send(LibraryActions.RETURN_COPY, new CopyReturnRequest(barcode)).isSuccess());
            BorrowRecordDTO returned = list(reader.send(LibraryActions.GET_BORROW_RECORDS, null),
                    BorrowRecordDTO.class).getFirst();
            assertEquals("RETURNED", returned.getStatus());
            assertNotNull(returned.getReturnTime());
            assertEquals(availableBefore - 1, search(reader, "9787111213826").getFirst().getAvailableCount(),
                    "returned copies remain unavailable until shelving");
            assertEquals(ErrorCodes.LIBRARY_COPY_NOT_AVAILABLE,
                    reader.send(LibraryActions.BORROW_COPY, new CopyBorrowRequest(barcode)).getCode());

            BookCopyDTO copy = list(librarian.send(LibraryActions.LIST_BOOK_COPIES,
                    new ListBookCopiesRequest("B001")), BookCopyDTO.class).stream()
                    .filter(value -> barcode.equals(value.getBarcode())).findFirst().orElseThrow();
            assertEquals("WAITING_SHELVING", copy.getStatus());
            assertTrue(librarian.send(LibraryActions.SHELVE_BOOK_COPY,
                    new BookCopyIdRequest(copy.getCopyId())).isSuccess());
            assertEquals(availableBefore, search(reader, "9787111213826").getFirst().getAvailableCount());
        }
    }

    private static List<BookDTO> search(ClientContext context, String keyword) throws Exception {
        Response response = context.send(LibraryActions.SEARCH_BOOKS, new BookSearchRequest(keyword, null));
        assertTrue(response.isSuccess(), response.getMessage());
        return assertInstanceOf(BookSearchResult.class, response.getData()).getBooks();
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
