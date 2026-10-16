package com.civicconnect.backend.request;

import java.util.List;
import java.util.Optional;
<<<<<<< Updated upstream

/**
 * What the request module needs from storage, written as a plain interface so the
 * business logic can be built and tested without a database. The PostgreSQL / JPA
 * implementation (database work) replaces {@link InMemoryRequestStore}.
 */
public interface RequestStore {

    /** Assigns an id, sets version 0 and stores the request. Returns the stored copy. 
     *@param request
     *@return
     */
    ServiceRequest insert(ServiceRequest request);

    Optional<ServiceRequest> findById(Long id);

    List<ServiceRequest> findAll();

    boolean existsByReference(String referenceNumber);

    /**
     * Compare-and-set: replaces the stored request only if its version still equals
     * {@code expectedVersion}, then bumps the version. Returns empty when someone else got there first.
     *@param updated
     *@param expectedVersion
     *@return
     */
    Optional<ServiceRequest> replaceIfVersion(ServiceRequest updated, long expectedVersion);

    /** Unconditional restore of an earlier snapshot. Used only to undo a failed status change. 
     *@param snapshot
     */
    void restore(ServiceRequest snapshot);

    RequestComment addComment(RequestComment comment);

    List<RequestComment> findComments(Long requestId);
=======
import java.util.UUID;

public interface RequestStore {

    ServiceRequest insert(ServiceRequest request);

    Optional<ServiceRequest> findById(UUID id);

    List<ServiceRequest> findAll();

    List<ServiceRequest> findByRequester(UUID requesterId);

    Optional<ServiceRequest> replaceIfVersion(ServiceRequest updated, long expectedVersion);

    RequestComment addComment(RequestComment comment);

    List<RequestComment> findComments(UUID requestId);
>>>>>>> Stashed changes
}
