package edu.seu.vcampus.server.module.user;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Limits repeated failed login attempts for one campus-card number. */
final class LoginAttemptLimiter {

    private final int maximumFailures;
    private final Duration failureWindow;
    private final Duration lockDuration;
    private final ConcurrentMap<String, FailureState> failures = new ConcurrentHashMap<>();

    LoginAttemptLimiter(
            int maximumFailures,
            Duration failureWindow,
            Duration lockDuration) {
        if (maximumFailures < 1) {
            throw new IllegalArgumentException("maximumFailures must be positive");
        }
        this.maximumFailures = maximumFailures;
        this.failureWindow = requirePositive(failureWindow, "failureWindow");
        this.lockDuration = requirePositive(lockDuration, "lockDuration");
    }

    boolean isBlocked(String username, Instant now) {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(now, "now must not be null");
        FailureState state = failures.get(username);
        if (state == null) {
            return false;
        }
        if (state.blockedUntil() != null && now.isBefore(state.blockedUntil())) {
            return true;
        }
        if (state.blockedUntil() != null
                || !now.isBefore(state.firstFailureAt().plus(failureWindow))) {
            failures.remove(username, state);
        }
        return false;
    }

    void recordFailure(String username, Instant now) {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(now, "now must not be null");
        failures.compute(username, (ignored, current) -> {
            if (current == null
                    || current.blockedUntil() != null
                    || !now.isBefore(current.firstFailureAt().plus(failureWindow))) {
                return new FailureState(1, now, null);
            }
            int count = current.count() + 1;
            Instant blockedUntil = count >= maximumFailures
                    ? now.plus(lockDuration)
                    : null;
            return new FailureState(count, current.firstFailureAt(), blockedUntil);
        });
    }

    void recordSuccess(String username) {
        if (username != null) {
            failures.remove(username);
        }
    }

    private static Duration requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private record FailureState(
            int count,
            Instant firstFailureAt,
            Instant blockedUntil) {
    }
}
