package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.LoginRequest;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.security.UserIdentity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAuthenticationServiceTest {

    @Test
    void looksUpOnlyMinimalIdentityByUserIdOrCampusCardNumber() {
        InMemoryAuthenticationService authentication = new InMemoryAuthenticationService();

        UserIdentity byCard = authentication
                .findByCampusCardNumber("20260001").orElseThrow();
        UserIdentity byId = authentication.findByUserId(byCard.userId()).orElseThrow();

        assertEquals(byCard, byId);
        assertEquals("20260001", byId.campusCardNumber());
        assertFalse(byId.displayName().isBlank());
        assertTrue(byId.enabled());
        assertTrue(authentication.findByUserId("missing-user").isEmpty());
        assertTrue(authentication.findByCampusCardNumber("not-a-card").isEmpty());
    }

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

    @Test
    void fiveFailuresTemporarilyBlockLoginAndSuccessfulLoginClearsFailures() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-07T00:00:00Z"));
        InMemoryAuthenticationService authentication = new InMemoryAuthenticationService(
                DemoUserAccounts.createRepository(), clock,
                Duration.ofMinutes(30), Duration.ofHours(8));

        for (int index = 0; index < 5; index++) {
            assertFalse(loginWithPassword(authentication, "wrong-password").isPresent());
        }
        assertFalse(loginWithPassword(authentication, "123456").isPresent());

        clock.advance(Duration.ofMinutes(5));
        assertTrue(loginWithPassword(authentication, "123456").isPresent());

        assertFalse(loginWithPassword(authentication, "wrong-password").isPresent());
        assertTrue(loginWithPassword(authentication, "123456").isPresent());
        assertTrue(loginWithPassword(authentication, "123456").isPresent());
    }

    @Test
    void failuresOutsideTheWindowDoNotAccumulate() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-07T00:00:00Z"));
        InMemoryAuthenticationService authentication = new InMemoryAuthenticationService(
                DemoUserAccounts.createRepository(), clock,
                Duration.ofMinutes(30), Duration.ofHours(8));

        for (int index = 0; index < 4; index++) {
            assertFalse(loginWithPassword(authentication, "wrong-password").isPresent());
        }
        clock.advance(Duration.ofMinutes(10));
        assertFalse(loginWithPassword(authentication, "wrong-password").isPresent());
        assertTrue(loginWithPassword(authentication, "123456").isPresent());
    }

    private static SessionInfo login(InMemoryAuthenticationService authentication) {
        return loginWithPassword(authentication, "123456").orElseThrow();
    }

    private static java.util.Optional<SessionInfo> loginWithPassword(
            InMemoryAuthenticationService authentication,
            String passwordText) {
        char[] password = passwordText.toCharArray();
        try {
            return authentication.login(new LoginRequest(
                    "20260001", PasswordProof.create("20260001", password)));
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
