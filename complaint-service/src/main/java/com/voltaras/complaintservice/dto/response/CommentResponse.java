package com.voltaras.complaintservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A single complaint comment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CommentResponse", description = "A comment on a complaint")
public class CommentResponse {

    @Schema(description = "Comment ID", example = "21")
    private Long id;

    @Schema(description = "Comment text")
    private String commentText;

    @Schema(description = "Auth Service user ID of the comment author", example = "13")
    private Long authorId;

    @Schema(description = "True when written by an admin resolution comment", example = "false")
    private boolean isAdminComment;

    @Schema(description = "Comment timestamp", example = "2026-08-12T11:00:00")
    private LocalDateTime createdAt;
}
