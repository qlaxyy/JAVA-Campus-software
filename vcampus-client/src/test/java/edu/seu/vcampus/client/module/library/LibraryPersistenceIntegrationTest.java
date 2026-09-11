package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.library.BookCopyDTO;
import edu.seu.vcampus.common.library.BookCopyIdRequest;
import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.library.BorrowRecordDTO;
import edu.seu.vcampus.common.library.CopyBorrowRequest;
import edu.seu.vcampus.common.library.CopyReturnRequest;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.library.ListBookCopiesRequest;
import edu.seu.vcampus.common.library.ReservationDTO;
import edu.seu.vcampus.common.library.ReservationIdRequest;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import edu.seu.vcampus.server.module.ServerModules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies Access persistence through the production router and real Socket transport. */
class LibraryPersistenceIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void circulationStateSurvivesTwoServerRestarts() throws Exception {
        Path database = temporaryDirectory.resolve("persistent-library.accdb");
        String barcode = "SEU-B004-001";
        int availableBefore;

        try (CampusServer first = persistentServer(database)) {
            ClientContext reader = login(first, "20260001");
            availableBefore = search(reader, "9787020002207").getAvailableCount();
            Response borrowed = reader.send(
                    LibraryActions.BORROW_COPY, new CopyBorrowRequest(barcode));
            assertTrue(borrowed.isSuccess(), borrowed.getMessage());
        }

        try (CampusServer second = persistentServer(database)) {
            ClientContext reader = login(second, "20260001");
            BorrowRecordDTO current = recordWithBarcode(records(reader), barcode);
            assertEquals("BORROWED", current.getStatus());
            assertEquals(barcode, current.getBarcode());
            assertEquals(availableBefore - 1,
                    search(reader, "9787020002207").getAvailableCount());
            Response returned = reader.send(
                    LibraryActions.RETURN_COPY, new CopyReturnRequest(barcode));
            assertTrue(returned.isSuccess(), returned.getMessage());
        }

        try (CampusServer third = persistentServer(database)) {
            ClientContext reader = login(third, "20260001");
            assertEquals("RETURNED",
                    recordWithBarcode(records(reader), barcode).getStatus());
            assertEquals(availableBefore - 1,
                    search(reader, "9787020002207").getAvailableCount(),
                    "a returned copy is unavailable until a librarian shelves it");

            ClientContext librarian = login(third, "20260005");
            BookCopyDTO copy = copies(librarian, "B004").stream()
                    .filter(value -> barcode.equals(value.getBarcode()))
                    .findFirst().orElseThrow();
            assertEquals("WAITING_SHELVING", copy.getStatus());
            Response shelved = librarian.send(
                    LibraryActions.SHELVE_BOOK_COPY,
                    new BookCopyIdRequest(copy.getCopyId()));
            assertTrue(shelved.isSuccess(), shelved.getMessage());
            assertEquals(availableBefore,
                    search(reader, "9787020002207").getAvailableCount());
        }

