package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 教务端课程和教学班管理业务。
 *
 * 课程及教学班是全局数据，
 * 不按照选课批次区分。
 */
final class CourseOfferingAdministrationService {

    private static final long REPOSITORY_SCOPE =
        0L;

    private final CoursePlanRepository
        planRepository;

    private final CourseSettingsRepository
        courseSettingsRepository;

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
        CoursePlanRepository planRepository,
        CourseSubstitutionRepository
            substitutionRepository,
        PeCourseRepository peCourseRepository,
        GeneralCourseRepository
            generalCourseRepository,
        CourseEnrollmentRepository
            enrollmentRepository,
        CourseOfferingSettingsRepository
            settingsRepository,
        CourseSettingsRepository
            courseSettingsRepository) {

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

        this.courseSettingsRepository =
            Objects.requireNonNull(
                courseSettingsRepository);
    }

    /**
     * 查询全部课程。
     */
    List<CourseInfo> listCourses() {

        return rawCourses()
            .stream()
            .map(this::applySettings)
            .map(this::applyCourseSettings)
            .toList();
    }

    /**
     * 兼容教师端和统计功能现有调用。
     *
     * 课程数据不再按照批次区分。
     */
    List<CourseInfo> listCourses(
        long ignoredBatchId) {

        return listCourses();
    }

    /**
     * 教务修改课程基本信息。
     */
    synchronized CourseUpdateResult updateCourse(
        long courseId,
        String courseCode,
        String courseName,
        double credits,
        String courseType) {

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
            : listCourses()) {

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

        for (CourseInfo course
            : listCourses()) {

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
                courseId,
                normalizedCode,
                normalizedName,
                credits,
                normalizedType));

        CourseInfo updatedCourse =
            applyCourseSettings(
                targetCourse);

        return CourseUpdateResult.success(
            "课程信息修改成功。",
            updatedCourse);
    }

    /**
     * 应用教务修改后的课程基本信息。
     */
    private CourseInfo applyCourseSettings(
        CourseInfo course) {

        CourseSettings settings =
            courseSettingsRepository
                .find(
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
            course.getDepartmentName(),
            course.isSelected(),
            course.getOfferings());
    }

    /**
     * 修改教学班容量和开放状态。
     */
    synchronized CourseOfferingUpdateResult
    updateOffering(
        long offeringId,
        int capacity,
        boolean open) {

        OfferingInfo offering =
            findOffering(
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
                offeringId,
                capacity,
                open));

        return CourseOfferingUpdateResult
            .success(
                "教学班设置修改成功。");
    }

    /**
     * 将全局课程和教学班设置应用到学生端课程。
     *
     * 参数保留仅用于兼容学生端现有调用，
     * 不影响课程或教学班设置。
     */
    CourseInfo applyStudentSettings(
        long ignoredBatchId,
        CourseInfo course) {

        CourseInfo updatedCourse =
            applyCourseSettings(
                course);

        List<OfferingInfo> updatedOfferings =
            updatedCourse.getOfferings()
                .stream()
                .map(this::applyStudentSettings)
                .toList();

        return new CourseInfo(
            updatedCourse.getCourseId(),
            updatedCourse.getCourseCode(),
            updatedCourse.getCourseName(),
            updatedCourse.getCredits(),
            updatedCourse.getCourseType(),
            updatedCourse.getDepartmentName(),
            updatedCourse.isSelected(),
            updatedOfferings);
    }

    /**
     * 将全局教学班设置应用到学生端教学班。
     */
    OfferingInfo applyStudentSettings(
        long ignoredBatchId,
        OfferingInfo offering) {

        return applyStudentSettings(
            offering);
    }

    private OfferingInfo applyStudentSettings(
        OfferingInfo offering) {

        CourseOfferingSettings settings =
            settingsRepository
                .find(
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
     * 将全局设置应用到课程。
     *
     * 参数保留仅用于兼容教师端和统计功能。
     */
    CourseInfo applySettings(
        long ignoredBatchId,
        CourseInfo course) {

        return applyCourseSettings(
            applySettings(
                course));
    }

    /**
     * 将全局设置应用到教学班。
     */
    OfferingInfo applySettings(
        long ignoredBatchId,
        OfferingInfo offering) {

        return applySettings(
            offering);
    }

    private CourseInfo applySettings(
        CourseInfo course) {

        List<OfferingInfo> offerings =
            course.getOfferings()
                .stream()
                .map(this::applySettings)
                .toList();

        return new CourseInfo(
            course.getCourseId(),
            course.getCourseCode(),
            course.getCourseName(),
            course.getCredits(),
            course.getCourseType(),
            course.getDepartmentName(),
            course.isSelected(),
            offerings);
    }

    private OfferingInfo applySettings(
        OfferingInfo offering) {

        CourseOfferingSettings settings =
            settingsRepository
                .find(
                    offering.getOfferingId())
                .orElse(null);

        int capacity =
            settings == null
                ? offering.getCapacity()
                : settings.capacity();

        boolean open =
            settings == null
                ? !"OFFERING_CLOSED".equals(
                offering.getAvailabilityStatus())
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
        long offeringId) {

        for (CourseInfo course
            : rawCourses()) {

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

    /**
     * 查询各类仓库中的原始课程。
     */
    private List<CourseInfo> rawCourses() {

        List<CourseInfo> courses =
            new ArrayList<>();

        courses.addAll(
            planRepository.findPlanCourses(
                REPOSITORY_SCOPE));

        courses.addAll(
            substitutionRepository
                .findSubstituteCourses(
                    REPOSITORY_SCOPE));

        for (PeCourseRecord record
            : peCourseRepository.findPeCourses(
            REPOSITORY_SCOPE)) {

            courses.add(
                record.course());
        }

        for (GeneralCourseRecord record
            : generalCourseRepository
            .findGeneralCourses(
                REPOSITORY_SCOPE)) {

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
