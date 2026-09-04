package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookBorrowRequest;
import edu.seu.vcampus.common.library.BookReturnRequest;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerContext;
import edu.seu.vcampus.server.module.ServerModule;

import java.util.Objects;
import java.util.Optional;
import java.util.ArrayList;

/** Server entry point owned by the library module. */
public final class LibraryServerModule implements ServerModule {

    private final LibraryService service;

    /** Creates the production library module with the current repository. */
    public LibraryServerModule() {
        this(createDefaultService());
    }

    LibraryServerModule(LibraryService service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    @Override
    public String id() {
        return ModuleNames.LIBRARY;
    }

    @Override
    public void registerHandlers(ActionRouter router, ServerContext context) {
        router.register(
                LibraryActions.SEARCH_BOOKS,
                request -> searchBooks(request, context));
        router.register(
                LibraryActions.BORROW_BOOK,
                request -> borrowBook(request, context));
        router.register(LibraryActions.RETURN_BOOK, request -> returnBook(request, context));
        router.register(LibraryActions.GET_BORROW_RECORDS,
                request -> getBorrowRecords(request, context));
    }

    private Response searchBooks(Request request, ServerContext context) {
        if (context.sessions().findSession(request.getToken()).isEmpty()) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "Please log in before searching the library.");
        }
        if (!(request.getData() instanceof BookSearchRequest searchRequest)) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    "Book-search data is invalid.");
        }

        try {
            BookSearchResult result = service.searchBooks(searchRequest);
            return Response.success(
                    request,
                    "Found " + result.getBooks().size() + " matching books.",
                    result);
        } catch (IllegalArgumentException exception) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    exception.getMessage());
        }
    }

    private Response borrowBook(Request request, ServerContext context) {
        Optional<SessionInfo> session = context.sessions().findSession(request.getToken());
        if (session.isEmpty()) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "Please log in before borrowing a library book.");
        }
        if (!(request.getData() instanceof BookBorrowRequest borrowRequest)) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    "Book-borrow data is invalid.");
        }

        try {
            service.borrowBook(session.orElseThrow().getUserId(), borrowRequest);
            return Response.success(request, "Book borrowed successfully.", null);
        } catch (LibraryBusinessException exception) {
            return Response.failure(
                    request.getRequestId(),
                    exception.code(),
                    exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_INVALID_ARGUMENT,
                    exception.getMessage());
        }
    }

    private static LibraryService createDefaultService() {
        InMemoryBookRepository bookRepository = new InMemoryBookRepository();
        InMemoryBorrowRecordRepository borrowRecordRepository =
                new InMemoryBorrowRecordRepository();
        return new LibraryService(bookRepository, borrowRecordRepository);
    }

    private Response getBorrowRecords(Request request, ServerContext context) {
        Optional<SessionInfo> session = context.sessions().findSession(request.getToken());
        if (session.isEmpty()) {
            return Response.failure(request.getRequestId(), ErrorCodes.AUTH_REQUIRED,
                    "Please log in before viewing borrow records.");
        }
        if (request.getData() != null) {
            return Response.failure(request.getRequestId(), ErrorCodes.COMMON_INVALID_REQUEST,
                    "Personal borrow records do not accept a user identifier or filter payload.");
        }
        return Response.success(request, "Borrow records loaded.",
                new ArrayList<>(service.getBorrowRecords(session.orElseThrow().getUserId())));
    }

    private Response returnBook(Request request, ServerContext context) {
        Optional<SessionInfo> session = context.sessions().findSession(request.getToken());
        if (session.isEmpty()) {
            return Response.failure(request.getRequestId(), ErrorCodes.AUTH_REQUIRED,
                    "Please log in before returning a library book.");
        }
        if (!(request.getData() instanceof BookReturnRequest returnRequest)) {
            return Response.failure(request.getRequestId(), ErrorCodes.COMMON_INVALID_REQUEST,
                    "Book-return data is invalid.");
        }
        try {
            service.returnBook(session.orElseThrow().getUserId(), returnRequest);
            return Response.success(request, "Book returned successfully.", null);
        } catch (LibraryBusinessException exception) {
            return Response.failure(request.getRequestId(), exception.code(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return Response.failure(request.getRequestId(), ErrorCodes.COMMON_INVALID_ARGUMENT,
                    exception.getMessage());
        }
    }
}
