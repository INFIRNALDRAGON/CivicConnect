package com.civicconnect.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.civicconnect.backend.dto.CreateRequestDto;
import com.civicconnect.backend.exception.ValidationException;
import com.civicconnect.backend.reporting.ReportingService;
import com.civicconnect.backend.request.RequestComment;
import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;
import com.civicconnect.backend.status.TransitionCommand;

class BoundaryAndAcceptanceTest {

    private final TestFixture f = new TestFixture();

    private ServiceRequest create(String description) {
        return f.requests.create(f.alice, new CreateRequestDto(TestFixture.POTHOLES, description, null));
    }

    @Test
    void nineCharactersAreRejectedAndTenAreAccepted() {
        assertThrows(ValidationException.class, () -> create("123456789"));
        assertEquals("1234567890", create("1234567890").getDescription());
    }

    @Test
    void surroundingSpacesDoNotCountTowardsTheMinimum() {
        assertThrows(ValidationException.class, () -> create("   123456789   "));
        assertThrows(ValidationException.class, () -> create("          "));
        assertEquals("1234567890", create("   1234567890   ").getDescription());
    }

    @Test
    void theUpperLimitIsExact() {
        assertEquals(2000, create("x".repeat(2000)).getDescription().length());
        assertThrows(ValidationException.class, () -> create("x".repeat(2001)));
    }

    @Test
    void exactlyOnTheThresholdIsNotOverdue() {
        f.submit(f.alice, TestFixture.POTHOLES);
        f.clock.advance(Duration.ofHours(72));
        assertTrue(f.reporting.overdue(f.mia, null).isEmpty());
    }

    @Test
    void oneMinuteBeforeTheThresholdIsNotOverdue() {
        f.submit(f.alice, TestFixture.POTHOLES);
        f.clock.advance(Duration.ofHours(71).plusMinutes(59));
        assertTrue(f.reporting.overdue(f.mia, null).isEmpty());
    }

    @Test
    void oneMinutePastTheThresholdIsOverdue() {
        f.submit(f.alice, TestFixture.POTHOLES);
        f.clock.advance(Duration.ofHours(72).plusMinutes(1));
        assertEquals(1, f.reporting.overdue(f.mia, null).size());
    }

    @Test
    void theThresholdBelongsToTheCategory() {
        f.submit(f.alice, TestFixture.IT);
        f.clock.advance(Duration.ofHours(24));
        assertTrue(f.reporting.overdue(f.mia, null).isEmpty());
        f.clock.advance(Duration.ofMinutes(1));
        assertEquals(1, f.reporting.overdue(f.mia, null).size());
    }

    @Test
    void requesterSeesStageAndLastUpdateOnOwnRequest() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        f.clock.advance(Duration.ofHours(2));
        ServiceRequest assigned = f.assignments.assign(f.sam, r.getId(), null, r.getVersion());

        Map<String, Object> view = f.assembler.detail(f.alice, f.requests.getForViewer(f.alice, assigned.getId()));
        assertEquals("ASSIGNED", view.get("status"));
        assertEquals(f.clock.instant().toString(), view.get("updatedAt"));
        assertFalse(view.containsKey("assignedStaffName"));
        assertFalse(view.containsKey("requesterReference"));
    }

    @Test
    void staffDetailShowsDescriptionHistoryAndNotesInOrder() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        f.clock.advance(Duration.ofMinutes(5));
        ServiceRequest assigned = f.assignments.assign(f.sam, r.getId(), null, r.getVersion());
        f.clock.advance(Duration.ofMinutes(5));
        f.requests.addComment(f.sam, r.getId(), "Called the depot");

        Map<String, Object> view = f.assembler.detail(f.sam, f.requests.requireById(assigned.getId()));
        assertEquals("Large pothole outside the clinic", view.get("description"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) view.get("history");
        assertEquals(3, history.size());
        assertEquals("SUBMITTED", history.get(0).get("newStatus"));
        assertEquals("ASSIGNED", history.get(1).get("newStatus"));
        assertEquals("Called the depot", history.get(2).get("comment"));
        assertEquals("Sam Staff", history.get(2).get("changedByName"));
    }

    @Test
    void aStoredNoteCarriesItsAuthorAndTime() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        f.clock.advance(Duration.ofMinutes(30));
        RequestComment saved = f.requests.addComment(f.sam, r.getId(), "  Crew booked for Friday  ");

        assertNotNull(saved.id());
        assertEquals(f.sam.id(), saved.authorId());
        assertEquals("Crew booked for Friday", saved.text());
        assertEquals(f.clock.instant(), saved.createdAt());
        assertEquals(List.of("Crew booked for Friday"),
                f.requests.commentsFor(f.sam, r.getId()).stream().map(RequestComment::text).toList());
    }

    @Test
    void requestersNeverReceiveInternalNotes() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        f.requests.addComment(f.sam, r.getId(), "Internal only");

        Map<String, Object> view = f.assembler.detail(f.alice, f.requests.requireById(r.getId()));
        assertFalse(view.toString().contains("Internal only"));
    }

    @Test
    void activityCanBeNarrowedToOneCategory() {
        ServiceRequest a = f.submit(f.alice, TestFixture.POTHOLES);
        f.submit(f.alice, TestFixture.POTHOLES);
        f.submit(f.bob, TestFixture.IT);
        f.transitions.transition(f.sam, a.getId(),
                new TransitionCommand(RequestStatus.REJECTED, "Duplicate", null, a.getVersion()));

        Map<RequestStatus, Long> roads = f.reporting.countsByStatus(f.mia, null, null, TestFixture.POTHOLES);
        assertEquals(1L, (long) roads.get(RequestStatus.SUBMITTED));
        assertEquals(1L, (long) roads.get(RequestStatus.REJECTED));
        Map<RequestStatus, Long> it = f.reporting.countsByStatus(f.mia, null, null, TestFixture.IT);
        assertEquals(1L, (long) it.get(RequestStatus.SUBMITTED));
        assertEquals(0L, (long) it.get(RequestStatus.REJECTED));

        ReportingService.Summary summary = f.reporting.summary(f.mia, null, null, TestFixture.IT);
        assertEquals(1, summary.total());
        assertEquals(1, summary.byCategory().size());
        assertEquals("IT support", summary.byCategory().get(0).categoryName());
    }
}
