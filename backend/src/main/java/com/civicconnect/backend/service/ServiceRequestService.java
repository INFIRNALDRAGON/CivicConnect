package com.civicconnect.backend.service;

import com.civicconnect.backend.dto.CreateRequestDto;
import com.civicconnect.backend.dto.HistoryEntryDto;
import com.civicconnect.backend.dto.RequestResponseDto;
import com.civicconnect.backend.exception.ResourceNotFoundException;
import com.civicconnect.backend.model.Category;
import com.civicconnect.backend.model.Priority;
import com.civicconnect.backend.model.RequestStatus;
import com.civicconnect.backend.model.ServiceRequest;
import com.civicconnect.backend.model.StatusHistory;
import com.civicconnect.backend.model.User;
import com.civicconnect.backend.repository.CategoryRepository;
import com.civicconnect.backend.repository.ServiceRequestRepository;
import com.civicconnect.backend.repository.StatusHistoryRepository;
import com.civicconnect.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceRequestService {

    private final ServiceRequestRepository requests;
    private final StatusHistoryRepository history;
    private final CategoryRepository categories;
    private final UserRepository users;

    @PersistenceContext
    private EntityManager entityManager;

    public ServiceRequestService(ServiceRequestRepository requests,
                                 StatusHistoryRepository history,
                                 CategoryRepository categories,
                                 UserRepository users) {
        this.requests = requests;
        this.history = history;
        this.categories = categories;
        this.users = users;
    }

    /**
     * FR-001 / FR-002. The request row and its first audit row are written in ONE
     * transaction, so a request can never exist without its history (NFR-006).
     */
    @Transactional
    public RequestResponseDto create(CreateRequestDto dto) {
        User requester = users.findById(dto.requesterId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + dto.requesterId()));
        Category category = categories.findByIdAndActiveTrue(dto.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Active category not found: " + dto.categoryId()));

        Priority priority = dto.priority() == null ? Priority.MEDIUM : dto.priority();
        ServiceRequest request = new ServiceRequest(category, dto.description().trim(), priority, requester);

        requests.saveAndFlush(request);
        history.save(new StatusHistory(request, null, RequestStatus.SUBMITTED, requester, null));

        // reference_number and updated_at are filled in by the database; load them back.
        entityManager.refresh(request);
        return RequestResponseDto.from(request);
    }

    @Transactional(readOnly = true)
    public RequestResponseDto get(UUID id) {
        return RequestResponseDto.from(find(id));
    }

    /** FR-003 / FR-004 / FR-007: every filter is optional. */
    @Transactional(readOnly = true)
    public List<RequestResponseDto> search(UUID requesterId, RequestStatus status, UUID categoryId) {
        Specification<ServiceRequest> spec = (root, query, cb) -> cb.conjunction();
        if (requesterId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("requester").get("id"), requesterId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        return requests.findAll(spec, Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream().map(RequestResponseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<HistoryEntryDto> history(UUID requestId) {
        find(requestId); // 404 if the request does not exist
        return history.findByRequestIdOrderByChangedAtAsc(requestId)
                .stream().map(HistoryEntryDto::from).toList();
    }

    private ServiceRequest find(UUID id) {
        return requests.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + id));
    }
}
