package com.civicconnect.backend.audit;

import java.util.List;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.RequestSubmittedEvent;
import com.civicconnect.backend.status.StatusChangedEvent;

@Component
public class AuditLogService {

    private final StatusHistoryStore store;

    public AuditLogService(StatusHistoryStore store) {
        this.store = store;
    }

    @EventListener
    public void onRequestSubmitted(RequestSubmittedEvent event) {
        store.append(new StatusHistoryEntry(null, event.requestId(), null, RequestStatus.SUBMITTED,
                event.requesterId(), event.timestamp(), null));
    }

    @EventListener
    public void onStatusChanged(StatusChangedEvent event) {
        store.append(new StatusHistoryEntry(null, event.requestId(), event.previousStatus(), event.newStatus(),
                event.actorId(), event.timestamp(), event.comment()));
    }

    public List<StatusHistoryEntry> historyFor(UUID requestId) {
        return store.findByRequest(requestId);
    }
}
