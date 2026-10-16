package com.civicconnect.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
<<<<<<< Updated upstream
=======
import static org.junit.jupiter.api.Assertions.assertNull;
>>>>>>> Stashed changes
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.civicconnect.backend.audit.StatusHistoryEntry;
import com.civicconnect.backend.exception.ConflictException;
import com.civicconnect.backend.exception.ForbiddenException;
import com.civicconnect.backend.exception.InvalidStatusTransitionException;
import com.civicconnect.backend.exception.ValidationException;
import com.civicconnect.backend.request.RequestStatus;
import com.civicconnect.backend.request.ServiceRequest;
import com.civicconnect.backend.status.TransitionCommand;

class StatusTransitionServiceTest {

    private final TestFixture f = new TestFixture();

    private ServiceRequest assigned() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        return f.assignments.assign(f.sam, r.getId(), null, r.getVersion());
    }

    private ServiceRequest inProgress() {
        ServiceRequest a = assigned();
        return f.transitions.transition(f.sam, a.getId(),
                new TransitionCommand(RequestStatus.IN_PROGRESS, null, null, a.getVersion()));
    }

<<<<<<< Updated upstream
    /** FR-010 acceptance criterion: Submitted straight to Resolved is rejected. */
=======
>>>>>>> Stashed changes
    @Test
    void cannotSkipStages() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        InvalidStatusTransitionException ex = assertThrows(InvalidStatusTransitionException.class, () ->
                f.transitions.transition(f.sam, r.getId(),
                        new TransitionCommand(RequestStatus.RESOLVED, "done", null, r.getVersion())));
        assertEquals(RequestStatus.SUBMITTED, ex.getFrom());
        assertEquals(RequestStatus.SUBMITTED, f.requests.requireById(r.getId()).getStatus());
<<<<<<< Updated upstream
        assertTrue(f.audit.historyFor(r.getId()).isEmpty());
=======
        assertEquals(1, f.audit.historyFor(r.getId()).size());
>>>>>>> Stashed changes
    }

    @Test
    void fullLifecycleIsAuditedInOrder() {
        ServiceRequest p = inProgress();
        ServiceRequest resolved = f.transitions.transition(f.sam, p.getId(),
                new TransitionCommand(RequestStatus.RESOLVED, "Filled the hole", null, p.getVersion()));
        assertNotNull(resolved.getResolvedAt());
        ServiceRequest closed = f.transitions.transition(f.mia, p.getId(),
                new TransitionCommand(RequestStatus.CLOSED, null, null, resolved.getVersion()));
        assertEquals(RequestStatus.CLOSED, closed.getStatus());

        List<StatusHistoryEntry> history = f.audit.historyFor(p.getId());
<<<<<<< Updated upstream
        assertEquals(List.of(RequestStatus.ASSIGNED, RequestStatus.IN_PROGRESS, RequestStatus.RESOLVED, RequestStatus.CLOSED),
                history.stream().map(StatusHistoryEntry::newStatus).toList());
        // gap-free: each row starts where the previous one ended (NFR-006)
        for (int i = 1; i < history.size(); i++) {
            assertEquals(history.get(i - 1).newStatus(), history.get(i).previousStatus());
        }
        assertEquals(RequestStatus.SUBMITTED, history.get(0).previousStatus());
=======
        assertEquals(List.of(RequestStatus.SUBMITTED, RequestStatus.ASSIGNED, RequestStatus.IN_PROGRESS,
                        RequestStatus.RESOLVED, RequestStatus.CLOSED),
                history.stream().map(StatusHistoryEntry::newStatus).toList());
        assertNull(history.get(0).previousStatus());
        for (int i = 1; i < history.size(); i++) {
            assertEquals(history.get(i - 1).newStatus(), history.get(i).previousStatus());
        }
>>>>>>> Stashed changes
    }

    @Test
    void closedIsTerminal() {
        ServiceRequest p = inProgress();
        ServiceRequest resolved = f.transitions.transition(f.sam, p.getId(),
                new TransitionCommand(RequestStatus.RESOLVED, "ok", null, p.getVersion()));
        ServiceRequest closed = f.transitions.transition(f.sam, p.getId(),
                new TransitionCommand(RequestStatus.CLOSED, null, null, resolved.getVersion()));
        assertThrows(InvalidStatusTransitionException.class, () ->
                f.transitions.transition(f.mia, p.getId(),
                        new TransitionCommand(RequestStatus.IN_PROGRESS, null, null, closed.getVersion())));
    }

    @Test
    void resolvingNeedsANote() {
        ServiceRequest p = inProgress();
        assertThrows(ValidationException.class, () -> f.transitions.transition(f.sam, p.getId(),
                new TransitionCommand(RequestStatus.RESOLVED, "  ", null, p.getVersion())));
    }

    @Test
