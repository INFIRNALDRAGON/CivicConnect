package com.civicconnect.backend.request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;

@Component
class JpaRequestStore implements RequestStore {

    private final RequestRepository requests;
    private final RequestTimesRepository times;
    private final RequestCommentRepository comments;

    @PersistenceContext
    private EntityManager entityManager;

    JpaRequestStore(RequestRepository requests, RequestTimesRepository times, RequestCommentRepository comments) {
        this.requests = requests;
        this.times = times;
        this.comments = comments;
    }

    @Override
    @Transactional
    public ServiceRequest insert(ServiceRequest request) {
        RequestEntity entity = new RequestEntity();
        entity.categoryId = request.getCategoryId();
        entity.description = request.getDescription();
        entity.status = request.getStatus().name();
        entity.priority = request.getPriority().name();
        entity.requesterId = request.getRequesterId();
        requests.saveAndFlush(entity);
        entityManager.refresh(entity);
        return toDomain(entity, request.getCreatedAt(), null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServiceRequest> findById(UUID id) {
        return requests.findById(id).map(e -> toDomain(e, times.findById(e.id).orElse(null)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceRequest> findAll() {
        Map<UUID, RequestTimesEntity> byId = new HashMap<>();
        times.findAll().forEach(t -> byId.put(t.id, t));
        return requests.findAll().stream().map(e -> toDomain(e, byId.get(e.id))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceRequest> findByRequester(UUID requesterId) {
        List<RequestEntity> found = requests.findByRequesterId(requesterId);
        if (found.isEmpty()) {
            return List.of();
        }
        Map<UUID, RequestTimesEntity> byId = new HashMap<>();
        times.findByIdIn(found.stream().map(e -> e.id).toList()).forEach(t -> byId.put(t.id, t));
        return found.stream().map(e -> toDomain(e, byId.get(e.id))).toList();
    }

    @Override
    @Transactional
    public Optional<ServiceRequest> replaceIfVersion(ServiceRequest updated, long expectedVersion) {
        RequestEntity entity = requests.findById(updated.getId()).orElse(null);
        if (entity == null || entity.version.longValue() != expectedVersion) {
            return Optional.empty();
        }
        entity.status = updated.getStatus().name();
        entity.assignedStaffId = updated.getAssignedStaffId();
        try {
            requests.saveAndFlush(entity);
            entityManager.refresh(entity);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException lost) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity, updated.getCreatedAt(), updated.getAssignedAt(), updated.getResolvedAt()));
    }

    @Override
    @Transactional
    public RequestComment addComment(RequestComment comment) {
        RequestCommentEntity entity = new RequestCommentEntity();
        entity.requestId = comment.requestId();
        entity.authorId = comment.authorId();
        entity.body = comment.text();
        entity.createdAt = comment.createdAt();
        RequestCommentEntity saved = comments.saveAndFlush(entity);
        return toComment(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestComment> findComments(UUID requestId) {
        return comments.findByRequestIdOrderByCreatedAtAsc(requestId).stream()
                .map(JpaRequestStore::toComment).toList();
    }

    private static RequestComment toComment(RequestCommentEntity e) {
        return new RequestComment(e.id, e.requestId, e.authorId, e.body, e.createdAt);
    }

    private static ServiceRequest toDomain(RequestEntity e, RequestTimesEntity t) {
        if (t == null) {
            return toDomain(e, e.updatedAt, null, null);
        }
        return toDomain(e, t.submittedAt == null ? e.updatedAt : t.submittedAt, t.assignedAt, t.resolvedAt);
    }

    private static ServiceRequest toDomain(RequestEntity e, java.time.Instant createdAt,
                                           java.time.Instant assignedAt, java.time.Instant resolvedAt) {
        return ServiceRequest.rehydrate(e.id, e.referenceNumber, e.categoryId, e.description,
                Priority.valueOf(e.priority), e.requesterId, createdAt, RequestStatus.valueOf(e.status),
                e.assignedStaffId, assignedAt, resolvedAt, e.updatedAt, e.version);
    }
}
