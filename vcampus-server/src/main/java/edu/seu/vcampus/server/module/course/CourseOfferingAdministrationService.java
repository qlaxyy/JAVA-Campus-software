package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 教务端教学班管理业务。
 */
final class CourseOfferingAdministrationService {

    private final CourseBatchService
        batchService;

    private final CoursePlanRepository
        planRepository;
    private final CourseSettingsRepository
        courseSettingsRepository =
        new InMemoryCourseSettingsRepository();
    private final CourseSubstitutionRepository
        substitutionRepository;

    private final PeCourseRepository
        peCourseRepository;

    private final GeneralCourseRepository
        generalCourseRepository;

    private final CourseEnrollmentRepository
        enrollmentRepository;

    private final CourseOfferingSettingsRepository
        settingsRepository;

    CourseOfferingAdministrationService(
        CourseBatchService batchService,
        CoursePlanRepository planRepository,
        CourseSubstitutionRepository
            substitutionRepository,
        PeCourseRepository peCourseRepository,
        GeneralCourseRepository
            generalCourseRepository,
        CourseEnrollmentRepository
            enrollmentRepository,
        CourseOfferingSettingsRepository
            settingsRepository) {

        this.batchService =
            Objects.requireNonNull(
                batchService);

        this.planRepository =
            Objects.requireNonNull(
                planRepository);

        this.substitutionRepository =
            Objects.requireNonNull(
                substitutionRepository);

        this.peCourseRepository =
            Objects.requireNonNull(
                peCourseRepository);

        this.generalCourseRepository =
            Objects.requireNonNull(
                generalCourseRepository);

        this.enrollmentRepository =
            Objects.requireNonNull(
                enrollmentRepository);

        this.settingsRepository =
            Objects.requireNonNull(
                settingsRepository);
    }

    /**
     * 查询应用教务设置后的全部课程。
     */
    List<CourseInfo> listCourses(
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

        for (PeCourseRecord record
            : peCourseRepository
            .findPeCourses(
                batchId)) {

            courses.add(
                record.course());
        }

        for (GeneralCourseRecord record
            : generalCourseRepository
            .findGeneralCourses(
                batchId)) {

            courses.add(
                record.course());
        }

        return courses.stream()
            .map(course ->
                applyCourseSettings(
                    batchId,
                    applySettings(
                        batchId,
                        course)))
            .toList();
    }

