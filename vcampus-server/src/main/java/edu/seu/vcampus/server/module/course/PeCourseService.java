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
 * 体育课业务服务。
 */
final class PeCourseService {

    private final CourseBatchService
        batchService;

    private final PeCourseRepository
        peRepository;

    private final CoursePlanRepository
        planRepository;

    private final CourseSubstitutionRepository
        substitutionRepository;
    private final GeneralCourseRepository
        generalCourseRepository;
    private final CourseEnrollmentRepository
        enrollmentRepository;

    private final StudentGenderRepository
        genderRepository;

    private final CourseScheduleConflictChecker
        conflictChecker =
        new CourseScheduleConflictChecker();

    PeCourseService(
        CourseBatchService batchService,
        PeCourseRepository peRepository,
        CoursePlanRepository planRepository,
        CourseSubstitutionRepository substitutionRepository,
        GeneralCourseRepository generalCourseRepository,
        CourseEnrollmentRepository enrollmentRepository,
        StudentGenderRepository genderRepository) {

        this.batchService =
            Objects.requireNonNull(
                batchService);

        this.peRepository =
            Objects.requireNonNull(
                peRepository);

        this.planRepository =
            Objects.requireNonNull(
                planRepository);

        this.substitutionRepository =
            Objects.requireNonNull(
                substitutionRepository);
        this.generalCourseRepository =
            Objects.requireNonNull(
                generalCourseRepository);
        this.enrollmentRepository =
            Objects.requireNonNull(
                enrollmentRepository);

        this.genderRepository =
            Objects.requireNonNull(
                genderRepository);
    }

    /**
     * 查询体育课。
     *
     * userId 用于查当前选课记录。
     * studentId 用于读取学籍性别。
     */
    List<CourseInfo> listPeCourses(
        long batchId,
        String sportProject,
        String userId,
        String studentId) {

        SelectionBatchInfo batch =
            batchService.findBatch(
                batchId);

        if (batch == null) {

            throw new IllegalArgumentException(
                "selection batch does not exist");
        }

        /*
         * 体育课不参加重修。
         */
        if (batch.getBatchType()
            == SelectionBatchType.RETAKE) {

            return List.of();
        }

        StudentGender gender =
            genderRepository.findGender(
                studentId);

        List<PeCourseRecord> records =
            peRepository.findPeCourses(
                batchId);

        List<OfferingInfo> selectedOfferings =
            findSelectedOfferings(
                batchId,
                userId,
                records);

        List<CourseInfo> result =
            new ArrayList<>();

        for (PeCourseRecord record
            : records) {

            if (sportProject != null
                && !sportProject.equals(
                record.sportProject())) {

                continue;
            }

            CourseInfo course =
                decorateCourse(
                    record,
                    userId,
                    gender,
                    selectedOfferings);

            /*
             * 学生性别不符合所有教学班时，
             * 整门体育课隐藏。
             */
            if (course != null) {

                result.add(
                    course);
            }
        }

        return result;
    }

    /**
     * 为选课 Service 提供全部体育课程原始数据。
     */
    List<CourseInfo> findRawCourses(
        long batchId) {

        return peRepository
            .findPeCourses(
                batchId)
            .stream()
            .map(
                PeCourseRecord::course)
            .toList();
    }

