package com.civicconnect.backend.audit;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.civicconnect.backend.request.RequestStatus;

@Component
class JpaStatusHistoryStore implements StatusHistoryStore {

    private final StatusHistoryRepository history;

    JpaStatusHistoryStore(StatusHistoryRepository history) {
        this.history = history;
    }

    @Override
    @Transactional
    public StatusHistoryEntry append(StatusHistoryEntry entry) {
        StatusHistoryEntity entity = new StatusHistoryEntity();
        entity.requestId = entry.requestId();
        entity.previousStatus = entry.previousStatus() == null ? null : entry.previousStatus().name();
        entity.newStatus = entry.newStatus().name();
        entity.changedBy = entry.changedBy();
        entity.changedAt = entry.changedAt();
        entity.comment = entry.comment();
        return toDomain(history.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusHistoryEntry> findByRequest(UUID requestId) {
        return history.findByRequestIdOrderByChangedAtAsc(requestId).stream()
                .map(JpaStatusHistoryStore::toDomain).toList();
    }

    private static StatusHistoryEntry toDomain(StatusHistoryEntity e) {
        return new StatusHistoryEntry(e.id, e.requestId,
                e.previousStatus == null ? null : RequestStatus.valueOf(e.previousStatus),
                RequestStatus.valueOf(e.newStatus), e.changedBy, e.changedAt, e.comment);
    }
}
