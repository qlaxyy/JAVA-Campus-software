package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.Container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryPanelTest {

    @Test
    void tableUsesSingleSelectionAndClickingAnotherBookReplacesSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createResultTable();

            assertEquals(ListSelectionModel.SINGLE_SELECTION,
                    table.getSelectionModel().getSelectionMode());
            table.changeSelection(0, 0, false, false);
            table.changeSelection(1, 0, false, false);

            assertEquals(1, table.getSelectedRowCount());
            assertEquals(1, table.getSelectedRow());
        });
    }

    @Test
    void ctrlClickCannotAddAnotherBookToSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createResultTable();
            table.changeSelection(0, 0, false, false);

            // Ctrl-click asks JTable to toggle a row without extending the selection.
            table.changeSelection(2, 0, true, false);

            assertEquals(1, table.getSelectedRowCount());
            assertEquals(2, table.getSelectedRow());
        });
    }

    @Test
    void shiftClickCannotSelectARangeOfBooks() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createResultTable();
            table.changeSelection(0, 0, false, false);

            // Shift-click asks JTable to extend the selection to the clicked row.
            table.changeSelection(2, 0, false, true);

            assertEquals(1, table.getSelectedRowCount());
            assertEquals(2, table.getSelectedRow());
        });
    }

    @Test
    void selectAllCannotSelectMultipleBooks() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = createResultTable();
            table.changeSelection(1, 0, false, false);

            table.selectAll();

            assertEquals(1, table.getSelectedRowCount());
        });
    }

    @Test
    void holdingDetailsUseAReadOnlyWrappingTextArea() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LibraryPanel panel = new LibraryPanel(
                    new ClientContext(new CampusClient("127.0.0.1", 1)));
            JTextArea details = named(panel, JTextArea.class, "library.holdingDetails");

            assertFalse(details.isEditable());
            assertTrue(details.getLineWrap());
            assertTrue(details.getWrapStyleWord());
            assertTrue(details.getText().contains("完整馆藏地"));
        });
    }

    private static JTable createResultTable() {
        LibraryPanel panel = new LibraryPanel(
                new ClientContext(new CampusClient("127.0.0.1", 1)));
        JTable table = named(panel, JTable.class, "library.searchResults");
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        // Selection tests need rows only; no network requests or borrowed records.
        for (int index = 1; index <= 3; index++) {
            model.addRow(new Object[] {"B00" + index, "ISBN" + index,
                    "Book " + index, "Author", "Category", "1/1"});
        }
        return table;
    }

    private static <T extends Component> T named(Container root, Class<T> type, String name) {
        for (Component component : root.getComponents()) {
            if (type.isInstance(component) && name.equals(component.getName())) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                try {
                    return named(child, type, name);
                } catch (IllegalStateException ignored) {
                    // Continue searching the remaining component branches.
                }
            }
        }
        throw new IllegalStateException("Component not found: " + name);
    }
}
