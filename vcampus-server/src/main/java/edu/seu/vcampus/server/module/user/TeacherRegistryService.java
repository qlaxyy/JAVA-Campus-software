package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.user.SaveTeacherProfileRequest;
import edu.seu.vcampus.common.user.BatchSaveTeacherProfilesRequest;
import edu.seu.vcampus.common.user.TeacherProfileListResponse;
import edu.seu.vcampus.common.user.TeacherProfileView;
import edu.seu.vcampus.server.security.TeacherDirectory;
import edu.seu.vcampus.server.security.TeacherIdentity;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Owns teacher qualifications and exposes only read-only identities to other modules. */
final class TeacherRegistryService implements TeacherDirectory {

    private static final String DEMO_TEACHER_USER_ID = "U-COURSE-TEACHER-001";
    private static final String SYSTEM_ADMIN_USER_ID = "U-ADMIN-001";

    private final UserRepository users;
    private final TeacherRepository teachers;
    private final Clock clock;

    TeacherRegistryService(UserRepository users, TeacherRepository teachers) {
        this(users, teachers, Clock.systemUTC());
    }

    TeacherRegistryService(UserRepository users, TeacherRepository teachers, Clock clock) {
        this.users = Objects.requireNonNull(users, "users must not be null");
        this.teachers = Objects.requireNonNull(teachers, "teachers must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        seedDemoTeacherIfPossible();
    }

    @Override
    public synchronized Optional<TeacherIdentity> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        TeacherProfile profile = teachers.findByUserId(userId.trim()).orElse(null);
        if (profile == null || !profile.active()) {
            return Optional.empty();
        }
        UserAccount account = users.findById(profile.userId()).orElse(null);
        return account == null || !account.enabled()
                ? Optional.empty()
                : Optional.of(toIdentity(profile, account));
    }

    @Override
    public synchronized List<TeacherIdentity> findActiveTeachers() {
        return teachers.findAll().stream()
                .filter(TeacherProfile::active)
                .map(profile -> users.findById(profile.userId())
                        .filter(UserAccount::enabled)
                        .map(account -> toIdentity(profile, account))
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TeacherIdentity::campusCardNumber))
                .toList();
    }

    synchronized TeacherProfileListResponse listProfiles() {
        List<TeacherProfileView> views = teachers.findAll().stream()
                .map(profile -> users.findById(profile.userId())
                        .map(account -> toView(profile, account))
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TeacherProfileView::getCampusCardNumber))
                .toList();
        return new TeacherProfileListResponse(views);
    }

    synchronized TeacherProfileView saveProfile(
            SaveTeacherProfileRequest request,
            String actorUserId) {
        UserAccount account = users.findById(request.getUserId())
                .orElseThrow(() -> new UserAdministrationException(
                        ErrorCodes.USER_ACCOUNT_NOT_FOUND, "没有找到要设置为教师的账号。"));
        Instant now = clock.instant();
        TeacherProfile old = teachers.findByUserId(account.userId()).orElse(null);
        TeacherProfile profile = new TeacherProfile(
                account.userId(),
                request.getDepartment(),
                request.getTitle(),
                request.isActive(),
                old == null ? actorUserId : old.createdByUserId(),
                old == null ? now : old.createdAt(),
                now);
        teachers.save(profile);
        return toView(profile, account);
    }

    synchronized TeacherProfileListResponse saveProfiles(
            BatchSaveTeacherProfilesRequest request,
            String actorUserId) {
        Instant now = clock.instant();
        List<ProfileWithAccount> validated = request.getTeachers().stream()
                .map(item -> {
                    UserAccount account = users.findById(item.getUserId())
                            .orElseThrow(() -> new UserAdministrationException(
                                    ErrorCodes.USER_ACCOUNT_NOT_FOUND,
                                    "批量导入包含不存在的账号。"));
                    TeacherProfile old = teachers.findByUserId(account.userId()).orElse(null);
                    TeacherProfile profile = new TeacherProfile(
                            account.userId(),
                            item.getDepartment(),
                            item.getTitle(),
                            item.isActive(),
                            old == null ? actorUserId : old.createdByUserId(),
                            old == null ? now : old.createdAt(),
                            now);
                    return new ProfileWithAccount(profile, account);
                })
                .toList();
        teachers.saveAll(validated.stream().map(ProfileWithAccount::profile).toList());
        return new TeacherProfileListResponse(validated.stream()
                .map(item -> toView(item.profile(), item.account()))
                .sorted(Comparator.comparing(TeacherProfileView::getCampusCardNumber))
                .toList());
    }

    synchronized Optional<TeacherProfileView> profileView(String userId) {
        return teachers.findByUserId(userId)
                .flatMap(profile -> users.findById(userId)
                        .map(account -> toView(profile, account)));
    }

    private void seedDemoTeacherIfPossible() {
        if (teachers.findByUserId(DEMO_TEACHER_USER_ID).isPresent()
                || users.findById(DEMO_TEACHER_USER_ID).isEmpty()) {
            return;
        }
        Instant now = clock.instant();
        teachers.save(new TeacherProfile(
                DEMO_TEACHER_USER_ID,
                "计算机科学与工程学院",
                "讲师",
                true,
                SYSTEM_ADMIN_USER_ID,
                now,
                now));
    }

    private static TeacherIdentity toIdentity(TeacherProfile profile, UserAccount account) {
        return new TeacherIdentity(
                account.userId(), account.username(), account.displayName(),
                profile.department(), profile.title());
    }

    private static TeacherProfileView toView(TeacherProfile profile, UserAccount account) {
        return new TeacherProfileView(
                account.userId(), account.username(), account.displayName(),
                profile.department(), profile.title(), profile.active());
    }

    private record ProfileWithAccount(TeacherProfile profile, UserAccount account) {
    }
}
