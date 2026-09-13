package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.common.hospital.HospitalModeAccessView;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HospitalModePanelTest {

    @Test
    void everyAccessibleModeUsesTheSameGreenPrimaryStyle() throws Exception {
        HospitalModePanel[] panel = new HospitalModePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new HospitalModePanel(() -> { }, () -> { }, () -> { }, () -> { });
            panel[0].showAccess(
                    new SessionInfo(
                            "token-doctor",
                            "U-TEACHER-001",
                            "20260029",
                            "陈医生",
                            Role.USER),
                    new HospitalModeAccessView(true, true, false));
        });

        JButton patient = button(panel[0], "进入患者模式");
        JButton doctor = button(panel[0], "进入医生模式");
        JButton admin = button(panel[0], "无权限");
        assertTrue(patient.isEnabled());
        assertTrue(doctor.isEnabled());
        assertEquals(HospitalTheme.PRIMARY, patient.getBackground());
        assertEquals(HospitalTheme.PRIMARY, doctor.getBackground());
        assertEquals(java.awt.Color.WHITE, patient.getForeground());
        assertEquals(java.awt.Color.WHITE, doctor.getForeground());
        assertFalse(admin.isEnabled());
        assertEquals(HospitalTheme.DISABLED, admin.getBackground());
    }

    private static JButton button(Container root, String text) {
        return components(root, JButton.class).stream()
                .filter(button -> text.equals(button.getText()))
                .findFirst()
                .orElseThrow();
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
