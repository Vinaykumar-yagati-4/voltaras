package com.voltaras.notificationservice.listener;

import com.voltaras.notificationservice.config.RabbitMQConfig;
import com.voltaras.notificationservice.enums.NotificationChannel;
import com.voltaras.notificationservice.enums.NotificationType;
import com.voltaras.notificationservice.event.BillGeneratedEvent;
import com.voltaras.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code voltaras.bill.generated.queue} and stores a
 * {@code BILL_GENERATED} notification for the bill owner.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillGeneratedNotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.BILL_GENERATED_QUEUE)
    public void onBillGenerated(BillGeneratedEvent event) {

        log.info("Bill generated event received: billId={}, authUserId={}",
                event.getBillId(), event.getAuthUserId());

        String amount = event.getAmount() != null
                ? "Rs. " + event.getAmount().toPlainString()
                : "your";

        String billingPeriod = event.getBillingPeriod() != null
                ? event.getBillingPeriod()
                : "this period";

        notificationService.createNotification(
                event.getAuthUserId(),
                "Bill Generated",
                "Your electricity bill for " + billingPeriod
                        + " amounting to " + amount + " has been generated.",
                NotificationType.BILL_GENERATED,
                NotificationChannel.IN_APP,
                "BILL",
                event.getBillId());
    }
}
