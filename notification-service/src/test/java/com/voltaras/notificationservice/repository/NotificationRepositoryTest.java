package com.voltaras.notificationservice.repository;

import com.voltaras.notificationservice.entity.Notification;
import com.voltaras.notificationservice.enums.NotificationChannel;
import com.voltaras.notificationservice.enums.NotificationStatus;
import com.voltaras.notificationservice.enums.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA slice tests for {@link NotificationRepository}, executed against the
 * in-memory H2 database (MySQL mode) from the test resources.
 */
@DataJpaTest
class NotificationRepositoryTest {

    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("Notifications are found per user, newest first")
    void findAllByAuthUserIdOrderByCreatedAtDesc_returnsUserNotifications() {

        Notification older = saveNotification(USER_ID, "Older");
        saveNotification(OTHER_USER_ID, "Other user");
        Notification newer = saveNotification(USER_ID, "Newer");

        List<Notification> result =
                notificationRepository.findAllByAuthUserIdOrderByCreatedAtDesc(USER_ID);

        assertThat(result).extracting(Notification::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    @DisplayName("Unread notifications are filtered by status")
    void findAllByAuthUserIdAndStatus_returnsOnlyUnread() {

        Notification unread = saveNotification(USER_ID, "Unread");
        Notification read = saveNotification(USER_ID, "Read");
        read.setStatus(NotificationStatus.READ);
        notificationRepository.save(read);

        List<Notification> result = notificationRepository
                .findAllByAuthUserIdAndStatusOrderByCreatedAtDesc(
                        USER_ID, NotificationStatus.UNREAD);

        assertThat(result).extracting(Notification::getId)
                .containsExactly(unread.getId());
    }

    @Test
    @DisplayName("A notification is found by ID only when owned by the user")
    void findByIdAndAuthUserId_enforcesOwnership() {

        Notification owned = saveNotification(USER_ID, "Owned");

        Optional<Notification> byOwner =
                notificationRepository.findByIdAndAuthUserId(owned.getId(), USER_ID);
        Optional<Notification> byOther =
                notificationRepository.findByIdAndAuthUserId(owned.getId(), OTHER_USER_ID);

        assertThat(byOwner).isPresent();
        assertThat(byOther).isEmpty();
    }

    @Test
    @DisplayName("Unread count counts only UNREAD notifications")
    void countByAuthUserIdAndStatus_countsUnread() {

        saveNotification(USER_ID, "Unread 1");
        Notification read = saveNotification(USER_ID, "Read");
        read.setStatus(NotificationStatus.READ);
        notificationRepository.save(read);
        saveNotification(USER_ID, "Unread 2");
        saveNotification(OTHER_USER_ID, "Other user unread");

        long unreadCount = notificationRepository.countByAuthUserIdAndStatus(
                USER_ID, NotificationStatus.UNREAD);

        assertThat(unreadCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("markAllAsRead bulk-updates only the user's unread notifications")
    void markAllAsRead_updatesUnreadOnly() {

        saveNotification(USER_ID, "Unread 1");
        saveNotification(USER_ID, "Unread 2");
        saveNotification(OTHER_USER_ID, "Other user unread");

        int updated = notificationRepository.markAllAsRead(
                USER_ID, LocalDateTime.now());

        assertThat(updated).isEqualTo(2);

        assertThat(notificationRepository.countByAuthUserIdAndStatus(
                USER_ID, NotificationStatus.UNREAD)).isZero();
        // The other user's notification is untouched.
        assertThat(notificationRepository.countByAuthUserIdAndStatus(
                OTHER_USER_ID, NotificationStatus.UNREAD)).isEqualTo(1L);

        // readAt is stamped on every updated row.
        List<Notification> all = notificationRepository
                .findAllByAuthUserIdOrderByCreatedAtDesc(USER_ID);
        assertThat(all).allSatisfy(n -> assertThat(n.getReadAt()).isNotNull());
    }

    private Notification saveNotification(Long authUserId, String title) {

        Notification notification = Notification.builder()
                .authUserId(authUserId)
                .title(title)
                .message("Test message for " + title)
                .type(NotificationType.MANUAL)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.UNREAD)
                .build();

        return notificationRepository.save(notification);
    }
}
