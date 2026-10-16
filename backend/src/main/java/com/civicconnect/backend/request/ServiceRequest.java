package com.civicconnect.backend.request;

import java.time.Instant;
import java.util.UUID;

public class ServiceRequest {

    private UUID id;
    private String referenceNumber;
    private final UUID categoryId;
    private final String description;
    private final Priority priority;
    private final UUID requesterId;
    private final Instant createdAt;

    private RequestStatus status;
    private UUID assignedStaffId;
    private Instant assignedAt;
    private Instant resolvedAt;
    private Instant updatedAt;
    private long version;

    public ServiceRequest(UUID categoryId, String description, Priority priority, UUID requesterId,
                          Instant createdAt) {
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

    public static ServiceRequest rehydrate(UUID id, String referenceNumber, UUID categoryId, String description,
                                           Priority priority, UUID requesterId, Instant createdAt,
                                           RequestStatus status, UUID assignedStaffId, Instant assignedAt,
                                           Instant resolvedAt, Instant updatedAt, long version) {
        ServiceRequest r = new ServiceRequest(categoryId, description, priority, requesterId, createdAt);
        r.id = id;
        r.referenceNumber = referenceNumber;
        r.status = status;
        r.assignedStaffId = assignedStaffId;
        r.assignedAt = assignedAt;
        r.resolvedAt = resolvedAt;
        r.updatedAt = updatedAt == null ? createdAt : updatedAt;
        r.version = version;
        return r;
    }

    public ServiceRequest copy() {
        return new ServiceRequest(this);
    }

    public void applyStatus(RequestStatus newStatus, Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
        if (newStatus == RequestStatus.RESOLVED) {
            this.resolvedAt = now;
        }
    }

    public void assignTo(UUID staffId, Instant now) {
        this.assignedStaffId = staffId;
        this.assignedAt = now;
    }

    public boolean isReadOnly() {
        return status.isReadOnly();
    }

    public UUID getId() { return id; }
    public String getReferenceNumber() { return referenceNumber; }
    public UUID getCategoryId() { return categoryId; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public UUID getRequesterId() { return requesterId; }
    public Instant getCreatedAt() { return createdAt; }
    public RequestStatus getStatus() { return status; }
    public UUID getAssignedStaffId() { return assignedStaffId; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public void setId(UUID id) { this.id = id; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public void setVersion(long version) { this.version = version; }
}
