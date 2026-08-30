package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.user.CreateUserAccountRequest;
import edu.seu.vcampus.common.user.ResetUserPasswordRequest;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.UpdateUserAccountRequest;
import edu.seu.vcampus.common.user.UpdateUserStatusRequest;
import edu.seu.vcampus.common.user.UserAccountListResponse;
import edu.seu.vcampus.common.user.UserAccountView;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

/** Server-authoritative account administration rules. */
final class UserAdministrationService {

    private final UserRepository repository;
    private final InMemoryAuthenticationService authentication;

    UserAdministrationService(
            UserRepository repository,
            InMemoryAuthenticationService authentication) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.authentication = Objects.requireNonNull(
                authentication, "authentication must not be null");
    }

    UserAccountListResponse listAccounts() {
        return new UserAccountListResponse(repository.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::username))
                .map(UserAccount::toView)
                .toList());
    }

    synchronized UserAccountView createAccount(CreateUserAccountRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (repository.findByUsername(request.getUsername()).isPresent()) {
            throw failure(ErrorCodes.USER_USERNAME_EXISTS, "该账户名已经存在。");
        }
        UserAccount account = new UserAccount(
                "U-" + UUID.randomUUID(),
                request.getUsername(), request.getDisplayName(), Role.USER,
                request.getAdminScopes(), request.getPasswordProof(), true);
        repository.save(account);
        return account.toView();
    }

    synchronized UserAccountView updateAccount(UpdateUserAccountRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        UserAccount current = requiredAccount(request.getUserId());
        UserAccount updated = current.withProfile(
                request.getDisplayName(), request.getAdminScopes());
        repository.save(updated);
        if (!current.adminScopes().equals(updated.adminScopes())) {
            authentication.invalidateUserSessions(updated.userId());
        }
        return updated.toView();
    }

    synchronized UserAccountView updateStatus(
            String actorUserId,
            UpdateUserStatusRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        UserAccount current = requiredAccount(request.getUserId());
        if (!request.isEnabled() && current.userId().equals(actorUserId)) {
            throw failure(ErrorCodes.USER_SELF_DISABLE_FORBIDDEN,
                    "不能禁用当前登录的超级管理员。");
        }
        if (!request.isEnabled()
                && current.role() == Role.SUPER_ADMIN
                && enabledSuperAdministratorCount() <= 1) {
            throw failure(ErrorCodes.USER_SELF_DISABLE_FORBIDDEN,
                    "系统必须保留至少一个启用的超级管理员。");
        }
        UserAccount updated = current.withEnabled(request.isEnabled());
        repository.save(updated);
        authentication.invalidateUserSessions(updated.userId());
        return updated.toView();
    }

    synchronized UserAccountView resetPassword(ResetUserPasswordRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        UserAccount current = requiredAccount(request.getUserId());
        UserAccount updated = current.withPasswordProof(request.getPasswordProof());
        repository.save(updated);
        authentication.invalidateUserSessions(updated.userId());
        return updated.toView();
    }

    private UserAccount requiredAccount(String userId) {
        return repository.findById(userId).orElseThrow(() -> failure(
                ErrorCodes.USER_ACCOUNT_NOT_FOUND, "账号不存在或已经失效。"));
    }

    private long enabledSuperAdministratorCount() {
        return repository.findAll().stream()
                .filter(UserAccount::enabled)
                .filter(account -> account.role() == Role.SUPER_ADMIN)
                .count();
    }

    private static UserAdministrationException failure(String code, String message) {
        return new UserAdministrationException(code, message);
    }
}
