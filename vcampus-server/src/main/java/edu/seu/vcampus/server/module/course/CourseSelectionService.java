package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.SelectionBatchStatus;
import edu.seu.vcampus.common.course.SelectionBatchType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 学生选课业务服务。
 *
 * 当前支持：
 *
 * 1. 方案内课程
 * 2. 方案外替代课程
 * 3. 体育课程
 * 4. 通选课程
 */
final class CourseSelectionService {
    private CourseOfferingAdministrationService
        offeringAdministrationService;
    private final CourseBatchService
        batchService;

    private final CoursePlanRepository
        planRepository;

    private final CourseSubstitutionRepository
        substitutionRepository;

    private final PeCourseService
        peCourseService;

    private final GeneralCourseService
        generalCourseService;

    private final CourseEnrollmentRepository
        enrollmentRepository;

    private final CourseHistoryRepository
        historyRepository;

    private final CourseScheduleConflictChecker
        conflictChecker =
        new CourseScheduleConflictChecker();

    CourseSelectionService(
        CourseBatchService batchService,
        CoursePlanRepository planRepository,
        CourseSubstitutionRepository substitutionRepository,
        PeCourseService peCourseService,
        GeneralCourseService generalCourseService,
        CourseEnrollmentRepository enrollmentRepository,
        CourseHistoryRepository historyRepository) {

        this.batchService =
            Objects.requireNonNull(
                batchService);

        this.planRepository =
            Objects.requireNonNull(
                planRepository);

        this.substitutionRepository =
            Objects.requireNonNull(
                substitutionRepository);

        this.peCourseService =
            Objects.requireNonNull(
                peCourseService);

        this.generalCourseService =
            Objects.requireNonNull(
                generalCourseService);

        this.enrollmentRepository =
            Objects.requireNonNull(
                enrollmentRepository);

        this.historyRepository =
            Objects.requireNonNull(
                historyRepository);
    }
    /**
     * 接入教务教学班设置。
     */
    void setOfferingAdministrationService(
        CourseOfferingAdministrationService service) {

        this.offeringAdministrationService =
            Objects.requireNonNull(
                service);
    }
    /**
     * 选择教学班。
     */
    synchronized CourseSelectionResult selectCourse(
        String userId,
        String studentId,
        long batchId,
        long offeringId) {

        /*
         * =========================
         * 1. 批次校验
         * =========================
         */
        SelectionBatchInfo batch =
            batchService.findBatch(
                batchId);

        if (batch == null) {

            return CourseSelectionResult.failure(
                "选课批次不存在。");
        }

        if (batch.getStatus()
            != SelectionBatchStatus.OPEN) {

            return CourseSelectionResult.failure(
                "当前批次不在选课时间内。");
        }

        if (!batch.isAllowSelect()) {

            return CourseSelectionResult.failure(
                "当前批次不可选课。");
        }

        /*
         * =========================
         * 2. 组装所有课程
         * =========================
         */
        List<CourseInfo> normalCourses =
            new ArrayList<>();

        /*
         * 方案内。
         */
        normalCourses.addAll(
            planRepository.findPlanCourses(
                batchId));

        /*
         * 方案外。
         */
        normalCourses.addAll(
            substitutionRepository
                .findSubstituteCourses(
                    batchId));

        /*
         * 所有课程。
         */
        List<CourseInfo> courses =
            new ArrayList<>();

        courses.addAll(
            normalCourses);

        /*
         * 体育。
         */
        courses.addAll(
            peCourseService
                .findRawCourses(
                    batchId));

        /*
         * 通选。
         */
        courses.addAll(
            generalCourseService
                .findRawCourses(
                    batchId));

        /*
         * =========================
         * 3. 找目标课程和教学班
         * =========================
         */
        CourseInfo targetCourse =
            null;

        OfferingInfo targetOffering =
            null;

        for (CourseInfo course
            : courses) {

            for (OfferingInfo offering
                : course.getOfferings()) {

                if (offering.getOfferingId()
                    == offeringId) {

                    targetCourse =
                        course;

                    targetOffering =
                        offering;

                    break;
                }
            }

            if (targetOffering != null) {

                break;
            }
        }

        if (targetCourse == null
            || targetOffering == null) {

            return CourseSelectionResult.failure(
                "该教学班不在当前可选课程范围内。");
        }

        /*
         * =========================
         * 4. 判断课程类别
         * =========================
         */
        boolean peCourse =
            peCourseService
                .findRawCourseByOffering(
                    batchId,
                    offeringId)
                != null;

        boolean generalCourse =
            generalCourseService
                .findRawCourseByOffering(
                    batchId,
                    offeringId)
                != null;

        /*
         * =========================
         * 5. 体育课资格校验
         * =========================
         */
        if (peCourse) {

            if (batch.getBatchType()
                == SelectionBatchType.RETAKE) {

                return CourseSelectionResult.failure(
                    "体育课不参加重修选课。");
            }

            CourseInfo visibleCourse =
                peCourseService
                    .findVisibleCourseByOffering(
                        batchId,
                        userId,
                        studentId,
                        offeringId);

            if (visibleCourse == null) {

                return CourseSelectionResult.failure(
                    "不符合该体育课程的选课条件。");
            }

            OfferingInfo visibleOffering =
                findOffering(
                    visibleCourse,
                    offeringId);

            if (visibleOffering == null) {

                return CourseSelectionResult.failure(
                    "不符合该体育课程的选课条件。");
            }

            targetCourse =
                visibleCourse;

            targetOffering =
                visibleOffering;
        }

        /*
         * =========================
         * 6. 通选课资格校验
         * =========================
         */
        if (generalCourse) {

            /*
             * 通选课不参加重修。
             */
            if (batch.getBatchType()
                == SelectionBatchType.RETAKE) {

                return CourseSelectionResult.failure(
                    "通选课不参加重修选课。");
            }

            /*
             * PRE_SELECTION 和 ADD_DROP
             * 都允许进入这里。
             *
             * 两个批次的课程范围完全相同。
             */
            CourseInfo visibleCourse =
                generalCourseService
                    .findVisibleCourseByOffering(
                        batchId,
                        userId,
                        offeringId);

            if (visibleCourse == null) {

                return CourseSelectionResult.failure(
                    "不符合该通选课程的选课条件。");
            }

            OfferingInfo visibleOffering =
                findOffering(
                    visibleCourse,
                    offeringId);

            if (visibleOffering == null) {

                return CourseSelectionResult.failure(
                    "不符合该通选课程的选课条件。");
            }

            /*
             * 使用计算过：
             *
             * - 动态人数
             * - 时间冲突
             * - 已选状态
             *
             * 的课程数据。
             */
            targetCourse =
                visibleCourse;

            targetOffering =
                visibleOffering;
        }

        /*
         * =========================
         * 应用教务设置并进行强制校验
         * =========================
         *
         * 必须放在体育课和通选课重新确定
         * targetOffering 之后，否则设置结果
         * 会被 visibleOffering 覆盖。
         */
        /*
         * 体育课和通选课可能会重新赋值 targetOffering，
         * 因此教务设置必须在它们之后应用。
         */
        if (offeringAdministrationService
            == null) {

            return CourseSelectionResult.failure(
                "教学班设置服务未初始化。");
        }

        targetOffering =
            offeringAdministrationService
                .applyStudentSettings(
                    batchId,
                    targetOffering);

        if ("OFFERING_CLOSED".equals(
            targetOffering
                .getAvailabilityStatus())) {

            return CourseSelectionResult.failure(
                "当前教学班已关闭，不可选课。");
        }
        /*
         * =========================
         * 7. 普通课程历史修读资格
         * =========================
         *
         * 体育和通选都不参与普通课程
         * 历史修读 / 重修资格逻辑。
         */
        if (!peCourse
            && !generalCourse) {

            boolean previouslyTaken =
                historyRepository
                    .hasTakenCourse(
                        userId,
                        targetCourse
                            .getCourseId());

            /*
             * 重修：
             * 以前必须修过。
             */
            if (batch.getBatchType()
                == SelectionBatchType.RETAKE
                && !previouslyTaken) {

                return CourseSelectionResult.failure(
                    "不符合重修选课条件。");
            }

            /*
             * 普通批次：
             * 修过以后不能普通再选。
             */
            if (batch.getBatchType()
                != SelectionBatchType.RETAKE
                && previouslyTaken) {

                return CourseSelectionResult.failure(
                    "该课程已修读，请通过重修选课。");
            }
        }

        /*
         * =========================
         * 8. 同一门课程不能重复选择
         * =========================
         *
         * 通选允许选多门，
         * 但必须是不同课程。
         *
         * 例如：
         *
         * 中国传统文化
         * +
         * 大学生心理健康
         *
         * 可以。
         *
         * 同一门“中国传统文化”
         * 两个教学班不能同时选。
         */
        for (OfferingInfo offering
            : targetCourse
            .getOfferings()) {

            if (enrollmentRepository
                .isOfferingSelected(
                    userId,
                    offering.getOfferingId())) {

                return CourseSelectionResult.failure(
                    "已经选择该课程。");
            }
        }

        /*
         * =========================
         * 9. 培养方案替代关系
         * =========================
         *
         * 体育课和通选课都不参与。
         */
        if (!peCourse
            && !generalCourse
            && hasSelectedEquivalentCourse(
            userId,
            targetCourse,
            normalCourses)) {

            return CourseSelectionResult.failure(
                "对应培养方案要求已通过其他课程满足。");
        }

        /*
         * =========================
         * 10. 容量检查
         * =========================
         */
        int currentSelectedCount;

        if (peCourse
            || generalCourse) {

            /*
             * 体育和通选的 Service
             * 已经把运行期间新增人数
             * 计算进 targetOffering。
             */
            currentSelectedCount =
                targetOffering
                    .getSelectedCount();

        } else {

            currentSelectedCount =
                targetOffering
                    .getSelectedCount()
                    + enrollmentRepository
                    .countAdditionalSelections(
                        offeringId);
        }

        if (currentSelectedCount
            >= targetOffering.getCapacity()) {

            return CourseSelectionResult.failure(
                "人数已满。");
        }

        /*
         * =========================
         * 11. 当前教学班动态状态
         * =========================
         */
        if (!"AVAILABLE".equals(
            targetOffering
                .getAvailabilityStatus())) {

            return CourseSelectionResult.failure(
                availabilityMessage(
                    targetOffering
                        .getAvailabilityStatus()));
        }

        /*
         * =========================
         * 12. Server 再次检查时间冲突
         * =========================
         *
         * 这里 courses 已经包括：
         *
         * 方案内
         * 方案外
         * 体育
         * 通选
         */
        List<OfferingInfo> selectedOfferings =
            findSelectedOfferings(
                userId,
                courses);

        if (conflictChecker.hasConflict(
            targetOffering,
            selectedOfferings)) {

            return CourseSelectionResult.failure(
                "时间冲突。");
        }

        /*
         * =========================
         * 13. 保存选课
         * =========================
         */
        enrollmentRepository.select(
            userId,
            studentId,
            batchId,
            offeringId);

        return CourseSelectionResult.success(
            "选课成功。");
    }
    /**
     * 教务老师为指定学生强制选课。
     *
     * 强制选课不检查：
     *
     * - 选课批次是否开放
     * - 课程容量
     * - 时间冲突
     * - 体育课性别限制
     * - 历史修读及重修条件
     *
     * 但课程、教学班和批次必须真实存在，
     * 并且不能重复选择同一门课程。
     */
    /**
     * 教务端查询指定批次的全部课程和教学班。
     */
    List<CourseInfo> listAllCoursesForAdmin(
        long batchId) {

        if (batchService.findBatch(
            batchId) == null) {

            return List.of();
        }

        List<CourseInfo> courses =
            new ArrayList<>();

        courses.addAll(
            planRepository.findPlanCourses(
                batchId));

        courses.addAll(
            substitutionRepository
                .findSubstituteCourses(
                    batchId));

        courses.addAll(
            peCourseService.findRawCourses(
                batchId));

        courses.addAll(
            generalCourseService.findRawCourses(
                batchId));

        return List.copyOf(
            courses);
    }
    synchronized CourseSelectionResult forceSelectCourse(
        String studentId,
        long batchId,
        long offeringId) {

        if (studentId == null
            || studentId.isBlank()) {

            return CourseSelectionResult.failure(
                "学生学号不能为空。");
        }

        String cleanStudentId =
            studentId.trim();

        SelectionBatchInfo batch =
            batchService.findBatch(
                batchId);

        if (batch == null) {

            return CourseSelectionResult.failure(
                "选课批次不存在。");
        }

        /*
         * 汇总该批次的全部课程。
         */
        List<CourseInfo> courses =
            new ArrayList<>();

        courses.addAll(
            planRepository.findPlanCourses(
                batchId));

        courses.addAll(
            substitutionRepository
                .findSubstituteCourses(
                    batchId));

        courses.addAll(
            peCourseService.findRawCourses(
                batchId));

        courses.addAll(
            generalCourseService.findRawCourses(
                batchId));

        /*
         * 查找目标课程和教学班。
         */
        CourseInfo targetCourse =
            null;

        OfferingInfo targetOffering =
            null;

        for (CourseInfo course
            : courses) {

            for (OfferingInfo offering
                : course.getOfferings()) {

                if (offering.getOfferingId()
                    == offeringId) {

                    targetCourse =
                        course;

                    targetOffering =
                        offering;

                    break;
                }
            }

            if (targetOffering != null) {

                break;
            }
        }

        if (targetCourse == null
            || targetOffering == null) {

            return CourseSelectionResult.failure(
                "课程或教学班不存在。");
        }

        /*
         * 即使是强制选课，也不能重复选择
         * 同一门课程的两个教学班。
         */
        for (OfferingInfo offering
            : targetCourse.getOfferings()) {

            if (enrollmentRepository
                .isOfferingSelected(
                    cleanStudentId,
                    offering.getOfferingId())) {

                return CourseSelectionResult.failure(
                    "该学生已经选择了这门课程。");
            }
        }

        /*
         * 学号作为课程模块内部的学生标识。
         */
        enrollmentRepository.select(
            cleanStudentId,
            cleanStudentId,
            batchId,
            offeringId);

        return CourseSelectionResult.success(
            "强制选课成功。");
    }
    /**
     * 在课程中找到教学班。
     */
    private OfferingInfo findOffering(
        CourseInfo course,
        long offeringId) {

        for (OfferingInfo offering
            : course.getOfferings()) {

            if (offering.getOfferingId()
                == offeringId) {

                return offering;
            }
        }

        return null;
    }

