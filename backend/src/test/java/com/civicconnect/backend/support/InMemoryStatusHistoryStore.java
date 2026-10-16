package com.civicconnect.backend.support;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import com.civicconnect.backend.audit.StatusHistoryEntry;
import com.civicconnect.backend.audit.StatusHistoryStore;

public class InMemoryStatusHistoryStore implements StatusHistoryStore {

    private final List<StatusHistoryEntry> entries = new CopyOnWriteArrayList<>();
    private final AtomicLong ids = new AtomicLong(7000);

    @Override
    public StatusHistoryEntry append(StatusHistoryEntry e) {
        StatusHistoryEntry stored = new StatusHistoryEntry(Ids.of(ids.incrementAndGet()), e.requestId(),
                e.previousStatus(), e.newStatus(), e.changedBy(), e.changedAt(), e.comment());
        entries.add(stored);
        return stored;
    }

    @Override
    public List<StatusHistoryEntry> findByRequest(UUID requestId) {
        return entries.stream().filter(e -> e.requestId().equals(requestId)).toList();
    }
}