    /**
     * 教务修改课程基本信息。
     */
    synchronized CourseUpdateResult updateCourse(
        long batchId,
        long courseId,
        String courseCode,
        String courseName,
        double credits,
        String courseType) {

        if (batchService.findBatch(
            batchId) == null) {

            return CourseUpdateResult.failure(
                "选课批次不存在。");
        }

        if (courseCode == null
            || courseCode.isBlank()) {

            return CourseUpdateResult.failure(
                "课程代码不能为空。");
        }

        if (courseName == null
            || courseName.isBlank()) {

            return CourseUpdateResult.failure(
                "课程名称不能为空。");
        }

        if (credits <= 0) {

            return CourseUpdateResult.failure(
                "课程学分必须大于零。");
        }

        if (courseType == null
            || courseType.isBlank()) {

            return CourseUpdateResult.failure(
                "课程类型不能为空。");
        }

        CourseInfo targetCourse =
            null;

        for (CourseInfo course
            : listCourses(batchId)) {

            if (course.getCourseId()
                == courseId) {

                targetCourse =
                    course;

                break;
            }
        }

        if (targetCourse == null) {

            return CourseUpdateResult.failure(
                "课程不存在。");
        }

        String normalizedCode =
            courseCode.trim();

        String normalizedName =
            courseName.trim();

        String normalizedType =
            courseType.trim();

        /*
         * 同一批次内课程代码不能重复。
         */
        for (CourseInfo course
            : listCourses(batchId)) {

            if (course.getCourseId()
                != courseId
                && normalizedCode.equalsIgnoreCase(
                course.getCourseCode())) {

                return CourseUpdateResult.failure(
                    "该课程代码已经被其他课程使用。");
            }
        }

        courseSettingsRepository.save(
            new CourseSettings(
                batchId,
                courseId,
                normalizedCode,
                normalizedName,
                credits,
                normalizedType));

        CourseInfo updatedCourse =
            applyCourseSettings(
                batchId,
                targetCourse);

        return CourseUpdateResult.success(
            "课程信息修改成功。",
            updatedCourse);
    }
    /**
     * 应用教务修改后的课程基本信息。
     */
    private CourseInfo applyCourseSettings(
        long batchId,
        CourseInfo course) {

        CourseSettings settings =
            courseSettingsRepository
                .find(
                    batchId,
                    course.getCourseId())
                .orElse(null);

        if (settings == null) {

            return course;
        }

        return new CourseInfo(
            course.getCourseId(),
            settings.courseCode(),
            settings.courseName(),
            settings.credits(),
            settings.courseType(),
            course.isSelected(),
            course.getOfferings());
    }
    /**
     * 修改教学班容量和开放状态。
     */
    synchronized CourseOfferingUpdateResult
    updateOffering(
        long batchId,
        long offeringId,
        int capacity,
        boolean open) {

        if (batchService.findBatch(
            batchId) == null) {

            return CourseOfferingUpdateResult
                .failure(
                    "选课批次不存在。");
        }

        OfferingInfo offering =
            findOffering(
                batchId,
                offeringId);

        if (offering == null) {

            return CourseOfferingUpdateResult
                .failure(
                    "教学班不存在。");
        }

        int selectedCount =
            offering.getSelectedCount()
                + enrollmentRepository
                .countAdditionalSelections(
                    offeringId);

        if (capacity < selectedCount) {

            return CourseOfferingUpdateResult
                .failure(
                    "容量不能小于当前已选人数 "
                        + selectedCount
                        + "。");
        }

        settingsRepository.save(
            new CourseOfferingSettings(
                batchId,
                offeringId,
                capacity,
                open));

        return CourseOfferingUpdateResult
            .success(
                "教学班设置修改成功。");
    }
    /**
     * 将教学班设置应用到学生端课程。
     *
     * 学生端课程已经计算过已选人数和冲突状态，
     * 因此这里不重复增加选课人数。
     */
    /**
     * 将课程信息设置和教学班设置
     * 同时应用到学生端课程。
     */
    CourseInfo applyStudentSettings(
        long batchId,
        CourseInfo course) {

        /*
         * 先应用课程代码、名称、学分和类型设置。
         */
        CourseInfo updatedCourse =
            applyCourseSettings(
                batchId,
                course);

        /*
         * 再应用各教学班的容量和开放状态。
         */
        List<OfferingInfo> updatedOfferings =
            updatedCourse.getOfferings()
                .stream()
                .map(offering ->
                    applyStudentSettings(
                        batchId,
                        offering))
                .toList();

        return new CourseInfo(
            updatedCourse.getCourseId(),
            updatedCourse.getCourseCode(),
            updatedCourse.getCourseName(),
            updatedCourse.getCredits(),
            updatedCourse.getCourseType(),
            updatedCourse.isSelected(),
            updatedOfferings);
    }

