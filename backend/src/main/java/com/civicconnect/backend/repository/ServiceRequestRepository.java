package com.civicconnect.backend.repository;

import com.civicconnect.backend.model.ServiceRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** JpaSpecificationExecutor gives us optional filters (FR-007) without null-parameter query problems. */
public interface ServiceRequestRepository
        extends JpaRepository<ServiceRequest, UUID>, JpaSpecificationExecutor<ServiceRequest> {
}