    /**
     * 找当前学生所有已选教学班。
     */
    private List<OfferingInfo>
    findSelectedOfferings(
        String userId,
        List<CourseInfo> courses) {

        Set<Long> selectedIds =
            enrollmentRepository
                .findSelectedOfferingIds(
                    userId);

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
     * 检查等效培养方案课程。
     */
    private boolean hasSelectedEquivalentCourse(
        String userId,
        CourseInfo targetCourse,
        List<CourseInfo> allCourses) {

        long targetRequirementId =
            requirementCourseId(
                targetCourse
                    .getCourseId());

        for (CourseInfo candidate
            : allCourses) {

            if (candidate.getCourseId()
                == targetCourse
                .getCourseId()) {

                continue;
            }

            if (requirementCourseId(
                candidate.getCourseId())
                != targetRequirementId) {

                continue;
            }

            for (OfferingInfo offering
                : candidate
                .getOfferings()) {

                if (enrollmentRepository
                    .isOfferingSelected(
                        userId,
                        offering
                            .getOfferingId())) {

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 最终满足哪个培养方案课程。
     */
    private long requirementCourseId(
        long courseId) {

        Long replacedCourseId =
            substitutionRepository
                .findReplacedCourseId(
                    courseId);

        return replacedCourseId == null
            ? courseId
            : replacedCourseId;
    }

    /**
     * 教学班不可选原因。
     */
    private String availabilityMessage(
        String status) {

        return switch (status) {

            case "FULL" ->
                "人数已满。";

            case "TIME_CONFLICT" ->
                "时间冲突。";

            case "NOT_ELIGIBLE" ->
                "不符合选课条件。";

            case "COURSE_ALREADY_SELECTED",
                 "SELECTED" ->
                "已经选择该课程。";

            case "REQUIREMENT_SATISFIED" ->
                "对应培养方案要求已通过其他课程满足。";

            case "OFFERING_CLOSED" ->
                "当前教学班不可选。";

            default ->
                "当前教学班不可选。";
        };
    }
}

/**
 * Server 内部选课结果。
 */
record CourseSelectionResult(
    boolean success,
    String message) {

    static CourseSelectionResult success(
        String message) {

        return new CourseSelectionResult(
            true,
            message);
    }

    static CourseSelectionResult failure(
        String message) {

        return new CourseSelectionResult(
            false,
            message);
    }
}
