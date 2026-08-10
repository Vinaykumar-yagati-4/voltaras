package com.voltaras.notificationservice.repository;

import com.voltaras.notificationservice.entity.Notification;
import com.voltaras.notificationservice.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link Notification} entities.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Finds all notifications of a user, newest first.
     */
    List<Notification> findAllByAuthUserIdOrderByCreatedAtDesc(Long authUserId);

    /**
     * Finds the notifications of a user in the given status, newest first.
     */
    List<Notification> findAllByAuthUserIdAndStatusOrderByCreatedAtDesc(
            Long authUserId, NotificationStatus status);

    /**
     * Finds a notification by ID only when it belongs to the given user.
     * Used by the mark-as-read API to enforce ownership.
     */
    Optional<Notification> findByIdAndAuthUserId(Long id, Long authUserId);

    /**
     * Counts the unread notifications of a user.
     */
    long countByAuthUserIdAndStatus(Long authUserId, NotificationStatus status);

    /**
     * Marks every unread notification of a user as READ in a single bulk
     * UPDATE, returning the number of affected rows. The persistence
     * context is cleared after the update so subsequent reads in the same
     * transaction never return stale UNREAD entities.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification n
               set n.status = com.voltaras.notificationservice.enums.NotificationStatus.READ,
                   n.readAt = :readAt,
                   n.updatedAt = :readAt
             where n.authUserId = :authUserId
               and n.status = com.voltaras.notificationservice.enums.NotificationStatus.UNREAD
            """)
    int markAllAsRead(@Param("authUserId") Long authUserId,
                      @Param("readAt") LocalDateTime readAt);
}
