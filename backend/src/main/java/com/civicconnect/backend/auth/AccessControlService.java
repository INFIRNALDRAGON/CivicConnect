package com.civicconnect.backend.auth;

import java.util.Arrays;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.civicconnect.backend.exception.ForbiddenException;

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

    public boolean canAccessCategory(AuthenticatedUser actor, UUID categoryId) {
        if (actor == null || actor.role() == null) {
            return false;
        }
        return switch (actor.role()) {
            case MANAGER -> true;
            case STAFF -> actor.categoryIds().contains(categoryId);
            case REQUESTER -> false;
        };
    }

    public void requireCategoryAccess(AuthenticatedUser actor, UUID categoryId) {
        requireAuthenticated(actor);
        if (!canAccessCategory(actor, categoryId)) {
            throw new ForbiddenException("This request is outside your authorised categories");
        }
    }

    public boolean canViewRequest(AuthenticatedUser actor, UUID requesterId, UUID categoryId) {
        if (actor == null || actor.role() == null) {
            return false;
        }
        if (actor.isRequester()) {
            return actor.id().equals(requesterId);
        }
        return canAccessCategory(actor, categoryId);
    }

    public void requireCanViewRequest(AuthenticatedUser actor, UUID requesterId, UUID categoryId) {
        requireAuthenticated(actor);
        if (!canViewRequest(actor, requesterId, categoryId)) {
            throw new ForbiddenException("You are not allowed to view this request");
        }
    }
}
