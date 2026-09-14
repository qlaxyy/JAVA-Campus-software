package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.library.SetBookStatusRequest;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 在线目录的检索字段与分页。
 *
 * <p>可检索字段横跨两张表：书名、作者、ISBN、分类、出版社、语种、出版年来自目录行，索书号与
 * 馆藏条码来自实体单册。因此一次检索必须同时命中两者——本用例中的 "C002/" 与 "seu-b004-002"
 * 只存在于单册记录里，目录行中没有任何一列包含它们。
 *
 * <p>分页只做在检索路径上：借阅历史受在借上限约束，单册列表受复本数约束，二者结果集天然有界。
 */
class LibraryCatalogSearchTest {

    private static final SessionInfo ADMIN = new SessionInfo("token", "A-1", "admin", "管理员",
            Role.USER, Set.of(AdminScope.LIBRARY));

    private final InMemoryBookRepository books = new InMemoryBookRepository();
    private final InMemoryBookCopyRepository copies =
            InMemoryBookCopyRepository.seededFrom(books.searchAll(""));
    private final LibraryService service =
            new LibraryService(books, new InMemoryBorrowRecordRepository(), copies);

    @Test
    void shelfMarkFindsTheTitleThatOwnsTheCopy() {
        // 演示数据里只有《红楼梦》（B004）的单册带 C002/ 索书号。
        assertEquals(List.of("B004"), bookIds(service.searchBooks(new BookSearchRequest("C002/", null))));
    }

    @Test
    void barcodeFindsTheTitleThatOwnsTheCopyRegardlessOfCase() {
        assertEquals(List.of("B004"),
                bookIds(service.searchBooks(new BookSearchRequest("seu-b004-002", null))));
        assertEquals(List.of("B004"),
                bookIds(service.searchBooks(new BookSearchRequest("SEU-B004-002", null))));
    }

    @Test
    void catalogColumnsRemainSearchable() {
        assertEquals(List.of("B001", "B002", "B003"),
                bookIds(service.searchBooks(new BookSearchRequest("JAVA", null))),
                "标题与作者匹配，大小写不敏感");
        assertEquals(List.of("B005"),
                bookIds(service.searchBooks(new BookSearchRequest("中华书局", null))),
                "出版社可检索");
        assertEquals(List.of("B005"),
                bookIds(service.searchBooks(new BookSearchRequest("2014", null))),
                "出版年可检索");
    }

    @Test
    void unknownKeywordMatchesNothing() {
        BookSearchResult result = service.searchBooks(new BookSearchRequest("不存在的图书", null));
        assertTrue(result.getBooks().isEmpty());
        assertEquals(0, result.getTotalCount());
        assertEquals(1, result.getTotalPages(), "空结果仍报告一页，便于界面显示“第 1 / 1 页”");
        assertFalse(result.hasNextPage());
    }

    @Test
    void pagingWalksTheWholeResultOnceWithoutGapsOrRepeats() {
        BookSearchResult first = service.searchBooks(new BookSearchRequest("", null, 1, 2));
        assertEquals(5, first.getTotalCount());
        assertEquals(3, first.getTotalPages());
        assertEquals(2, first.getBooks().size());
        assertTrue(first.hasNextPage());

        List<String> walked = new ArrayList<>();
        for (int page = 1; page <= first.getTotalPages(); page++) {
            walked.addAll(bookIds(service.searchBooks(new BookSearchRequest("", null, page, 2))));
        }
        assertEquals(bookIds(service.searchBooks(new BookSearchRequest("", null))), walked,
                "逐页取完应等于不分页的结果，顺序也一致");
        assertEquals(walked.size(), walked.stream().distinct().count(), "各页之间不应重复");
    }

    @Test
    void lastPageHoldsTheRemainderAndReportsTheSameTotal() {
        BookSearchResult last = service.searchBooks(new BookSearchRequest("", null, 3, 2));
        assertEquals(5, last.getTotalCount(), "总数是全部匹配数，不是本页条数");
        assertEquals(1, last.getBooks().size());
        assertEquals(3, last.getPage());
        assertFalse(last.hasNextPage());
    }

    @Test
    void pageBeyondTheEndIsEmptyRatherThanAnError() {
        BookSearchResult past = service.searchBooks(new BookSearchRequest("", null, 999, 20));
        assertTrue(past.getBooks().isEmpty());
        assertEquals(5, past.getTotalCount());
        assertEquals(999, past.getPage());
        assertFalse(past.hasNextPage());
        assertTrue(service.searchBooks(
                        new BookSearchRequest("", null, Integer.MAX_VALUE, 100)).getBooks().isEmpty(),
                "极大的页码不能因偏移量溢出而抛异常");
    }

    @Test
    void pageSizeAndPageNumberAreValidated() {
        assertEquals(5, service.searchBooks(new BookSearchRequest(
                "", null, 1, BookSearchRequest.MAX_PAGE_SIZE)).getBooks().size(),
                "上限本身是合法的");
        assertThrows(IllegalArgumentException.class,
                () -> service.searchBooks(new BookSearchRequest("", null, 0, 20)));
        assertThrows(IllegalArgumentException.class,
                () -> service.searchBooks(new BookSearchRequest("", null, 1, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> service.searchBooks(new BookSearchRequest(
                        "", null, 1, BookSearchRequest.MAX_PAGE_SIZE + 1)));
    }

    @Test
    void pagedResultsStillCarryTheInventorySummaryForTheirOwnRows() {
        BookSearchResult page = service.searchBooks(new BookSearchRequest("", null, 1, 1));
        assertEquals(1, page.getBooks().size());
        BookDTO only = page.getBooks().getFirst();
        assertEquals(copies.findByBookId(only.getBookId()).stream()
                        .filter(copy -> copy.status() != BookCopyStatus.WITHDRAWN).count(),
                only.getTotalCount());
        assertEquals(copies.findByBookId(only.getBookId()).stream()
                        .filter(copy -> copy.status() == BookCopyStatus.AVAILABLE).count(),
                only.getAvailableCount());
        assertFalse(only.getLocations().isEmpty());
    }

    @Test
    void categoryFilterIsAppliedBeforePaging() {
        BookSearchResult page = service.searchBooks(new BookSearchRequest("", "C001", 1, 2));
        assertEquals(3, page.getTotalCount());
        assertEquals(2, page.getTotalPages());
        assertEquals(2, page.getBooks().size());
        assertTrue(page.getBooks().stream()
                .allMatch(book -> "C001".equals(book.getCategoryId())));
    }

    @Test
    void administratorSearchAlsoMatchesShelfMarksAndKeepsInactiveTitles() {
        service.setBookStatus(ADMIN, new SetBookStatusRequest("B004", "INACTIVE"));

        assertTrue(service.searchBooks(new BookSearchRequest("C002/", null)).getBooks().isEmpty(),
                "读者看不到已停止借阅的书目");
        BookSearchResult admin = service.searchBooksForAdmin(ADMIN,
                new BookSearchRequest("C002/", null, 1, 20));
        assertEquals(List.of("B004"), bookIds(admin));
        assertEquals("INACTIVE", admin.getBooks().getFirst().getStatus());
    }

    private static List<String> bookIds(BookSearchResult result) {
        return result.getBooks().stream().map(BookDTO::getBookId).toList();
    }
}
