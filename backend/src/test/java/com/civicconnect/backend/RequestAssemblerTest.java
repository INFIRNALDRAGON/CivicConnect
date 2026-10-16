package com.civicconnect.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;
import com.civicconnect.backend.status.TransitionCommand;

class RequestAssemblerTest {

    private final TestFixture f = new TestFixture();

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> history(Map<String, Object> dto) {
        return (List<Map<String, Object>>) dto.get("history");
    }

    @Test
    void requesterViewHidesStaffFieldsAndInternalNotes() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        ServiceRequest a = f.assignments.assign(f.sam, r.getId(), null, r.getVersion());
        f.requests.addComment(f.sam, r.getId(), "Internal: crew is busy");
        f.transitions.transition(f.sam, r.getId(),
                new TransitionCommand(RequestStatus.IN_PROGRESS, "Starting tomorrow", null, a.getVersion()));

        Map<String, Object> dto = f.assembler.detail(f.alice, f.requests.requireById(r.getId()));
        assertEquals("IN_PROGRESS", dto.get("status"));
        assertEquals(true, dto.get("assigned"));
        assertFalse(dto.containsKey("assignedStaffName"));
        assertFalse(dto.containsKey("requesterReference"));
        assertFalse(dto.containsKey("allowedTransitions"));
        assertEquals(3, history(dto).size());
        assertTrue(history(dto).stream().noneMatch(h -> "Internal: crew is busy".equals(h.get("comment"))));
        assertTrue(history(dto).stream().noneMatch(h -> h.containsKey("changedByName")));
        assertEquals("Starting tomorrow", history(dto).get(2).get("comment"));
    }

    @Test
    void staffViewMergesNotesIntoTheTimelineWithNames() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        f.clock.advance(Duration.ofMinutes(5));
        ServiceRequest a = f.assignments.assign(f.sam, r.getId(), null, r.getVersion());
        f.clock.advance(Duration.ofMinutes(5));
        f.requests.addComment(f.sam, r.getId(), "Crew is busy");

        Map<String, Object> dto = f.assembler.detail(f.sam, f.requests.requireById(r.getId()));
        assertEquals("Sam Staff", dto.get("assignedStaffName"));
        assertEquals(List.of("IN_PROGRESS"), dto.get("allowedTransitions"));
        assertTrue(((String) dto.get("requesterReference")).startsWith("R-"));
        List<Map<String, Object>> h = history(dto);
        assertEquals(3, h.size());
        assertNull(h.get(0).get("previousStatus"));
        assertTrue(((String) h.get(0).get("changedByName")).startsWith("Requester R-"));
        assertEquals("Sam Staff", h.get(1).get("changedByName"));
        assertEquals("ASSIGNED", h.get(2).get("previousStatus"));
        assertEquals("ASSIGNED", h.get(2).get("newStatus"));
        assertEquals("Crew is busy", h.get(2).get("comment"));
        assertEquals(a.getVersion(), ((Number) dto.get("version")).longValue());
    }

    @Test
    void submittedRequestOffersAcceptAndReject() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        Map<String, Object> dto = f.assembler.detail(f.sam, r);
        assertEquals(List.of("ASSIGNED", "REJECTED"), dto.get("allowedTransitions"));
        f.assignments.assign(f.sam, r.getId(), null, r.getVersion());
        Map<String, Object> other = f.assembler.detail(f.sue, f.requests.requireById(r.getId()));
        assertEquals(List.of(), other.get("allowedTransitions"));
    }

    @Test
    void overdueFlagUsesTheCategoryThreshold() {
        ServiceRequest it = f.submit(f.alice, TestFixture.IT);
        f.clock.advance(Duration.ofHours(25));
        Map<String, Object> dto = f.assembler.summaries(f.mia, List.of(it)).get(0);
        assertEquals(true, dto.get("overdue"));
        assertNull(dto.get("resolvedAt"));
    }
}
