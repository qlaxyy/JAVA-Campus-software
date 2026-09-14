package edu.seu.vcampus.client.module.course;

import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assertions.*;

class CourseThemeTest {
    @Test void redundantPageNarrationIsHiddenButEmptyStateRemainsVisible() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var narration = CourseTheme.pageSubtitle("查看本人负责的课程和上课安排");
            assertFalse(narration.isVisible());
            assertEquals("", narration.getText());
            var emptyState = CourseTheme.subtitle("当前学期暂无选课批次。");
            assertTrue(emptyState.isVisible());
            assertEquals("当前学期暂无选课批次。", emptyState.getText());
        });
    }
}