<<<<<<< Updated upstream
    void onlyTheOwnerOrAManagerStartsWork() {
        ServiceRequest a = assigned();   // owned by sam
=======
    void rejectingNeedsAReasonAndIsFinal() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        assertThrows(ValidationException.class, () -> f.transitions.transition(f.sam, r.getId(),
                new TransitionCommand(RequestStatus.REJECTED, " ", null, r.getVersion())));

        ServiceRequest rejected = f.transitions.transition(f.sam, r.getId(),
                new TransitionCommand(RequestStatus.REJECTED, "Duplicate of an earlier report", null, r.getVersion()));
        assertEquals(RequestStatus.REJECTED, rejected.getStatus());
        assertNull(rejected.getAssignedStaffId());

        assertThrows(InvalidStatusTransitionException.class, () -> f.transitions.transition(f.mia, r.getId(),
                new TransitionCommand(RequestStatus.ASSIGNED, null, f.sam.id(), rejected.getVersion())));
        assertThrows(ConflictException.class, () -> f.requests.addComment(f.sam, r.getId(), "late note"));

        List<StatusHistoryEntry> history = f.audit.historyFor(r.getId());
        assertEquals("Duplicate of an earlier report", history.get(history.size() - 1).comment());
        assertTrue(f.notifications.listFor(f.alice).stream()
                .anyMatch(n -> n.message().contains("Reason: Duplicate of an earlier report")));
    }

    @Test
    void onlySubmittedRequestsCanBeRejected() {
        ServiceRequest a = assigned();
        assertThrows(InvalidStatusTransitionException.class, () -> f.transitions.transition(f.sam, a.getId(),
                new TransitionCommand(RequestStatus.REJECTED, "too late", null, a.getVersion())));
    }

    @Test
    void onlyTheOwnerOrAManagerStartsWork() {
        ServiceRequest a = assigned();
>>>>>>> Stashed changes
        assertThrows(ForbiddenException.class, () -> f.transitions.transition(f.sue, a.getId(),
                new TransitionCommand(RequestStatus.IN_PROGRESS, null, null, a.getVersion())));
    }

    @Test
    void staffOutsideTheirCategoryAreRefused() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        assertThrows(ForbiddenException.class, () -> f.assignments.assign(f.itStaff, r.getId(), null, r.getVersion()));
        assertThrows(ForbiddenException.class, () -> f.transitions.transition(f.alice, r.getId(),
                new TransitionCommand(RequestStatus.ASSIGNED, null, f.alice.id(), r.getVersion())));
    }

<<<<<<< Updated upstream
    /** ASR-01: if the audit write fails, the status must not change. */
    @Test
    void failedAuditWriteLeavesStatusUnchanged() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        f.auditShouldFail = true;
        assertThrows(IllegalStateException.class, () -> f.assignments.assign(f.sam, r.getId(), null, r.getVersion()));
        ServiceRequest after = f.requests.requireById(r.getId());
        assertEquals(RequestStatus.SUBMITTED, after.getStatus());
        assertEquals(null, after.getAssignedStaffId());
        assertEquals(r.getVersion(), after.getVersion());
        assertTrue(f.notifications.listFor(f.alice).isEmpty());
    }

    /** ASR-03: two staff act on the same version at the same moment. Exactly one wins. */
