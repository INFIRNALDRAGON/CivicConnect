package com.civicconnect.backend.request;

import java.time.Clock;
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

import com.civicconnect.backend.auth.AccessControlService;
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.auth.Role;
import com.civicconnect.backend.dto.CreateRequestDto;
import com.civicconnect.backend.exception.ConflictException;
import com.civicconnect.backend.exception.NotFoundException;
import com.civicconnect.backend.exception.ValidationException;

@Service
public class RequestService {

    static final int MIN_DESCRIPTION_LENGTH = 10;
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
        if (dto.categoryId() == null) {
            errors.add("Please select a category");
        } else if (categories.findActive(dto.categoryId()).isEmpty()) {
            errors.add("The selected category is not available");
        }
        String description = dto.description() == null ? "" : dto.description().trim();
        if (description.length() < MIN_DESCRIPTION_LENGTH) {
            errors.add("Description must be at least " + MIN_DESCRIPTION_LENGTH + " characters");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            errors.add("Description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Priority priority = dto.priority() == null ? Priority.MEDIUM : dto.priority();
        Instant now = clock.instant();
        ServiceRequest stored = store.insert(
                new ServiceRequest(dto.categoryId(), description, priority, actor.id(), now));
        publisher.publishEvent(new RequestSubmittedEvent(stored.getId(), stored.getReferenceNumber(),
                stored.getRequesterId(), now));
        return stored;
    }

    public ServiceRequest getForViewer(AuthenticatedUser actor, UUID id) {
        access.requireAuthenticated(actor);
        ServiceRequest request = requireById(id);
        access.requireCanViewRequest(actor, request.getRequesterId(), request.getCategoryId());
        return request;
    }

    public List<ServiceRequest> listForRequester(AuthenticatedUser actor) {
        access.requireRole(actor, Role.REQUESTER);
        return store.findByRequester(actor.id()).stream()
                .sorted(Comparator.comparing(ServiceRequest::getCreatedAt).reversed())
                .toList();
    }

    public List<ServiceRequest> listForStaff(AuthenticatedUser actor, RequestFilter filter) {
        access.requireStaffOrManager(actor);
        RequestFilter f = filter == null ? RequestFilter.none() : filter;
        String needle = f.text() == null ? "" : f.text().trim().toLowerCase(Locale.ROOT);

        Comparator<ServiceRequest> byAge = Comparator.comparing(ServiceRequest::getCreatedAt)
                .thenComparing(ServiceRequest::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
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

    @Transactional
    public RequestComment addComment(AuthenticatedUser actor, UUID requestId, String text) {
        return addComment(actor, requestId, text, null);
    }

    @Transactional
    public RequestComment addComment(AuthenticatedUser actor, UUID requestId, String text, Long expectedVersion) {
        access.requireStaffOrManager(actor);
        ServiceRequest request = requireById(requestId);
        access.requireCategoryAccess(actor, request.getCategoryId());
        if (text == null || text.isBlank()) {
            throw new ValidationException("Comment text is required");
        }
        if (text.trim().length() > MAX_COMMENT_LENGTH) {
            throw new ValidationException("Keep notes under " + MAX_COMMENT_LENGTH + " characters");
        }
        if (request.isReadOnly()) {
            throw new ConflictException("This request is " + request.getStatus().label()
                    + " and no longer accepts comments");
        }
        if (expectedVersion != null && expectedVersion != request.getVersion()) {
            throw new ConflictException("This request was changed by someone else. Reload and try again");
        }
        return store.addComment(new RequestComment(null, requestId, actor.id(), text.trim(), clock.instant()));
    }

    public List<RequestComment> commentsFor(AuthenticatedUser actor, UUID requestId) {
        access.requireStaffOrManager(actor);
        ServiceRequest request = requireById(requestId);
        access.requireCategoryAccess(actor, request.getCategoryId());
        return store.findComments(requestId);
    }

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

    public ServiceRequest saveStatusChange(ServiceRequest updated, long expectedVersion) {
        return store.replaceIfVersion(updated, expectedVersion).orElseThrow(() ->
                new ConflictException("This request was changed by someone else. Reload and try again"));
    }

    public List<ServiceRequest> findAllForReporting(AuthenticatedUser actor) {
        access.requireRole(actor, Role.MANAGER);
        return store.findAll();
    }
}
