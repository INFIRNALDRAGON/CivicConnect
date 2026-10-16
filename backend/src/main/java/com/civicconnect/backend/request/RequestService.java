package com.civicconnect.backend.request;

import java.time.Clock;
<<<<<<< Updated upstream
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
=======
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
>>>>>>> Stashed changes

import com.civicconnect.backend.auth.AccessControlService;
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.auth.Role;
import com.civicconnect.backend.dto.CreateRequestDto;
import com.civicconnect.backend.exception.ConflictException;
import com.civicconnect.backend.exception.NotFoundException;
import com.civicconnect.backend.exception.ValidationException;

<<<<<<< Updated upstream
/**
 * Public interface of the request module: submit, list, filter, view detail, comment.
 * Depends only on auth (module rule from ADR-M2-02).
 */
=======
>>>>>>> Stashed changes
@Service
public class RequestService {

    static final int MIN_DESCRIPTION_LENGTH = 10;
<<<<<<< Updated upstream

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

    /** FR-001, FR-002, NFR-002. 
     *@param actor
     *@param dto
     *@return
     */
    public ServiceRequest create(AuthenticatedUser actor, CreateRequestDto dto) {
        access.requireRole(actor, Role.REQUESTER);

        List<String> errors = new ArrayList<>();
        if (dto == null) {
            throw new ValidationException("Request body is required");
        }
=======
    static final int MAX_DESCRIPTION_LENGTH = 2000;
    static final int MAX_COMMENT_LENGTH = 1000;
    static final int DEFAULT_OVERDUE_HOURS = 72;

    private final RequestStore store;
    private final CategoryCatalog categories;
    private final AccessControlService access;
    private final Clock clock;
    private final ApplicationEventPublisher publisher;

    public RequestService(RequestStore store, CategoryCatalog categories, AccessControlService access,
                          Clock clock, ApplicationEventPublisher publisher) {
        this.store = store;
        this.categories = categories;
        this.access = access;
        this.clock = clock;
        this.publisher = publisher;
    }

    @Transactional
    public ServiceRequest create(AuthenticatedUser actor, CreateRequestDto dto) {
        access.requireRole(actor, Role.REQUESTER);
        if (dto == null) {
            throw new ValidationException("Request body is required");
        }

        List<String> errors = new ArrayList<>();
>>>>>>> Stashed changes
        if (dto.categoryId() == null) {
            errors.add("Please select a category");
        } else if (categories.findActive(dto.categoryId()).isEmpty()) {
            errors.add("The selected category is not available");
        }
        String description = dto.description() == null ? "" : dto.description().trim();
        if (description.length() < MIN_DESCRIPTION_LENGTH) {
            errors.add("Description must be at least " + MIN_DESCRIPTION_LENGTH + " characters");
        }
<<<<<<< Updated upstream
=======
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            errors.add("Description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
>>>>>>> Stashed changes
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

<<<<<<< Updated upstream
        String reference = uniqueReference();
        Priority priority = dto.priority() == null ? Priority.MEDIUM : dto.priority();
        ServiceRequest request = new ServiceRequest(reference, dto.categoryId(), description, priority,
                actor.id(), clock.instant());
        return store.insert(request);
    }

    /** FR-003 and FR-008: a requester sees their own, staff see their category scope, managers see all. 
     *@param actor
     *@param id
     *@return
     */
    public ServiceRequest getForViewer(AuthenticatedUser actor, Long id) {
=======
        Priority priority = dto.priority() == null ? Priority.MEDIUM : dto.priority();
        Instant now = clock.instant();
        ServiceRequest stored = store.insert(
                new ServiceRequest(dto.categoryId(), description, priority, actor.id(), now));
        publisher.publishEvent(new RequestSubmittedEvent(stored.getId(), stored.getReferenceNumber(),
                stored.getRequesterId(), now));
        return stored;
    }

    public ServiceRequest getForViewer(AuthenticatedUser actor, UUID id) {
>>>>>>> Stashed changes
        access.requireAuthenticated(actor);
        ServiceRequest request = requireById(id);
        access.requireCanViewRequest(actor, request.getRequesterId(), request.getCategoryId());
        return request;
    }

<<<<<<< Updated upstream
    /** FR-004: newest first. 
     *@param actor
     *@return
     */
    public List<ServiceRequest> listForRequester(AuthenticatedUser actor) {
        access.requireRole(actor, Role.REQUESTER);
        return store.findAll().stream()
                .filter(r -> r.getRequesterId().equals(actor.id()))
=======
    public List<ServiceRequest> listForRequester(AuthenticatedUser actor) {
        access.requireRole(actor, Role.REQUESTER);
        return store.findByRequester(actor.id()).stream()
>>>>>>> Stashed changes
                .sorted(Comparator.comparing(ServiceRequest::getCreatedAt).reversed())
                .toList();
    }

<<<<<<< Updated upstream
    /** FR-006 (scope) and FR-007 (search, filter, sort by age). 
     *@param actor
     *@param filter
     *@return
     */
=======
>>>>>>> Stashed changes
    public List<ServiceRequest> listForStaff(AuthenticatedUser actor, RequestFilter filter) {
        access.requireStaffOrManager(actor);
        RequestFilter f = filter == null ? RequestFilter.none() : filter;
        String needle = f.text() == null ? "" : f.text().trim().toLowerCase(Locale.ROOT);

        Comparator<ServiceRequest> byAge = Comparator.comparing(ServiceRequest::getCreatedAt)
<<<<<<< Updated upstream
                .thenComparing(ServiceRequest::getId);
=======
                .thenComparing(ServiceRequest::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
    /** FR-011 and CR-05: refused once the request is Resolved or Closed. 
     *@param actor
     *@param requestId
     *@param text
     *@return
     */
    public RequestComment addComment(AuthenticatedUser actor, Long requestId, String text) {
=======
    @Transactional
    public RequestComment addComment(AuthenticatedUser actor, UUID requestId, String text) {
        return addComment(actor, requestId, text, null);
    }

    @Transactional
    public RequestComment addComment(AuthenticatedUser actor, UUID requestId, String text, Long expectedVersion) {
>>>>>>> Stashed changes
        access.requireStaffOrManager(actor);
        ServiceRequest request = requireById(requestId);
        access.requireCategoryAccess(actor, request.getCategoryId());
        if (text == null || text.isBlank()) {
            throw new ValidationException("Comment text is required");
        }
<<<<<<< Updated upstream
=======
        if (text.trim().length() > MAX_COMMENT_LENGTH) {
            throw new ValidationException("Keep notes under " + MAX_COMMENT_LENGTH + " characters");
        }
>>>>>>> Stashed changes
        if (request.isReadOnly()) {
            throw new ConflictException("This request is " + request.getStatus().label()
                    + " and no longer accepts comments");
        }
<<<<<<< Updated upstream
        return store.addComment(new RequestComment(null, requestId, actor.id(), text.trim(), clock.instant()));
    }

    /** Comments are internal: staff and managers only. 
     *@param actor
     *@param requestId
     *@return
     */
    public List<RequestComment> commentsFor(AuthenticatedUser actor, Long requestId) {
=======
        if (expectedVersion != null && expectedVersion != request.getVersion()) {
            throw new ConflictException("This request was changed by someone else. Reload and try again");
        }
        return store.addComment(new RequestComment(null, requestId, actor.id(), text.trim(), clock.instant()));
    }

    public List<RequestComment> commentsFor(AuthenticatedUser actor, UUID requestId) {
>>>>>>> Stashed changes
        access.requireStaffOrManager(actor);
        ServiceRequest request = requireById(requestId);
        access.requireCategoryAccess(actor, request.getCategoryId());
        return store.findComments(requestId);
    }

<<<<<<< Updated upstream
    public String categoryName(Long categoryId) {
        return categories.findById(categoryId).map(Category::name).orElse("Unknown");
    }

    // ---- used by other modules through this public interface -------------------------------

    /** For the status and assignment modules. No access check: callers do their own. 
     *@param id
     *@return
     */
    public ServiceRequest requireById(Long id) {
        return store.findById(id).orElseThrow(() -> new NotFoundException("Request not found"));
    }

    /**
     * Status module only. Saves a changed request if nobody else changed it since the caller read it.
     * Throws 409 otherwise (ASR-03).
     *@param updated
     *@param expectedVersion
     *@return
     */
=======
    public String categoryName(UUID categoryId) {
        return categories.findById(categoryId).map(Category::name).orElse("Unknown");
    }

    public Map<UUID, String> categoryNames() {
        Map<UUID, String> names = new HashMap<>();
        categories.all().forEach(c -> names.put(c.id(), c.name()));
        return names;
    }

    public List<Category> allCategories() {
        return categories.all();
    }

    public List<Category> activeCategories() {
        return categories.allActive();
    }

    public Duration overdueAfter(UUID categoryId) {
        int hours = categories.findById(categoryId).map(Category::overdueAfterHours).orElse(DEFAULT_OVERDUE_HOURS);
        return Duration.ofHours(hours);
    }

    public boolean isOverdue(ServiceRequest request, Instant now, Map<UUID, Duration> thresholds) {
        Duration limit = thresholds.getOrDefault(request.getCategoryId(), Duration.ofHours(DEFAULT_OVERDUE_HOURS));
        return request.getStatus().isOpen() && Duration.between(request.getCreatedAt(), now).compareTo(limit) > 0;
    }

    public Map<UUID, Duration> overdueThresholds() {
        Map<UUID, Duration> thresholds = new HashMap<>();
        categories.all().forEach(c -> thresholds.put(c.id(), Duration.ofHours(c.overdueAfterHours())));
        return thresholds;
    }

    public ServiceRequest requireById(UUID id) {
        return store.findById(id).orElseThrow(() -> new NotFoundException("We could not find that request."));
    }

>>>>>>> Stashed changes
    public ServiceRequest saveStatusChange(ServiceRequest updated, long expectedVersion) {
        return store.replaceIfVersion(updated, expectedVersion).orElseThrow(() ->
                new ConflictException("This request was changed by someone else. Reload and try again"));
    }

<<<<<<< Updated upstream
    /** Status module only. Undoes {@link #saveStatusChange} when a later step fails. 
     *@param previousSnapshot     
     */
    public void undoStatusChange(ServiceRequest previousSnapshot) {
        store.restore(previousSnapshot);
    }

    /** Reporting module only. Managers only: aggregates are built from all requests. 
     *@param actor
     *@return
     */
=======
>>>>>>> Stashed changes
    public List<ServiceRequest> findAllForReporting(AuthenticatedUser actor) {
        access.requireRole(actor, Role.MANAGER);
        return store.findAll();
    }
<<<<<<< Updated upstream

    private String uniqueReference() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = references.next();
            if (!store.existsByReference(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique reference number");
    }
=======
>>>>>>> Stashed changes
}
