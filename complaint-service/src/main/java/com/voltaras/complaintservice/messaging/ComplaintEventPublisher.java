package com.voltaras.complaintservice.messaging;

import com.voltaras.complaintservice.config.RabbitMQProducerConfig;
import com.voltaras.complaintservice.entity.Complaint;
import com.voltaras.complaintservice.enums.ComplaintStatus;
import com.voltaras.complaintservice.event.ComplaintStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes complaint domain events to the Notification Service exchange.
 *
 * <p>
 * Publishing is best-effort and asynchronous: a broker outage must never
 * fail or roll back the complaint update itself, so every publish failure
 * is logged and swallowed. This matches the Notification Service's retry
 * design (3 consumer attempts, then drop) — delivery is not guaranteed.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ComplaintEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes a status-changed event for the given complaint.
     *
     * @param complaint the complaint whose status changed
     * @param newStatus the new status
     */
    public void publishStatusChanged(Complaint complaint, ComplaintStatus newStatus) {

        ComplaintStatusChangedEvent event = ComplaintStatusChangedEvent.builder()
                .complaintId(complaint.getId())
                .authUserId(complaint.getConsumerId())
                .status(newStatus.name())
                .build();

        try {

            rabbitTemplate.convertAndSend(
                    RabbitMQProducerConfig.EXCHANGE,
                    RabbitMQProducerConfig.COMPLAINT_STATUS_ROUTING_KEY,
                    event
            );

            log.info("Complaint status event published: complaintId={}, status={}",
                    event.getComplaintId(), event.getStatus());

        } catch (RuntimeException ex) {

            // AmqpException (and any other runtime failure) is caught here
            // so a broker outage never fails the complaint update.
            log.error("Failed to publish complaint status event for complaint {}: {}",
                    complaint.getId(), ex.getMessage(), ex);
        }
    }
}
