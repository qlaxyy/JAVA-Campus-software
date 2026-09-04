package edu.seu.vcampus.server.module.course;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 教学班管理设置 Repository。
 *
 * 保存教务修改后的容量和开放状态。
 */
interface CourseOfferingSettingsRepository {

    /**
     * 查询教学班设置。
     */
    Optional<CourseOfferingSettings> find(
        long batchId,
        long offeringId);

    /**
     * 保存或覆盖教学班设置。
     */
    void save(
        CourseOfferingSettings settings);
}

/**
 * 教学班管理设置。
 */
record CourseOfferingSettings(
    long batchId,
    long offeringId,
    int capacity,
    boolean open) {

    CourseOfferingSettings {

        if (batchId <= 0) {

            throw new IllegalArgumentException(
                "batchId must be positive");
        }

        if (offeringId <= 0) {

            throw new IllegalArgumentException(
                "offeringId must be positive");
        }

        if (capacity < 0) {

            throw new IllegalArgumentException(
                "capacity must not be negative");
        }
    }
}

/**
 * 开发阶段使用的内存教学班设置仓库。
 */
final class InMemoryCourseOfferingSettingsRepository
    implements CourseOfferingSettingsRepository {

    private final ConcurrentMap<
        OfferingSettingsKey,
        CourseOfferingSettings> settings =
        new ConcurrentHashMap<>();

    @Override
    public Optional<CourseOfferingSettings> find(
        long batchId,
        long offeringId) {

        return Optional.ofNullable(
            settings.get(
                new OfferingSettingsKey(
                    batchId,
                    offeringId)));
    }

    @Override
    public void save(
        CourseOfferingSettings value) {

        if (value == null) {

            throw new IllegalArgumentException(
                "settings must not be null");
        }

        settings.put(
            new OfferingSettingsKey(
                value.batchId(),
                value.offeringId()),
            value);
    }

    /**
     * 批次和教学班共同确定一条设置。
     */
    private record OfferingSettingsKey(
        long batchId,
        long offeringId) {
    }
}
