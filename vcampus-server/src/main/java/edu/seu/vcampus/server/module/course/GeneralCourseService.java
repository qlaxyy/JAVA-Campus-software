package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.SelectionBatchType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 通选课程业务服务。
 *
 * 通选课规则：
 *
 * 1. PRE_SELECTION 可以显示。
 * 2. ADD_DROP 与 PRE_SELECTION
 *    使用完全相同的课程范围。
 * 3. RETAKE 不显示通选课。
 * 4. 学生可以选择多门不同通选课。
 * 5. 同一门课程不能重复选择教学班。
 * 6. 必须检查容量。
 * 7. 必须检查时间冲突。
 */
final class GeneralCourseService {

    private final CourseBatchService
        batchService;

    private final GeneralCourseRepository
        generalCourseRepository;

    private final CoursePlanRepository
        planRepository;

    private final CourseSubstitutionRepository
        substitutionRepository;

    private final PeCourseRepository
        peCourseRepository;

    private final CourseEnrollmentRepository
        enrollmentRepository;

    private final CourseScheduleConflictChecker
        conflictChecker =
        new CourseScheduleConflictChecker();

    GeneralCourseService(
        CourseBatchService batchService,
        GeneralCourseRepository generalCourseRepository,
        CoursePlanRepository planRepository,
        CourseSubstitutionRepository substitutionRepository,
        PeCourseRepository peCourseRepository,
        CourseEnrollmentRepository enrollmentRepository) {

        this.batchService =
            Objects.requireNonNull(
                batchService);

        this.generalCourseRepository =
            Objects.requireNonNull(
                generalCourseRepository);

        this.planRepository =
            Objects.requireNonNull(
                planRepository);

        this.substitutionRepository =
            Objects.requireNonNull(
                substitutionRepository);

        this.peCourseRepository =
            Objects.requireNonNull(
                peCourseRepository);

        this.enrollmentRepository =
            Objects.requireNonNull(
                enrollmentRepository);
    }

    /**
     * 查询通选课程。
     *
     * @param generalCategory
     *     null 表示全部类别。
     */
    List<CourseInfo> listGeneralCourses(
        long batchId,
        String generalCategory,
        String userId) {

        if (batchId <= 0) {

            throw new IllegalArgumentException(
                "batchId must be positive");
        }

        Objects.requireNonNull(
            userId,
            "userId must not be null");

        /*
         * =========================
         * 1. 当前批次
         * =========================
         */
        SelectionBatchInfo batch =
            batchService.findBatch(
                batchId);

        if (batch == null) {

            throw new IllegalArgumentException(
                "selection batch does not exist");
        }

        /*
         * =========================
         * 2. 重修批次不显示通选课
         * =========================
         *
         * PRE_SELECTION 和 ADD_DROP
         * 都继续向下执行。
         *
         * 因此二者课程范围完全一样。
         */
        if (batch.getBatchType()
            == SelectionBatchType.RETAKE) {

            return List.of();
        }

        /*
         * =========================
         * 3. 全部通选课程
         * =========================
         */
        List<GeneralCourseRecord> records =
            generalCourseRepository
                .findGeneralCourses(
                    batchId);

        /*
         * =========================
         * 4. 找当前学生所有已选教学班
         * =========================
         *
         * 这里不仅检查通选课，
         * 还检查：
         *
         * - 方案内
         * - 方案外
         * - 体育课
         *
         * 因此通选页面可以提前显示
         * 与其他类型课程之间的冲突。
         */
        List<CourseInfo> allCourses =
            createAllCourses(
                batchId,
                records);

        Set<Long> selectedIds =
            enrollmentRepository
                .findSelectedOfferingIds(
                    userId);

        List<OfferingInfo> selectedOfferings =
            findSelectedOfferings(
                allCourses,
                selectedIds);

        /*
         * =========================
         * 5. 类别筛选 + 动态状态
         * =========================
         */
        List<CourseInfo> result =
            new ArrayList<>();

        for (GeneralCourseRecord record
            : records) {

            /*
             * null = 全部。
             */
            if (generalCategory != null
                && !generalCategory.equals(
                record.generalCategory())) {

                continue;
            }

            result.add(
                decorateCourse(
                    record.course(),
                    userId,
                    selectedOfferings));
        }

        return result;
    }

    /**
     * 后续选课 Service 使用：
     * 获取全部通选课程原始 CourseInfo。
     */
    List<CourseInfo> findRawCourses(
        long batchId) {

        return generalCourseRepository
            .findGeneralCourses(
                batchId)
            .stream()
            .map(
                GeneralCourseRecord::course)
            .toList();
    }