    /**
     * 将容量和开放状态应用到学生端教学班。
     */
    OfferingInfo applyStudentSettings(
        long batchId,
        OfferingInfo offering) {

        CourseOfferingSettings settings =
            settingsRepository
                .find(
                    batchId,
                    offering.getOfferingId())
                .orElse(null);

        if (settings == null) {

            return offering;
        }

        int capacity =
            settings.capacity();

        int selectedCount =
            offering.getSelectedCount();

        int remainingCount =
            Math.max(
                capacity - selectedCount,
                0);

        String status;

        if (offering.isSelected()) {

            status =
                "SELECTED";

        } else if (!settings.open()) {

            status =
                "OFFERING_CLOSED";

        } else if (remainingCount <= 0) {

            status =
                "FULL";

        } else if ("FULL".equals(
            offering.getAvailabilityStatus())
            || "OFFERING_CLOSED".equals(
            offering.getAvailabilityStatus())) {

            status =
                "AVAILABLE";

        } else {

            /*
             * 保留时间冲突、同课程已选等
             * 学生个人状态。
             */
            status =
                offering.getAvailabilityStatus();
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
            capacity,
            remainingCount,
            offering.isSelected(),
            status);
    }
    /**
     * 将教务设置应用到一门课程。
     */
    CourseInfo applySettings(
        long batchId,
        CourseInfo course) {

        List<OfferingInfo> offerings =
            course.getOfferings()
                .stream()
                .map(offering ->
                    applySettings(
                        batchId,
                        offering))
                .toList();

        return new CourseInfo(
            course.getCourseId(),
            course.getCourseCode(),
            course.getCourseName(),
            course.getCredits(),
            course.getCourseType(),
            course.isSelected(),
            offerings);
    }

    /**
     * 将教务设置应用到一个教学班。
     */
    OfferingInfo applySettings(
        long batchId,
        OfferingInfo offering) {

        CourseOfferingSettings settings =
            settingsRepository
                .find(
                    batchId,
                    offering.getOfferingId())
                .orElse(null);

        int capacity =
            settings == null
                ? offering.getCapacity()
                : settings.capacity();

        boolean open =
            settings == null
                ? !"OFFERING_CLOSED".equals(
                offering
                    .getAvailabilityStatus())
                : settings.open();

        int selectedCount =
            offering.getSelectedCount()
                + enrollmentRepository
                .countAdditionalSelections(
                    offering.getOfferingId());

        int remainingCount =
            Math.max(
                capacity - selectedCount,
                0);

        String status;

        if (offering.isSelected()) {

            status =
                "SELECTED";

        } else if (!open) {

            status =
                "OFFERING_CLOSED";

        } else if (remainingCount <= 0) {

            status =
                "FULL";

        } else {

            status =
                "AVAILABLE";
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
            capacity,
            remainingCount,
            offering.isSelected(),
            status);
    }

    /**
     * 根据 ID 查找原始教学班。
     */
    private OfferingInfo findOffering(
        long batchId,
        long offeringId) {

        for (CourseInfo course
            : rawCourses(
            batchId)) {

            for (OfferingInfo offering
                : course.getOfferings()) {

                if (offering.getOfferingId()
                    == offeringId) {

                    return offering;
                }
            }
        }

        return null;
    }

    private List<CourseInfo> rawCourses(
        long batchId) {

        List<CourseInfo> courses =
            new ArrayList<>();

        courses.addAll(
            planRepository.findPlanCourses(
                batchId));

        courses.addAll(
            substitutionRepository
                .findSubstituteCourses(
                    batchId));

        for (PeCourseRecord record
            : peCourseRepository
            .findPeCourses(
                batchId)) {

            courses.add(
                record.course());
        }

        for (GeneralCourseRecord record
            : generalCourseRepository
            .findGeneralCourses(
                batchId)) {

            courses.add(
                record.course());
        }

        return courses;
    }
}
/**
 * 修改课程信息的结果。
 */
record CourseUpdateResult(
    boolean success,
    String message,
    CourseInfo course) {

    static CourseUpdateResult success(
        String message,
        CourseInfo course) {

        return new CourseUpdateResult(
            true,
            message,
            course);
    }

    static CourseUpdateResult failure(
        String message) {

        return new CourseUpdateResult(
            false,
            message,
            null);
    }
}
/**
 * 教学班修改结果。
 */
record CourseOfferingUpdateResult(
    boolean success,
    String message) {

    static CourseOfferingUpdateResult success(
        String message) {

        return new CourseOfferingUpdateResult(
            true,
            message);
    }

    static CourseOfferingUpdateResult failure(
        String message) {

        return new CourseOfferingUpdateResult(
            false,
            message);
    }
}