        assertTrue(Files.exists(database));
    }

    @Test
    void freshDatabaseSeedsAndPreservesCoherentDemonstrationActivity() throws Exception {
        Path database = temporaryDirectory.resolve("demonstration-library.accdb");
        String reservationId;

        try (CampusServer first = persistentServer(database)) {
            ClientContext reader = login(first, "20260001");
            List<BorrowRecordDTO> records = records(reader);
            assertEquals("BORROWED",
                    recordWithBarcode(records, "SEU-B001-001").getStatus());
            assertEquals("RETURNED",
                    recordWithBarcode(records, "SEU-B002-001").getStatus());

            ReservationDTO ready = reservations(reader).stream()
                    .filter(value -> "READY_FOR_PICKUP".equals(value.getStatus()))
                    .findFirst().orElseThrow();
            reservationId = ready.getReservationId();
            assertEquals("SEU-B003-001", ready.getAssignedBarcode());

            ClientContext librarian = login(first, "20260005");
            assertEquals("LOANED", copyWithBarcode(
                    copies(librarian, "B001"), "SEU-B001-001").getStatus());
            assertEquals("AVAILABLE", copyWithBarcode(
                    copies(librarian, "B002"), "SEU-B002-001").getStatus());
            assertEquals("RESERVED", copyWithBarcode(
                    copies(librarian, "B003"), "SEU-B003-001").getStatus());
        }

        try (CampusServer second = persistentServer(database)) {
            ClientContext reader = login(second, "20260001");
            ReservationDTO ready = reservationWithId(reservations(reader), reservationId);
            assertEquals("READY_FOR_PICKUP", ready.getStatus());
            assertEquals("SEU-B003-001", ready.getAssignedBarcode());

            ClientContext librarian = login(second, "20260005");
            assertEquals("RESERVED", copyWithBarcode(
                    copies(librarian, "B003"), "SEU-B003-001").getStatus());

            Response canceled = reader.send(
                    LibraryActions.CANCEL_RESERVATION,
                    new ReservationIdRequest(reservationId));
            assertTrue(canceled.isSuccess(), canceled.getMessage());
        }

        try (CampusServer third = persistentServer(database)) {
            ClientContext reader = login(third, "20260001");
            assertEquals("CANCELED",
                    reservationWithId(reservations(reader), reservationId).getStatus(),
                    "an existing database must not recreate the ready reservation");
            assertEquals(2, records(reader).size(),
                    "an existing database must not duplicate demonstration borrows");

            ClientContext librarian = login(third, "20260005");
            assertEquals("AVAILABLE", copyWithBarcode(
                    copies(librarian, "B003"), "SEU-B003-001").getStatus());
        }
    }

    private CampusServer persistentServer(Path database) throws Exception {
        CampusServer server = new CampusServer(
                0, 3, ServerModules.createPersistentRouter(database));
        server.start();
        return server;
    }

    private ClientContext login(CampusServer server, String username) throws Exception {
        ClientContext context = new ClientContext(
                new CampusClient("127.0.0.1", server.getPort(), 5_000));
        Response response = context.login(username, "123456".toCharArray());
        assertTrue(response.isSuccess(), response.getMessage());
        return context;
    }

    private BookDTO search(ClientContext context, String keyword) throws Exception {
        Response response = context.send(
                LibraryActions.SEARCH_BOOKS, new BookSearchRequest(keyword, null));
        assertTrue(response.isSuccess(), response.getMessage());
        return assertInstanceOf(BookSearchResult.class, response.getData())
                .getBooks().getFirst();
    }

    private List<BorrowRecordDTO> records(ClientContext context) throws Exception {
        return list(context.send(LibraryActions.GET_BORROW_RECORDS, null),
                BorrowRecordDTO.class);
    }

    private List<ReservationDTO> reservations(ClientContext context) throws Exception {
        return list(context.send(LibraryActions.GET_MY_RESERVATIONS, null),
                ReservationDTO.class);
    }

    private List<BookCopyDTO> copies(ClientContext context, String bookId) throws Exception {
        return list(context.send(
                LibraryActions.LIST_BOOK_COPIES, new ListBookCopiesRequest(bookId)),
                BookCopyDTO.class);
    }

    private <T> List<T> list(Response response, Class<T> type) {
        assertTrue(response.isSuccess(), response.getMessage());
        List<?> values = assertInstanceOf(List.class, response.getData());
        assertTrue(values.stream().allMatch(type::isInstance));
        return values.stream().map(type::cast).toList();
    }

    private BorrowRecordDTO recordWithBarcode(
            List<BorrowRecordDTO> records, String barcode) {
        return records.stream()
                .filter(record -> barcode.equals(record.getBarcode()))
                .findFirst().orElseThrow();
    }

    private ReservationDTO reservationWithId(
            List<ReservationDTO> reservations, String reservationId) {
        return reservations.stream()
                .filter(value -> reservationId.equals(value.getReservationId()))
                .findFirst().orElseThrow();
    }

    private BookCopyDTO copyWithBarcode(List<BookCopyDTO> copies, String barcode) {
        return copies.stream()
                .filter(copy -> barcode.equals(copy.getBarcode()))
                .findFirst().orElseThrow();
    }
}
