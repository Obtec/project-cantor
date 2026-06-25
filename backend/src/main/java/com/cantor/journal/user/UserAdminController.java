package com.cantor.journal.user;

import com.cantor.journal.common.ApiException;
import com.cantor.journal.notification.NotificationService;
import com.cantor.journal.notification.NotificationType;
import com.cantor.journal.security.UserPrincipal;
import com.cantor.journal.user.dto.AuthDtos.UserAdminResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 편집자(편집위원) 권한 관리. 기존 편집장이 다른 사용자에게 편집장 권한을 부여/회수한다.
 * (JWT 필터가 매 요청 시 DB에서 역할을 다시 읽으므로 변경은 즉시 반영된다.)
 */
@RestController
@RequestMapping("/api/editor/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EDITOR')")
public class UserAdminController {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @GetMapping
    public List<UserAdminResponse> list() {
        return userRepository.findAll().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(UserAdminResponse::from)
                .toList();
    }

    @PostMapping("/{id}/editor")
    @Transactional
    public UserAdminResponse grant(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("사용자를 찾을 수 없습니다."));
        if (user.getRoles().add(Role.EDITOR)) {
            userRepository.save(user);
            notificationService.notify(user, NotificationType.EDITOR_GRANTED,
                    "편집장 권한이 부여되었습니다.", "/editor");
        }
        return UserAdminResponse.from(user);
    }

    @DeleteMapping("/{id}/editor")
    @Transactional
    public UserAdminResponse revoke(@PathVariable Long id,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getId().equals(id)) {
            throw ApiException.badRequest("본인의 편집장 권한은 회수할 수 없습니다.");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("사용자를 찾을 수 없습니다."));
        if (user.getRoles().contains(Role.EDITOR)
                && userRepository.findByRolesContaining(Role.EDITOR).size() <= 1) {
            throw ApiException.badRequest("최소 한 명의 편집장이 필요합니다.");
        }
        if (user.getRoles().remove(Role.EDITOR)) {
            userRepository.save(user);
        }
        return UserAdminResponse.from(user);
    }
}
