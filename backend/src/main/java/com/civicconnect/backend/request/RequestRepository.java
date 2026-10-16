package com.civicconnect.backend.request;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RequestRepository extends JpaRepository<RequestEntity, UUID> {

    List<RequestEntity> findByRequesterId(UUID requesterId);
}
