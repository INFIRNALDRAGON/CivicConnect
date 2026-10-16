package com.civicconnect.backend.support;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.auth.Role;
import com.civicconnect.backend.auth.UserAccount;
import com.civicconnect.backend.auth.UserDirectory;
import com.civicconnect.backend.exception.ConflictException;

public class InMemoryUserDirectory implements UserDirectory {

    private final Map<UUID, UserAccount> accounts = new ConcurrentHashMap<>();
    private final Map<UUID, AuthenticatedUser> scopes = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong(9000);

    public void register(AuthenticatedUser user) {
        register(user, "User " + user.id().getLeastSignificantBits());
    }

    public void register(AuthenticatedUser user, String name) {
        scopes.put(user.id(), user);
        accounts.put(user.id(), new UserAccount(user.id(), name,
                "user" + user.id().getLeastSignificantBits() + "@civicconnect.test", "hash", user.role()));
    }

    public void registerAccount(UserAccount account) {
        accounts.put(account.id(), account);
        scopes.put(account.id(), new AuthenticatedUser(account.id(), account.role(), java.util.Set.of()));
    }

    @Override
    public Optional<AuthenticatedUser> findById(UUID id) {
        return Optional.ofNullable(scopes.get(id));
    }

    @Override
    public Optional<UserAccount> findAccountById(UUID id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public Optional<UserAccount> findAccountByEmail(String email) {
        String wanted = email.toLowerCase(Locale.ROOT);
        return accounts.values().stream().filter(a -> a.email().toLowerCase(Locale.ROOT).equals(wanted)).findFirst();
    }

    @Override
    public synchronized UserAccount createRequester(String name, String email, String passwordHash) {
        if (findAccountByEmail(email).isPresent()) {
            throw new ConflictException("An account with this email already exists.");
        }
        UserAccount created = new UserAccount(Ids.of(ids.incrementAndGet()), name, email, passwordHash, Role.REQUESTER);
        registerAccount(created);
        return created;
    }

    @Override
    public Map<UUID, String> namesOf(Collection<UUID> wanted) {
        Map<UUID, String> names = new HashMap<>();
        for (UUID id : wanted) {
            UserAccount a = accounts.get(id);
            if (a != null) {
                names.put(id, a.name());
            }
        }
        return names;
    }
}
