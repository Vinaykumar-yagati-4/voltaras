package com.voltaras.notificationservice.service.impl;

import com.voltaras.notificationservice.dto.request.CreateNotificationRequest;
import com.voltaras.notificationservice.dto.response.NotificationResponse;
import com.voltaras.notificationservice.dto.response.UnreadNotificationCountResponse;
import com.voltaras.notificationservice.entity.Notification;
import com.voltaras.notificationservice.enums.NotificationChannel;
import com.voltaras.notificationservice.enums.NotificationStatus;
import com.voltaras.notificationservice.enums.NotificationType;
import com.voltaras.notificationservice.exception.ResourceNotFoundException;
import com.voltaras.notificationservice.mapper.NotificationMapper;
import com.voltaras.notificationservice.repository.NotificationRepository;
import com.voltaras.notificationservice.service.NotificationService;
import com.voltaras.notificationservice.util.AdminRoleValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of {@link NotificationService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public NotificationResponse createNotification(
            Long authUserId,
            String title,
            String message,
            NotificationType type,
            NotificationChannel channel,
            String referenceType,
            Long referenceId) {

        Notification notification = Notification.builder()
                .authUserId(authUserId)
                .title(title)
                .message(message)
                .type(type)
                .channel(channel)
                .status(NotificationStatus.UNREAD)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();

        Notification saved = notificationRepository.save(notification);

        log.info("Notification created: id={}, authUserId={}, type={}",
                saved.getId(), saved.getAuthUserId(), saved.getType());

        return notificationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public NotificationResponse createManualNotification(
            Long adminUserId, String systemRole, CreateNotificationRequest request) {

        AdminRoleValidator.requireAdmin(systemRole);

        Notification notification = Notification.builder()
                .authUserId(request.getAuthUserId())
                .title(request.getTitle())
                .message(request.getMessage())
                .type(NotificationType.MANUAL)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.UNREAD)
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .build();

        Notification saved = notificationRepository.save(notification);

        log.info("Manual notification created: id={}, authUserId={}, byAdmin={}",
                saved.getId(), saved.getAuthUserId(), adminUserId);

        return notificationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long authUserId) {

        return notificationMapper.toResponseList(
                notificationRepository.findAllByAuthUserIdOrderByCreatedAtDesc(authUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyUnreadNotifications(Long authUserId) {

        return notificationMapper.toResponseList(
                notificationRepository.findAllByAuthUserIdAndStatusOrderByCreatedAtDesc(
                        authUserId, NotificationStatus.UNREAD));
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long authUserId, Long notificationId) {

        Notification notification = notificationRepository
                .findByIdAndAuthUserId(notificationId, authUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification", "id", notificationId));

        if (notification.getStatus() != NotificationStatus.READ) {

            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(LocalDateTime.now());

            notification = notificationRepository.save(notification);

            log.info("Notification {} marked as read for user {}",
                    notificationId, authUserId);
        }

        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long authUserId) {

        int updated = notificationRepository.markAllAsRead(
                authUserId, LocalDateTime.now());

        log.info("Marked {} notifications as read for user {}",
                updated, authUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(Long authUserId) {

        return UnreadNotificationCountResponse.builder()
                .authUserId(authUserId)
                .unreadCount(notificationRepository.countByAuthUserIdAndStatus(
                        authUserId, NotificationStatus.UNREAD))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotificationsForAdmin(
            Long adminUserId, String systemRole, Long targetAuthUserId) {

        AdminRoleValidator.requireAdmin(systemRole);

        log.info("Admin {} read notifications of user {}",
                adminUserId, targetAuthUserId);

        return notificationMapper.toResponseList(
                notificationRepository.findAllByAuthUserIdOrderByCreatedAtDesc(targetAuthUserId));
    }
}
