package com.voltaras.notificationservice.service;

import com.voltaras.notificationservice.dto.request.CreateNotificationRequest;
import com.voltaras.notificationservice.dto.response.NotificationResponse;
import com.voltaras.notificationservice.dto.response.UnreadNotificationCountResponse;
import com.voltaras.notificationservice.entity.Notification;
import com.voltaras.notificationservice.enums.NotificationChannel;
import com.voltaras.notificationservice.enums.NotificationStatus;
import com.voltaras.notificationservice.enums.NotificationType;
import com.voltaras.notificationservice.exception.AccessDeniedException;
import com.voltaras.notificationservice.exception.ResourceNotFoundException;
import com.voltaras.notificationservice.mapper.NotificationMapper;
import com.voltaras.notificationservice.repository.NotificationRepository;
import com.voltaras.notificationservice.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final Long USER_ID = 100L;
    private static final Long NOTIFICATION_ID = 5L;

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(
                notificationRepository, notificationMapper);
    }

    @Test
    @DisplayName("Event-driven createNotification saves an UNREAD IN_APP notification")
    void createNotification_savesAndReturnsResponse() {

        Notification notification = buildNotification(NotificationStatus.UNREAD);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(notificationMapper.toResponse(any(Notification.class)))
                .thenReturn(NotificationResponse.builder()
                        .id(NOTIFICATION_ID)
                        .authUserId(USER_ID)
                        .status(NotificationStatus.UNREAD)
                        .build());

        NotificationResponse response = notificationService.createNotification(
                USER_ID,
                "Bill Generated",
                "Your bill has been generated.",
                NotificationType.BILL_GENERATED,
                NotificationChannel.IN_APP,
                "BILL",
                12L);

        assertThat(response.getId()).isEqualTo(NOTIFICATION_ID);
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.UNREAD);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("createManualNotification with ADMIN role creates a MANUAL/IN_APP notification")
    void createManualNotification_adminRole_creates() {

        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .authUserId(USER_ID)
                .title("Voltage Maintenance")
                .message("Power cut on Sunday.")
                .referenceType("COMPLAINT")
                .referenceId(42L)
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> {
                    Notification n = inv.getArgument(0);
                    n.setId(NOTIFICATION_ID);
                    return n;
                });
        when(notificationMapper.toResponse(any(Notification.class)))
                .thenAnswer(inv -> {
                    Notification n = inv.getArgument(0);
                    return NotificationResponse.builder()
                            .id(n.getId())
                            .authUserId(n.getAuthUserId())
                            .title(n.getTitle())
                            .type(n.getType())
                            .channel(n.getChannel())
                            .status(n.getStatus())
                            .referenceType(n.getReferenceType())
                            .referenceId(n.getReferenceId())
                            .build();
                });

        NotificationResponse response = notificationService
                .createManualNotification(1L, "ADMIN", request);

        assertThat(response.getType()).isEqualTo(NotificationType.MANUAL);
        assertThat(response.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.UNREAD);
        assertThat(response.getReferenceId()).isEqualTo(42L);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("createManualNotification accepts the ROLE_ADMIN spelling")
    void createManualNotification_roleAdminSpelling_accepted() {

        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .authUserId(USER_ID)
                .title("Maintenance")
                .message("Scheduled power cut.")
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        notificationService.createManualNotification(1L, "ROLE_ADMIN", request);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("createManualNotification is rejected for a non-admin role")
    void createManualNotification_nonAdmin_throws() {

        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .authUserId(USER_ID)
                .title("Maintenance")
                .message("Scheduled power cut.")
                .build();

        assertThatThrownBy(() -> notificationService
                .createManualNotification(1L, "CONSUMER", request))
                .isInstanceOf(AccessDeniedException.class);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("getMyNotifications returns the user's notifications")
    void getMyNotifications_returnsUserNotifications() {

        when(notificationRepository.findAllByAuthUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(buildNotification(NotificationStatus.READ)));
        when(notificationMapper.toResponseList(any()))
                .thenReturn(List.of(NotificationResponse.builder()
                        .authUserId(USER_ID)
                        .build()));

        List<NotificationResponse> responses =
                notificationService.getMyNotifications(USER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getAuthUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("getMyUnreadNotifications filters by UNREAD status")
    void getMyUnreadNotifications_filtersUnread() {

        when(notificationRepository
                .findAllByAuthUserIdAndStatusOrderByCreatedAtDesc(
                        USER_ID, NotificationStatus.UNREAD))
                .thenReturn(List.of(buildNotification(NotificationStatus.UNREAD)));

        notificationService.getMyUnreadNotifications(USER_ID);

        verify(notificationRepository)
                .findAllByAuthUserIdAndStatusOrderByCreatedAtDesc(
                        USER_ID, NotificationStatus.UNREAD);
    }

    @Test
    @DisplayName("markAsRead on an owned notification sets READ and readAt")
    void markAsRead_owned_marksRead() {

        Notification notification = buildNotification(NotificationStatus.UNREAD);
        when(notificationRepository.findByIdAndAuthUserId(NOTIFICATION_ID, USER_ID))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(notificationMapper.toResponse(any(Notification.class)))
                .thenAnswer(inv -> {
                    Notification n = inv.getArgument(0);
                    return NotificationResponse.builder()
                            .id(n.getId())
                            .status(n.getStatus())
                            .readAt(n.getReadAt())
                            .build();
                });

        NotificationResponse response = notificationService
                .markAsRead(USER_ID, NOTIFICATION_ID);

        assertThat(response.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(response.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("markAsRead on a notification owned by another user throws 404")
    void markAsRead_notOwned_throws() {

        when(notificationRepository.findByIdAndAuthUserId(NOTIFICATION_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService
                .markAsRead(USER_ID, NOTIFICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification not found");

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("markAllAsRead delegates to the bulk repository update")
    void markAllAsRead_updatesAll() {

        when(notificationRepository.markAllAsRead(any(), any()))
                .thenReturn(3);

        notificationService.markAllAsRead(USER_ID);

        verify(notificationRepository).markAllAsRead(
                eq(USER_ID), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("getUnreadCount returns the unread count")
    void getUnreadCount_returnsCount() {

        when(notificationRepository.countByAuthUserIdAndStatus(
                USER_ID, NotificationStatus.UNREAD))
                .thenReturn(4L);

        UnreadNotificationCountResponse response =
                notificationService.getUnreadCount(USER_ID);

        assertThat(response.getAuthUserId()).isEqualTo(USER_ID);
        assertThat(response.getUnreadCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("getUserNotificationsForAdmin is rejected for a non-admin role")
    void getUserNotificationsForAdmin_nonAdmin_throws() {

        assertThatThrownBy(() -> notificationService
                .getUserNotificationsForAdmin(1L, "CONSUMER", USER_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(notificationRepository, never())
                .findAllByAuthUserIdOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("getUserNotificationsForAdmin with ADMIN role returns target user notifications")
    void getUserNotificationsForAdmin_adminRole_returns() {

        when(notificationRepository.findAllByAuthUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(buildNotification(NotificationStatus.UNREAD)));

        notificationService.getUserNotificationsForAdmin(1L, "ADMIN", USER_ID);

        verify(notificationRepository)
                .findAllByAuthUserIdOrderByCreatedAtDesc(USER_ID);
    }

    private Notification buildNotification(NotificationStatus status) {

        return Notification.builder()
                .id(NOTIFICATION_ID)
                .authUserId(USER_ID)
                .title("Test notification")
                .message("Test message")
                .type(NotificationType.MANUAL)
                .channel(NotificationChannel.IN_APP)
                .status(status)
                .build();
    }
}
