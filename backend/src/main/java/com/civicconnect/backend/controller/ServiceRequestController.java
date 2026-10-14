package com.civicconnect.backend.controller;

import com.civicconnect.backend.dto.CreateRequestDto;
import com.civicconnect.backend.dto.HistoryEntryDto;
import com.civicconnect.backend.dto.RequestResponseDto;
import com.civicconnect.backend.model.RequestStatus;
import com.civicconnect.backend.service.ServiceRequestService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Controllers only translate HTTP to service calls; all rules and data access live below. */
@RestController
@RequestMapping("/api/requests")
public class ServiceRequestController {

    private final ServiceRequestService service;

    public ServiceRequestController(ServiceRequestService service) {
        this.service = service;
    }

    /** FR-001: submit a new request. Returns 201 with a Location header. */
    @PostMapping
    public ResponseEntity<RequestResponseDto> create(@Valid @RequestBody CreateRequestDto body) {
        RequestResponseDto created = service.create(body);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** FR-003 / FR-004 / FR-007: list requests, all filters optional. */
    @GetMapping
    public List<RequestResponseDto> search(
            @RequestParam(name = "requesterId", required = false) UUID requesterId,
            @RequestParam(name = "status", required = false) RequestStatus status,
            @RequestParam(name = "categoryId", required = false) UUID categoryId) {
        return service.search(requesterId, status, categoryId);
    }

    /** FR-003 / FR-008: one request in full. */
    @GetMapping("/{id}")
    public RequestResponseDto get(@PathVariable("id") UUID id) {
        return service.get(id);
    }

    /** NFR-006: the audit trail of one request, oldest first. */
    @GetMapping("/{id}/history")
    public List<HistoryEntryDto> history(@PathVariable("id") UUID id) {
        return service.history(id);
    }
}
