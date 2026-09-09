package edu.seu.vcampus.client.module.hospital;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientCareGuidePanelTest {

    @Test
    void guideOffersWorkingRegistrationFollowUpAndHealthRecordRoutes() throws Exception {
        AtomicBoolean registration = new AtomicBoolean();
        AtomicBoolean followUp = new AtomicBoolean();
        AtomicBoolean healthRecord = new AtomicBoolean();
        PatientCareGuidePanel[] panel = new PatientCareGuidePanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new PatientCareGuidePanel(
                () -> { },
                () -> registration.set(true),
                () -> followUp.set(true),
                () -> healthRecord.set(true)));

        click(panel[0], "guideRegistrationButton");
        click(panel[0], "guideFollowUpButton");
        click(panel[0], "guideHealthRecordButton");

        assertTrue(registration.get());
        assertTrue(followUp.get());
        assertTrue(healthRecord.get());
    }

    private static void click(Container root, String name) throws Exception {
        JButton button = components(root, JButton.class).stream()
                .filter(candidate -> name.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
        SwingUtilities.invokeAndWait(button::doClick);
    }

    private static <T extends Component> List<T> components(
            Container root,
            Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container container) {
                found.addAll(components(container, type));
            }
        }
        return found;
    }
}
