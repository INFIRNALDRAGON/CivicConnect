package com.civicconnect.backend.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    String VIEW = "select new com.civicconnect.backend.notification.Notification("
            + "n.id, n.requestId, r.referenceNumber, n.recipientId, n.message, n.createdAt, n.readAt) "
            + "from NotificationEntity n join ReferenceEntity r "
            + "on r.id = n.requestId ";

    @Query(VIEW + "where n.recipientId = :recipientId order by n.createdAt desc")
    List<Notification> findViewsByRecipient(@Param("recipientId") UUID recipientId);

    @Query(VIEW + "where n.id = :id")
    Optional<Notification> findViewById(@Param("id") UUID id);

    Optional<NotificationEntity> findByIdAndRecipientId(UUID id, UUID recipientId);
}
