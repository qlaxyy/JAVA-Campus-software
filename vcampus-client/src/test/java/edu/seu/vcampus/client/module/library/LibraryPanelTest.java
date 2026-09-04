package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import org.junit.jupiter.api.Test;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private static JTable createResultTable() {
        LibraryPanel panel = new LibraryPanel(
                new ClientContext(new CampusClient("127.0.0.1", 1)));
        JScrollPane scrollPane = Arrays.stream(panel.getComponents())
                .filter(JScrollPane.class::isInstance)
                .map(JScrollPane.class::cast)
                .findFirst()
                .orElseThrow();
        JTable table = (JTable) scrollPane.getViewport().getView();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        // Selection tests need rows only; no network requests or borrowed records.
        for (int index = 1; index <= 3; index++) {
            model.addRow(new Object[] {"B00" + index, "ISBN" + index,
                    "Book " + index, "Author", "Category", "1/1"});
        }
        return table;
    }
}
