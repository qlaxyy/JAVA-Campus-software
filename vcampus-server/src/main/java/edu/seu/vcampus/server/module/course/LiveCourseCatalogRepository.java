package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.SelectionBatchStatus;

import java.util.List;
import java.util.Objects;

/**
 * 使用真实课程与教学班生成全校课程目录。
 */
final class LiveCourseCatalogRepository
    implements CourseCatalogRepository {

    private final CourseBatchService
        batchService;

    private final CourseOfferingAdministrationService
        offeringAdministrationService;

    LiveCourseCatalogRepository(
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

    @Override
    public List<CourseCatalogRecord>
    findCurrentSemesterCourses() {

        List<SelectionBatchInfo> batches =
            batchService.listBatches();

        if (batches.isEmpty()) {

            return List.of();
        }

        /*
         * 优先读取当前开放批次。
         * 没有开放批次时读取当前学期第一个批次。
         */
        SelectionBatchInfo selectedBatch =
            batches.stream()
                .filter(batch ->
                    batch.getStatus()
                        == SelectionBatchStatus.OPEN)
                .findFirst()
                .orElse(
                    batches.get(0));

        return offeringAdministrationService
            .listCourses(
                selectedBatch.getBatchId())
            .stream()
            .filter(course ->
                !course.getOfferings()
                    .isEmpty())
            .map(this::toCatalogRecord)
            .toList();
    }

    private CourseCatalogRecord toCatalogRecord(
        CourseInfo course) {

        return new CourseCatalogRecord(
            course,
            course.getDepartmentName());
    }
}
