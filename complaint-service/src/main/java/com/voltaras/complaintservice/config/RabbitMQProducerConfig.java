package com.voltaras.complaintservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltaras.complaintservice.event.ComplaintStatusChangedEvent;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * RabbitMQ producer wiring for the Complaint Service.
 *
 * <p>
 * The service only publishes events; it declares no exchanges, queues or
 * bindings. It reuses the Notification Service's topology:
 * exchange {@code voltaras.notification.exchange}, routing key
 * {@code notification.complaint.status}, queue
 * {@code voltaras.complaint.status.queue}.
 * </p>
 *
 * <p>
 * Serialization follows the existing Notification Service convention
 * ({@link Jackson2JsonMessageConverter}) with one addition: the producer
 * writes a <em>logical</em> type ID ({@code ComplaintStatusChangedEvent})
 * instead of the producer-side fully qualified class name, so the
 * Notification Service (whose event class lives in a different package)
 * can resolve it through its own type-ID mapping.
 * </p>
 */
@Configuration
public class RabbitMQProducerConfig {

    /**
     * Exchange and routing key constants mirroring
     * {@code NotificationServiceRabbitMQConfig}.
     */
    public static final String EXCHANGE = "voltaras.notification.exchange";
    public static final String COMPLAINT_STATUS_ROUTING_KEY =
            "notification.complaint.status";

    /**
     * Logical type ID the Notification Service maps to its own
     * {@code ComplaintStatusChangedEvent} class.
     */
    public static final String COMPLAINT_STATUS_EVENT_TYPE_ID =
            "ComplaintStatusChangedEvent";

    /**
     * Serializes events to JSON and stamps the logical {@code __TypeId__}
     * header expected by the Notification Service.
     */
    @Bean
    public Jackson2JsonMessageConverter complaintEventMessageConverter(
            ObjectMapper objectMapper) {

        Jackson2JsonMessageConverter converter =
                new Jackson2JsonMessageConverter(objectMapper);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();

        // id-to-class mapping: on outbound serialization the mapper writes
        // the mapped logical ID instead of the FQCN; on inbound it resolves
        // the logical ID back to the event class.
        typeMapper.setIdClassMapping(Map.of(
                COMPLAINT_STATUS_EVENT_TYPE_ID,
                ComplaintStatusChangedEvent.class
        ));

        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    @Bean
    public RabbitTemplate complaintRabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter complaintEventMessageConverter) {

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(complaintEventMessageConverter);

        return template;
    }
}
