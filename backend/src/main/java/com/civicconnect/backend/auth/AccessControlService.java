package com.civicconnect.backend.auth;

import java.util.Arrays;

import org.springframework.stereotype.Service;

import com.civicconnect.backend.exception.ForbiddenException;

/**
 * Role and category-scope checks (FR-006, NFR-003, ASR-02).
 * Services call this themselves instead of trusting the controller layer alone,
 * so a missing controller rule cannot expose data. Every check is default-deny.
 */
@Service
public class AccessControlService {

    public void requireAuthenticated(AuthenticatedUser actor) {
        if (actor == null || actor.id() == null || actor.role() == null) {
            throw new ForbiddenException("Authentication required");
        }
    }

    public void requireRole(AuthenticatedUser actor, Role... allowed) {
        requireAuthenticated(actor);
        if (Arrays.stream(allowed).noneMatch(r -> r == actor.role())) {
            throw new ForbiddenException("Your role is not allowed to do this");
        }
    }

    public void requireStaffOrManager(AuthenticatedUser actor) {
        requireRole(actor, Role.STAFF, Role.MANAGER);
    }

    /** Managers see every category. Staff see only the categories they are scoped to. Requesters see none. */
    public boolean canAccessCategory(AuthenticatedUser actor, Long categoryId) {
        if (actor == null || actor.role() == null) {
            return false;
        }
        return switch (actor.role()) {
            case MANAGER -> true;
            case STAFF -> actor.categoryIds().contains(categoryId);
            case REQUESTER -> false;
        };
    }

    public void requireCategoryAccess(AuthenticatedUser actor, Long categoryId) {
        requireAuthenticated(actor);
        if (!canAccessCategory(actor, categoryId)) {
            throw new ForbiddenException("This request is outside your authorised categories");
        }
    }

    /** A requester sees only their own requests; staff and managers use category scope. */
    public boolean canViewRequest(AuthenticatedUser actor, Long requesterId, Long categoryId) {
        if (actor == null || actor.role() == null) {
            return false;
        }
        if (actor.isRequester()) {
            return actor.id().equals(requesterId);
        }
        return canAccessCategory(actor, categoryId);
    }

    public void requireCanViewRequest(AuthenticatedUser actor, Long requesterId, Long categoryId) {
        requireAuthenticated(actor);
        if (!canViewRequest(actor, requesterId, categoryId)) {
            throw new ForbiddenException("You are not allowed to view this request");
        }
    }
}
