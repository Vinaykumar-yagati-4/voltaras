package com.voltaras.notificationservice.listener;

import com.voltaras.notificationservice.config.RabbitMQConfig;
import com.voltaras.notificationservice.enums.NotificationChannel;
import com.voltaras.notificationservice.enums.NotificationType;
import com.voltaras.notificationservice.event.RechargeSuccessfulEvent;
import com.voltaras.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code voltaras.recharge.success.queue} and stores a
 * {@code RECHARGE_SUCCESS} notification for the wallet owner.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RechargeSuccessfulNotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.RECHARGE_SUCCESS_QUEUE)
    public void onRechargeSuccessful(RechargeSuccessfulEvent event) {

        log.info("Recharge success event received: rechargeTransactionId={}, authUserId={}",
                event.getRechargeTransactionId(), event.getAuthUserId());

        String amount = event.getAmount() != null
                ? "Rs. " + event.getAmount().toPlainString()
                : "an amount of";

        notificationService.createNotification(
                event.getAuthUserId(),
                "Recharge Successful",
                "Your wallet was recharged with " + amount + ".",
                NotificationType.RECHARGE_SUCCESS,
                NotificationChannel.IN_APP,
                "RECHARGE",
                event.getRechargeTransactionId());
    }
}
