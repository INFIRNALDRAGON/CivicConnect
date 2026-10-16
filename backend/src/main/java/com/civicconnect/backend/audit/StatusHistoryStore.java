package com.civicconnect.backend.audit;

import java.util.List;
import java.util.UUID;

public interface StatusHistoryStore {

    StatusHistoryEntry append(StatusHistoryEntry entry);

    List<StatusHistoryEntry> findByRequest(UUID requestId);
}
