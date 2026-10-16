package com.civicconnect.backend.request;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RequestTimesRepository extends JpaRepository<RequestTimesEntity, UUID> {

    List<RequestTimesEntity> findByIdIn(Collection<UUID> ids);
}
