package com.civicconnect.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
<<<<<<< Updated upstream
=======
import static org.junit.jupiter.api.Assertions.assertNotNull;
>>>>>>> Stashed changes
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.civicconnect.backend.exception.ForbiddenException;
import com.civicconnect.backend.exception.NotFoundException;
import com.civicconnect.backend.notification.Notification;
import com.civicconnect.backend.reporting.ReportingService;
import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;
import com.civicconnect.backend.status.TransitionCommand;

class NotificationAndReportingTest {

    private final TestFixture f = new TestFixture();

    private void resolve(ServiceRequest r) {
        ServiceRequest a = f.assignments.assign(f.sam, r.getId(), null, r.getVersion());
        ServiceRequest p = f.transitions.transition(f.sam, r.getId(),
                new TransitionCommand(RequestStatus.IN_PROGRESS, null, null, a.getVersion()));
        f.transitions.transition(f.sam, r.getId(),
                new TransitionCommand(RequestStatus.RESOLVED, "Patched", null, p.getVersion()));
    }

<<<<<<< Updated upstream
    /** FR-005: a status-specific message reaches the requester, without naming the staff member. */
=======
>>>>>>> Stashed changes
    @Test
    void requesterIsToldAboutEachChange() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        resolve(r);

        List<Notification> mine = f.notifications.listFor(f.alice);
<<<<<<< Updated upstream
        assertEquals(3, mine.size());
=======
        assertEquals(4, mine.size());
        assertTrue(mine.stream().anyMatch(n -> n.message().contains("received")));
>>>>>>> Stashed changes
        assertTrue(mine.stream().anyMatch(n -> n.message().contains("accepted")));
        assertTrue(mine.stream().anyMatch(n -> n.message().contains("Resolution: Patched")));
        assertTrue(mine.stream().noneMatch(n -> n.message().contains(String.valueOf(f.sam.id()))));
        assertTrue(f.notifications.listFor(f.bob).isEmpty());
    }

    @Test
    void notificationsCanBeMarkedReadOnlyByTheirOwner() {
        resolve(f.submit(f.alice, TestFixture.POTHOLES));
        Notification n = f.notifications.listFor(f.alice).get(0);
        assertThrows(NotFoundException.class, () -> f.notifications.markRead(f.bob, n.id()));
        assertFalse(f.notifications.markRead(f.alice, n.id()).readAt() == null);
        assertThrows(ForbiddenException.class, () -> f.notifications.listFor(f.sam));
    }

<<<<<<< Updated upstream
    /** FR-013: counts by status; default range is the last 7 days. */
=======
>>>>>>> Stashed changes
    @Test
    void statusCountsUseTheDefaultSevenDayRange() {
        ServiceRequest old = f.submit(f.alice, TestFixture.POTHOLES);
        f.clock.advance(Duration.ofDays(10));
        f.submit(f.bob, TestFixture.POTHOLES);
        resolve(f.submit(f.alice, TestFixture.POTHOLES));

        Map<RequestStatus, Long> counts = f.reporting.countsByStatus(f.mia, null, null, null);
        assertEquals(1L, (long) counts.get(RequestStatus.SUBMITTED));
        assertEquals(1L, (long) counts.get(RequestStatus.RESOLVED));
<<<<<<< Updated upstream
=======
        assertEquals(0L, (long) counts.get(RequestStatus.REJECTED));
>>>>>>> Stashed changes

        Map<RequestStatus, Long> wide = f.reporting.countsByStatus(f.mia,
                old.getCreatedAt().minus(Duration.ofDays(1)), f.clock.instant().plusSeconds(1), null);
        assertEquals(2L, (long) wide.get(RequestStatus.SUBMITTED));
    }

<<<<<<< Updated upstream
    /** FR-014: open requests past the threshold are overdue; resolved ones are not. */
    @Test
    void overdueUsesTheThresholdAndIgnoresResolved() {
        ServiceRequest stale = f.submit(f.alice, TestFixture.POTHOLES);
        ServiceRequest done = f.submit(f.bob, TestFixture.POTHOLES);
        resolve(done);
        f.clock.advance(Duration.ofHours(73));
        f.submit(f.alice, TestFixture.IT);   // brand new

        List<ReportingService.OverdueItem> overdue = f.reporting.overdue(f.mia, null);
        assertEquals(1, overdue.size());
        assertEquals(stale.getReferenceNumber(), overdue.get(0).referenceNumber());
        assertTrue(overdue.get(0).ageHours() >= 73);
        assertTrue(f.reporting.overdue(f.mia, TestFixture.IT).isEmpty());
    }

    /** FR-016: average time to resolution per category. */
