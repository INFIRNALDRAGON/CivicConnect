package com.civicconnect.backend.request;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RequestCommentRepository extends JpaRepository<RequestCommentEntity, UUID> {

    List<RequestCommentEntity> findByRequestIdOrderByCreatedAtAsc(UUID requestId);
}
