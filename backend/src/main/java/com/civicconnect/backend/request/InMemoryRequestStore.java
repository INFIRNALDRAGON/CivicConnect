package com.civicconnect.backend.request;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

/** Thread-safe in-memory stand-in for the database. Swap for a repository-backed RequestStore later. */
@Component
public class InMemoryRequestStore implements RequestStore {

    private final Map<Long, ServiceRequest> requests = new HashMap<>();
    private final Map<Long, List<RequestComment>> comments = new HashMap<>();
    private final AtomicLong requestIds = new AtomicLong();
    private final AtomicLong commentIds = new AtomicLong();

    @Override
    public synchronized ServiceRequest insert(ServiceRequest request) {
        ServiceRequest stored = request.copy();
        stored.setId(requestIds.incrementAndGet());
        stored.setVersion(0);
        requests.put(stored.getId(), stored);
        return stored.copy();
    }

    @Override
    public synchronized Optional<ServiceRequest> findById(Long id) {
        return Optional.ofNullable(requests.get(id)).map(ServiceRequest::copy);
    }

    @Override
    public synchronized List<ServiceRequest> findAll() {
        return requests.values().stream()
                .sorted(Comparator.comparing(ServiceRequest::getId))
                .map(ServiceRequest::copy)
                .toList();
    }

    @Override
    public synchronized boolean existsByReference(String referenceNumber) {
        return requests.values().stream().anyMatch(r -> r.getReferenceNumber().equals(referenceNumber));
    }

    @Override
    public synchronized Optional<ServiceRequest> replaceIfVersion(ServiceRequest updated, long expectedVersion) {
        ServiceRequest current = requests.get(updated.getId());
        if (current == null || current.getVersion() != expectedVersion) {
            return Optional.empty();
        }
        ServiceRequest stored = updated.copy();
        stored.setVersion(expectedVersion + 1);
        requests.put(stored.getId(), stored);
        return Optional.of(stored.copy());
    }

    @Override
    public synchronized void restore(ServiceRequest snapshot) {
        requests.put(snapshot.getId(), snapshot.copy());
    }

    @Override
    public synchronized RequestComment addComment(RequestComment comment) {
        RequestComment stored = new RequestComment(commentIds.incrementAndGet(), comment.requestId(),
                comment.authorId(), comment.text(), comment.createdAt());
        comments.computeIfAbsent(stored.requestId(), k -> new ArrayList<>()).add(stored);
        return stored;
    }

    @Override
    public synchronized List<RequestComment> findComments(Long requestId) {
        return List.copyOf(comments.getOrDefault(requestId, List.of()));
    }
}
