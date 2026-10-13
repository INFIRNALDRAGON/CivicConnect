package com.civicconnect.backend.request;

import java.time.Instant;

/**
 * The domain object for one service request. Plain Java: no persistence annotations.
 * <p>
 * Mutation rules: the request module sets the creation fields once; only the status module
 * calls {@link #applyStatus} and {@link #assignTo}. The {@code version} counter is bumped by
 * the store on every committed change and is what makes two simultaneous updates safe (ASR-03).
 */
public class ServiceRequest {

    private Long id;
    private final String referenceNumber;
    private final Long categoryId;
    private final String description;
    private final Priority priority;
    private final Long requesterId;
    private final Instant createdAt;

    private RequestStatus status;
    private Long assignedStaffId;
    private Instant assignedAt;
    private Instant resolvedAt;
    private Instant updatedAt;
    private long version;

    public ServiceRequest(String referenceNumber, Long categoryId, String description,
                          Priority priority, Long requesterId, Instant createdAt) {
        this.referenceNumber = referenceNumber;
        this.categoryId = categoryId;
        this.description = description;
        this.priority = priority;
        this.requesterId = requesterId;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.status = RequestStatus.SUBMITTED;
    }

    private ServiceRequest(ServiceRequest o) {
        this.id = o.id;
        this.referenceNumber = o.referenceNumber;
        this.categoryId = o.categoryId;
        this.description = o.description;
        this.priority = o.priority;
        this.requesterId = o.requesterId;
        this.createdAt = o.createdAt;
        this.status = o.status;
        this.assignedStaffId = o.assignedStaffId;
        this.assignedAt = o.assignedAt;
        this.resolvedAt = o.resolvedAt;
        this.updatedAt = o.updatedAt;
        this.version = o.version;
    }

    /** Defensive copy. The store hands out copies so callers cannot change stored state by accident. */
    public ServiceRequest copy() {
        return new ServiceRequest(this);
    }

    /** Status module only. */
    public void applyStatus(RequestStatus newStatus, Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
        if (newStatus == RequestStatus.RESOLVED) {
            this.resolvedAt = now;
        }
    }

    /** Status module only. Records owner and assignment timestamp (FR-009). */
    public void assignTo(Long staffId, Instant now) {
        this.assignedStaffId = staffId;
        this.assignedAt = now;
    }

    public boolean isReadOnly() {
        return status.isReadOnly();
    }

    public Long getId() { return id; }
    public String getReferenceNumber() { return referenceNumber; }
    public Long getCategoryId() { return categoryId; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public Long getRequesterId() { return requesterId; }
    public Instant getCreatedAt() { return createdAt; }
    public RequestStatus getStatus() { return status; }
    public Long getAssignedStaffId() { return assignedStaffId; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    /** Store only. */
    public void setId(Long id) { this.id = id; }

    /** Store only. */
    public void setVersion(long version) { this.version = version; }
}
