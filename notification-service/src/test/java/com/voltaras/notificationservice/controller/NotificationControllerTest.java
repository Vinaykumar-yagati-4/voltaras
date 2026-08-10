package com.voltaras.notificationservice.controller;

import com.voltaras.notificationservice.dto.response.NotificationResponse;
import com.voltaras.notificationservice.dto.response.UnreadNotificationCountResponse;
import com.voltaras.notificationservice.enums.NotificationStatus;
import com.voltaras.notificationservice.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link NotificationController}.
 */
@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @DisplayName("GET my notifications: 200 OK with the user's notifications")
    void getMyNotifications_returns200() throws Exception {

        when(notificationService.getMyNotifications(100L))
                .thenReturn(List.of(NotificationResponse.builder()
                        .id(1L)
                        .authUserId(100L)
                        .title("Bill Generated")
                        .status(NotificationStatus.UNREAD)
                        .build()));

        mockMvc.perform(get("/api/notifications")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].authUserId").value(100))
                .andExpect(jsonPath("$[0].title").value("Bill Generated"));
    }

    @Test
    @DisplayName("GET my notifications: missing X-User-Id returns 400 MISSING_HEADER")
    void getMyNotifications_missingUserHeader_returns400() throws Exception {

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_HEADER"));
    }

    @Test
    @DisplayName("GET unread notifications: 200 OK")
    void getMyUnreadNotifications_returns200() throws Exception {

        mockMvc.perform(get("/api/notifications/unread")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk());

        verify(notificationService).getMyUnreadNotifications(100L);
    }

    @Test
    @DisplayName("PATCH mark one notification read: 200 OK")
    void markAsRead_returns200() throws Exception {

        when(notificationService.markAsRead(100L, 5L))
                .thenReturn(NotificationResponse.builder()
                        .id(5L)
                        .authUserId(100L)
                        .status(NotificationStatus.READ)
                        .build());

        mockMvc.perform(patch("/api/notifications/5/read")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));
    }

    @Test
    @DisplayName("PATCH mark all read: 204 No Content")
    void markAllAsRead_returns204() throws Exception {

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("X-User-Id", "100"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(100L);
    }

    @Test
    @DisplayName("GET unread count: 200 OK with count")
    void getUnreadCount_returns200() throws Exception {

        when(notificationService.getUnreadCount(100L))
                .thenReturn(UnreadNotificationCountResponse.builder()
                        .authUserId(100L)
                        .unreadCount(4)
                        .build());

        mockMvc.perform(get("/api/notifications/count/unread")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authUserId").value(100))
                .andExpect(jsonPath("$.unreadCount").value(4));
    }
}
