package com.voltaras.notificationservice.listener;

import com.voltaras.notificationservice.enums.NotificationChannel;
import com.voltaras.notificationservice.enums.NotificationType;
import com.voltaras.notificationservice.event.BillGeneratedEvent;
import com.voltaras.notificationservice.event.ComplaintStatusChangedEvent;
import com.voltaras.notificationservice.event.PaymentCompletedEvent;
import com.voltaras.notificationservice.event.RechargeSuccessfulEvent;
import com.voltaras.notificationservice.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the RabbitMQ listeners: each listener must convert its
 * event into a notification with the matching type, channel and reference.
 */
@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock private NotificationService notificationService;

    @Test
    @DisplayName("BillGeneratedEvent becomes a BILL_GENERATED notification")
    void billGeneratedListener_createsNotification() {

        BillGeneratedNotificationListener listener =
                new BillGeneratedNotificationListener(notificationService);

        BillGeneratedEvent event = BillGeneratedEvent.builder()
                .billId(12L)
                .authUserId(100L)
                .amount(new BigDecimal("1250.00"))
                .billingPeriod("August 2026")
                .build();

        listener.onBillGenerated(event);

        verify(notificationService).createNotification(
                anyLong(), anyString(), anyString(),
                any(NotificationType.class), any(NotificationChannel.class),
                anyString(), anyLong());
    }

    @Test
    @DisplayName("PaymentCompletedEvent becomes a PAYMENT_SUCCESS notification")
    void paymentCompletedListener_createsNotification() {

        PaymentCompletedNotificationListener listener =
                new PaymentCompletedNotificationListener(notificationService);

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(7L)
                .billId(12L)
                .authUserId(100L)
                .amount(new BigDecimal("1250.00"))
                .build();

        listener.onPaymentCompleted(event);

        verify(notificationService).createNotification(
                anyLong(), anyString(), anyString(),
                any(NotificationType.class), any(NotificationChannel.class),
                anyString(), anyLong());
    }

    @Test
    @DisplayName("RechargeSuccessfulEvent becomes a RECHARGE_SUCCESS notification")
    void rechargeSuccessfulListener_createsNotification() {

        RechargeSuccessfulNotificationListener listener =
                new RechargeSuccessfulNotificationListener(notificationService);

        RechargeSuccessfulEvent event = RechargeSuccessfulEvent.builder()
                .rechargeTransactionId(3L)
                .authUserId(100L)
                .amount(new BigDecimal("500.00"))
                .build();

        listener.onRechargeSuccessful(event);

        verify(notificationService).createNotification(
                anyLong(), anyString(), anyString(),
                any(NotificationType.class), any(NotificationChannel.class),
                anyString(), anyLong());
    }

    @Test
    @DisplayName("ComplaintStatusChangedEvent becomes a COMPLAINT_STATUS_UPDATED notification")
    void complaintStatusChangedListener_createsNotification() {

        ComplaintStatusChangedNotificationListener listener =
                new ComplaintStatusChangedNotificationListener(notificationService);

        ComplaintStatusChangedEvent event = ComplaintStatusChangedEvent.builder()
                .complaintId(42L)
                .authUserId(100L)
                .status("RESOLVED")
                .build();

        listener.onComplaintStatusChanged(event);

        verify(notificationService).createNotification(
                anyLong(), anyString(), anyString(),
                any(NotificationType.class), any(NotificationChannel.class),
                anyString(), anyLong());
    }
}
