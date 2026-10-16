package com.civicconnect.backend.audit;

import java.util.List;
<<<<<<< Updated upstream
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
=======
import java.util.UUID;
>>>>>>> Stashed changes

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

<<<<<<< Updated upstream
import com.civicconnect.backend.status.StatusChangedEvent;

/**
 * Observer that writes the append-only history (NFR-006, ASR-01).
 * <p>
 * Deliberately a plain synchronous {@code @EventListener}: it runs in the publisher's thread and
 * transaction, so if this write throws, the status change is rolled back. There is no update or
 * delete method, on purpose. The list stands in for the status_history table.
 */
@Component
public class AuditLogService {

    private final List<StatusHistoryEntry> entries = new CopyOnWriteArrayList<>();
    private final AtomicLong ids = new AtomicLong();

    @EventListener
    public void onStatusChanged(StatusChangedEvent event) {
        entries.add(new StatusHistoryEntry(ids.incrementAndGet(), event.requestId(), event.previousStatus(),
                event.newStatus(), event.actorId(), event.timestamp(), event.comment()));
    }

    /** Ordered oldest to newest. Callers must have checked the viewer may see this request. 
     *@param requestId Request to assign
     *@return 
     */  
    public List<StatusHistoryEntry> historyFor(Long requestId) {
        return entries.stream().filter(e -> e.requestId().equals(requestId)).toList();
=======
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
>>>>>>> Stashed changes
    }
}
