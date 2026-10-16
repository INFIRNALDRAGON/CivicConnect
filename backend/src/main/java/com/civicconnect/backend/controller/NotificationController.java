package com.civicconnect.backend.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    public List<Map<String, Object>> mine(@AuthenticationPrincipal AuthenticatedUser actor) {
        return notifications.listFor(actor).stream().map(NotificationController::toDto).toList();
    }

    @PostMapping("/{id}/read")
    public Map<String, Object> markRead(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID id) {
        return toDto(notifications.markRead(actor, id));
    }

    private static Map<String, Object> toDto(Notification n) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", n.id().toString());
        dto.put("requestId", n.requestId().toString());
        dto.put("referenceNumber", n.referenceNumber());
        dto.put("message", n.message());
        dto.put("createdAt", n.createdAt().toString());
        dto.put("readAt", n.readAt() == null ? null : n.readAt().toString());
        return dto;
    }
}
