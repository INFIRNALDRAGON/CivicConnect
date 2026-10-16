package com.civicconnect.backend.notification;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.civicconnect.backend.auth.AccessControlService;
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.auth.Role;
import com.civicconnect.backend.exception.NotFoundException;

@Service
public class NotificationQueryService {

    private final NotificationStore store;
    private final AccessControlService access;
    private final Clock clock;

    public NotificationQueryService(NotificationStore store, AccessControlService access, Clock clock) {
        this.store = store;
        this.access = access;
        this.clock = clock;
    }

    public List<Notification> listFor(AuthenticatedUser actor) {
        access.requireRole(actor, Role.REQUESTER);
        return store.findByRecipient(actor.id());
    }

    public Notification markRead(AuthenticatedUser actor, UUID notificationId) {
        access.requireRole(actor, Role.REQUESTER);
        return store.markRead(notificationId, actor.id(), clock.instant())
                .orElseThrow(() -> new NotFoundException("Notification not found"));
    }
}
