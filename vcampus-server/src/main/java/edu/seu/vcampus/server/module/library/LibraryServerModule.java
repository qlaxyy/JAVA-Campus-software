package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.AddBookCopyRequest;
import edu.seu.vcampus.common.library.AddBookRequest;
import edu.seu.vcampus.common.library.AdminBorrowQueryRequest;
import edu.seu.vcampus.common.library.BookCopyIdRequest;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.library.CopyBorrowRequest;
import edu.seu.vcampus.common.library.CopyReturnRequest;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.library.ListBookCopiesRequest;
import edu.seu.vcampus.common.library.SetBookStatusRequest;
import edu.seu.vcampus.common.library.UpdateBookCopyRequest;
import edu.seu.vcampus.common.library.UpdateBookRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import edu.seu.vcampus.server.module.ServerContext;
import edu.seu.vcampus.server.module.ServerModule;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

/** Server entry point owned by the library module. */
public final class LibraryServerModule implements ServerModule {

    private final LibraryService service;

    /** Creates the production library module with the current repositories. */
    public LibraryServerModule() {
        this(createDefaultService());
    }

    /** Creates the production library module backed by the shared Access database. */
    public static LibraryServerModule createAccessBacked(Path databasePath) {
        AccessLibraryStore store = new AccessLibraryStore(new AccessDatabase(databasePath));
        BookRepository books = new AccessBookRepository(store);
        BorrowRecordRepository records = new AccessBorrowRecordRepository(store);
        BookCategoryRepository categories = new AccessBookCategoryRepository(store);
        BookCopyRepository copies = new AccessBookCopyRepository(store);
        LibraryService service = new LibraryService(
                books,
                records,
                Clock.systemDefaultZone(),
                () -> UUID.randomUUID().toString(),
                categories,
                copies,
                store);
        return new LibraryServerModule(service);
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
        router.register(LibraryActions.SEARCH_BOOKS,
                request -> searchBooks(request, context));
        router.register(LibraryActions.BORROW_COPY,
                request -> borrowCopy(request, context));
        router.register(LibraryActions.RETURN_COPY,
                request -> returnCopy(request, context));
        router.register(LibraryActions.GET_BORROW_RECORDS,
                request -> getBorrowRecords(request, context));
        router.register(LibraryActions.LIST_CATEGORIES,
                request -> listCategories(request, context));

        router.register(LibraryActions.ADMIN_SEARCH_BOOKS, request -> administer(
                request, context, BookSearchRequest.class, service::searchBooksForAdmin));
        router.register(LibraryActions.ADD_BOOK, request -> administer(
                request, context, AddBookRequest.class, service::addBook));
        router.register(LibraryActions.UPDATE_BOOK, request -> administer(
                request, context, UpdateBookRequest.class, service::updateBook));
        router.register(LibraryActions.SET_BOOK_STATUS, request -> administer(
                request, context, SetBookStatusRequest.class, service::setBookStatus));
        router.register(LibraryActions.ADD_BOOK_COPY, request -> administer(
                request, context, AddBookCopyRequest.class, service::addBookCopy));
        router.register(LibraryActions.LIST_BOOK_COPIES, request -> administer(
                request, context, ListBookCopiesRequest.class,
                (actor, data) -> new ArrayList<>(service.listBookCopies(actor, data))));
        router.register(LibraryActions.UPDATE_BOOK_COPY, request -> administer(
                request, context, UpdateBookCopyRequest.class, service::updateBookCopy));
        router.register(LibraryActions.SHELVE_BOOK_COPY, request -> administer(
                request, context, BookCopyIdRequest.class, service::shelveBookCopy));
        router.register(LibraryActions.WITHDRAW_BOOK_COPY, request -> administer(
                request, context, BookCopyIdRequest.class, service::withdrawBookCopy));
        router.register(LibraryActions.ADMIN_QUERY_BORROWS, request -> administer(
                request, context, AdminBorrowQueryRequest.class,
                (actor, data) -> new ArrayList<>(service.queryBorrows(actor, data))));
    }