    /**
     * 找体育教学班原始课程。
     */
    CourseInfo findRawCourseByOffering(
        long batchId,
        long offeringId) {

        for (PeCourseRecord record
            : peRepository.findPeCourses(
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
     * 根据学生当前性别取得
     * 一个体育教学班最终可选状态。
     */
    CourseInfo findVisibleCourseByOffering(
        long batchId,
        String userId,
        String studentId,
        long offeringId) {

        List<CourseInfo> courses =
            listPeCourses(
                batchId,
                null,
                userId,
                studentId);

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

    private CourseInfo decorateCourse(
        PeCourseRecord record,
        String userId,
        StudentGender gender,
        List<OfferingInfo> selectedOfferings) {

        boolean courseSelected =
            record.course()
                .getOfferings()
                .stream()
                .anyMatch(offering ->
                    enrollmentRepository
                        .isOfferingSelected(
                            userId,
                            offering
                                .getOfferingId()));

        List<OfferingInfo> visibleOfferings =
            new ArrayList<>();

        for (OfferingInfo offering
            : record.course()
            .getOfferings()) {

            PeOfferingRule rule =
                findRule(
                    record,
                    offering.getOfferingId());

            if (rule == null) {
                continue;
            }

            if (!isGenderEligible(
                gender,
                rule.genderRestriction())) {

                continue;
            }

            visibleOfferings.add(
                decorateOffering(
                    offering,
                    rule,
                    userId,
                    gender,
                    courseSelected,
                    selectedOfferings));
        }

        if (visibleOfferings.isEmpty()) {

            return null;
        }

        return new CourseInfo(
            record.course()
                .getCourseId(),
            record.course()
                .getCourseCode(),
            record.course()
                .getCourseName(),
            record.course()
                .getCredits(),
            record.course()
                .getCourseType(),
            courseSelected,
            visibleOfferings);
    }

    private OfferingInfo decorateOffering(
        OfferingInfo offering,
        PeOfferingRule rule,
        String userId,
        StudentGender gender,
        boolean courseSelected,
        List<OfferingInfo> selectedOfferings) {

        boolean selected =
            enrollmentRepository
                .isOfferingSelected(
                    userId,
                    offering.getOfferingId());

        /*
         * 当前运行期间总新增人数。
         */
        int additionalTotal =
            enrollmentRepository
                .countAdditionalSelections(
                    offering.getOfferingId());

        int totalSelected =
            offering.getSelectedCount()
                + additionalTotal;

        int totalRemaining =
            Math.max(
                offering.getCapacity()
                    - totalSelected,
                0);

        int selectedCount;
        int capacity;
        int remaining;

        /*
         * 男女分别设置人数。
         */
        if (rule.genderRestriction()
            == PeGenderRestriction.MIXED_SPLIT) {

            int additionalGender =
                countAdditionalSelections(
                    offering.getOfferingId(),
                    gender);

            if (gender
                == StudentGender.MALE) {

                selectedCount =
                    rule.maleSelectedCount()
                        + additionalGender;

                capacity =
                    Objects.requireNonNull(
                        rule.maleCapacity());

            } else {

                selectedCount =
                    rule.femaleSelectedCount()
                        + additionalGender;

                capacity =
                    Objects.requireNonNull(
                        rule.femaleCapacity());
            }

            int genderRemaining =
                Math.max(
                    capacity
                        - selectedCount,
                    0);

            /*
             * 男女名额和总名额都必须还有位置。
             */
            remaining =
                Math.min(
                    genderRemaining,
                    totalRemaining);

        } else {

            /*
             * 男生限定 / 女生限定 /
             * 不限制性别。
             */
            selectedCount =
                totalSelected;

            capacity =
                offering.getCapacity();

            remaining =
                totalRemaining;
        }

        String status;

        if (selected) {

            status =
                "SELECTED";

        } else if (courseSelected) {

            status =
                "COURSE_ALREADY_SELECTED";

        } else if (remaining <= 0) {

            status =
                "FULL";

        } else if (conflictChecker.hasConflict(
            offering,
            selectedOfferings)) {

            status =
                "TIME_CONFLICT";

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
            remaining,
            selected,
            status);
    }

    /**
     * 计算当前运行期间某性别
     * 新增了多少人。
     */
    private int countAdditionalSelections(
        long offeringId,
        StudentGender gender) {

        int result =
            0;

        List<CourseEnrollmentRecord> records =
            enrollmentRepository
                .findSelectedEnrollmentsByOffering(
                    offeringId);

        for (CourseEnrollmentRecord record
            : records) {

            StudentGender selectedGender =
                genderRepository.findGender(
                    record.studentId());

            if (selectedGender == gender) {

                result++;
            }
        }

        return result;
    }

    private List<OfferingInfo> findSelectedOfferings(
        long batchId,
        String userId,
        List<PeCourseRecord> peRecords) {

        Set<Long> selectedIds =
            enrollmentRepository
                .findSelectedOfferingIds(
                    userId);

        List<CourseInfo> allCourses =
            new ArrayList<>();

        allCourses.addAll(
            planRepository.findPlanCourses(
                batchId));

        allCourses.addAll(
            substitutionRepository
                .findSubstituteCourses(
                    batchId));

        for (PeCourseRecord record
            : peRecords) {

            allCourses.add(
                record.course());
        }
        /*
         * 通选课。
         *
         * 学生选择通选课后，
         * 体育课页面也要检查与其时间冲突。
         */
        for (GeneralCourseRecord record
            : generalCourseRepository
            .findGeneralCourses(
                batchId)) {

            allCourses.add(
                record.course());
        }
        List<OfferingInfo> result =
            new ArrayList<>();

        for (CourseInfo course
            : allCourses) {

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

    private PeOfferingRule findRule(
        PeCourseRecord record,
        long offeringId) {

        for (PeOfferingRule rule
            : record.offeringRules()) {

            if (rule.offeringId()
                == offeringId) {

                return rule;
            }
        }

        return null;
    }

    private boolean isGenderEligible(
        StudentGender gender,
        PeGenderRestriction restriction) {

        return switch (restriction) {

            case MALE_ONLY ->
                gender == StudentGender.MALE;

            case FEMALE_ONLY ->
                gender == StudentGender.FEMALE;

            case MIXED_SPLIT,
                 UNRESTRICTED ->
                true;
        };
    }
}
