package com.smartinstitute.erp.notification.inapp.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class InAppNotificationDto {
    private Long id;
    private String message;
    private boolean read;
    private Instant createdAt;
}
