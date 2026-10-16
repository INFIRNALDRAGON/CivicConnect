package com.civicconnect.backend.request;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequestStore {

    ServiceRequest insert(ServiceRequest request);

    Optional<ServiceRequest> findById(UUID id);

    List<ServiceRequest> findAll();

    List<ServiceRequest> findByRequester(UUID requesterId);

    Optional<ServiceRequest> replaceIfVersion(ServiceRequest updated, long expectedVersion);

    RequestComment addComment(RequestComment comment);

    List<RequestComment> findComments(UUID requestId);
}
