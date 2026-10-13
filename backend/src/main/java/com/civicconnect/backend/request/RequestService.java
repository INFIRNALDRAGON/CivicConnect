package com.civicconnect.backend.request;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.civicconnect.backend.auth.AccessControlService;
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.auth.Role;
import com.civicconnect.backend.dto.CreateRequestDto;
import com.civicconnect.backend.exception.ConflictException;
import com.civicconnect.backend.exception.NotFoundException;
import com.civicconnect.backend.exception.ValidationException;

/**
 * Public interface of the request module: submit, list, filter, view detail, comment.
 * Depends only on auth (module rule from ADR-M2-02).
 */
@Service
public class RequestService {

    static final int MIN_DESCRIPTION_LENGTH = 10;

    private final RequestStore store;
    private final CategoryCatalog categories;
    private final ReferenceNumberGenerator references;
    private final AccessControlService access;
    private final Clock clock;

    public RequestService(RequestStore store, CategoryCatalog categories, ReferenceNumberGenerator references,
                          AccessControlService access, Clock clock) {
        this.store = store;
        this.categories = categories;
        this.references = references;
        this.access = access;
        this.clock = clock;
    }

    /** FR-001, FR-002, NFR-002. */
    public ServiceRequest create(AuthenticatedUser actor, CreateRequestDto dto) {
        access.requireRole(actor, Role.REQUESTER);

        List<String> errors = new ArrayList<>();
        if (dto == null) {
            throw new ValidationException("Request body is required");
        }
        if (dto.categoryId() == null) {
            errors.add("Please select a category");
        } else if (categories.findActive(dto.categoryId()).isEmpty()) {
            errors.add("The selected category is not available");
        }
        String description = dto.description() == null ? "" : dto.description().trim();
        if (description.length() < MIN_DESCRIPTION_LENGTH) {
            errors.add("Description must be at least " + MIN_DESCRIPTION_LENGTH + " characters");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        String reference = uniqueReference();
        Priority priority = dto.priority() == null ? Priority.MEDIUM : dto.priority();
        ServiceRequest request = new ServiceRequest(reference, dto.categoryId(), description, priority,
                actor.id(), clock.instant());
        return store.insert(request);
    }

    /** FR-003 and FR-008: a requester sees their own, staff see their category scope, managers see all. */
    public ServiceRequest getForViewer(AuthenticatedUser actor, Long id) {
        access.requireAuthenticated(actor);
        ServiceRequest request = requireById(id);
        access.requireCanViewRequest(actor, request.getRequesterId(), request.getCategoryId());
        return request;
    }

    /** FR-004: newest first. */
    public List<ServiceRequest> listForRequester(AuthenticatedUser actor) {
        access.requireRole(actor, Role.REQUESTER);
        return store.findAll().stream()
                .filter(r -> r.getRequesterId().equals(actor.id()))
                .sorted(Comparator.comparing(ServiceRequest::getCreatedAt).reversed())
                .toList();
    }

    /** FR-006 (scope) and FR-007 (search, filter, sort by age). */
    public List<ServiceRequest> listForStaff(AuthenticatedUser actor, RequestFilter filter) {
        access.requireStaffOrManager(actor);
        RequestFilter f = filter == null ? RequestFilter.none() : filter;
        String needle = f.text() == null ? "" : f.text().trim().toLowerCase(Locale.ROOT);

        Comparator<ServiceRequest> byAge = Comparator.comparing(ServiceRequest::getCreatedAt)
                .thenComparing(ServiceRequest::getId);
        if (f.sortOrDefault() == RequestSort.NEWEST_FIRST) {
            byAge = byAge.reversed();
        }

        return store.findAll().stream()
                .filter(r -> access.canAccessCategory(actor, r.getCategoryId()))
                .filter(r -> f.categoryId() == null || f.categoryId().equals(r.getCategoryId()))
                .filter(r -> f.status() == null || f.status() == r.getStatus())
                .filter(r -> needle.isEmpty()
                        || r.getReferenceNumber().toLowerCase(Locale.ROOT).contains(needle)
                        || r.getDescription().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(byAge)
                .toList();
    }

    /** FR-011 and CR-05: refused once the request is Resolved or Closed. */
    public RequestComment addComment(AuthenticatedUser actor, Long requestId, String text) {
        access.requireStaffOrManager(actor);
        ServiceRequest request = requireById(requestId);
        access.requireCategoryAccess(actor, request.getCategoryId());
        if (text == null || text.isBlank()) {
            throw new ValidationException("Comment text is required");
        }
        if (request.isReadOnly()) {
            throw new ConflictException("This request is " + request.getStatus().label()
                    + " and no longer accepts comments");
        }
        return store.addComment(new RequestComment(null, requestId, actor.id(), text.trim(), clock.instant()));
    }

    /** Comments are internal: staff and managers only. */
    public List<RequestComment> commentsFor(AuthenticatedUser actor, Long requestId) {
        access.requireStaffOrManager(actor);
        ServiceRequest request = requireById(requestId);
        access.requireCategoryAccess(actor, request.getCategoryId());
        return store.findComments(requestId);
    }

    public String categoryName(Long categoryId) {
        return categories.findById(categoryId).map(Category::name).orElse("Unknown");
    }

    // ---- used by other modules through this public interface -------------------------------

    /** For the status and assignment modules. No access check: callers do their own. */
    public ServiceRequest requireById(Long id) {
        return store.findById(id).orElseThrow(() -> new NotFoundException("Request not found"));
    }

    /**
     * Status module only. Saves a changed request if nobody else changed it since the caller read it.
     * Throws 409 otherwise (ASR-03).
     */
    public ServiceRequest saveStatusChange(ServiceRequest updated, long expectedVersion) {
        return store.replaceIfVersion(updated, expectedVersion).orElseThrow(() ->
                new ConflictException("This request was changed by someone else. Reload and try again"));
    }

    /** Status module only. Undoes {@link #saveStatusChange} when a later step fails. */
    public void undoStatusChange(ServiceRequest previousSnapshot) {
        store.restore(previousSnapshot);
    }

    /** Reporting module only. Managers only: aggregates are built from all requests. */
    public List<ServiceRequest> findAllForReporting(AuthenticatedUser actor) {
        access.requireRole(actor, Role.MANAGER);
        return store.findAll();
    }

    private String uniqueReference() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = references.next();
            if (!store.existsByReference(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique reference number");
    }
}
