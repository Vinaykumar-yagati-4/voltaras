package com.voltaras.notificationservice.controller;

import com.voltaras.notificationservice.dto.request.CreateNotificationRequest;
import com.voltaras.notificationservice.dto.response.NotificationResponse;
import com.voltaras.notificationservice.enums.NotificationChannel;
import com.voltaras.notificationservice.enums.NotificationStatus;
import com.voltaras.notificationservice.enums.NotificationType;
import com.voltaras.notificationservice.exception.AccessDeniedException;
import com.voltaras.notificationservice.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link AdminNotificationController}.
 */
@WebMvcTest(AdminNotificationController.class)
class AdminNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @DisplayName("POST manual notification with ADMIN role: 201 Created")
    void createManualNotification_adminRole_returns201() throws Exception {

        when(notificationService.createManualNotification(
                eq(1L), eq("ADMIN"), any(CreateNotificationRequest.class)))
                .thenReturn(NotificationResponse.builder()
                        .id(1L)
                        .authUserId(100L)
                        .title("Maintenance")
                        .type(NotificationType.MANUAL)
                        .channel(NotificationChannel.IN_APP)
                        .status(NotificationStatus.UNREAD)
                        .build());

        mockMvc.perform(post("/api/notifications/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authUserId": 100,
                                  "title": "Maintenance",
                                  "message": "Scheduled power cut on Sunday."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("MANUAL"))
                .andExpect(jsonPath("$.status").value("UNREAD"));
    }

    @Test
    @DisplayName("POST manual notification with a non-admin role: 403 ACCESS_DENIED")
    void createManualNotification_consumerRole_returns403() throws Exception {

        when(notificationService.createManualNotification(
                eq(1L), eq("CONSUMER"), any(CreateNotificationRequest.class)))
                .thenThrow(new AccessDeniedException(
                        "Only ADMIN users can perform this operation"));

        mockMvc.perform(post("/api/notifications/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authUserId": 100,
                                  "title": "Maintenance",
                                  "message": "Scheduled power cut on Sunday."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("POST manual notification with an invalid body: 400 VALIDATION_ERROR")
    void createManualNotification_invalidBody_returns400() throws Exception {

        mockMvc.perform(post("/api/notifications/admin")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authUserId": 100,
                                  "title": " ",
                                  "message": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").exists());
    }

    @Test
    @DisplayName("GET notifications of a user with ADMIN role: 200 OK")
    void getUserNotificationsForAdmin_adminRole_returns200() throws Exception {

        when(notificationService.getUserNotificationsForAdmin(1L, "ADMIN", 100L))
                .thenReturn(List.of(NotificationResponse.builder()
                        .id(1L)
                        .authUserId(100L)
                        .title("Bill Generated")
                        .build()));

        mockMvc.perform(get("/api/notifications/admin/user/100")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authUserId").value(100));
    }
}
