package com.civicconnect.backend.assignment;

import org.springframework.stereotype.Service;

import com.civicconnect.backend.auth.AccessControlService;
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.auth.Role;
import com.civicconnect.backend.auth.UserDirectory;
import com.civicconnect.backend.exception.ConflictException;
import com.civicconnect.backend.exception.ForbiddenException;
import com.civicconnect.backend.exception.NotFoundException;
import com.civicconnect.backend.request.RequestService;
import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;
import com.civicconnect.backend.status.StatusTransitionService;
import com.civicconnect.backend.status.TransitionCommand;

/**
 * FR-009. Decides who may assign, then triggers Submitted to Assigned through the status module.
 * Manual assignment only: automated routing is out of scope.
 * Staff accept a request for themselves; managers assign it to any staff member in scope.
 */
@Service
public class AssignmentService {

    private final RequestService requests;
    private final StatusTransitionService transitions;
    private final AccessControlService access;
    private final UserDirectory users;

    public AssignmentService(RequestService requests, StatusTransitionService transitions,
                             AccessControlService access, UserDirectory users) {
        this.requests = requests;
        this.transitions = transitions;
        this.access = access;
        this.users = users;
    }

    /** @param assigneeId null means "the caller accepts it themselves" */
    public ServiceRequest assign(AuthenticatedUser actor, Long requestId, Long assigneeId, long expectedVersion) {
        access.requireStaffOrManager(actor);
        Long targetId = assigneeId == null ? actor.id() : assigneeId;

        if (actor.isStaff() && !targetId.equals(actor.id())) {
            throw new ForbiddenException("Staff can only accept requests for themselves");
        }

        AuthenticatedUser assignee = targetId.equals(actor.id())
                ? actor
                : users.findById(targetId).orElseThrow(() -> new NotFoundException("Assignee not found"));
        if (assignee.role() != Role.STAFF && assignee.role() != Role.MANAGER) {
            throw new ForbiddenException("Requests can only be assigned to staff");
        }

        ServiceRequest request = requests.requireById(requestId);
        access.requireCategoryAccess(actor, request.getCategoryId());
        if (!access.canAccessCategory(assignee, request.getCategoryId())) {
            throw new ForbiddenException("The assignee is not authorised for this category");
        }
        if (request.getAssignedStaffId() != null || request.getStatus() != RequestStatus.SUBMITTED) {
            throw new ConflictException("This request is already assigned");
        }

        return transitions.transition(actor, requestId,
                new TransitionCommand(RequestStatus.ASSIGNED, null, targetId, expectedVersion));
    }
}
