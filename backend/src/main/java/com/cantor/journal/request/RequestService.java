package com.cantor.journal.request;

import com.cantor.journal.common.ApiException;
import com.cantor.journal.notification.NotificationService;
import com.cantor.journal.notification.NotificationType;
import com.cantor.journal.paper.Paper;
import com.cantor.journal.paper.PaperService;
import com.cantor.journal.user.Role;
import com.cantor.journal.user.User;
import com.cantor.journal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final PaperRequestRepository requestRepository;
    private final PaperService paperService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional
    public PaperRequest create(Long paperId, User requester, PaperRequest.Type type, String message) {
        Paper paper = paperService.get(paperId);
        if (!paper.getSubmitter().getId().equals(requester.getId())) {
            throw ApiException.forbidden("본인이 제출한 논문에 대해서만 요청할 수 있습니다.");
        }
        PaperRequest req = requestRepository.save(PaperRequest.builder()
                .paper(paper)
                .requester(requester)
                .type(type)
                .message(message)
                .status(PaperRequest.Status.PENDING)
                .build());
        notificationService.notifyAll(
                userRepository.findByRolesContaining(Role.EDITOR),
                NotificationType.AUTHOR_REQUEST,
                "저자가 " + typeLabel(type) + " 요청을 보냈습니다: " + paper.getTitle(),
                "/editor");
        return req;
    }

    @Transactional(readOnly = true)
    public List<PaperRequest> pending() {
        return requestRepository.findByStatusOrderByCreatedAtDesc(PaperRequest.Status.PENDING);
    }

    @Transactional(readOnly = true)
    public List<PaperRequest> forPaper(Long paperId) {
        return requestRepository.findByPaperIdOrderByCreatedAtDesc(paperId);
    }

    @Transactional(readOnly = true)
    public PaperRequest get(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("요청을 찾을 수 없습니다."));
    }

    @Transactional
    public PaperRequest resolve(Long id, PaperRequest.Status status, String note) {
        if (status == PaperRequest.Status.PENDING) {
            throw ApiException.badRequest("처리 상태(RESOLVED/REJECTED)를 지정하세요.");
        }
        PaperRequest req = get(id);
        req.setStatus(status);
        req.setEditorNote(note);
        req.setResolvedAt(Instant.now());
        requestRepository.save(req);
        notificationService.notify(
                req.getRequester(),
                NotificationType.REQUEST_RESOLVED,
                typeLabel(req.getType()) + " 요청이 " + statusLabel(status) + "되었습니다: " + req.getPaper().getTitle(),
                "/papers/" + req.getPaper().getId());
        return req;
    }

    private String typeLabel(PaperRequest.Type t) {
        return t == PaperRequest.Type.EDIT ? "수정" : "삭제";
    }

    private String statusLabel(PaperRequest.Status s) {
        return s == PaperRequest.Status.RESOLVED ? "처리" : "거절";
    }
}