    private Response searchBooks(Request request, ServerContext context) {
        if (session(request, context).isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof BookSearchRequest searchRequest)) {
            return invalidRequest(request, "图书检索请求格式不正确");
        }
        try {
            BookSearchResult result = service.searchBooks(searchRequest);
            return Response.success(request, "找到 " + result.getBooks().size() + " 本图书", result);
        } catch (LibraryBusinessException exception) {
            return businessFailure(request, exception);
        } catch (IllegalArgumentException exception) {
            return invalidArgument(request, exception);
        }
    }

    private Response borrowCopy(Request request, ServerContext context) {
        Optional<SessionInfo> session = session(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof CopyBorrowRequest data)) {
            return invalidRequest(request, "条码借书请求格式不正确");
        }
        try {
            service.borrowCopy(session.orElseThrow().getUserId(), data);
            return Response.success(request, "借书登记成功", null);
        } catch (LibraryBusinessException exception) {
            return businessFailure(request, exception);
        } catch (IllegalArgumentException exception) {
            return invalidArgument(request, exception);
        }
    }

    private Response returnCopy(Request request, ServerContext context) {
        Optional<SessionInfo> session = session(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof CopyReturnRequest data)) {
            return invalidRequest(request, "条码归还请求格式不正确");
        }
        try {
            service.returnCopy(session.orElseThrow().getUserId(), data);
            return Response.success(request, "归还成功，单册等待管理员上架", null);
        } catch (LibraryBusinessException exception) {
            return businessFailure(request, exception);
        } catch (IllegalArgumentException exception) {
            return invalidArgument(request, exception);
        }
    }

    private Response getBorrowRecords(Request request, ServerContext context) {
        Optional<SessionInfo> session = session(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (request.getData() != null) {
            return invalidRequest(request, "个人借阅记录查询不接受用户编号或筛选参数");
        }
        return Response.success(request, "借阅记录已加载",
                new ArrayList<>(service.getBorrowRecords(session.orElseThrow().getUserId())));
    }

    private Response listCategories(Request request, ServerContext context) {
        if (session(request, context).isEmpty()) {
            return authenticationRequired(request);
        }
        if (request.getData() != null) {
            return invalidRequest(request, "分类查询无需参数");
        }
        return Response.success(request, "分类已加载", new ArrayList<>(service.listCategories()));
    }

    private <T> Response administer(Request request, ServerContext context, Class<T> type,
            BiFunction<SessionInfo, T, ? extends Serializable> action) {
        Optional<SessionInfo> actor = session(request, context);
        if (actor.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!actor.orElseThrow().canAdminister(ModuleNames.LIBRARY)) {
            return Response.failure(request.getRequestId(), ErrorCodes.AUTH_FORBIDDEN,
                    "需要图书馆管理权限");
        }
        if (!type.isInstance(request.getData())) {
            return invalidRequest(request, "图书馆管理请求格式不正确");
        }
        try {
            return Response.success(request, "图书馆管理操作成功",
                    action.apply(actor.orElseThrow(), type.cast(request.getData())));
        } catch (LibraryBusinessException exception) {
            return businessFailure(request, exception);
        } catch (IllegalArgumentException exception) {
            return invalidArgument(request, exception);
        }
    }

    private static Optional<SessionInfo> session(Request request, ServerContext context) {
        return context.sessions().findSession(request.getToken());
    }

    private static Response authenticationRequired(Request request) {
        return Response.failure(request.getRequestId(), ErrorCodes.AUTH_REQUIRED, "请先登录");
    }

    private static Response invalidRequest(Request request, String message) {
        return Response.failure(request.getRequestId(), ErrorCodes.COMMON_INVALID_REQUEST, message);
    }

    private static Response invalidArgument(Request request, IllegalArgumentException exception) {
        return Response.failure(request.getRequestId(), ErrorCodes.COMMON_INVALID_ARGUMENT,
                exception.getMessage());
    }

    private static Response businessFailure(Request request, LibraryBusinessException exception) {
        return Response.failure(request.getRequestId(), exception.code(), exception.getMessage());
    }

    private static LibraryService createDefaultService() {
        InMemoryBookRepository books = new InMemoryBookRepository();
        InMemoryBorrowRecordRepository records = new InMemoryBorrowRecordRepository();
        InMemoryBookCopyRepository copies =
                InMemoryBookCopyRepository.seededFrom(books.searchAll(""));
        return new LibraryService(books, records, copies);
    }
}
