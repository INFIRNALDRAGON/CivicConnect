package com.civicconnect.backend.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.civicconnect.backend.assignment.AssignmentService;
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.dto.CreateRequestDto;
import com.civicconnect.backend.exception.ValidationException;
import com.civicconnect.backend.request.RequestFilter;
import com.civicconnect.backend.request.RequestService;
import com.civicconnect.backend.request.RequestSort;
import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;
import com.civicconnect.backend.status.StatusTransitionService;
import com.civicconnect.backend.status.TransitionCommand;

@RestController
@RequestMapping("/api/requests")
public class ServiceRequestController {

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
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@AuthenticationPrincipal AuthenticatedUser actor,
                                                      @RequestBody CreateRequestDto body) {
        ServiceRequest created = requests.create(actor, body);
        return ResponseEntity.created(URI.create("/api/requests/" + created.getId()))
                .body(assembler.detail(actor, created));
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
    }

    private static long requireVersion(Long version) {
        if (version == null) {
            throw new ValidationException("version is required (the value you last saw)");
        }
        return version;
    }

    private static RequestStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return RequestStatus.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown status: " + value);
        }
    }

    private static RequestSort parseSort(String value) {
        return "age_asc".equals(value) ? RequestSort.NEWEST_FIRST : RequestSort.OLDEST_FIRST;
    }
}
