package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 全校课程查询分页结果。
 */
public final class CourseSearchResult
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<CourseSearchItem> items;

    private final int page;
    private final int pageSize;

    /**
     * 筛选后的总记录数。
     */
    private final int totalCount;

    /**
     * 总页数。
     */
    private final int totalPages;

    public CourseSearchResult(
        List<CourseSearchItem> items,
        int page,
        int pageSize,
        int totalCount,
        int totalPages) {

        this.items =
            List.copyOf(
                Objects.requireNonNull(
                    items));

        this.page = page;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.totalPages = totalPages;
    }

    public List<CourseSearchItem> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
