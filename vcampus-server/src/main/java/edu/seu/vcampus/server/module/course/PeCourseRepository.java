package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;

import java.util.List;

/**
 * 体育课程 Repository。
 */
interface PeCourseRepository {

    List<PeCourseRecord> findPeCourses(
        long batchId);
}

/**
 * 体育课程内部记录。
 */
record PeCourseRecord(
    CourseInfo course,
    String sportProject,
    List<PeOfferingRule> offeringRules) {
}

/**
 * 体育教学班特殊规则。
 */
record PeOfferingRule(
    long offeringId,
    PeGenderRestriction genderRestriction,
    Integer maleCapacity,
    Integer femaleCapacity,
    int maleSelectedCount,
    int femaleSelectedCount) {
}

/**
 * 体育教学班性别规则。
 */
enum PeGenderRestriction {

    /**
     * 仅男生。
     */
    MALE_ONLY,

    /**
     * 仅女生。
     */
    FEMALE_ONLY,

    /**
     * 男女均可，但男女分别限制人数，
     * 同时仍受整个教学班总容量约束。
     */
    MIXED_SPLIT,

    /**
     * 不限制性别，只使用总容量。
     */
    UNRESTRICTED
}
