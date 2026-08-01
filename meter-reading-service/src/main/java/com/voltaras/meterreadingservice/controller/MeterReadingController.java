package com.voltaras.meterreadingservice.controller;

import com.voltaras.meterreadingservice.dto.request.RejectMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.request.SubmitMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.request.UpdateMeterReadingRequest;
import com.voltaras.meterreadingservice.dto.response.MeterReadingResponse;
import com.voltaras.meterreadingservice.enums.MeterReadingStatus;
import com.voltaras.meterreadingservice.service.MeterReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meter-readings")
@RequiredArgsConstructor
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping
    public ResponseEntity<MeterReadingResponse> submitReading(
            @RequestHeader("X-User-Id") Long authUserId,
            @Valid @RequestBody SubmitMeterReadingRequest request
    ) {

        MeterReadingResponse response =
                meterReadingService.submitReading(authUserId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<MeterReadingResponse>> getMyReadings(
            @RequestHeader("X-User-Id") Long authUserId
    ) {

        List<MeterReadingResponse> response =
                meterReadingService.getMyReadings(authUserId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/{readingId}")
    public ResponseEntity<MeterReadingResponse> getMyReadingById(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long readingId
    ) {

        MeterReadingResponse response =
                meterReadingService.getMyReadingById(authUserId, readingId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/{readingId}")
    public ResponseEntity<MeterReadingResponse> updateMyReading(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long readingId,
            @Valid @RequestBody UpdateMeterReadingRequest request
    ) {

        MeterReadingResponse response =
                meterReadingService.updateMyReading(authUserId, readingId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/{readingId}")
    public ResponseEntity<Map<String, String>> deleteMyReading(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long readingId
    ) {

        meterReadingService.deleteMyReading(authUserId, readingId);

        return ResponseEntity.ok(
                Map.of("message", "Meter reading deleted successfully")
        );
    }

    @GetMapping("/admin")
    public ResponseEntity<List<MeterReadingResponse>> getAllReadingsForAdmin(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(name = "status", required = false) MeterReadingStatus status
    ) {

        List<MeterReadingResponse> response =
                meterReadingService.getAllReadingsForAdmin(role, status);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/admin/{readingId}/verify")
    public ResponseEntity<MeterReadingResponse> verifyReading(
            @RequestHeader("X-User-Id") Long adminUserId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long readingId
    ) {

        MeterReadingResponse response =
                meterReadingService.verifyReading(adminUserId, role, readingId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/admin/{readingId}/reject")
    public ResponseEntity<MeterReadingResponse> rejectReading(
            @RequestHeader("X-User-Id") Long adminUserId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long readingId,
            @Valid @RequestBody RejectMeterReadingRequest request
    ) {

        MeterReadingResponse response =
                meterReadingService.rejectReading(adminUserId, role, readingId, request);

        return ResponseEntity.ok(response);
    }
}
