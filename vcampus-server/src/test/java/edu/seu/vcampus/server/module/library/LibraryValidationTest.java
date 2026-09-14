package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.AddBookCategoryRequest;
import edu.seu.vcampus.common.library.AddBookCopyRequest;
import edu.seu.vcampus.common.library.AddBookRequest;
import edu.seu.vcampus.common.library.UpdateBookCopyRequest;
import edu.seu.vcampus.common.library.SetBookStatusRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 服务层参数校验矩阵。
 *
 * <p>长度与格式校验由服务器权威执行，客户端预校验只用于改善体验，因此这里直接调用
 * {@code LibraryService}。测试只断言行为，不断言提示文案：超长与格式错误都应是
 * {@link IllegalArgumentException}（由 Module 转成 {@code COMMON_INVALID_ARGUMENT}），
 * 而"格式正确但对象不存在"应是带错误码的 {@link LibraryBusinessException}。
 */
class LibraryValidationTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-09-06T08:00:00Z"), ZoneOffset.UTC);
    private static final SessionInfo ADMIN = new SessionInfo("admin", "A-1", "libraryadmin", "管理员",
            Role.USER, Set.of(AdminScope.LIBRARY));

    private static final int PRICE_FEN = 5_000;

    private final AtomicInteger isbnSequence = new AtomicInteger();
    private final InMemoryBookRepository books = new InMemoryBookRepository();
    private final InMemoryBorrowRecordRepository records = new InMemoryBorrowRecordRepository();
    private final InMemoryBookCopyRepository copies =
            InMemoryBookCopyRepository.seededFrom(books.searchAll(""));
    private final LibraryService service = new LibraryService(books, records, CLOCK,
            () -> "R-" + isbnSequence.incrementAndGet(),
            new InMemoryBookCategoryRepository(), copies);

    @Test
    void bookTitleLimitIsTwoHundredCharacters() {
        assertEquals(200, service.addBook(ADMIN,
                book("书".repeat(200), "作者", "出版社", 2026, "中文")).getTitle().length());
        assertThrows(IllegalArgumentException.class, () -> service.addBook(ADMIN,
                book("书".repeat(201), "作者", "出版社", 2026, "中文")));
    }

    @Test
    void bookAuthorLimitIsOneHundredCharacters() {
        assertEquals("作".repeat(100), service.addBook(ADMIN,
                book("书名", "作".repeat(100), "出版社", 2026, "中文")).getAuthor());
        assertThrows(IllegalArgumentException.class, () -> service.addBook(ADMIN,
                book("书名", "作".repeat(101), "出版社", 2026, "中文")));
    }

    @Test
    void optionalPublisherAndLanguageEnforceTheirLimits() {
        assertEquals("出".repeat(100), service.addBook(ADMIN,
                book("书名", "作者", "出".repeat(100), 2026, "中文")).getPublisher());
        assertThrows(IllegalArgumentException.class, () -> service.addBook(ADMIN,
                book("书名", "作者", "出".repeat(101), 2026, "中文")));

        assertEquals(30, service.addBook(ADMIN,
                book("书名", "作者", "出版社", 2026, "语".repeat(30))).getLanguage().length());
        assertThrows(IllegalArgumentException.class, () -> service.addBook(ADMIN,
                book("书名", "作者", "出版社", 2026, "语".repeat(31))));
    }

    @Test
    void blankRequiredTextIsRejectedWhileBlankOptionalTextBecomesEmpty() {
        for (String blank : List.of("", "   ")) {
            assertThrows(IllegalArgumentException.class, () -> service.addBook(ADMIN,
                    book(blank, "作者", "出版社", 2026, "中文")));
            assertThrows(IllegalArgumentException.class, () -> service.addBook(ADMIN,
                    book("书名", blank, "出版社", 2026, "中文")));
        }
        assertEquals("", service.addBook(ADMIN,
                book("书名", "作者", null, 2026, null)).getPublisher());
        assertEquals("", service.addBook(ADMIN,
                book("书名", "作者", "   ", 2026, "  ")).getLanguage());
    }

    @Test
    void categoryNameLimitIsFiftyCharacters() {
        assertEquals(50, service.addCategory(ADMIN,
                new AddBookCategoryRequest("类".repeat(50))).getCategoryName().length());
        assertThrows(IllegalArgumentException.class,
                () -> service.addCategory(ADMIN, new AddBookCategoryRequest("类".repeat(51))));
    }

    @Test
    void copyBarcodeLocationAndCallNumberEnforceTheirLimits() {
        assertEquals(50, service.addBookCopy(ADMIN,
                new AddBookCopyRequest("B001", "B".repeat(50), "九龙湖", "索书号")).getBarcode().length());
        assertThrows(IllegalArgumentException.class, () -> service.addBookCopy(ADMIN,
                new AddBookCopyRequest("B001", "B".repeat(51), "九龙湖", "索书号")));

        assertEquals(100, service.addBookCopy(ADMIN,
                new AddBookCopyRequest("B001", "BC-LIMIT", "馆".repeat(100), "索".repeat(100)))
                .getLocation().length());
        assertThrows(IllegalArgumentException.class, () -> service.addBookCopy(ADMIN,
                new AddBookCopyRequest("B001", "BC-LONG-LOC", "馆".repeat(101), "索书号")));
        assertThrows(IllegalArgumentException.class, () -> service.addBookCopy(ADMIN,
                new AddBookCopyRequest("B001", "BC-LONG-CALL", "九龙湖", "索".repeat(101))));
    }

    @Test
    void identifierLimitsAreCheckedBeforeLookup() {
        // 图书编号上限 20：恰好 20 位的未知编号会走到"未找到"，21 位在校验阶段就被拒绝
        failure(ErrorCodes.LIBRARY_BOOK_NOT_FOUND,
                () -> service.setBookStatus(ADMIN, new SetBookStatusRequest("B".repeat(20), "ACTIVE")));
        assertThrows(IllegalArgumentException.class,
                () -> service.setBookStatus(ADMIN, new SetBookStatusRequest("B".repeat(21), "ACTIVE")));

        // 单册编号上限 50
        failure(ErrorCodes.LIBRARY_COPY_NOT_FOUND, () -> service.updateBookCopy(ADMIN,
                new UpdateBookCopyRequest("C".repeat(50), "九龙湖", "索书号")));
        assertThrows(IllegalArgumentException.class, () -> service.updateBookCopy(ADMIN,
                new UpdateBookCopyRequest("C".repeat(51), "九龙湖", "索书号")));

        // 分类编号上限 20
        failure(ErrorCodes.LIBRARY_CATEGORY_NOT_FOUND, () -> service.addBook(ADMIN,
                new AddBookRequest(nextIsbn(), "书名", "作者", "C".repeat(20), "出版社",
                        2026, "中文", PRICE_FEN)));
        assertThrows(IllegalArgumentException.class, () -> service.addBook(ADMIN,
                new AddBookRequest(nextIsbn(), "书名", "作者", "C".repeat(21), "出版社",
                        2026, "中文", PRICE_FEN)));
    }

    @Test
    void isbnAcceptsTenAndThirteenDigitFormsAndNormalizesSeparators() {
        // 三种写法归一化后必须是三个不同的 ISBN，否则会先撞上重复校验
        assertEquals("9787111000000", service.addBook(ADMIN,
                bookWithIsbn("9787111000000")).getIsbn());
        assertEquals("9787111000001", service.addBook(ADMIN,
                bookWithIsbn("978-7-111-00000-1")).getIsbn());
        assertEquals("9787111000002", service.addBook(ADMIN,
                bookWithIsbn(" 978 7 111 00000 2 ")).getIsbn());
        assertEquals("7111000000", service.addBook(ADMIN,
                bookWithIsbn("7111000000")).getIsbn());
        assertEquals("711100000X", service.addBook(ADMIN,
                bookWithIsbn("711100000x")).getIsbn());
    }

    @Test
    void isbnRejectsMalformedValues() {
        for (String invalid : List.of(
                "978711100000", "97871110000000", "71110000X", "978711100000A", "711100000Y", "")) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.addBook(ADMIN, bookWithIsbn(invalid)),
                    "应拒绝 ISBN：" + invalid);
        }
    }

    @Test
    void isbnLengthLimitAppliesBeforeSeparatorsAreStripped() {
        String digits = "9787111000000";
        String withinLimit = String.join("  ", digits.split(""));   // 37 个字符
        String overLimit = String.join("   ", digits.split(""));    // 49 个字符

        assertEquals(digits, service.addBook(ADMIN, bookWithIsbn(withinLimit)).getIsbn());
        // 49 个字符去掉分隔符后本身是合法 ISBN，说明长度上限先于格式归一化生效
        assertThrows(IllegalArgumentException.class,
                () -> service.addBook(ADMIN, bookWithIsbn(overLimit)));
    }

    @Test
    void publicationYearAcceptsOnlyOneThousandToNineThousandNineHundredNinetyNine() {
        assertEquals(1000, service.addBook(ADMIN,
                book("书名", "作者", "出版社", 1000, "中文")).getPublicationYear().intValue());
        assertEquals(9999, service.addBook(ADMIN,
                book("书名", "作者", "出版社", 9999, "中文")).getPublicationYear().intValue());
        assertNull(service.addBook(ADMIN,
                book("书名", "作者", "出版社", null, "中文")).getPublicationYear());
        for (Integer invalid : List.of(999, 10000, -1)) {
            assertThrows(IllegalArgumentException.class, () -> service.addBook(ADMIN,
                    book("书名", "作者", "出版社", invalid, "中文")), "应拒绝出版年：" + invalid);
        }
    }

    /** 自动分配 ISBN 的合法请求，用于长度与范围校验；这些测试不关心 ISBN 本身。 */
    @Test
    void bookPriceMustBeWithinRange() {
        assertEquals(1, service.addBook(ADMIN, bookWithPrice(1)).getPriceFen());
        assertEquals(999_999, service.addBook(ADMIN, bookWithPrice(999_999)).getPriceFen());

        assertThrows(IllegalArgumentException.class,
                () -> service.addBook(ADMIN, bookWithPrice(0)));
        assertThrows(IllegalArgumentException.class,
                () -> service.addBook(ADMIN, bookWithPrice(1_000_000)));
        assertThrows(IllegalArgumentException.class,
                () -> service.addBook(ADMIN, bookWithPrice(-1)));
    }

    private AddBookRequest bookWithPrice(int priceFen) {
        return new AddBookRequest(nextIsbn(), "书名", "作者", "C001", "出版社", 2026, "中文", priceFen);
    }

    private AddBookRequest book(String title, String author, String publisher,
            Integer publicationYear, String language) {
        return new AddBookRequest(nextIsbn(), title, author, "C001", publisher,
                publicationYear, language, PRICE_FEN);
    }

    /** 显式指定 ISBN 的请求，用于 ISBN 格式测试。 */
    private AddBookRequest bookWithIsbn(String isbn) {
        return new AddBookRequest(isbn, "书名", "作者", "C001", "出版社", 2026, "中文", PRICE_FEN);
    }

    /** 自动 ISBN 使用独立的号段，避免与 ISBN 测试中显式写出的值相撞。 */
    private String nextIsbn() {
        return "9780000%06d".formatted(isbnSequence.incrementAndGet());
    }

    private static void failure(String code, Runnable action) {
        assertEquals(code, assertThrows(LibraryBusinessException.class, action::run).code());
    }
}