=======
    @Test
    void failedAuditWriteSurfacesAndNobodyIsNotified() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        int before = f.notifications.listFor(f.alice).size();
        f.auditShouldFail = true;
        assertThrows(IllegalStateException.class, () -> f.assignments.assign(f.sam, r.getId(), null, r.getVersion()));
        assertEquals(before, f.notifications.listFor(f.alice).size());
    }

    @Test
    void staleVersionIsAConflict() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        f.assignments.assign(f.sam, r.getId(), null, r.getVersion());
        assertThrows(ConflictException.class, () -> f.transitions.transition(f.mia, r.getId(),
                new TransitionCommand(RequestStatus.IN_PROGRESS, null, null, r.getVersion())));
    }

>>>>>>> Stashed changes
    @Test
    void simultaneousUpdatesGiveOneWinnerAndOneConflict() throws Exception {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        AtomicInteger wins = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        CountDownLatch go = new CountDownLatch(1);

        Runnable samAccepts = () -> attempt(go, wins, conflicts, () -> f.assignments.assign(f.sam, r.getId(), null, r.getVersion()));
        Runnable sueAccepts = () -> attempt(go, wins, conflicts, () -> f.assignments.assign(f.sue, r.getId(), null, r.getVersion()));
        Thread t1 = new Thread(samAccepts);
        Thread t2 = new Thread(sueAccepts);
        t1.start();
        t2.start();
        go.countDown();
        t1.join();
        t2.join();

        assertEquals(1, wins.get());
        assertEquals(1, conflicts.get());
<<<<<<< Updated upstream
        assertEquals(1, f.audit.historyFor(r.getId()).size());
=======
        assertEquals(2, f.audit.historyFor(r.getId()).size());
>>>>>>> Stashed changes
    }

    private static void attempt(CountDownLatch go, AtomicInteger wins, AtomicInteger conflicts, Runnable action) {
        try {
            go.await();
            action.run();
            wins.incrementAndGet();
        } catch (ConflictException e) {
            conflicts.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void staffCannotAssignToSomeoneElseButManagersCan() {
        ServiceRequest r = f.submit(f.alice, TestFixture.POTHOLES);
        assertThrows(ForbiddenException.class, () -> f.assignments.assign(f.sam, r.getId(), f.sue.id(), r.getVersion()));
        ServiceRequest done = f.assignments.assign(f.mia, r.getId(), f.sue.id(), r.getVersion());
        assertEquals(f.sue.id(), done.getAssignedStaffId());
        assertNotNull(done.getAssignedAt());
    }

    @Test
    void cannotAssignTwice() {
        ServiceRequest a = assigned();
        assertThrows(ConflictException.class, () -> f.assignments.assign(f.mia, a.getId(), f.sue.id(), a.getVersion()));
    }

<<<<<<< Updated upstream
    /** CR-05: no comments once the request is resolved. */
=======
>>>>>>> Stashed changes
    @Test
    void commentsAreRefusedAfterResolved() {
        ServiceRequest p = inProgress();
        f.requests.addComment(f.sam, p.getId(), "Crew booked for Tuesday");
        f.transitions.transition(f.sam, p.getId(),
                new TransitionCommand(RequestStatus.RESOLVED, "Fixed", null, p.getVersion()));
        assertThrows(ConflictException.class, () -> f.requests.addComment(f.sam, p.getId(), "One more thing"));
        assertEquals(1, f.requests.commentsFor(f.sam, p.getId()).size());
    }
<<<<<<< Updated upstream
=======

    @Test
    void commentWithStaleVersionIsAConflict() {
        ServiceRequest p = inProgress();
        assertThrows(ConflictException.class,
                () -> f.requests.addComment(f.sam, p.getId(), "note", p.getVersion() - 1));
        assertNotNull(f.requests.addComment(f.sam, p.getId(), "note", p.getVersion()));
    }
>>>>>>> Stashed changes
}