    /**
     * 后续选课 Service 使用：
     * 根据 offeringId 查通选课程。
     */
    CourseInfo findRawCourseByOffering(
        long batchId,
        long offeringId) {

        for (GeneralCourseRecord record
            : generalCourseRepository
            .findGeneralCourses(
                batchId)) {

            for (OfferingInfo offering
                : record.course()
                .getOfferings()) {

                if (offering.getOfferingId()
                    == offeringId) {

                    return record.course();
                }
            }
        }

        return null;
    }

    /**
     * 获取当前学生看到的动态课程。
     *
     * 后续真正提交通选课时使用。
     */
    CourseInfo findVisibleCourseByOffering(
        long batchId,
        String userId,
        long offeringId) {

        List<CourseInfo> courses =
            listGeneralCourses(
                batchId,
                null,
                userId);

        for (CourseInfo course
            : courses) {

            for (OfferingInfo offering
                : course.getOfferings()) {

                if (offering.getOfferingId()
                    == offeringId) {

                    return course;
                }
            }
        }

        return null;
    }

    /**
     * 组装当前可能参与时间冲突的
     * 所有课程。
     */
    private List<CourseInfo> createAllCourses(
        long batchId,
        List<GeneralCourseRecord> generalRecords) {

        List<CourseInfo> allCourses =
            new ArrayList<>();

        /*
         * 方案内。
         */
        allCourses.addAll(
            planRepository
                .findPlanCourses(
                    batchId));

        /*
         * 方案外。
         */
        allCourses.addAll(
            substitutionRepository
                .findSubstituteCourses(
                    batchId));

        /*
         * 体育课。
         */
        for (PeCourseRecord peRecord
            : peCourseRepository
            .findPeCourses(
                batchId)) {

            allCourses.add(
                peRecord.course());
        }

        /*
         * 通选课。
         */
        for (GeneralCourseRecord generalRecord
            : generalRecords) {

            allCourses.add(
                generalRecord.course());
        }

        return allCourses;
    }

    /**
     * 找学生已经选择的教学班。
     */
    private List<OfferingInfo> findSelectedOfferings(
        List<CourseInfo> courses,
        Set<Long> selectedIds) {

        List<OfferingInfo> result =
            new ArrayList<>();

        for (CourseInfo course
            : courses) {

            for (OfferingInfo offering
                : course.getOfferings()) {

                if (selectedIds.contains(
                    offering.getOfferingId())) {

                    result.add(
                        offering);
                }
            }
        }

        return result;
    }

    /**
     * 合并一门通选课的动态状态。
     */
    private CourseInfo decorateCourse(
        CourseInfo course,
        String userId,
        List<OfferingInfo> selectedOfferings) {

        /*
         * 同一门通选课已经选择
         * 某个教学班了吗？
         */
        boolean courseSelected =
            course.getOfferings()
                .stream()
                .anyMatch(offering ->
                    enrollmentRepository
                        .isOfferingSelected(
                            userId,
                            offering
                                .getOfferingId()));

        List<OfferingInfo> offerings =
            course.getOfferings()
                .stream()
                .map(offering ->
                    decorateOffering(
                        offering,
                        userId,
                        courseSelected,
                        selectedOfferings))
                .toList();

        return new CourseInfo(
            course.getCourseId(),
            course.getCourseCode(),
            course.getCourseName(),
            course.getCredits(),
            course.getCourseType(),
            courseSelected,
            offerings);
    }

    /**
     * 合并一个通选教学班的动态状态。
     */
    private OfferingInfo decorateOffering(
        OfferingInfo offering,
        String userId,
        boolean courseSelected,
        List<OfferingInfo> selectedOfferings) {

        /*
         * 当前教学班自己是否已选。
         */
        boolean selected =
            enrollmentRepository
                .isOfferingSelected(
                    userId,
                    offering.getOfferingId());

        /*
         * 动态人数。
         */
        int selectedCount =
            offering.getSelectedCount()
                + enrollmentRepository
                .countAdditionalSelections(
                    offering.getOfferingId());

        int remainingCount =
            Math.max(
                offering.getCapacity()
                    - selectedCount,
                0);

        String status;

        if (selected) {

            status =
                "SELECTED";

        } else if (courseSelected) {

            /*
             * 同一门通选课的
             * 其他教学班已经选了。
             */
            status =
                "COURSE_ALREADY_SELECTED";

        } else if (remainingCount <= 0) {

            status =
                "FULL";

        } else if (conflictChecker.hasConflict(
            offering,
            selectedOfferings)) {

            status =
                "TIME_CONFLICT";

        } else {

            status =
                offering
                    .getAvailabilityStatus();
        }

        return new OfferingInfo(
            offering.getOfferingId(),
            offering.getClassNo(),
            offering.getTeacherNames(),
            offering.getSchedules(),
            offering.getLocationName(),
            offering.getCampusName(),
            offering.getTeachingLanguage(),
            selectedCount,
            offering.getCapacity(),
            remainingCount,
            selected,
            status);
    }
}
