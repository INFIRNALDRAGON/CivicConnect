package com.civicconnect.backend.auth;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(UUID id, Role role, Set<UUID> categoryIds) {

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
