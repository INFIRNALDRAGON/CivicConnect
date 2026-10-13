package com.civicconnect.backend.auth;

import java.util.Set;

/**
 * The caller as the rest of the code sees them. Built by the security layer
 * (e.g. from a verified token) and passed into every service call, so services
 * never read a global security context (keeps the app stateless: ASR-05).
 *
 * @param categoryIds categories a STAFF member may see (user_category, CR-06). Ignored for other roles.
 */
public record AuthenticatedUser(Long id, Role role, Set<Long> categoryIds) {

    public AuthenticatedUser {
        categoryIds = categoryIds == null ? Set.of() : Set.copyOf(categoryIds);
    }

    public boolean isRequester() {
        return role == Role.REQUESTER;
    }

    public boolean isStaff() {
        return role == Role.STAFF;
    }

    public boolean isManager() {
        return role == Role.MANAGER;
    }

    public boolean isStaffOrManager() {
        return isStaff() || isManager();
    }
}
