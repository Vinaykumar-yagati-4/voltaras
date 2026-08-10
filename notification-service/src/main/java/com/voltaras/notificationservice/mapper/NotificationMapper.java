package com.voltaras.notificationservice.mapper;

import com.voltaras.notificationservice.dto.response.NotificationResponse;
import com.voltaras.notificationservice.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Maps {@link Notification} entities to response DTOs.
 *
 * <p>Request DTOs are deliberately not mapped directly to the entity: type,
 * channel, status and timestamps are all assigned by the service layer.
 * </p>
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);
}
