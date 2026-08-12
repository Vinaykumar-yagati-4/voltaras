package com.voltaras.complaintservice.messaging;

import com.voltaras.complaintservice.config.RabbitMQProducerConfig;
import com.voltaras.complaintservice.entity.Complaint;
import com.voltaras.complaintservice.enums.ComplaintPriority;
import com.voltaras.complaintservice.enums.ComplaintStatus;
import com.voltaras.complaintservice.event.ComplaintStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Tests for the best-effort complaint event publisher.
 */
@ExtendWith(MockitoExtension.class)
class ComplaintEventPublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;

    private ComplaintEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ComplaintEventPublisher(rabbitTemplate);
    }

    @Test
    @DisplayName("Publish sends the event to the notification exchange with the right routing key")
    void publishStatusChanged_sendsEvent() {

        Complaint complaint = Complaint.builder()
                .id(42L)
                .consumerId(13L)
                .status(ComplaintStatus.OPEN)
                .priority(ComplaintPriority.NORMAL)
                .build();

        publisher.publishStatusChanged(complaint, ComplaintStatus.RESOLVED);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQProducerConfig.EXCHANGE),
                eq(RabbitMQProducerConfig.COMPLAINT_STATUS_ROUTING_KEY),
                captor.capture());

        ComplaintStatusChangedEvent event =
                (ComplaintStatusChangedEvent) captor.getValue();

        assertThat(event.getComplaintId()).isEqualTo(42L);
        assertThat(event.getAuthUserId()).isEqualTo(13L);
        assertThat(event.getStatus()).isEqualTo("RESOLVED");
    }

    @Test
    @DisplayName("Publish swallows broker failures so the complaint update is never rolled back")
    void publishStatusChanged_brokerDown_doesNotThrow() {

        Complaint complaint = Complaint.builder()
                .id(42L)
                .consumerId(13L)
                .status(ComplaintStatus.OPEN)
                .priority(ComplaintPriority.NORMAL)
                .build();

        doThrow(new AmqpConnectException(
                new java.net.ConnectException("Connection refused")))
                .when(rabbitTemplate)
                .convertAndSend(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(Object.class));

        assertThatCode(() -> publisher.publishStatusChanged(
                complaint, ComplaintStatus.IN_PROGRESS))
                .doesNotThrowAnyException();
    }
}
