package com.civicconnect.backend.auth;

import java.util.Optional;

/**
 * Lookup of users by id. The real implementation reads the users / user_category
 * tables (database work, not included here).
 */
public interface UserDirectory {
    Optional<AuthenticatedUser> findById(Long id);
}
