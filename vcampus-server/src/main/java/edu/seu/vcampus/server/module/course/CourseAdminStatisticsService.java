package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseAdminStatisticsInfo;
import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 教务端选课数据统计服务。
 */
final class CourseAdminStatisticsService {

    private final CourseBatchService
        batchService;

    private final CourseOfferingAdministrationService
        offeringAdministrationService;

    CourseAdminStatisticsService(
        CourseBatchService batchService,
        CourseOfferingAdministrationService
            offeringAdministrationService) {

        this.batchService =
            Objects.requireNonNull(
                batchService);

        this.offeringAdministrationService =
            Objects.requireNonNull(
                offeringAdministrationService);
    }

    /**
     * 统计指定选课批次。
     */
    CourseAdminStatisticsResult statistics(
        long batchId) {

        if (batchService.findBatch(
            batchId) == null) {

            return CourseAdminStatisticsResult.failure(
                "选课批次不存在。");
        }

        List<CourseInfo> loadedCourses =
            offeringAdministrationService
                .listCourses(
                    batchId);

        /*
         * 按课程 ID 去重。
         */
        Map<Long, CourseInfo> courses =
            new LinkedHashMap<>();

        for (CourseInfo course
            : loadedCourses) {

            courses.putIfAbsent(
                course.getCourseId(),
                course);
        }

        /*
         * 按教学班 ID 去重。
         */
        Map<Long, OfferingInfo> offerings =
            new LinkedHashMap<>();

        for (CourseInfo course
            : courses.values()) {

            for (OfferingInfo offering
                : course.getOfferings()) {

                offerings.putIfAbsent(
                    offering.getOfferingId(),
                    offering);
            }
        }

        int selectedCount =
            0;

        int totalCapacity =
            0;

        int remainingCount =
            0;

        int fullOfferingCount =
            0;

        int closedOfferingCount =
            0;

        for (OfferingInfo offering
            : offerings.values()) {

            selectedCount +=
                offering.getSelectedCount();

            totalCapacity +=
                offering.getCapacity();

            remainingCount +=
                offering.getRemainingCount();

            if ("FULL".equals(
                offering
                    .getAvailabilityStatus())
                || offering.getRemainingCount()
                <= 0) {

                fullOfferingCount++;
            }

            if ("OFFERING_CLOSED".equals(
                offering
                    .getAvailabilityStatus())) {

                closedOfferingCount++;
            }
        }

        double selectionRate =
            totalCapacity <= 0
                ? 0.0
                : selectedCount
                * 100.0
                / totalCapacity;

        CourseAdminStatisticsInfo information =
            new CourseAdminStatisticsInfo(
                batchId,
                courses.size(),
                offerings.size(),
                selectedCount,
                totalCapacity,
                remainingCount,
                fullOfferingCount,
                closedOfferingCount,
                selectionRate);

        return CourseAdminStatisticsResult.success(
            "选课统计加载成功。",
            information);
    }
}

/**
 * 统计查询结果。
 */
record CourseAdminStatisticsResult(
    boolean success,
    String message,
    CourseAdminStatisticsInfo statistics) {

    static CourseAdminStatisticsResult success(
        String message,
        CourseAdminStatisticsInfo statistics) {

        return new CourseAdminStatisticsResult(
            true,
            message,
            statistics);
    }

    static CourseAdminStatisticsResult failure(
        String message) {

        return new CourseAdminStatisticsResult(
            false,
            message,
            null);
    }
}
