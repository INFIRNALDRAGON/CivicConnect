package com.civicconnect.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.civicconnect.backend.assignment.AssignmentService;
import com.civicconnect.backend.audit.AuditLogService;
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.dto.AssignRequestDto;
import com.civicconnect.backend.dto.CommentDto;
import com.civicconnect.backend.dto.CreateRequestDto;
import com.civicconnect.backend.dto.RequestDetailDto;
import com.civicconnect.backend.dto.ServiceRequestDto;
import com.civicconnect.backend.dto.StatusChangeDto;
import com.civicconnect.backend.exception.ValidationException;
import com.civicconnect.backend.request.RequestFilter;
import com.civicconnect.backend.request.RequestService;
import com.civicconnect.backend.request.RequestSort;
import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;
import com.civicconnect.backend.status.StatusTransitionService;
import com.civicconnect.backend.status.TransitionCommand;

/**
 * Thin REST layer: parse input, call one service, map to DTOs. No business rules live here.
 * The caller arrives as an {@link AuthenticatedUser} principal set by the security filter.
 */
@RestController
@RequestMapping("/api/requests")
public class ServiceRequestController {

    private final RequestService requests;
    private final AssignmentService assignments;
    private final StatusTransitionService transitions;
    private final AuditLogService audit;

    public ServiceRequestController(RequestService requests, AssignmentService assignments,
                                    StatusTransitionService transitions, AuditLogService audit) {
        this.requests = requests;
        this.assignments = assignments;
        this.transitions = transitions;
        this.audit = audit;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestDto create(@AuthenticationPrincipal AuthenticatedUser actor, @RequestBody CreateRequestDto body) {
        return toDto(actor, requests.create(actor, body));
    }

    @GetMapping("/mine")
    public List<ServiceRequestDto> mine(@AuthenticationPrincipal AuthenticatedUser actor) {
        return requests.listForRequester(actor).stream().map(r -> toDto(actor, r)).toList();
    }

    @GetMapping
    public List<ServiceRequestDto> queue(@AuthenticationPrincipal AuthenticatedUser actor,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false, defaultValue = "OLDEST_FIRST") String sort) {
        RequestFilter filter = new RequestFilter(categoryId, parseEnum(RequestStatus.class, status, "status"),
                q, parseEnum(RequestSort.class, sort, "sort"));
        return requests.listForStaff(actor, filter).stream().map(r -> toDto(actor, r)).toList();
    }

    @GetMapping("/{id}")
    public RequestDetailDto detail(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        ServiceRequest request = requests.getForViewer(actor, id);   // access check happens here
        boolean staff = actor.isStaffOrManager();

        List<RequestDetailDto.HistoryEntry> history = audit.historyFor(id).stream()
                .map(h -> new RequestDetailDto.HistoryEntry(h.previousStatus(), h.newStatus(), h.changedAt(),
                        staff ? h.comment() : null))
                .toList();
        List<RequestDetailDto.CommentEntry> comments = staff
                ? requests.commentsFor(actor, id).stream()
                        .map(c -> new RequestDetailDto.CommentEntry(c.authorId(), c.text(), c.createdAt())).toList()
                : List.of();
        return new RequestDetailDto(toDto(actor, request), history, comments);
    }

    @PostMapping("/{id}/assign")
    public ServiceRequestDto assign(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                    @RequestBody AssignRequestDto body) {
        return toDto(actor, assignments.assign(actor, id, body.assigneeId(), requireVersion(body.version())));
    }

    @PostMapping("/{id}/status")
    public ServiceRequestDto changeStatus(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                          @RequestBody StatusChangeDto body) {
        if (body.target() == null) {
            throw new ValidationException("A target status is required");
        }
        TransitionCommand cmd = new TransitionCommand(body.target(), body.comment(), null,
                requireVersion(body.version()));
        return toDto(actor, transitions.transition(actor, id, cmd));
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDetailDto.CommentEntry addComment(@AuthenticationPrincipal AuthenticatedUser actor,
                                                    @PathVariable Long id, @RequestBody CommentDto body) {
        var c = requests.addComment(actor, id, body.text());
        return new RequestDetailDto.CommentEntry(c.authorId(), c.text(), c.createdAt());
    }

    private ServiceRequestDto toDto(AuthenticatedUser actor, ServiceRequest r) {
        return ServiceRequestDto.from(r, requests.categoryName(r.getCategoryId()), actor.isStaffOrManager());
    }

    private static long requireVersion(Long version) {
        if (version == null) {
            throw new ValidationException("version is required (the value you last saw)");
        }
        return version;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown value for " + field + ": " + value);
        }
    }
}
