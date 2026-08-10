package com.voltaras.notificationservice.listener;

import com.voltaras.notificationservice.config.RabbitMQConfig;
import com.voltaras.notificationservice.enums.NotificationChannel;
import com.voltaras.notificationservice.enums.NotificationType;
import com.voltaras.notificationservice.event.ComplaintStatusChangedEvent;
import com.voltaras.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code voltaras.complaint.status.queue} and stores a
 * {@code COMPLAINT_STATUS_UPDATED} notification for the complaint owner.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ComplaintStatusChangedNotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.COMPLAINT_STATUS_QUEUE)
    public void onComplaintStatusChanged(ComplaintStatusChangedEvent event) {

        log.info("Complaint status event received: complaintId={}, authUserId={}",
                event.getComplaintId(), event.getAuthUserId());

        String status = event.getStatus() != null ? event.getStatus() : "changed";

        notificationService.createNotification(
                event.getAuthUserId(),
                "Complaint Status Updated",
                "The status of your complaint #" + event.getComplaintId()
                        + " was updated to " + status + ".",
                NotificationType.COMPLAINT_STATUS_UPDATED,
                NotificationChannel.IN_APP,
                "COMPLAINT",
                event.getComplaintId());
    }
}
