package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.CampusCardNumber;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.server.demo.FinalDemoRoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Creates the public development accounts used by local development and tests. */
final class DemoUserAccounts {

    private DemoUserAccounts() {
    }

    static InMemoryUserRepository createRepository() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        seedMissingAccounts(repository);
        return repository;
    }

    /**
     * Adds demo identities that are missing by stable user id without overwriting
     * persisted accounts. A locally occupied documented card number is preserved;
     * only the missing demo identity receives the next free number.
     */
    static void seedMissingAccounts(UserRepository repository) {
        List<UserAccount> existing = repository.findAll();
        Map<String, UserAccount> byId = new HashMap<>();
        Set<String> usedNumbers = new HashSet<>();
        for (UserAccount account : existing) {
            byId.put(account.userId(), account);
            usedNumbers.add(account.username());
        }

        List<FinalDemoRoster.AccountSeed> missing = FinalDemoRoster.accounts().stream()
                .filter(seed -> !byId.containsKey(seed.userId()))
                .toList();
        if (missing.isEmpty()) {
            return;
        }

        Set<String> reservedNumbers = missing.stream()
                .map(FinalDemoRoster.AccountSeed::campusCardNumber)
                .filter(number -> !usedNumbers.contains(number))
                .collect(java.util.stream.Collectors.toSet());
        Map<Integer, Integer> nextSequences = new HashMap<>();
        java.util.stream.Stream.concat(
                        usedNumbers.stream(),
                        missing.stream().map(FinalDemoRoster.AccountSeed::campusCardNumber))
                .filter(CampusCardNumber::isValid)
                .forEach(number -> nextSequences.merge(
                        Integer.parseInt(number.substring(0, 4)),
                        CampusCardNumber.sequence(number) + 1,
                        Math::max));

        List<UserAccount> additions = new ArrayList<>();
        for (FinalDemoRoster.AccountSeed seed : missing) {
            String username = seed.campusCardNumber();
            if (usedNumbers.contains(username)) {
                int year = Integer.parseInt(username.substring(0, 4));
                int nextSequence = nextSequences.getOrDefault(
                        year, CampusCardNumber.MIN_SEQUENCE);
                do {
                    username = CampusCardNumber.format(year, nextSequence++);
                } while (usedNumbers.contains(username) || reservedNumbers.contains(username));
                nextSequences.put(year, nextSequence);
            } else {
                reservedNumbers.remove(username);
            }
            additions.add(account(
                    seed.userId(), username, seed.displayName(), seed.role(),
                    seed.adminScopes()));
            usedNumbers.add(username);
        }
        repository.saveAll(additions);
    }

    private static UserAccount account(
            String userId,
            String username,
            String displayName,
            Role role,
            Set<AdminScope> scopes) {
        char[] password = FinalDemoRoster.INITIAL_PASSWORD.toCharArray();
        try {
            return new UserAccount(
                    userId, username, displayName, role, scopes,
                    PasswordProof.create(username, password), true);
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
