package com.cantor.journal.notification.dto;

import com.cantor.journal.notification.Notification;
import com.cantor.journal.notification.NotificationType;

import java.time.Instant;

public class NotificationDtos {

    public record NotificationResponse(
            Long id,
            NotificationType type,
            String message,
            String link,
            boolean read,
            Instant createdAt
    ) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(
                    n.getId(), n.getType(), n.getMessage(), n.getLink(), n.isRead(), n.getCreatedAt());
        }
    }
}
