package com.voltaras.notificationservice.listener;

import com.voltaras.notificationservice.config.RabbitMQConfig;
import com.voltaras.notificationservice.enums.NotificationChannel;
import com.voltaras.notificationservice.enums.NotificationType;
import com.voltaras.notificationservice.event.PaymentCompletedEvent;
import com.voltaras.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code voltaras.payment.success.queue} and stores a
 * {@code PAYMENT_SUCCESS} notification for the payer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedNotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void onPaymentCompleted(PaymentCompletedEvent event) {

        log.info("Payment success event received: paymentId={}, authUserId={}",
                event.getPaymentId(), event.getAuthUserId());

        String amount = event.getAmount() != null
                ? "Rs. " + event.getAmount().toPlainString()
                : "your";

        notificationService.createNotification(
                event.getAuthUserId(),
                "Payment Successful",
                "Your bill payment of " + amount
                        + " was completed successfully.",
                NotificationType.PAYMENT_SUCCESS,
                NotificationChannel.IN_APP,
                "PAYMENT",
                event.getPaymentId());
    }
}
