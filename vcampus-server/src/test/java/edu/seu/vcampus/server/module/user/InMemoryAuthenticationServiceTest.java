package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.LoginRequest;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.security.UserIdentity;
import edu.seu.vcampus.common.user.SaveTeacherProfileRequest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    @Test
    void successfulLoginUpgradesLegacyPasswordProof() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        String proof = PasswordProof.create("20260123", "123456".toCharArray());
        repository.save(UserAccount.fromPersistence(
                "U-LEGACY-LOGIN", "20260123", "旧账号",
                edu.seu.vcampus.common.user.Role.USER, java.util.Set.of(),
                proof, null, null, 0, true));
        InMemoryAuthenticationService authentication =
                new InMemoryAuthenticationService(repository);

        assertTrue(authentication.login(new LoginRequest("20260123", proof)).isPresent());

        UserAccount upgraded = repository.findById("U-LEGACY-LOGIN").orElseThrow();
        assertFalse(upgraded.passwordNeedsUpgrade());
        assertEquals("0".repeat(64), upgraded.persistedLegacyPasswordProof());
        assertTrue(upgraded.passwordHash() != null && !upgraded.passwordHash().isBlank());
        assertTrue(upgraded.passwordSalt() != null && !upgraded.passwordSalt().isBlank());
        assertEquals(PasswordCredential.CURRENT_ITERATIONS, upgraded.passwordIterations());
    }

    @Test
    void concurrentAccountProvisioningGeneratesDistinctCampusCardNumbers()
            throws Exception {
        InMemoryAuthenticationService authentication =
                new InMemoryAuthenticationService();
        int contenders = 12;
        CountDownLatch ready = new CountDownLatch(contenders);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(contenders)) {
            java.util.List<Future<String>> futures = new java.util.ArrayList<>();
            for (int index = 0; index < contenders; index++) {
                int accountIndex = index;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    return authentication.createGeneratedRegularAccount(
                            "并发账号" + accountIndex).username();
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            Set<String> generated = new HashSet<>();
            for (Future<String> future : futures) {
                generated.add(future.get(20, TimeUnit.SECONDS));
            }
            assertEquals(contenders, generated.size());
        }
    }

    @Test
    void superAdministratorMaintainsTeacherQualificationWithoutChangingRole() {
        InMemoryAuthenticationService authentication = new InMemoryAuthenticationService();

        assertTrue(authentication.teacherDirectory()
                .findByUserId("U-TEACHER-001").isPresent());
        assertEquals(8, authentication.teacherDirectory().findActiveTeachers().size());

        authentication.teachers().saveProfile(
                new SaveTeacherProfileRequest(
                        "U-STUDENT-001", "电子科学与工程学院", "实验师", true),
                "U-ADMIN-001");

        assertEquals("实验师", authentication.teacherDirectory()
                .findByUserId("U-STUDENT-001").orElseThrow().title());
        assertEquals(edu.seu.vcampus.common.user.Role.USER,
                authentication.users().findById("U-STUDENT-001").orElseThrow().role());

        authentication.teachers().saveProfile(
                new SaveTeacherProfileRequest(
                        "U-STUDENT-001", "电子科学与工程学院", "实验师", false),
                "U-ADMIN-001");
        assertTrue(authentication.teacherDirectory()
                .findByUserId("U-STUDENT-001").isEmpty());
        assertFalse(authentication.teachers().listProfiles().getTeachers().stream()
                .filter(profile -> profile.getUserId().equals("U-STUDENT-001"))
                .findFirst().orElseThrow().isActive());
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
