package com.civicconnect.backend.audit;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface StatusHistoryRepository extends JpaRepository<StatusHistoryEntity, UUID> {

    List<StatusHistoryEntity> findByRequestIdOrderByChangedAtAsc(UUID requestId);
}
