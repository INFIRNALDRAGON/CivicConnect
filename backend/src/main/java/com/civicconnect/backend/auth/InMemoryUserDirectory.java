package com.civicconnect.backend.auth;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/** Stand-in until the user repository exists. Replace with a database-backed UserDirectory. */
@Component
public class InMemoryUserDirectory implements UserDirectory {

    private final Map<Long, AuthenticatedUser> users = new ConcurrentHashMap<>();

    public void register(AuthenticatedUser user) {
        users.put(user.id(), user);
    }

    @Override
    public Optional<AuthenticatedUser> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }
}
