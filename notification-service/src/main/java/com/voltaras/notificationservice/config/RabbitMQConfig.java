package com.voltaras.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for the VOLTARAS Notification Service.
 *
 * <p>
 * The Notification Service is the first VOLTARAS service that uses
 * RabbitMQ. A single durable topic exchange
 * ({@value #EXCHANGE}) carries the domain events published by the other
 * services; each event type has its own durable queue bound with a specific
 * routing key, so every consumer receives only the events it cares about.
 * </p>
 *
 * <p>
 * All events are JSON: the {@link Jackson2JsonMessageConverter} bean is
 * automatically applied by Spring Boot to both the auto-configured
 * {@link RabbitTemplate} and the auto-configured listener container
 * factory, so no custom container factory is needed and the
 * {@code spring.rabbitmq.listener.simple.*} properties (retry, requeue,
 * auto-startup) remain effective.
 * </p>
 */
@Configuration
public class RabbitMQConfig {

    /** Durable topic exchange holding all VOLTARAS notification events. */
    public static final String EXCHANGE = "voltaras.notification.exchange";

    /** Queue for bill generated events. */
    public static final String BILL_GENERATED_QUEUE = "voltaras.bill.generated.queue";

    /** Queue for payment success events. */
    public static final String PAYMENT_SUCCESS_QUEUE = "voltaras.payment.success.queue";

    /** Queue for recharge success events. */
    public static final String RECHARGE_SUCCESS_QUEUE = "voltaras.recharge.success.queue";

    /** Queue for complaint status changed events. */
    public static final String COMPLAINT_STATUS_QUEUE = "voltaras.complaint.status.queue";

    /** Routing key for bill generated events. */
    public static final String BILL_GENERATED_ROUTING_KEY = "notification.bill.generated";

    /** Routing key for payment success events. */
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "notification.payment.success";

    /** Routing key for recharge success events. */
    public static final String RECHARGE_SUCCESS_ROUTING_KEY = "notification.recharge.success";

    /** Routing key for complaint status changed events. */
    public static final String COMPLAINT_STATUS_ROUTING_KEY = "notification.complaint.status";

    /**
     * Durable topic exchange. Producers publish events with one of the
     * {@code notification.*} routing keys; RabbitMQ fans each event out to
     * every queue bound with that key.
     */
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue billGeneratedQueue() {
        return new Queue(BILL_GENERATED_QUEUE, true);
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return new Queue(PAYMENT_SUCCESS_QUEUE, true);
    }

    @Bean
    public Queue rechargeSuccessQueue() {
        return new Queue(RECHARGE_SUCCESS_QUEUE, true);
    }

    @Bean
    public Queue complaintStatusQueue() {
        return new Queue(COMPLAINT_STATUS_QUEUE, true);
    }

    @Bean
    public Binding billGeneratedBinding(Queue billGeneratedQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(billGeneratedQueue)
                .to(notificationExchange)
                .with(BILL_GENERATED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentSuccessBinding(Queue paymentSuccessQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(paymentSuccessQueue)
                .to(notificationExchange)
                .with(PAYMENT_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Binding rechargeSuccessBinding(Queue rechargeSuccessQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(rechargeSuccessQueue)
                .to(notificationExchange)
                .with(RECHARGE_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Binding complaintStatusBinding(Queue complaintStatusQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(complaintStatusQueue)
                .to(notificationExchange)
                .with(COMPLAINT_STATUS_ROUTING_KEY);
    }

    /**
     * Serializes events to/from JSON. Spring Boot applies this bean to the
     * auto-configured {@link RabbitTemplate} and listener container factory.
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
