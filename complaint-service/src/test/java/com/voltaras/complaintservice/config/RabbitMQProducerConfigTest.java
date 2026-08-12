package com.voltaras.complaintservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltaras.complaintservice.event.ComplaintStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that the producer writes the logical {@code __TypeId__}
 * ({@code ComplaintStatusChangedEvent}) expected by the Notification
 * Service converter, instead of the complaint-service FQCN.
 */
class RabbitMQProducerConfigTest {

    private Jackson2JsonMessageConverter converter;

    @BeforeEach
    void setUp() {
        converter = new RabbitMQProducerConfig()
                .complaintEventMessageConverter(new ObjectMapper());
    }

    @Test
    @DisplayName("Outbound message carries the logical type ID, not the FQCN")
    void outboundMessage_usesLogicalTypeId() {

        ComplaintStatusChangedEvent event = ComplaintStatusChangedEvent.builder()
                .complaintId(42L)
                .authUserId(13L)
                .status("RESOLVED")
                .build();

        Message message = converter.toMessage(event, new MessageProperties());

        String typeId = (String) message.getMessageProperties()
                .getHeaders()
                .get("__TypeId__");

        assertThat(typeId).isEqualTo("ComplaintStatusChangedEvent");
        assertThat(typeId).isNotEqualTo(
                "com.voltaras.complaintservice.event.ComplaintStatusChangedEvent");
    }

    @Test
    @DisplayName("Round trip: message deserializes back into the event class")
    void roundTrip_deserializesEvent() {

        ComplaintStatusChangedEvent event = ComplaintStatusChangedEvent.builder()
                .complaintId(42L)
                .authUserId(13L)
                .status("CLOSED")
                .build();

        Message message = converter.toMessage(event, new MessageProperties());

        ComplaintStatusChangedEvent decoded =
                (ComplaintStatusChangedEvent) converter.fromMessage(message);

        assertThat(decoded.getComplaintId()).isEqualTo(42L);
        assertThat(decoded.getAuthUserId()).isEqualTo(13L);
        assertThat(decoded.getStatus()).isEqualTo("CLOSED");
    }
}
