package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.common.library.BookCategoryDTO;
import edu.seu.vcampus.common.protocol.Response;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import java.util.List;

/** Shared category response validation and display for catalog and administrator forms. */
final class LibraryCategories {
    private LibraryCategories() { }

    static List<BookCategoryDTO> read(Response response) {
        if (response == null || !response.isSuccess()) {
            throw new IllegalArgumentException(response == null ? "服务器未返回分类" : LibraryMessages.failure(response));
        }
        if (!(response.getData() instanceof List<?> values)
                || values.stream().anyMatch(value -> !(value instanceof BookCategoryDTO))) {
            throw new IllegalArgumentException("服务器返回的分类数据格式不正确");
        }
        return values.stream().map(BookCategoryDTO.class::cast).toList();
    }

    static void render(JComboBox<BookCategoryDTO> combo) {
        combo.setRenderer((list, value, index, selected, focus) ->
                new DefaultListCellRenderer().getListCellRendererComponent(list,
                        value == null ? "全部分类" : value.getCategoryName(), index, selected, focus));
    }
}
