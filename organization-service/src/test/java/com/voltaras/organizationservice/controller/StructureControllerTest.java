package com.voltaras.organizationservice.controller;

import com.voltaras.organizationservice.config.SecurityConfig;
import com.voltaras.organizationservice.dto.response.BuildingResponse;
import com.voltaras.organizationservice.dto.response.MessageResponse;
import com.voltaras.organizationservice.dto.response.UnitResponse;
import com.voltaras.organizationservice.enums.StructureStatus;
import com.voltaras.organizationservice.enums.UnitStatus;
import com.voltaras.organizationservice.service.StructureService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StructureController.class)
@Import(SecurityConfig.class)
class StructureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StructureService structureService;

    @Test
    @DisplayName("POST building: 201 Created")
    void createBuilding_returns201Created() throws Exception {
        BuildingResponse response = BuildingResponse.builder()
                .id(10L)
                .organizationId(1L)
                .name("Main Building")
                .code("MAIN")
                .status(StructureStatus.ACTIVE)
                .build();

        when(structureService.createBuilding(eq(100L), eq(1L), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/organizations/1/buildings")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Main Building",
                                  "code": "MAIN",
                                  "address": "12 College Road"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("MAIN"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST building: blank name fails validation with 400")
    void createBuilding_validationFailure_returns400() throws Exception {
        mockMvc.perform(post("/api/organizations/1/buildings")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "code": "MAIN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET buildings: 200 OK")
    void getOrganizationBuildings_returns200Ok() throws Exception {
        BuildingResponse response = BuildingResponse.builder()
                .id(10L)
                .name("Main Building")
                .build();

        when(structureService.getOrganizationBuildings(100L, "CONSUMER", 1L))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/organizations/1/buildings")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("POST unit: negative capacity fails validation with 400")
    void createUnit_negativeCapacity_returns400() throws Exception {
        mockMvc.perform(post("/api/floors/30/units")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "unitNumber": "101",
                                  "unitType": "ROOM",
                                  "capacity": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PATCH unit status: unknown status fails validation with 400")
    void updateUnitStatus_invalidStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/units/40/status")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "BOOKED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("PATCH unit status: 200 OK")
    void updateUnitStatus_returns200Ok() throws Exception {
        UnitResponse response = UnitResponse.builder()
                .id(40L)
                .status(UnitStatus.OCCUPIED)
                .build();

        when(structureService.updateUnitStatus(eq(100L), eq(40L), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/units/40/status")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "OCCUPIED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OCCUPIED"));
    }

    @Test
    @DisplayName("DELETE unit: 200 OK with message")
    void deleteUnit_returns200WithMessage() throws Exception {
        when(structureService.deleteUnit(100L, 40L))
                .thenReturn(MessageResponse.builder()
                        .message("Resource deleted successfully")
                        .build());

        mockMvc.perform(delete("/api/units/40")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "CONSUMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Resource deleted successfully"));
    }
}