package com.civicconnect.backend;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import com.civicconnect.backend.assignment.AssignmentService;
import com.civicconnect.backend.audit.AuditLogService;
import com.civicconnect.backend.auth.AccessControlService;
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.auth.Role;
import com.civicconnect.backend.controller.RequestAssembler;
import com.civicconnect.backend.dto.CreateRequestDto;
import com.civicconnect.backend.notification.NotificationListener;
import com.civicconnect.backend.notification.NotificationQueryService;
import com.civicconnect.backend.reporting.ReportingService;
import com.civicconnect.backend.request.Category;
import com.civicconnect.backend.request.RequestService;
import com.civicconnect.backend.request.RequestSubmittedEvent;
import com.civicconnect.backend.request.ServiceRequest;
import com.civicconnect.backend.status.StatusChangedEvent;
import com.civicconnect.backend.status.StatusTransitionService;
import com.civicconnect.backend.support.Ids;
import com.civicconnect.backend.support.InMemoryCategoryCatalog;
import com.civicconnect.backend.support.InMemoryNotificationStore;
import com.civicconnect.backend.support.InMemoryRequestStore;
import com.civicconnect.backend.support.InMemoryStatusHistoryStore;
import com.civicconnect.backend.support.InMemoryUserDirectory;

public class TestFixture {

    public static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-01T08:00:00Z");

        public void advance(Duration d) { now = now.plus(d); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    public static final UUID POTHOLES = Ids.of(1);
    public static final UUID IT = Ids.of(2);
    public static final UUID RETIRED = Ids.of(3);
    public static final UUID UNKNOWN = Ids.of(99);

    public final MutableClock clock = new MutableClock();
    public final AccessControlService access = new AccessControlService();
    public final InMemoryRequestStore store = new InMemoryRequestStore();
    public final InMemoryCategoryCatalog categories = new InMemoryCategoryCatalog();
    public final InMemoryUserDirectory users = new InMemoryUserDirectory();
    public final InMemoryStatusHistoryStore historyStore = new InMemoryStatusHistoryStore();
    public final AuditLogService audit = new AuditLogService(historyStore);
    public final InMemoryNotificationStore notificationStore = new InMemoryNotificationStore();
    public final NotificationListener notificationListener = new NotificationListener(notificationStore, clock);
    public final NotificationQueryService notifications =
            new NotificationQueryService(notificationStore, access, clock);

    public boolean auditShouldFail = false;

    private final org.springframework.context.ApplicationEventPublisher publisher = event -> {
        if (event instanceof RequestSubmittedEvent submitted) {
            audit.onRequestSubmitted(submitted);
            notificationListener.onRequestSubmitted(submitted);
        } else if (event instanceof StatusChangedEvent changed) {
            if (auditShouldFail) {
                throw new IllegalStateException("audit write failed");
            }
            audit.onStatusChanged(changed);
            notificationListener.onStatusChanged(changed);
        }
    };

    public final RequestService requests = new RequestService(store, categories, access, clock, publisher);
    public final StatusTransitionService transitions = new StatusTransitionService(requests, access, publisher, clock);
    public final AssignmentService assignments = new AssignmentService(requests, transitions, access, users);
    public final ReportingService reporting = new ReportingService(requests, clock);
    public final RequestAssembler assembler = new RequestAssembler(requests, audit, users, clock);

    public final AuthenticatedUser alice = new AuthenticatedUser(Ids.of(10), Role.REQUESTER, Set.of());
    public final AuthenticatedUser bob = new AuthenticatedUser(Ids.of(11), Role.REQUESTER, Set.of());
    public final AuthenticatedUser sam = new AuthenticatedUser(Ids.of(20), Role.STAFF, Set.of(POTHOLES));
    public final AuthenticatedUser sue = new AuthenticatedUser(Ids.of(21), Role.STAFF, Set.of(POTHOLES));
    public final AuthenticatedUser itStaff = new AuthenticatedUser(Ids.of(22), Role.STAFF, Set.of(IT));
    public final AuthenticatedUser mia = new AuthenticatedUser(Ids.of(30), Role.MANAGER, Set.of());

    public TestFixture() {
        categories.add(new Category(POTHOLES, "Roads and potholes", true, 72));
        categories.add(new Category(IT, "IT support", true, 24));
        categories.add(new Category(RETIRED, "Retired category", false, 72));
        users.register(alice, "Alice Requester");
        users.register(bob, "Bob Requester");
        users.register(sam, "Sam Staff");
        users.register(sue, "Sue Staff");
        users.register(itStaff, "Ian IT");
        users.register(mia, "Mia Manager");
    }

    public ServiceRequest submit(AuthenticatedUser requester, UUID categoryId) {
        return requests.create(requester, new CreateRequestDto(categoryId, "Large pothole outside the clinic", null));
    }
}
