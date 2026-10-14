package com.civicconnect.backend.repository;

import com.civicconnect.backend.model.StatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {
    List<StatusHistory> findByRequestIdOrderByChangedAtAsc(UUID requestId);
}
