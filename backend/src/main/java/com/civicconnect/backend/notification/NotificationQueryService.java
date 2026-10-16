package com.civicconnect.backend.notification;

import java.time.Clock;
import java.util.List;
<<<<<<< Updated upstream
=======
import java.util.UUID;
>>>>>>> Stashed changes

import org.springframework.stereotype.Service;

import com.civicconnect.backend.auth.AccessControlService;
import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.auth.Role;
import com.civicconnect.backend.exception.NotFoundException;

<<<<<<< Updated upstream
/** Public interface of the notification module. Users only ever see their own messages. */
=======
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
    public Notification markRead(AuthenticatedUser actor, Long notificationId) {
=======
    public Notification markRead(AuthenticatedUser actor, UUID notificationId) {
>>>>>>> Stashed changes
        access.requireRole(actor, Role.REQUESTER);
        return store.markRead(notificationId, actor.id(), clock.instant())
                .orElseThrow(() -> new NotFoundException("Notification not found"));
    }
}
