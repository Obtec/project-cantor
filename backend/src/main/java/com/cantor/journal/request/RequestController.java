package com.cantor.journal.request;

import com.cantor.journal.common.ApiException;
import com.cantor.journal.paper.Paper;
import com.cantor.journal.paper.PaperService;
import com.cantor.journal.request.dto.RequestDtos.CreateRequest;
import com.cantor.journal.request.dto.RequestDtos.RequestResponse;
import com.cantor.journal.request.dto.RequestDtos.ResolveRequest;
import com.cantor.journal.security.UserPrincipal;
import com.cantor.journal.user.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;
    private final PaperService paperService;

    /** 저자가 본인 논문에 대한 수정/삭제 요청 생성. */
    @PostMapping("/papers/{paperId}/requests")
    @PreAuthorize("isAuthenticated()")
    public RequestResponse create(@PathVariable Long paperId,
                                  @Valid @RequestBody CreateRequest req,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return RequestResponse.from(
                requestService.create(paperId, principal.getUser(), req.type(), req.message()));
    }

    /** 특정 논문의 요청 이력(저자 본인 또는 편집자). */
    @GetMapping("/papers/{paperId}/requests")
    @PreAuthorize("isAuthenticated()")
    public List<RequestResponse> forPaper(@PathVariable Long paperId,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        Paper paper = paperService.get(paperId);
        boolean editor = principal.getUser().getRoles().contains(Role.EDITOR);
        boolean owner = paper.getSubmitter().getId().equals(principal.getId());
        if (!editor && !owner) {
            throw ApiException.forbidden("권한이 없습니다.");
        }
        return requestService.forPaper(paperId).stream().map(RequestResponse::from).toList();
    }

    /** 편집자: 처리 대기 요청 목록. */
    @GetMapping("/requests")
    @PreAuthorize("hasRole('EDITOR')")
    public List<RequestResponse> pending() {
        return requestService.pending().stream().map(RequestResponse::from).toList();
    }

    /** 편집자: 요청 처리(승인/거절). */
    @PostMapping("/requests/{id}/resolve")
    @PreAuthorize("hasRole('EDITOR')")
    public RequestResponse resolve(@PathVariable Long id, @Valid @RequestBody ResolveRequest req) {
        return RequestResponse.from(requestService.resolve(id, req.status(), req.note()));
    }
}
