package com.civicconnect.backend.controller;

import java.util.List;
<<<<<<< Updated upstream
=======
import java.util.Map;
import java.util.UUID;
>>>>>>> Stashed changes

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
<<<<<<< Updated upstream
import com.civicconnect.backend.audit.AuditLogService;
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.dto.AssignRequestDto;
import com.civicconnect.backend.dto.CommentDto;
import com.civicconnect.backend.dto.CreateRequestDto;
import com.civicconnect.backend.dto.RequestDetailDto;
import com.civicconnect.backend.dto.ServiceRequestDto;
import com.civicconnect.backend.dto.StatusChangeDto;
=======
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.dto.CreateRequestDto;
>>>>>>> Stashed changes
import com.civicconnect.backend.exception.ValidationException;
import com.civicconnect.backend.request.RequestFilter;
import com.civicconnect.backend.request.RequestService;
import com.civicconnect.backend.request.RequestSort;
import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;
import com.civicconnect.backend.status.StatusTransitionService;
import com.civicconnect.backend.status.TransitionCommand;

<<<<<<< Updated upstream
/**
 * Thin REST layer: parse input, call one service, map to DTOs. No business rules live here.
 * The caller arrives as an {@link AuthenticatedUser} principal set by the security filter.
 */
=======
>>>>>>> Stashed changes
@RestController
@RequestMapping("/api/requests")
public class ServiceRequestController {

<<<<<<< Updated upstream
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
=======
    public record VersionBody(Long version) {
    }

    public record StatusBody(RequestStatus targetStatus, String comment, Long version) {
    }

    public record CommentBody(String comment, Long version) {
    }

    private final RequestService requests;
    private final AssignmentService assignments;
    private final StatusTransitionService transitions;
    private final RequestAssembler assembler;

    public ServiceRequestController(RequestService requests, AssignmentService assignments,
                                    StatusTransitionService transitions, RequestAssembler assembler) {
        this.requests = requests;
        this.assignments = assignments;
        this.transitions = transitions;
        this.assembler = assembler;
>>>>>>> Stashed changes
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
<<<<<<< Updated upstream
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
=======
    public Map<String, Object> create(@AuthenticationPrincipal AuthenticatedUser actor,
                                      @RequestBody CreateRequestDto body) {
        return assembler.detail(actor, requests.create(actor, body));
    }

    @GetMapping("/mine")
    public List<Map<String, Object>> mine(@AuthenticationPrincipal AuthenticatedUser actor) {
        return assembler.summaries(actor, requests.listForRequester(actor));
    }

    @GetMapping
    public List<Map<String, Object>> queue(@AuthenticationPrincipal AuthenticatedUser actor,
                                           @RequestParam(required = false) UUID categoryId,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String search,
                                           @RequestParam(required = false) String sort) {
        RequestFilter filter = new RequestFilter(categoryId, parseStatus(status), search, parseSort(sort));
        return assembler.summaries(actor, requests.listForStaff(actor, filter));
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID id) {
        return assembler.detail(actor, requests.getForViewer(actor, id));
    }

    @PostMapping("/{id}/assign")
    public Map<String, Object> assign(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID id,
                                      @RequestBody VersionBody body) {
        ServiceRequest saved = assignments.assign(actor, id, null, requireVersion(body.version()));
        return assembler.detail(actor, saved);
    }

    @PostMapping("/{id}/status")
    public Map<String, Object> changeStatus(@AuthenticationPrincipal AuthenticatedUser actor,
                                            @PathVariable UUID id, @RequestBody StatusBody body) {
        if (body.targetStatus() == null) {
            throw new ValidationException("A target status is required");
        }
        TransitionCommand cmd = new TransitionCommand(body.targetStatus(), body.comment(), null,
                requireVersion(body.version()));
        return assembler.detail(actor, transitions.transition(actor, id, cmd));
    }

    @PostMapping("/{id}/comments")
    public Map<String, Object> addComment(@AuthenticationPrincipal AuthenticatedUser actor,
                                          @PathVariable UUID id, @RequestBody CommentBody body) {
        requests.addComment(actor, id, body.comment(), body.version());
        return assembler.detail(actor, requests.requireById(id));
>>>>>>> Stashed changes
    }

    private static long requireVersion(Long version) {
        if (version == null) {
            throw new ValidationException("version is required (the value you last saw)");
        }
        return version;
    }

<<<<<<< Updated upstream
    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
=======
    private static RequestStatus parseStatus(String value) {
>>>>>>> Stashed changes
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
<<<<<<< Updated upstream
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown value for " + field + ": " + value);
        }
=======
            return RequestStatus.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown status: " + value);
        }
    }

    private static RequestSort parseSort(String value) {
        return "age_asc".equals(value) ? RequestSort.NEWEST_FIRST : RequestSort.OLDEST_FIRST;
>>>>>>> Stashed changes
    }
}
