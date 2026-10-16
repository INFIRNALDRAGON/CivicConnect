package com.civicconnect.backend.auth;

<<<<<<< Updated upstream
import java.util.Optional;

/**
 * Lookup of users by id. The real implementation reads the users / user_category
 * tables (database work, not included here).
 */
public interface UserDirectory {
    Optional<AuthenticatedUser> findById(Long id);
=======
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
>>>>>>> Stashed changes
}