=======
    @Test
    void overdueUsesTheCategoryThresholdAndIgnoresClosedOutRequests() {
        ServiceRequest stale = f.submit(f.alice, TestFixture.POTHOLES);
        ServiceRequest done = f.submit(f.bob, TestFixture.POTHOLES);
        resolve(done);
        ServiceRequest rejected = f.submit(f.bob, TestFixture.POTHOLES);
        f.transitions.transition(f.sam, rejected.getId(),
                new TransitionCommand(RequestStatus.REJECTED, "Duplicate", null, rejected.getVersion()));
        ServiceRequest quickIt = f.submit(f.alice, TestFixture.IT);
        f.clock.advance(Duration.ofHours(73));
        f.submit(f.alice, TestFixture.POTHOLES);

        List<ReportingService.OverdueItem> overdue = f.reporting.overdue(f.mia, null);
        assertEquals(List.of(stale.getReferenceNumber(), quickIt.getReferenceNumber()),
                overdue.stream().map(ReportingService.OverdueItem::referenceNumber).toList());
        assertTrue(overdue.get(0).ageHours() >= 73);
        assertEquals(1, f.reporting.overdue(f.mia, TestFixture.IT).size());
    }

    @Test
    void shorterCategoryThresholdMakesItOverdueSooner() {
        f.submit(f.alice, TestFixture.IT);
        f.submit(f.alice, TestFixture.POTHOLES);
        f.clock.advance(Duration.ofHours(25));
        List<ReportingService.OverdueItem> overdue = f.reporting.overdue(f.mia, null);
        assertEquals(1, overdue.size());
        assertEquals(TestFixture.IT, overdue.get(0).categoryId());
    }

>>>>>>> Stashed changes
    @Test
    void averageResolutionTimeIsPerCategory() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        f.clock.advance(Duration.ofHours(5));
        resolve(r);

        List<ReportingService.ResolutionTime> times = f.reporting.averageResolutionTimes(f.mia, null, null);
        assertEquals(1, times.size());
        assertEquals(TestFixture.POTHOLES, times.get(0).categoryId());
        assertEquals(5.0, times.get(0).averageHours());
        assertEquals(1, times.get(0).sampleSize());
    }

    @Test
<<<<<<< Updated upstream
    void reportingIsManagerOnly() {
        assertThrows(ForbiddenException.class, () -> f.reporting.countsByStatus(f.sam, null, null, null));
        assertThrows(ForbiddenException.class, () -> f.reporting.overdue(f.alice, null));
=======
    void summaryMatchesTheDashboardContract() {
        ServiceRequest a = f.submit(f.alice, TestFixture.POTHOLES);
        f.submit(f.bob, TestFixture.IT);
        f.clock.advance(Duration.ofHours(5));
        resolve(a);
        f.clock.advance(Duration.ofHours(30));

        ReportingService.Summary s = f.reporting.summary(f.mia, null, null, null);
        assertEquals(2, s.total());
        assertEquals(1L, (long) s.totals().get(RequestStatus.RESOLVED));
        assertEquals(1L, (long) s.totals().get(RequestStatus.SUBMITTED));
        assertEquals(1, s.openCount());
        assertEquals(1, s.overdueCount());
        assertEquals(2, s.byCategory().size());
        var roads = s.byCategory().stream().filter(c -> c.categoryId().equals(TestFixture.POTHOLES)).findFirst().orElseThrow();
        assertEquals(5.0, roads.avgResolutionHours());
        assertEquals(1, roads.resolved());
        assertEquals(2, s.requests().size());

        ReportingService.Summary onlyIt = f.reporting.summary(f.mia, null, null, TestFixture.IT);
        assertEquals(1, onlyIt.total());
    }

    @Test
    void reportingIsManagerOnly() {
        assertThrows(ForbiddenException.class, () -> f.reporting.countsByStatus(f.sam, null, null, null));
        assertThrows(ForbiddenException.class, () -> f.reporting.overdue(f.alice, null));
        assertThrows(ForbiddenException.class, () -> f.reporting.summary(f.sam, null, null, null));
        assertNotNull(f.reporting.summary(f.mia, null, null, null));
>>>>>>> Stashed changes
    }
}
