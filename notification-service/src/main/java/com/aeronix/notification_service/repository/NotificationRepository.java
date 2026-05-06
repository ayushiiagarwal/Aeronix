package com.aeronix.notification_service.repository;

import com.aeronix.notification_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByRecipientIdOrderBySentAtDesc(Integer recipientId);

    List<Notification> findByRecipientIdAndIsRead(Integer recipientId, boolean isRead);

    int countByRecipientIdAndIsRead(Integer recipientId, boolean isRead);

    List<Notification> findByType(Notification.NotificationType type);

    List<Notification> findByRelatedBookingId(String relatedBookingId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
            "WHERE n.notificationId = :id")
    int markAsRead(@Param("id") Integer notificationId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
            "WHERE n.recipientId = :recipientId AND n.isRead = false")
    int markAllReadForRecipient(@Param("recipientId") Integer recipientId);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId " +
            "ORDER BY n.sentAt DESC")
    List<Notification> findAllByRecipient(@Param("recipientId") Integer recipientId);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId " +
            "AND n.type = :type ORDER BY n.sentAt DESC")
    List<Notification> findByRecipientAndType(
            @Param("recipientId") Integer recipientId,
            @Param("type") Notification.NotificationType type);

    void deleteByNotificationId(Integer notificationId);
}
