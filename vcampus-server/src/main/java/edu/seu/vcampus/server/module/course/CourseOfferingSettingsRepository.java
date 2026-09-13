package edu.seu.vcampus.server.module.course;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 教学班管理设置仓库。
 *
 * 教学班容量和开放状态按照 offeringId
 * 全局保存，与选课批次无关。
 */
interface CourseOfferingSettingsRepository {

    /**
     * 查询教学班设置。
     */
    Optional<CourseOfferingSettings> find(
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
    long offeringId,
    int capacity,
    boolean open) {

    CourseOfferingSettings {

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
        Long,
        CourseOfferingSettings> settings =
        new ConcurrentHashMap<>();

    @Override
    public Optional<CourseOfferingSettings> find(
        long offeringId) {

        return Optional.ofNullable(
            settings.get(
                offeringId));
    }

    @Override
    public void save(
        CourseOfferingSettings value) {

        if (value == null) {

            throw new IllegalArgumentException(
                "settings must not be null");
        }

        settings.put(
            value.offeringId(),
            value);
    }
}
