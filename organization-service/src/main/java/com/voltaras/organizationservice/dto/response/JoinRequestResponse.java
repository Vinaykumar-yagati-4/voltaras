package com.voltaras.organizationservice.dto.response;

import com.voltaras.organizationservice.enums.JoinRequestStatus;
import com.voltaras.organizationservice.enums.MembershipRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestResponse {

    private Long id;
    private Long organizationId;
    private String organizationName;
    private Long authUserId;
    private MembershipRole requestedRole;
    private JoinRequestStatus status;
    private String requestMessage;
    private String rejectionRemarks;
    private Long reviewedByAuthUserId;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
