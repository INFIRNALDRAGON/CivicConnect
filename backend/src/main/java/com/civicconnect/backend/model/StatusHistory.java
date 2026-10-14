package com.civicconnect.backend.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Append-only audit row (NFR-006). @Immutable stops Hibernate ever issuing an UPDATE. */
@Entity
@Immutable
@Table(name = "status_history")
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ServiceRequest request;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private RequestStatus previousStatus;          // null for the submission row

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private RequestStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(name = "changed_at", insertable = false, updatable = false)
    private Instant changedAt;                      // database default now()

    private String comment;

    protected StatusHistory() { }

    public StatusHistory(ServiceRequest request, RequestStatus previousStatus,
                         RequestStatus newStatus, User changedBy, String comment) {
        this.request = request;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.comment = comment;
    }

    public UUID getId() { return id; }
    public ServiceRequest getRequest() { return request; }
    public RequestStatus getPreviousStatus() { return previousStatus; }
    public RequestStatus getNewStatus() { return newStatus; }
    public User getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
    public String getComment() { return comment; }
}
