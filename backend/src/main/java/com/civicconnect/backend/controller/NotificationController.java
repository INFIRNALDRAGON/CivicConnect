package com.civicconnect.backend.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.civicconnect.backend.auth.AuthenticatedUser;
import com.civicconnect.backend.notification.Notification;
import com.civicconnect.backend.notification.NotificationQueryService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationQueryService notifications;

    public NotificationController(NotificationQueryService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<Notification> mine(@AuthenticationPrincipal AuthenticatedUser actor) {
        return notifications.listFor(actor);
    }

    @PostMapping("/{id}/read")
    public Notification markRead(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        return notifications.markRead(actor, id);
    }
}
