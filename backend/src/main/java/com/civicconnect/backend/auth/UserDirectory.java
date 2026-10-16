package com.civicconnect.backend.auth;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserDirectory {

    Optional<AuthenticatedUser> findById(UUID id);

    Optional<UserAccount> findAccountById(UUID id);

    Optional<UserAccount> findAccountByEmail(String email);

    UserAccount createRequester(String name, String email, String passwordHash);

    Map<UUID, String> namesOf(Collection<UUID> ids);
}
