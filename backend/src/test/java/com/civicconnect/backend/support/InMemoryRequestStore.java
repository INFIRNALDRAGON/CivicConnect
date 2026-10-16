package com.civicconnect.backend.support;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.civicconnect.backend.request.RequestComment;
import com.civicconnect.backend.request.RequestStore;
import com.civicconnect.backend.request.ServiceRequest;

public class InMemoryRequestStore implements RequestStore {

    private final Map<UUID, ServiceRequest> requests = new HashMap<>();
    private final Map<UUID, List<RequestComment>> comments = new HashMap<>();
    private final AtomicLong requestIds = new AtomicLong(1000);
    private final AtomicLong commentIds = new AtomicLong(5000);
    private final AtomicLong references = new AtomicLong();

    @Override
    public synchronized ServiceRequest insert(ServiceRequest request) {
        ServiceRequest stored = request.copy();
        stored.setId(Ids.of(requestIds.incrementAndGet()));
        stored.setReferenceNumber(String.format("CC-%08d", references.incrementAndGet()));
        stored.setVersion(0);
        requests.put(stored.getId(), stored);
        return stored.copy();
    }

    @Override
    public synchronized Optional<ServiceRequest> findById(UUID id) {
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
    public synchronized List<ServiceRequest> findByRequester(UUID requesterId) {
        return findAll().stream().filter(r -> r.getRequesterId().equals(requesterId)).toList();
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
    public synchronized RequestComment addComment(RequestComment comment) {
        RequestComment stored = new RequestComment(Ids.of(commentIds.incrementAndGet()), comment.requestId(),
                comment.authorId(), comment.text(), comment.createdAt());
        comments.computeIfAbsent(stored.requestId(), k -> new ArrayList<>()).add(stored);
        return stored;
    }

    @Override
    public synchronized List<RequestComment> findComments(UUID requestId) {
        return List.copyOf(comments.getOrDefault(requestId, List.of()));
    }
}
