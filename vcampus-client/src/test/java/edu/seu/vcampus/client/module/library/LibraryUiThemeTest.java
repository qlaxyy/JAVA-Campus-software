package edu.seu.vcampus.client.module.library;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the button contrast regression that previously hid action labels. */
class LibraryUiThemeTest {

    @Test
    void queryScopesLeaveEnoughRoomForEveryOptionAndArrow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JComboBox<String> scope = new JComboBox<>(
                    new String[]{"当前借阅", "借阅历史", "逾期未还"});
            LibraryUiTheme.styleComboBox(scope);
            scope.setSize(scope.getPreferredSize());
            scope.doLayout();
            int arrowWidth = 0;
            for (java.awt.Component child : scope.getComponents()) {
                if (child instanceof javax.swing.AbstractButton) {
                    arrowWidth += child.getWidth();
                }
            }
            int available = scope.getWidth() - arrowWidth
                    - scope.getInsets().left - scope.getInsets().right;
            for (int index = 0; index < scope.getItemCount(); index++) {
                int textWidth = scope.getFontMetrics(scope.getFont())
                        .stringWidth(scope.getItemAt(index));
                assertTrue(available >= textWidth + 8,
                        "query scope must display its complete option, without ellipsis");
            }
        });
    }

    @Test
    void everyButtonKindKeepsDisabledTextReadable() throws Exception {
        AtomicReference<List<JButton>> buttons = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            JButton primary = new JButton("主操作");
            JButton secondary = new JButton("次操作");
            JButton danger = new JButton("危险操作");
            LibraryUiTheme.stylePrimaryButton(primary);
            LibraryUiTheme.styleSecondaryButton(secondary);
            LibraryUiTheme.styleDangerButton(danger);
            buttons.set(List.of(primary, secondary, danger));
        });

        for (JButton button : buttons.get()) {
            SwingUtilities.invokeAndWait(() -> {
                button.setEnabled(false);
                button.setSize(150, 42);
                assertEquals(LibraryUiTheme.DISABLED_TEXT, button.getForeground());
                assertEquals(LibraryUiTheme.DISABLED_BACKGROUND, button.getBackground());
                assertTrue(contrastRatio(button.getForeground(), button.getBackground()) >= 4.0,
                        "disabled action text must retain readable contrast");
                BufferedImage image = new BufferedImage(
                        button.getWidth(), button.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                button.paint(graphics);
                graphics.dispose();
            });
        }
    }

    private static double contrastRatio(Color first, Color second) {
        double lighter = Math.max(luminance(first), luminance(second));
        double darker = Math.min(luminance(first), luminance(second));
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double luminance(Color color) {
        double red = channel(color.getRed() / 255.0);
        double green = channel(color.getGreen() / 255.0);
        double blue = channel(color.getBlue() / 255.0);
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private static double channel(double value) {
        return value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
