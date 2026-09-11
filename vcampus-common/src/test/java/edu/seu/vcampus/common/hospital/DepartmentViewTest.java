package edu.seu.vcampus.common.hospital;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepartmentViewTest {

    @Test
    void representsCategoryAndBookableSubdepartment() {
        DepartmentView category = new DepartmentView(
                "dept-orthopedics", "骨科", "dept-surgery", false);
        DepartmentView clinic = new DepartmentView(
                "dept-joint", "骨关节外科", "dept-orthopedics", true);

        assertEquals("dept-surgery", category.getParentDepartmentId());
        assertFalse(category.isBookable());
        assertEquals("dept-orthopedics", clinic.getParentDepartmentId());
        assertTrue(clinic.isBookable());
    }

    @Test
    void keepsTwoArgumentConstructorAsRootBookableDepartment() {
        DepartmentView department = new DepartmentView("dept-general", "全科门诊");

        assertNull(department.getParentDepartmentId());
        assertTrue(department.isBookable());
    }

    @Test
    void rejectsBlankOrSelfParent() {
        assertThrows(IllegalArgumentException.class, () -> new DepartmentView(
                "dept-1", "科室", "   ", true));
        assertThrows(IllegalArgumentException.class, () -> new DepartmentView(
                "dept-1", "科室", "dept-1", true));
    }
}
