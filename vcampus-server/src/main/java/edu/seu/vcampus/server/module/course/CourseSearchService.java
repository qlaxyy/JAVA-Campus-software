package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.CourseSearchItem;
import edu.seu.vcampus.common.course.CourseSearchRequest;
import edu.seu.vcampus.common.course.CourseSearchResult;
import edu.seu.vcampus.common.course.OfferingInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 全校课程查询业务。
 */
final class CourseSearchService {

    /**
     * 项目需求固定每页 20 条。
     *
     * 即使客户端传其他 pageSize，
     * Server 仍然使用 20。
     */
    private static final int PAGE_SIZE = 20;

    private final CourseCatalogRepository
        repository;

    CourseSearchService(
        CourseCatalogRepository repository) {

        this.repository =
            Objects.requireNonNull(
                repository);
    }

    /**
     * 查询当前学期全校开课教学班。
     */
    CourseSearchResult search(
        CourseSearchRequest request) {

        Objects.requireNonNull(
            request,
            "request must not be null");

        /*
         * =========================
         * 1. 筛选
         * =========================
         */
        List<CourseSearchItem> filtered =
            new ArrayList<>();

        for (CourseCatalogRecord record
            : repository
            .findCurrentSemesterCourses()) {

            CourseInfo course =
                record.course();

            /*
             * 课程号。
             */
            if (!containsIgnoreCase(
                course.getCourseCode(),
                request.getCourseCode())) {

                continue;
            }

            /*
             * 课程名。
             */
            if (!containsIgnoreCase(
                course.getCourseName(),
                request.getCourseName())) {

                continue;
            }

            /*
             * 开课院系。
             */
            if (!containsIgnoreCase(
                record.departmentName(),
                request.getDepartmentName())) {

                continue;
            }

            /*
             * 一门课程可能有多个教学班，
             * 每个教学班分别判断教师和余量。
             */
            for (OfferingInfo offering
                : course.getOfferings()) {

                if (!teacherMatches(
                    offering,
                    request.getTeacherName())) {

                    continue;
                }

                if (!availabilityMatches(
                    offering,
                    request.getAvailability())) {

                    continue;
                }

                filtered.add(
                    toSearchItem(
                        course,
                        record.departmentName(),
                        offering));
            }
        }

        /*
         * =========================
         * 2. 分页
         * =========================
         */
        int totalCount =
            filtered.size();

        int totalPages =
            totalCount == 0
                ? 0
                : (totalCount
                + PAGE_SIZE
                - 1)
                / PAGE_SIZE;

        int page =
            request.getPage();

        int fromIndex =
            (page - 1)
                * PAGE_SIZE;

        /*
         * 如果请求页已经超过最后一页，
         * 返回空列表，但保留 totalPages。
         */
        List<CourseSearchItem> pageItems;

        if (fromIndex >= totalCount) {

            pageItems =
                List.of();

        } else {

            int toIndex =
                Math.min(
                    fromIndex + PAGE_SIZE,
                    totalCount);

            pageItems =
                new ArrayList<>(
                    filtered.subList(
                        fromIndex,
                        toIndex));
        }

        return new CourseSearchResult(
            pageItems,
            page,
            PAGE_SIZE,
            totalCount,
            totalPages);
    }

    /**
     * 普通文本模糊搜索。
     *
     * keyword == null 表示不过滤。
     */
    private boolean containsIgnoreCase(
        String value,
        String keyword) {

        if (keyword == null) {

            return true;
        }

        if (value == null) {

            return false;
        }

        return value
            .toLowerCase()
            .contains(
                keyword
                    .toLowerCase());
    }

    /**
     * 教师模糊搜索。
     */
    private boolean teacherMatches(
        OfferingInfo offering,
        String teacherName) {

        if (teacherName == null) {

            return true;
        }

        for (String teacher
            : offering.getTeacherNames()) {

            if (containsIgnoreCase(
                teacher,
                teacherName)) {

                return true;
            }
        }

        return false;
    }

    /**
     * 余量筛选。
     *
     * ALL：
     * 全部。
     *
     * AVAILABLE：
     * 仍有余量。
     *
     * FULL：
     * 已满。
     */
    private boolean availabilityMatches(
        OfferingInfo offering,
        String availability) {

        if (availability == null
            || "ALL".equalsIgnoreCase(
            availability)) {

            return true;
        }

        if ("AVAILABLE".equalsIgnoreCase(
            availability)) {

            return offering
                .getRemainingCount() > 0;
        }

        if ("FULL".equalsIgnoreCase(
            availability)) {

            return offering
                .getRemainingCount() <= 0;
        }

        /*
         * 非法值不返回任何记录。
         */
        return false;
    }

    /**
     * 转成客户端需要的查询记录。
     */
    private CourseSearchItem toSearchItem(
        CourseInfo course,
        String departmentName,
        OfferingInfo offering) {

        return new CourseSearchItem(
            course.getCourseId(),
            course.getCourseCode(),
            course.getCourseName(),
            course.getCredits(),
            course.getCourseType(),
            departmentName,
            offering.getOfferingId(),
            offering.getClassNo(),
            offering.getTeacherNames(),
            offering.getSchedules(),
            offering.getLocationName(),
            offering.getCampusName(),
            offering.getTeachingLanguage(),
            offering.getSelectedCount(),
            offering.getCapacity(),
            offering.getRemainingCount());
    }
}
