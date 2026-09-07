package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.LoginRequest;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAuthenticationServiceTest {

    @Test
    void idleSessionExpiresButActiveSessionExtendsItsIdleDeadline() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-06T00:00:00Z"));
        InMemoryAuthenticationService authentication = new InMemoryAuthenticationService(
                DemoUserAccounts.createRepository(), clock,
                Duration.ofMinutes(30), Duration.ofHours(8));
        SessionInfo session = login(authentication);

        clock.advance(Duration.ofMinutes(29));
        assertTrue(authentication.findSession(session.getToken()).isPresent());
        clock.advance(Duration.ofMinutes(29));
        assertTrue(authentication.findSession(session.getToken()).isPresent());
        clock.advance(Duration.ofMinutes(30));
        assertFalse(authentication.findSession(session.getToken()).isPresent());
    }

    @Test
    void sessionExpiresAtAbsoluteDeadlineDespiteContinuedActivity() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-06T00:00:00Z"));
        InMemoryAuthenticationService authentication = new InMemoryAuthenticationService(
                DemoUserAccounts.createRepository(), clock,
                Duration.ofMinutes(30), Duration.ofHours(8));
        SessionInfo session = login(authentication);

        for (int index = 0; index < 23; index++) {
            clock.advance(Duration.ofMinutes(20));
            assertTrue(authentication.findSession(session.getToken()).isPresent());
        }
        clock.advance(Duration.ofMinutes(20));
        assertFalse(authentication.findSession(session.getToken()).isPresent());
    }

    private static SessionInfo login(InMemoryAuthenticationService authentication) {
        char[] password = "123456".toCharArray();
        try {
            return authentication.login(new LoginRequest(
                    "20260001", PasswordProof.create("20260001", password))).orElseThrow();
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
