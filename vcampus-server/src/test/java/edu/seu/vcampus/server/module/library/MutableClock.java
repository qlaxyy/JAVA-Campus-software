package edu.seu.vcampus.server.module.library;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * 可在测试中手动推进的 {@link Clock}。
 *
 * <p>借期 30 天、预约保留 24 小时与爽约冷却 7 天这类规则无法靠真实时间验证，
 * 业务代码统一通过注入的 {@code Clock} 取当前时间，因此测试可以精确推进到规则边界上，
 * 例如"到期时刻"或"截止时刻"本身。
 */
final class MutableClock extends Clock {

    private volatile Instant current;
    private final ZoneId zone;

    MutableClock(Instant current, ZoneId zone) {
        this.current = current;
        this.zone = zone;
    }

    void advance(Duration duration) {
        current = current.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new MutableClock(current, newZone);
    }

    @Override
    public Instant instant() {
        return current;
    }
}
