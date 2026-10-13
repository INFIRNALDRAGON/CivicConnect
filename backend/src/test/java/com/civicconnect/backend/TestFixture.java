package com.civicconnect.backend;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import com.civicconnect.backend.assignment.AssignmentService;
import com.civicconnect.backend.audit.AuditLogService;
import com.civicconnect.backend.auth.AccessControlService;
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.auth.InMemoryUserDirectory;
import com.civicconnect.backend.auth.Role;
import com.civicconnect.backend.dto.CreateRequestDto;
import com.civicconnect.backend.notification.NotificationListener;
import com.civicconnect.backend.notification.NotificationQueryService;
import com.civicconnect.backend.notification.NotificationStore;
import com.civicconnect.backend.reporting.ReportingService;
import com.civicconnect.backend.request.Category;
import com.civicconnect.backend.request.CategoryCatalog;
import com.civicconnect.backend.request.InMemoryRequestStore;
import com.civicconnect.backend.request.ReferenceNumberGenerator;
import com.civicconnect.backend.request.RequestService;
import com.civicconnect.backend.request.ServiceRequest;
import com.civicconnect.backend.status.StatusChangedEvent;
import com.civicconnect.backend.status.StatusTransitionService;

/**
 * Wires the whole backend by hand (no Spring context, no database) so every rule can be tested quickly.
 * The publisher below calls the observers directly, like Spring does inside one transaction.
 */
public class TestFixture {

    /** A clock the tests can move forward. */
    public static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-01T08:00:00Z");

        public void advance(Duration d) { now = now.plus(d); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    public static final Long POTHOLES = 1L;   // category ids
    public static final Long IT = 2L;

    public final MutableClock clock = new MutableClock();
    public final AccessControlService access = new AccessControlService();
    public final InMemoryRequestStore store = new InMemoryRequestStore();
    public final CategoryCatalog categories = new CategoryCatalog();
    public final InMemoryUserDirectory users = new InMemoryUserDirectory();
    public final AuditLogService audit = new AuditLogService();
    public final NotificationStore notificationStore = new NotificationStore();
    public final NotificationListener notificationListener = new NotificationListener(notificationStore);
    public final NotificationQueryService notifications =
            new NotificationQueryService(notificationStore, access, clock);

    /** Set to make the audit observer fail, to test that the status change is undone (ASR-01). */
    public boolean auditShouldFail = false;

    public final RequestService requests =
            new RequestService(store, categories, new ReferenceNumberGenerator(), access, clock);
    public final StatusTransitionService transitions = new StatusTransitionService(requests, access, event -> {
        StatusChangedEvent e = (StatusChangedEvent) event;
        if (auditShouldFail) {
            throw new IllegalStateException("audit write failed");
        }
        audit.onStatusChanged(e);
        notificationListener.onStatusChanged(e);   // after-commit observer
    }, clock);
    public final AssignmentService assignments = new AssignmentService(requests, transitions, access, users);
    public final ReportingService reporting = new ReportingService(requests, clock, 72);

    public final AuthenticatedUser alice = new AuthenticatedUser(10L, Role.REQUESTER, Set.of());
    public final AuthenticatedUser bob = new AuthenticatedUser(11L, Role.REQUESTER, Set.of());
    public final AuthenticatedUser sam = new AuthenticatedUser(20L, Role.STAFF, Set.of(POTHOLES));
    public final AuthenticatedUser sue = new AuthenticatedUser(21L, Role.STAFF, Set.of(POTHOLES));
    public final AuthenticatedUser itStaff = new AuthenticatedUser(22L, Role.STAFF, Set.of(IT));
    public final AuthenticatedUser mia = new AuthenticatedUser(30L, Role.MANAGER, Set.of());

    public TestFixture() {
        categories.add(new Category(POTHOLES, "Roads and potholes", true));
        categories.add(new Category(IT, "IT support", true));
        categories.add(new Category(3L, "Retired category", false));
        for (AuthenticatedUser u : new AuthenticatedUser[] {alice, bob, sam, sue, itStaff, mia}) {
            users.register(u);
        }
    }

    public ServiceRequest submit(AuthenticatedUser requester, Long categoryId) {
        return requests.create(requester, new CreateRequestDto(categoryId, "Large pothole outside the clinic", null));
    }
}
