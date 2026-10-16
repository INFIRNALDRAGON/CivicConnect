package com.civicconnect.backend.controller;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.civicconnect.backend.audit.AuditLogService;
import com.civicconnect.backend.audit.StatusHistoryEntry;
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.auth.UserDirectory;
import com.civicconnect.backend.request.RequestComment;
import com.civicconnect.backend.request.RequestService;
import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;
import com.civicconnect.backend.status.RequestStates;

@Component
public class RequestAssembler {

    private final RequestService requests;
    private final AuditLogService audit;
    private final UserDirectory users;
    private final Clock clock;

    public RequestAssembler(RequestService requests, AuditLogService audit, UserDirectory users, Clock clock) {
        this.requests = requests;
        this.audit = audit;
        this.users = users;
        this.clock = clock;
    }

    public List<Map<String, Object>> summaries(AuthenticatedUser viewer, List<ServiceRequest> list) {
        Instant now = clock.instant();
        Map<UUID, String> categoryNames = requests.categoryNames();
        Map<UUID, Duration> thresholds = requests.overdueThresholds();
        Map<UUID, String> staffNames = viewer.isStaffOrManager()
                ? users.namesOf(list.stream().map(ServiceRequest::getAssignedStaffId)
                        .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet()))
                : Map.of();
        return list.stream().map(r -> base(viewer, r, now, categoryNames, thresholds, staffNames)).toList();
    }

    public Map<String, Object> detail(AuthenticatedUser viewer, ServiceRequest request) {
        Instant now = clock.instant();
        List<StatusHistoryEntry> entries = audit.historyFor(request.getId());
        List<RequestComment> notes = viewer.isStaffOrManager()
                ? requests.commentsFor(viewer, request.getId()) : List.of();

        Set<UUID> ids = new HashSet<>();
        if (request.getAssignedStaffId() != null) {
            ids.add(request.getAssignedStaffId());
        }
        entries.forEach(e -> ids.add(e.changedBy()));
        notes.forEach(n -> ids.add(n.authorId()));
        Map<UUID, String> names = viewer.isStaffOrManager() ? users.namesOf(ids) : Map.of();

        Map<String, Object> dto = base(viewer, request, now, requests.categoryNames(),
                requests.overdueThresholds(), names);
        dto.put("history", history(viewer, request, entries, notes, names));
        return dto;
    }

    private Map<String, Object> base(AuthenticatedUser viewer, ServiceRequest r, Instant now,
                                     Map<UUID, String> categoryNames, Map<UUID, Duration> thresholds,
                                     Map<UUID, String> staffNames) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", r.getId().toString());
        dto.put("referenceNumber", r.getReferenceNumber());
        dto.put("categoryId", r.getCategoryId().toString());
        dto.put("categoryName", categoryNames.getOrDefault(r.getCategoryId(), "Unknown"));
        dto.put("description", r.getDescription());
        dto.put("status", r.getStatus().name());
        dto.put("priority", r.getPriority().name());
        dto.put("createdAt", r.getCreatedAt().toString());
        dto.put("updatedAt", r.getUpdatedAt().toString());
        dto.put("resolvedAt", r.getResolvedAt() == null ? null : r.getResolvedAt().toString());
        dto.put("assigned", r.getAssignedStaffId() != null);
        dto.put("overdue", requests.isOverdue(r, now, thresholds));
        dto.put("version", r.getVersion());
        if (viewer.isStaffOrManager()) {
            dto.put("assignedStaffName", r.getAssignedStaffId() == null ? null : staffNames.get(r.getAssignedStaffId()));
            dto.put("assignedAt", r.getAssignedAt() == null ? null : r.getAssignedAt().toString());
            dto.put("requesterReference", requesterReference(r.getRequesterId()));
            dto.put("allowedTransitions", allowedTransitions(viewer, r));
        }
        return dto;
    }

    private List<Map<String, Object>> history(AuthenticatedUser viewer, ServiceRequest request,
                                              List<StatusHistoryEntry> entries, List<RequestComment> notes,
                                              Map<UUID, String> names) {
        boolean staff = viewer.isStaffOrManager();
        List<Map<String, Object>> out = new ArrayList<>();
        List<Object[]> timeline = new ArrayList<>();

        for (StatusHistoryEntry e : entries) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", e.id().toString());
            item.put("previousStatus", e.previousStatus() == null ? null : e.previousStatus().name());
            item.put("newStatus", e.newStatus().name());
            item.put("changedAt", e.changedAt().toString());
            item.put("comment", e.comment());
            if (staff) {
                item.put("changedByName", e.previousStatus() == null
                        ? "Requester " + requesterReference(request.getRequesterId())
                        : names.getOrDefault(e.changedBy(), "Staff member"));
            }
            timeline.add(new Object[] {e.changedAt(), 0, item});
        }

        for (RequestComment c : notes) {
            RequestStatus during = statusAt(entries, c.createdAt());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.id().toString());
            item.put("previousStatus", during.name());
            item.put("newStatus", during.name());
            item.put("changedAt", c.createdAt().toString());
            item.put("comment", c.text());
            item.put("changedByName", names.getOrDefault(c.authorId(), "Staff member"));
            timeline.add(new Object[] {c.createdAt(), 1, item});
        }

        timeline.sort(Comparator.<Object[], Instant>comparing(t -> (Instant) t[0])
                .thenComparing(t -> (Integer) t[1]));
        for (Object[] t : timeline) {
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) t[2];
            out.add(item);
        }
        return out;
    }

    private static RequestStatus statusAt(List<StatusHistoryEntry> entries, Instant when) {
        RequestStatus status = RequestStatus.SUBMITTED;
        for (StatusHistoryEntry e : entries) {
            if (!e.changedAt().isAfter(when)) {
                status = e.newStatus();
            }
        }
        return status;
    }

    private static List<String> allowedTransitions(AuthenticatedUser viewer, ServiceRequest r) {
        if (r.getStatus() == RequestStatus.ASSIGNED
                && !viewer.id().equals(r.getAssignedStaffId()) && !viewer.isManager()) {
            return List.of();
        }
        return RequestStates.of(r.getStatus()).allowedNext().stream().sorted().map(Enum::name).toList();
    }

    static String requesterReference(UUID requesterId) {
        return "R-" + requesterId.toString().replace("-", "").substring(0, 6).toUpperCase(java.util.Locale.ROOT);
    }
}
