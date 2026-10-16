package com.civicconnect.backend.auth;

import java.util.Arrays;
<<<<<<< Updated upstream
=======
import java.util.UUID;
>>>>>>> Stashed changes

import org.springframework.stereotype.Service;

import com.civicconnect.backend.exception.ForbiddenException;

<<<<<<< Updated upstream
/**
 * Role and category-scope checks (FR-006, NFR-003, ASR-02).
 * Services call this themselves instead of trusting the controller layer alone,
 * so a missing controller rule cannot expose data. Every check is default-deny.
 */
=======
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
    /** Managers see every category. Staff see only the categories they are scoped to. Requesters see none. 
     @param actor the staff member or manager making the call
     @param categoryId the category the request falls in to
     @return returns a boolean value
     */
    public boolean canAccessCategory(AuthenticatedUser actor, Long categoryId) {
=======
    public boolean canAccessCategory(AuthenticatedUser actor, UUID categoryId) {
>>>>>>> Stashed changes
        if (actor == null || actor.role() == null) {
            return false;
        }
        return switch (actor.role()) {
            case MANAGER -> true;
            case STAFF -> actor.categoryIds().contains(categoryId);
            case REQUESTER -> false;
        };
    }

<<<<<<< Updated upstream
    public void requireCategoryAccess(AuthenticatedUser actor, Long categoryId) {
=======
    public void requireCategoryAccess(AuthenticatedUser actor, UUID categoryId) {
>>>>>>> Stashed changes
        requireAuthenticated(actor);
        if (!canAccessCategory(actor, categoryId)) {
            throw new ForbiddenException("This request is outside your authorised categories");
        }
    }

<<<<<<< Updated upstream
    /** A requester sees only their own requests; staff and managers use category scope. 
     *@param actor the staff member or manager making the call
     *@param requesterId the id of the requester
     *@param categoryId the category the request falls in to
     *@return returns a boolean value
     */
    public boolean canViewRequest(AuthenticatedUser actor, Long requesterId, Long categoryId) {
=======
    public boolean canViewRequest(AuthenticatedUser actor, UUID requesterId, UUID categoryId) {
>>>>>>> Stashed changes
        if (actor == null || actor.role() == null) {
            return false;
        }
        if (actor.isRequester()) {
            return actor.id().equals(requesterId);
        }
        return canAccessCategory(actor, categoryId);
    }

<<<<<<< Updated upstream
    public void requireCanViewRequest(AuthenticatedUser actor, Long requesterId, Long categoryId) {
=======
    public void requireCanViewRequest(AuthenticatedUser actor, UUID requesterId, UUID categoryId) {
>>>>>>> Stashed changes
        requireAuthenticated(actor);
        if (!canViewRequest(actor, requesterId, categoryId)) {
            throw new ForbiddenException("You are not allowed to view this request");
        }
    }
}
