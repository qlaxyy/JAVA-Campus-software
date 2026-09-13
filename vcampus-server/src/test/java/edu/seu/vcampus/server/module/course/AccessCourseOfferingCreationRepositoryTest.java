package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.ScheduleInfo;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessCourseOfferingCreationRepositoryTest {

    @TempDir
    Path directory;

    @Test
    void createsOfferingAndScheduleForExistingCourse() {
        AccessDatabase database = new AccessDatabase(
            directory.resolve("course-create.accdb"));
        AccessCoursePlanRepository plan =
            new AccessCoursePlanRepository(
                database,
                new InMemoryCoursePlanRepository());
        AccessCourseOfferingCreationRepository creation =
            new AccessCourseOfferingCreationRepository(database);

        long offeringId = creation.create(
            101L,
            "02",
            "教一-101",
            "九龙湖校区",
            "中文",
            30,
            new ScheduleInfo(1, 1, 2, 1, 16, "EVERY"));

        CourseInfo course = plan.findPlanCourses(1L)
            .stream()
            .filter(value -> value.getCourseId() == 101L)
            .findFirst()
            .orElseThrow();

        assertEquals(2, course.getOfferings().size());
        assertEquals(offeringId,
            course.getOfferings().getLast().getOfferingId());
        assertEquals(1,
            course.getOfferings().getLast().getSchedules().size());
        assertTrue(offeringId > 0);

        assertThrows(
            IllegalArgumentException.class,
            () -> creation.create(
                101L,
                "02",
                "教一-102",
                "九龙湖校区",
                "中文",
                30,
                new ScheduleInfo(2, 1, 2, 1, 16, "EVERY")));
    }
}
