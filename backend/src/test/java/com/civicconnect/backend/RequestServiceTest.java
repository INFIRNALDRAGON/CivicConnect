package com.civicconnect.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
<<<<<<< Updated upstream
import java.util.HashSet;
import java.util.List;
import java.util.Set;
=======
import java.util.List;
>>>>>>> Stashed changes

import org.junit.jupiter.api.Test;

import com.civicconnect.backend.dto.CreateRequestDto;
import com.civicconnect.backend.exception.ForbiddenException;
import com.civicconnect.backend.exception.ValidationException;
<<<<<<< Updated upstream
import com.civicconnect.backend.request.ReferenceNumberGenerator;
=======
>>>>>>> Stashed changes
import com.civicconnect.backend.request.RequestFilter;
import com.civicconnect.backend.request.RequestSort;
import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;

class RequestServiceTest {

    private final TestFixture f = new TestFixture();

    @Test
<<<<<<< Updated upstream
    void submitStoresRequestWithReference() {
=======
    void submitStoresRequestWithReferenceAndFirstHistoryRow() {
>>>>>>> Stashed changes
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        assertEquals(RequestStatus.SUBMITTED, r.getStatus());
        assertTrue(r.getReferenceNumber().startsWith("CC-"));
        assertEquals(f.alice.id(), r.getRequesterId());
<<<<<<< Updated upstream
    }

    /** FR-002: no category blocks submission. FR-001: description of at least 10 characters. */
=======
        var history = f.audit.historyFor(r.getId());
        assertEquals(1, history.size());
        assertEquals(RequestStatus.SUBMITTED, history.get(0).newStatus());
        assertEquals(f.alice.id(), history.get(0).changedBy());
    }

>>>>>>> Stashed changes
    @Test
    void submissionIsValidated() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                f.requests.create(f.alice, new CreateRequestDto(null, "short", null)));
        assertEquals(2, ex.getErrors().size());
        assertThrows(ValidationException.class, () ->
<<<<<<< Updated upstream
                f.requests.create(f.alice, new CreateRequestDto(3L, "A long enough description", null)));   // inactive
        assertThrows(ValidationException.class, () ->
                f.requests.create(f.alice, new CreateRequestDto(99L, "A long enough description", null)));  // unknown
=======
                f.requests.create(f.alice, new CreateRequestDto(TestFixture.RETIRED, "A long enough description", null)));
        assertThrows(ValidationException.class, () ->
                f.requests.create(f.alice, new CreateRequestDto(TestFixture.UNKNOWN, "A long enough description", null)));
        assertThrows(ValidationException.class, () ->
                f.requests.create(f.alice, new CreateRequestDto(TestFixture.IT, "x".repeat(2001), null)));
>>>>>>> Stashed changes
    }

    @Test
    void onlyRequestersSubmit() {
        assertThrows(ForbiddenException.class, () ->
                f.requests.create(f.sam, new CreateRequestDto(TestFixture.POTHOLES, "A long enough description", null)));
        assertThrows(ForbiddenException.class, () ->
                f.requests.create(null, new CreateRequestDto(TestFixture.POTHOLES, "A long enough description", null)));
    }

<<<<<<< Updated upstream
    /** NFR-002: 100,000 generated references, zero duplicates. */
    @Test
    void referencesAreUnique() {
        ReferenceNumberGenerator gen = new ReferenceNumberGenerator();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 100_000; i++) {
            assertTrue(seen.add(gen.next()), "duplicate reference");
=======
    @Test
    void referencesAreUnique() {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 500; i++) {
            assertTrue(seen.add(f.submit(f.alice, TestFixture.POTHOLES).getReferenceNumber()));
>>>>>>> Stashed changes
        }
    }

    @Test
    void requesterSeesOnlyOwnRequests() {
        ServiceRequest mine = f.submit(f.alice, TestFixture.POTHOLES);
        ServiceRequest theirs = f.submit(f.bob, TestFixture.POTHOLES);

        assertEquals(List.of(mine.getId()), f.requests.listForRequester(f.alice).stream().map(ServiceRequest::getId).toList());
        assertThrows(ForbiddenException.class, () -> f.requests.getForViewer(f.alice, theirs.getId()));
        assertEquals(theirs.getId(), f.requests.getForViewer(f.bob, theirs.getId()).getId());
    }

<<<<<<< Updated upstream
    /** FR-006: staff see only their categories; managers see everything. */
=======
>>>>>>> Stashed changes
    @Test
    void staffQueueIsScopedToCategories() {
        f.submit(f.alice, TestFixture.POTHOLES);
        f.submit(f.alice, TestFixture.IT);

        assertEquals(1, f.requests.listForStaff(f.sam, null).size());
        assertEquals(1, f.requests.listForStaff(f.itStaff, null).size());
        assertEquals(2, f.requests.listForStaff(f.mia, null).size());
        assertThrows(ForbiddenException.class, () -> f.requests.listForStaff(f.alice, null));
        ServiceRequest it = f.requests.listForStaff(f.itStaff, null).get(0);
        assertThrows(ForbiddenException.class, () -> f.requests.getForViewer(f.sam, it.getId()));
    }

<<<<<<< Updated upstream
    /** FR-007: category filter combined with age sort, oldest first. */
=======
>>>>>>> Stashed changes
    @Test
    void queueFiltersAndSortsByAge() {
        ServiceRequest first = f.submit(f.alice, TestFixture.POTHOLES);
        f.clock.advance(Duration.ofHours(1));
        ServiceRequest second = f.submit(f.bob, TestFixture.POTHOLES);
        f.clock.advance(Duration.ofHours(1));
        f.submit(f.alice, TestFixture.IT);

        List<ServiceRequest> oldest = f.requests.listForStaff(f.mia,
                new RequestFilter(TestFixture.POTHOLES, null, null, RequestSort.OLDEST_FIRST));
        assertEquals(List.of(first.getId(), second.getId()), oldest.stream().map(ServiceRequest::getId).toList());

        List<ServiceRequest> newest = f.requests.listForStaff(f.mia,
                new RequestFilter(TestFixture.POTHOLES, null, null, RequestSort.NEWEST_FIRST));
        assertEquals(List.of(second.getId(), first.getId()), newest.stream().map(ServiceRequest::getId).toList());

        assertTrue(f.requests.listForStaff(f.mia,
                new RequestFilter(null, RequestStatus.CLOSED, null, null)).isEmpty());
        assertFalse(f.requests.listForStaff(f.mia,
                new RequestFilter(null, null, first.getReferenceNumber().toLowerCase(), null)).isEmpty());
    }
}
