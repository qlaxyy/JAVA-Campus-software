package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerModule;
import edu.seu.vcampus.server.module.ServerContext;

import java.util.Objects;

/** Server entry point owned by the library module. */
public final class LibraryServerModule implements ServerModule {

    private final LibraryService service;

    public LibraryServerModule() {
        this(new LibraryService(new InMemoryBookRepository()));
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
}
