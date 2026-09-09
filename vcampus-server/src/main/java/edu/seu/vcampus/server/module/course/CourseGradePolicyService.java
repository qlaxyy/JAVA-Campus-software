package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseGradePolicyInfo;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 教学班成绩比例业务服务。
 */
final class CourseGradePolicyService {

    /**
     * 未单独设置时使用：
     * 平时成绩 40%，期末成绩 60%。
     */
    private static final int
        DEFAULT_USUAL_WEIGHT_PERCENT =
        40;

    private static final int
        DEFAULT_FINAL_EXAM_WEIGHT_PERCENT =
        60;

    private final CourseGradePolicyRepository
        repository;

    private final Clock clock;

    CourseGradePolicyService(
        CourseGradePolicyRepository repository,
        Clock clock) {

        this.repository =
            Objects.requireNonNull(
                repository);

        this.clock =
            Objects.requireNonNull(
                clock);
    }

    /**
     * 查询教学班成绩比例。
     *
     * 没有保存过设置时返回默认 40% + 60%。
     */
    CourseGradePolicyInfo getPolicy(
        long offeringId) {

        CourseGradePolicyRecord record =
            repository
                .findByOfferingId(
                    offeringId)
                .orElse(null);

        if (record == null) {

            return new CourseGradePolicyInfo(
                offeringId,
                DEFAULT_USUAL_WEIGHT_PERCENT,
                DEFAULT_FINAL_EXAM_WEIGHT_PERCENT);
        }

        return new CourseGradePolicyInfo(
            record.offeringId(),
            record.usualWeightPercent(),
            record.finalExamWeightPercent());
    }

    /**
     * 修改教学班成绩比例。
     */
    synchronized CourseGradePolicyUpdateResult
    updatePolicy(
        long offeringId,
        int usualWeightPercent,
        int finalExamWeightPercent) {

        if (offeringId <= 0) {

            return CourseGradePolicyUpdateResult
                .failure(
                    "教学班 ID 不正确。");
        }

        if (usualWeightPercent < 0
            || usualWeightPercent > 100) {

            return CourseGradePolicyUpdateResult
                .failure(
                    "平时成绩比例必须在 0 到 100 之间。");
        }

        if (finalExamWeightPercent < 0
            || finalExamWeightPercent > 100) {

            return CourseGradePolicyUpdateResult
                .failure(
                    "期末成绩比例必须在 0 到 100 之间。");
        }

        if (usualWeightPercent
            + finalExamWeightPercent
            != 100) {

            return CourseGradePolicyUpdateResult
                .failure(
                    "平时成绩比例与期末成绩比例之和必须为 100。");
        }

        CourseGradePolicyRecord record =
            repository.save(
                offeringId,
                usualWeightPercent,
                finalExamWeightPercent,
                LocalDateTime.now(
                    clock));

        CourseGradePolicyInfo policy =
            new CourseGradePolicyInfo(
                record.offeringId(),
                record.usualWeightPercent(),
                record.finalExamWeightPercent());

        return CourseGradePolicyUpdateResult
            .success(
                "成绩比例修改成功。",
                policy);
    }

    /**
     * 根据教学班比例计算总成绩。
     */
    double calculateTotalScore(
        long offeringId,
        double usualScore,
        double finalExamScore) {

        CourseGradePolicyInfo policy =
            getPolicy(
                offeringId);

        double totalScore =
            usualScore
                * policy.getUsualWeightPercent()
                / 100.0
                + finalExamScore
                * policy.getFinalExamWeightPercent()
                / 100.0;

        /*
         * 保留两位小数，避免显示过长的小数。
         */
        return Math.round(
            totalScore * 100.0)
            / 100.0;
    }
}

/**
 * 成绩比例修改结果。
 */
record CourseGradePolicyUpdateResult(
    boolean success,
    String message,
    CourseGradePolicyInfo policy) {

    static CourseGradePolicyUpdateResult success(
        String message,
        CourseGradePolicyInfo policy) {

        return new CourseGradePolicyUpdateResult(
            true,
            message,
            policy);
    }

    static CourseGradePolicyUpdateResult failure(
        String message) {

        return new CourseGradePolicyUpdateResult(
            false,
            message,
            null);
    }
}
