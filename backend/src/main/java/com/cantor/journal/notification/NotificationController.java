package com.cantor.journal.notification;

import com.cantor.journal.common.ApiException;
import com.cantor.journal.notification.dto.NotificationDtos.NotificationResponse;
import com.cantor.journal.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(principal.getId())
                .stream().map(NotificationResponse::from).toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return Map.of("count", notificationRepository.countByRecipientIdAndReadFalse(principal.getId()));
    }

    @PostMapping("/{id}/read")
    @Transactional
    public void markRead(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("알림을 찾을 수 없습니다."));
        if (!n.getRecipient().getId().equals(principal.getId())) {
            throw ApiException.forbidden("본인 알림이 아닙니다.");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @PostMapping("/read-all")
    @Transactional
    public void markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationRepository.markAllRead(principal.getId());
    }
}
